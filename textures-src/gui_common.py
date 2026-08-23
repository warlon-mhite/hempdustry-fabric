#!/usr/bin/env python3
"""Vanilla container chrome, shared by both machine GUI generators.

Every colour and every shape in here was **measured out of the 1.21.1 client jar**, not guessed:
``assets/minecraft/textures/gui/container/furnace.png`` for the panel, the slots and the two unlit
indicators, and ``gui/sprites/container/furnace/{lit_progress,burn_progress}.png`` for the lit ones.
``check_panel.py`` diffs the generated panel back against furnace.png and must stay at zero
differences — that is what stops this drifting into "vanilla-ish".

The two things a hand-rolled container background always gets wrong, and which are the whole reason
this module exists:

* **The panel edge is four pixels, not one.** A 1px light/dark outline is the tell. Vanilla is a
  black outline, then a 2px white bevel on the top and left, a 2px #555555 bevel on the bottom and
  right, and the corners are *chamfered* over three pixels so the black steps diagonally.
* **The unlit indicators are filled #8B8B8B silhouettes, not outlines.** An outlined flame reads as
  a drawing of a flame; vanilla's reads as an unlit flame.
"""

import os
import struct
import zlib

PANEL_W, PANEL_H = 176, 166

# Vanilla's container palette, sampled from furnace.png.
BLACK = (0, 0, 0, 255)
BG = (198, 198, 198, 255)          # C6C6C6 panel fill
BEVEL_LIGHT = (255, 255, 255, 255)  # top/left bevel, and slot bottom/right
BEVEL_DARK = (85, 85, 85, 255)      # 555555 bottom/right panel bevel
SLOT_DARK = (55, 55, 55, 255)       # 373737 slot top/left
SLOT_BG = (139, 139, 139, 255)      # 8B8B8B slot interior, and every unlit indicator
CLEAR = (0, 0, 0, 0)

# gui/sprites/container/furnace/burn_progress.png draws its fill in white over a grey shadow.
FILL_LIGHT = (255, 255, 255, 255)
FILL_SHADOW = (104, 104, 104, 255)  # 686868

# The panel's four corners, transcribed pixel for pixel out of furnace.png. The straight runs
# between them are uniform, so these seven-pixel squares are the entire irregular part.
#   #  black outline   W  white bevel   5  #555555 bevel   -  panel fill   .  transparent
CORNER_TL = [
    "..#####",
    ".#WWWWW",
    "#WWWWWW",
    "#WWW---",
    "#WW----",
    "#WW----",
    "#WW----",
]
CORNER_TR = [
    "####...",
    "WWWW#..",
    "WWWW-#.",
    "----55#",
    "----55#",
    "----55#",
    "----55#",
]
CORNER_BL = [
    "#WW----",
    "#WW----",
    "#WW----",
    "#WW----",
    ".#-5555",
    "..#5555",
    "...####",
]
CORNER_BR = [
    "----55#",
    "----55#",
    "----55#",
    "---555#",
    "555555#",
    "55555#.",
    "#####..",
]
_CORNER_COLORS = {"#": BLACK, "W": BEVEL_LIGHT, "5": BEVEL_DARK, "-": BG, ".": CLEAR}

# furnace.png at (56,36): the unlit flame. A filled silhouette, which is the point.
FLAME_OFF = [
    "..............",
    "..#.........#.",
    "..#.........#.",
    "...#...#...#..",
    "...#...#...#..",
    "..##....#..##.",
    "..##....#..##.",
    ".###...##..###",
    ".##....##...##",
    ".##...###...##",
    ".###..##...###",
    "..##..##...##.",
    "..##..###..##.",
    ".###..###..###",
]
FLAME_OFF_COLORS = {"#": SLOT_BG, ".": BG}

# gui/sprites/container/furnace/lit_progress.png, colour for colour.
FLAME_ON = [
    ".r.........r..",
    ".or...r...rod.",
    "..o...o...o.d.",
    ".ryd..yr..yr..",
    ".oyd...o..yo..",
    ".yfd..rod.fyd.",
    "ryod..yod.oyr.",
    "ofrd.rfyd.ryod",
    "yfd..oyrd..fyd",
    "ffo..ofdd.ofyd",
    "rfyd.yfd..yfrd",
    ".ffd.yfy..ffd.",
    "ofod.offd.ofo.",
    ".ddd..ddd..ddd",
]
FLAME_ON_COLORS = {
    ".": BG,
    "r": (216, 76, 69, 255),    # D84C45
    "o": (255, 182, 0, 255),    # FFB600
    "y": (255, 255, 31, 255),   # FFFF1F
    "f": (255, 255, 255, 255),  # FFFFFF
    "d": SLOT_BG,               # 8B8B8B, the sprite's own shadow
}

