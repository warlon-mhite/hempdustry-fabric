#!/usr/bin/env python3
"""Derive the stained- and tinted-glass bongs from the clear one.

    python3 textures-src/bong_glass.py [--preview out.png]

Not a drawing: a palette swap of textures/item/bong.png. Its four glass shades are replaced by
shades of vanilla's own stained-glass colour (the one flat colour every block/<c>_stained_glass.png
is painted in), and the water and the bowl are left exactly as drawn. So when bong.png is
redrawn, re-run this and the seventeen follow it -- which is why there is no .mctex per colour.
"""
import struct
import sys
import zlib
from pathlib import Path

ITEM = Path(__file__).resolve().parent.parent / "src/main/resources/assets/hempdustry/textures/item"

# The clear bong's glass, lightest to darkest: highlight, light, mid, outline.
HIGHLIGHT, LIGHT, MID, OUTLINE = 0xFFFFFF, 0xD4E5F7, 0xB3CFEC, 0x5D8FC2

# block/<c>_stained_glass.png, sampled from the 1.21.11 client jar. Tinted glass's darkest
# opaque texel stands in for its colour.
GLASS = {
    "white": 0xFFFFFF, "orange": 0xD87F33, "magenta": 0xB24CD8, "light_blue": 0x6699D8,
    "yellow": 0xE5E533, "lime": 0x7FCC19, "pink": 0xF27FA5, "gray": 0x4C4C4C,
    "light_gray": 0x999999, "cyan": 0x4C7F99, "purple": 0x7F3FB2, "blue": 0x334CB2,
    "brown": 0x664C33, "green": 0x667F33, "red": 0x993333, "black": 0x191919,
    "tinted": 0x35283B,
}


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def shades(color, name):
    base, white = rgb(color), (255, 255, 255)
    # Tinted glass is the one that should read dark; lifting it as far as the others makes it grey.
    lift = 0.5 if name == "tinted" else 1.0
    return {
        rgb(LIGHT): lerp(base, white, 0.4 * lift),
        rgb(MID): lerp(base, white, 0.2 * lift),
        rgb(OUTLINE): lerp(base, (0, 0, 0), 0.25),
    }


def read_png(path):
    data = path.read_bytes()
    i, idat = 8, b""
    while i < len(data):
        n, kind = struct.unpack(">I4s", data[i:i + 8])
        body = data[i + 8:i + 8 + n]
        if kind == b"IHDR":
            w, h, depth, ctype = struct.unpack(">IIBB", body[:10])
            assert (depth, ctype) == (8, 6), "expected 8-bit RGBA"
        elif kind == b"IDAT":
            idat += body
        i += 12 + n
    raw, stride, prev, rows, p = zlib.decompress(idat), w * 4, bytearray(w * 4), [], 0
    for _ in range(h):
        f, line = raw[p], bytearray(raw[p + 1:p + 1 + stride])
        p += 1 + stride
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0
            b, c = prev[x], (prev[x - 4] if x >= 4 else 0)
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                q = a + b - c
                pa, pb, pc = abs(q - a), abs(q - b), abs(q - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append([tuple(line[x * 4:x * 4 + 4]) for x in range(w)])
        prev = line
    return rows


def write_png(path, rows):
    raw = b"".join(b"\0" + bytes(v for px in row for v in px) for row in rows)
    def chunk(kind, body):
        return struct.pack(">I", len(body)) + kind + body + struct.pack(">I", zlib.crc32(kind + body))
    path.write_bytes(b"\x89PNG\r\n\x1a\n"
                     + chunk(b"IHDR", struct.pack(">IIBBBBB", len(rows[0]), len(rows), 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def main():
    clear = read_png(ITEM / "bong.png")
    out = {}
    for name, color in GLASS.items():
        swap = shades(color, name)
        rows = [[swap.get(px[:3], px[:3]) + (px[3],) for px in row] for row in clear]
        write_png(ITEM / f"{name}_bong.png", rows)
        out[name] = rows
    if "--preview" in sys.argv:
        # One row of 8x-upscaled sprites on a mid-grey ground, clear bong first.
        sheet, s = [clear] + list(out.values()), 8
        ground = (0x8B, 0x8B, 0x8B, 255)
        preview = []
        for y in range(16 * s):
            row = []
            for sprite in sheet:
                for x in range(16 * s):
                    px = sprite[y // s][x // s]
                    row.append(px if px[3] else ground)
            preview.append(row)
        write_png(Path(sys.argv[sys.argv.index("--preview") + 1]), preview)
    print(f"wrote {len(out)} bongs")


if __name__ == "__main__":
    main()
