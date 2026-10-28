package com.warlonmhite.hempdustry.fabric;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.platform.Registration;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.fabricmc.fabric.api.registry.CompostableRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelValueEvents;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricRegistration implements Registration {
    /** Handed to every fuel-table build, in the order they were registered. */
    private final Map<ItemLike, Integer> fuels = new LinkedHashMap<>();

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(BlockEntityFactory<? extends T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.<T>create(factory::create, blocks).build();
    }

    @Override
    public void addValidBlocks(BlockEntityType<?> type, Block... blocks) {
        for (Block block : blocks) {
            ((FabricBlockEntityType) type).addValidBlock(block);
        }
    }

    @Override
    public BlockSetType registerBlockSetType(Identifier id, BlockSetType template) {
        return BlockSetTypeBuilder.copyOf(template).register(id);
    }

    @Override
    public WoodType registerWoodType(Identifier id, WoodType template, BlockSetType setType) {
        return WoodTypeBuilder.copyOf(template).register(id, setType);
    }

    @Override
    public <T> void registerSyncedDynamicRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        DynamicRegistries.registerSynced(key, codec);
    }

    @Override
    public void syncRecipes(RecipeType<?> type, RecipeSerializer<?> serializer) {
        RecipeSynchronization.synchronizeRecipeSerializer(serializer);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricCreativeModeTab.builder();
    }

    @Override
    public void compostable(ItemLike item, float chance) {
        CompostableRegistry.INSTANCE.add(item, chance);
    }

    @Override
    public void fuel(ItemLike item, int ticks) {
        if (fuels.isEmpty()) {
            FuelValueEvents.BUILD.register((builder, context) -> fuels.forEach(builder::add));
        }
        fuels.put(item, ticks);
    }

    @Override
    public void flammable(Block block, int igniteOdds, int burnOdds) {
        // Fabric calls them burn and spread: the odds of catching fire, and of burning away.
        FlammableBlockRegistry.getDefaultInstance().add(block, igniteOdds, burnOdds);
    }

    @Override
    public void addFeature(TagKey<Biome> biomes, GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature) {
        BiomeModifications.addFeature(BiomeSelectors.tag(biomes), step, feature);
    }
}
