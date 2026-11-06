#!/usr/bin/env python3
"""Writes textures-src/ender_haze.mctex: Ender Haze's crop, its buds, seeds and spliff, and the Averted
Gaze icon.

    python3 textures-src/ender_haze.py common/src/main/resources/assets/hempdustry/textures \
      textures-src/ender_haze.mctex
    python3 mctex.py render \
      textures-src/ender_haze.mctex -o common/src/main/resources/assets/hempdustry/textures

PLACEHOLDER. The crop is Lemon Haze's own drawings (Warlon Mhite's), recoloured, not redrawn: the
foliage goes to the chorus plant's purples with the chorus flower's lilacs for the pale fronds, the
orange pistils to a ramp anchored on E580FF -- the End's declared sky light colour -- for the bulb,
and the stressed plant's browns to a grey mauve. Ender Haze is five tall where Lemon Haze is three, so
a middle segment repeats, every other one mirrored. Untinted: the models parent
hempdustry:block/crop_cross. Re-run this script, never edit the .mctex by hand, and re-derive if
Lemon Haze's art is ever redrawn.
"""
import os
import sys

from PIL import Image

FOLIAGE = ['3A143A', '472147', '5B345B', '7C5B7C', '8F718F', 'AB8AAB', 'D6BBD6']
BULB = ['7A3A8F', '9A4FB0', 'B964D0', 'D27AEA', 'E580FF', 'F0B8FF', 'FBF3FE']
SPENT = ['4A3A4A', '634561', '7C6A7C', '998A99', 'C8BCC8']
CHARS = '123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ'

# (output name, Lemon Haze source, mirrored)
CROP = [
    ('stage0', 'stage0', False), ('stage1', 'stage1', False), ('stage2', 'stage2', False),
    ('stage3_bottom', 'stage4_bottom', False), ('stage3_top', 'stage4_top', False),
]
for out_stage, src_stage in (('stage4', 'stage6'), ('stage7', 'stage7')):
    for suffix in ('', '_stressed'):
        CROP += [
            (f'{out_stage}_bottom{suffix}', f'{src_stage}_bottom{suffix}', False),
            (f'{out_stage}_middle{suffix}', f'{src_stage}_middle{suffix}', False),
            (f'{out_stage}_middle_mirrored{suffix}', f'{src_stage}_middle{suffix}', True),
            (f'{out_stage}_top{suffix}', f'{src_stage}_top{suffix}', False),
        ]
ITEMS = [('ender_haze_buds', 'sativa_buds'), ('ender_haze_seeds', 'sativa_seeds'),
         ('ender_haze_spliff', 'sativa_spliff')]

# Averted Gaze: an enderman's eye, looking away -- the pupil pushed into the corner.
ICON = [
    "..................",
    "..................",
    "..................",
    "..................",
    "......aaaaaa......",
    "....aabbbbbbaa....",
    "...abbbbbbbcdda...",
    "..abbbbbbbbcdeea..",
    "..abbbbbbbbcdeea..",
    "...abbbbbbbcdda...",
    "....aabbbbbbaa....",
    "......aaaaaa......",
    "..................",
    "..................",
    "..................",
    "..................",
    "..................",
    "..................",
]
ICON_PALETTE = {'a': '2B0F2B', 'b': 'E0C0F0', 'c': 'CC00FA', 'd': 'E580FF', 'e': '1A061A'}


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def kind(c):
    r, g, b = c
    if min(c) > 230:
        return 'white'
    if g >= r and g >= b:
        return 'foliage'
    if r > g > b and r - b > 80:
        return 'bulb'
    return 'spent'


def load(path):
    return Image.open(path).convert('RGBA')


def main(textures, out):
    sources = {name: load(os.path.join(textures, 'block', f'sativa_crop_{src}.png'))
               for _, src, _ in CROP for name in [src]}
    items = {src: load(os.path.join(textures, 'item', f'{src}.png')) for _, src in ITEMS}
    ranges = {}
    for im in list(sources.values()) + list(items.values()):
        for px in im.getdata():
            if px[3]:
                k = kind(px[:3])
                lo, hi = ranges.get(k, (255.0, 0.0))
                ranges[k] = (min(lo, lum(px[:3])), max(hi, lum(px[:3])))

    def remap(c):
        k = kind(c)
        if k == 'white':
            return BULB[-1]
        ramp = {'foliage': FOLIAGE, 'bulb': BULB, 'spent': SPENT}[k]
        lo, hi = ranges[k]
        t = 0.0 if hi <= lo else (lum(c) - lo) / (hi - lo)
        return ramp[round(t * (len(ramp) - 1))]

    doc = __doc__.strip().splitlines()
    start = next(i for i, line in enumerate(doc) if line.startswith("PLACEHOLDER"))
    lines = ["# " + line if line else "#" for line in doc[start:]]
    lines.append('')

    def emit(name, path, im, mirror, kind_attr):
        if mirror:
            im = im.transpose(Image.FLIP_LEFT_RIGHT)
        w, h = im.size
        palette = {}
        rows = []
        for y in range(h):
            row = ''
            for x in range(w):
                px = im.getpixel((x, y))
                if not px[3]:
                    row += '.'
                    continue
                colour = remap(px[:3])
                if colour not in palette:
                    palette[colour] = CHARS[len(palette)]
                row += palette[colour]
            rows.append(row)
        lines.append(f'@palette {name}')
        lines.append('. = none')
        for colour, ch in palette.items():
            lines.append(f'{ch} = {colour}')
        lines.append('')
        lines.append(f'@texture {path} {w}x{h} palette={name} type={kind_attr}')
        lines.extend(rows)
        lines.append('')

    for name, src, mirror in CROP:
        emit(f'ender_haze_crop_{name}', f'block/ender_haze_crop_{name}', sources[src], mirror, 'plant')
    for name, src in ITEMS:
        emit(name, f'item/{name}', items[src], False, 'item')

    lines.append('@palette averted_gaze')
    lines.append('. = none')
    for ch, colour in ICON_PALETTE.items():
        lines.append(f'{ch} = {colour}')
    lines.append('')
    lines.append('@texture mob_effect/averted_gaze 18x18 palette=averted_gaze type=item')
    lines.extend(ICON)
    lines.append('')

    with open(out, 'w') as f:
        f.write('\n'.join(lines))


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
