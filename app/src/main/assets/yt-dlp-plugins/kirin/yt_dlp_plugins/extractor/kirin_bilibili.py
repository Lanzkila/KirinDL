"""Use Bilibili's own mirrors without replacing formats, hosts or signed queries.

Loaded only for KirinDL Bilibili transfers. The app starts a fresh extraction
after a transfer failure and selects at most two backup positions. Keeping the
same downloader and format IDs preserves its normal partial-file bookkeeping.
"""

from urllib.parse import urlsplit

from yt_dlp.extractor.bilibili import BiliBiliIE as _BiliBiliIE

__all__ = []


def _http_url(value):
    if not isinstance(value, str):
        return False
    try:
        parsed = urlsplit(value)
        return parsed.scheme in ('http', 'https') and bool(parsed.hostname)
    except ValueError:
        return False


def _mirror_map(play_info):
    """Map each original stream URL to backups on that exact API stream entry."""
    mirrors = {}

    def visit(value):
        if isinstance(value, list):
            for entry in value:
                visit(entry)
        elif isinstance(value, dict):
            primary = next((value.get(key) for key in ('baseUrl', 'base_url', 'url')
                            if _http_url(value.get(key))), None)
            if primary:
                candidates = mirrors.setdefault(primary, [])
                for key in ('backupUrl', 'backup_url'):
                    backups = value.get(key) or []
                    if isinstance(backups, str):
                        backups = [backups]
                    if isinstance(backups, list):
                        for url in backups:
                            if _http_url(url) and url != primary and url not in candidates:
                                candidates.append(url)
            for key in ('dash', 'video', 'audio', 'dolby', 'flac', 'durl'):
                if key in value:
                    visit(value[key])

    visit(play_info)
    return mirrors


def _apply_mirrors(formats, mirrors, attempt):
    """Mutate URLs only; upstream retains format ID, codec, size and headers."""
    available = 0
    switched = 0
    for fmt in formats:
        for stream in [fmt, *fmt.get('fragments', [])]:
            candidates = mirrors.get(stream.get('url'), [])
            available = max(available, len(candidates))
            if 0 < attempt <= len(candidates):
                stream['url'] = candidates[attempt - 1]
                switched += 1
    return available, switched


class _KirinBiliBiliIE(_BiliBiliIE, plugin_name='kirin-cdn'):
    def extract_formats(self, play_info):
        # Call upstream first: never recompute its codec/quality-specific IDs
        # from a mirror filename or maintain a fork of Bilibili's API signing.
        formats = super().extract_formats(play_info)
        try:
            attempt = int(self._configuration_arg(
                'kirin_cdn_attempt', ['0'], ie_key='BiliBili')[0])
        except (ValueError, TypeError, IndexError):
            attempt = 0
        attempt = min(max(attempt, 0), 2)
        available, switched = _apply_mirrors(formats, _mirror_map(play_info), attempt)
        self.to_screen(f'[KirinBilibili] backups={min(available, 2)} attempt={attempt} switched={switched}')
        return formats
