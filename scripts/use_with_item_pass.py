#!/usr/bin/env python3
"""Check that no block's useItemOn returns the answer that skips useWithoutItem.

A right-click on a block is two calls. The server asks useItemOn with the held stack, and asks
useWithoutItem only when that answered PASS_TO_DEFAULT_BLOCK_INTERACTION
(ServerPlayerGameMode#useItemOn). On this line useItemOn returns ItemInteractionResult, and
SKIP_DEFAULT_BLOCK_INTERACTION is not that: it tells the game the block has nothing to do,
useWithoutItem is never asked, and a click with the wrong item in hand never opens the screen or
takes the batch out. BlockBehaviour#useItemOn's own default is PASS_TO_DEFAULT_BLOCK_INTERACTION,
so a refusal hands back super. (From 1.21.2 the method returns InteractionResult and the same trap
is a plain PASS.) It shipped once, in the Dry Sifter, and a player found it first: a game test's
useBlock is more forgiving than the real interaction.

Only the body of each useItemOn is read, with comments and strings stripped, so a PASS in
useWithoutItem next door, or one quoted in a comment, is not a finding.

Usage:
    python3 scripts/use_with_item_pass.py [checkout]

Exits non-zero and names each file and line.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path(__file__).resolve().parent.parent

# Comments and string literals, blanked out with their newlines kept, so line numbers still match.
NOISE = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"', re.S)
PASS = re.compile(r"\bItemInteractionResult\.SKIP_DEFAULT_BLOCK_INTERACTION\b")


def main():
    methods, findings = 0, []
    for path in sorted((ROOT / "src/main/java").rglob("*.java")):
        code = NOISE.sub(lambda m: re.sub(r"[^\n]", " ", m.group()), path.read_text(encoding="utf-8"))
        for match in re.finditer(r"\bItemInteractionResult\s+useItemOn\s*\(", code):
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
        print(f"SKIP_DEFAULT_BLOCK_INTERACTION from useItemOn  {line}")
    print(f"{methods} useItemOn override(s) read: {len(findings)} SKIP_DEFAULT_BLOCK_INTERACTION")
    if methods == 0:
        # From 1.21.2 it returns InteractionResult: after that bump, or a rename, this would check
        # nothing, so it says so rather than passing.
        sys.exit("No useItemOn returning ItemInteractionResult found, so nothing was checked.")
    sys.exit(1 if findings else 0)


if __name__ == "__main__":
    main()
