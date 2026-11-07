package com.warlonmhite.hempdustry.forge;

import com.google.common.collect.ImmutableSet;
import com.warlonmhite.hempdustry.platform.Registration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.world.BiomeLoadingEvent;
import net.minecraftforge.registries.GameData;

/**
 * What the mod registers that vanilla keeps no public way to register, Forge's way.
 *
 * <p>Compost chances go into vanilla's own table, which Forge 36's composter reads. Fuel times are
 * Forge's burn-time event's answer: vanilla's furnace builds its fuel table afresh from a fixed list.
 * The features added to biomes go in as each biome loads (Forge's biome loading event), picked by
 * id and category: 1.16.5 has no biome tags.
 */
public final class ForgeRegistration implements Registration {

    private final List<WoodType> woodTypes = new ArrayList<>();
    private final Map<Item, Integer> fuels = new ConcurrentHashMap<>();
    private final List<Feature> features = new ArrayList<>();
    private final Map<PoiType, Set<BlockState>> pointsOfInterest = new LinkedHashMap<>();
    private final Map<Block, Item> blockItems = new ConcurrentHashMap<>();

    /** A feature to place: in which biomes, at which step. */
    private static final class Feature {
        final BiomeSelector biomes;
        final GenerationStep.Decoration step;
        final Supplier<ConfiguredFeature<?, ?>> feature;

        Feature(BiomeSelector biomes, GenerationStep.Decoration step, Supplier<ConfiguredFeature<?, ?>> feature) {
            this.biomes = biomes;
            this.step = step;
            this.feature = feature;
        }
    }

    @Override
    public <T extends Entity> EntityType<T> entityType(EntityType.Builder<T> builder, ResourceLocation id) {
        // Vanilla's build looks the id up in the data fixer, which knows ours (Schema1460Mixin).
        return builder.build(id.toString());
    }

    @Override
    public void addValidBlocks(BlockEntityType<?> type, Block... blocks) {
        type.validBlocks = ImmutableSet.<Block>builder().addAll(type.validBlocks).add(blocks).build();
    }

    @Override
    public void addPointOfInterestStates(PoiType poi, Set<BlockState> states) {
        pointsOfInterest.put(poi, ImmutableSet.copyOf(states));
        putPointsOfInterest();
    }

    @Override
    public void blockItem(Block block, Item item) {
        blockItems.put(block, item);
        Item.BY_BLOCK.put(block, item);
    }

    /**
     * Forge rebuilds both maps -- block to item, state to point of interest -- from its registries
     * whenever the ids are remapped (a world loading), so ours are put back each time. Called from the
     * mod bus's id-mapping event ({@code HempdustryForge}).
     */
    void onIdMapping() {
        putPointsOfInterest();
        Item.BY_BLOCK.putAll(blockItems);
    }

    private void putPointsOfInterest() {
        Map<BlockState, PoiType> byState = GameData.getBlockStatePointOfInterestTypeMap();
        pointsOfInterest.forEach((type, states) -> {
            for (BlockState state : states) {
                PoiType taken = byState.putIfAbsent(state, type);
                if (taken != null && taken != type) {
                    throw new IllegalStateException(state + " is already a point of interest of " + taken);
                }
            }
        });
    }

    @Override
    public WoodType registerWoodType(ResourceLocation id) {
        WoodType type = WoodType.register(WoodType.create(id.toString()));
        woodTypes.add(type);
        return type;
    }

    @Override
    public CreativeModeTab creativeTab(String id, Component title, Supplier<ItemStack> icon,
                                       Consumer<NonNullList<ItemStack>> fill) {
        return new CreativeModeTab(id) {
            @Override
            public ItemStack makeIcon() {
                return icon.get();
            }

            @Override
            public void fillItemList(NonNullList<ItemStack> items) {
                fill.accept(items);
            }

            @Override
            public Component getDisplayName() {
                return title;
            }
        };
    }

    @Override
    public BlockBehaviour.Properties harvestTool(BlockBehaviour.Properties properties, String tool) {
        return properties.harvestTool(ToolType.get(tool));
    }

    @Override
    public BannerPattern bannerPattern(String name, String file, String hash, boolean needsItem) {
        return BannerPattern.create(name, file, hash, needsItem);
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
    public void addFeature(BiomeSelector biomes, GenerationStep.Decoration step, Supplier<ConfiguredFeature<?, ?>> feature) {
        features.add(new Feature(biomes, step, feature));
    }

    void burnTime(FurnaceFuelBurnTimeEvent event) {
        Integer ticks = fuels.get(event.getItemStack().getItem());
        if (ticks != null) {
            event.setBurnTime(ticks);
        }
    }

    /** Adds every feature whose biomes take this one, as Forge loads it. */
    void loadBiome(BiomeLoadingEvent event) {
        ResourceLocation biome = event.getName();
        if (biome == null) {
            return;
        }
        for (Feature feature : features) {
            if (feature.biomes.test(biome, event.getCategory())) {
                event.getGeneration().addFeature(feature.step, feature.feature.get());
            }
        }
    }

    public Map<Item, Integer> fuels() {
        return Collections.unmodifiableMap(fuels);
    }

    public List<WoodType> woodTypes() {
        return Collections.unmodifiableList(woodTypes);
    }
}
