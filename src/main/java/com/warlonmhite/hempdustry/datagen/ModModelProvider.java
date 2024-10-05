package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.HEMP_BRICKS_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.HEMP_PLANKS);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.HEMPCRETE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.HEMPCRETE_POWDER_BLOCK);
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.CANNABUTTER, Models.GENERATED);
        itemModelGenerator.register(ModItems.FLIP_FLOPS, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BEANNIE, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BRICK, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FIBER, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FLOUR, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_HAREM_PANTS, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_SHIRT, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_STEM, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMPCRETE, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_BUDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_SEEDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_SPLIFF, Models.GENERATED);
        itemModelGenerator.register(ModItems.PACKED_PIPE, Models.GENERATED);
        itemModelGenerator.register(ModItems.PIPE, Models.GENERATED);
        itemModelGenerator.register(ModItems.SATIVA_BUDS, Models.GENERATED);
    }
}
