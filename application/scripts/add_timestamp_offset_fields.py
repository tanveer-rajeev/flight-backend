#!/usr/bin/env python3
"""Add createdTimeOffset / updatedTimeOffset fields to JPA entities with timestamp columns."""

import re
from pathlib import Path

ENTITY_ROOT = Path("src/main/java/com/example/tufantrip/entity")

CREATED_PATTERNS = [
    r"(private LocalDateTime createdAt;)",
    r"(private LocalDateTime createAt;)",
    r"(private LocalDateTime submittedAt;)",
    r"(private LocalDateTime uploadedAt;)",
]
UPDATED_PATTERNS = [
    r"(private LocalDateTime updatedAt;)",
    r"(private LocalDateTime updateAt;)",
]

OFFSET_CREATED = (
    "\n\n    @Column(name = \"created_time_offset\", length = 32)\n"
    "    private String createdTimeOffset;"
)
OFFSET_UPDATED = (
    "\n\n    @Column(name = \"updated_time_offset\", length = 32)\n"
    "    private String updatedTimeOffset;"
)


def add_offset_fields(content: str) -> str:
    if "createdTimeOffset" not in content:
        for pattern in CREATED_PATTERNS:
            if re.search(pattern, content):
                content = re.sub(pattern, r"\1" + OFFSET_CREATED, content, count=1)
                break

    if "updatedTimeOffset" not in content:
        for pattern in UPDATED_PATTERNS:
            if re.search(pattern, content):
                content = re.sub(pattern, r"\1" + OFFSET_UPDATED, content, count=1)
                break

    return content


def main():
    for path in ENTITY_ROOT.rglob("*.java"):
        text = path.read_text()
        new_text = add_offset_fields(text)
        if new_text != text:
            path.write_text(new_text)
            print(f"Updated {path}")


if __name__ == "__main__":
    main()
