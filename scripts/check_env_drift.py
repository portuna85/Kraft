#!/usr/bin/env python3
"""
Verify that every KRAFT_* placeholder referenced from application.yml
is present in .env.example.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path


def main() -> int:
    yml = Path("src/main/resources/application.yml").read_text(encoding="utf-8")
    example = Path(".env.example").read_text(encoding="utf-8")

    keys = re.findall(r"\$\{(KRAFT_[^:}]+)", yml)
    missing = sorted({key for key in keys if key not in example})

    if missing:
        print("ERROR: Missing in .env.example:", ", ".join(missing))
        return 1

    print("OK: all KRAFT_* keys are present in .env.example")
    return 0


if __name__ == "__main__":
    sys.exit(main())
