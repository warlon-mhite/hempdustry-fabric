package com.warlonmhite.hempdustry.loot;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.Strain;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.ExplosionDecayLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.UniformLootNumberProvider;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

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
 * Tall plants drop loot exactly once per break (see {@code TallPlantBlock#onBreak}), so the grass
 * pool isn't double-rolled.
 *
 * <p>Hemp fibre in shipwreck supply chests, as cordage rather than as an on-ramp — see
 * {@link #SHIPWRECK_FIBER_CHANCE}.
 *
 * <p>And the Ganja disc in the two chests vanilla stocks its common discs in. Its creeper drop is
 * <em>not</em> here — that comes free from joining {@code #minecraft:creeper_drop_music_discs}
 * (see {@code ModItemTagProvider}), which the vanilla creeper table already rolls.
 */
public class ModLootTableModifiers {

    /** Chance per broken plant to drop a hemp seed. Kept very small on purpose. */
    private static final float GRASS_SEED_CHANCE = 0.01f; // 1%

    /** Chance per applicable chest to contain a hemp-seed stash. */
    private static final float CHEST_SEED_CHANCE = 0.30f;

    /**
     * Chance per applicable chest to contain the Ganja disc. Vanilla's 13/cat sit at weight 15 in
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

    /** Vanilla's own "player didn't use shears" gate — same one wheat seeds use on grass. */
    private static final LootCondition.Builder WITHOUT_SHEARS =
            MatchToolLootCondition.builder(ItemPredicate.Builder.create().items(Items.SHEARS)).invert();

    private static final Set<RegistryKey<LootTable>> GRASS_SOURCES = Set.of(
            Blocks.TALL_GRASS.getLootTableKey(),
            Blocks.LARGE_FERN.getLootTableKey());

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

    public static void modifyLootTables() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) {
                return; // don't touch datapack overrides, only vanilla/mod tables
            }
            if (GRASS_SOURCES.contains(key)) {
                RegistryEntry<Enchantment> fortune =
                        registries.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
                // One entry per active strain at equal weight inside a single roll: the *chance* of
                // finding a hemp seed stays GRASS_SEED_CHANCE no matter how many strains exist, and
                // which strain you get is a coin flip. (A pool picks exactly one of its entries.)
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(WITHOUT_SHEARS)
                        .conditionally(RandomChanceLootCondition.builder(GRASS_SEED_CHANCE));
                for (Strain strain : Strain.ACTIVE) {
                    pool.with(ItemEntry.builder(strain.seeds())
                            .apply(ApplyBonusLootFunction.uniformBonusCount(fortune, 2))
                            .apply(ExplosionDecayLootFunction.builder()));
                }
                tableBuilder.pool(pool);
            } else if (CHEST_SOURCES.contains(key)) {
                LootPool.Builder pool = LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(CHEST_SEED_CHANCE));
                for (Strain strain : Strain.ACTIVE) {
                    pool.with(ItemEntry.builder(strain.seeds())
                            .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 3))));
                }
                tableBuilder.pool(pool);
            }

            // Independent of the seed branch above — a shipwreck's supply chest is also a seed chest,
            // and a wreck holding both rope and a few seeds is exactly what a wreck should hold.
            if (FIBER_CHEST_SOURCES.contains(key)) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(SHIPWRECK_FIBER_CHANCE))
                        .with(ItemEntry.builder(ModItems.HEMP_FIBER)
                                .apply(SetCountLootFunction.builder(UniformLootNumberProvider.create(1, 4)))));
            }

            // Independent of the seed branch above — the two disc chests are also seed chests.
            if (DISC_CHEST_SOURCES.contains(key)) {
                tableBuilder.pool(LootPool.builder()
                        .rolls(ConstantLootNumberProvider.create(1))
                        .conditionally(RandomChanceLootCondition.builder(CHEST_DISC_CHANCE))
                        .with(ItemEntry.builder(ModItems.MUSIC_DISC_GANJA)));
            }
        });
    }
}
