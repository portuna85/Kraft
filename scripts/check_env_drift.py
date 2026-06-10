#!/usr/bin/env python3
"""
Verify that:
1. Every KRAFT_* placeholder referenced from application.yml is present in .env.example.
2. Every required variable (${VAR:?...} pattern) in docker-compose*.yml is present in .env.example.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path


def main() -> int:
    example = Path(".env.example").read_text(encoding="utf-8")
    errors: list[str] = []

    # --- 1. application.yml KRAFT_* placeholders ---
    yml = Path("src/main/resources/application.yml").read_text(encoding="utf-8")
    keys = re.findall(r"\$\{(KRAFT_[^:}]+)", yml)
    missing_yml = sorted({key for key in keys if key not in example})
    if missing_yml:
        errors.append("application.yml references missing from .env.example: " + ", ".join(missing_yml))

    # --- 2. docker-compose required vars (${VAR:?...} pattern) ---
    compose_required: set[str] = set()
    for compose_file in Path(".").glob("docker-compose*.yml"):
        text = compose_file.read_text(encoding="utf-8")
        compose_required.update(re.findall(r"\$\{([A-Z_][A-Z0-9_]*):\?", text))
    missing_compose = sorted(v for v in compose_required if v not in example)
    if missing_compose:
        errors.append("docker-compose requires vars missing from .env.example: " + ", ".join(missing_compose))

    if errors:
        for msg in errors:
            print("ERROR:", msg)
        return 1

    print("OK: all required keys are present in .env.example")
    return 0


if __name__ == "__main__":
    sys.exit(main())
