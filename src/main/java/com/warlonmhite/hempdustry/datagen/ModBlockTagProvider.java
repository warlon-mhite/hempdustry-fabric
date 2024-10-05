package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends FabricTagProvider.BlockTagProvider {
    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE)
                .add(ModBlocks.HEMPCRETE_BLOCK);

        getOrCreateTagBuilder(BlockTags.SHOVEL_MINEABLE)
                .add(ModBlocks.HEMPCRETE_POWDER_BLOCK);

        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                .add(ModBlocks.HEMP_BRICKS_BLOCK)
                .add(ModBlocks.HEMP_BRICKS_SLAB)
                .add(ModBlocks.HEMP_BRICKS_STAIRS)
                .add(ModBlocks.HEMP_BRICKS_WALL)
                .add(ModBlocks.HEMP_PLANKS)
                .add(ModBlocks.HEMP_PLANKS_STAIRS)
                .add(ModBlocks.HEMP_PLANKS_BUTTON)
                .add(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE)
                .add(ModBlocks.HEMP_PLANKS_SLAB)
                .add(ModBlocks.HEMP_PLANKS_FENCE)
                .add(ModBlocks.HEMP_PLANKS_FENCE_GATE)
                .add(ModBlocks.HEMP_PLANKS_DOOR)
                .add(ModBlocks.HEMP_PLANKS_TRAPDOOR);

        getOrCreateTagBuilder(BlockTags.FENCES).add(ModBlocks.HEMP_PLANKS_FENCE);
        getOrCreateTagBuilder(BlockTags.FENCE_GATES).add(ModBlocks.HEMP_PLANKS_FENCE_GATE);
        getOrCreateTagBuilder(BlockTags.WALLS).add(ModBlocks.HEMP_BRICKS_WALL);
    }
}
