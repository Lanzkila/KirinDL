"""Chaquopy bridge for KirinDownloader's optional Codeberg gallery-dl engine.

The upstream gallery-dl package remains untouched. This bridge supplies Android paths, persistent
configuration/cache files, safe output confinement, extractor preflight, and runtime diagnostics.
"""

import importlib
import json
import os
import sys
import traceback


def _load_engine(engine_dir):
    engine_dir = os.path.abspath(engine_dir)
    if engine_dir not in sys.path:
        sys.path.insert(0, engine_dir)

    # If an update was installed while Python stayed alive, make sure the next import sees it.
    for name in list(sys.modules):
        if name == "gallery_dl" or name.startswith("gallery_dl."):
            del sys.modules[name]

    import gallery_dl

    return gallery_dl


def _prepare_config(config, util, output_dir, config_path, cookies_path, cache_path):
    """Load app-private settings, then enforce Android safety/runtime invariants."""
    config.clear()

    config_loaded = False
    if config_path:
        config_path = os.path.abspath(config_path)
        if os.path.isfile(config_path):
            with open(config_path, encoding="utf-8") as fp:
                util.json_loads(fp.read())
            config.load((config_path,), strict=False)
            config_loaded = True

    cookies_loaded = False
    if cookies_path:
        cookies_path = os.path.abspath(cookies_path)
        if os.path.isfile(cookies_path) and os.path.getsize(cookies_path) > 0:
            config.set(("extractor",), "cookies", cookies_path)
            cookies_loaded = True

    if cache_path:
        cache_path = os.path.abspath(cache_path)
        os.makedirs(os.path.dirname(cache_path), exist_ok=True)
        config.set(("cache",), "file", cache_path)

    # The user config is loaded first. These Android invariants are applied last so custom settings
    # cannot redirect files outside the job sandbox, request interactive terminal input, or disable
    # TLS certificate verification.
    if output_dir:
        output_dir = os.path.abspath(output_dir)
        os.makedirs(output_dir, exist_ok=True)
        config.set(("extractor",), "base-directory", output_dir)

    config.set(("extractor",), "input", False)
    config.set(("extractor",), "verify", True)
    config.set(("downloader",), "verify", True)

    return config_loaded, cookies_loaded


def _module_status(module_name):
    try:
        module = importlib.import_module(module_name)
        version = getattr(module, "__version__", "")
        if not version:
            version = getattr(module, "VERSION", "")
        return True, str(version) if version else ""
    except Exception:
        return False, ""


def _extractor_info(extr):
    if extr is None:
        return {
            "supported": False,
            "base_category": "",
            "category": "",
            "subcategory": "",
            "class_name": "",
            "extractor": "",
        }

    category = str(getattr(extr, "category", "") or "")
    subcategory = str(getattr(extr, "subcategory", "") or "")
    base_category = str(getattr(extr, "basecategory", "") or "")
    class_name = extr.__class__.__name__
    label = " / ".join(value for value in (category, subcategory) if value) or class_name

    return {
        "supported": True,
        "base_category": base_category,
        "category": category,
        "subcategory": subcategory,
        "class_name": class_name,
        "extractor": label,
    }


def engine_version(engine_dir):
    try:
        gallery_dl = _load_engine(engine_dir)
        return str(gallery_dl.__version__)
    except Exception:
        return ""


def diagnostics(engine_dir):
    """Return a compact report of core and optional Python helpers visible to gallery-dl."""
    try:
        gallery_dl = _load_engine(engine_dir)

        required = (
            ("requests", "requests"),
            ("sqlite3", "sqlite3"),
        )
        optional = (
            ("yt-dlp", "yt_dlp"),
            ("PySocks", "socks"),
            ("PyYAML", "yaml"),
            ("Jinja2", "jinja2"),
            ("Brotli", "brotli"),
            ("Zstandard", "zstandard"),
        )

        ready = []
        missing_optional = []

        for label, module_name in required:
            available, version = _module_status(module_name)
            if not available:
                return json.dumps(
                    {
                        "ok": False,
                        "version": str(gallery_dl.__version__),
                        "ready": ready,
                        "missing_optional": missing_optional,
                        "error": "Required Python module '%s' is unavailable" % label,
                    },
                    ensure_ascii=False,
                )
            ready.append("%s%s" % (label, (" " + version) if version else ""))

        for label, module_name in optional:
            available, version = _module_status(module_name)
            if available:
                ready.append("%s%s" % (label, (" " + version) if version else ""))
            else:
                missing_optional.append(label)

        return json.dumps(
            {
                "ok": True,
                "version": str(gallery_dl.__version__),
                "ready": ready,
                "missing_optional": missing_optional,
                "error": "",
            },
            ensure_ascii=False,
        )
    except Exception as exc:
        return json.dumps(
            {
                "ok": False,
                "version": "",
                "ready": [],
                "missing_optional": [],
                "error": "%s: %s" % (type(exc).__name__, str(exc)),
            },
            ensure_ascii=False,
        )


