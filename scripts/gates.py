#!/usr/bin/env python3
"""Run every gate a change has to pass, in order, and print one table of what each said.

    python3 scripts/gates.py                      # every gate
    python3 scripts/gates.py --quick              # leave out the build and the cold datagen
    python3 scripts/gates.py --released <jar>     # and check the API against a released jar
    python3 scripts/gates.py --released-neoforge <jar>   # ...and NeoForge's against its own

Cheapest first, so a failure shows early:
  - the script checks: lang_coverage, use_with_item_pass, biome_tint_pairing, tall_plant_loot, and
    recipe_collisions against vanilla's own recipes, read out of the game jar in the Gradle cache;
  - runGametest, which also fails on any WARN or ERROR in the server's log nobody listed -- on
    Fabric, and on NeoForge when the branch has it;
  - a cold runDatagen, which has to leave the generated data exactly as it is committed, and the
    same for NeoForge's own data run;
  - the build;
  - api_additive.py against a released jar, when one is given: Fabric's jar against a Fabric release,
    NeoForge's against a NeoForge one. The two jars are not comparable with each other (Fabric's
    descriptors name Minecraft in intermediary, NeoForge's in Mojang's names), and their api packages
    differ: Fabric's events are Fabric API callbacks, NeoForge's are bus events.

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
NEOFORGE_GENERATED = str(LAYOUT.neoforge_generated.relative_to(ROOT)) if LAYOUT.neoforge else None


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


def cold(task, generated):
    """A data run from scratch, then what git sees changed in its output."""
    return ["sh", "-c", f"rm -rf {generated}/.cache && ./gradlew {task} --console=plain"
                        f" && git status --porcelain -- {generated} | head -20"]


#: Each cold data run and the folder it writes.
COLD = {"cold datagen": GENERATED, "NeoForge cold datagen": NEOFORGE_GENERATED}


def gates(args):
    py = [sys.executable]
    script = lambda name: ROOT / "scripts" / name
    yield "lang coverage", py + [script("lang_coverage.py")], last(r"missing|orphan")
    yield "onUse skipped", py + [script("use_with_item_pass.py")], last(r"SKIP_DEFAULT_BLOCK_INTERACTION")
    yield "biome tint pairing", py + [script("biome_tint_pairing.py")], last(r"tinted|OK|offend")
    yield "tall plant loot", py + [script("tall_plant_loot.py")], last(r"OK|pool")
    yield "recipe collisions", py + [script("recipe_collisions.py"), GENERATED, str(vanilla_data())], \
        last(r"scanned|collision")
    tests_verdict = lambda output: last(r"nobody expected")(output) or last(r"required tests")(output) or last(r"BUILD")(output)
    yield "game tests", ["./gradlew", LAYOUT.fabric + "runGametest", "--console=plain"], tests_verdict
    if LAYOUT.neoforge:
        yield "NeoForge game tests", ["./gradlew", LAYOUT.neoforge + "runGameTestServer", "--console=plain"], tests_verdict
    if not args.quick:
        yield "cold datagen", cold(LAYOUT.fabric + "runDatagen", GENERATED), last(r"\S")
        if LAYOUT.neoforge:
            yield "NeoForge cold datagen", cold(LAYOUT.neoforge + "runData", NEOFORGE_GENERATED), last(r"\S")
        yield "build", ["./gradlew", "build", "--console=plain"], last(r"BUILD")
    if args.released:
        yield "API kept", py + [script("api_additive.py"), args.released, newest_jar(LAYOUT.fabric_libs)], \
            last(r"missing")
    if args.released_neoforge:
        yield "NeoForge API kept", py + [script("api_additive.py"), args.released_neoforge,
                                         newest_jar(LAYOUT.neoforge_libs)], last(r"missing")


def newest_jar(libs):
    """The jar the last build left in libs, not its sources: the one a player installs."""
    jars = sorted(libs.glob("*.jar"), key=lambda p: p.stat().st_mtime)
    jars = [j for j in jars if not j.name.endswith(("-sources.jar", "-dev.jar"))]
    return str(jars[-1]) if jars else "none"


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--quick", action="store_true", help="leave out the build and the cold datagen")
    parser.add_argument("--released", metavar="JAR", help="the last release's remapped jar, for the API check")
    parser.add_argument("--released-neoforge", metavar="JAR",
                        help="the last release's NeoForge jar, for the same check on NeoForge's jar")
    args = parser.parse_args()
    if args.released_neoforge and not LAYOUT.neoforge:
        parser.error("this branch builds no NeoForge jar")
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
        if name in COLD and ok:
            # The porcelain lines follow the build's own: anything listed is a file datagen changed.
            changed = [line for line in output.splitlines() if re.match(r"^( M|M |\?\?| D|D ) ", line)]
            ok, detail = not changed, ("changed " + ", ".join(changed[:5])) if changed else COLD[name] + " unchanged"
        results.append((name, "ok" if ok else "FAIL", f"{detail} ({time.time() - started:.0f} s)"))
    width = max(len(name) for name, _, _ in results)
    print()
    for name, status, detail in results:
        print(f"{name.ljust(width)}  {status:4}  {detail}")
    return 1 if any(status == "FAIL" for _, status, _ in results) else 0


if __name__ == "__main__":
    shutil.which("git") or sys.exit("needs git on the PATH")
    sys.exit(main())
