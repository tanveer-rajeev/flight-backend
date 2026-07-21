#!/usr/bin/env python3
"""Add UserDateTimeUtil import to files that reference it."""

from pathlib import Path

IMPORT = "import util.com.aerionsoft.application.UserDateTimeUtil;\n"

for path in Path("src/main/java").rglob("*.java"):
    text = path.read_text()
    if "UserDateTimeUtil" not in text:
        continue
    if "import util.com.aerionsoft.application.UserDateTimeUtil" in text:
        continue
  # Insert after package declaration
    lines = text.splitlines(keepends=True)
    for i, line in enumerate(lines):
        if line.startswith("package "):
            lines.insert(i + 1, "\n" + IMPORT)
            path.write_text("".join(lines))
            print(path)
            break
