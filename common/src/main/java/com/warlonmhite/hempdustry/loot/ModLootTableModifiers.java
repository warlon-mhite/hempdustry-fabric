package com.warlonmhite.hempdustry.loot;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.block.custom.RuderalisCropBlock;
import com.warlonmhite.hempdustry.block.custom.WarpedKushCropBlock;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.LocationPredicate;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LocationCheck;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import com.warlonmhite.hempdustry.block.custom.BeldiaCropBlock;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Runtime tweaks to vanilla loot tables.
 *
 * <p>A strain-agnostic hemp seed, as a low-key on-ramp into the mod:
 * <ul>
 *   <li>Tall grass &amp; large ferns drop one when broken, following the vanilla grass →
 *       wheat-seeds rule (not sheared, small chance, Fortune-boosted, explosion decay).</li>
 *   <li>A few exploration chests (shipwreck / dungeon / mineshaft / mansion / outpost) hold a small
 *       stash — vanilla already seeds crops into most of these, and the rest fit the theme.</li>
 *   <li>Plains and taiga village houses hold a few, as each vanilla village house holds its biome's
 *       crop ({@link #VILLAGE_HOUSE_SEED_CHANCE}); desert houses and the desert temple hold Beldía's.</li>
 * </ul>
 * <b>A tall plant rolls its loot table twice per break</b> — once for the half that was hit, once
 * more as the orphan pops off — so the grass pool carries vanilla's own two-halves guard. See
 * {@link #onlyTheHalfThatWasBroken}; without it the seed drops at double the shipped rate.
 *
 * <p>Hemp fibre and canvas in shipwreck supply chests, as cordage and sailcloth rather than as an
 * on-ramp — see {@link #SHIPWRECK_FIBER_CHANCE} — and a little schwag beside vanilla's poisonous
 * potatoes, see {@link #SHIPWRECK_SCHWAG_CHANCE}. Toasted hemp seed in a village fisher's chest, as
 * bait ({@link #FISHER_BAIT_WEIGHT}), and hemp fibre among fishing junk.
 *
 * <p>And the mod's music discs in the two chests vanilla stocks its common discs in. Their creeper drop is
 * <em>not</em> here — that comes free from joining {@code #minecraft:creeper_drop_music_discs}
 * (see {@code ModItemTagProvider}), which the vanilla creeper table already rolls.
 */
public class ModLootTableModifiers {

    /** Chance per broken plant to drop a hemp seed. Kept very small on purpose. */
    private static final float GRASS_SEED_CHANCE = 0.01f; // 1%

    /** Chance per applicable chest to contain a hemp-seed stash. */
    private static final float CHEST_SEED_CHANCE = 0.30f;

    /**
     * Chance per applicable chest to contain one of our discs. Vanilla's 13/cat sit at weight 15 in
     * those chests' main pool (~20% a given chest holds one); we can't slot into an existing pool
     * from a loot-table event, so this is a separate roll deliberately pitched a little rarer.
     */
    private static final float CHEST_DISC_CHANCE = 0.12f;

    /**
     * Chance a shipwreck's supply chest holds a coil of hemp fibre. Deliberately generous — this is
     * ship's stores, not treasure. Hemp <em>is</em> the historical naval fibre: the Corderie Royale
     * at Rochefort was built in 1666 under Louis XIV purely to make the navy's cordage, its rope-walk
     * turning out 200 m lengths over 20 cm thick, and the word "canvas" comes from "cannabis". A
     * wrecked ship with no rope aboard is the odd thing, not one with some.
     */
    private static final float SHIPWRECK_FIBER_CHANCE = 0.45f;

    /**
     * Chance a shipwreck's supply chest holds a few schwag. It is where vanilla keeps its own
     * poisonous potatoes (weight 7 of 84, two to six at a time), and pressed low-grade weed is what
     * smugglers really moved by boat. Pitched at about what three of those 84 weights would come to
     * over the pool's three to ten rolls, rather than the potato's seven: every schwag is still two
     * decarboxylated hemp in the oven, and a wreck should not be a butter farm.
     */
    private static final float SHIPWRECK_SCHWAG_CHANCE = 0.20f;

    /**
     * Chance a desert temple chest holds 1-3 Beldía seeds. A temple has four chests, so about 59% a
     * temple holds some: more likely than not, and the main way a player meets the plant.
     */
    private static final float DESERT_TEMPLE_SEED_CHANCE = 0.20f;

    /**
     * Weight of Beldía seeds inside the one pool of the desert well's and the desert pyramid's
     * brushing tables. Both total 8, so this is about 11% a brush -- a brick's or an emerald's share
     * -- and each sherd gets a ninth rarer. A weight, not a chance, so {@code loot.chanceMultiplier}
     * leaves it alone.
     *
     * <p><b>Inside the existing pool, never a pool of its own.</b> A brushed block keeps exactly one
     * item: {@code BrushableBlockEntity} logs "Expected max 1 loot from loot table" and drops the rest.
     */
    private static final int DESERT_ARCHAEOLOGY_SEED_WEIGHT = 1;
    /**
     * Chance a plains or taiga village house chest holds 1-3 hemp seeds. Vanilla stocks each village
     * house with its biome's crop — wheat seeds in a savanna house (10 of 46), pumpkin seeds in a taiga
     * one (5 of 54), beetroot seeds in a snowy one — and hemp was grown beside European villages for
     * its fibre, worked by village trades like the hemp comber; Marseille's Canebière is named for a
     * hemp field. <b>But kept rare</b> (Warlon Mhite): seeds are meant to be a find, so this is pitched
     * at a house's rare tier — what weight 1 of its main pool comes to over three to eight rolls, the
     * book's and the emerald's share — not at a common crop's. A roll of its own rather than a weight,
     * because both chests keep a second pool (the bundle) that an edit to every pool would reach.
     */
    private static final float VILLAGE_HOUSE_SEED_CHANCE = 0.10f;
    private static final Set<ResourceKey<LootTable>> VILLAGE_HOUSE_SOURCES = Set.of(
            BuiltInLootTables.VILLAGE_PLAINS_HOUSE,
            BuiltInLootTables.VILLAGE_TAIGA_HOUSE);
    /** Beldía seeds in a desert village house, where the desert's farmers live — as rare as the others. */
    private static final float DESERT_HOUSE_SEED_CHANCE = 0.10f;
    /**
     * Weight of toasted hemp seeds (1-3) in a village fisher's chest, beside vanilla's wheat seeds at 3
     * of 11 — vanilla's own nod to bait. Cooked hemp seed, <i>chènevis</i>, is the classic European
     * coarse-fishing bait, soaked and cooked for roach and bream: the toasted seed is that bait. The
     * chest has one pool, so it goes in as a weight.
     */
    private static final int FISHER_BAIT_WEIGHT = 2;
    /** Weight of hemp fibre in fishing junk, at string's own 5 of 110: an old line, a scrap of net. */
    private static final int FISHING_JUNK_FIBER_WEIGHT = 5;
    /**
     * Hemp canvas against hemp fibre inside the shipwreck's cordage roll, 1 to 3: sailcloth beside the
     * rope, both from the same stores, and still one roll so a wreck holds no more hemp than it did.
     */
    private static final int SHIPWRECK_FIBER_WEIGHT = 3;
    private static final int SHIPWRECK_CANVAS_WEIGHT = 1;

    private static final Set<ResourceKey<LootTable>> DESERT_ARCHAEOLOGY_SOURCES = Set.of(
            BuiltInLootTables.DESERT_WELL_ARCHAEOLOGY,
            BuiltInLootTables.DESERT_PYRAMID_ARCHAEOLOGY);

    /**
     * Chance a shipwreck's supply chest holds Industrial Hemp seed — the crop the rope and the sails
     * came from. Its own roll, beside the strain seed pool rather than in it, so a wreck's odds of a
     * smoking strain do not thin out. Pitched below that pool's 30%: Ditchweed is the main source.
     */
    private static final float SHIPWRECK_INDUSTRIAL_SEED_CHANCE = 0.25f;

    /**
     * Chance a bastion's treasure chest keeps a few Nether buds beside its gold. The piglins' grave
     * goods: a 2,700-year-old grave in western China held nearly two pounds of female flower beside a
     * warrior's bow, and the oldest braziers of burnt cannabis were in a cemetery. Placement is the
     * whole story -- vanilla never explains piglins.
     */
    private static final float BASTION_TREASURE_BUD_CHANCE = 0.5f;

    /**
     * Weight of Nether seeds in piglin bartering, against vanilla's 469: about 2.1% a gold ingot, an
     * ender pearl's odds. Vanilla already barters soul sand and nylium, so a Nether seed is its kind of
     * trade. A weight is not a chance, so {@code loot.chanceMultiplier} leaves it alone.
     */
    private static final int BARTER_SEED_WEIGHT = 10;

    /**
     * Weight of Ruderalis seeds inside the one pool of the common trail-ruins brushing table, which
     * holds 31 entries totalling 45 -- so 1 of 46, about 2.2% a brush. Vanilla already keeps wheat
     * and beetroot seeds there at weight 1 each, its own grammar for an old farmer's seed stock. Not
     * the rare table: that one is twelve collectibles, and a seed would dilute them.
     *
     * <p>A weight, not a chance, so {@code loot.chanceMultiplier} leaves it alone. And inside the
     * existing pool, never a pool of its own -- a brushed block keeps exactly one item.
     */
    private static final int TRAIL_RUINS_SEED_WEIGHT = 1;

    /**
     * Chance a sniffer's dig also turns up a Ruderalis seed. <b>A pool of its own, on top of
     * vanilla's</b>: that table is one roll between torchflower seeds and pitcher pod, and a third
     * entry would take both to a third -- and slow sniffer breeding with them, since
     * {@code #minecraft:sniffer_food} is torchflower seeds alone. So vanilla's even split is untouched
     * and a sniffer now and then digs up two things at once. With its eight-minute sniff cooldown
     * that is about one seed per sniffer per half hour: a key, never a supply.
     */
    private static final float SNIFFER_SEED_CHANCE = 0.25f;

    /**
     * Vanilla's own "player didn't use shears" gate — same one wheat seeds use on grass.
     *
     * <p>A method rather than a constant since 1.21.5: an item predicate names items through a
     * {@code HolderGetter}, which only exists once the registries are loaded.
     */
    private static LootItemCondition.Builder withoutShears(HolderLookup.Provider registries) {
        return MatchTool.toolMatches(ItemPredicate.Builder.item()
                .of(registries.lookupOrThrow(Registries.ITEM), Items.SHEARS)).invert();
    }

    /**
     * The two-block plants that carry a hemp seed, mapped to the block itself — the block is needed
     * to build {@link #onlyTheHalfThatWasBroken}, which has to name it.
     */
    private static final Map<ResourceKey<LootTable>, Block> GRASS_SOURCES = Map.of(
            Blocks.TALL_GRASS.getLootTable().orElseThrow(), Blocks.TALL_GRASS,
            Blocks.LARGE_FERN.getLootTable().orElseThrow(), Blocks.LARGE_FERN);

    private static final Set<ResourceKey<LootTable>> CHEST_SOURCES = Set.of(
            BuiltInLootTables.SHIPWRECK_SUPPLY,
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.ABANDONED_MINESHAFT,
            BuiltInLootTables.WOODLAND_MANSION,
            BuiltInLootTables.PILLAGER_OUTPOST);

    /** The Nether's own chests, where its seeds are found instead: a fortress, and a bastion's stores. */
    private static final Set<ResourceKey<LootTable>> NETHER_CHEST_SOURCES = Set.of(
            BuiltInLootTables.NETHER_BRIDGE,
            BuiltInLootTables.BASTION_OTHER);

    /** The two chests vanilla stocks its common (creeper-droppable) discs in — 13 and cat. */
    private static final Set<ResourceKey<LootTable>> DISC_CHEST_SOURCES = Set.of(
            BuiltInLootTables.SIMPLE_DUNGEON,
            BuiltInLootTables.WOODLAND_MANSION);

    /**
     * Supply chests only — cordage is stores and rigging, not valuables, so it has no business in a
     * shipwreck's treasure chest and nothing to do with the map chest.
     */
    private static final Set<ResourceKey<LootTable>> FIBER_CHEST_SOURCES = Set.of(
            BuiltInLootTables.SHIPWRECK_SUPPLY);

    /**
     * <b>Vanilla's guard against a two-block plant paying out twice, and it is load-bearing.</b>
     *
     * <p>Breaking one half of a tall plant rolls its loot table <em>twice</em>: once from
     * {@code DoublePlantBlock#playerWillDestroy} for the half the player hit, and once more as the orphaned half
     * pops off. Vanilla is immune because both of its pools pair "this is the half that was broken"
     * with "the other half is still standing" — true on the first roll, false on the second, since by
     * then the partner is gone. A pool injected without that pair fires on both, and the drop rate is
     * silently {@code 1-(1-p)²} instead of {@code p}.
     *
     * <p>Measured on a dedicated server, 2026-08-22: with the chance dialled to 10% for signal, an
     * explicit single roll gave 8.5% and a real block break gave 19.75% (n=400 each) — the double.
     * Both halves are covered rather than only the lower, so it makes no difference which end of the
     * plant the player swings at, exactly as vanilla's own two pools arrange.
     */
    private static LootItemCondition.Builder onlyTheHalfThatWasBroken(HolderLookup.Provider registries,
                                                                  Block plant) {
        return AnyOfCondition.anyOf(
                AllOfCondition.allOf(half(plant, DoubleBlockHalf.LOWER),
                        partnerAt(registries, plant, DoubleBlockHalf.UPPER, 1)),
                AllOfCondition.allOf(half(plant, DoubleBlockHalf.UPPER),
                        partnerAt(registries, plant, DoubleBlockHalf.LOWER, -1)));
    }

    /** "The block being broken is {@code plant}, on this half." */
    private static LootItemCondition.Builder half(Block plant, DoubleBlockHalf which) {
        return LootItemBlockStatePropertyCondition.hasBlockStateProperties(plant)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, which));
    }

    /** "{@code offsetY} away there is still a {@code plant} on the other half." */
    private static LootItemCondition.Builder partnerAt(HolderLookup.Provider registries, Block plant,
                                                   DoubleBlockHalf which, int offsetY) {
        return LocationCheck.checkLocation(
                LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block()
                        .of(registries.lookupOrThrow(Registries.BLOCK), plant)
                        .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(DoublePlantBlock.HALF, which))),
                new BlockPos(0, offsetY, 0));
    }

    /**
     * Whether these seeds plant Beldía, which keeps them out of tall grass and the generic chests and
     * puts them in the desert's. The lore is placement: a desert plant is found in the desert.
     *
     * <p><b>Code, not a tag, and it has to be</b> -- this and the two below. Item tags are not bound
     * yet while loot tables load: reading one here throws "Trying to access unbound tag" and takes
     * the server down with it. Nothing is lost either, since each of these plants needs a crop block
     * of its own, which is code no datapack can add.
     */
    private static boolean isDesertSeed(Item seeds) {
        return plants(seeds) instanceof BeldiaCropBlock;
    }

    /** Whether these seeds plant a Nether crop, found in the Nether's chests instead. */
    private static boolean isNetherSeed(Item seeds) {
        return plants(seeds) instanceof WarpedKushCropBlock;
    }

    /** Whether these seeds plant Ruderalis, which is only ever dug up: by a brush, or a sniffer. */
    private static boolean isBuriedSeed(Item seeds) {
        return plants(seeds) instanceof RuderalisCropBlock;
    }

    /**
     * Whether these seeds belong to one place, and so stay out of the tall grass and the generic
     * chests that every other strain shares. Each such strain is found where it belongs, and leaving
     * all of them out keeps the others' share of those rolls what it was.
     */
    private static boolean isRegionBound(Item seeds) {
        return isDesertSeed(seeds) || isNetherSeed(seeds) || isBuriedSeed(seeds);
    }

    /** The block these seeds plant, or null for an item that plants nothing. */
    private static Block plants(Item seeds) {
        return seeds instanceof BlockItem item ? item.getBlock() : null;
    }

    /**
     * One roll at {@code base} for 1-3 seeds of one plant strain: every strain whose seeds are
     * {@code found} there, at equal weight, so the chance stays {@code base} however many strains exist
     * and which you get is a coin flip. A region-bound plant is found only where it belongs -- Beldía
     * in the desert, Warped Kush in the Nether, Ruderalis underground -- and every other strain
     * everywhere else. Driven off the loaded strain registry, so a datapack strain appears here
     * without touching this file.
     */
    private static LootPool.Builder seedStash(HolderLookup.Provider registries, float base, Predicate<Item> found) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance(base)));
        for (Holder.Reference<Strain> strain : Strain.all(registries)) {
            strain.value().seedItem().filter(found)
                    .ifPresent(seeds -> pool.add(LootItem.lootTableItem(seeds)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
        }
        return pool;
    }

    /** A shipped chance after {@code loot.chanceMultiplier}, kept inside 0..1 whatever is configured. */
    private static float chance(float base) {
        return Mth.clamp((float) (base * HempdustryConfig.get().loot().chanceMultiplier()), 0.0F, 1.0F);
    }

    public static void modifyLootTables() {
        Services.EVENTS.onLootTableLoad((key, builtIn, registries, table) -> {
            if (!builtIn) {
                // Don't touch datapack overrides, only the game's and the mods' tables: a pack
                // curator who replaces a chest table meant to replace it, and an injection they cannot
                // see is not something to hand them. Mojang's experiment packs count as the game's --
                // Villager Trade Rebalance replaces the desert temple, mineshaft and outpost chests,
                // and a player who turns it on keeps the mod's loot in them (LootTableOrigin).
                return;
            }
            // A pack curator can override a loot *table* with a datapack, but not an injection like
            // this one -- which is exactly why the switch exists. Read per event rather than cached:
            // loot tables are rebuilt on /reload, so the setting takes effect with everything else.
            if (!HempdustryConfig.get().loot().enabled()) {
                return;
            }
            if (GRASS_SOURCES.containsKey(key)) {
                Holder<Enchantment> fortune =
                        registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
                // One entry per active strain at equal weight inside a single roll: the *chance* of
                // finding a hemp seed stays GRASS_SEED_CHANCE no matter how many strains exist, and
                // which strain you get is a coin flip. (A pool picks exactly one of its entries.)
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(onlyTheHalfThatWasBroken(registries, GRASS_SOURCES.get(key)))
                        .when(withoutShears(registries))
                        .when(LootItemRandomChanceCondition.randomChance(chance(GRASS_SEED_CHANCE)));
                // Driven off the loaded strain registry, so a datapack strain's seeds appear in
                // grass without touching this file.
                // Seedless strains — the hash family — are simply not in the pool: there is no
                // seed to find, and a pool entry per plant strain is what keeps the coin flip fair.
                // Nor is a region's own: Beldía is found where it grows, nothing from warped nylium
                // grows in a meadow's grass, and Ruderalis grows nowhere until it is dug up.
                for (Holder.Reference<Strain> strain : Strain.all(registries)) {
                    strain.value().seedItem().filter(seeds -> !isRegionBound(seeds))
                            .ifPresent(seeds -> pool.add(LootItem.lootTableItem(seeds)
                            .apply(ApplyBonusCount.addUniformBonusCount(fortune, 2))
                            .apply(ApplyExplosionDecay.explosionDecay())));
                }
                table.addPool(pool);
            } else if (CHEST_SOURCES.contains(key)) {
                table.addPool(seedStash(registries, CHEST_SEED_CHANCE, seeds -> !isRegionBound(seeds)));
            } else if (VILLAGE_HOUSE_SOURCES.contains(key)) {
                table.addPool(seedStash(registries, VILLAGE_HOUSE_SEED_CHANCE, seeds -> !isRegionBound(seeds)));
            } else if (key.equals(BuiltInLootTables.DESERT_PYRAMID)) {
                table.addPool(seedStash(registries, DESERT_TEMPLE_SEED_CHANCE, ModLootTableModifiers::isDesertSeed));
            } else if (key.equals(BuiltInLootTables.VILLAGE_DESERT_HOUSE)) {
                table.addPool(seedStash(registries, DESERT_HOUSE_SEED_CHANCE, ModLootTableModifiers::isDesertSeed));
            } else if (NETHER_CHEST_SOURCES.contains(key)) {
                table.addPool(seedStash(registries, CHEST_SEED_CHANCE, ModLootTableModifiers::isNetherSeed));
            } else if (DESERT_ARCHAEOLOGY_SOURCES.contains(key)) {
                // Into vanilla's one brushing pool, as a weighted entry beside the sherds.
                for (Holder.Reference<Strain> strain : Strain.all(registries)) {
                    strain.value().seedItem().filter(ModLootTableModifiers::isDesertSeed)
                            .ifPresent(seeds -> table.addToEveryPool(LootItem.lootTableItem(seeds)
                                    .setWeight(DESERT_ARCHAEOLOGY_SEED_WEIGHT)));
                }
            } else if (key.equals(BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON)) {
                // Into vanilla's one brushing pool, beside its wheat and beetroot seeds.
                for (Holder.Reference<Strain> strain : Strain.all(registries)) {
                    strain.value().seedItem().filter(ModLootTableModifiers::isBuriedSeed)
                            .ifPresent(seeds -> table.addToEveryPool(LootItem.lootTableItem(seeds)
                                    .setWeight(TRAIL_RUINS_SEED_WEIGHT)));
                }
            } else if (key.equals(BuiltInLootTables.SNIFFER_DIGGING)) {
                // A second pool, never a third entry in vanilla's -- see SNIFFER_SEED_CHANCE.
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(chance(SNIFFER_SEED_CHANCE)));
                for (Holder.Reference<Strain> strain : Strain.all(registries)) {
                    strain.value().seedItem().filter(ModLootTableModifiers::isBuriedSeed)
                            .ifPresent(seeds -> pool.add(LootItem.lootTableItem(seeds)));
                }
                table.addPool(pool);
            } else if (key.equals(BuiltInLootTables.BASTION_TREASURE)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(chance(BASTION_TREASURE_BUD_CHANCE)));
                for (Holder.Reference<Strain> strain : Strain.all(registries)) {
                    Strain value = strain.value();
                    if (value.seedItem().filter(seeds -> isNetherSeed(seeds)).isPresent()) {
                        pool.add(LootItem.lootTableItem(value.buds())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))));
                    }
                }
                table.addPool(pool);
            } else if (key.equals(BuiltInLootTables.PIGLIN_BARTERING)) {
                // Into vanilla's one bartering pool, as a weighted entry beside soul sand and the rest,
                // rather than a second pool -- which would hand one over on top of every barter.
                for (Holder.Reference<Strain> strain : Strain.all(registries)) {
                    strain.value().seedItem().filter(seeds -> isNetherSeed(seeds))
                            .ifPresent(seeds -> table.addToEveryPool(LootItem.lootTableItem(seeds)
                                    .setWeight(BARTER_SEED_WEIGHT)
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
                }
            }

            // Independent of the seed branch above — a shipwreck's supply chest is also a seed chest,
            // and a wreck holding both rope and a few seeds is exactly what a wreck should hold.
            if (FIBER_CHEST_SOURCES.contains(key)) {
                table.addPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(chance(SHIPWRECK_FIBER_CHANCE)))
                        .add(LootItem.lootTableItem(ModItems.HEMP_FIBER)
                                .setWeight(SHIPWRECK_FIBER_WEIGHT)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4))))
                        .add(LootItem.lootTableItem(ModItems.HEMP_CANVAS)
                                .setWeight(SHIPWRECK_CANVAS_WEIGHT)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
                table.addPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(chance(SHIPWRECK_SCHWAG_CHANCE)))
                        .add(LootItem.lootTableItem(ModItems.SCHWAG)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4)))));
                table.addPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(chance(SHIPWRECK_INDUSTRIAL_SEED_CHANCE)))
                        .add(LootItem.lootTableItem(ModItems.INDUSTRIAL_HEMP_SEEDS)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3)))));
            }

            // Both of these have exactly one pool, so the hemp goes in beside vanilla's own entries
            // as a weight, the way the desert brushing tables take Beldía.
            if (key.equals(BuiltInLootTables.VILLAGE_FISHER)) {
                table.addToEveryPool(LootItem.lootTableItem(ModItems.TOASTED_HEMP_SEEDS)
                        .setWeight(FISHER_BAIT_WEIGHT)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))));
            }
            if (key.equals(BuiltInLootTables.FISHING_JUNK)) {
                table.addToEveryPool(LootItem.lootTableItem(ModItems.HEMP_FIBER)
                        .setWeight(FISHING_JUNK_FIBER_WEIGHT));
            }
            // Independent of the seed branch above — the two disc chests are also seed chests.
            // One entry per disc at equal weight inside a single roll, the same shape as the seed
            // pool above: the *chance* of finding one of our discs stays CHEST_DISC_CHANCE however
            // many we ship, and which one you get is a coin flip.
            if (DISC_CHEST_SOURCES.contains(key)) {
                LootPool.Builder pool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(chance(CHEST_DISC_CHANCE)));
                for (Item disc : ModItems.MUSIC_DISCS) {
                    pool.add(LootItem.lootTableItem(disc));
                }
                table.addPool(pool);
            }
        });
    }
}
