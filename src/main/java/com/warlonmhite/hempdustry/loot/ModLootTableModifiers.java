package com.warlonmhite.hempdustry.loot;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
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
 * </ul>
 * <b>A tall plant rolls its loot table twice per break</b> — once for the half that was hit, once
 * more as the orphan pops off — so the grass pool carries vanilla's own two-halves guard. See
 * {@link #onlyTheHalfThatWasBroken}; without it the seed drops at double the shipped rate.
 *
 * <p>Hemp fibre in shipwreck supply chests, as cordage rather than as an on-ramp — see
 * {@link #SHIPWRECK_FIBER_CHANCE}.
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

    /** A shipped chance after {@code loot.chanceMultiplier}, kept inside 0..1 whatever is configured. */
    private static float chance(float base) {
        return MathHelper.clamp((float) (base * HempdustryConfig.get().loot().chanceMultiplier()), 0.0F, 1.0F);
    }

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                return; // don't touch datapack overrides, only vanilla/mod tables
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
                for (RegistryEntry.Reference<Strain> strain : Strain.all(registries)) {
                    strain.value().seeds().ifPresent(seeds -> pool.with(ItemEntry.builder(seeds)
                            .apply(ApplyBonusLootFunction.uniformBonusCount(fortune, 2))
                            .apply(ExplosionDecayLootFunction.builder())));
                }
                tableBuilder.pool(pool);
            } else if (CHEST_SOURCES.contains(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(chance(CHEST_SEED_CHANCE)));
                for (RegistryEntry.Reference<Strain> strain : Strain.all(registries)) {
                    strain.value().seeds().ifPresent(seeds -> pool.with(ItemEntry.builder(seeds)
                            .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3)))));
                }
                tableBuilder.pool(pool);
            }

            // Independent of the seed branch above — a shipwreck's supply chest is also a seed chest,
            // and a wreck holding both rope and a few seeds is exactly what a wreck should hold.
            if (FIBER_CHEST_SOURCES.contains(key)) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(chance(SHIPWRECK_FIBER_CHANCE)))
                        .with(ItemEntry.builder(ModItems.HEMP_FIBER)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 4)))));
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
