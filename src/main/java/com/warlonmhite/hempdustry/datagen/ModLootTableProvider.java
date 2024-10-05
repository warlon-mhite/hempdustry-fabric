package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
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
    }
}
