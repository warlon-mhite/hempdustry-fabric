package com.warlonmhite.hempdustry.util;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class ModTags {

    public static class Biomes {

        public static final TagKey<Biome> INDICA_FLOWER_GEN = createTag("indica_flower_gen");
        public static final TagKey<Biome> INDICA_FLOWER_GEN_CAVE = createTag("indica_flower_gen_cave");
        // Three disjoint tiers — see the tag JSONs in resources and ModConfiguredFeatures.
        public static final TagKey<Biome> SATIVA_FLOWER_GEN = createTag("sativa_flower_gen");
        public static final TagKey<Biome> SATIVA_FLOWER_GEN_SPARSE = createTag("sativa_flower_gen_sparse");
        public static final TagKey<Biome> SATIVA_FLOWER_GEN_RARE = createTag("sativa_flower_gen_rare");
        private static TagKey<Biome> createTag(String name) {
            return TagKey.of(RegistryKeys.BIOME, Identifier.of(Hempdustry.MOD_ID, name));
        }
    }


    public static class Items {

        public static final TagKey<Item> HEMP_SEEDS = createTag("hemp_seeds");
        /**
         * What the Infuser accepts in its milk slot. A tag rather than a hard {@code milk_bucket}
         * check so another mod's milk works, and so a datapack can widen it without touching code.
         */
        public static final TagKey<Item> MILK_BUCKETS = createTag("milk_buckets");
        /**
         * Everything dosed with cannabutter (or, for bhang, with decarboxylated hemp directly).
         * Exists so "eat your first edible" is one advancement rather than one per food, and so a
         * future edible joins it instead of needing the criterion edited.
         *
         * <p><b>Space Cake is deliberately not in here.</b> Eating a slice is a block interaction,
         * not item consumption, so {@code minecraft:consume_item} never fires for it — listing it
         * would be a lie the tag couldn't keep.
         */
        public static final TagKey<Item> EDIBLES = createTag("edibles");
        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of(Hempdustry.MOD_ID, name));
        }
    }

    /**
     * Tags in the <b>{@code c}</b> namespace that Fabric's convention set does not already declare.
     *
     * <h2>Why mint these at all</h2>
     *
     * {@code c:crops}, {@code c:bricks} and {@code c:storage_blocks} hold nothing but subtags —
     * {@code c:crops/wheat}, {@code c:bricks/nether} and so on — so joining the parent directly would
     * break the shape every other mod reads. The convention <em>is</em> that a new material adds its
     * own subtag and folds it into the parent, which is what these do.
     *
     * <p>Minting a {@code c:} path is not a land grab: if another mod declares {@code c:crops/hemp}
     * too, the two definitions merge, which is exactly the outcome wanted. The alternative — a
     * {@code hempdustry:} tag — would be invisible to anyone who had not heard of this mod, and the
     * whole value of a convention tag is that it works between strangers.
     */
    public static class Conventional {

        /** Hemp's entry in {@code #c:crops}. The harvest, not the plant — buds and leaf. */
        public static final TagKey<Item> HEMP_CROPS = itemTag("crops/hemp");
        /** Hemp's entry in {@code #c:bricks}, alongside {@code c:bricks/normal} and {@code /nether}. */
        public static final TagKey<Item> HEMP_BRICKS = itemTag("bricks/hemp");
        /** The bale's entry in {@code #c:storage_blocks}, where hay sits as {@code /wheat}. */
        public static final TagKey<Item> HEMP_STORAGE_BLOCKS = itemTag("storage_blocks/hemp");
        /** Block-side counterpart of {@link #HEMP_STORAGE_BLOCKS}. */
        public static final TagKey<Block> HEMP_STORAGE_BLOCKS_BLOCK = blockTag("storage_blocks/hemp");

        private static TagKey<Item> itemTag(String path) {
            return TagKey.of(RegistryKeys.ITEM, Identifier.of("c", path));
        }

        private static TagKey<Block> blockTag(String path) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", path));
        }
    }

    public static class Blocks {

        /**
         * What counts as a heat source under an Infuser. Anything in here with a {@code LIT}
         * property must also be lit; anything without one (magma) is always hot — see
         * {@code InfuserBlockEntity#isHeatedFrom}.
         */
        public static final TagKey<Block> HEAT_SOURCES = createTag("heat_sources");
        /**
         * Every hemp crop, strain-agnostic — the block-side counterpart of
         * {@link Items#HEMP_SEEDS}. Used by the "Trim Season" advancement to recognise a shearing
         * as a defoliation without naming either strain, so a third one joins the tag and is
         * covered for free.
         *
         * <p>Deliberately ours rather than {@code #minecraft:crops}: that tag holds wheat and
         * friends too, and this needs to mean "a plant that can be trimmed".
         */
        public static final TagKey<Block> HEMP_CROPS = createTag("hemp_crops");
        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, Identifier.of(Hempdustry.MOD_ID, name));
        }
    }
}
