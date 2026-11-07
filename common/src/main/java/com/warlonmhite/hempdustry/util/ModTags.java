package com.warlonmhite.hempdustry.util;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import com.google.common.collect.ImmutableSet;
import com.warlonmhite.hempdustry.platform.Registration;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class ModTags {

    /**
     * Where the wild plants grow. Minecraft 1.16.5 has no biome tags, so each tier names vanilla's
     * 1.16 biomes by id -- the same biomes the newer lines' tags hold, under their 1.16 names -- and
     * takes a modded biome by the category it declares, which is what the newer lines reach through the
     * {@code #c:is_*} tags. The tiers stay disjoint, so no biome is decorated twice. There is no cave
     * tier: 1.16.5 has no lush caves.
     */
    public static class Biomes {

        public static final Registration.BiomeSelector INDICA_FLOWER_GEN = select(
                ImmutableSet.of("plains", "jungle", "jungle_hills", "jungle_edge", "modified_jungle",
                        "modified_jungle_edge", "bamboo_jungle", "bamboo_jungle_hills", "swamp", "swamp_hills"),
                Biome.BiomeCategory.JUNGLE, Biome.BiomeCategory.SWAMP);
        // Three disjoint tiers — see ModConfiguredFeatures.
        public static final Registration.BiomeSelector SATIVA_FLOWER_GEN = select(
                ImmutableSet.of("savanna", "savanna_plateau", "shattered_savanna", "shattered_savanna_plateau",
                        "sunflower_plains"),
                Biome.BiomeCategory.SAVANNA);
        public static final Registration.BiomeSelector SATIVA_FLOWER_GEN_SPARSE = select(
                ImmutableSet.of("wooded_badlands_plateau", "modified_wooded_badlands_plateau"),
                Biome.BiomeCategory.MESA);
        // 1.18 folded the two plain plateaus into badlands, so they sit in badlands' tier here.
        public static final Registration.BiomeSelector SATIVA_FLOWER_GEN_RARE = select(
                ImmutableSet.of("badlands", "eroded_badlands", "badlands_plateau", "modified_badlands_plateau"));
        public static final Registration.BiomeSelector BELDIA_GEN = select(
                ImmutableSet.of("desert", "desert_hills", "desert_lakes"),
                Biome.BiomeCategory.DESERT);

        /** Vanilla's biomes by path, and any other namespace's by category. */
        private static Registration.BiomeSelector select(Set<String> vanilla, Biome.BiomeCategory... modded) {
            Set<Biome.BiomeCategory> categories = ImmutableSet.copyOf(modded);
            return (biome, category) -> "minecraft".equals(biome.getNamespace())
                    ? vanilla.contains(biome.getPath())
                    : categories.contains(category);
        }
    }


    public static class Items {

        public static final Tag.Named<Item> HEMP_SEEDS = createTag("hemp_seeds");
        /**
         * What the Infuser accepts in its milk slot. A tag rather than a hard {@code milk_bucket}
         * check so another mod's milk works, and so a datapack can widen it without touching code.
         */
        public static final Tag.Named<Item> MILK_BUCKETS = createTag("milk_buckets");
        /**
         * Everything dosed with cannabutter (or, for bhang, with decarboxylated hemp directly).
         * Exists so "eat your first edible" is one advancement rather than one per food, and so a
         * future edible joins it instead of needing the criterion edited.
         *
         * <p><b>Space Cake is deliberately not in here.</b> Eating a slice is a block interaction,
         * not item consumption, so {@code minecraft:consume_item} never fires for it — listing it
         * would be a lie the tag couldn't keep.
         */
        public static final Tag.Named<Item> EDIBLES = createTag("edibles");
        /**
         * The wholesome half: the foods made from hemp seed, which carry no THC at all. Exists for
         * the "Hemp Hearts" advancement, so a new seed food joins the tag instead of the criterion.
         *
         * <p>Hemp milk is left out on purpose. It is drunk rather than eaten, and it is an Infuser
         * ingredient first — exactly as a cow's milk bucket is not a food.
         */
        public static final Tag.Named<Item> HEMP_SEED_FOODS = createTag("hemp_seed_foods");
        /**
         * Flower the Sifting Box separates resin from — every strain's buds, built off the strain
         * registry so a third one joins by existing. Sifted at {@code FLOWER_CHANCE}.
         */
        public static final Tag.Named<Item> SIFTABLE_FLOWER = createTag("siftable/flower");
        /**
         * Buds grown for the sieve, which move the screen {@code RESINOUS_CHANCE} levels each instead
         * of one — Beldía's. Kept <b>out of</b> {@link #SIFTABLE_FLOWER} rather than overlapping it,
         * so each tag's recipe-viewer page counts only what it says.
         */
        public static final Tag.Named<Item> SIFTABLE_RESINOUS = createTag("siftable/resinous");
        /**
         * Leaf and trim the Sifting Box separates resin from. Sifted at the slower
         * {@code TRIM_CHANCE}: bulkier, and much less of it is resin.
         *
         * <p>Two tags rather than one with a rate baked in, because the rate <em>is</em> the
         * balance — see {@link com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock}.
         */
        public static final Tag.Named<Item> SIFTABLE_TRIM = createTag("siftable/trim");
        /**
         * Powder the box re-sifts at a finer mesh, producing {@code filtered_kief} rather than more
         * kief. Its own tag rather than a third rate on the plant side, because
         * {@code SiftingBoxBlock.Content} keeps plant matter and powder off the same screen — one
         * rule to state, not a list of exceptions.
         *
         * <p><b>Bubble hash is deliberately not in here</b>, and neither is filtered kief. Both are
         * already the clean end of their own road, and a further pass on either would be a pure loss
         * at no mechanical gain — which is a trap, and no route through this block may be one.
         */
        public static final Tag.Named<Item> SIFTABLE_KIEF = createTag("siftable/kief");
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
         * with it and leaving the bar uncuttable with nothing but a server-log line to say why. Every
         * entry is therefore {@code "required": false}. On 1.20.1 the file also names the knives' tags
         * of that version, {@code #c:tools/knives} and {@code #forge:tools/knives}. This is exactly how {@code #c:is_lush}
         * stopped wild indica generating in caves.
         *
         * <p>Hand-written rather than datagen'd because there is no {@code KNIFE_TOOLS} constant on
         * Fabric — checked against {@code fabric-convention-tags-v2} 2.8.0, which has
         * {@code SHEAR_TOOLS} and {@code MELEE_WEAPON_TOOLS} and no knife at all. {@code c:tools/knife}
         * is the id Farmer's Delight and its kin actually use.
         */
        public static final Tag.Named<Item> HASH_CUTTERS = createTag("hash_cutters");

        /**
         * Shears, every mod's: what trims a plant. On the newer lines this is {@code #c:tools/shear},
         * which both loaders fill; on 1.20.1 no one convention tag holds them all, so the mod's own tag
         * takes vanilla's shears and each convention's, optionally.
         */
        public static final Tag.Named<Item> SHEARS = createTag("shears");
        /** Bone meal and every mod's fertiliser, the same way: what feeds a Grow Pot. */
        public static final Tag.Named<Item> FERTILIZERS = createTag("fertilizers");
        private static Tag.Named<Item> createTag(String name) {
            return ItemTags.bind(new ResourceLocation(Hempdustry.MOD_ID, name).toString());
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
        public static final Tag.Named<Item> HEMP_CROPS = itemTag("crops/hemp");
        /** Hemp's entry in {@code #c:bricks}, alongside {@code c:bricks/normal} and {@code /nether}. */
        public static final Tag.Named<Item> HEMP_BRICKS = itemTag("bricks/hemp");
        /** The bale's entry in {@code #c:storage_blocks}, where hay sits as {@code /wheat}. */
        public static final Tag.Named<Item> HEMP_STORAGE_BLOCKS = itemTag("storage_blocks/hemp");
        /** Block-side counterpart of {@link #HEMP_STORAGE_BLOCKS}. */
        public static final Tag.Named<Block> HEMP_STORAGE_BLOCKS_BLOCK = blockTag("storage_blocks/hemp");

        private static Tag.Named<Item> itemTag(String path) {
            return ItemTags.bind(new ResourceLocation("c", path).toString());
        }

        private static Tag.Named<Block> blockTag(String path) {
            return BlockTags.bind(new ResourceLocation("c", path).toString());
        }
    }

    public static class Blocks {

        /**
         * What counts as a heat source under an Infuser. Anything in here with a {@code LIT}
         * property must also be lit; anything without one (magma) is always hot — see
         * {@code InfuserBlockEntity#isHeatedFrom}.
         */
        public static final Tag.Named<Block> HEAT_SOURCES = createTag("heat_sources");
        /**
         * Every hemp crop, strain-agnostic — the block-side counterpart of
         * {@link Items#HEMP_SEEDS}. Used by the "Trim Season" advancement to recognise a shearing
         * as a defoliation without naming either strain, so a third one joins the tag and is
         * covered for free.
         *
         * <p>Deliberately ours rather than {@code #minecraft:crops}: that tag holds wheat and
         * friends too, and this needs to mean "a plant that can be trimmed".
         */
        public static final Tag.Named<Block> HEMP_CROPS = createTag("hemp_crops");
        /** Counts as a Grow Lamp over a hemp plant, whatever its light level — see {@code GrowLight}. */
        public static final Tag.Named<Block> GROW_LAMPS = createTag("grow_lamps");
        /** Bright, but not a light over a plant: never counted, whatever its light level. */
        public static final Tag.Named<Block> NOT_GROW_LIGHTS = createTag("not_grow_lights");
        /**
         * The ground Beldía grows on, and generates on, outside a bed of ours. A tag of our own
         * rather than {@code #c:sands}, which not every Fabric API has, or {@code #minecraft:sand},
         * which holds suspicious sand. It takes {@code #c:sands} in optionally, so another mod's sand
         * joins wherever that tag exists, and a datapack can widen or narrow it here.
         */
        public static final Tag.Named<Block> BELDIA_SOIL = createTag("beldia_soil");
        private static Tag.Named<Block> createTag(String name) {
            return BlockTags.bind(new ResourceLocation(Hempdustry.MOD_ID, name).toString());
        }
    }
}
