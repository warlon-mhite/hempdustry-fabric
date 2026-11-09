#!/usr/bin/env python3
"""Draws Cherry Wine from Purple Kush: the same plant, wild flower, buds, seeds and spliff, recoloured.

    python3 textures-src/cherry_wine.py common/src/main/resources/assets/hempdustry/textures

Cherry Wine is Purple Kush's body (two tall, the same harvest), so its art is Purple Kush's pixels
with two colour moves and nothing redrawn: the violet of the flowering leaves and the buds turns a
pale, washed green (CBD flower bred from hemp), and the orange hairs turn pink, the geraniol pink of
its smoke. Every other pixel keeps its value, so a stage reads exactly like Purple Kush's stage.
Re-run after any change to Purple Kush's art; never edit the output by hand.
"""
import colorsys
import os
import sys

from PIL import Image

STAGES = ["indica_crop_stage%d" % i for i in range(12)]
STRESSED = ["indica_crop_stage%d_stressed" % i for i in range(4, 12)]


def move(rgb):
    r, g, b = (c / 255 for c in rgb)
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    deg = h * 360
    if s > 0.12 and (deg >= 250 or deg < 8):
        # violet and wine leaves and bud bodies: a pale, washed green, a little lighter
        h, s, v = 88 / 360, min(s, 0.55) * 0.75, min(1.0, v * 1.12 + 0.04)
    elif s > 0.35 and 12 <= deg < 60:
        # orange and gold hairs: pink, the geraniol of its smoke
        h, s, v = 338 / 360, min(0.62, s * 0.7), min(1.0, v * 1.04 + 0.06)
    r, g, b = colorsys.hsv_to_rgb(h, s, v)
    return round(r * 255), round(g * 255), round(b * 255)


def ember(rgb):
    """A spliff's lit tip, every warm pixel of it pink, as Ender Haze's tip wears its own colour."""
    r, g, b = (c / 255 for c in rgb)
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    if s > 0.35 and (h * 360 >= 300 or h * 360 < 60):
        h, s = 338 / 360, min(0.7, s * 0.75)
    r, g, b = colorsys.hsv_to_rgb(h, s, v)
    return round(r * 255), round(g * 255), round(b * 255)


def recolour(src, dst, how=move):
    image = Image.open(src).convert("RGBA")
    px = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = how((r, g, b)) + (a,)
    image.save(dst)


def main(textures):
    for name in STAGES + STRESSED:
        recolour(os.path.join(textures, "block", name + ".png"),
                 os.path.join(textures, "block", name.replace("indica_crop", "cherry_wine_crop") + ".png"))
    recolour(os.path.join(textures, "block", "indica_flower.png"), os.path.join(textures, "block", "cherry_wine_flower.png"))
    recolour(os.path.join(textures, "item", "indica_buds.png"), os.path.join(textures, "item", "cherry_wine_buds.png"))
    recolour(os.path.join(textures, "item", "indica_seeds.png"), os.path.join(textures, "item", "cherry_wine_seeds.png"))
    recolour(os.path.join(textures, "item", "indica_spliff.png"), os.path.join(textures, "item", "cherry_wine_spliff.png"), ember)


if __name__ == "__main__":
    main(sys.argv[1])
