# Changelog

All notable changes to Hempdustry are recorded here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[Semantic Versioning](https://semver.org/). Jars are versioned `MAJOR.MINOR.PATCH+MCVERSION`, so
`2.0.0+1.21.11` is release 2.0.0 built for Minecraft 1.21.11. A pre-release carries its tag in the
same place: `2.0.0-beta+1.21.11` sorts *before* `2.0.0+1.21.11`, which is what lets a player on the
beta be told when the release lands.

## [2.1.0] — unreleased

### Added

- **Hash: the resin, taken off the plant.** A second way to smoke hemp, made the way the trade makes
  it: the powder that falls through a screen is **kief**, and kief pressed with heat is **hashish**.
  Hash is for smoking and only for smoking — nothing in the family goes into the Decarboxylator or
  makes cannabutter.
  - The **Sifting Box** is a tub with a screen in it, crafted like a composter from hemp plank
    slabs, with iron bars over hemp canvas in the middle. Fill it the way you fill a composter:
    seven buds, or about forty-seven fan leaves, and it hands back **nine kief**. Sift kief a second
    time for **filtered kief**, at about three kief to one. Plant matter and powder never share a
    screen.
  - **Wash it instead.** Fill a Sifting Box with a water bucket and put ice against all four of its
    sides, and it washes plant matter into **bubble hash**, with frost rising off it while it
    works. The ice sets the price: blue ice makes nine bubble hash from seven buds, packed ice from
    about nine, plain ice from about fourteen — and plain ice melts if you light the room, which
    stops the wash without a word. Build it in the dark, or buy packed ice.
  - The **Hemp Press** — eight hemp bricks around a piston — is a screw press heated from below,
    like the Infuser. It presses kief into **hashish**, bubble hash or filtered kief into **filtered
    hashish**, and filtered hashish into **rosin**, one piece at a time. It also presses a retted
    hemp stem into eight hemp fibre.
  - **Hashish** smokes for Night Vision, with Resistance, Slowness and Hunger. **Filtered hashish**
    gives the same high with half the chance of greening out — smoother, never stronger. Nine pieces
    pack into a **bar** you can set down on a shelf, and you cut pieces back off it with a sword or
    a knife: five cuts, nine pieces, and you watch it shrink as you go.
  - **Charas** is rubbed off a living plant. Shear a fully grown plant and there is a one-in-four
    chance of a pinch — once per plant, and the plant stays standing. Every rub also snips off one
    hemp leaf, which the plant gives one fewer of at harvest. Charas smokes for Slow Falling, and
    nine of it roll into a **charas ball** that you can pull apart again by hand.
  - **Rosin** is the most concentrated thing in the mod and the plainest to smoke: the hash high
    without its Night Vision. One piece fills a whole bong bowl. A pipe or a vaporizer will not take
    it.
  - **Moon rocks** are a bud, a rosin and a coat — hashish, kief, filtered hashish, bubble hash,
    filtered kief or charas — crafted together. A moon rock packs a bong with a full bowl of the
    bud's strain and the coat's effect on top: Night Vision from a brown coat, Night Vision with
    less chance of greening out from a blonde one, Slow Falling from charas. Bong only.
  - **Hash rolls into spliffs.** A pinch of hashish, filtered hashish or charas goes in with one or
    two buds. The spliff keeps the strain's name, and a line underneath says what else is in it.
- **The Redstone Vaporizer**, a fourth way to smoke. It heats the bud rather than burning it, which
  makes it the gentlest device in the mod: one bud a bowl, an effect at level I that lasts longer
  than a pipe's, and no way to green out. A finished bowl of buds hands the spent bud back as
  **scorched hemp**. Craft it from iron, redstone and hemp planks, and repair it with iron.
- **Scorched hemp**, and butter before the Decarboxylator. Smelt a fan leaf or a bud in a furnace
  and it comes out scorched. The Infuser takes it, at a price: four scorched count as one, so a
  batch never gets past the weakest butter however long it simmers, and every piece drags the grade
  down. It cannot be washed. It will vape one more time, for Slowness and Hunger only, and it
  composts.
- **Beldía**, a third strain: the Rif's landrace, the plant Moroccan hash was made from. It grows on
  sand — any sand, never farmland — with water within farmland's reach, or in a Grow Pot or a Hydro
  Tray. Take its water away and it stops growing, but it never dies. Its buds are resinous: each one
  counts twice in the Sifting Box, so four fill it. Smoked, it is **Mirage** — Invisibility on the
  hit, then Blindness for a moment as you exhale, Hunger, and twice the coughing.
  - Wild Beldía grows ripe on desert riverbanks and shores. Its seeds turn up in desert temple
    chests and in the suspicious sand of desert wells and pyramids, and never in grass.
- **Grow hemp indoors: a Grow Pot and a Grow Lamp.**
  - The **Grow Pot** is a planter of hemp brick with soil in it, for either strain. It is always
    watered, cannot be trampled, and a plant in it never competes with its neighbours, so pots can
    be packed wall to wall. Feed it bone meal — up to three — and a plant grows half again as fast
    as in the best field; each plant that ripens uses one up, and a spent pot grows like dry
    farmland. The soil shows dark while it is fed. A potted plant grows one stem short: its roots
    are boxed in. Picking the pot up keeps its soil.
  - The **Grow Lamp** is an LED panel in copper, hung from the ceiling on copper chains, with
    amethyst-lensed diodes. It switches on like a redstone lamp — power the ceiling, or put a lever
    on it. A lit lamp sheds a faint violet haze, and shader packs that read LabPBR materials make
    its diodes glow. Hang it one to three blocks above your plants: every plant in the 3×3 below that grows up
    under it harvests **two extra buds** and an extra leaf, gives a second leaf every time you trim
    it, and grows a little faster while young.
  - **Other lights help too, less.** A lit redstone lamp, copper bulb or modded lamp directly above
    a plant is worth one extra bud and a leaf; glowstone, sea lanterns, lanterns and other full-
    brightness blocks, an even chance of one. Torches and campfires do nothing.
  - **The plant remembers its light.** What counts is the light it grew under the whole way, and
    the weakest of it — hanging a lamp over a finished plant does nothing, and one lamp cannot be
    carried from plant to plant. **Lose the light while a plant is flowering and it stresses**: a
    bud short at harvest, and two seeds in its place. Plants grown under any lamp give charas
    half as often; charas is a field craft. A lamp-grown plant looks a shade deeper green, and a
    **stressed one is drawn differently**: its colour drains — Purple Kush's purple fades to a
    sickly green — the oldest leaves yellow and die back brown, and yellow anthers show in the
    flowers, which is where the seeds come from.
- **A Hydro Tray, for growing at twice the pace.** Craft it from a Grow Pot, four copper ingots and
  a redstone dust. Fill it with a **water bucket**, mix in **fertiliser**, and give it a **redstone
  signal** — that is the pump — and hemp in it grows **twice as fast as the best field** (about 12
  minutes for Purple Kush, 16 for Lemon Haze), with trays packed wall to wall. You can see all
  three at a glance: the sight tube on its front shows the water level and turns from blue to
  green once it is fed, and the pump's light comes on and bubbles rise through the clay pebbles while
  it runs. A dispenser can fill it with water, and a dispenser
  of bone meal can feed it.
  - **Each plant that ripens drinks a third of the water**, and the fertiliser drains with the last
    of it — so the rhythm is: fill, feed, three plants.
  - **It is unforgiving, not dangerous.** Plain water or a stopped pump grows at an ordinary field's
    pace; a dry tray is slower than plain dirt. **Nothing ever dies of neglect** — that is vanilla's
    rule for farmland too. A plant in a tray harvests one stem short, like one in a pot, and a tray
    you pick up comes back empty.
  - **No hydroponics in the Nether.** Water boils away there — pour it in and it hisses off as usual.
  - **Any fertiliser feeds either bed.** Bone meal, or whatever your modpack tags as fertiliser —
    pot and tray both read the shared `#c:fertilizers` tag.
- **Bongs in every colour of glass.** Craft a bong from stained glass and the matching stained pane
  for any of the sixteen colours, or from tinted glass and a plain pane for a smoked-glass one. They
  pack, smoke and wear out exactly like the clear bong, repair with the glass they are made of, and
  share one button in the recipe book. A packed one is still called "Purple Kush Bong" — the colour
  is right there on it.
- **Set a bong down.** Right-click a block with an empty bong and it stands there, like a flower
  pot; sneak and right-click to put down a packed one, bowl and all. It breaks in one hit and always
  drops back exactly as it went down — packed, worn, enchanted or named. It is for show: to smoke
  it, pick it back up.
- **The bong is 3D in your hand**, and stays the familiar flat icon in your inventory, on the ground
  and in item frames — the way the trident and the spyglass do it.
  - For resource packs: every glass has its own item sprite (`textures/item/<colour>_bong.png`) and
    block texture (`textures/block/<colour>_bong.png`), and the placed and held bong is one model,
    `models/block/bong_template.json`.
  - *Bong Voyage* and *Burnout* count a bong of any colour.
- **A cannabis leaf banner pattern.** Put a hemp leaf in the loom's pattern slot — you keep it — and
  paint a seven-leaflet leaf onto a banner or a shield, in any of the sixteen colours.

### Changed

- **A bong hit is drawn, not clicked.** Hold use with a packed bong and it comes up to your mouth
  for a second and a half: the water bubbles, smoke gathers in the chamber and climbs the neck, a
  wisp rises from the bowl, and the hit lands when the draw is done. Let go early and nothing is
  spent — no charge, no durability — and the bubbling stops when you do. The pipe and the vaporizer
  still hit on a single click.
- **Creepers are drawn to hemp.** Now and then a creeper wanders into a grown field under open sky,
  prowls it for a while and moves on — only some creepers ever do, and only one to a field at a
  time. A creeper standing in a grown field is very hard to see, so a big farm is worth walling and
  lighting. Seedlings and roofed farms draw nothing. `creepersSeekHemp` in the config's `world`
  section turns it off.
- **Purple Kush's colour is a brighter violet**, so a packed Purple Kush load shows up on every
  device.
- **Level III is back, but only by pairing.** 2.0.1 stopped every helpful effect at level II. In
  2.1.0 a bud smoked together with a concentrate — hashish, charas, filtered hashish or rosin, in a
  spliff, a device or a moon rock — lifts that bud's helpful effects one level past the cap, to III.
  Only once: two concentrates do not make IV, and the concentrate's own effects stay at II. The price
  is a full green-out at a dose of three, which a bud-and-hash load easily is.
- **The munchies scale on hash too.** Every hash, rosin, Beldía and scorched hemp now gives Hunger
  that grows with the dose, as the plants do since 2.0.1.
- **The Sifting Box says why it refused something.** A filled box with no ice around it tells you
  the wash needs ice on all four sides, and a dry screen tells you it will not mix plant matter with
  kief. Shears on a plant already rubbed for charas say so.

### Notes for pack makers

- **The level-III cap is a knob.** `maxBuffLevel` in the config's `effects` section (default 2) is
  the plain cap; pairing a bud with a concentrate goes one past it. Raise it for a stronger mod.
- **Hash is data.** Hashish, filtered hashish, charas and rosin are strains with no seeds and no
  flower, so a datapack rebalances them — or adds a resin of its own — in
  `data/<namespace>/hempdustry/strain/` like any other strain. Strains gain three optional fields,
  `dose_per_item`, `green_out_factor` and `cough_factor`, and each effect two more,
  `duration_factor` and `on_exhale`; `seeds` and `flower` are optional now. Every strain file
  written for 2.0 loads unchanged.
- **A new recipe type, `hempdustry:pressing`**: one item in, one out. All five Hemp Press recipes
  are ordinary JSON. The Infuser's `infusing` recipe gains an optional `scorched_hemp` field, and a
  recipe written for 2.0 still loads.
- **New tags.** `#hempdustry:siftable/flower`, `siftable/trim`, `siftable/resinous` and
  `siftable/kief` decide what the Sifting Box takes and how fast it fills.
  `#hempdustry:hash_cutters` is what cuts a bar — swords, and `#c:tools/knife` for modded knives.
  `#hempdustry:grow_lamps` (a block tag) is what counts as a Grow Lamp, and
  `#hempdustry:not_grow_lights` is what is bright but never lights a plant — fire, campfires and
  lava. The biome tag `#hempdustry:beldia_gen` is where wild Beldía grows, and the leaf's banner
  pattern hangs off `#hempdustry:pattern_item/hemp_leaf`, so a datapack can add another. Both beds
  are fed from `#c:fertilizers`.
- **A placement modifier, `hempdustry:in_beldia_biome`**, checks every plant of a patch rather than
  the patch's centre, because a patch spreads past the edge of its biome.

## [2.0.3] — 2026-10-05

### Fixed

- **Packing counts one bud per slot.** Crafting takes one item from each slot, but packing counted
  the whole stack: three buds stacked in one slot beside an empty bong packed a level-III bowl and
  used up one bud, in a crafting table or a Crafter alike. A bowl's level is now the number of slots
  holding buds.
- **Breaking a running Infuser gives back its batch.** A broken Infuser dropped only what was still
  in its slots: the hemp already drawn into the batch, up to 24 of it, was lost, and a finished tub
  dropped its cannabutter rather than handing back its hemp. Breaking the Infuser or the
  Decarboxylator also updates a comparator beside it again.
- **A world moved from Minecraft 1.21.1 keeps the items inside its machines.** Minecraft upgrades
  every item in a world that changes version, but it skipped the ones in a Decarboxylator or an
  Infuser, so an enchanted bow left in an oven's fuel slot arrived with no enchantments.
- **Hemp fibre repairs the hemp outfit on an anvil.** Nothing repaired it at all.
- **A datapack strain that reuses another strain's items no longer crashes the creative
  inventory.** The tab added those seeds, buds and flower a second time, which the game refuses:
  pressing E crashed the client, and the whole Hempdustry tab vanished from JEI. Each shows once now.
- **The bong and the smoke come from the smoker.** Both sounds were stereo, which Minecraft plays at
  full volume with no position, so everyone within 16 blocks heard every hit as if it were their
  own. They are mono now, and fade with distance like any other sound.
- **Bookmarking a Hempdustry page in JEI works.** It did nothing.
- **A very small `greenOutChanceMultiplier` no longer makes every hit a green-out.** A value just
  above 0 overflowed into certainty; it now means almost never, as it should.
- **`/hempdustry reload` no longer sends you to `/reload` for strains**, which never re-read them:
  strains are read once, when the world opens, so changing one takes a restart. The 2.0.0 notes below
  said `/reload` applies an edited strain; it does not.

## [2.0.2] — 2026-09-29

The first stable release. From here on, a world made with any version of Hempdustry 2 opens on every
later one, with nothing for you to do: nothing is renamed or removed, and a datapack or resource pack
that works keeps working.

### Breaking

- **The two potted hemp flowers no longer register an item.** `hempdustry:potted_indica_flower` and
  `hempdustry:potted_sativa_flower` were items absent from the creative tab and obtainable only with
  `/give`, though they showed in recipe viewers' item lists — vanilla has no item for any potted
  plant, because the plant is the item. If you gave yourself one it is gone. **Potted hemp flowers
  already placed in your world are untouched**, planting a wild flower in a pot works as before, and
  middle-clicking one still hands you the flower. Done now because 2.0.2 is the first stable release,
  after which no id can be removed.

### Fixed

- **Llamas wear hemp carpet.** On Minecraft 1.21.11 a llama refused it: the carpet was missing
  what the game now asks a carpet before a llama may wear it. It goes on a llama or a trader llama
  like any other carpet, and draws in hemp cloth.
- **Hemp trapdoors and potted hemp flowers drop when broken.** Breaking either gave back nothing
  at all; a trapdoor now drops itself, and a pot gives back the pot and the plant, as vanilla's do.

## [2.0.1-beta] — 2026-09-23

### Breaking

- **The two crop blocks no longer register an item.** `hempdustry:indica_crop` and
  `hempdustry:sativa_crop` were items with no model, absent from the creative tab and obtainable only
  with `/give` — vanilla wheat has no such item either, because the seeds are the item. If you gave
  yourself one it is gone. **Planted crops, the seeds and everything the plants drop are untouched**,
  and middle-clicking a growing plant still hands you its seeds. Done now rather than later because
  removing a registry id stops being allowed at 2.0.0 stable.
- **Hoppers and pipes can no longer put milk into the Infuser.** Its milk slot and bucket-return
  slot are gone (see *Changed*), so a hopper line that fed it milk buckets stops doing so. Point a
  **dispenser** at the tub instead: it pours the milk in and keeps the empty bucket, and a
  comparator behind the tub reads 0 once a batch is collected, so the dispenser can refill it on
  its own. Hemp still goes in by hopper exactly as before, and the spout still pours the
  cannabutter out. **Nothing in your world is lost** — a bucket a beta world left in either old slot
  pops out of the top of the tub the first time it loads.

### Added

- **A Russian translation**, by **MargoxaTheGamer** — the whole mod, translated by hand.
- **A German translation**, by **Gülps'sch420** — German, and Swiss German for Switzerland,
  translated by hand, with an Austrian locale adapted from her German. Twelve locales now ship:
  five English variants, three French, three German and Russian.
- **Recipe pages in [REI](https://modrinth.com/mod/rei)**, alongside the JEI ones the mod already
  had. Both viewers show the same three pages — Decarboxylator, Infuser and the cauldron's retting
  and washing — and the same packing rows in the crafting tab, because both read one description of
  what the mod does rather than two. Install either, neither, or the other one; nothing in the mod
  loads a line of viewer code unless the viewer is there.
  - The cauldron page is the one worth having: retting and washing are not recipes, so the recipe
    book cannot show them and no datapack can list them.
- **Three advancements for the kitchen**, which had foods the tree never mentioned. **Hemp Hearts**
  for eating anything made from hemp seed, **Got Bhang?** for drinking bhang, and **Club des
  Hashischins** for eating dawamesk — named after the 1840s Paris club whose members, Gautier and
  Baudelaire among them, ate exactly that. The seed foods share a new item tag,
  `#hempdustry:hemp_seed_foods`, so a datapack can add its own.
- **Shears tell you why they did nothing.** On a plant too young to trim, or one already trimmed in
  its current window, the action bar says so.
- **A ripe plant smells.** Now and then a faint wisp curls up off the top of a plant that is ready to
  harvest, so a finished field can be told from a growing one from across the farm. The ripe art
  still says the same thing on its own; the wisps are a second cue. They follow vanilla's Particles
  setting (Decreased thins them, Minimal hides them), and `ripeAroma: false` in the `client` section
  of `config/hempdustry.json` turns off just these. A plant with a block on its head gives off
  nothing. They cost the server nothing at all, and a client the same frame rate with or without
  them, even standing in a field of 26,000 ripe plants.
- **A Block of Hemp Leaves.** Nine hemp leaves pack into a block and come back out of it, the
  storage block the leaf never had; it sits beside the hemp bale in the creative tab. It behaves like
  leaves: quick to break with shears, a hoe or a sword, it burns, a piston pops it, and it composts.
  Like any leaves, a sapling or a huge mushroom that grows into it takes its place.
- **The bong sounds like a bong.** It bubbles on the hit and the inhale follows once the water
  clears, where it used to share the pipe's sound.

### Changed

- **Smoking and edibles are rebalanced so the strongest thing in the mod no longer arrives in the
  first hour.** A bong of three Lemon Haze used to give Speed III and Haste III, more than a full
  beacon, from some glass and three buds.
  - **No helpful effect goes above level II** — Speed, Haste, Resistance, Absorption and the rest,
    from smoking and from edibles alike. That is where a beacon stops.
  - **A third bud now makes the high last longer instead of stronger.** A bong or spliff of three
    lasts half as long again as one of two.
  - **The price still grows with the dose.** A bong of three is still Weakness III on Lemon Haze and
    Mining Fatigue III on Purple Kush, and **Hunger now grows with the dose too**, from I to III. It
    used to stay at I, which cost about one food point that a fed player never saw.
  - **A green-out at a dose of three is now a real one.** It ends every effect of a kind the mod
    hands out as a buff — Speed, Haste, Resistance, Absorption, Regeneration, and any buff a
    datapack strain adds — **whatever gave it to you**, a golden apple or a potion included. Effects
    the mod never gives, such as Fire Resistance, are left alone. It also empties your saturation,
    knocks you down for thirty seconds and keeps you from smoking for a minute. A green-out at a dose
    of two is the short dizzy spell it always was.
  - **Edibles last longer as they get stronger, rather than hitting harder.** Absorption stops at II,
    Resistance and Regeneration are level I, and a stronger butter makes the effect last longer.
    **Perfect cannabutter's edibles only ever slow you to Slowness I.** The munchies now last the
    whole high instead of a minute.
  - Server owners and modpack makers can raise the ceiling again: the new `maxBuffLevel` setting in
    `config/hempdustry.json` (default 2) caps only the helpful effects, and the strains are still a
    datapack.
- **Milk is poured into the Infuser by hand, like water into a cauldron.** Right-click the tub with
  a bucket of milk or hemp milk: it fills, and you get the empty back. Right-click with anything
  else — or with milk while it is already full — and the screen opens as before, so the pour and
  the screen share one button without getting in each other's way. In the screen, the two bucket
  slots are replaced by a milk indicator beside the flame, and the recipe pages in JEI and REI now
  say the milk is poured in. The tub still holds one milk and makes one cannabutter per batch.
- **Wild hemp is much harder to find.** Wild Purple Kush and Lemon Haze now turn up about a fifth as
  often as before, and a find is usually one to four plants rather than a field. Every so often you
  will still stumble on a big patch of six to eight. Only newly explored land is affected; wild
  plants already standing in your world stay where they are.
  - Bone meal on grass could also grow wild hemp, anywhere it grows wild — an endless free supply
    of seeds. It grows only vanilla flowers now.
  - Wild Purple Kush now really does grow on the moss of lush caves, a plant here and there. It was
    meant to from the start, but it was being planted before the moss was laid and never took.
- **Minecraft 1.21.11 is now the main line.** 2.0.0-beta shipped a 1.21.11 build beside the 1.21.1
  one; from here on, 1.21.11 is where new work lands. **Your world comes with you**: every item,
  block, recipe, tag, advancement and saved machine keeps the id and the save key it had, so a world
  made with either `2.0.0-beta` build opens on this one as itself.
  - **Still on 1.21.1?** That build is not going anywhere. The last version of that line is kept on
    its own `1.21.1` branch, can still be built and played, and gets anything that costs nothing to
    give it — the REI pages above landed on both. It is simply not where the mod is going.
  - Requires **Fabric Loader 0.18.2** and **Fabric API 0.139.4** or newer.
- **Purple Kush and Lemon Haze seeds no longer look alike.** Both strains shipped the same picture,
  so a hotbar could not tell one from the other. They keep the same scatter of seeds, each leaning
  towards its strain's colour: Purple Kush a cool violet-grey, Lemon Haze a warm olive.
- **Lava heats the Infuser.** Lava underneath works like a campfire, and a lava cauldron is the tidy
  way to do it: a full block that lasts for ever. `#hempdustry:heat_sources` had the magma block but
  not the lava itself.
- **The hemp bale composts now**, at hay's rate, as does the new Block of Hemp Leaves.
- **Every player gets the in-game update notice now, wherever they got the mod.** Updates on
  CurseForge, NexusMods and GitHub were always there to download, but the notice only asked Modrinth,
  where the mod is still in review, and its chat line only mentioned Modrinth, so it never fired. It
  now reads a small update file published with the mod, tells you once when a newer version is out,
  and its link opens the download page. `client.updateCheck: false` still turns it off.

### Removed

- **No EMI recipe pages on 1.21.11**, as in the 2.0.0-beta build for it, and not by choice: EMI has
  never been released for any Minecraft version past 1.21.1, so there is nothing for the plugin to
  attach to. **JEI is unaffected** and shows the same Decarboxylator, Infuser and cauldron pages it
  always did. The EMI plugin is kept intact on the `1.21.1` branch and comes straight back the day
  EMI ships for a version this mod targets.

### Fixed

- Hemp milk drank in complete silence, and bhang made an eating noise rather than a drinking one.
  Both now sound and animate like the drinks they are.
- The advancement tab drew its background as missing-texture magenta.
- Every block from the mod showed a raw id like `item.hempdustry.decarboxylator` in hand and in the
  creative tab instead of its name.
- The in-game update checker was asking Modrinth for builds for the wrong Minecraft version, so it
  would have quietly never found one. It now asks about the version you are actually running.

*(All four were introduced by the port and fixed before it shipped; none of them ever reached a
release build.)*

- **`cropGrowthMultiplier` did not do what it said.** Small settings were rounded away — 1.2 grew
  Purple Kush no faster at all — 1.5 and 2.0 grew it at the same speed, and some settings grew
  Lemon Haze faster than asked. It now scales the growth chance itself, so 2.0 really is twice as
  fast. **At the default of 1.0 nothing changes.**
- **Hemp plants floated a pixel above farmland.** They now sit in it, the way vanilla crops do.
- **JEI's recipe pages had lost every note under their recipes**: the fuel and cooking time,
  the Infuser's pouring, heat and batch notes, and the cauldron's counts per water level. On
  1.21.11 they were being drawn fully transparent. They show again, and each page is now wide
  enough to hold them in any language. REI always drew its notes, but ran them off the recipe and
  under its own corner button the same way; its pages widen to fit too.
- **Two hemp planks made six sticks from the recipe book, but four when laid in the grid by hand.**
  Hemp planks now make four sticks like any other planks, however they are crafted. For more sticks
  out of hemp, make them from stems.
- **A typo in `config/hempdustry.json` no longer wipes the file.** A file the mod could not read —
  broken JSON, or a word where a number goes — was written over with every default, taking all your
  other settings with it, and `/hempdustry reload` still said it had worked. Now the file is left
  exactly as it is, the settings already running stay, the log says what is wrong, and the reload
  command says it failed.
- **A hemp boat no longer spoils Minecraft's upgrade of the entities around it.** When a world moves
  to a newer Minecraft, the game rewrites every saved entity into the new format, and it could not
  read a hemp boat or hemp chest boat — so it skipped every entity in that chunk. Moving a world from
  1.21.1 to 1.21.11, an armour stand beside a hemp boat came out with no armour. The game now knows
  both boats, the entities around them are upgraded like any others, and what a hemp chest boat
  carries is upgraded too. Move a 1.21.1 world to 1.21.11 with this version or later, not with
  2.0.0-beta's 1.21.11 build.

### Known issues

- **With the Villager Trade Rebalance experiment turned on, five chest types hold nothing of ours.**
  That experiment ships as a datapack and replaces the desert pyramid, mineshaft, outpost, ancient
  city and jungle temple tables, and this mod deliberately never injects into a datapack's table —
  a pack that replaces a chest meant to replace it. So in such a world the mineshaft and outpost
  carry no hemp seeds. Every other chest, and tall grass, are unaffected, and the experiment is off
  by default.
- **A player's first join after updating may log
  `Tried to load unrecognized recipe: hempdustry:stick_from_planks removed now.`**, and a warning
  about the advancement that unlocked it. That is the six-stick recipe removed above; the game drops
  it from the recipe book and never mentions it again. It is harmless.

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
this is a hand-written rewrite for Fabric, and this was the first release on that line
(Minecraft 1.21.1, with a build for 1.21.11 beside it). The version
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

[2.1.0]: https://github.com/warlon-mhite/hempdustry-fabric/compare/v2.0.3...HEAD
[2.0.3]: https://github.com/warlon-mhite/hempdustry-fabric/releases/tag/v2.0.3
[2.0.2]: https://github.com/warlon-mhite/hempdustry-fabric/releases/tag/v2.0.2
[2.0.1-beta]: https://github.com/warlon-mhite/hempdustry-fabric/releases/tag/v2.0.1-beta
[2.0.0-beta]: https://github.com/warlon-mhite/hempdustry-fabric/releases/tag/beta
