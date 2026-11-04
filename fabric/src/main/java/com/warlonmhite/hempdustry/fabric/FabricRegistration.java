package com.warlonmhite.hempdustry.fabric;

import net.minecraft.world.item.Item;
import java.util.Set;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.core.registries.BuiltInRegistries;
import com.warlonmhite.hempdustry.mixin.PointOfInterestTypesAccessor;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.mixin.BlockEntityTypeAccessor;
import com.warlonmhite.hempdustry.platform.Registration;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
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
        // Vanilla's build looks the id up in the data fixer, which knows ours (Schema1460Mixin).
        return builder.build(id.toString());
    }

    @Override
    public void addValidBlocks(BlockEntityType<?> type, Block... blocks) {
        BlockEntityTypeAccessor accessor = (BlockEntityTypeAccessor) type;
        accessor.hempdustry$setValidBlocks(ImmutableSet.<Block>builder()
                .addAll(accessor.hempdustry$validBlocks()).add(blocks).build());
    }

    @Override
    public void blockItem(Block block, Item item) {
        Item.BY_BLOCK.put(block, item);
    }

    @Override
    public void addPointOfInterestStates(ResourceKey<PoiType> poi, Set<BlockState> states) {
        PointOfInterestTypesAccessor.hempdustry$registerStates(BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(poi), states);
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
