#!/usr/bin/env python3
"""Check that no block's useItemOn returns a plain InteractionResult.PASS.

Since 1.21.2 a right-click on a block is two calls. The server asks onUseWithItem with the held
stack, and asks useWithoutItem only when that answered TRY_WITH_EMPTY_HAND
(ServerPlayerGameMode#useItemOn). A plain PASS is not that: it tells the game the
block has nothing to do, useWithoutItem is never asked, and a click with the wrong item in hand never opens
the screen or takes the batch out. BlockBehaviour#useItemOn's own default is
TRY_WITH_EMPTY_HAND, so a refusal hands back super. It shipped once, in the Dry Sifter, and
a player found it first: a game test's useBlock is more forgiving than the real interaction.

Only the body of each useItemOn is read, with comments and strings stripped, so a PASS in
useWithoutItem next door, or one quoted in a comment, is not a finding.

Usage:
    python3 scripts/use_with_item_pass.py [checkout]

Exits non-zero and names each file and line.
"""
import pathlib
import re
import sys

import layout

ROOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parent.parent

# Comments and string literals, blanked out with their newlines kept, so line numbers still match.
NOISE = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"', re.S)
PASS = re.compile(r"\bInteractionResult\.PASS\b")


def main():
    methods, findings = 0, []
    for path in layout.of(ROOT).java_files():
        code = NOISE.sub(lambda m: re.sub(r"[^\n]", " ", m.group()), path.read_text(encoding="utf-8"))
        for match in re.finditer(r"\bInteractionResult\s+useItemOn\s*\(", code):
            body = code.index("{", match.end())
            depth, end = 0, body
            for end in range(body, len(code)):
                depth += {"{": 1, "}": -1}.get(code[end], 0)
                if depth == 0:
                    break
            methods += 1
            for hit in PASS.finditer(code, body, end):
                findings.append(f"{path.relative_to(ROOT)}:{code.count(chr(10), 0, hit.start()) + 1}")
    for line in findings:
        print(f"PASS from useItemOn  {line}")
    print(f"{methods} useItemOn override(s) read: {len(findings)} plain PASS")
    if methods == 0:
        # Before 1.21.2 it returned ItemInteractionResult, a different API with a different trap: on
        # that line, or after a rename, this would check nothing, so it says so rather than passing.
        # (It did once: Yarn named it onUseWithItem returning ActionResult.)
        sys.exit("No useItemOn returning InteractionResult found, so nothing was checked.")
    sys.exit(1 if findings else 0)


if __name__ == "__main__":
    main()
