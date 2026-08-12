# Changelog

All notable changes to Hempdustry are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[Semantic Versioning](https://semver.org/). Jars are versioned `MAJOR.MINOR.PATCH+MCVERSION`, so
`2.0.0+1.21.1` is release 2.0.0 built for Minecraft 1.21.1.

## [Unreleased]

Nothing yet.

## [2.0.0] — unreleased

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
- **A hemp armour set**: beannie, shirt, harem pants and flip-flops. It is not good armour.

### Added — everything else

- **Twenty-one advancements** covering cultivation, smoking, the cannabutter chain and the edibles.
- **Two music discs** with original tracks by [Nefuß](https://nefu1.bandcamp.com/) — *Moonlight* and
  *Robadob* — droppable by creepers and findable in dungeon and mansion chests.
- **Six paintings.**
- **Eight locales**, kept 1:1 — five English variants and three French, with regional vocabulary
  rather than copy-paste.
- **An update checker** that tells you once, on your first world join, if a newer release exists.
  Client-side only, silent on any failure.

### Notes for pack makers

- **Fabric API is required.**
- The mod is required on **both client and server**.
- **Listing the mod in a pack is fine, monetised or not** — the launcher downloads it from Modrinth,
  so no licence is engaged at all. **Embedding, mirroring, modifying and forking are also permitted**,
  non-commercially, with credit and share-alike on anything adapted; the art and music are
  CC BY-NC-SA 4.0 and the code is AGPL-3.0-only, which does not restrict commercial use at all.
  **Only a commercial use needs to ask.** Full table in [CREDITS.md](CREDITS.md), inside the jar.
- Neither machine has a data-driven recipe type yet, so their conversions cannot be rebalanced with
  KubeJS or CraftTweaker. Recipes in the crafting grid are ordinary datagen JSON and can be.
- There is no JEI/EMI/REI plugin yet, so **packing a smoking device does not appear in a recipe
  viewer**. It is: an empty pipe or bong, plus one to three buds, in the crafting grid.

[Unreleased]: https://github.com/warlon-mhite/hempdustry-fabric/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/warlon-mhite/hempdustry-fabric/releases/tag/v2.0.0
