#!/usr/bin/env python3
"""Solve every problem from source, then write the bundle each platform ships.

Run after editing problems_source.py:  python3 tools/build_problems.py
"""
import json
from pathlib import Path

from problems_source import PROBLEMS

ROOT = Path(__file__).resolve().parent.parent
TARGETS = [
    ROOT / "shared/aime-problems.json",
    ROOT / "android/app/src/main/assets/aime-problems.json",
]
# Content scripts can't read packaged JSON without exposing it to web pages, so iOS gets a script.
IOS_TARGET = ROOT / "ios/SafariExtension/Resources/problems.js"


def main() -> None:
    bundle = []
    seen = set()
    for index, (topic, text, solve) in enumerate(PROBLEMS, start=1):
        if text in seen:
            raise ValueError(f"duplicate problem: {text}")
        seen.add(text)
        answer = solve()
        if not isinstance(answer, int) or not 0 <= answer <= 999:
            raise ValueError(f"{solve.__name__} gave {answer!r}, outside the AIME range 0-999")
        bundle.append({"id": index, "topic": topic, "text": text, "answer": answer})
    payload = json.dumps(bundle, indent=2) + "\n"
    for target in TARGETS:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(payload)
    IOS_TARGET.write_text(f"globalThis.SPEEDBUMP_PROBLEMS = {payload.rstrip()};\n")
    print(f"{len(bundle)} problems written")
    for problem in bundle:
        print(f"  #{problem['id']:>2} {problem['topic']:<14} answer={problem['answer']:03d}")


if __name__ == "__main__":
    main()
