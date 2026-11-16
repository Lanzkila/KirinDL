"""Read-only preview of public Telegram channel posts using t.me/s/ HTML."""

import html
import json
import mimetypes
import re
from html.parser import HTMLParser
from urllib.parse import quote, urljoin, urlsplit

import requests

from ..models import DEFAULT_PREVIEW_LIMIT, MAX_PAGE_BYTES, MAX_PREVIEW_LIMIT, USER_AGENT
from ..url_parser import parse_telegram_url, public_feed_url

_MEDIA_HOST_SUFFIXES = (".telesco.pe", ".telegram.org", ".telegram-cdn.org")
_STYLE_URL = re.compile(r"url\(\s*['\"]?(.*?)['\"]?\s*\)", re.IGNORECASE)
_MESSAGE_TEXT_LIMIT = 3000


def _is_telegram_media_url(raw_url):
    try:
        parts = urlsplit(str(raw_url or "").strip())
    except ValueError:
        return False
    host = (parts.hostname or "").lower()
    if parts.scheme.lower() != "https" or not host:
        return False
    return any(host.endswith(suffix) for suffix in _MEDIA_HOST_SUFFIXES)


def _media_kind(tag, classes, url):
    lower = str(url or "").lower().split("?", 1)[0]
    joined = " ".join(classes)
    if tag in ("video", "source") or "video" in joined:
        return "video"
    if tag == "audio" or "audio" in joined or "voice" in joined:
        return "audio"
    if "document" in joined or "file" in joined:
        return "document"
    if "photo" in joined or lower.endswith((".jpg", ".jpeg", ".png", ".webp", ".gif", ".avif")):
        return "photo"
    if lower.endswith((".mp4", ".m4v", ".mov", ".webm", ".mkv")):
        return "video"
    if lower.endswith((".mp3", ".m4a", ".ogg", ".opus", ".wav")):
        return "audio"
    return "file"


class _TelegramHtmlParser(HTMLParser):
    _VOID_TAGS = {
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr",
    }

    def __init__(self, username):
        super().__init__(convert_charrefs=True)
        self.username = username
        self.posts = []
        self.channel_title = ""
        self.description = ""
        self.subscribers = ""
        self._stack = []
        self._depth = 0
        self._current = None
        self._root_depth = None
        self._root_tag = None
        self._text_depth = None
        self._text_tag = None
        self._views_depth = None
        self._views_tag = None
        self._media_context = []

    def _start_post(self, data_post):
        message_id = str(data_post or "").rsplit("/", 1)[-1]
        if not message_id.isdigit():
            return
        self._current = {
            "id": int(message_id),
            "text_parts": [],
            "views_parts": [],
            "date": "",
            "media": [],
        }

    def _add_media(self, raw_url, tag="", classes=None, thumbnail=""):
        if self._current is None:
            return
        classes = classes or set()
        absolute = urljoin("https://t.me/", html.unescape(str(raw_url or "").strip()))
        if not _is_telegram_media_url(absolute):
            return
        if any(item["url"] == absolute for item in self._current["media"]):
            return
        kind = _media_kind(tag, classes, absolute)
        path_name = urlsplit(absolute).path.rsplit("/", 1)[-1]
        mime_type = mimetypes.guess_type(path_name)[0] or ""
        self._current["media"].append(
            {
                "id": "%s-%d" % (self._current["id"], len(self._current["media"]) + 1),
                "kind": kind,
                "url": absolute,
                "thumbnail_url": thumbnail if _is_telegram_media_url(thumbnail) else (
                    absolute if kind == "photo" else ""
                ),
                "file_name": path_name if "." in path_name else "",
                "mime_type": mime_type,
            }
        )

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        classes = set((attrs.get("class") or "").split())
        is_void = tag in self._VOID_TAGS

        if not is_void:
            self._depth += 1
        current_depth = self._depth

        if "tgme_widget_message" in classes and attrs.get("data-post"):
            if self._current is not None:
                self._finish_post()
            self._start_post(attrs.get("data-post"))
            self._root_depth = current_depth
            self._root_tag = tag

        if self._current is not None:
            if "tgme_widget_message_text" in classes:
                self._text_depth = current_depth
                self._text_tag = tag
            if "tgme_widget_message_views" in classes:
                self._views_depth = current_depth
                self._views_tag = tag
            if tag == "time" and attrs.get("datetime"):
                self._current["date"] = attrs.get("datetime", "")

            media_classes = {
                token for token in classes
                if token in ("photo", "photo_wrap", "video", "audio", "voice", "document", "file")
                or token.startswith("tgme_widget_message_photo")
                or token.startswith("tgme_widget_message_video")
                or token.startswith("tgme_widget_message_document")
            }
            if media_classes:
                self._media_context.append((current_depth, media_classes))
                style = attrs.get("style", "")
                for match in _STYLE_URL.findall(style):
                    self._add_media(match, tag=tag, classes=media_classes)

            context_classes = set()
            for _, tokens in self._media_context:
                context_classes.update(tokens)

            if tag in ("video", "audio", "source"):
                self._add_media(
                    attrs.get("src"),
                    tag=tag,
                    classes=context_classes | classes,
                    thumbnail=attrs.get("poster", ""),
                )
                if attrs.get("poster"):
                    self._add_media(
                        attrs.get("poster"),
                        tag="img",
                        classes={"photo"},
                    )
            elif tag == "img" and context_classes:
                self._add_media(attrs.get("src"), tag=tag, classes=context_classes | classes)

            if tag == "a" and ("document" in context_classes or "file" in context_classes):
                self._add_media(attrs.get("href"), tag=tag, classes=context_classes | classes)

        if tag == "meta":
            prop = (attrs.get("property") or attrs.get("name") or "").lower()
            value = html.unescape(attrs.get("content") or "").strip()
            if prop == "og:title" and value:
                self.channel_title = value
            elif prop == "og:description" and value:
                self.description = value

        if not is_void:
            self._stack.append((tag, classes, current_depth))

    def handle_startendtag(self, tag, attrs):
        self.handle_starttag(tag, attrs)
        if tag not in self._VOID_TAGS:
            self.handle_endtag(tag)

    def handle_data(self, data):
        if self._current is None:
            return
        if self._text_depth is not None and self._depth >= self._text_depth:
            self._current["text_parts"].append(data)
        if self._views_depth is not None and self._depth >= self._views_depth:
            self._current["views_parts"].append(data)

    def handle_endtag(self, tag):
        if not self._stack:
            return
        # HTMLParser normally supplies well-formed nesting for Telegram's web page. If a
        # malformed optional tag appears, pop back to the matching open tag without crashing.
        index = next(
            (i for i in range(len(self._stack) - 1, -1, -1) if self._stack[i][0] == tag),
            None,
        )
        if index is None:
            return

        closing = self._stack[index:]
        depth = self._stack[index][2]
        classes = self._stack[index][1]

        if self._current is not None:
            if self._text_tag == tag and self._text_depth == depth:
                self._text_depth = None
                self._text_tag = None
            if self._views_tag == tag and self._views_depth == depth:
                self._views_depth = None
                self._views_tag = None
            self._media_context = [
                item for item in self._media_context if item[0] < depth
            ]

            if self._root_tag == tag and self._root_depth == depth:
                self._finish_post()

        del self._stack[index:]
        self._depth = max(0, self._depth - len(closing))

    def _finish_post(self):
        if self._current is None:
            return
        current = self._current
        text = " ".join(" ".join(current["text_parts"]).split())[:_MESSAGE_TEXT_LIMIT]
        views = " ".join(" ".join(current["views_parts"]).split())
        post_id = current["id"]
        self.posts.append(
            {
                "id": post_id,
                "url": "https://t.me/%s/%d" % (self.username, post_id),
                "title": (text.split("\n", 1)[0][:120] if text else "Telegram post %d" % post_id),
                "text": text,
                "date": current["date"],
                "views": views,
                "media": current["media"],
            }
        )
        self._current = None
        self._root_depth = None
        self._root_tag = None
        self._text_depth = None
        self._text_tag = None
        self._views_depth = None
        self._views_tag = None
        self._media_context = []

    def close(self):
        super().close()
        if self._current is not None:
            self._finish_post()