# furnace.png at (79,34): the unlit arrow, and gui/sprites/container/furnace/burn_progress.png:
# the filled one. Both are copied verbatim and both are drawn at the *same* origin, which is where
# vanilla's one subtlety lives — the lit arrow's white sits one pixel higher than the unlit
# silhouette, so its #686868 shadow lands exactly on the unlit shape's bottom and right edge and the
# filled arrow reads as raised out of the panel. Nudging either one to "line them up" destroys that.
ARROW_W, ARROW_H = 24, 16
ARROW_OFF = [
    "........................",
    "...............#........",
    "...............##.......",
    "...............###......",
    "...............####.....",
    "...............#####....",
    "...............######...",
    ".#####################..",
    ".######################.",
    ".#####################..",
    "...............######...",
    "...............#####....",
    "...............####.....",
    "...............###......",
    "...............##.......",
    "...............#........",
]
ARROW_OFF_COLORS = {"#": SLOT_BG, ".": BG}
ARROW_ON = [
    "...............#........",
    "...............##.......",
    "...............###......",
    "...............####.....",
    "...............#####....",
    "...............######...",
    ".#####################..",
    ".######################.",
    ".#####################s.",
    ".ssssssssssssss######s..",
    "...............#####s...",
    "...............####s....",
    "...............###s.....",
    "...............##s......",
    "...............#s.......",
    "...............s........",
]
ARROW_ON_COLORS = {"#": FILL_LIGHT, "s": FILL_SHADOW, ".": BG}


class Sheet:
    """A 256x256 RGBA canvas — the size every container background has to be."""

    def __init__(self, size=256):
        self.size = size
        self.px = [[CLEAR for _ in range(size)] for _ in range(size)]

    def rect(self, x, y, w, h, color):
        for j in range(y, y + h):
            for i in range(x, x + w):
                if 0 <= i < self.size and 0 <= j < self.size:
                    self.px[j][i] = color

    def blit(self, rows, colors, ox, oy):
        for j, line in enumerate(rows):
            for i, ch in enumerate(line):
                color = colors.get(ch)
                if color is not None:
                    self.px[oy + j][ox + i] = color

    def panel(self):
        """The 176x166 background, byte-identical to vanilla's once the slots are stamped on."""
        self.rect(0, 0, PANEL_W, PANEL_H, BG)

        # Straight runs, drawn between the corners so the chamfers can be stamped over the top.
        edge, inner = 7, 7
        self.rect(inner, 0, PANEL_W - 2 * inner, 1, BLACK)
        self.rect(inner, 1, PANEL_W - 2 * inner, 2, BEVEL_LIGHT)
        self.rect(0, inner, 1, PANEL_H - 2 * inner, BLACK)
        self.rect(1, inner, 2, PANEL_H - 2 * inner, BEVEL_LIGHT)
        self.rect(inner, PANEL_H - 1, PANEL_W - 2 * inner, 1, BLACK)
        self.rect(inner, PANEL_H - 3, PANEL_W - 2 * inner, 2, BEVEL_DARK)
        self.rect(PANEL_W - 1, inner, 1, PANEL_H - 2 * inner, BLACK)
        self.rect(PANEL_W - 3, inner, 2, PANEL_H - 2 * inner, BEVEL_DARK)

        self.blit(CORNER_TL, _CORNER_COLORS, 0, 0)
        self.blit(CORNER_TR, _CORNER_COLORS, PANEL_W - edge, 0)
        self.blit(CORNER_BL, _CORNER_COLORS, 0, PANEL_H - edge)
        self.blit(CORNER_BR, _CORNER_COLORS, PANEL_W - edge, PANEL_H - edge)

    def slot(self, x, y):
        """One 18x18 slot, with vanilla's two 8B8B8B corner pixels where the bevels meet."""
        self.rect(x - 1, y - 1, 18, 18, SLOT_DARK)
        self.rect(x, y, 17, 17, BEVEL_LIGHT)
        self.rect(x, y, 16, 16, SLOT_BG)
        self.px[y - 1][x + 16] = SLOT_BG
        self.px[y + 16][x - 1] = SLOT_BG

    def big_slot(self, x, y):
        """A furnace-style result slot: a 24x24 well around the 16x16 slot at (x, y).

        Vanilla's own offset — its furnace output Slot is at (116,35) inside a well starting at
        (112,31) — so the slot sits four pixels in on every side.
        """
        self.well(x - 4, y - 4, 24, 24)

    def well(self, x, y, w, h):
        """A sunken track for a bar — a slot's bevel stretched to any size."""
        self.rect(x - 1, y - 1, w + 2, h + 2, SLOT_DARK)
        self.rect(x, y, w + 1, h + 1, BEVEL_LIGHT)
        self.rect(x, y, w, h, SLOT_BG)
        self.px[y - 1][x + w] = SLOT_BG
        self.px[y + h][x - 1] = SLOT_BG

    def player_inventory(self, top=84, hotbar=142):
        for row in range(3):
            for col in range(9):
                self.slot(8 + col * 18, top + row * 18)
        for col in range(9):
            self.slot(8 + col * 18, hotbar)

    def write(self, *path_parts):
        path = os.path.join(*path_parts)
        raw = bytearray()
        for row in self.px:
            raw.append(0)  # filter type 0
            for r, g, b, a in row:
                raw += bytes((r, g, b, a))

        def chunk(tag, data):
            body = tag + data
            return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

        png = b"\x89PNG\r\n\x1a\n"
        png += chunk(b"IHDR", struct.pack(">IIBBBBB", self.size, self.size, 8, 6, 0, 0, 0))
        png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        png += chunk(b"IEND", b"")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as handle:
            handle.write(png)
        print("wrote", os.path.normpath(path), f"({self.size}x{self.size})")


def resources(*parts):
    """A path under src/main/resources/assets/hempdustry/, relative to this file."""
    here = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(here, "..", "src", "main", "resources", "assets", "hempdustry", *parts)
