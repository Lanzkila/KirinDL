# KirinDL Telegram Integration

This directory is reserved for KirinDL-owned Telegram adapter code.

Planned scope:
- Public channel and post preview through Telegram's public web views where available.
- Media and album metadata suitable for a selective queue.
- A separate, optional authenticated client integration for channels the user's Telegram account can access.

Keep Telegram logic isolated from `gallery_bridge.py` and `kirindl_instaloader/`.
Do not store Telegram credentials or session data in this package. Any authenticated integration must use a dedicated secure session-storage design and explicit user sign-in.
