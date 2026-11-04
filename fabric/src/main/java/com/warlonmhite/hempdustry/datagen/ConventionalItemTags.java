package com.warlonmhite.hempdustry.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * The item convention tags the mod lists itself in. Three conventions meet on 1.20.1, and a pack may
 * read any of them: the {@code c:} names Minecraft 1.20.5 and later settled on (the same files the
 * mod ships on its newer lines), Fabric's older {@code c:} names, and Forge's {@code forge:} ones.
 * Fabric API 0.92 declares only a handful of the first kind, so they are named here.
 */
final class ConventionalItemTags {
    static final TagKey<Item> MILK_BUCKETS = c("buckets/milk");
    static final TagKey<Item> STRINGS = c("strings");
    static final TagKey<Item> LEATHERS = c("leathers");
    static final TagKey<Item> ARMORS = c("armors");
    static final TagKey<Item> SOUP_FOODS = c("foods/soup");
    static final TagKey<Item> COOKIE_FOODS = c("foods/cookie");
    static final TagKey<Item> EDIBLE_WHEN_PLACED_FOODS = c("foods/edible_when_placed");
    static final TagKey<Item> FOODS = c("foods");
    static final TagKey<Item> CROPS = c("crops");
    static final TagKey<Item> BRICKS = c("bricks");
    static final TagKey<Item> STORAGE_BLOCKS = c("storage_blocks");
    static final TagKey<Item> CONCRETES = c("concretes");
    static final TagKey<Item> CONCRETE_POWDERS = c("concrete_powders");
    static final TagKey<Item> MUSIC_DISCS = c("music_discs");
    static final TagKey<Item> SHEAR_TOOLS = c("tools/shear");
    static final TagKey<Item> FERTILIZERS = c("fertilizers");

    /** Fabric's own names before 1.20.5, which Fabric API 0.92 declares. */
    static final TagKey<Item> OLD_MILK_BUCKETS = c("milk_buckets");
    static final TagKey<Item> OLD_SHEARS = c("shears");

    /** Forge's names on 1.20.1 ({@code net.minecraftforge.common.Tags}). */
    static final TagKey<Item> FORGE_CROPS = forge("crops");
    static final TagKey<Item> FORGE_HEMP_CROPS = forge("crops/hemp");
    static final TagKey<Item> FORGE_STORAGE_BLOCKS = forge("storage_blocks");
    static final TagKey<Item> FORGE_HEMP_STORAGE_BLOCKS = forge("storage_blocks/hemp");
    static final TagKey<Item> FORGE_STRING = forge("string");
    static final TagKey<Item> FORGE_LEATHER = forge("leather");
    static final TagKey<Item> FORGE_ARMORS = forge("armors");
    static final TagKey<Item> FORGE_SHEARS = forge("shears");
    static final TagKey<Item> FORGE_FERTILIZERS = forge("fertilizers");

    private ConventionalItemTags() {
    }

    private static TagKey<Item> c(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("c", path));
    }

    private static TagKey<Item> forge(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("forge", path));
    }
}
