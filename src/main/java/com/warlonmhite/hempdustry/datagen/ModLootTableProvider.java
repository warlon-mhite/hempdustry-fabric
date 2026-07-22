package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
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
import net.minecraft.predicate.StatePredicate;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;

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

        addDrop(ModBlocks.INDICA_CROP, indicaCropDrops());
    }

    /**
     * Loot for the two-tall indica crop. Only the LOWER half yields anything (so a
     * plant is harvested exactly once regardless of which half is broken — see
     * IndicaCropBlock#onBreak). A mature lower half (age 7) drops buds plus
     * fortune-scaled seeds; an immature lower half drops a single seed; the UPPER
     * half drops nothing.
     */
    private LootTable.Builder indicaCropDrops() {
        RegistryWrapper.Impl<Enchantment> enchantments = this.registryLookup.getWrapperOrThrow(RegistryKeys.ENCHANTMENT);

        LootCondition.Builder isLower = BlockStatePropertyLootCondition.builder(ModBlocks.INDICA_CROP)
                .properties(StatePredicate.Builder.create()
                        .exactMatch(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER));
        LootCondition.Builder isMatureLower = BlockStatePropertyLootCondition.builder(ModBlocks.INDICA_CROP)
                .properties(StatePredicate.Builder.create()
                        .exactMatch(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER)
                        .exactMatch(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE));

        return this.applyExplosionDecay(ModBlocks.INDICA_CROP,
                LootTable.builder()
                        .pool(LootPool.builder()
                                .conditionally(isLower)
                                .with(ItemEntry.builder(ModItems.INDICA_BUDS)
                                        .conditionally(isMatureLower)
                                        .alternatively(ItemEntry.builder(ModItems.INDICA_SEEDS))))
                        .pool(LootPool.builder()
                                .conditionally(isMatureLower)
                                .with(ItemEntry.builder(ModItems.INDICA_SEEDS)
                                        .apply(ApplyBonusLootFunction.binomialWithBonusCount(
                                                enchantments.getOrThrow(Enchantments.FORTUNE), 0.5714286F, 3)))));
    }
}
