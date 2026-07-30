#!/usr/bin/env python3
"""PLACEHOLDER generator for the Infuser's container GUI texture.

Same role a .mctex plays for the 16x16 art: keeps a 256x256 sheet editable and diffable instead of
being a binary nobody can change. Re-run after editing:

    python3 textures-src/infuser_gui.py

Output: src/main/resources/assets/hempdustry/textures/gui/container/infuser.png

Must stay in step with InfuserScreenHandler (slots) and InfuserScreen (sprite regions).

    [milk]     [hemp]                       the two hemp types stack separately
       *       [washed]   ====|===>  [out]  bar with a NOTCH at the early-pull minimum
    (flame)                                 heat indicator: reports the block BELOW

The notch is the whole point of the bar. A plain fill would say "cooking"; the notch says
"collectable from here, but not finished" -- which is the actual decision the player is making.
"""

import os
import struct
import zlib

W = H = 256
PANEL_W, PANEL_H = 176, 166

BG = (198, 198, 198, 255)
EDGE_LIGHT = (255, 255, 255, 255)
EDGE_DARK = (85, 85, 85, 255)
SLOT_BG = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)
TRACK_DARK = (85, 85, 85, 255)
TRACK_BG = (139, 139, 139, 255)
CLEAR = (0, 0, 0, 0)

# Slots — must match InfuserScreenHandler.
MILK = (26, 35)
HEMP = (62, 17)
WASHED = (62, 53)
OUTPUT = (134, 35)

# Live overlays — must match InfuserScreen.
BAR_XY, BAR_WH = (84, 39), (44, 5)
FLAME_XY = (26, 56)

# Sprite regions in the sheet margin.
BAR_AT = (176, 0)
NOTCH_AT = (176, 5)
FLAME_AT = (180, 5)

# 6000 / 18000 -- one third along.
NOTCH_FRACTION = 6000 / 18000

px = [[CLEAR for _ in range(W)] for _ in range(H)]


def rect(x, y, w, h, color):
    for j in range(y, y + h):
        for i in range(x, x + w):
            if 0 <= i < W and 0 <= j < H:
                px[j][i] = color


def blit(rows, colors, ox, oy):
    for j, line in enumerate(rows):
        for i, ch in enumerate(line):
            if ch != ".":
                px[oy + j][ox + i] = colors[ch]


def slot(x, y):
    rect(x - 1, y - 1, 18, 18, SLOT_DARK)
    rect(x, y, 17, 17, EDGE_LIGHT)
    rect(x, y, 16, 16, SLOT_BG)


# ---- panel ----
rect(0, 0, PANEL_W, PANEL_H, BG)
rect(0, 0, PANEL_W, 1, EDGE_LIGHT)
rect(0, 0, 1, PANEL_H, EDGE_LIGHT)
rect(0, PANEL_H - 1, PANEL_W, 1, EDGE_DARK)
rect(PANEL_W - 1, 0, 1, PANEL_H, EDGE_DARK)

for s in (MILK, HEMP, WASHED, OUTPUT):
    slot(*s)

for row in range(3):
    for col in range(9):
        slot(8 + col * 18, 84 + row * 18)
for col in range(9):
    slot(8 + col * 18, 142)

# ---- empty bar track, sunk into the panel ----
bx, by = BAR_XY
bw, bh = BAR_WH
rect(bx - 1, by - 1, bw + 2, bh + 2, TRACK_DARK)
rect(bx, by, bw, bh, TRACK_BG)

# The notch, drawn into the panel so it is visible even at zero progress -- the player should be
# able to see there is a "good enough" point and a "finished" point BEFORE anything has cooked.
notch_x = bx + round(NOTCH_FRACTION * bw)
rect(notch_x - 1, by - 3, 2, bh + 6, (60, 60, 60, 255))

# ---- unlit flame outline, so the lit sprite has something to replace ----
FLAME_OFF = [
    "......##......",
    ".....#..#.....",
    "....#....#....",
    "....#.....#...",
    "...#.......#..",
    "...#........#.",
    "..#.........#.",
    "..#.........#.",
    "..#.........#.",
    "..#.........#.",
    "...#.......#..",
    "....#.....#...",
    ".....#...#....",
    "......###.....",
]
blit(FLAME_OFF, {"#": (110, 110, 110, 255)}, *FLAME_XY)

# ---- sprites in the margin ----

# Filled bar: a warm gradient so a nearly-done batch reads as richer, echoing the block's own
# liquid tint deepening as it simmers.
bar_rows = []
for j in range(bh):
    row = ""
    for i in range(bw):
        row += "a" if j in (0, bh - 1) else "b"
    bar_rows.append(row)
blit(bar_rows, {"a": (150, 118, 46, 255), "b": (200, 163, 70, 255)}, *BAR_AT)

# The notch marker drawn over the fill, so it stays readable once the bar passes it.
NOTCH = ["##"] * 7
blit(NOTCH, {"#": (60, 60, 60, 255)}, *NOTCH_AT)

FLAME_ON = [
    "......##......",
    ".....#oo#.....",
    "....#ooyo#....",
    "....#oyyyo#...",
    "...#ooyYyo#...",
    "...#oyYYyoo#..",
    "..#ooyYYYyo#..",
    "..#oyYYYYyo#..",
    "..#oyYYYYyo#..",
    "..#ooyYYyoo#..",
    "...#ooyyoo#...",
    "....#oooo#....",
    ".....#oo#.....",
    "......##......",
]
blit(FLAME_ON, {
    "#": (90, 32, 6, 255),
    "o": (200, 84, 18, 255),
    "y": (240, 150, 40, 255),
    "Y": (255, 216, 110, 255),
}, *FLAME_AT)


def write_png(path):
    raw = bytearray()
    for row in px:
        raw.append(0)
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png)
    print("wrote", path, f"({W}x{H})")


if __name__ == "__main__":
    here = os.path.dirname(os.path.abspath(__file__))
    write_png(os.path.join(here, "..", "src", "main", "resources", "assets", "hempdustry",
                           "textures", "gui", "container", "infuser.png"))
