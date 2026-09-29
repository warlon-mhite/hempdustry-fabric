package com.warlonmhite.hempdustry.loot;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import com.warlonmhite.hempdustry.block.custom.BeldiaCropBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.AllOfLootCondition;
import net.minecraft.loot.condition.AnyOfLootCondition;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.LocationCheckLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.ExplosionDecayLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.predicate.BlockPredicate;
import net.minecraft.predicate.entity.LocationPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.Map;
import java.util.Set;

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
    private static final Set<RegistryKey<LootTable>> VILLAGE_HOUSE_SOURCES = Set.of(
            LootTables.VILLAGE_PLAINS_CHEST,
            LootTables.VILLAGE_TAIGA_HOUSE_CHEST);
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

    private static final Set<RegistryKey<LootTable>> DESERT_ARCHAEOLOGY_SOURCES = Set.of(
            LootTables.DESERT_WELL_ARCHAEOLOGY,
            LootTables.DESERT_PYRAMID_ARCHAEOLOGY);

    /**
     * Vanilla's own "player didn't use shears" gate — same one wheat seeds use on grass.
     *
     * <p>A method rather than a constant since 1.21.5: an item predicate names items through a
     * {@code RegistryEntryLookup}, which only exists once the registries are loaded.
     */
    private static LootCondition.Builder withoutShears(RegistryWrapper.WrapperLookup registries) {
        return MatchToolLootCondition.builder(ItemPredicate.Builder.create()
                .items(registries.getOrThrow(RegistryKeys.ITEM), Items.SHEARS)).invert();
    }

    /**
     * The two-block plants that carry a hemp seed, mapped to the block itself — the block is needed
     * to build {@link #onlyTheHalfThatWasBroken}, which has to name it.
     */
    private static final Map<RegistryKey<LootTable>, Block> GRASS_SOURCES = Map.of(
            Blocks.TALL_GRASS.getLootTableKey().orElseThrow(), Blocks.TALL_GRASS,
            Blocks.LARGE_FERN.getLootTableKey().orElseThrow(), Blocks.LARGE_FERN);

    private static final Set<RegistryKey<LootTable>> CHEST_SOURCES = Set.of(
            LootTables.SHIPWRECK_SUPPLY_CHEST,
            LootTables.SIMPLE_DUNGEON_CHEST,
            LootTables.ABANDONED_MINESHAFT_CHEST,
            LootTables.WOODLAND_MANSION_CHEST,
            LootTables.PILLAGER_OUTPOST_CHEST);

    /** The two chests vanilla stocks its common (creeper-droppable) discs in — 13 and cat. */
    private static final Set<RegistryKey<LootTable>> DISC_CHEST_SOURCES = Set.of(
            LootTables.SIMPLE_DUNGEON_CHEST,
            LootTables.WOODLAND_MANSION_CHEST);

    /**
     * Supply chests only — cordage is stores and rigging, not valuables, so it has no business in a
     * shipwreck's treasure chest and nothing to do with the map chest.
     */
    private static final Set<RegistryKey<LootTable>> FIBER_CHEST_SOURCES = Set.of(
            LootTables.SHIPWRECK_SUPPLY_CHEST);

    /**
     * <b>Vanilla's guard against a two-block plant paying out twice, and it is load-bearing.</b>
     *
     * <p>Breaking one half of a tall plant rolls its loot table <em>twice</em>: once from
     * {@code TallPlantBlock#onBreak} for the half the player hit, and once more as the orphaned half
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
    private static LootCondition.Builder onlyTheHalfThatWasBroken(RegistryWrapper.WrapperLookup registries,
                                                                  Block plant) {
        return AnyOfLootCondition.builder(
                AllOfLootCondition.builder(half(plant, DoubleBlockHalf.LOWER),
                        partnerAt(registries, plant, DoubleBlockHalf.UPPER, 1)),
                AllOfLootCondition.builder(half(plant, DoubleBlockHalf.UPPER),
                        partnerAt(registries, plant, DoubleBlockHalf.LOWER, -1)));
    }

    /** "The block being broken is {@code plant}, on this half." */
    private static LootCondition.Builder half(Block plant, DoubleBlockHalf which) {
        return BlockStatePropertyLootCondition.builder(plant)
                .properties(StatePredicate.Builder.create().exactMatch(TallPlantBlock.HALF, which));
    }

    /** "{@code offsetY} away there is still a {@code plant} on the other half." */
    private static LootCondition.Builder partnerAt(RegistryWrapper.WrapperLookup registries, Block plant,
                                                   DoubleBlockHalf which, int offsetY) {
        return LocationCheckLootCondition.builder(
                LocationPredicate.Builder.create().block(BlockPredicate.Builder.create()
                        .blocks(registries.getOrThrow(RegistryKeys.BLOCK), plant)
                        .state(StatePredicate.Builder.create().exactMatch(TallPlantBlock.HALF, which))),
                new BlockPos(0, offsetY, 0));
    }

    /**
     * Whether these seeds plant Beldía, which keeps them out of tall grass and the generic chests and
     * puts them in the desert's. The lore is placement: a desert plant is found in the desert.
     *
     * <p><b>Code, not a tag, and it has to be.</b> Item tags are not bound yet while loot tables load
     * -- reading one here throws "Trying to access unbound tag" and takes the server down with it.
     */
    private static boolean isDesertSeed(Item seeds) {
        return seeds instanceof BlockItem item && item.getBlock() instanceof BeldiaCropBlock;
    }

    /**
     * One roll at {@code base} for 1-3 seeds of one plant strain: every strain with seeds at equal
     * weight, so the chance stays {@code base} however many strains exist and which you get is a coin
     * flip. {@code desert} picks Beldía's seeds alone, and otherwise every strain but Beldía's — the
     * desert plant is found in the desert. Driven off the loaded strain registry, so a datapack strain
     * appears here without touching this file.
     */
    private static LootPool.Builder seedStash(RegistryWrapper.WrapperLookup registries, float base, boolean desert) {
        LootPool.Builder pool = LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .conditionally(RandomChanceLootCondition.builder(chance(base)));
        for (RegistryEntry.Reference<Strain> strain : Strain.all(registries)) {
            strain.value().seeds().filter(seeds -> isDesertSeed(seeds) == desert)
                    .ifPresent(seeds -> pool.with(ItemEntry.builder(seeds)
                            .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))));
        }
        return pool;
    }

    /** A shipped chance after {@code loot.chanceMultiplier}, kept inside 0..1 whatever is configured. */
    private static float chance(float base) {
        return MathHelper.clamp((float) (base * HempdustryConfig.get().loot().chanceMultiplier()), 0.0F, 1.0F);
    }

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                // Don't touch datapack overrides, only vanilla/mod tables: a pack curator who
                // replaces a chest table meant to replace it, and an injection they cannot see is
                // not something to hand them.
                //
                // KNOWN COST, and do not "fix" it by deleting this guard. Mojang's experimental
                // feature packs are datapacks too, so Villager Trade Rebalance -- which replaces
                // desert_pyramid, abandoned_mineshaft, pillager_outpost, ancient_city and
                // jungle_temple -- arrives here as DATA_PACK, and in such a world those five chests
                // hold none of ours. LootTableSource has four values (VANILLA, MOD, DATA_PACK,
                // REPLACED) and none of them tells a Mojang feature pack from a third party's, so
                // there is no targeted fix through this API; dropping the guard would inject into
                // every pack's deliberate override instead, which is the worse of the two.
                // Fabric's game-test world turns every experiment on, which is how this surfaced.
                return;
            }
            // A pack curator can override a loot *table* with a datapack, but not an injection like
            // this one -- which is exactly why the switch exists. Read per event rather than cached:
            // loot tables are rebuilt on /reload, so the setting takes effect with everything else.
            if (!HempdustryConfig.get().loot().enabled()) {
                return;
            }
            if (GRASS_SOURCES.containsKey(key)) {
                RegistryEntry<Enchantment> fortune =
                        registries.getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
                // One entry per active strain at equal weight inside a single roll: the *chance* of
                // finding a hemp seed stays GRASS_SEED_CHANCE no matter how many strains exist, and
                // which strain you get is a coin flip. (A pool picks exactly one of its entries.)
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(onlyTheHalfThatWasBroken(registries, GRASS_SOURCES.get(key)))
                        .conditionally(withoutShears(registries))
                        .conditionally(RandomChanceLootCondition.builder(chance(GRASS_SEED_CHANCE)));
                // Driven off the loaded strain registry, so a datapack strain's seeds appear in
                // grass without touching this file.
                // Seedless strains — the hash family — are simply not in the pool: there is no
                // seed to find, and a pool entry per plant strain is what keeps the coin flip fair.
                // Nor is the desert's: Beldía is found where it grows, and leaving it out keeps the
                // other strains' share of this roll what it was.
                for (RegistryEntry.Reference<Strain> strain : Strain.all(registries)) {
                    strain.value().seeds().filter(seeds -> !isDesertSeed(seeds))
                            .ifPresent(seeds -> pool.with(ItemEntry.builder(seeds)
                            .apply(ApplyBonusLootFunction.uniformBonusCount(fortune, 2))
                            .apply(ExplosionDecayLootFunction.builder())));
                }
                tableBuilder.pool(pool);
            } else if (CHEST_SOURCES.contains(key)) {
                tableBuilder.pool(seedStash(registries, CHEST_SEED_CHANCE, false));
            } else if (VILLAGE_HOUSE_SOURCES.contains(key)) {
                tableBuilder.pool(seedStash(registries, VILLAGE_HOUSE_SEED_CHANCE, false));
            } else if (key.equals(LootTables.DESERT_PYRAMID_CHEST)) {
                tableBuilder.pool(seedStash(registries, DESERT_TEMPLE_SEED_CHANCE, true));
            } else if (key.equals(LootTables.VILLAGE_DESERT_HOUSE_CHEST)) {
                tableBuilder.pool(seedStash(registries, DESERT_HOUSE_SEED_CHANCE, true));
            } else if (DESERT_ARCHAEOLOGY_SOURCES.contains(key)) {
                // Into vanilla's one brushing pool, as a weighted entry beside the sherds.
                tableBuilder.modifyPools(pool -> {
                    for (RegistryEntry.Reference<Strain> strain : Strain.all(registries)) {
                        strain.value().seeds().filter(ModLootTableModifiers::isDesertSeed)
                                .ifPresent(seeds -> pool.with(ItemEntry.builder(seeds)
                                        .weight(DESERT_ARCHAEOLOGY_SEED_WEIGHT)));
                    }
                });
            }

            // Independent of the seed branch above — a shipwreck's supply chest is also a seed chest,
            // and a wreck holding both rope and a few seeds is exactly what a wreck should hold.
            if (FIBER_CHEST_SOURCES.contains(key)) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(chance(SHIPWRECK_FIBER_CHANCE)))
                        .with(ItemEntry.builder(ModItems.HEMP_FIBER)
                                .weight(SHIPWRECK_FIBER_WEIGHT)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 4))))
                        .with(ItemEntry.builder(ModItems.HEMP_CANVAS)
                                .weight(SHIPWRECK_CANVAS_WEIGHT)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))));
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(chance(SHIPWRECK_SCHWAG_CHANCE)))
                        .with(ItemEntry.builder(ModItems.SCHWAG)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 4)))));
            }

            // Both of these have exactly one pool, so the hemp goes in beside vanilla's own entries
            // as a weight, the way the desert brushing tables take Beldía.
            if (key.equals(LootTables.VILLAGE_FISHER_CHEST)) {
                tableBuilder.modifyPools(pool -> pool.with(ItemEntry.builder(ModItems.TOASTED_HEMP_SEEDS)
                        .weight(FISHER_BAIT_WEIGHT)
                        .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))));
            }
            if (key.equals(LootTables.FISHING_JUNK_GAMEPLAY)) {
                tableBuilder.modifyPools(pool -> pool.with(ItemEntry.builder(ModItems.HEMP_FIBER)
                        .weight(FISHING_JUNK_FIBER_WEIGHT)));
            }
            // Independent of the seed branch above — the two disc chests are also seed chests.
            // One entry per disc at equal weight inside a single roll, the same shape as the seed
            // pool above: the *chance* of finding one of our discs stays CHEST_DISC_CHANCE however
            // many we ship, and which one you get is a coin flip.
            if (DISC_CHEST_SOURCES.contains(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(chance(CHEST_DISC_CHANCE)));
                for (Item disc : ModItems.MUSIC_DISCS) {
                    pool.with(ItemEntry.builder(disc));
                }
                tableBuilder.pool(pool);
            }
        });
    }
}
