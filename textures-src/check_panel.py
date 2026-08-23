#!/usr/bin/env python3
"""Diff the generated container chrome against vanilla's, pixel for pixel.

The whole claim of ``gui_common`` is that the panel edge, the slot bevel and the two unlit
indicators are *vanilla's*, not an approximation of them. This is what checks that claim, against
the real furnace.png in the client jar loom has already downloaded:

    python3 textures-src/check_panel.py

Exits non-zero on any difference. Skips (exit 0, with a message) if the jar isn't there, so it
never fails a machine that has not run a Gradle build yet.
"""

import glob
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gui_common as gui  # noqa: E402

JAR_GLOB = os.path.expanduser("~/.gradle/caches/fabric-loom/*/minecraft-client.jar")
FURNACE = "assets/minecraft/textures/gui/container/furnace.png"

# Where vanilla puts the two things we copy, inside furnace.png.
VANILLA_FLAME_OFF = (56, 36)
VANILLA_ARROW_OFF = (79, 34)
VANILLA_SLOT = (8, 84)       # the first player-inventory slot
VANILLA_BIG_SLOT = (116, 35)  # the furnace result slot, inside its 24x24 well


def read_png(data):
    """Minimal PNG reader — enough for vanilla's indexed and truecolour GUI sheets."""
    import struct
    import zlib

    pos, idat, plte, trns = 8, b"", None, None
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        tag, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + length]
        if tag == b"IHDR":
            width, height, depth, color_type = struct.unpack(">IIBB", body[:10])
        elif tag == b"PLTE":
            plte = body
        elif tag == b"tRNS":
            trns = body
        elif tag == b"IDAT":
            idat += body
        pos += 12 + length

    raw = zlib.decompress(idat)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
    bpp = max(1, channels * depth // 8)
    stride = (width * channels * depth + 7) // 8
    rows, prev, i = [], bytearray(stride), 0
    for _ in range(height):
        filt = raw[i]; i += 1
        line = bytearray(raw[i:i + stride]); i += stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b = prev[x]
            c = prev[x - bpp] if x >= bpp else 0
            if filt == 1:
                line[x] = (line[x] + a) & 255
            elif filt == 2:
                line[x] = (line[x] + b) & 255
            elif filt == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif filt == 4:
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        prev = line
        rows.append(bytes(line))

    def pixel(x, y):
        line = rows[y]
        if color_type == 3:
            if depth == 8:
                index = line[x]
            else:
                per = 8 // depth
                index = (line[x // per] >> (8 - depth * (x % per + 1))) & ((1 << depth) - 1)
            r, g, b = plte[index * 3:index * 3 + 3]
            return (r, g, b, trns[index] if trns and index < len(trns) else 255)
        chunk = line[x * bpp:(x + 1) * bpp]
        if color_type == 6:
            return tuple(chunk[:4])
        if color_type == 2:
            return (chunk[0], chunk[1], chunk[2], 255)
        if color_type == 0:
            return (chunk[0],) * 3 + (255,)
        return (chunk[0],) * 3 + (chunk[1],)

    return width, height, pixel


def compare(name, ours, theirs, coords):
    bad = [(x, y, ours(x, y), theirs(x, y)) for x, y in coords if ours(x, y) != theirs(x, y)]
    if bad:
        print(f"FAIL {name}: {len(bad)} pixel(s) differ from vanilla")
        for x, y, got, want in bad[:8]:
            print(f"       ({x:>3},{y:>3})  ours {got}  vanilla {want}")
    else:
        print(f"ok   {name}: {len(coords)} pixels identical to vanilla")
    return not bad


def main():
    jars = sorted(glob.glob(JAR_GLOB))
    if not jars:
        print("skip: no minecraft-client.jar in the loom cache — run ./gradlew build first")
        return 0
    with zipfile.ZipFile(jars[-1]) as jar:
        _, _, vanilla = read_png(jar.read(FURNACE))

    sheet = gui.Sheet()
    sheet.panel()
    sheet.slot(*VANILLA_SLOT)
    sheet.big_slot(*VANILLA_BIG_SLOT)
    sheet.blit(gui.FLAME_OFF, gui.FLAME_OFF_COLORS, *VANILLA_FLAME_OFF)
    sheet.blit(gui.ARROW_OFF, gui.ARROW_OFF_COLORS, *VANILLA_ARROW_OFF)
    ours = lambda x, y: sheet.px[y][x]  # noqa: E731

    # The outer 7px ring of the panel: outline, both bevels and all four chamfered corners.
    ring = [(x, y) for y in range(gui.PANEL_H) for x in range(gui.PANEL_W)
            if x < 7 or y < 7 or x >= gui.PANEL_W - 7 or y >= gui.PANEL_H - 7]
    slot_box = [(VANILLA_SLOT[0] - 1 + i, VANILLA_SLOT[1] - 1 + j)
                for j in range(18) for i in range(18)]
    big_box = [(VANILLA_BIG_SLOT[0] - 4 + i, VANILLA_BIG_SLOT[1] - 4 + j)
               for j in range(26) for i in range(26)]
    flame_box = [(VANILLA_FLAME_OFF[0] + i, VANILLA_FLAME_OFF[1] + j)
                 for j in range(14) for i in range(14)]
    arrow_box = [(VANILLA_ARROW_OFF[0] + i, VANILLA_ARROW_OFF[1] + j)
                 for j in range(gui.ARROW_H) for i in range(gui.ARROW_W)]

    passed = all([
        compare("panel border", ours, vanilla, ring),
        compare("slot bevel", ours, vanilla, slot_box),
        compare("result-slot well", ours, vanilla, big_box),
        compare("unlit flame", ours, vanilla, flame_box),
        compare("unlit arrow", ours, vanilla, arrow_box),
    ])
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