_PREVIEW_SCAN_LIMIT = 40
_LARGE_GALLERY_THRESHOLD = 30
_IMAGE_EXTENSIONS = {
    "jpg", "jpeg", "png", "gif", "webp", "avif", "bmp", "tif", "tiff", "heic",
}
_VIDEO_EXTENSIONS = {
    "mp4", "webm", "mov", "mkv", "m4v", "avi", "flv", "ts",
}


def _safe_text(value):
    if value is None:
        return ""
    if isinstance(value, str):
        return value.strip()
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        return str(value)
    if isinstance(value, (list, tuple)):
        for item in value:
            text = _safe_text(item)
            if text:
                return text
        return ""
    if isinstance(value, dict):
        for key in ("name", "username", "title", "display_name", "id"):
            text = _safe_text(value.get(key))
            if text:
                return text
    return ""


def _pick_text(data, keys):
    if not isinstance(data, dict):
        return ""
    for key in keys:
        if key in data:
            value = _safe_text(data.get(key))
            if value:
                return value
    return ""


def _pick_positive_int(data, keys):
    if not isinstance(data, dict):
        return None
    for key in keys:
        value = data.get(key)
        if isinstance(value, bool):
            continue
        try:
            number = int(value)
        except (TypeError, ValueError):
            continue
        if number >= 0:
            return number
    return None


def _extension_from(url, data):
    extension = _pick_text(data, ("extension", "ext")).lower().lstrip(".")
    if extension:
        return extension
    clean = str(url or "").split("?", 1)[0].split("#", 1)[0]
    name = clean.rsplit("/", 1)[-1]
    if "." not in name:
        return ""
    return name.rsplit(".", 1)[-1].lower()


def _classify_preflight_error(exc):
    text = ("%s: %s" % (type(exc).__name__, str(exc))).lower()
    if "429" in text or "rate limit" in text or "too many requests" in text:
        return "rate_limited"
    if (
        "401" in text
        or "login required" in text
        or "authentication" in text
        or "not logged in" in text
        or "cookies required" in text
        or "cookie required" in text
    ):
        return "login_required"
    return "extractor_error"


