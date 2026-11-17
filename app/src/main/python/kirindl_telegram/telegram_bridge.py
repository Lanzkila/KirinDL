# DISABLED: Telegram now uses the existing yt-dlp TelegramEmbedIE path.
# Original adapter kept commented for reference; do not re-enable a separate engine.
# """Stable JSON bridge called by KirinDL's Kotlin Telegram engine."""
# 
# from .download.media import download_media
# from .preview.channel import preview_channel
# 
# 
# def preview(url, limit=20, before_id=""):
#     return preview_channel(url, limit=limit, before_id=before_id)
# 
# 
# def download(media_json, output_dir):
#     return download_media(media_json, output_dir)
# 