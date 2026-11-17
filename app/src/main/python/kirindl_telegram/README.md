# Telegram handling in KirinDL

The standalone KirinDL Telegram adapter is **disabled** and retained only as commented reference code.

Telegram links are sent through the app's existing yt-dlp download flow. The upstream yt-dlp `TelegramEmbedIE` extractor supports public post URLs in the form:

`https://t.me/<channel>/<message-id>`

Its supported scope is embedded video media exposed by a public post (including multi-video posts). This is not full channel scraping and does not promise support for channel-only URLs, photos, audio, documents, private/invite-only channels, or authenticated account content.

Gallery-DL has no Telegram extractor in its upstream supported-sites list, so it is not used for Telegram.

No Telegram-specific login, API hash, phone number, password, session data, preview runtime, or separate download engine is active in KirinDL.
