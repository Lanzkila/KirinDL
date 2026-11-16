# KirinDL Telegram Engine

A standalone adapter for **public Telegram channels and public post links**.

## Modules

- `url_parser.py`: validates public `t.me` and `telegram.me` channel/post URLs.
- `preview/channel.py`: reads Telegram's public `t.me/s/<channel>` web preview and extracts a bounded page of posts and media metadata.
- `download/media.py`: downloads only direct media URLs on Telegram CDN hostnames exposed by the public preview.
- `telegram_bridge.py`: stable JSON entry points used by Chaquopy.
- Kotlin engine packages under `com.junkfood.seal.util.telegram` expose preview models, pagination, and selected-media downloads.

## Current limitations

- Public channels and public posts only; invite-only/private channels require a separately designed authenticated Telegram client.
- The public web preview exposes a limited rolling window. Older posts may be paged with the returned message ID cursor.
- Only media with direct Telegram CDN URLs in the preview can be downloaded. Unsupported embedded players or hidden documents are reported as unavailable rather than guessed.
- No Telegram login, API hash, phone number, password, or session data is collected or stored.
- Downloads are placed in the app-specific external Downloads folder under `KirinDL/Telegram` (or internal app storage when external app storage is unavailable).
