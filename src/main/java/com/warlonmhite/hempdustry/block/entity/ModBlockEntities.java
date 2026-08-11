package com.warlonmhite.hempdustry.block.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
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

    public static void registerBlockEntities() {
        Hempdustry.LOGGER.info("Registering Block Entities for " + Hempdustry.MOD_ID);
    }
}
