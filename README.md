# Hempdustry

*Minecraft for stoners and hemprepreneurs.*

A hemp and cannabis mod for **Minecraft 1.21.1** on **Fabric**. Grow it, harvest it, smoke it, cook with
it or use it as a building material, because that is most of what hemp has
actually been for over the last five thousand years.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-lightgrey)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-AGPL--3.0-blue)](LICENSE)

**Download:** SOON

> **Status: version 2.0.0**
> This is a rewrite for Fabric 1.21.1 of an older Forge 1.16 mod (GanjaCraft) that was built with
> MCreator. "Hempdustry 2" is the marker of that rewrite, not a claim that everything from the
> original is back. Some features have been reimagined and the mod is a bit different from the original.

---

## What's in it

### The plant

Two strains, **Purple Kush** and **Lemon Haze**, each with its own crop, seeds, buds and wild flower.
They differ in shape and in what they give you rather than in some claimed difference of feeling:
Purple Kush is a short, leafy two-block plant, Lemon Haze is a three-block one that grows slower,
wants headroom, and pays out in stems instead of leaves.

Wild flowers generate across a curated set of biomes — jungle, swamp and lush caves for Purple Kush,
savanna through to badlands for Lemon Haze — and can be sheared up intact for a flower pot. Hemp
seeds also turn up in tall grass and in a handful of exploration chests, so you can stumble into the
mod without looking for it.

**Defoliation.** A growing plant can be sheared twice, in two windows during its life. Each cut hands
you a hemp leaf on the spot and shifts that plant's eventual harvest one step towards buds and away
from leaves. Ignoring it entirely is a perfectly good way to play — the plant matures either way, it
just finishes leaf-heavy instead of bud-heavy.

Bees will pollinate hemp. Parrots and chickens eat the seeds; goats eat the leaves.

### Smoking

Three things to smoke with — a **spliff**, a **wooden pipe** and a **bong** — and what's loaded is
data on the item rather than a separate item per combination, the same way a potion carries its
contents.

- **The device sets the duration, the dose sets the strength.** Pack more buds for a stronger hit;
  the bong's bowl simply takes more of them than the pipe's.
- **Every strain is a bundle of good and bad.** Purple Kush trades mining speed for damage
  resistance; Lemon Haze trades melee damage for movement and mining speed. Dosing raises both halves.
- **Green out.** Big doses carry a real chance of losing the hit entirely and spending a while
  nauseous and useless instead. A single-bud hit never does.
- Pipes and bongs are damageable, enchantable and anvil-repairable, packed or empty.

### From crop to cannabutter

A four-step chain, and each step is a thing you can see happening:

1. **Decarboxylator** — a hemp-brick oven with three trays that cook on their own timers. It is
   expensive on purpose; edibles are meant to be a goal, not a first-day crop.
2. **A water cauldron** — rinse the decarboxylated hemp. Optional, but it decides how good the
   result can get. (The same cauldron also *rets* hemp stems into fibre, at a better rate than the
   crafting grid gives you.)
3. **Infuser** — a hempcrete tub around a cauldron that takes its heat from whatever is burning
   underneath it. A campfire is cheap and always lit; a furnace only works while it is itself busy.
   The batch carries two independent axes, **strength** (how much hemp went in) and **quality** (how
   patient and how well-prepared you were), and pours itself into a chest through a visible spout
   when it peaks.
4. **Cannabutter**, and then the food.

### Edibles, and food that isn't

**Space Cookies, Space Brownies, Space Cake, Cannabutter Toast, Dawamesk** and a **Bucket of Bhang**.
One batch of butter spreads across eight cookies or concentrates into a single dawamesk, and that
dilution is what sets the tiers apart.

They do not hit straight away. There's a delay of anywhere from thirty seconds to three minutes
before anything happens, the first thing you feel is your legs getting heavy, and the good part
arrives after that. A better batch of butter narrows how unpredictable the wait is and makes the
effects last longer. Eating a second one because the first "isn't working" is a mistake the game
will let you make.

Separately, and with nothing psychoactive in it at all: **toasted hemp seeds**, a **hemp seed bar**,
a **bucket of hemp milk** (which clears effects, like cow's milk, and lets you skip the cow) and
**siemieniotka**, the Silesian hemp-seed soup eaten at Christmas Eve.

### Materials and building

Hemp is a fibre crop first, and the mod treats it that way.

- **Cloth chain:** stems → fibre → canvas → hemp wool → hemp carpet, reversible one step at a time.
  Canvas stands in for leather where vanilla uses leather; hemp wool stands in for wool where vanilla
  uses wool. Between them you get books, item frames, paintings and a bed without a cow or a sheep.
- **Hempcrete**, including a powder form that sets on contact with water like concrete does.
- **Hemp bricks** and a full **hemp plank set** — stairs, slabs, doors, trapdoors, fences, signs,
  hanging signs, boats — which is fireproof, the way Crimson and Warped are.
- A hemp **armour set**: beannie, shirt, harem pants and flip-flops. It is not good armour.

### Odds and ends

Six paintings, a music disc, twenty-one advancements, and full translations in eight locales
(five English variants and three French, with regional vocabulary rather than copy-paste — Quebec
gets *gougounes* and its own slang).

---

## Building from source

Requires **JDK 21**.

```bash
./gradlew build          # the mod jar lands in build/libs/
./gradlew runClient      # a dev client with the mod loaded
./gradlew runDatagen     # regenerate models, recipes, loot tables, tags, advancements
```

Most of the mod's data is generated rather than hand-written; if you change a recipe, a loot table,
a tag or an advancement, run `runDatagen` and commit what it produces.

## Contributing

Issues and pull requests are welcome. If you are reporting a bug, the Minecraft version, the Fabric
Loader version and anything else in your mods folder are the three things that will help most.

## Licence

[GNU Affero General Public License v3.0](LICENSE).

## AI disclaimer

Large Language Models (LLM) chiefly Anthropic's **Claude Opus** and **Claude Sonnet**, used through
Claude Code, have been used in the making of this mod. Their role is assisting with more complex
features, helping for cross-mod compatibility, helping debug, documenting the code, update locale with local slangs (fr_ca or en_uk for example) generating placeholder textures, balancing mechanics,
and challenging and improving ideas.
