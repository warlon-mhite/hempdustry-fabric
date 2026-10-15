package com.warlonmhite.hempdustry.fabric;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.platform.Registration;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.block.type.WoodTypeBuilder;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public final class FabricRegistration implements Registration {
    @Override
    public <T extends Entity> EntityType<T> entityType(EntityType.Builder<T> builder, ResourceLocation id) {
        // Fabric API's own build(): no data fixer lookup for an id the game does not know.
        return builder.build();
    }

    @Override
    public void addValidBlocks(BlockEntityType<?> type, Block... blocks) {
        for (Block block : blocks) {
            ((FabricBlockEntityType) type).addSupportedBlock(block);
        }
    }

    @Override
    public BlockSetType registerBlockSetType(ResourceLocation id, BlockSetType template) {
        return BlockSetTypeBuilder.copyOf(template).register(id);
    }

    @Override
    public WoodType registerWoodType(ResourceLocation id, WoodType template, BlockSetType setType) {
        return WoodTypeBuilder.copyOf(template).register(id, setType);
    }

    @Override
    public <T> void registerSyncedDynamicRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        DynamicRegistries.registerSynced(key, codec);
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return FabricItemGroup.builder();
    }

    @Override
    public void compostable(ItemLike item, float chance) {
        CompostingChanceRegistry.INSTANCE.add(item, chance);
    }

    @Override
    public void fuel(ItemLike item, int ticks) {
        FuelRegistry.INSTANCE.add(item, ticks);
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
