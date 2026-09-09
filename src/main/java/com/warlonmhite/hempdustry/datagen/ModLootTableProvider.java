package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.HashishBarBlock;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.AnyOfLootCondition;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.entry.LeafEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.property.IntProperty;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ModLootTableProvider extends FabricBlockLootTableProvider {
    /**
     * Breaking a hashish bar hands back what is still in it: the whole bar if it is uncut, and the
     * pieces that have not been cut off yet if it is not.
     *
     * <p>Vanilla's cake drops nothing at all, which would be punishing here for no reason — the bar
     * is a stash, and a misplaced pickaxe should not delete a week of sifting. Nothing dupes either:
     * the counts are exactly what is left after the cuts already taken.
     *
     * <p>One pool per cut count, each with a mutually exclusive condition, rather than one pool of
     * alternatives. Same output, and each row reads as its own line.
     *
     * <p>Parameterised because the two bars want the identical table with a different piece in it —
     * which is the same reason they are the same block class with one method overridden.
     */
    private LootTable.Builder hashishBarDrops(Block bar, Item piece) {
        LootTable.Builder table = LootTable.builder().pool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .conditionally(cutsExactly(bar, 0))
                .with(ItemEntry.builder(bar)));
        for (int cuts = 1; cuts <= 4; cuts++) {
            table.pool(LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .conditionally(cutsExactly(bar, cuts))
                    .with(ItemEntry.builder(piece).apply(SetCountLootFunction.builder(
                            ConstantLootNumberProvider.create(HashishBarBlock.remaining(cuts))))));
        }
        return table;
    }

    /**
     * The condition names the block explicitly, so the two bars cannot share one by accident —
     * {@code block_state_property} checks the block as well as the state, and a table pointing at
     * the wrong one would simply never fire and drop nothing at all.
     */
    private static LootCondition.Builder cutsExactly(Block bar, int cuts) {
        return BlockStatePropertyLootCondition.builder(bar)
                .properties(StatePredicate.Builder.create().exactMatch(HashishBarBlock.CUTS, cuts));
    }

    /**
     * Buds dropped by an untrimmed mature plant, and the Fortune curve on them — <b>deliberately
     * identical for every strain</b>, which is why they're constants here and not per-crop
     * parameters. A strain is told apart by what its buds <em>do</em> when smoked and by the
     * leaf/stem split below, never by how many buds it hands over.
     */
    private static final int BUDS_AT_ZERO_CUTS = 2;
    private static final float BUDS_FORTUNE_CHANCE = 0.20F;

    public ModLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        addDrop(ModBlocks.HEMP_BRICKS_BLOCK);
        addDrop(ModBlocks.HEMP_BRICKS_SLAB,slabDrops(ModBlocks.HEMP_BRICKS_SLAB));
        addDrop(ModBlocks.HEMP_BRICKS_STAIRS);
        addDrop(ModBlocks.HEMP_BRICKS_WALL);
        addDrop(ModBlocks.HEMP_PLANKS);
        addDrop(ModBlocks.HEMP_PLANKS_SLAB, slabDrops(ModBlocks.HEMP_PLANKS_SLAB));
        addDrop(ModBlocks.HEMP_PLANKS_STAIRS);
        addDrop(ModBlocks.HEMP_PLANKS_BUTTON);
        addDrop(ModBlocks.HEMP_PLANKS_DOOR, doorDrops((ModBlocks.HEMP_PLANKS_DOOR)));
        addDrop(ModBlocks.HEMP_PLANKS_FENCE);
        addDrop(ModBlocks.HEMP_PLANKS_FENCE_GATE);
        addDrop(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE);
        addDrop(ModBlocks.HEMPCRETE_BLOCK);
        addDrop(ModBlocks.HEMPCRETE_POWDER_BLOCK);
        addDrop(ModBlocks.HEMP_BALE);
        addDrop(ModBlocks.HEMP_WOOL);
        addDrop(ModBlocks.HEMP_CARPET);
        // Empty, exactly like vanilla's cake: you cannot pick a cake back up once it is placed.
        // Vanilla still ships a loot table file for it rather than omitting one, so we do too.
        addDrop(ModBlocks.SPACE_CAKE, LootTable.builder());
        // The block entity's contents are scattered by DecarboxylatorBlock#onStateReplaced, so the
        // loot table only has to hand back the machine itself.
        addDrop(ModBlocks.DECARBOXYLATOR);
        addDrop(ModBlocks.INFUSER);
        addDrop(ModBlocks.DRY_SIFTER);
        addDrop(ModBlocks.HASHISH_BAR, hashishBarDrops(ModBlocks.HASHISH_BAR, ModItems.HASHISH));
        addDrop(ModBlocks.FILTERED_HASHISH_BAR,
                hashishBarDrops(ModBlocks.FILTERED_HASHISH_BAR, ModItems.FILTERED_HASHISH));

        // Wall sign / wall hanging sign share the standing block's loot table (see ModBlocks#dropsLike),
        // so they must not get their own addDrop call here.
        addDrop(ModBlocks.HEMP_PLANKS_SIGN);
        addDrop(ModBlocks.HEMP_PLANKS_HANGING_SIGN);

        addDrop(ModBlocks.INDICA_CROP, indicaCropDrops());
        addDrop(ModBlocks.INDICA_FLOWER, indicaFlowerDrops());

        addDrop(ModBlocks.SATIVA_CROP, sativaCropDrops());
        addDrop(ModBlocks.SATIVA_FLOWER, sativaFlowerDrops());
    }

    /**
     * Wild indica flower: shears or Silk Touch lift the flower itself intact (for potting/decor,
     * same as leaves); any other tool breaks it down into seeds with the same Fortune scaling as
     * the crop's seeds (base 1 + binomial(fortuneLevel + 3, 0.40)).
     */
    private LootTable.Builder indicaFlowerDrops() {
        RegistryEntry<Enchantment> fortune = this.registries.getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        return this.dropsWithSilkTouchOrShears(ModBlocks.INDICA_FLOWER,
                this.applyExplosionDecay(ModBlocks.INDICA_FLOWER,
                        ItemEntry.builder(ModItems.INDICA_SEEDS)
                                .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.40F, 3))));
    }

    /**
     * Loot for the two-tall indica crop. Purple Kush is the compact, bushy strain: <b>two leaves
     * more and two stems fewer</b> than Lemon Haze at every cut count. Buds are identical between
     * the strains — see {@link #BUDS_AT_ZERO_CUTS} — and so are seeds.
     */
    private LootTable.Builder indicaCropDrops() {
        return this.hempCropDrops(ModBlocks.INDICA_CROP,
                () -> StatePredicate.Builder.create().exactMatch(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER),
                IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE,
                ModItems.INDICA_BUDS, ModItems.INDICA_SEEDS,
                5, 2);
    }

    /**
     * The harvest table shared by both hemp crops.
     *
     * <p>Only the LOWER segment yields anything, so a plant is harvested exactly once no matter
     * which of its blocks is broken (see each crop's {@code onBreak}); an immature LOWER returns a
     * single seed. A mature LOWER pays out buds, seeds, hemp stem and hemp leaf, each with the
     * usual base + {@code binomial(fortuneLevel + extra, probability)} bonus.
     *
     * <p><b>Cut history moves buds and leaves in opposite directions</b>, by one each per cut the
     * plant received while it was growing (see {@link Defoliation}): a plant sheared in both
     * windows finishes with {@link #BUDS_AT_ZERO_CUTS} + 2 buds and {@code leavesAtZeroCuts - 2}
     * leaves, an untouched one with the base of each. Stems are <b>flat</b> — stalk is structural,
     * and pruning foliage has no business changing how much of it there is.
     *
     * <p><b>Buds and seeds are strain-independent</b> and stem/leaf counts are the only per-crop
     * numbers, which is why only those two are parameters. Keep it that way: a strain should be
     * told apart by its effects and its leaf/stem character, not by handing over more of the same
     * premium drop.
     *
     * <p>Note that the leaves a player takes during the two cuts exactly replace the leaves they
     * give up at harvest, so a plant yields the same number of leaves either way and defoliating is
     * a straight gain in buds. That is intended: it is what the real practice does (you keep the
     * trimmed fan leaves <em>and</em> the flowers do better), and the price is paid in shear
     * durability and in having to come back at the right two moments instead of planting and
     * forgetting.
     */
    private LootTable.Builder hempCropDrops(Block crop, Supplier<StatePredicate.Builder> lowerPredicate,
                                            IntProperty ageProperty, int maxAge,
                                            Item buds, Item seeds,
                                            int leavesAtZeroCuts, int stemCount) {
        RegistryWrapper.Impl<Enchantment> enchantments = this.registries.getOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);

        LootCondition.Builder isLower = BlockStatePropertyLootCondition.builder(crop)
                .properties(lowerPredicate.get());
        LootCondition.Builder isMatureLower = BlockStatePropertyLootCondition.builder(crop)
                .properties(lowerPredicate.get().exactMatch(ageProperty, maxAge));

        // The three cut buckets. "One cut" is the only one that needs an OR, because it is reached
        // by two different paths: the player caught the early window, or only the late one.
        LootCondition.Builder zeroCuts = cutBucket(crop, lowerPredicate, ageProperty, maxAge, false, false);
        LootCondition.Builder oneCut = AnyOfLootCondition.builder(
                cutBucket(crop, lowerPredicate, ageProperty, maxAge, true, false),
                cutBucket(crop, lowerPredicate, ageProperty, maxAge, false, true));
        LootCondition.Builder twoCuts = cutBucket(crop, lowerPredicate, ageProperty, maxAge, true, true);

        return this.applyExplosionDecay(crop,
                LootTable.builder()
                        // Buds when mature — more of them the more the plant was trimmed. Falls
                        // through to a single seed when the plant isn't mature yet.
                        .pool(LootPool.builder()
                                .conditionally(isLower)
                                .with(scaledEntry(buds, BUDS_AT_ZERO_CUTS, zeroCuts, fortune, BUDS_FORTUNE_CHANCE, 2)
                                        .alternatively(scaledEntry(buds, BUDS_AT_ZERO_CUTS + 1, oneCut, fortune, BUDS_FORTUNE_CHANCE, 2))
                                        .alternatively(scaledEntry(buds, BUDS_AT_ZERO_CUTS + 2, twoCuts, fortune, BUDS_FORTUNE_CHANCE, 2))
                                        .alternatively(ItemEntry.builder(seeds))))
                        // Hemp leaf when mature — fewer of them the more the plant was trimmed.
                        .pool(LootPool.builder()
                                .conditionally(isMatureLower)
                                .with(scaledEntry(ModItems.HEMP_LEAF, leavesAtZeroCuts, zeroCuts, fortune, 0.30F, 3)
                                        .alternatively(scaledEntry(ModItems.HEMP_LEAF, leavesAtZeroCuts - 1, oneCut, fortune, 0.30F, 3))
                                        .alternatively(scaledEntry(ModItems.HEMP_LEAF, leavesAtZeroCuts - 2, twoCuts, fortune, 0.30F, 3))))
                        // Seeds when mature. Identical across strains, so replanting either costs the same.
                        .pool(LootPool.builder()
                                .conditionally(isMatureLower)
                                .with(ItemEntry.builder(seeds)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(3.0F)))
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.40F, 3))))
                        // Hemp stem when mature. Unaffected by trimming.
                        .pool(LootPool.builder()
                                .conditionally(isMatureLower)
                                .with(ItemEntry.builder(ModItems.HEMP_STEM)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create((float) stemCount)))
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.30F, 3)))));
    }

    /** "This plant is a mature LOWER and was trimmed in exactly these windows." */
    private static LootCondition.Builder cutBucket(Block crop, Supplier<StatePredicate.Builder> lowerPredicate,
                                                   IntProperty ageProperty, int maxAge,
                                                   boolean trimmedEarly, boolean trimmedLate) {
        return BlockStatePropertyLootCondition.builder(crop)
                .properties(lowerPredicate.get()
                        .exactMatch(ageProperty, maxAge)
                        .exactMatch(Defoliation.TRIMMED_EARLY, trimmedEarly)
                        .exactMatch(Defoliation.TRIMMED_LATE, trimmedLate));
    }

    /** A fixed-count drop gated on one cut bucket, with the usual Fortune bonus on top. */
    private static LeafEntry.Builder<?> scaledEntry(Item item, int count, LootCondition.Builder bucket,
                                                    RegistryEntry<Enchantment> fortune,
                                                    float fortuneChance, int fortuneExtra) {
        return ItemEntry.builder(item)
                .conditionally(bucket)
                .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create((float) count)))
                .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, fortuneChance, fortuneExtra));
    }

    /**
     * Wild Lemon Haze: the same deal as the wild Purple Kush above — shears or Silk Touch lift the
     * flower itself for potting, anything else breaks it down into that strain's seeds.
     *
     * <h2>The {@code half=lower} guard is the whole difference from indica's</h2>
     *
     * This flower is <b>two blocks tall</b>, and a two-block plant rolls its loot table <em>twice</em>
     * per break: once for the half the player hit, once as the orphaned half pops off in the
     * neighbour update. Without a guard the player gets two flowers, or two rolls of the seed
     * bonus, from one plant — silently, and in the generous direction (`crops.md`, *the tall-plant
     * loot double-roll*). Pinning the pool to the lower half means exactly one of the two rolls can
     * ever pay out, whichever half was struck. It is the same fix vanilla's {@code rose_bush} table
     * uses, and the same one {@code hempCropDrops} applies through {@code isLower}.
     */
    private LootTable.Builder sativaFlowerDrops() {
        RegistryEntry<Enchantment> fortune = this.registries.getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        LootCondition.Builder isLower = BlockStatePropertyLootCondition.builder(ModBlocks.SATIVA_FLOWER)
                .properties(StatePredicate.Builder.create().exactMatch(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
        return LootTable.builder().pool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1.0F))
                .conditionally(isLower)
                .with(ItemEntry.builder(ModBlocks.SATIVA_FLOWER)
                        .conditionally(this.createWithShearsCondition().or(this.createSilkTouchCondition()))
                        .alternatively(this.applyExplosionDecay(ModBlocks.SATIVA_FLOWER,
                                ItemEntry.builder(ModItems.SATIVA_SEEDS)
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.40F, 3))))));
    }

    /**
     * Loot for the three-tall sativa crop. Lemon Haze is the tall, lanky strain: <b>two stems more
     * and two leaves fewer</b> than Purple Kush at every cut count — a plant that is mostly stalk
     * should read as mostly stalk in the drops.
     *
     * <p>Buds and seeds are identical to indica's. Lemon Haze is worth growing for what its buds
     * <em>do</em> (Speed/Haste against Purple Kush's Resistance) and for the stem yield, not for a
     * bigger pile of the same thing — see {@link #BUDS_AT_ZERO_CUTS}. Note the two strains end up
     * on the <b>same total item count</b> in every row; they differ in composition, not quantity.
     */
    private LootTable.Builder sativaCropDrops() {
        return this.hempCropDrops(ModBlocks.SATIVA_CROP,
                () -> StatePredicate.Builder.create().exactMatch(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER),
                SativaCropBlock.AGE, SativaCropBlock.MAX_AGE,
                ModItems.SATIVA_BUDS, ModItems.SATIVA_SEEDS,
                3, 4);
    }
}
