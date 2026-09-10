# Changelog

All notable changes to Hempdustry are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[Semantic Versioning](https://semver.org/). Jars are versioned `MAJOR.MINOR.PATCH+MCVERSION`, so
`2.0.0+1.21.1` is release 2.0.0 built for Minecraft 1.21.1. A pre-release carries its tag in the
same place: `2.0.0-beta+1.21.1` sorts *before* `2.0.0+1.21.1`, which is what lets a player on the
beta be told when the release lands.

## [Unreleased]

### Breaking

- **The two crop blocks no longer register an item.** `hempdustry:indica_crop` and
  `hempdustry:sativa_crop` were items with no model, absent from the creative tab and obtainable only
  with `/give` — vanilla wheat has no such item either, because the seeds are the item. If you gave
  yourself one it is gone. **Planted crops, the seeds and everything the plants drop are untouched**,
  and middle-clicking a growing plant still hands you its seeds. It also silences the two
  `Unable to load model: 'hempdustry:item/indica_crop'` warnings the client logged on every launch.
  Done now rather than later because removing a registry id stops being allowed at 2.0.0 stable.
- **Hoppers and pipes can no longer put milk into the Infuser.** Its milk slot and bucket-return
  slot are gone (see *Changed*), so a hopper line that fed it milk buckets stops doing so. Point a
  **dispenser** at the tub instead: it pours the milk in and keeps the empty bucket, and a
  comparator behind the tub reads 0 once a batch is collected, so the dispenser can refill it on
  its own. Hemp still goes in by hopper exactly as before, and the spout still pours the
  cannabutter out. **Nothing in your world is lost** — a bucket a beta world left in either old slot
  pops out of the top of the tub the first time it loads.

### Changed

- **Milk is poured into the Infuser by hand, like water into a cauldron.** Right-click the tub with
  a bucket of milk or hemp milk: it fills, and you get the empty back. Right-click with anything
  else — or with milk while it is already full — and the screen opens as before, so the pour and
  the screen share one button without getting in each other's way. In the screen, the two bucket
  slots are replaced by a milk indicator beside the flame, and the recipe pages in JEI, REI and EMI
  now say the milk is poured in. The tub still holds one milk and makes one cannabutter per batch.
- **Wild hemp is much harder to find.** Wild Purple Kush and Lemon Haze now turn up about a fifth as
  often as before, and a find is usually one to four plants rather than a field. Every so often you
  will still stumble on a big patch of six to eight. Only newly explored land is affected; wild
  plants already standing in your world stay where they are.
  - Bone meal on grass could also grow wild hemp, anywhere it grows wild — an endless free supply
    of seeds. It grows only vanilla flowers now.
  - Wild Purple Kush now really does grow on the moss of lush caves, a plant here and there. It was
    meant to from the start, but it was being planted before the moss was laid and never took.

### Added

- **Recipe pages in [REI](https://modrinth.com/mod/rei)**, alongside the JEI and EMI ones the mod
  already had. All three viewers show the same three pages — Decarboxylator, Infuser and the
  cauldron's retting and washing — and the same packing rows in the crafting tab, because they read
  one description of what the mod does rather than three. Install one, or none; nothing in the mod
  loads a line of viewer code unless the viewer is there.
  - The cauldron page is the one worth having: retting and washing are not recipes, so the recipe
    book cannot show them and no datapack can list them.

## [2.0.0-beta] — 2026-09-03

The first public build of the rewrite, and a beta because most of it has never been played. The
feature list below is what 2.0.0 will be; what the beta is asking for is whether it holds up with a
player in it.

**What "beta" means for your world.** The intent is that a beta world opens on 2.0.0, and the mod
already ships compatibility shims it did not strictly owe anyone. But the promise that a world always
survives an update starts at **2.0.0 stable**, not here — if play-testing turns up something that
should have been built differently, the beta is when it is still cheap to fix. Any build that does
break something will say so under an explicit **Breaking** heading, naming what you lose. Keep a
backup of a world you care about, as with any beta.

**Hempdustry 2 — the Fabric rewrite.** The mod was originally a Forge 1.16 mod built with MCreator;
this is a hand-written rewrite for Fabric 1.21.1 and the first release on that line. The version
number marks the rewrite rather than feature parity: some of the original's content has been
reimagined, and some has not been ported yet.

If you played the original, the plant, the smoking gear and the building materials will be familiar;
almost everything else is new or works differently.

### Added — the plant

- **Two strains, Purple Kush and Lemon Haze**, each with its own crop, seeds, buds and wild flower.
  They differ in plant shape and in what they drop, not in a claimed difference of feeling: Purple
  Kush is a short, leafy two-block plant; Lemon Haze is three blocks tall, grows slower, needs
  headroom, and pays out in stems where the other pays in leaves.
- **Wild flowers generate** across a curated set of biomes — jungle, swamp and lush caves for Purple
  Kush; savanna through to badlands for Lemon Haze, thinning out as the ground gets harsher. Shears
  or Silk Touch lift one intact for a flower pot.
- **Hemp seeds turn up in tall grass** and in a handful of exploration chests, so the mod can be
  stumbled into rather than sought out.
- **Defoliation.** A growing plant can be sheared twice, in two windows during its life. Each cut
  gives a hemp leaf immediately and shifts that plant's harvest one step towards buds and away from
  leaves. Ignoring it is a perfectly good way to play.
- Bees pollinate hemp; parrots and chickens eat the seeds; goats eat the leaves.

### Added — smoking

- **A spliff, a wooden pipe and a bong.** What is loaded is data on the item rather than a separate
  item per combination, the same way a potion carries its contents.
- **The device sets the duration, the dose sets the strength.** Packing more buds raises the effect
  level; the bong's bowl simply holds more than the pipe's.
- **Every strain is a bundle of good and bad**, and dosing raises both halves. Purple Kush trades
  mining speed for damage resistance; Lemon Haze trades melee damage for movement and mining speed.
- **Green out** — a big dose carries a real chance of losing the hit entirely and spending a while
  nauseous and useless instead. A single-bud hit never can.
- Pipes and bongs are damageable, enchantable and anvil-repairable, packed or empty.

### Added — the cannabutter chain

- **Decarboxylator**, the mod's first machine: a hemp-brick oven with three trays that cook on their
  own timers. Deliberately expensive; edibles are meant to be a goal rather than a first-day crop.
- **A water cauldron** does two jobs — rinsing decarboxylated hemp, which decides how good the
  result can get, and **retting** hemp stems into fibre at a better rate than the crafting grid.
- **Infuser**, a hempcrete tub that takes its heat from whatever is burning underneath it. A batch
  carries two independent axes — strength and quality — and pours itself into an adjacent container
  through a visible spout once it peaks.
- **Cannabutter**, carrying both axes into whatever you cook with it.

### Added — food

- **Edibles:** Space Cookies, Space Brownies, Space Cake, Cannabutter Toast, Dawamesk and a Bucket
  of Bhang. One batch of butter spreads across eight cookies or concentrates into a single dawamesk,
  and that dilution is what separates the tiers.
- **Edibles do not hit straight away.** Thirty seconds to three minutes pass before anything
  happens, the first thing you feel is your legs getting heavy, and the good part arrives after
  that. A better batch of butter narrows the wait and makes the effects last longer.
- **Food with nothing psychoactive in it:** toasted hemp seeds, a hemp seed bar, a bucket of hemp
  milk, and siemieniotka, the Silesian hemp-seed soup eaten at Christmas Eve.
- **Hemp milk works anywhere milk does.** It clears effects the way cow's milk does, and it is
  accepted by the Infuser, by Space Cake and bhang, and by vanilla's cake — so the whole cannabutter
  chain, and a cake, can be had without ever finding a cow.

### Added — materials and building

- **A cloth chain** — stems → fibre → canvas → hemp wool → hemp carpet, reversible one step at a
  time. Canvas stands in where vanilla uses leather, hemp wool where vanilla uses wool; between them
  you can reach books, item frames, paintings and a bed without a cow or a sheep.
- **Hempcrete**, including a powder form that sets on contact with water like concrete.
- **Hemp bricks** and a full **hemp plank set** — stairs, slabs, doors, trapdoors, fences, signs,
  hanging signs, boats — fireproof, the way Crimson and Warped are.
- **A hemp armour set**: beanie, shirt, harem pants and flip-flops. It is not good armour, but it
  takes enchantments and armour trims like any other.

### Added — everything else

- **Twenty-one advancements** covering cultivation, smoking, the cannabutter chain and the edibles.
- **Two music discs** with original tracks by [Nefuß](https://nefu1.bandcamp.com/) — *Moonlight* and
  *Robadob* — droppable by creepers and findable in dungeon and mansion chests.
- **Six paintings.**
- **Eight locales**, kept 1:1 — five English variants and three French, with regional vocabulary
  rather than copy-paste.
- **The Infuser simmers audibly.** A looping simmer plays while and only while a batch is actually
  cooking — milk in the tub and heat underneath. Subtitled in all eight locales.
- **An update checker** that tells you once, on your first world join, if a newer release exists.
  Client-side only, silent on any failure, and switched off with `client.updateCheck: false` in
  `config/hempdustry.json` — which is what a pack with pinned versions generally wants.

### Notes for pack makers

- **Fabric API is required.**
- The mod is required on **both client and server**.
- **`config/hempdustry.json` is written on first run**, commented, with every knob at its default —
  effect duration and strength, the effect master switch (an "industrial hemp only" mode), green-out
  and nausea toggles, cooldowns, crop and machine speed, the Infuser's two timings, the mod's
  additions to vanilla loot tables, and `client.updateCheck`. Everything but that last one is applied
  server-side, so a client needs no matching file. `/hempdustry reload` re-reads it. A knob added in a
  later version appears in an existing file at its default; there is no migration step.
- **Listing the mod in a pack is fine, monetised or not** — the launcher downloads it from Modrinth,
  so no licence is engaged at all. **Embedding, mirroring, modifying and forking are also permitted**,
  non-commercially, with credit and share-alike on anything adapted; the art and music are
  CC BY-NC-SA 4.0 and the code is AGPL-3.0-only, which does not restrict commercial use at all.
  **Only a commercial use needs to ask.** Full table in [CREDITS.md](CREDITS.md), inside the jar.
- **EMI and JEI plugins are included.** Both show the Decarboxylator, the Infuser and the two water-
  cauldron soaks, and both list every way to pack a pipe or a bong in the ordinary crafting category,
  where "move ingredients into the grid" works on them. REI is not supported.
- Both directions of item automation work with Fabric storage mods: the machines can be extracted from
  by any pipe, and **the Infuser's spout pours into anything that exposes a storage** — an AE2
  interface, a Modern Industrialization pipe — as well as ordinary chests, hoppers and minecarts.

#### Saves and updates

**From 2.0.0 onward a world opens on every later version, with no migration script and nothing for
the player to do.** Registry ids, item components, block entity data, blockstates, advancement ids and
the datapack formats below are all treated as frozen after this release; anything that has to change
ships a fallback that keeps reading the old form. If an update ever cannot honour that, it will be a
major version and it will say so here.

#### For datapack and resource-pack authors

- **Strains are a datapack registry.** `data/<your_pack>/hempdustry/strain/<id>.json` — note the
  `hempdustry` directory inside your namespace; a file without it is silently ignored. Fields:
  `translation_key`, `color`, `seeds`, `buds`, `flower`, optional `model_index` and `effects`. Edit a
  shipped strain by writing the same id, and `/reload` applies it.
- **A strain you add gets its own look for free.** Leave `model_index` at `0` and the spliff tip and
  device bowls are tinted with your `color`. Colours multiply, so pick the *brightest* point of the
  range you want rather than the middle. `model_index` 1–99 is reserved for this mod; use 100+ only if
  you are also shipping textures.
- **Resource packs**: the smoking gear draws as base art on `layer0` plus a tinted mask on `layer1`.
  Keep that split when replacing the art. Item properties are `hempdustry:packed` (0/1) and
  `hempdustry:strain` (a `model_index`).
- **Convention tags joined**, so recipes elsewhere find hemp without either side knowing about the
  other: `c:crops/hemp`, `c:bricks/hemp`, `c:storage_blocks/hemp`, `c:concretes`,
  `c:concrete_powders`, `c:strings`, `c:leathers`, `c:buckets/milk`, `c:armors`, `c:music_discs` and
  the `c:foods` family.
- **`#hempdustry:heat_sources`** — add a modded forge or crucible and it will heat an Infuser.
  **`#hempdustry:milk_buckets`** — add another mod's milk and the Infuser will take it.
- **Both machines run on datapack recipes.** `hempdustry:decarboxylating` takes `ingredient` and
  `result`, one file per input — that is how another mod's buds get into the oven, and how a pack
  changes a yield. `hempdustry:infusing` is a single file describing the whole tub: `container`,
  `hemp`, `washed_hemp`, `result`. Strength and Quality are measured by the machine and are not part
  of the recipe. Both are rebalanceable with KubeJS and CraftTweaker.

#### For mod authors

- **There is an API package**, `com.warlonmhite.hempdustry.api`, with four Fabric events:
  `ALLOW_SMOKE` (veto a hit — a sober zone, a jail plugin, an accessibility mod), `AFTER_SMOKE` and
  `AFTER_EAT` (your own effect, a tolerance system, a quest task) and `AFTER_INFUSE` (a batch of
  cannabutter collected, with its strength and grade). All fire on the server thread.
- **Counting consumption means listening to two events, not one.** A hit is `AFTER_SMOKE`, an edible
  is `AFTER_EAT`; an addon watching only the first sees half the traffic.
- **That package is frozen from this release**, along with the types its signatures name. New events
  may be added; none will be removed or re-shaped. Everything outside it is internal.
- Compile against it from Modrinth's maven:

  ```gradle
  repositories { maven { url = "https://api.modrinth.com/maven" } }
  dependencies { modImplementation "maven.modrinth:hempdustry:2.0.0-beta+1.21.1" }
  ```

[Unreleased]: https://github.com/warlon-mhite/hempdustry-fabric/compare/v2.0.0-beta...HEAD
[2.0.0-beta]: https://github.com/warlon-mhite/hempdustry-fabric/releases/tag/v2.0.0-beta