def inspect_url(
    url,
    engine_dir,
    config_path="",
    cookies_path="",
    cache_path="",
):
    """Resolve and lightly inspect a URL without downloading media files.

    The extractor iterator may request HTML/API metadata, but KirinDL never invokes a downloader
    here. Scanning is capped so a very large feed/gallery does not turn confirmation into a full
    crawl.
    """
    try:
        gallery_dl = _load_engine(engine_dir)
        from gallery_dl import config, extractor, util
        from gallery_dl.extractor.message import Message

        _, cookies_loaded = _prepare_config(
            config,
            util,
            "",
            config_path,
            cookies_path,
            cache_path,
        )

        extr = extractor.find(url)
        info = _extractor_info(extr)
        if not info["supported"]:
            return json.dumps(
                {
                    "ok": True,
                    "version": str(gallery_dl.__version__),
                    **info,
                    "title": "",
                    "author": "",
                    "thumbnail": "",
                    "media_type": "",
                    "estimated_count": None,
                    "count_exact": False,
                    "scanned_count": 0,
                    "large_gallery": False,
                    "cookies_loaded": cookies_loaded,
                    "preflight_status": "unsupported",
                    "preflight_error": "No gallery-dl extractor matched this URL",
                    "error": "No gallery-dl extractor matched this URL",
                },
                ensure_ascii=False,
            )

        title = ""
        author = ""
        thumbnail = ""
        exact_count = None
        emitted_count = 0
        url_count = 0
        queue_count = 0
        media_kinds = set()
        hit_limit = False
        preflight_status = "ready"
        preflight_error = ""

        try:
            for message, target, data in extr:
                if isinstance(data, dict):
                    if not title:
                        title = _pick_text(
                            data,
                            (
                                "title", "gallery_title", "album", "album_name", "set_name",
                                "collection", "name", "post_title",
                            ),
                        )
                    if not author:
                        author = _pick_text(
                            data,
                            (
                                "username", "user_name", "author", "author_name", "artist",
                                "artist_name", "uploader", "channel", "account", "owner", "user",
                            ),
                        )
                    if not thumbnail:
                        candidate = _pick_text(
                            data,
                            (
                                "thumbnail", "thumbnail_url", "preview", "preview_url", "cover",
                                "cover_url", "poster", "poster_url",
                            ),
                        )
                        if candidate.startswith(("https://", "http://")):
                            thumbnail = candidate

                    if message == Message.Directory and exact_count is None:
                        exact_count = _pick_positive_int(
                            data,
                            (
                                "count", "total", "total_count", "image_count", "file_count",
                                "post_count", "media_count",
                            ),
                        )

                if message == Message.Url:
                    url_count += 1
                    emitted_count += 1
                    extension = _extension_from(target, data)
                    if extension in _IMAGE_EXTENSIONS:
                        media_kinds.add("image")
                    elif extension in _VIDEO_EXTENSIONS:
                        media_kinds.add("video")
                    elif extension:
                        media_kinds.add("other")
                elif message == Message.Queue:
                    queue_count += 1
                    emitted_count += 1

                if emitted_count >= _PREVIEW_SCAN_LIMIT:
                    hit_limit = True
                    break
        except Exception as exc:
            preflight_status = _classify_preflight_error(exc)
            preflight_error = "%s: %s" % (type(exc).__name__, str(exc))

        if "image" in media_kinds and "video" in media_kinds:
            media_type = "Mixed media"
        elif "video" in media_kinds:
            media_type = "Videos"
        elif "image" in media_kinds:
            media_type = "Images"
        elif queue_count and not url_count:
            media_type = "Collection"
        elif url_count:
            media_type = "Media"
        else:
            media_type = ""

        if exact_count is not None:
            estimated_count = exact_count
            count_exact = True
        elif emitted_count:
            estimated_count = emitted_count
            count_exact = not hit_limit and preflight_status == "ready"
        else:
            estimated_count = None
            count_exact = False

        large_gallery = bool(
            estimated_count is not None
            and estimated_count >= _LARGE_GALLERY_THRESHOLD
            and (count_exact or hit_limit)
        )

        return json.dumps(
            {
                "ok": True,
                "version": str(gallery_dl.__version__),
                **info,
                "title": title,
                "author": author,
                "thumbnail": thumbnail,
                "media_type": media_type,
                "estimated_count": estimated_count,
                "count_exact": count_exact,
                "scanned_count": emitted_count,
                "large_gallery": large_gallery,
                "cookies_loaded": cookies_loaded,
                "preflight_status": preflight_status,
                "preflight_error": preflight_error,
                "error": preflight_error,
            },
            ensure_ascii=False,
        )
    except Exception as exc:
        return json.dumps(
            {
                "ok": False,
                "version": "",
                "supported": False,
                "base_category": "",
                "category": "",
                "subcategory": "",
                "class_name": "",
                "extractor": "",
                "title": "",
                "author": "",
                "thumbnail": "",
                "media_type": "",
                "estimated_count": None,
                "count_exact": False,
                "scanned_count": 0,
                "large_gallery": False,
                "cookies_loaded": False,
                "preflight_status": "extractor_error",
                "preflight_error": "%s: %s" % (type(exc).__name__, str(exc)),
                "error": "%s: %s" % (type(exc).__name__, str(exc)),
            },
            ensure_ascii=False,
        )


def download(
    url,
    output_dir,
    engine_dir,
    config_path="",
    cookies_path="",
    cache_path="",
):
    """Run one gallery-dl job and return a JSON result to Kotlin."""
    extractor_label = ""
    try:
        gallery_dl = _load_engine(engine_dir)
        from gallery_dl import config, extractor, job, util

        output_dir = os.path.abspath(output_dir)
        os.makedirs(output_dir, exist_ok=True)

        config_loaded, cookies_loaded = _prepare_config(
            config,
            util,
            output_dir,
            config_path,
            cookies_path,
            cache_path,
        )

        extr = extractor.find(url)
        info = _extractor_info(extr)
        extractor_label = info["extractor"]

        if not info["supported"]:
            return json.dumps(
                {
                    "ok": False,
                    "status": -2,
                    "version": str(gallery_dl.__version__),
                    "files": [],
                    "extractor": "",
                    "config_loaded": config_loaded,
                    "cookies_loaded": cookies_loaded,
                    "error": "No gallery-dl extractor matched this URL",
                },
                ensure_ascii=False,
            )

        status = int(job.DownloadJob(url).run() or 0)

        files = []
        for base, _, names in os.walk(output_dir):
            for name in names:
                path = os.path.join(base, name)
                if os.path.isfile(path) and not name.endswith((".part", ".tmp")):
                    files.append(os.path.abspath(path))
        files.sort()

        return json.dumps(
            {
                "ok": status == 0,
                "status": status,
                "version": str(gallery_dl.__version__),
                "files": files,
                "extractor": extractor_label,
                "config_loaded": config_loaded,
                "cookies_loaded": cookies_loaded,
                "error": "" if status == 0 else "gallery-dl finished with status %d" % status,
            },
            ensure_ascii=False,
        )
    except Exception as exc:
        return json.dumps(
            {
                "ok": False,
                "status": -1,
                "version": "",
                "files": [],
                "extractor": extractor_label,
                "config_loaded": False,
                "cookies_loaded": False,
                "error": "%s: %s" % (type(exc).__name__, str(exc)),
                "trace": traceback.format_exc(limit=8),
            },
            ensure_ascii=False,
        )
