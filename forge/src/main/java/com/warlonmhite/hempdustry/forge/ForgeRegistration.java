package com.warlonmhite.hempdustry.forge;

import java.util.LinkedHashMap;
import net.minecraftforge.registries.IdMappingEvent;
import net.minecraftforge.common.MinecraftForge;
import java.util.Set;
import net.minecraftforge.registries.GameData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.core.registries.BuiltInRegistries;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.mixin.BlockEntityTypeAccessor;
import com.warlonmhite.hempdustry.platform.Registration;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.registries.DataPackRegistryEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge takes the datapack registries in an event of its own, fired at a set point while the game
 * starts; the mod registers in one pass ({@code Hempdustry.init}). So what cannot be done on the spot
 * is kept here and handed over when its event comes ({@link Pending}), and a call that arrives after
 * its event has gone fails loudly rather than doing nothing.
 *
 * <p>Compost chances go into vanilla's own table, which Forge 47's composter reads. Fuel times are
 * Forge's burn-time event's answer: vanilla's furnace builds its fuel table afresh from a fixed list.
 * The features added to biomes are data on Forge, read from the mod's datapack, so here they are only
 * written down: the data generator turns them into biome modifiers ({@code ForgeData}), and the files
 * are committed.
 */
public final class ForgeRegistration implements Registration {
    private final Pending<DataPackRegistryEvent.NewRegistry> dynamicRegistries = new Pending<>("datapack registries");
    private final List<WoodType> woodTypes = new ArrayList<>();
    private final Map<Item, Integer> fuels = new ConcurrentHashMap<>();
    private final List<Feature> features = new ArrayList<>();
    private final Map<ResourceKey<PoiType>, Set<BlockState>> pointsOfInterest = new LinkedHashMap<>();
    private final Map<Block, Item> blockItems = new ConcurrentHashMap<>();
    private boolean listening;

    /** A feature added to every biome in a tag, at a step: what one biome modifier file says. */
    public record Feature(TagKey<Biome> biomes, GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature) {
    }

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

    /**
     * On Forge 47 the state-to-type map is built from each type's own states, and rebuilt whenever a
     * world's registry snapshot is loaded; a point of interest type is a record that cannot take a
     * state of its own. So the states go into the map now, and back in after each rebuild.
     */
    @Override
    public void addPointOfInterestStates(ResourceKey<PoiType> poi, Set<BlockState> states) {
        listenForRebuilds();
        pointsOfInterest.put(poi, Set.copyOf(states));
        putPointsOfInterest();
    }

    @Override
    public void blockItem(Block block, Item item) {
        listenForRebuilds();
        blockItems.put(block, item);
        Item.BY_BLOCK.put(block, item);
    }

    /** Forge rebuilds its registries' side maps after a world's snapshot loads; what the mod adds goes back in. */
    private synchronized void listenForRebuilds() {
        if (!listening) {
            listening = true;
            MinecraftForge.EVENT_BUS.addListener((IdMappingEvent event) -> {
                putPointsOfInterest();
                Item.BY_BLOCK.putAll(blockItems);
            });
        }
    }

    private void putPointsOfInterest() {
        Map<BlockState, PoiType> byState = GameData.getBlockStatePointOfInterestTypeMap();
        pointsOfInterest.forEach((key, states) -> {
            PoiType type = BuiltInRegistries.POINT_OF_INTEREST_TYPE.getOrThrow(key);
            for (BlockState state : states) {
                PoiType taken = byState.putIfAbsent(state, type);
                if (taken != null && taken != type) {
                    throw new IllegalStateException(state + " is already a point of interest of " + taken);
                }
            }
        });
    }

    @Override
    public BlockSetType registerBlockSetType(ResourceLocation id, BlockSetType template) {
        return BlockSetType.register(new BlockSetType(id.toString(), template.canOpenByHand(),
                template.soundType(), template.doorClose(), template.doorOpen(),
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
        ComposterBlock.COMPOSTABLES.put(item.asItem(), chance);
    }

    @Override
    public void fuel(ItemLike item, int ticks) {
        fuels.put(item.asItem(), ticks);
    }

    @Override
    public void flammable(Block block, int igniteOdds, int burnOdds) {
        // Vanilla's own table, which Forge's fire reads unless a block says otherwise.
        ((FireBlock) Blocks.FIRE).setFlammable(block, igniteOdds, burnOdds);
    }

    @Override
    public void addFeature(TagKey<Biome> biomes, GenerationStep.Decoration step, ResourceKey<PlacedFeature> feature) {
        features.add(new Feature(biomes, step, feature));
    }

    void onNewDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        dynamicRegistries.handOver(event);
    }

    /** A furnace's question: how long this burns, if it is one of the mod's fuels. */
    void burnTime(FurnaceFuelBurnTimeEvent event) {
        Integer ticks = fuels.get(event.getItemStack().getItem());
        if (ticks != null) {
            event.setBurnTime(ticks);
        }
    }

    /** The mod's fuels, item by item. */
    public Map<Item, Integer> fuels() {
        return Collections.unmodifiableMap(fuels);
    }

    /** The wood types registered, which a client has to know to draw their signs. */
    public List<WoodType> woodTypes() {
        return Collections.unmodifiableList(woodTypes);
    }

    public List<Feature> features() {
        return Collections.unmodifiableList(features);
    }
}
