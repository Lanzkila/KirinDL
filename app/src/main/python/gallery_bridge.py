"""Small Chaquopy bridge for KirinDownloader's optional gallery-dl engine.

The gallery-dl package itself is deliberately not bundled in the APK. The Android side
installs a verified pure-Python wheel from official PyPI into app-private storage only when
the user asks for it, then this module imports it from that directory.
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


def engine_version(engine_dir):
    try:
        gallery_dl = _load_engine(engine_dir)
        return str(gallery_dl.__version__)
    except Exception:
        return ""


def download(url, output_dir, engine_dir):
    """Run one gallery-dl job and return a JSON result to Kotlin."""
    try:
        gallery_dl = _load_engine(engine_dir)
        from gallery_dl import config, job

        output_dir = os.path.abspath(output_dir)
        os.makedirs(output_dir, exist_ok=True)

        # Start every Android job from a clean gallery-dl configuration so an earlier request
        # cannot leak settings into a later one.
        config.clear()
        config.set(("extractor",), "base-directory", output_dir)

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
                "error": "%s: %s" % (type(exc).__name__, str(exc)),
                "trace": traceback.format_exc(limit=8),
            },
            ensure_ascii=False,
        )
