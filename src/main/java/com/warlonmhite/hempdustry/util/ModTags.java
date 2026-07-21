package com.warlonmhite.hempdustry.util;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class ModTags {

    public static class Biomes {

        public static final TagKey<Biome> INDICA_FLOWER_GEN = createTag("indica_flower_gen");
        private static TagKey<Biome> createTag(String name) {
            return TagKey.of(RegistryKeys.BIOME, Identifier.of(Hempdustry.MOD_ID, name));
        }
    }


    public static class Items {

        public static final TagKey<Item> HEMP_SEEDS = createTag("hemp_seeds");
        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(Hempdustry.MOD_ID, name));
        }
    }
}
