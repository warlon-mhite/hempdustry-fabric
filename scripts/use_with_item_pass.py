#!/usr/bin/env python3
"""Check that no block's onUseWithItem returns a plain ActionResult.PASS.

Since 1.21.2 a right-click on a block is two calls. The server asks onUseWithItem with the held
stack, and asks onUse only when that answered PASS_TO_DEFAULT_BLOCK_ACTION
(ServerPlayerInteractionManager#interactBlock). A plain PASS is not that: it tells the game the
block has nothing to do, onUse is never asked, and a click with the wrong item in hand never opens
the screen or takes the batch out. AbstractBlock#onUseWithItem's own default is
PASS_TO_DEFAULT_BLOCK_ACTION, so a refusal hands back super. It shipped once, in the Dry Sifter, and
a player found it first: a game test's useBlock is more forgiving than the real interaction.

Only the body of each onUseWithItem is read, with comments and strings stripped, so a PASS in onUse
next door, or one quoted in a comment, is not a finding.

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
PASS = re.compile(r"\bActionResult\.PASS\b")


def main():
    methods, findings = 0, []
    for path in sorted((ROOT / "src/main/java").rglob("*.java")):
        code = NOISE.sub(lambda m: re.sub(r"[^\n]", " ", m.group()), path.read_text(encoding="utf-8"))
        for match in re.finditer(r"\bActionResult\s+onUseWithItem\s*\(", code):
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
        print(f"PASS from onUseWithItem  {line}")
    print(f"{methods} onUseWithItem override(s) read: {len(findings)} plain PASS")
    if methods == 0:
        # Before 1.21.2 it returned ItemActionResult, a different API with a different trap: on that
        # line, or after a rename, this would check nothing, so it says so rather than passing.
        sys.exit("No onUseWithItem returning ActionResult found, so nothing was checked.")
    sys.exit(1 if findings else 0)


if __name__ == "__main__":
    main()
