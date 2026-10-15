package com.warlonmhite.hempdustry.block.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.BongBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    // Vanilla's BlockEntityType.Builder, not the deprecated FabricBlockEntityTypeBuilder, with the
    // datafixer type left null, which is what a mod wants.
    public static final BlockEntityType<DecarboxylatorBlockEntity> DECARBOXYLATOR =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "decarboxylator"),
                    BlockEntityType.Builder.of(DecarboxylatorBlockEntity::new,
                            ModBlocks.DECARBOXYLATOR).build(null));

    public static final BlockEntityType<InfuserBlockEntity> INFUSER =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "infuser"),
                    BlockEntityType.Builder.of(InfuserBlockEntity::new,
                            ModBlocks.INFUSER).build(null));

    public static final BlockEntityType<HempPressBlockEntity> HEMP_PRESS =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp_press"),
                    BlockEntityType.Builder.of(HempPressBlockEntity::new,
                            ModBlocks.HEMP_PRESS).build(null));

    // One type for every glass: the entity holds components, not anything that differs by colour.
    public static final BlockEntityType<BongBlockEntity> BONG =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "bong"),
                    BlockEntityType.Builder.of(BongBlockEntity::new,
                            ModBlocks.DEVICE_BLOCKS.values().toArray(Block[]::new)).build(null));

    public static final BlockEntityType<HempBedBlockEntity> HEMP_BED =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp_bed"),
                    BlockEntityType.Builder.of(HempBedBlockEntity::new,
                            ModBlocks.HEMP_BED).build(null));

    public static void registerBlockEntities() {
        Hempdustry.LOGGER.info("Registering Block Entities for " + Hempdustry.MOD_ID);
    }
}
