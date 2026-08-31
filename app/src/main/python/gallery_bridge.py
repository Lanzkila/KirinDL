"""Chaquopy bridge for KirinDownloader's optional Codeberg gallery-dl engine.

The gallery-dl source package is installed on demand from the official Codeberg repository into
app-private storage. This bridge keeps Android-specific paths and compatibility settings separate
from the upstream extractor package.
"""

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
            # Validate with gallery-dl's own JSON loader first so malformed settings fail with a
            # normal error instead of terminating the embedded Python interpreter.
            with open(config_path, encoding="utf-8") as fp:
                util.json_loads(fp.read())
            config.load((config_path,), strict=False)
            config_loaded = True

    cookies_loaded = False
    if cookies_path:
        cookies_path = os.path.abspath(cookies_path)
        if os.path.isfile(cookies_path) and os.path.getsize(cookies_path) > 0:
            # gallery-dl natively accepts a Mozilla/Netscape cookies.txt path.
            config.set(("extractor",), "cookies", cookies_path)
            cookies_loaded = True

    if cache_path:
        cache_path = os.path.abspath(cache_path)
        os.makedirs(os.path.dirname(cache_path), exist_ok=True)
        # Keep extractor sessions/cache across jobs and across Codeberg engine updates.
        config.set(("cache",), "file", cache_path)

    # User config is loaded first. These settings are deliberately applied last so a config file
    # cannot redirect Android output outside the job sandbox or disable TLS verification.
    config.set(("extractor",), "base-directory", output_dir)
    config.set(("extractor",), "input", False)
    config.set(("extractor",), "verify", True)
    config.set(("downloader",), "verify", True)

    return config_loaded, cookies_loaded


def engine_version(engine_dir):
    try:
        gallery_dl = _load_engine(engine_dir)
        return str(gallery_dl.__version__)
    except Exception:
        return ""


def download(
    url,
    output_dir,
    engine_dir,
    config_path="",
    cookies_path="",
    cache_path="",
):
    """Run one gallery-dl job and return a JSON result to Kotlin."""
    try:
        gallery_dl = _load_engine(engine_dir)
        from gallery_dl import config, job, util

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
                "config_loaded": False,
                "cookies_loaded": False,
                "error": "%s: %s" % (type(exc).__name__, str(exc)),
                "trace": traceback.format_exc(limit=8),
            },
            ensure_ascii=False,
        )
