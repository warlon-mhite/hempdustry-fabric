#!/usr/bin/env python3
"""Draws Dud Weed from Lemon Haze: the fibre family's lanky outline on Purple Kush's two-tall body.

    python3 textures-src/dud_weed.py common/src/main/resources/assets/hempdustry/textures
    python3 textures-src/mildew.py common/src/main/resources/assets/hempdustry

Dud Weed grows on Purple Kush's crop (two segments, the indica stage layout: stages 0-7 the lower
half by age, 8-11 the upper half of ages 4-7) but looks like the fibre family, Lemon Haze and
Industrial Hemp: narrow leaves, long internodes, sparse. So its art is Lemon Haze's pixels laid onto
that layout, and nothing is redrawn:

    ages 0-3   Lemon Haze's one-block stages
    ages 4-5   Lemon Haze's bottom under its top (it is two tall then too)
    ages 6-7   Lemon Haze's middle under its full age-7 cola: the three-tall plant without its bare
               stalk. Lemon Haze's age-6 top is a stub on its third block, which on two blocks would
               make age 6 shorter than age 5; so both take the age-7 top, and the lower half ripens

then recoloured onto Industrial Hemp's palette, the crop it is a sport of: every green turns the fibre
crop's hop yellow-green (hue 93, its saturation), the pale trichome highlights its ripe tops' cream,
and the orange hairs its tops' khaki, so the buds read pale with few hairs. Values are kept, so a
stage reads exactly like Lemon Haze's stage. The buds, seeds and spliff are recoloured the same way
(the seeds from Industrial Hemp's, greened a little; the spliff's ember goes out to khaki, a dud).
Re-run after any change to Lemon Haze's or Industrial Hemp's art; never edit the output by hand.
"""
import colorsys
import os
import sys

from PIL import Image

# (Dud Weed texture, Lemon Haze texture) for the twelve stages, then the stressed ones from age 4.
STAGES = [("dud_weed_crop_stage%d" % age, "sativa_crop_stage%d" % age) for age in range(4)] + [
    ("dud_weed_crop_stage4", "sativa_crop_stage4_bottom"),
    ("dud_weed_crop_stage5", "sativa_crop_stage5_bottom"),
    ("dud_weed_crop_stage6", "sativa_crop_stage6_middle"),
    ("dud_weed_crop_stage7", "sativa_crop_stage7_middle"),
    ("dud_weed_crop_stage8", "sativa_crop_stage4_top"),
    ("dud_weed_crop_stage9", "sativa_crop_stage5_top"),
    ("dud_weed_crop_stage10", "sativa_crop_stage7_top"),
    ("dud_weed_crop_stage11", "sativa_crop_stage7_top"),
]
STRESSED = [(dud + "_stressed", haze + "_stressed") for dud, haze in STAGES[4:]]

# Industrial Hemp's own colours (industrial_hemp_crop_stage7_top): leaf #6FA24F / #93C06A, hue 91-96
# at saturation 0.45-0.52; ripe tops #D8CF9A (cream, hue 51, s 0.29) and #B9B764 (khaki, hue 58, s 0.46).
LEAF_HUE, LEAF_SAT = 93 / 360, 0.50
CREAM_HUE, CREAM_SAT = 51 / 360, 0.29
KHAKI_HUE, KHAKI_SAT = 58 / 360, 0.46


def hsv(rgb):
    return colorsys.rgb_to_hsv(*(c / 255 for c in rgb))


def rgb(h, s, v):
    return tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, s, v))


def move(colour):
    h, s, v = hsv(colour)
    deg = h * 360
    if s < 0.22 and v > 0.85:
        # the white and mint highlights on Lemon Haze's colas: the fibre crop's cream tops
        return rgb(CREAM_HUE, CREAM_SAT, v * 0.92)
    if s > 0.35 and (deg < 45 or deg >= 345):
        # orange, rust and brown hairs: khaki, so the buds read pale with few hairs
        return rgb(KHAKI_HUE, KHAKI_SAT, v * 0.8)
    if 70 <= deg < 175:
        # every green and sage: the fibre crop's hop green, as saturated as it is (the pale sages a
        # little less, so the leaf keeps its two tones)
        return rgb(LEAF_HUE, LEAF_SAT if s > 0.3 else LEAF_SAT * 0.6, v)
    return colour


def seed(colour):
    """Industrial Hemp's seed in its khaki tops' colour: the same seed, another plant, told apart in a slot."""
    h, s, v = hsv(colour)
    return rgb(KHAKI_HUE, min(0.6, s * 1.7), min(1.0, v * 1.05))


def ember(colour):
    """The spliff's lit tip gone out: every warm pixel of it khaki, a damp squib."""
    h, s, v = hsv(colour)
    deg = h * 360
    if s > 0.35 and (deg >= 300 or deg < 60):
        return rgb(KHAKI_HUE, KHAKI_SAT * 0.8, v * 0.8)
    return colour


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
    block = os.path.join(textures, "block")
    item = os.path.join(textures, "item")
    for dud, haze in STAGES + STRESSED:
        recolour(os.path.join(block, haze + ".png"), os.path.join(block, dud + ".png"))
    recolour(os.path.join(item, "sativa_buds.png"), os.path.join(item, "dud_weed_buds.png"))
    recolour(os.path.join(item, "industrial_hemp_seeds.png"), os.path.join(item, "dud_weed_seeds.png"), seed)
    recolour(os.path.join(item, "sativa_spliff.png"), os.path.join(item, "dud_weed_spliff.png"), ember)


if __name__ == "__main__":
    main(sys.argv[1])
