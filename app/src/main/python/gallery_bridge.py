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


def inspect_url(
    url,
    engine_dir,
    config_path="",
    cookies_path="",
    cache_path="",
):
    """Resolve an input URL to an internal gallery-dl extractor without downloading files."""
    try:
        gallery_dl = _load_engine(engine_dir)
        from gallery_dl import config, extractor, util

        _prepare_config(
            config,
            util,
            "",
            config_path,
            cookies_path,
            cache_path,
        )

        extr = extractor.find(url)
        info = _extractor_info(extr)

        return json.dumps(
            {
                "ok": True,
                "version": str(gallery_dl.__version__),
                **info,
                "error": "" if info["supported"] else "No gallery-dl extractor matched this URL",
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
