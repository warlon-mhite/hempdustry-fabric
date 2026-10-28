package com.warlonmhite.hempdustry.block.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.BongBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    // Through the loader: vanilla's own BlockEntityType.Builder is gone since 1.21.2 and its
    // constructor is private. No datafixer type, which is what a mod wants.
    public static final BlockEntityType<DecarboxylatorBlockEntity> DECARBOXYLATOR =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "decarboxylator"),
                    Services.REGISTRATION.blockEntityType(DecarboxylatorBlockEntity::new,
                            ModBlocks.DECARBOXYLATOR));

    public static final BlockEntityType<InfuserBlockEntity> INFUSER =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "infuser"),
                    Services.REGISTRATION.blockEntityType(InfuserBlockEntity::new,
                            ModBlocks.INFUSER));

    public static final BlockEntityType<HempPressBlockEntity> HEMP_PRESS =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "hemp_press"),
                    Services.REGISTRATION.blockEntityType(HempPressBlockEntity::new,
                            ModBlocks.HEMP_PRESS));

    // One type for every glass: the entity holds components, not anything that differs by colour.
    public static final BlockEntityType<BongBlockEntity> BONG =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "bong"),
                    Services.REGISTRATION.blockEntityType(BongBlockEntity::new,
                            ModBlocks.DEVICE_BLOCKS.values().toArray(Block[]::new)));

    public static void registerBlockEntities() {
        Hempdustry.LOGGER.info("Registering Block Entities for " + Hempdustry.MOD_ID);
    }
}
