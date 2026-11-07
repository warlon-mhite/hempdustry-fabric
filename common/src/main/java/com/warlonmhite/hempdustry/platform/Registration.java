package com.warlonmhite.hempdustry.platform;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/**
 * What the mod registers that vanilla gives no public way to register, or that the loader keeps a
 * table of its own for. Called while the mod's content is being registered.
 */
public interface Registration {

    /**
     * Builds an entity type of ours, named {@code id}. Vanilla's own build asks the data fixer about
     * the id and logs an error for one it does not know, which is every mod's.
     */
    <T extends Entity> EntityType<T> entityType(EntityType.Builder<T> builder, ResourceLocation id);

    /**
     * Lets one of vanilla's block entity types live in our blocks too — a sign. Without it, placing
     * the block throws, because a block entity type checks its blocks.
     */
    void addValidBlocks(BlockEntityType<?> type, Block... blocks);

    /**
     * Makes {@code item} the item of {@code block} for {@code Block#asItem}, as a {@code BlockItem} is:
     * for a block whose item is not one (a placed bong is its device). Forge rebuilds that map from the
     * block items whenever a world loads, so it is put back each time.
     */
    void blockItem(Block block, Item item);

    /**
     * Makes {@code states} points of interest of the type {@code poi} -- the Hemp Bed's head a
     * villager's home. Vanilla keeps one map from state to type; Forge rebuilds it from each type's own
     * list of states, so the type's list takes them too.
     */
    void addPointOfInterestStates(PoiType poi, Set<BlockState> states);

    /** A wood type of our own, named {@code id}, for the signs. Vanilla's own registration is private. */
    WoodType registerWoodType(ResourceLocation id);

    /**
     * The mod's creative tab: {@code icon} its icon, {@code fill} what it lists, in order. A 1.16.5 tab
     * is a class of its own the loader places and pages.
     */
    CreativeModeTab creativeTab(String id, Component title, Supplier<ItemStack> icon, Consumer<NonNullList<ItemStack>> fill);

    /**
     * Names the tool that mines a block quickly -- {@code "axe"}, {@code "pickaxe"}, {@code "shovel"},
     * {@code "hoe"}. Minecraft 1.16.5 has no {@code #minecraft:mineable/*} tags; Forge keeps the tool on
     * the block's properties, so these name the same tools the newer lines' tags do.
     */
    BlockBehaviour.Properties harvestTool(BlockBehaviour.Properties properties, String tool);

    /**
     * A banner pattern of our own. On 1.16.5 vanilla's patterns are an enum only Forge can extend; its
     * texture is {@code minecraft:entity/banner/<file>} (and {@code shield/}), so {@code file} carries
     * the mod's prefix. {@code hash} is what a banner stores; {@code needsItem}, whether the loom asks
     * for the pattern item.
     */
    BannerPattern bannerPattern(String name, String file, String hash, boolean needsItem);

    /** Makes {@code item} compostable with the given chance of raising the composter's level. */
    void compostable(ItemLike item, float chance);

    /** Makes {@code item} burn in a furnace for {@code ticks}. */
    void fuel(ItemLike item, int ticks);

    /** Makes {@code block} catch fire ({@code igniteOdds}) and burn away ({@code burnOdds}) like vanilla's. */
    void flammable(Block block, int igniteOdds, int burnOdds);

    /** Places {@code feature} in every biome {@code biomes} takes, at {@code step}. */
    void addFeature(BiomeSelector biomes, GenerationStep.Decoration step, Supplier<ConfiguredFeature<?, ?>> feature);

    /**
     * Which biomes a feature goes in. Minecraft 1.16.5 has no biome tags: a biome is picked by its id,
     * and a modded one by the category it declares.
     */
    @FunctionalInterface
    interface BiomeSelector {
        boolean test(ResourceLocation biome, Biome.BiomeCategory category);
    }
}
