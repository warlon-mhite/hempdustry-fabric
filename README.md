# Hempdustry

*Minecraft for stoners and hemprepreneurs.*

Hempdustry is a hemp and cannabis mod for Minecraft Java Edition. This repository holds every
supported version of it: one branch per Minecraft version, each one a complete project that builds
its own jars for its own loaders.

**Links:** [Website](https://hempdustry.fun) · [Wiki](https://github.com/warlon-mhite/hempdustry-java/wiki) ·
[Releases](https://github.com/warlon-mhite/hempdustry-java/releases) · CurseForge · NexusMods

[![Code](https://img.shields.io/badge/Code-AGPL--3.0-blue)](LICENSE)
[![Art & music](https://img.shields.io/badge/Art%20%26%20music-CC%20BY--NC--SA%204.0-blue)](https://creativecommons.org/licenses/by-nc-sa/4.0/)

---

## Which version to use

Pick the row for your Minecraft version. Each release jar is named for its mod version and Minecraft
version, `2.1.0+26.3` for example, and the loader is the one the jar is built for.

| Minecraft | Loaders | Branch | Support | Latest release |
|---|---|---|---|---|
| 26.3 | Fabric, NeoForge | `26.3-2.1` | **Active** | `v2.1.0+26.3` |
| 1.21.11 | Fabric, NeoForge | `2.1` | **Active** | `v2.1.0` |
| 1.21.1 | Fabric, NeoForge | `1.21.1-2.1` | **Active** | `v2.1.0+1.21.1` |
| 26.2 | Fabric, NeoForge | `26.2-2.1` | Last patch (2.1.1), then ends | `v2.1.0+26.2` |
| 1.20.1 | Fabric, Forge | `1.20.1-2.1` | Maintained | first release planned as 2.1.1 |
| 1.18.2 | Forge | `1.18.2-2.1` | Maintained | `v2.1.0+1.18.2` |
| 1.16.5 | Forge | `1.16.5-2.1` | Maintained | `v2.1.0+1.16.5` |
| 1.21.11 (2.0.3) | Fabric | `master` | End of life | `v2.0.3` |
| 1.21.1 (2.0.3) | Fabric | `1.21.1` | End of life | `v2.0.3+1.21.1` |

The 1.21.11 release of 2.1.0 is tagged `v2.1.0` and its jars are on that GitHub release; the other
releases have their own `v<version>+<minecraft>` tag.

Minecraft 26.4 is not ported yet. It gets its own branch once Fabric and NeoForge both build for it.

### Support levels

- **Active** — developed. Gets new features in the next minor release and every bug fix, on each
  loader the branch builds for.
- **Maintained** — no new features and no new minor releases. Gets fixes for crashes, world loss and
  world-import bugs. Nothing else.
- **Last patch** — one final patch release, then the branch is closed.
- **End of life** — no releases. The code stays for history.

Levels are reviewed at release checkpoints, from download numbers per line. The table above is the
current decision.

---

## Branches

- **Naming:** `<minecraft>-2.1` for every line of the 2.1 series. The 1.21.11 line predates the
  pattern and is simply `2.1`.
- **Each branch stands alone.** It has its own Gradle build, its own pinned versions and its own CI.
  A branch never builds for a Minecraft version other than its name.
- **`master`** is the old 2.0.3 Fabric trunk for 1.21.11, closed. It is kept for history and holds two
  things that stay live: this README, and the update manifest the in-game checker reads
  (`updates/hempdustry.json`, see [For server owners](#for-server-owners)).
- **Feature branches** (a modpack integration, a new content set, a rebase) appear and disappear. They
  carry no stability promise; the table above is the only one that matters to a player.
- **Tags:** `v<version>+<minecraft>` for each release, plus `v<version>` for the 1.21.11 line.

## Repository layout

A branch has the same shape on every line, with the loaders it builds for:

```
common/       code and data shared by every loader (vanilla APIs only)
fabric/       Fabric entry points, Fabric's side of the platform layer
neoforge/     NeoForge entry points (branches that build for NeoForge)
forge/        Forge entry points (1.16.5, 1.18.2, 1.20.1)
buildSrc/     shared Gradle build logic
scripts/      the checks run by scripts/gates.py (and scripts/twins.py on the 1.21.11 and 26.x lines)
textures-src/ editable sources for the generated placeholder textures
updates/      the update manifest (master, 2.1, 26.3-2.1 and 26.2-2.1; master's copy is the live one)
```

Generated data (recipes, loot tables, tags, advancements, models, worldgen) is committed, and the
`runDatagen` task writes it. Hand-written assets and lang files sit beside it in `common/` and each
loader's module.

Where a loader's module is named (`:fabric`, `:neoforge`, `:forge`), the Gradle tasks take that
prefix.

---

## For developers

### Toolchains

Each branch's `gradle.properties` is the source of truth for its Minecraft, loader and API versions.
What differs by line:

| Lines | Java to build with |
|---|---|
| `2.1`, `1.21.1-2.1`, `1.20.1-2.1` | 21 (1.20.1 targets bytecode 17) |
| `26.3-2.1`, `26.2-2.1` | 25 |
| `1.18.2-2.1` | 17 |
| `1.16.5-2.1` | 17 toolchain, Java 8 bytecode through Jabel |

Install the JDKs yourself: no branch downloads one. Gradle picks the right installed JDK for each
branch, and `org.gradle.java.installations.paths` in your own `~/.gradle/gradle.properties` points it
at one it does not find.

### Build and run

```bash
git clone -b 26.3-2.1 https://github.com/warlon-mhite/hempdustry-java.git
cd hempdustry-fabric
./gradlew build                  # both loaders' jars, in each module's build/libs/
./gradlew :fabric:runClient      # dev client, Fabric (JEI on the runtime; -Pviewer=rei or -Pviewer=none)
./gradlew :neoforge:runClient    # dev client, NeoForge
./gradlew :fabric:runDatagen     # regenerate the data; commit what it writes
./gradlew :fabric:runGametest    # headless server and the game tests, on Fabric
./gradlew :neoforge:runGameTestServer   # the same tests, on NeoForge
python3 scripts/gates.py         # every gate, in the order CI runs them (--quick to skip build and datagen)
```

Forge branches use `:forge` in place of `:neoforge`. `scripts/gates.py` runs the script checks, both
loaders' game tests (with a log gate that fails on any unexpected warning), a cold datagen that must
reproduce the committed data exactly, and the build. Its full output for each gate is kept in
`build/gates/`.

The game tests run real server code against the real registry and never reach the shipped jar.

### Making a change

- **Start on the line you can play-test the change on.** A fix for a reported bug starts on the line
  it was reported on.
- **Every change goes to every Active line that has the bug.** A change is carried to the other lines
  as a *twin*: the same commit subject and author, rewritten where the game's code differs. Run
  `python3 scripts/twins.py <other-branch>` to list what one line lacks.
- **A commit that belongs to one Minecraft version** carries a trailer instead:
  `Line-only: 26.3`.
- **Never carry build files between lines.** `gradle.properties`, the build scripts, the wrapper and
  the version ranges in the manifests are per line by design.
- A change is finished when `python3 scripts/gates.py` is green on the branch it lands on.
- After a change to a provider, run `runDatagen` and commit the output.

### Reporting a bug or opening a pull request

Name the Minecraft version, the loader, the Hempdustry jar and any other mods in the game. The branch
is named after the Minecraft version, so the version alone tells a maintainer where to look. A pull
request should say which lines it should reach.

---

## For modpack makers

- **Pick the jar for your Minecraft version and your loader.** One line per Minecraft version; a jar
  built for another version will not load.
- **Dependencies.** On Fabric: Fabric Loader and Fabric API. On NeoForge: NeoForge only. On Forge:
  Forge only.
- **Optional:** JEI or REI. With either one the mod adds pages for its machines, for packing a pipe or
  bong, and for the cauldron steps. Without one, everything still works.
- **Worlds.** From 2.0.2 on, a world made with any Hempdustry 2 release opens on every later one.
  Nothing needs doing on your side.
- **Licences.** The ordinary case is fine, including a pack that earns money: see
  [Licences](#licences).

## For server owners

- **Install the same jar on the server and on every client**, with the loader and Fabric API on the
  server if you run Fabric.
- **Recipe pages on a dedicated server.** Since Minecraft 1.21.2 a server no longer sends players its
  recipes. If you want JEI to show Hempdustry's crafting recipes to players, install JEI on the server
  too. The machine and packing pages show either way.
- **Update checker.** The in-game checker reads `updates/hempdustry.json` from the `master` branch over
  `raw.githubusercontent.com`, so it works whichever site a player downloaded the mod from. Each
  Minecraft version has a `-latest` and a `-recommended` key. The file is maintained by hand and is
  updated as part of each release.
- **Moving a world to a newer Minecraft version** goes through Minecraft's own data upgrade. Back up
  a world before you change its Minecraft version.

## Support and feedback

Bug reports go to the issue tracker on this repository. Include the Minecraft version, the loader, the
Hempdustry jar name and the output of the game log around the problem.

---

## Credits

The two music discs are **[Nefuß](https://nefu1.bandcamp.com/)** — *Moonlight*
([Bandcamp](https://nefu1.bandcamp.com/track/moonlight) ·
[SoundCloud](https://soundcloud.com/user-427551104/moonlight)) and *Robadob*
([Bandcamp](https://nefu1.bandcamp.com/track/robadob-2) ·
[SoundCloud](https://soundcloud.com/user-427551104/robadob)) — both written for this mod.

The Russian translation is **MargoxaTheGamer**'s, translated by hand from English.

The German translation is **Gülps'sch420**'s — German and Swiss German, translated by hand from
English.

American English and French are Warlon Mhite's. The other regional locales (British, Canadian,
Australian and New Zealand English, Canadian and Swiss French, and Austrian German) were adapted with
a Large Language Model from those and from Gülps'sch420's German, and no native speaker has read them
yet.

Some of the textures were drawn by a commissioned pixel artist who asked not to be credited by name.
Everything else is Warlon Mhite's.

## Licences

The mod is not all under one licence:

- **Code and data** — [GNU Affero General Public License v3.0](LICENSE) (`AGPL-3.0-only`).
- **Art and music** — [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/), the
  music by Nefuß and the rest by Warlon Mhite.
- **Not covered by either**: the people depicted in the paintings, a handful of textures derived
  from Minecraft's own, and the name "Hempdustry". None of those are the author's to license.

**Pack makers: the ordinary case is fine, including a pack that earns money.** If your pack lists
Hempdustry and the launcher fetches it from Modrinth, you have not copied anything and no licence
here asks anything of you. Running it on a server, monetised or not, is the same.

**And copying it is fine too — you are allowed to redistribute, embed, mirror, modify and fork this
mod.** The licences attach conditions rather than asking you to write in: credit, keep it
non-commercial, share adaptations alike, publish source for code changes. **Permission is only
needed for a commercial use.** The AGPL on the code does not restrict commercial use at all; it asks
for source, not a share.

[CREDITS.md](CREDITS.md) has the full table, and ships inside the jar.

## AI disclaimer

Large Language Models (LLM) chiefly Anthropic's **Claude Opus** and **Claude Sonnet**, used through
Claude Code, have been used in the making of this mod. Their role is assisting with more complex
features, helping for cross-mod compatibility, helping debug, documenting the code, update locale with local slangs (fr_ca or en_gb for example) generating placeholder textures, balancing mechanics,
and challenging and improving ideas.

---

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**
Minecraft is a trademark of Mojang Synergies AB.
