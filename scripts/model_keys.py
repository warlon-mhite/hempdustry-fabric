#!/usr/bin/env python3
"""Check that every key in the mod's block and item models is one the game's model reader reads.

The reader skips a key it does not know without a word: no warning in the log, no failed load, the
model just draws without it. That is how a renamed key goes missing. Minecraft 26.3 replaced an
element's `"shade": false` with `"shade_direction_override": "up"`, the mod's crops kept the old key,
and every crop drew a shade darker than on 26.2, with nothing to say why.

So the keys are not listed here: they are read out of the model reader's own classes in the game jar
(the one Loom keeps in the Gradle cache, as recipe_collisions does for vanilla's recipes), whose
constant pools name every key they look up. A key the jar does not name is reported, with the file it
is in. Keys that are notes to people are allowed: any key starting with "_", and an element's "name",
which modelling tools write as a label.

Usage:
    python3 scripts/model_keys.py

Exits non-zero and names each unread key. If the reader's classes are not where this script looks,
it fails too: the reader has moved, and the list of where to look needs updating.
"""
import collections
import json
import pathlib
import struct
import sys
import zipfile

import layout

ROOT = pathlib.Path(__file__).resolve().parent.parent
LAYOUT = layout.of(ROOT)

#: The classes that read each part of a model, by the part they read (26.3's names).
PACKAGE = "net/minecraft/client/resources/model/cuboid/"
READERS = {
    "model": "CuboidModel$Deserializer",
    "element": "CuboidModelElement$Deserializer",
    "face": "CuboidFace$Deserializer",
    "transform": "ItemTransform$Deserializer",
}
#: Keys for people, which the game is meant to skip.
NOTES = {"element": {"name"}}


def minecraft_version():
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if line.startswith("minecraft_version="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("gradle.properties names no minecraft_version")


def utf8_constants(data):
    """Every UTF-8 constant in a class file's constant pool."""
    count = struct.unpack(">H", data[8:10])[0]
    i, index, found = 10, 1, set()
    while index < count:
        tag = data[i]
        if tag == 1:
            length = struct.unpack(">H", data[i + 1:i + 3])[0]
            found.add(data[i + 3:i + 3 + length].decode("utf-8", "replace"))
            i += 3 + length
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            i += 5
        elif tag in (5, 6):
            i += 9
            index += 1  # a long or a double takes two slots
        elif tag in (7, 8, 16, 19, 20):
            i += 3
        elif tag == 15:
            i += 4
        else:
            raise ValueError(f"unknown constant pool tag {tag}")
        index += 1
    return found


def read_keys():
    jar = pathlib.Path.home() / ".gradle/caches/fabric-loom" / minecraft_version() / "minecraft-client.jar"
    if not jar.exists():
        sys.exit(f"no game jar at {jar}: build once so Loom puts it there")
    keys = {}
    with zipfile.ZipFile(jar) as z:
        for part, name in READERS.items():
            try:
                data = z.read(PACKAGE + name + ".class")
            except KeyError:
                sys.exit(f"{PACKAGE}{name} is not in {jar.name}: has the model reader moved?")
            keys[part] = {s for s in utf8_constants(data) if s.isidentifier() and s.islower()}
            keys[part] |= NOTES.get(part, set())
    return keys


def model_files():
    for base in [LAYOUT.resources, LAYOUT.generated] + ([LAYOUT.neoforge_generated] if LAYOUT.neoforge else []):
        yield from sorted((base / "assets").glob("*/models/**/*.json"))


def unread(model, keys):
    """(where, key) for every key in this model that its part's reader does not read."""
    def check(obj, part, where):
        for key in obj:
            if not key.startswith("_") and key not in keys[part]:
                yield where, key

    yield from check(model, "model", "")
    for i, element in enumerate(model.get("elements", [])):
        yield from check(element, "element", f"elements[{i}].")
        if isinstance(element.get("rotation"), dict):
            yield from check(element["rotation"], "element", f"elements[{i}].rotation.")
        for side, face in element.get("faces", {}).items():
            yield from check(face, "face", f"elements[{i}].faces.{side}.")
    for slot, transform in model.get("display", {}).items():
        yield from check(transform, "transform", f"display.{slot}.")


def main():
    keys = read_keys()
    problems = collections.defaultdict(list)
    files = 0
    for path in model_files():
        files += 1
        for where, key in unread(json.loads(path.read_text(encoding="utf-8")), keys):
            problems[key].append(path.relative_to(ROOT))
    if problems:
        print("model keys the game does not read (it skips them without a word):")
        for key, paths in sorted(problems.items()):
            print(f"  - \"{key}\": {len(paths)} time(s) in {len(set(paths))} file(s), e.g. {paths[0]}")
        return 1
    print(f"model keys OK: {files} model(s), every key read by {minecraft_version()}'s model reader")
    return 0


if __name__ == "__main__":
    sys.exit(main())
