package com.warlonmhite.hempdustry.block.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.BongBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import net.minecraft.block.Block;
import com.warlonmhite.hempdustry.block.entity.custom.HempBedBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    // Vanilla's BlockEntityType.Builder, not the deprecated FabricBlockEntityTypeBuilder. The no-argument
    // build() is Fabric's, interface-injected onto the vanilla builder — it is build(Type) with the
    // datafixer type left null, which is what a mod wants.
    public static final BlockEntityType<DecarboxylatorBlockEntity> DECARBOXYLATOR =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Hempdustry.MOD_ID, "decarboxylator"),
                    BlockEntityType.Builder.create(DecarboxylatorBlockEntity::new,
                            ModBlocks.DECARBOXYLATOR).build());

    public static final BlockEntityType<InfuserBlockEntity> INFUSER =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Hempdustry.MOD_ID, "infuser"),
                    BlockEntityType.Builder.create(InfuserBlockEntity::new,
                            ModBlocks.INFUSER).build());

    public static final BlockEntityType<HempPressBlockEntity> HEMP_PRESS =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Hempdustry.MOD_ID, "hemp_press"),
                    BlockEntityType.Builder.create(HempPressBlockEntity::new,
                            ModBlocks.HEMP_PRESS).build());

    // One type for every glass: the entity holds components, not anything that differs by colour.
    public static final BlockEntityType<BongBlockEntity> BONG =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Hempdustry.MOD_ID, "bong"),
                    BlockEntityType.Builder.create(BongBlockEntity::new,
                            ModBlocks.DEVICE_BLOCKS.values().toArray(Block[]::new)).build(null));

    public static final BlockEntityType<HempBedBlockEntity> HEMP_BED =
            Registry.register(Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Hempdustry.MOD_ID, "hemp_bed"),
                    BlockEntityType.Builder.create(HempBedBlockEntity::new,
                            ModBlocks.HEMP_BED).build(null));

    public static void registerBlockEntities() {
        Hempdustry.LOGGER.info("Registering Block Entities for " + Hempdustry.MOD_ID);
    }
}
