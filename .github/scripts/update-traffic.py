#!/usr/bin/env python3
"""Collect GitHub's daily repository page-view stats and persist them in-repo."""

from __future__ import annotations

import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = ROOT / ".github" / "data"
HISTORY_FILE = DATA_DIR / "traffic-history.json"
VIEWS_BADGE_FILE = DATA_DIR / "repo-views.json"
LAST_VISIT_BADGE_FILE = DATA_DIR / "last-visited.json"


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def read_json(path: Path, default: dict) -> dict:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def get_daily_views(repository: str, token: str) -> list[dict]:
    url = f"https://api.github.com/repos/{repository}/traffic/views?per=day"
    request = Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "KirinDL-repository-traffic-sync",
        },
    )
    try:
        with urlopen(request, timeout=30) as response:
            payload = json.load(response)
    except HTTPError as error:
        if error.code in (401, 403):
            raise SystemExit(
                "GitHub rejected TRAFFIC_TOKEN. Use a fine-grained token for "
                "this repository with Administration: read permission."
            ) from error
        raise SystemExit(f"GitHub traffic API returned HTTP {error.code}.") from error
    except (URLError, TimeoutError) as error:
        raise SystemExit(f"Could not reach GitHub traffic API: {error}") from error

    if not isinstance(payload, dict) or not isinstance(payload.get("views"), list):
        raise SystemExit("GitHub returned an unexpected traffic response.")
    return payload["views"]


def main() -> int:
    repository = os.environ.get("GITHUB_REPOSITORY", "").strip()
    token = os.environ.get("TRAFFIC_TOKEN", "").strip()
    if not repository or "/" not in repository:
        raise SystemExit("GITHUB_REPOSITORY is missing or invalid.")
    if not token:
        raise SystemExit("TRAFFIC_TOKEN is required for repository traffic access.")

    now = utc_now()
    now_text = now.isoformat(timespec="seconds").replace("+00:00", "Z")
    incoming = get_daily_views(repository, token)

    history = read_json(
        HISTORY_FILE,
        {
            "schema_version": 1,
            "source": "GitHub repository traffic API",
            "first_sync_at": None,
            "last_updated": None,
            "last_visited": None,
            "total_views": 0,
            "total_daily_unique_views": 0,
            "daily": [],
        },
    )

    daily_by_date: dict[str, dict] = {}
    for item in history.get("daily", []):
        date = item.get("date")
        if isinstance(date, str):
            daily_by_date[date] = item

    for item in incoming:
        timestamp = item.get("timestamp")
        if not timestamp:
            continue
        try:
            day = datetime.fromisoformat(timestamp.replace("Z", "+00:00")).date().isoformat()
        except (AttributeError, ValueError):
            continue
        daily_by_date[day] = {
            "date": day,
            "views": max(0, int(item.get("count", 0))),
            "unique_views": max(0, int(item.get("uniques", 0))),
        }

    daily = [daily_by_date[key] for key in sorted(daily_by_date)]
    total_views = sum(item["views"] for item in daily)
    total_daily_unique_views = sum(item["unique_views"] for item in daily)
    visit_days = [item["date"] for item in daily if item["views"] > 0]
    last_visited = max(visit_days) if visit_days else history.get("last_visited")

    history.update(
        {
            "schema_version": 1,
            "source": "GitHub repository traffic API",
            "first_sync_at": history.get("first_sync_at") or now_text,
            "last_updated": now_text,
            "last_visited": last_visited,
            "total_views": total_views,
            "total_daily_unique_views": total_daily_unique_views,
            "daily": daily,
            "note": (
                "GitHub supplies a rolling 14-day window. The workflow preserves "
                "daily snapshots here after each successful sync. Daily unique views "
                "can include the same person on multiple days."
            ),
        }
    )
    write_json(HISTORY_FILE, history)

    write_json(
        VIEWS_BADGE_FILE,
        {
            "schemaVersion": 1,
            "label": "Repo views",
            "message": f"{total_views:,}",
            "color": "blue",
        },
    )

    last_visit_label = (
        datetime.strptime(last_visited, "%Y-%m-%d").strftime("%d %b %Y") + " UTC"
        if last_visited
        else "No visits yet"
    )
    write_json(
        LAST_VISIT_BADGE_FILE,
        {
            "schemaVersion": 1,
            "label": "Last visited",
            "message": last_visit_label,
            "color": "green" if last_visited else "lightgrey",
        },
    )

    print(
        f"Saved {len(daily)} daily records; "
        f"{total_views:,} tracked views; last visited: {last_visit_label}."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
