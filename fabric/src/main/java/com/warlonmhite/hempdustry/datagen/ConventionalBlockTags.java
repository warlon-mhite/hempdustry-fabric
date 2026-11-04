package com.warlonmhite.hempdustry.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** The block convention tags the mod lists itself in; {@link ConventionalItemTags} says why they are named here. */
final class ConventionalBlockTags {
    static final TagKey<Block> STORAGE_BLOCKS = c("storage_blocks");
    static final TagKey<Block> CONCRETES = c("concretes");

    static final TagKey<Block> FORGE_STORAGE_BLOCKS = forge("storage_blocks");
    static final TagKey<Block> FORGE_HEMP_STORAGE_BLOCKS = forge("storage_blocks/hemp");

    private ConventionalBlockTags() {
    }

    private static TagKey<Block> c(String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("c", path));
    }

    private static TagKey<Block> forge(String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("forge", path));
    }
}
