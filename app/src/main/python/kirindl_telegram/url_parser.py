# DISABLED: Telegram now uses the existing yt-dlp TelegramEmbedIE path.
# Original adapter kept commented for reference; do not re-enable a separate engine.
# """Strict URL parsing for public Telegram channels and individual posts."""
# 
# import re
# from urllib.parse import urlsplit
# 
# _USERNAME = re.compile(r"^[A-Za-z0-9_]{5,32}$")
# _RESERVED = {
#     "addstickers", "confirmphone", "joinchat", "login", "proxy",
#     "setlanguage", "share", "socks", "iv", "c",
# }
# 
# 
# def parse_telegram_url(raw_url):
#     """Return (username, message_id) or raise ValueError for unsupported links."""
#     value = str(raw_url or "").strip()
#     if not value:
#         raise ValueError("Enter a Telegram channel or post URL")
# 
#     if "://" not in value:
#         value = "https://" + value
# 
#     parts = urlsplit(value)
#     try:
#         port = parts.port
#     except ValueError as exc:
#         raise ValueError("Invalid Telegram URL") from exc
# 
#     if parts.scheme.lower() != "https":
#         raise ValueError("Telegram preview requires an HTTPS link")
#     if parts.hostname not in ("t.me", "www.t.me", "telegram.me", "www.telegram.me"):
#         raise ValueError("URL is not a Telegram link")
#     if parts.username or parts.password or port is not None:
#         raise ValueError("Invalid Telegram URL")
# 
#     segments = [segment for segment in parts.path.split("/") if segment]
#     if segments and segments[0].lower() == "s":
#         segments = segments[1:]
# 
#     if not segments or len(segments) > 2:
#         raise ValueError("Expected a public Telegram channel or post link")
# 
#     username = segments[0]
#     if not _USERNAME.fullmatch(username) or username.lower() in _RESERVED:
#         raise ValueError("This Telegram link is not a public channel URL")
# 
#     message_id = None
#     if len(segments) == 2:
#         if not segments[1].isdigit():
#             raise ValueError("Telegram post ID must be numeric")
#         message_id = int(segments[1])
#         if message_id <= 0:
#             raise ValueError("Telegram post ID must be positive")
# 
#     return username, message_id
# 
# 
# def public_feed_url(username):
#     if not _USERNAME.fullmatch(str(username or "")):
#         raise ValueError("Invalid Telegram username")
#     return "https://t.me/s/%s" % username
# 