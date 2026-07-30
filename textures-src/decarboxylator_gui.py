#!/usr/bin/env python3
"""PLACEHOLDER generator for the Decarboxylator's container GUI texture.

A 256x256 sheet is far too big to hand-author as an .mctex character matrix (that would be 256
rows of 256 characters), so this script plays the same role for it: the art stays re-editable
and diffable instead of being a binary nobody can change. Re-run it after editing:

    python3 textures-src/decarboxylator_gui.py

Output: src/main/resources/assets/hempdustry/textures/gui/container/decarboxylator.png

Layout must stay in step with DecarboxylatorScreenHandler (slot positions) and
DecarboxylatorScreen (sprite regions), which hold the same numbers as constants.

    [T1][T2][T3]      three trays, centred on the panel (span 61..115, midpoint 88 = 176/2)
     |   |   |        one arrow per tray, each filling on its own timer
       [OUT]          collection slot, centred directly under the trays
  [FUEL]              fuel + flame off to the left, arranged as a furnace does

The empty-slot icons (a flame in the fuel slot, a leaf in each tray) are NOT drawn here — they
are real sprites on the block atlas, served by Slot#getBackgroundSprite, so they disappear the
moment a slot is filled. See empty_slot_fuel / empty_slot_hemp in decarboxylator.mctex.
"""

import os
import struct
import zlib

W = H = 256
PANEL_W, PANEL_H = 176, 166

# Vanilla container palette.
BG = (198, 198, 198, 255)
EDGE_LIGHT = (255, 255, 255, 255)
EDGE_DARK = (85, 85, 85, 255)
SLOT_BG = (139, 139, 139, 255)
SLOT_DARK = (55, 55, 55, 255)
CLEAR = (0, 0, 0, 0)

# Slot origins — must match DecarboxylatorScreenHandler.
TRAYS = [(62, 17), (80, 17), (98, 17)]
OUTPUT = (80, 54)
FUEL = (26, 54)

# Where the screen draws the live overlays.
FLAME_XY = (26, 36)
ARROW_Y = 36

# Sprite regions read by DecarboxylatorScreen.
FLAME_AT, FLAME_SIZE = (176, 0), (14, 14)
ARROW_AT, ARROW_SIZE = (176, 14), (16, 16)

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
    """A vanilla 16x16 slot: dark bevel top/left, white bevel bottom/right."""
    rect(x - 1, y - 1, 18, 18, SLOT_DARK)
    rect(x, y, 17, 17, EDGE_LIGHT)
    rect(x, y, 16, 16, SLOT_BG)


# ---- panel ----
rect(0, 0, PANEL_W, PANEL_H, BG)
rect(0, 0, PANEL_W, 1, EDGE_LIGHT)
rect(0, 0, 1, PANEL_H, EDGE_LIGHT)
rect(0, PANEL_H - 1, PANEL_W, 1, EDGE_DARK)
rect(PANEL_W - 1, 0, 1, PANEL_H, EDGE_DARK)

# ---- machine slots ----
slot(*FUEL)
for tray in TRAYS:
    slot(*tray)
slot(*OUTPUT)

# ---- player inventory + hotbar ----
for row in range(3):
    for col in range(9):
        slot(8 + col * 18, 84 + row * 18)
for col in range(9):
    slot(8 + col * 18, 142)

# ---- unlit backdrops, so the live overlays have something to reveal against ----
# Furnace-style: the dark flame outline and the empty arrow track are part of the panel; the
# screen then draws the lit flame and the filled arrow on top.
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

ARROW_OFF = [
    "................",
    ".....######.....",
    ".....#....#.....",
    ".....#....#.....",
    ".....#....#.....",
    ".....#....#.....",
    ".....#....#.....",
    "...###....###...",
    "...#........#...",
    "....#......#....",
    ".....#....#.....",
    "......#..#......",
    ".......##.......",
    "................",
    "................",
    "................",
]
for tray_x, _ in TRAYS:
    blit(ARROW_OFF, {"#": (110, 110, 110, 255)}, tray_x, ARROW_Y)

# ---- lit flame sprite (revealed bottom-up as fuel burns) ----
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

# ---- filled arrow sprite (revealed top-down as a tray cooks) ----
ARROW_ON = [
    "................",
    ".....######.....",
    ".....#dddd#.....",
    ".....#dddd#.....",
    ".....#dddd#.....",
    ".....#dddd#.....",
    ".....#dddd#.....",
    "...##########...",
    "...#dddddddd#...",
    "....#dddddd#....",
    ".....#dddd#.....",
    "......#dd#......",
    ".......##.......",
    "................",
    "................",
    "................",
]
blit(ARROW_ON, {"#": (60, 60, 60, 255), "d": (150, 200, 90, 255)}, *ARROW_AT)


def write_png(path):
    raw = bytearray()
    for row in px:
        raw.append(0)  # filter type 0
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
                           "textures", "gui", "container", "decarboxylator.png"))
