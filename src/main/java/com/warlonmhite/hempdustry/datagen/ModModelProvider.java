package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.item.ArmorItem;

public class ModModelProvider extends FabricModelProvider {
    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator blockStateModelGenerator) {
        BlockStateModelGenerator.BlockTexturePool hempBricksPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.HEMP_BRICKS_BLOCK);
        BlockStateModelGenerator.BlockTexturePool hempPlanksPool = blockStateModelGenerator.registerCubeAllModelTexturePool(ModBlocks.HEMP_PLANKS);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.HEMPCRETE_BLOCK);
        blockStateModelGenerator.registerSimpleCubeAll(ModBlocks.HEMPCRETE_POWDER_BLOCK);

        hempBricksPool.stairs(ModBlocks.HEMP_BRICKS_STAIRS);
        hempBricksPool.slab(ModBlocks.HEMP_BRICKS_SLAB);
        hempBricksPool.wall(ModBlocks.HEMP_BRICKS_WALL);

        hempPlanksPool.stairs(ModBlocks.HEMP_PLANKS_STAIRS);
        hempPlanksPool.slab(ModBlocks.HEMP_PLANKS_SLAB);
        hempPlanksPool.button(ModBlocks.HEMP_PLANKS_BUTTON);
        hempPlanksPool.pressurePlate(ModBlocks.HEMP_PLANKS_PRESSURE_PLATE);
        hempPlanksPool.fence(ModBlocks.HEMP_PLANKS_FENCE);
        hempPlanksPool.fenceGate(ModBlocks.HEMP_PLANKS_FENCE_GATE);

        blockStateModelGenerator.registerDoor(ModBlocks.HEMP_PLANKS_DOOR);
        blockStateModelGenerator.registerTrapdoor(ModBlocks.HEMP_PLANKS_TRAPDOOR);

        blockStateModelGenerator.registerLog(ModBlocks.HEMP_BALE).log(ModBlocks.HEMP_BALE);


        blockStateModelGenerator.registerCrop(ModBlocks.INDICA_CROP, IndicaCropBlock.AGE, 0, 1, 2, 3, 4, 5, 6, 7, 8);

    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.CANNABUTTER, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BRICK, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FIBER, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FLOUR, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_STEM, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMPCRETE, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_BUDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_SPLIFF, Models.GENERATED);
        itemModelGenerator.register(ModItems.PIPE, Models.GENERATED);
        itemModelGenerator.register(ModItems.GLASS_BONG, Models.GENERATED);
        itemModelGenerator.register(ModItems.SATIVA_BUDS, Models.GENERATED);

        itemModelGenerator.registerArmor(((ArmorItem) ModItems.FLIP_FLOPS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_BEANNIE));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_HAREM_PANTS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_SHIRT));
    }
}
