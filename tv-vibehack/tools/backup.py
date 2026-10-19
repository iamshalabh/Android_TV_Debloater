#!/usr/bin/env python3
"""One-shot backup of an Android TV before any change is made.

Connects once and writes the package list, disabled-package list, getprop output,
all three settings tables and the current HOME resolution into backup/<stamp>/.

    TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/backup.py
"""
import datetime
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tv import connect  # noqa: E402

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# name -> shell command run on the TV; output saved to backup/<stamp>/<name>.txt
ARTIFACTS = [
    ("getprop", "getprop"),
    ("packages_all", "pm list packages"),
    ("packages_disabled", "pm list packages -d"),
    ("packages_enabled", "pm list packages -e"),
    ("packages_system", "pm list packages -s"),
    ("packages_thirdparty", "pm list packages -3"),
    ("packages_user0", "pm list packages --user 0"),
    ("settings_global", "settings list global"),
    ("settings_system", "settings list system"),
    ("settings_secure", "settings list secure"),
    ("home_resolve", "cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"),
    ("home_activities", "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME"),
    ("wm_size", "wm size"),
    ("wm_density", "wm density"),
]


def main():
    stamp = datetime.datetime.now().strftime("%Y-%m-%d_%H%M%S")
    outdir = os.path.join(REPO, "backup", stamp)
    os.makedirs(outdir, exist_ok=True)

    dev = connect()
    info = {}
    for name, cmd in ARTIFACTS:
        try:
            out = dev.shell(cmd, read_timeout_s=180.0)
        except Exception as exc:  # keep going, note the failure
            out = ""
            info.setdefault("errors", {})[name] = repr(exc)
        with open(os.path.join(outdir, name + ".txt"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(out)
        print(f"{name:20s} {len(out.splitlines()):6d} lines")
    dev.close()

    with open(os.path.join(outdir, "meta.json"), "w", encoding="utf-8") as fh:
        json.dump({"stamp": stamp, "errors": info.get("errors", {})}, fh, indent=2)
    print(f"\nbackup written to {outdir}")


if __name__ == "__main__":
    main()
