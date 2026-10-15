package com.warlonmhite.hempdustry.neoforge;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.platform.Registration;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * NeoForge takes most of these in an event of its own, fired at a set point while the game starts;
 * the mod registers in one pass ({@code Hempdustry.init}). So what cannot be done on the spot is kept
 * here and handed over when its event comes ({@link Pending}), and a call that arrives after its event
 * has gone fails loudly rather than doing nothing.
 *
 * <p>Compost chances, fuel times and the features added to biomes are data on NeoForge, read from
 * the mod's datapack, so here they are only written down: the data generator turns them into those
 * files ({@code NeoForgeData}), and the files are committed.
 */
public final class NeoForgeRegistration implements Registration {
    private final Pending<DataPackRegistryEvent.NewRegistry> dynamicRegistries = new Pending<>("datapack registries");
    private final Pending<BlockEntityTypeAddBlocksEvent> validBlocks = new Pending<>("block entity blocks");
    private final List<WoodType> woodTypes = new ArrayList<>();
    private final Map<ItemLike, Float> compostables = new LinkedHashMap<>();
    private final Map<ItemLike, Integer> fuels = new LinkedHashMap<>();
    private final List<Feature> features = new ArrayList<>();

    /** A feature added to every biome in a tag, at a step: what one biome modifier file says. */
    public record Feature(TagKey<Biome> biomes, GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature) {
    }

    @Override
    public <T extends Entity> EntityType<T> entityType(EntityType.Builder<T> builder, ResourceLocation id) {
        // NeoForge logs the data fixer's miss at debug; the id must not be null, which it does not survive.
        return builder.build(id.toString());
    }

    @Override
    public void addValidBlocks(BlockEntityType<?> type, Block... blocks) {
        validBlocks.add(event -> event.modify(type, blocks));
    }

    @Override
    public BlockSetType registerBlockSetType(ResourceLocation id, BlockSetType template) {
        return BlockSetType.register(new BlockSetType(id.toString(), template.canOpenByHand(),
                template.canOpenByWindCharge(), template.canButtonBeActivatedByArrows(),
                template.pressurePlateSensitivity(), template.soundType(), template.doorClose(), template.doorOpen(),
                template.trapdoorClose(), template.trapdoorOpen(), template.pressurePlateClickOff(),
                template.pressurePlateClickOn(), template.buttonClickOff(), template.buttonClickOn()));
    }

    @Override
    public WoodType registerWoodType(ResourceLocation id, WoodType template, BlockSetType setType) {
        WoodType type = WoodType.register(new WoodType(id.toString(), setType, template.soundType(),
                template.hangingSignSoundType(), template.fenceGateClose(), template.fenceGateOpen()));
        woodTypes.add(type);
        return type;
    }

    @Override
    public <T> void registerSyncedDynamicRegistry(ResourceKey<Registry<T>> key, Codec<T> codec) {
        dynamicRegistries.add(event -> event.dataPackRegistry(key, codec, codec));
    }

    @Override
    public CreativeModeTab.Builder creativeTabBuilder() {
        return CreativeModeTab.builder();
    }

    @Override
    public void compostable(ItemLike item, float chance) {
        compostables.put(item, chance);
    }

    @Override
    public void fuel(ItemLike item, int ticks) {
        fuels.put(item, ticks);
    }

    @Override
    public void flammable(Block block, int igniteOdds, int burnOdds) {
        // Vanilla's own table, which NeoForge's fire reads unless a block says otherwise.
        ((FireBlock) Blocks.FIRE).setFlammable(block, igniteOdds, burnOdds);
    }

    @Override
    public void addFeature(TagKey<Biome> biomes, GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature) {
        features.add(new Feature(biomes, step, feature));
    }

    void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        dynamicRegistries.handOver(event);
    }

    void onAddBlocks(BlockEntityTypeAddBlocksEvent event) {
        validBlocks.handOver(event);
    }

    /** The wood types registered, which a client has to know to draw their signs. */
    public List<WoodType> woodTypes() {
        return Collections.unmodifiableList(woodTypes);
    }

    public Map<ItemLike, Float> compostables() {
        return Collections.unmodifiableMap(compostables);
    }

    public Map<ItemLike, Integer> fuels() {
        return Collections.unmodifiableMap(fuels);
    }

    public List<Feature> features() {
        return Collections.unmodifiableList(features);
    }
}
