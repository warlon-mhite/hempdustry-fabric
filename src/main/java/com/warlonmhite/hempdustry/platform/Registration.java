package com.warlonmhite.hempdustry.platform;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * What the mod registers that vanilla gives no public way to register, or that each loader keeps a
 * table of its own for. Called while the mod's content is being registered.
 */
public interface Registration {

    /** A block entity type for {@code blocks}; vanilla's constructor is private. */
    <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityFactory<? extends T> factory, Block... blocks);

    /**
     * Lets one of vanilla's block entity types live in our blocks too — a sign, a hanging sign, a
     * shelf. Without it, placing the block throws, because a block entity type checks its blocks.
     */
    void addValidBlocks(BlockEntityType<?> type, Block... blocks);

    /** Registers a copy of {@code template} named {@code id}. Vanilla's own registration is private. */
    BlockSetType registerBlockSetType(Identifier id, BlockSetType template);

    /** Registers a copy of {@code template} named {@code id}, made of {@code setType}. */
    WoodType registerWoodType(Identifier id, WoodType template, BlockSetType setType);

    /**
     * A dynamic registry of our own, read from datapacks and sent to every client, so a datapack's
     * entries reach a client without a packet of ours.
     */
    <T> void registerSyncedDynamicRegistry(ResourceKey<Registry<T>> key, Codec<T> codec);

    /**
     * Sends recipes of a custom type to the client, which a recipe viewer and the recipe book read.
     * Since 1.21.2 the server keeps recipes to itself unless asked.
     */
    void syncRecipeSerializer(RecipeSerializer<?> serializer);

    /** A builder for a creative tab of our own, placed and paged by the loader. */
    CreativeModeTab.Builder creativeTabBuilder();

    /** Makes {@code item} compostable with the given chance of raising the composter's level. */
    void compostable(ItemLike item, float chance);

    /** Makes {@code item} burn in a furnace for {@code ticks}. */
    void fuel(ItemLike item, int ticks);

    /** Makes {@code block} catch fire ({@code igniteOdds}) and burn away ({@code burnOdds}) like vanilla's. */
    void flammable(Block block, int igniteOdds, int burnOdds);

    /** Places {@code feature} in every biome in {@code biomes}, at {@code step}. */
    void addFeature(TagKey<Biome> biomes, GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature);

    /** A block entity's constructor. Vanilla's own interface for it is not public. */
    @FunctionalInterface
    interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }
}
