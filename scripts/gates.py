#!/usr/bin/env python3
"""Run every gate a change has to pass, in order, and print one table of what each said.

    python3 scripts/gates.py                      # every gate
    python3 scripts/gates.py --quick              # leave out the build and the cold datagen
    python3 scripts/gates.py --released <jar>     # and check the API against a released jar

Cheapest first, so a failure shows early:
  - the script checks: lang_coverage, use_with_item_pass, biome_tint_pairing, tall_plant_loot, and
    recipe_collisions against vanilla's own recipes, read out of the game jar in the Gradle cache;
  - runGametest, which also fails on any WARN or ERROR in the server's log nobody listed;
  - a cold runDatagen, which has to leave the generated data exactly as it is committed;
  - the build;
  - api_additive.py against a released jar, when one is given.

A gate that is not on this branch (an older line has fewer scripts) is skipped and says so. Each
gate's full output is kept in build/gates/<gate>.log. Exits 1 if any gate failed.
"""
import argparse
import pathlib
import re
import shutil
import subprocess
import sys
import time
import zipfile

import layout

ROOT = pathlib.Path(__file__).resolve().parent.parent
LAYOUT = layout.of(ROOT)
LOGS = ROOT / "build/gates"
# The generated data, as git and the shell see it: relative to the checkout.
GENERATED = str(LAYOUT.generated.relative_to(ROOT))


def minecraft_version():
    for line in (ROOT / "gradle.properties").read_text().splitlines():
        if line.startswith("minecraft_version="):
            return line.split("=", 1)[1].strip()
    raise SystemExit("gradle.properties names no minecraft_version")


def vanilla_data():
    """Vanilla's recipes, unzipped once from the client jar loom keeps, for recipe_collisions.py."""
    out = LOGS / ("vanilla-" + minecraft_version())
    if not (out / "data").exists():
        jar = pathlib.Path.home() / ".gradle/caches/fabric-loom" / minecraft_version() / "minecraft-client.jar"
        with zipfile.ZipFile(jar) as z:
            for name in z.namelist():
                if name.startswith("data/minecraft/recipe/"):
                    z.extract(name, out)
    return out


def last(pattern):
    """The last line of the output matching pattern: what a gate reports as its verdict."""
    def pick(output):
        found = [line.strip() for line in output.splitlines() if re.search(pattern, line)]
        return found[-1] if found else ""
    return pick


def gates(args):
    py = [sys.executable]
    script = lambda name: ROOT / "scripts" / name
    yield "lang coverage", py + [script("lang_coverage.py")], last(r"missing|orphan")
    yield "onUse skipped", py + [script("use_with_item_pass.py")], last(r"SKIP_DEFAULT_BLOCK_INTERACTION")
    yield "biome tint pairing", py + [script("biome_tint_pairing.py")], last(r"tinted|OK|offend")
    yield "tall plant loot", py + [script("tall_plant_loot.py")], last(r"OK|pool")
    yield "recipe collisions", py + [script("recipe_collisions.py"), GENERATED, str(vanilla_data())], \
        last(r"scanned|collision")
    yield "game tests", ["./gradlew", LAYOUT.fabric + "runGametest", "--console=plain"], \
        lambda output: last(r"nobody expected")(output) or last(r"required tests")(output) or last(r"BUILD")(output)
    if not args.quick:
        yield "cold datagen", ["sh", "-c", f"rm -rf {GENERATED}/.cache && ./gradlew {LAYOUT.fabric}runDatagen"
                               f" --console=plain && git status --porcelain -- {GENERATED} | head -20"], last(r"\S")
        yield "build", ["./gradlew", "build", "--console=plain"], last(r"BUILD")
    if args.released:
        jars = sorted(LAYOUT.fabric_libs.glob("*.jar"), key=lambda p: p.stat().st_mtime)
        jars = [j for j in jars if not j.name.endswith(("-sources.jar", "-dev.jar"))]
        yield "API kept", py + [script("api_additive.py"), args.released, str(jars[-1]) if jars else "none"], \
            last(r"missing")


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--quick", action="store_true", help="leave out the build and the cold datagen")
    parser.add_argument("--released", metavar="JAR", help="the last release's remapped jar, for the API check")
    args = parser.parse_args()
    LOGS.mkdir(parents=True, exist_ok=True)
    results = []
    for name, command, verdict in gates(args):
        if not pathlib.Path(command[1] if command[0] == sys.executable else ROOT / "gradlew").exists():
            results.append((name, "skip", "not on this branch"))
            continue
        started = time.time()
        print(f"-- {name} ...", flush=True)
        run = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
        output = run.stdout + run.stderr
        (LOGS / (name.replace(" ", "_") + ".log")).write_text(output)
        ok = run.returncode == 0
        detail = verdict(output)
        if name == "cold datagen" and ok:
            # The porcelain lines follow the build's own: anything listed is a file datagen changed.
            changed = [line for line in output.splitlines() if re.match(r"^( M|M |\?\?| D|D ) ", line)]
            ok, detail = not changed, ("changed " + ", ".join(changed[:5])) if changed else GENERATED + " unchanged"
        results.append((name, "ok" if ok else "FAIL", f"{detail} ({time.time() - started:.0f} s)"))
    width = max(len(name) for name, _, _ in results)
    print()
    for name, status, detail in results:
        print(f"{name.ljust(width)}  {status:4}  {detail}")
    return 1 if any(status == "FAIL" for _, status, _ in results) else 0


if __name__ == "__main__":
    shutil.which("git") or sys.exit("needs git on the PATH")
    sys.exit(main())
