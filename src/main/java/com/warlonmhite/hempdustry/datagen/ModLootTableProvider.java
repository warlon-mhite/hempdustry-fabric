package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Block;
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
        // The block entity's contents are scattered by DecarboxylatorBlock#onStateReplaced, so the
        // loot table only has to hand back the machine itself.
        addDrop(ModBlocks.DECARBOXYLATOR);

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
        RegistryEntry<Enchantment> fortune = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
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
        RegistryWrapper.Impl<Enchantment> enchantments = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
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
     */
    private LootTable.Builder sativaFlowerDrops() {
        RegistryEntry<Enchantment> fortune = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
        return this.dropsWithSilkTouchOrShears(ModBlocks.SATIVA_FLOWER,
                this.applyExplosionDecay(ModBlocks.SATIVA_FLOWER,
                        ItemEntry.builder(ModItems.SATIVA_SEEDS)
                                .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.40F, 3))));
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
