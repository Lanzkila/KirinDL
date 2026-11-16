"""Download media exposed by Telegram's public channel preview."""

import json
import mimetypes
import os
import re
from urllib.parse import urlsplit

import requests

from ..models import MAX_MEDIA_BYTES, USER_AGENT
from ..preview.channel import _is_telegram_media_url

_SAFE_NAME = re.compile(r"[^A-Za-z0-9._ -]+")


def _safe_filename(value, fallback):
    name = os.path.basename(str(value or "").replace("\\", "/")).strip()
    name = _SAFE_NAME.sub("_", name).strip(" .")
    if not name or name in (".", ".."):
        name = fallback
    return name[:160]


def _unique_path(directory, name):
    root, extension = os.path.splitext(name)
    candidate = os.path.join(directory, name)
    index = 1
    while os.path.exists(candidate):
        candidate = os.path.join(directory, "%s (%d)%s" % (root, index, extension))
        index += 1
    return candidate


def download_media(media_json, output_dir):
    """Download one or more selected direct media URLs into an app-provided directory."""
    try:
        media_items = json.loads(media_json) if isinstance(media_json, str) else media_json
        if not isinstance(media_items, list) or not media_items:
            raise ValueError("Select at least one Telegram media item")

        output_dir = os.path.realpath(output_dir)
        os.makedirs(output_dir, exist_ok=True)
        results = []

        for index, item in enumerate(media_items):
            if not isinstance(item, dict):
                continue
            url = str(item.get("url") or "").strip()
            if not _is_telegram_media_url(url):
                results.append(
                    {"ok": False, "url": url, "error": "Media URL is not a trusted Telegram CDN URL"}
                )
                continue

            requested_name = _safe_filename(
                item.get("file_name"),
                "telegram_media_%d" % (index + 1),
            )
            destination = _unique_path(output_dir, requested_name)
            part_path = destination + ".part"

            try:
                with requests.get(
                    url,
                    headers={"User-Agent": USER_AGENT},
                    stream=True,
                    timeout=(20, 60),
                    allow_redirects=True,
                ) as response:
                    response.raise_for_status()
                    if not _is_telegram_media_url(response.url):
                        raise ValueError("Telegram media redirected to an unexpected host")

                    total = 0
                    length = response.headers.get("Content-Length")
                    if length and int(length) > MAX_MEDIA_BYTES:
                        raise ValueError("Media exceeds the configured 2 GB limit")

                    with open(part_path, "wb") as output:
                        for chunk in response.iter_content(chunk_size=128 * 1024):
                            if not chunk:
                                continue
                            total += len(chunk)
                            if total > MAX_MEDIA_BYTES:
                                raise ValueError("Media exceeds the configured 2 GB limit")
                            output.write(chunk)

                if total <= 0:
                    raise ValueError("Telegram returned an empty media file")
                os.replace(part_path, destination)

                mime = response.headers.get("Content-Type", "").split(";", 1)[0].strip()
                if not mime:
                    mime = mimetypes.guess_type(destination)[0] or ""
                results.append(
                    {
                        "ok": True,
                        "url": url,
                        "path": destination,
                        "file_name": os.path.basename(destination),
                        "mime_type": mime,
                        "size_bytes": total,
                    }
                )
            except Exception as exc:
                try:
                    if os.path.exists(part_path):
                        os.remove(part_path)
                except OSError:
                    pass
                results.append(
                    {"ok": False, "url": url, "error": "%s: %s" % (type(exc).__name__, str(exc))}
                )

        return json.dumps(
            {
                "ok": any(item.get("ok") for item in results),
                "results": results,
                "error": "" if any(item.get("ok") for item in results) else "No Telegram media files were downloaded",
            },
            ensure_ascii=False,
        )
    except Exception as exc:
        return json.dumps(
            {
                "ok": False,
                "results": [],
                "error": "%s: %s" % (type(exc).__name__, str(exc)),
            },
            ensure_ascii=False,
        )
