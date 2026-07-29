package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.BlockStatePropertyLootCondition;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.function.ApplyBonusLootFunction;
import net.minecraft.loot.function.SetCountLootFunction;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.concurrent.CompletableFuture;

public class ModLootTableProvider extends FabricBlockLootTableProvider {
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
     * Loot for the two-tall indica crop. Only the LOWER half yields anything (so a
     * plant is harvested exactly once regardless of which half is broken — see
     * IndicaCropBlock#onBreak). The UPPER half drops nothing; an immature lower half
     * drops a single seed back.
     *
     * A mature lower half (age 7) drops, each with a guaranteed base plus a
     * fortune-scaling bonus (base + binomial(fortuneLevel + extra, probability),
     * the same mechanism vanilla uses for seeds):
     * <ul>
     *   <li>Indica buds — base 2, bonus 3 (chance) / 4 (very rare)</li>
     *   <li>Indica seeds — base 3, bonus 4 (likely) / 5 / 6 (very rare)</li>
     *   <li>Hemp stem — base 2, bonus 3 (likely) / 4 (uncommon) / 5 (very rare)</li>
     * </ul>
     */
    private LootTable.Builder indicaCropDrops() {
        RegistryWrapper.Impl<Enchantment> enchantments = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);

        LootCondition.Builder isLower = BlockStatePropertyLootCondition.builder(ModBlocks.INDICA_CROP)
                .properties(StatePredicate.Builder.create()
                        .exactMatch(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER));
        LootCondition.Builder isMatureLower = BlockStatePropertyLootCondition.builder(ModBlocks.INDICA_CROP)
                .properties(StatePredicate.Builder.create()
                        .exactMatch(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                        .exactMatch(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE));

        return this.applyExplosionDecay(ModBlocks.INDICA_CROP,
                LootTable.builder()
                        // Buds when mature, otherwise a single seed returned.
                        .pool(LootPool.builder()
                                .conditionally(isLower)
                                .with(ItemEntry.builder(ModItems.INDICA_BUDS)
                                        .conditionally(isMatureLower)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2.0F)))
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.15F, 2))
                                        .alternatively(ItemEntry.builder(ModItems.INDICA_SEEDS))))
                        // Seeds when mature.
                        .pool(LootPool.builder()
                                .conditionally(isMatureLower)
                                .with(ItemEntry.builder(ModItems.INDICA_SEEDS)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(3.0F)))
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.40F, 3))))
                        // Hemp stem when mature.
                        .pool(LootPool.builder()
                                .conditionally(isMatureLower)
                                .with(ItemEntry.builder(ModItems.HEMP_STEM)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(2.0F)))
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.30F, 3)))));
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
     * Loot for the three-tall sativa crop. Same shape as {@link #indicaCropDrops()} — only the
     * LOWER segment yields anything, so a plant is harvested exactly once no matter which of its
     * three blocks is broken (see SativaCropBlock#onBreak), and an immature one returns one seed.
     *
     * <p>A mature LOWER (age 7) pays out a little better than indica on buds and stem: the plant is
     * three blocks of biomass instead of two and takes noticeably longer to get there, so it has to
     * be worth the extra space and time. Seeds are deliberately identical to indica's, to keep
     * replanting either strain equally cheap.
     */
    private LootTable.Builder sativaCropDrops() {
        RegistryWrapper.Impl<Enchantment> enchantments = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);
        RegistryEntry<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);

        LootCondition.Builder isLower = BlockStatePropertyLootCondition.builder(ModBlocks.SATIVA_CROP)
                .properties(StatePredicate.Builder.create()
                        .exactMatch(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER));
        LootCondition.Builder isMatureLower = BlockStatePropertyLootCondition.builder(ModBlocks.SATIVA_CROP)
                .properties(StatePredicate.Builder.create()
                        .exactMatch(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                        .exactMatch(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE));

        return this.applyExplosionDecay(ModBlocks.SATIVA_CROP,
                LootTable.builder()
                        // Buds when mature, otherwise a single seed returned.
                        .pool(LootPool.builder()
                                .conditionally(isLower)
                                .with(ItemEntry.builder(ModItems.SATIVA_BUDS)
                                        .conditionally(isMatureLower)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(3.0F)))
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.20F, 2))
                                        .alternatively(ItemEntry.builder(ModItems.SATIVA_SEEDS))))
                        // Seeds when mature.
                        .pool(LootPool.builder()
                                .conditionally(isMatureLower)
                                .with(ItemEntry.builder(ModItems.SATIVA_SEEDS)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(3.0F)))
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.40F, 3))))
                        // Hemp stem when mature.
                        .pool(LootPool.builder()
                                .conditionally(isMatureLower)
                                .with(ItemEntry.builder(ModItems.HEMP_STEM)
                                        .apply(SetCountLootFunction.builder(ConstantLootNumberProvider.create(3.0F)))
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(fortune, 0.30F, 3)))));
    }
}
