package com.warlonmhite.hempdustry.forge.client;

import com.warlonmhite.hempdustry.HempdustryClient;
import com.warlonmhite.hempdustry.platform.client.ClientRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.searchtree.MutableSearchTree;
import net.minecraft.client.searchtree.SearchRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Each of these has an event of its own on Forge 36, or a table it fills while the client sets up;
 * the mod's client registers in one pass ({@link HempdustryClient#init}). That pass runs at the first
 * of these events -- after the game's registries are done, so every block and item exists -- and each
 * event then takes what was left for it ({@link ClientPending}). The model and atlas events fire again on
 * every resource reload, so what they take is kept and handed to each.
 *
 * <p>Forge 36 has no renderer events: entity and block entity renderers are handed to its rendering
 * registries during client setup, as are render layers, screens and item properties.
 */
public final class ForgeClientRegistration implements ClientRegistration {
    private final ClientPending<FMLClientSetupEvent> setup =
            new ClientPending<>("block render layers, screens and item properties");
    private final ClientPending<FMLClientSetupEvent> renderers = new ClientPending<>("renderers");
    private final ClientPending<ParticleFactoryRegisterEvent> particles = new ClientPending<>("particle providers");
    private final ClientPending<ColorHandlerEvent.Block> blockColors = new ClientPending<>("block colours");
    private final ClientPending<ColorHandlerEvent.Item> itemColors = new ClientPending<>("item colours");
    private final List<ResourceLocation> extraModels = new ArrayList<>();
    private final List<Consumer<Map<ResourceLocation, BakedModel>>> bakedModels = new ArrayList<>();
    private final List<Consumer<TextureStitchEvent.Pre>> sprites = new ArrayList<>();
    private boolean registered;

    /** Listens for every event the client's registrations go to; called by the client's entry point. */
    void listen(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> {
            init();
            // Forge's rendering registries are its own and thread-safe, and the entity renderers are
            // built from them right after this event, so they go in now. The rest are vanilla's plain
            // maps, filled on the main thread.
            renderers.handOver(event);
            event.enqueueWork(() -> setup.handOver(event));
        });
        modBus.addListener((ParticleFactoryRegisterEvent event) -> init().particles.handOver(event));
        modBus.addListener((ColorHandlerEvent.Block event) -> init().blockColors.handOver(event));
        modBus.addListener((ColorHandlerEvent.Item event) -> init().itemColors.handOver(event));
        modBus.addListener((ModelRegistryEvent event) -> init().extraModels.forEach(ModelLoader::addSpecialModel));
        modBus.addListener((ModelBakeEvent event) ->
                init().bakedModels.forEach(each -> each.accept(event.getModelRegistry())));
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
    public <T extends Entity> void entityRenderer(EntityType<T> type,
                                                  Function<EntityRenderDispatcher, ? extends EntityRenderer<? super T>> renderer) {
        renderers.add(event -> RenderingRegistry.registerEntityRenderingHandler(type, renderer::apply));
    }

    @Override
    public <T extends BlockEntity> void blockEntityRenderer(
            BlockEntityType<T> type,
            Function<BlockEntityRenderDispatcher, ? extends BlockEntityRenderer<? super T>> renderer) {
        renderers.add(event -> ClientRegistry.bindTileEntityRenderer(type, renderer));
    }

    @Override
    public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void menuScreen(
            MenuType<? extends M> type, ScreenFactory<M, U> screen) {
        // Forge 36 has no event for these: vanilla's own table, filled while the client sets up.
        setup.add(event -> MenuScreens.register(type, screen::create));
    }

    @Override
    public <T extends ParticleOptions> void particleProvider(
            ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider) {
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
        // Vanilla's own register, which Forge opens; on 1.16.5 it takes the property unclamped.
        setup.add(event -> ItemProperties.register(item, id, property));
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
                ? new ModelResourceLocation(
                        new ResourceLocation(id.getNamespace(), id.getPath().substring("item/".length())), "inventory")
                : id;
        bakedModels.add(models -> {
            BakedModel model = models.get(key);
            if (model != null) {
                models.put(key, compose.apply(model,
                        parts.stream().map(models::get).collect(Collectors.toList())));
            }
        });
    }

    @Override
    public void atlasSprite(ResourceLocation atlas, ResourceLocation sprite) {
        sprites.add(event -> {
            if (event.getMap().location().equals(atlas)) {
                event.addSprite(sprite);
            }
        });
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

    @Override
    public void onJoinWorld(Runnable listener) {
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggedInEvent event) -> listener.run());
    }
}
