#!/usr/bin/env python3
"""Check that no class builds an ItemStack while it loads.

Since 26.1 an ItemStack can only exist once a world has bound the items' components; building one
earlier throws "Components not bound yet". A static field holding a stack is built when its class
loads, and for a client class that is the first resource reload, at the title screen: the Hemp Press
renderer did it, the reload stopped half-way, and every particle sprite after it was missing, so the
client fell out of its world at the first poof of smoke. A game test cannot see it -- the server never
loads a renderer -- so this reads the code. A stack kept for later is an ItemStackTemplate, made into a
stack where it is used.

Fields only: a static method that returns a stack runs when called, which is a choice the caller makes.

Usage:
    python3 scripts/static_stacks.py [checkout]

Exits non-zero and names each file and line.
"""
import pathlib
import re
import sys

import layout

ROOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parent.parent

# Comments and string literals, blanked out with their newlines kept, so line numbers still match.
NOISE = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"', re.S)
# A static field whose type is ItemStack, or a collection or array of them, given a value where it is
# declared.
FIELD = re.compile(r"\bstatic\b[^;(){}=\n]*?\b(?:ItemStack\b(?:\s*\[\s*\])?|\w+<[^;=(){}\n]*\bItemStack\b[^;=(){}\n]*>)\s+\w+\s*=")


def main():
    files, findings = 0, []
    for path in layout.of(ROOT).java_files():
        files += 1
        code = NOISE.sub(lambda m: re.sub(r"[^\n]", " ", m.group()), path.read_text(encoding="utf-8"))
        for match in FIELD.finditer(code):
            line = code.count("\n", 0, match.start()) + 1
            findings.append(f"{path.relative_to(ROOT)}:{line}: a static ItemStack field, built when the class loads")
    if findings:
        print("static item stacks:")
        for finding in findings:
            print("  " + finding)
        return 1
    print(f"static item stacks: none in {files} source files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
