package com.warlonmhite.hempdustry.util;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BannerPattern;
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
        /**
         * What repairs the hemp armour set on an anvil. Since 1.21.4 an {@code ArmorMaterial}
         * names a tag rather than an {@code Ingredient}, so this exists to hold one item.
         */
        public static final TagKey<Item> HEMP_ARMOR_REPAIR = createTag("hemp_armor_repair");
        /**
         * Flower the Dry Sifter separates resin from — every strain's buds, built off the strain
         * registry so a third one joins by existing. Sifted at {@code FLOWER_CHANCE}.
         */
        public static final TagKey<Item> SIFTABLE_FLOWER = createTag("siftable/flower");
        /**
         * Leaf and trim the Dry Sifter separates resin from. Sifted at the slower
         * {@code TRIM_CHANCE}: bulkier, and much less of it is resin.
         *
         * <p>Two tags rather than one with a rate baked in, because the rate <em>is</em> the
         * balance — see {@link com.warlonmhite.hempdustry.block.custom.DrySifterBlock}.
         */
        public static final TagKey<Item> SIFTABLE_TRIM = createTag("siftable/trim");
        /**
         * What cuts a hashish bar: swords and knives.
         *
         * <p><b>Deliberately not shears.</b> Shears already mean "trim a plant" in this mod, and a
         * verb that currently means exactly one thing should keep meaning exactly one thing. A blade
         * pressed into a slab is the real motion anyway. No tier gate either — hash is soft, and
         * vanilla's tier gates are about stone hardness.
         *
         * <p>The tag <b>file</b> is where the danger is. It names {@code #c:tools/knife}, which no
         * vanilla item joins, so on a client with no cooking mod installed it does not resolve — and
         * <b>one unresolvable required entry drops the whole tag</b>, taking {@code #minecraft:swords}
         * with it and leaving the bar uncuttable with nothing but a server-log line to say why. Both
         * entries are therefore {@code "required": false}. This is exactly how {@code #c:is_lush}
         * stopped wild indica generating in caves (CLAUDE.md §5).
         *
         * <p>Hand-written rather than datagen'd because there is no {@code KNIFE_TOOLS} constant on
         * Fabric — checked against {@code fabric-convention-tags-v2} 2.8.0, which has
         * {@code SHEAR_TOOLS} and {@code MELEE_WEAPON_TOOLS} and no knife at all. {@code c:tools/knife}
         * is the id Farmer's Delight and its kin actually use.
         */
        public static final TagKey<Item> HASH_CUTTERS = createTag("hash_cutters");
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

    public static class BannerPatterns {

        /**
         * The banner patterns {@code hemp_leaf} unlocks in a loom.
         *
         * <p>The loom does not read the item, it reads the item's
         * {@code minecraft:provides_banner_patterns} component, and that component holds a
         * <em>tag of banner patterns</em> rather than one pattern. So a pattern item is always two
         * halves: this tag, and the component on {@link com.warlonmhite.hempdustry.item.ModItems#HEMP_LEAF}
         * pointing at it. Vanilla mints one such tag per pattern item under
         * {@code pattern_item/}, which is the path this copies.
         *
         * <p>Being a tag is also what makes it extensible: a datapack can add a second pattern here
         * and the same leaf unlocks both, with no code change.
         */
        public static final TagKey<BannerPattern> HEMP_LEAF_PATTERN_ITEM =
                TagKey.of(RegistryKeys.BANNER_PATTERN, Identifier.of(Hempdustry.MOD_ID, "pattern_item/hemp_leaf"));

        private BannerPatterns() {
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
