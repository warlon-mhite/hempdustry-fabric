package com.warlonmhite.hempdustry.util;

import com.warlonmhite.hempdustry.platform.Registration;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class ModTags {

    /**
     * Where the wild plants grow. Forge 1.18.2 places a feature in a biome as the biomes load, before
     * any tag is bound, so each tier names its biomes by id -- vanilla's, and the modded ones the newer
     * lines' tags list -- and takes any other modded biome by the category it declares, which is what
     * the newer lines reach through the {@code #c:is_*} tags. The tiers stay disjoint, so no biome is
     * decorated twice.
     */
    public static class Biomes {

        /** Every biome a tier names, so that no other tier takes it by its category. */
        private static final Set<String> NAMED = new HashSet<>();

        public static final Registration.BiomeSelector INDICA_FLOWER_GEN = select(
                Set.of("minecraft:plains", "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle",
                        "minecraft:swamp",
                        "biomesoplenty:rainforest", "biomesoplenty:fungal_jungle", "biomesoplenty:bayou",
                        "biomesoplenty:floodplain", "biomesoplenty:wetland", "biomesoplenty:mystic_grove",
                        "biomesoplenty:lush_grassland",
                        "regions_unexplored:rainforest", "regions_unexplored:sparse_rainforest",
                        "regions_unexplored:tropics", "regions_unexplored:bayou", "regions_unexplored:old_growth_bayou",
                        "regions_unexplored:marsh", "regions_unexplored:fungal_fen",
                        "terralith:tropical_jungle", "terralith:rocky_jungle", "terralith:jungle_mountains",
                        "terralith:amethyst_rainforest", "terralith:orchid_swamp"),
                Biome.BiomeCategory.JUNGLE, Biome.BiomeCategory.SWAMP);
        /** The cave tier: lush caves, and the one modded underground jungle the newer lines name. */
        public static final Registration.BiomeSelector INDICA_FLOWER_GEN_CAVE = select(
                Set.of("minecraft:lush_caves", "terralith:underground_jungle"));
        // Three disjoint tiers — see ModConfiguredFeatures.
        public static final Registration.BiomeSelector SATIVA_FLOWER_GEN = select(
                Set.of("minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna",
                        "minecraft:sunflower_plains",
                        "biomesoplenty:savanna", "biomesoplenty:lush_savanna", "biomesoplenty:prairie",
                        "biomesoplenty:grassland", "biomesoplenty:mediterranean_forest",
                        "regions_unexplored:prairie", "regions_unexplored:steppe", "regions_unexplored:baobab_savanna",
                        "regions_unexplored:outback",
                        "terralith:arid_highlands", "terralith:brushland", "terralith:shrubland",
                        "terralith:savanna_slopes"),
                Biome.BiomeCategory.SAVANNA);
        public static final Registration.BiomeSelector SATIVA_FLOWER_GEN_SPARSE = select(
                Set.of("minecraft:wooded_badlands",
                        "biomesoplenty:dryland", "terralith:savanna_badlands", "terralith:red_oasis",
                        "regions_unexplored:mesa_spires"),
                Biome.BiomeCategory.MESA);
        public static final Registration.BiomeSelector SATIVA_FLOWER_GEN_RARE = select(
                Set.of("minecraft:badlands", "minecraft:eroded_badlands"));
        public static final Registration.BiomeSelector BELDIA_GEN = select(
                Set.of("minecraft:desert"),
                Biome.BiomeCategory.DESERT);

        /**
         * The biomes named, and any other biome outside {@code minecraft} that declares one of the
         * categories. A biome named for another tier is never taken by category, so the tiers stay
         * disjoint for a modded biome too.
         */
        private static Registration.BiomeSelector select(Set<String> named, Biome.BiomeCategory... modded) {
            NAMED.addAll(named);
            Set<Biome.BiomeCategory> categories = Set.of(modded);
            return (biome, category) -> named.contains(biome.toString())
                    || (!"minecraft".equals(biome.getNamespace()) && !NAMED.contains(biome.toString())
                    && categories.contains(category));
        }

        /** Whether {@code biome} is one {@code selector} takes, as worldgen sees it. */
        public static boolean test(Registration.BiomeSelector selector, Holder<Biome> biome) {
            return biome.unwrapKey()
                    .map(key -> selector.test(key.location(), Biome.getBiomeCategory(biome)))
                    .orElse(false);
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
         * The wholesome half: the foods made from hemp seed, which carry no THC at all. Exists for
         * the "Hemp Hearts" advancement, so a new seed food joins the tag instead of the criterion.
         *
         * <p>Hemp milk is left out on purpose. It is drunk rather than eaten, and it is an Infuser
         * ingredient first — exactly as a cow's milk bucket is not a food.
         */
        public static final TagKey<Item> HEMP_SEED_FOODS = createTag("hemp_seed_foods");
        /**
         * Flower the Sifting Box separates resin from — every strain's buds, built off the strain
         * registry so a third one joins by existing. Sifted at {@code FLOWER_CHANCE}.
         */
        public static final TagKey<Item> SIFTABLE_FLOWER = createTag("siftable/flower");
        /**
         * Buds grown for the sieve, which move the screen {@code RESINOUS_CHANCE} levels each instead
         * of one — Beldía's. Kept <b>out of</b> {@link #SIFTABLE_FLOWER} rather than overlapping it,
         * so each tag's recipe-viewer page counts only what it says.
         */
        public static final TagKey<Item> SIFTABLE_RESINOUS = createTag("siftable/resinous");
        /**
         * Leaf and trim the Sifting Box separates resin from. Sifted at the slower
         * {@code TRIM_CHANCE}: bulkier, and much less of it is resin.
         *
         * <p>Two tags rather than one with a rate baked in, because the rate <em>is</em> the
         * balance — see {@link com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock}.
         */
        public static final TagKey<Item> SIFTABLE_TRIM = createTag("siftable/trim");
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
        public static final TagKey<Item> SIFTABLE_KIEF = createTag("siftable/kief");
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
        public static final TagKey<Item> HASH_CUTTERS = createTag("hash_cutters");

        /**
         * Shears, every mod's: what trims a plant. On the newer lines this is {@code #c:tools/shear},
         * which both loaders fill; on 1.20.1 no one convention tag holds them all, so the mod's own tag
         * takes vanilla's shears and each convention's, optionally.
         */
        public static final TagKey<Item> SHEARS = createTag("shears");
        /** Bone meal and every mod's fertiliser, the same way: what feeds a Grow Pot. */
        public static final TagKey<Item> FERTILIZERS = createTag("fertilizers");
        private static TagKey<Item> createTag(String name) {
            return TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation(Hempdustry.MOD_ID, name));
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
            return TagKey.create(Registry.ITEM_REGISTRY, new ResourceLocation("c", path));
        }

        private static TagKey<Block> blockTag(String path) {
            return TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("c", path));
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
        /** Counts as a Grow Lamp over a hemp plant, whatever its light level — see {@code GrowLight}. */
        public static final TagKey<Block> GROW_LAMPS = createTag("grow_lamps");
        /** Bright, but not a light over a plant: never counted, whatever its light level. */
        public static final TagKey<Block> NOT_GROW_LIGHTS = createTag("not_grow_lights");
        /**
         * The ground Beldía grows on, and generates on, outside a bed of ours. A tag of our own
         * rather than {@code #c:sands}, which not every Fabric API has, or {@code #minecraft:sand},
         * which holds suspicious sand. It takes {@code #c:sands} in optionally, so another mod's sand
         * joins wherever that tag exists, and a datapack can widen or narrow it here.
         */
        public static final TagKey<Block> BELDIA_SOIL = createTag("beldia_soil");
        private static TagKey<Block> createTag(String name) {
            return TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(Hempdustry.MOD_ID, name));
        }
    }
}
