#!/usr/bin/env python3
"""Draws White Widow from Purple Kush: the same plant, buds, seeds and spliff, recoloured to the pale garden.

    python3 textures-src/white_widow.py common/src/main/resources/assets/hempdustry/textures
    python3 textures-src/mildew.py common/src/main/resources/assets/hempdustry

White Widow is Purple Kush's body (two tall, the same harvest), so its art is Purple Kush's pixels
with three colour moves and nothing redrawn. Every hue is sampled from vanilla's pale garden:

* the green leaves turn the drained grey-green of pale oak leaves (#585D57 .. #A0A69C, hue ~100,
  saturation 0.06), each pixel keeping its place in Purple Kush's light-to-dark ramp;
* the violet bud bodies turn frost white, the trichomes the strain is named for -- the white of the
  open eyeblossom's petals (#E5DDE4 .. #FEFDFE), a touch greener so it sits with the leaves;
* the orange hairs turn resin orange, the creaking heart's resin clump (#E05504, #F3791B, #FAB326).

The plant is drawn untinted -- its colour is baked in, as pale oak leaves' is (they are not in
vanilla's BlockColors) -- since a biome multiply cannot make white out of grey in a green biome.

The stressed art is Purple Kush's bleached leaves, which would read as the same pale grey: there
they turn a dry straw instead, so a hermaphrodite still looks wrong at a glance. White Widow is
stressed a stage earlier than Purple Kush (a lost light or a leak from age 3), so it has a stressed
stage 3 Purple Kush lacks: the healthy stage 3 with its leaves turned the same straw.

The wild flower has two looks, as the eyeblossom does: closed, a young plant with its white buds
(stage 5); open at night, a flowering one with its orange hairs out (stage 7).

Re-run after any change to Purple Kush's art; never edit the output by hand.
"""
import colorsys
import os
import sys

from PIL import Image

STAGES = ["indica_crop_stage%d" % i for i in range(12)]
STRESSED = ["indica_crop_stage%d_stressed" % i for i in range(4, 12)]
# Stressed from age 3, a stage before Purple Kush: drawn from the healthy stage 3.
EARLY_STRESSED = "indica_crop_stage3"

LEAF_HUE = 100 / 360
LEAF_SAT = 0.07
FROST_HUE = 95 / 360
RESIN_HUE = 22 / 360
STRAW_HUE = 48 / 360


def _hsv(rgb):
    return colorsys.rgb_to_hsv(*(c / 255 for c in rgb))


def _rgb(h, s, v):
    r, g, b = colorsys.hsv_to_rgb(h, s, max(0.0, min(1.0, v)))
    return round(r * 255), round(g * 255), round(b * 255)


def _leaf(v):
    # Purple Kush's leaf ramp runs v 0.35 .. 0.55; pale oak's runs 0.36 .. 0.65. Stretched a little
    # brighter, so a field reads pale against the grass rather than dark.
    return _rgb(LEAF_HUE, LEAF_SAT, 0.40 + (v - 0.30) * 1.25)


def plant(rgb, stressed=False, wilted=False):
    h, s, v = _hsv(rgb)
    deg = h * 360
    if v < 0.14:
        return rgb  # outlines stay as drawn
    if s > 0.12 and (deg >= 250 or deg < 8):
        # violet bud bodies and wine-dark calyxes: frost white
        return _rgb(FROST_HUE, 0.03, 0.80 + v * 0.35)
    if s > 0.35 and 8 <= deg < 62:
        # orange and gold hairs, and the bud item's rust: resin orange, its dark end kept dark
        return _rgb(RESIN_HUE, min(0.97, s * 1.05 + 0.05), v * 1.02)
    if stressed and deg < 90 and v > 0.45:
        # the stressed art's bleached leaves: dry straw, not the healthy plant's pale grey
        return _rgb(STRAW_HUE, 0.30, v * 0.95)
    if s > 0.10 and 62 <= deg < 210:
        # the early stressed stage: its brighter leaves wilt to straw, as the later stressed art bleaches
        return _rgb(STRAW_HUE, 0.30, 0.40 + (v - 0.30) * 1.25) if wilted and v > 0.42 else _leaf(v)
    if stressed and s <= 0.12 and v > 0.45:
        return _rgb(STRAW_HUE, 0.28, v * 0.95)
    return rgb


def seed(rgb):
    """Purple Kush's seeds, their violet greyed to the pale garden's bark: still a seed, never white."""
    h, s, v = _hsv(rgb)
    if v < 0.14:
        return rgb
    return _rgb(30 / 360, min(s, 0.18) * 0.6, v * 1.05)


def ember(rgb):
    """A spliff: its lit tip resin orange, as Ender Haze's tip wears its own colour, and its paper --
    which every strain tints its own way, Purple Kush's lavender -- the frost white of the buds."""
    h, s, v = _hsv(rgb)
    deg = h * 360
    if s > 0.35 and (deg >= 300 or deg < 60):
        return _rgb(RESIN_HUE, min(0.97, s * 1.05), v)
    if s <= 0.35 and v >= 0.14:
        return _rgb(FROST_HUE, 0.05, v * 1.08 + 0.06)
    return rgb


def recolour(src, dst, how):
    image = Image.open(src).convert("RGBA")
    px = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = how((r, g, b)) + (a,)
    image.save(dst)


def main(textures):
    block = os.path.join(textures, "block")
    item = os.path.join(textures, "item")
    for name in STAGES:
        recolour(os.path.join(block, name + ".png"),
                 os.path.join(block, name.replace("indica_crop", "white_widow_crop") + ".png"), plant)
    for name in STRESSED:
        recolour(os.path.join(block, name + ".png"),
                 os.path.join(block, name.replace("indica_crop", "white_widow_crop") + ".png"),
                 lambda rgb: plant(rgb, stressed=True))
    recolour(os.path.join(block, EARLY_STRESSED + ".png"),
             os.path.join(block, "white_widow_crop_stage3_stressed.png"),
             lambda rgb: plant(rgb, stressed=True, wilted=True))
    recolour(os.path.join(block, "indica_crop_stage5.png"), os.path.join(block, "white_widow_flower.png"), plant)
    recolour(os.path.join(block, "indica_crop_stage7.png"), os.path.join(block, "white_widow_flower_open.png"), plant)
    recolour(os.path.join(item, "indica_buds.png"), os.path.join(item, "white_widow_buds.png"), plant)
    recolour(os.path.join(item, "indica_seeds.png"), os.path.join(item, "white_widow_seeds.png"), seed)
    recolour(os.path.join(item, "indica_spliff.png"), os.path.join(item, "white_widow_spliff.png"), ember)


if __name__ == "__main__":
    main(sys.argv[1])
