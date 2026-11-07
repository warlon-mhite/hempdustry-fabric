package com.warlonmhite.hempdustry.forge.client;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.NonNullList;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.client.searchtree.MutableSearchTree;
import com.warlonmhite.hempdustry.HempdustryClient;
import com.warlonmhite.hempdustry.mixin.client.ModelPredicateProviderRegistryAccessor;
import com.warlonmhite.hempdustry.forge.Pending;
import com.warlonmhite.hempdustry.platform.client.ClientRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ForgeModelBakery;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Each of these has an event of its own on Forge, fired while the client starts; the mod's client
 * registers in one pass ({@link HempdustryClient#init}). That pass runs at the first of these events
 * -- after the game's registries are done, so every block and item exists -- and each event then
 * takes what was left for it ({@link Pending}). The two model events fire again on every resource
 * reload, so what they take is kept and handed to each.
 */
public final class ForgeClientRegistration implements ClientRegistration {
    private final Pending<FMLClientSetupEvent> setup = new Pending<>("block render layers and item properties");
    private final Pending<EntityRenderersEvent.RegisterLayerDefinitions> modelLayers = new Pending<>("model layers");
    private final Pending<EntityRenderersEvent.RegisterRenderers> renderers = new Pending<>("renderers");
    private final Pending<ParticleFactoryRegisterEvent> particles = new Pending<>("particle providers");
    private final Pending<ColorHandlerEvent.Block> blockColors = new Pending<>("block colours");
    private final Pending<ColorHandlerEvent.Item> itemColors = new Pending<>("item colours");
    private final List<ResourceLocation> extraModels = new ArrayList<>();
    private final List<Consumer<Map<ResourceLocation, BakedModel>>> bakedModels = new ArrayList<>();
    private final List<Consumer<TextureStitchEvent.Pre>> sprites = new ArrayList<>();
    private boolean registered;

    /** Listens for every event the client's registrations go to; called by the client's entry point. */
    void listen(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> {
            init();
            event.enqueueWork(() -> setup.handOver(event));
        });
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> init().modelLayers.handOver(event));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> init().renderers.handOver(event));
        modBus.addListener((ParticleFactoryRegisterEvent event) -> init().particles.handOver(event));
        modBus.addListener((ColorHandlerEvent.Block event) -> init().blockColors.handOver(event));
        modBus.addListener((ColorHandlerEvent.Item event) -> init().itemColors.handOver(event));
        modBus.addListener((ModelRegistryEvent event) -> init().extraModels.forEach(ForgeModelBakery::addSpecialModel));
        modBus.addListener((ModelBakeEvent event) -> init().bakedModels.forEach(each -> each.accept(event.getModelRegistry())));
        modBus.addListener((TextureStitchEvent.Pre event) -> init().sprites.forEach(each -> each.accept(event)));
    }

    private synchronized ForgeClientRegistration init() {
        if (!registered) {
            registered = true;
            HempdustryClient.init();
        }
        return this;
    }

    @Override
    public void blockLayer(Block block, RenderType layer) {
        setup.add(event -> ItemBlockRenderTypes.setRenderLayer(block, layer));
    }

    @Override
    public void modelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        modelLayers.add(event -> event.registerLayerDefinition(layer, definition));
    }

    @Override
    public <T extends Entity> void entityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> renderer) {
        renderers.add(event -> event.registerEntityRenderer(type, renderer));
    }

    @Override
    public <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> renderer) {
        renderers.add(event -> event.registerBlockEntityRenderer(type, renderer));
    }

    @Override
    public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void menuScreen(
            MenuType<? extends M> type, ScreenFactory<M, U> screen) {
        // Forge 40 has no event for these: vanilla's own table, filled while the client sets up.
        setup.add(event -> MenuScreens.register(type, screen::create));
    }

    @Override
    public <T extends ParticleOptions> void particleProvider(
            ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider) {
        // Forge 40's event carries nothing: the particle engine is vanilla's, registered into during it.
        particles.add(event -> Minecraft.getInstance().particleEngine.register(type, provider::apply));
    }

    @Override
    public void blockColor(BlockColor color, Block... blocks) {
        blockColors.add(event -> event.getBlockColors().register(color, blocks));
    }

    @Override
    public void itemColor(ItemColor color, ItemLike... items) {
        itemColors.add(event -> event.getItemColors().register(color, items));
    }

    @Override
    public void itemProperty(Item item, ResourceLocation id, ItemPropertyFunction property) {
        // Straight onto vanilla's map, as its own register does: that one takes only a clamped function.
        setup.add(event -> ModelPredicateProviderRegistryAccessor.getItemSpecific()
                .computeIfAbsent(item, key -> new HashMap<>())
                .put(id, property));
    }

    @Override
    public void extraModels(Collection<ResourceLocation> ids) {
        extraModels.addAll(ids);
    }

    @Override
    public @Nullable BakedModel extraModel(ResourceLocation id) {
        ModelManager models = Minecraft.getInstance().getModelManager();
        BakedModel model = models.getModel(id);
        return model == models.getMissingModel() ? null : model;
    }

    /**
     * Forge keys an item's own model by the item ({@code <id>#inventory}), so an {@code item/}
     * model id is looked up that way; the parts are baked as extra models.
     */
    @Override
    public void composedModel(ResourceLocation id, List<ResourceLocation> parts,
                              BiFunction<BakedModel, List<BakedModel>, BakedModel> compose) {
        extraModels(parts);
        ResourceLocation key = id.getPath().startsWith("item/")
                ? new ModelResourceLocation(new ResourceLocation(id.getNamespace(), id.getPath().substring("item/".length())), "inventory")
                : id;
        bakedModels.add(models -> {
            BakedModel model = models.get(key);
            if (model != null) {
                models.put(key, compose.apply(model,
                        parts.stream().map(models::get).toList()));
            }
        });
    }

    @Override
    public void atlasSprite(ResourceLocation atlas, ResourceLocation sprite) {
        sprites.add(event -> {
            if (event.getAtlas().location().equals(atlas)) {
                event.addSprite(sprite);
            }
        });
    }

    @Override
    public void onJoinWorld(Runnable listener) {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedInEvent event) -> listener.run());
    }


    /**
     * What {@code Minecraft#createSearchTrees} puts in the two item searches, put there again: every
     * item's search-tab stacks, by name and tooltip, and by tag.
     */
    @Override
    public void refreshCreativeSearch() {
        Minecraft client = Minecraft.getInstance();
        NonNullList<ItemStack> stacks = NonNullList.create();
        for (Item item : Registry.ITEM) {
            item.fillItemCategory(CreativeModeTab.TAB_SEARCH, stacks);
        }
        MutableSearchTree<ItemStack> names = client.getSearchTree(SearchRegistry.CREATIVE_NAMES);
        MutableSearchTree<ItemStack> tags = client.getSearchTree(SearchRegistry.CREATIVE_TAGS);
        names.clear();
        tags.clear();
        for (ItemStack stack : stacks) {
            names.add(stack);
            tags.add(stack);
        }
        names.refresh();
        tags.refresh();
    }

}
