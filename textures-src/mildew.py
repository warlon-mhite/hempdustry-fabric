#!/usr/bin/env python3
"""Draws the powder a hemp plant shows when it has mildew, and the dust it shows once sulfured.

    python3 textures-src/mildew.py common/src/main/resources/assets/hempdustry

Every crop stage texture gets two overlays, `<stage>_mildew.png` and `<stage>_sulfur.png`, made from
the stage's own pixels: only a pixel of the plant can take powder, and a pixel with open air above it
(the top of a leaf, where spores settle) takes it far more often. The pattern is a hash of the stage
and the pixel, so a re-run draws exactly the same thing.

The overlays are drawn by `block/crop_overlay`, a second cross a hair in front of the plant's own,
and each crop's blockstate gets one part per stage and value. This script writes those models and
parts too, replacing the ones it wrote before, so it is the one place to change any of it.

The powder's colours are the mod's own off-whites (the Infuser's milk, the hemp-leaf banner). The
sulfur's are vanilla's sulfur block's, because the dust is that sulfur.
"""
import hashlib
import json
import os
import re
import sys

from PIL import Image

POWDER = {"top": (0xF3, 0xF3, 0xF3), "body": (0xE7, 0xE2, 0xD2), "shade": (0xDD, 0xD7, 0xC4)}
SULFUR = {"top": (0xE0, 0xD6, 0x75), "body": (0xCF, 0xC8, 0x6A), "shade": (0xC2, 0xB3, 0x6B)}

# The odds a pixel of the plant takes a speck: with open air above it, and without.
LAYERS = {
    "mildew": (POWDER, 0.42, 0.14, "mildewed"),
    "sulfur": (SULFUR, 0.22, 0.06, "sulfured"),
}

CROPS = ("indica_crop", "sativa_crop", "beldia_crop")
STAGE = re.compile(r"^(?:%s)_stage\d+(?:_(?:bottom|middle|top))?$" % "|".join(CROPS))


def roll(*key):
    return int.from_bytes(hashlib.sha1("/".join(map(str, key)).encode()).digest()[:4], "big") / 2 ** 32


def overlay(stage, image, colours, top_odds, body_odds, kind):
    w, h = image.size
    src = image.load()
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    dst = out.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3] == 0:
                continue
            open_above = y == 0 or src[x, y - 1][3] == 0
            if roll(kind, stage, x, y) >= (top_odds if open_above else body_odds):
                continue
            if open_above:
                tone = colours["top"]
            else:
                tone = colours["body"] if roll(kind, stage, x, y, "tone") < 0.6 else colours["shade"]
            dst[x, y] = tone + (255,)
    return out


def main(assets):
    textures = os.path.join(assets, "textures", "block")
    models = os.path.join(assets, "models", "block")
    stages = sorted(f[:-4] for f in os.listdir(textures) if f.endswith(".png") and STAGE.match(f[:-4]))
    if not stages:
        sys.exit("no crop stage textures under " + textures)
    for stage in stages:
        image = Image.open(os.path.join(textures, stage + ".png")).convert("RGBA")
        for suffix, (colours, top, body, _) in LAYERS.items():
            overlay(stage, image, colours, top, body, suffix).save(os.path.join(textures, "%s_%s.png" % (stage, suffix)))
            with open(os.path.join(models, "%s_%s.json" % (stage, suffix)), "w") as out:
                json.dump({"parent": "hempdustry:block/crop_overlay",
                           "textures": {"overlay": "hempdustry:block/%s_%s" % (stage, suffix)}}, out, indent=2)
                out.write("\n")

    for crop in CROPS:
        path = os.path.join(assets, "blockstates", crop + ".json")
        with open(path) as f:
            blockstate = json.load(f)
        plant = [p for p in blockstate["multipart"] if "mildew" not in p.get("when", {})]
        layers = []
        for part in plant:
            model = part["apply"]["model"].removesuffix("_stressed")
            when = {k: v for k, v in part["when"].items() if k != "condition"}
            for suffix, (_, _, _, value) in LAYERS.items():
                layers.append({"when": dict(when, mildew=value), "apply": {"model": "%s_%s" % (model, suffix)}})
        # One overlay per stage and value: the plant's parts split a stage on its condition, which
        # the powder does not care about, so drop the duplicates that split leaves.
        unique = []
        for layer in layers:
            if layer not in unique:
                unique.append(layer)
        blockstate["multipart"] = plant + unique
        with open(path, "w") as f:
            json.dump(blockstate, f, indent=2)
            f.write("\n")
    print("%d stages, %d overlays" % (len(stages), len(stages) * len(LAYERS)))


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "common/src/main/resources/assets/hempdustry")
