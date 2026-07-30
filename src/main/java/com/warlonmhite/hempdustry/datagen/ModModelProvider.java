package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.PackedSmokingDeviceItem;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

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

        blockStateModelGenerator.registerFlowerPotPlant(ModBlocks.INDICA_FLOWER, ModBlocks.POTTED_INDICA_FLOWER, BlockStateModelGenerator.TintType.NOT_TINTED);
        blockStateModelGenerator.registerFlowerPotPlant(ModBlocks.SATIVA_FLOWER, ModBlocks.POTTED_SATIVA_FLOWER, BlockStateModelGenerator.TintType.NOT_TINTED);

        // The crops' blockstates and stage models are hand-written under resources/ — the model
        // generator has no notion of a two- or three-tall crop, so INDICA_CROP and SATIVA_CROP
        // are deliberately absent here.
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        itemModelGenerator.register(ModItems.CANNABUTTER, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BRICK, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FIBER, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_FLOUR, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_STEM, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_LEAF, Models.GENERATED);
        itemModelGenerator.register(ModItems.DECARBOXYLATED_HEMP, Models.GENERATED);
        itemModelGenerator.register(ModItems.WASHED_DECARBOXYLATED_HEMP, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMPCRETE, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_SEEDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_BUDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.INDICA_SPLIFF, Models.GENERATED);
        itemModelGenerator.register(ModItems.WOODEN_PIPE, Models.GENERATED);
        itemModelGenerator.register(ModItems.BONG, Models.GENERATED);
        itemModelGenerator.register(ModItems.MUSIC_DISC_GANJA, Models.GENERATED);

        // All packed variants of a device share one "packed" texture (item/packed_pipe, item/packed_bong).
        // Give a strain its own texture later by keying this off the strain too.
        for (Item packed : ModItems.packedDevices()) {
            DeviceType device = ((PackedSmokingDeviceItem) packed).device();
            Identifier texture = Identifier.of(Hempdustry.MOD_ID, "item/" + device.packedTexture());
            Models.GENERATED.upload(ModelIds.getItemModelId(packed), TextureMap.layer0(texture), itemModelGenerator.writer);
        }
        itemModelGenerator.register(ModItems.SATIVA_BUDS, Models.GENERATED);
        // item/sativa_seeds.png is currently a copy of the indica one — a hemp seed is a hemp seed.
        itemModelGenerator.register(ModItems.SATIVA_SEEDS, Models.GENERATED);
        itemModelGenerator.register(ModItems.SATIVA_SPLIFF, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_PLANKS_SIGN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_PLANKS_HANGING_SIGN, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_BOAT, Models.GENERATED);
        itemModelGenerator.register(ModItems.HEMP_CHEST_BOAT, Models.GENERATED);

        itemModelGenerator.registerArmor(((ArmorItem) ModItems.FLIP_FLOPS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_BEANNIE));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_HAREM_PANTS));
        itemModelGenerator.registerArmor(((ArmorItem) ModItems.HEMP_SHIRT));
    }
}