def _fetch_page(url, before_id=""):
    params = {}
    if before_id:
        params["before"] = str(int(before_id))
    response = requests.get(
        url,
        params=params,
        headers={
            "User-Agent": USER_AGENT,
            "Accept": "text/html,application/xhtml+xml",
        },
        timeout=(15, 30),
        allow_redirects=True,
    )
    response.raise_for_status()
    if not response.url.startswith("https://t.me/"):
        raise ValueError("Telegram redirected the preview to an unexpected host")
    body = response.content
    if len(body) > MAX_PAGE_BYTES:
        raise ValueError("Telegram preview page exceeded the size limit")
    return response.text


def preview_channel(url, limit=DEFAULT_PREVIEW_LIMIT, before_id="", requested_message_id=None):
    """Fetch a bounded page of posts from a public Telegram channel."""
    username, message_id = parse_telegram_url(url)
    limit = max(1, min(int(limit), MAX_PREVIEW_LIMIT))
    target_message_id = requested_message_id or message_id

    try:
        feed_url = public_feed_url(username)
        cursor = before_id
        if target_message_id is not None:
            cursor = int(target_message_id) + 1

        page = _fetch_page(feed_url, cursor)
        parser = _TelegramHtmlParser(username)
        parser.feed(page)
        parser.close()

        counter = re.search(
            r'<div[^>]+class=["\'][^"\']*tgme_channel_info_counter[^"\']*["\'][^>]*>(.*?)</div>',
            page,
            re.IGNORECASE | re.DOTALL,
        )
        if counter:
            parser.subscribers = " ".join(
                re.sub(r"<[^>]+>", " ", counter.group(1)).split()
            )

        posts = sorted(parser.posts, key=lambda post: post["id"], reverse=True)
        if target_message_id is not None:
            posts = [post for post in posts if post["id"] == int(target_message_id)]
            if not posts:
                raise ValueError(
                    "Telegram did not expose that post in its public web preview"
                )
        else:
            posts = posts[:limit]

        ids = [post["id"] for post in posts]
        next_before_id = min(ids) if ids else None
        return json.dumps(
            {
                "ok": True,
                "username": username,
                "title": parser.channel_title or username,
                "description": parser.description,
                "subscribers": parser.subscribers,
                "posts": posts,
                "next_before_id": next_before_id,
                "has_more": target_message_id is None and len(posts) >= limit,
                "is_single_post": target_message_id is not None,
                "authenticated": False,
            },
            ensure_ascii=False,
        )
    except Exception as exc:
        return json.dumps(
            {
                "ok": False,
                "username": username,
                "title": username,
                "description": "",
                "subscribers": "",
                "posts": [],
                "next_before_id": None,
                "has_more": False,
                "is_single_post": target_message_id is not None,
                "authenticated": False,
                "error": "%s: %s" % (type(exc).__name__, str(exc)),
            },
            ensure_ascii=False,
        )
