# DISABLED: legacy Instaloader integration; Instagram is handled by gallery-dl.
# """Bounded, public-only Instagram profile preview for KirinDL.
#
# This adapter never logs in, stores credentials, or downloads media. It returns canonical
# post permalinks and preview thumbnails; the Android GalleryDL queue handles downloads.
# """
# import json
# import re
# from urllib.parse import urlsplit
#
# _PROFILE_PATTERN = re.compile(r"^[A-Za-z0-9._]{1,30}$")
# _RESERVED_SEGMENTS = {
#     "p", "reel", "reels", "tv", "stories", "explore", "accounts", "direct",
#     "about", "developer", "api", "graphql", "challenge", "web", "legal",
# }
#
#
# def _username_from_url(url):
#     parts = urlsplit(str(url or "").strip())
#     try:
#         port = parts.port
#     except ValueError as exc:
#         raise ValueError("Invalid Instagram profile URL") from exc
#
#     if parts.scheme.lower() != "https":
#         raise ValueError("Instagram profile preview requires HTTPS")
#     if parts.hostname not in ("instagram.com", "www.instagram.com"):
#         raise ValueError("URL is not an Instagram profile")
#     if parts.username or parts.password or port is not None:
#         raise ValueError("Invalid Instagram profile URL")
#
#     segments = [segment for segment in parts.path.split("/") if segment]
#     if (
#         len(segments) != 1
#         or not _PROFILE_PATTERN.fullmatch(segments[0])
#         or segments[0].lower() in _RESERVED_SEGMENTS
#     ):
#         raise ValueError("Expected an Instagram profile URL, not a post or reel")
#     return segments[0]
#
#
# def preview_profile(url, limit=36):
#     """Return up to 60 recent public posts as canonical Instagram permalinks."""
#     username = _username_from_url(url)
#     limit = max(1, min(int(limit), 60))
#
#     try:
#         import instaloader
#
#         loader = instaloader.Instaloader(
#             download_pictures=False,
#             download_videos=False,
#             download_video_thumbnails=False,
#             download_geotags=False,
#             download_comments=False,
#             save_metadata=False,
#             compress_json=False,
#             quiet=True,
#         )
#         profile = instaloader.Profile.from_username(loader.context, username)
#         if bool(getattr(profile, "is_private", False)):
#             raise RuntimeError(
#                 "This profile is private. Instaloader preview currently supports public profiles only."
#             )
#
#         items = []
#         for post in profile.get_posts():
#             if len(items) >= limit:
#                 break
#
#             shortcode = str(getattr(post, "shortcode", "") or "")
#             if not re.fullmatch(r"[A-Za-z0-9_-]+", shortcode):
#                 continue
#
#             items.append(
#                 {
#                     "id": shortcode,
#                     "title": str(getattr(post, "caption", "") or "")[:120] or shortcode,
#                     "url": "https://www.instagram.com/p/%s/" % shortcode,
#                     "thumbnail": str(getattr(post, "url", "") or ""),
#                     "creator": username,
#                 }
#             )
#
#         media_count = getattr(profile, "mediacount", None)
#         try:
#             media_count = int(media_count) if media_count is not None else None
#         except (TypeError, ValueError):
#             media_count = None
#
#         return json.dumps(
#             {
#                 "ok": True,
#                 "username": username,
#                 "full_name": str(getattr(profile, "full_name", "") or ""),
#                 "media_count": media_count,
#                 "items": items,
#                 "authenticated": False,
#             },
#             ensure_ascii=False,
#         )
#     except Exception as exc:
#         return json.dumps(
#             {
#                 "ok": False,
#                 "username": username,
#                 "full_name": "",
#                 "media_count": None,
#                 "items": [],
#                 "authenticated": False,
#                 "error": "%s: %s" % (type(exc).__name__, str(exc)),
#             },
#             ensure_ascii=False,
#         )
#