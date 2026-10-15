package com.warlonmhite.hempdustry.neoforge.client;

import com.warlonmhite.hempdustry.HempdustryClient;
import com.warlonmhite.hempdustry.mixin.client.ModelPredicateProviderRegistryAccessor;
import com.warlonmhite.hempdustry.neoforge.Pending;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
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
 * Each of these has an event of its own on NeoForge, fired while the client starts; the mod's client
 * registers in one pass ({@link HempdustryClient#init}). That pass runs at the first of these events
 * -- after the game's registries are done, so every block and item exists -- and each event then
 * takes what was left for it ({@link Pending}). The two model events fire again on every resource
 * reload, so what they take is kept and handed to each.
 */
public final class NeoForgeClientRegistration implements ClientRegistration {
    private final Pending<FMLClientSetupEvent> setup = new Pending<>("block render layers and item properties");
    private final Pending<EntityRenderersEvent.RegisterLayerDefinitions> modelLayers = new Pending<>("model layers");
    private final Pending<EntityRenderersEvent.RegisterRenderers> renderers = new Pending<>("renderers");
    private final Pending<RegisterMenuScreensEvent> screens = new Pending<>("menu screens");
    private final Pending<RegisterParticleProvidersEvent> particles = new Pending<>("particle providers");
    private final Pending<RegisterColorHandlersEvent.Block> blockColors = new Pending<>("block colours");
    private final Pending<RegisterColorHandlersEvent.Item> itemColors = new Pending<>("item colours");
    private final List<ResourceLocation> extraModels = new ArrayList<>();
    private final List<Consumer<Map<ModelResourceLocation, BakedModel>>> bakedModels = new ArrayList<>();
    private boolean registered;

    /** Listens for every event the client's registrations go to; called by the client's entry point. */
    void listen(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> {
            init();
            event.enqueueWork(() -> setup.handOver(event));
        });
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> init().modelLayers.handOver(event));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> init().renderers.handOver(event));
        modBus.addListener(RegisterMenuScreensEvent.class, event -> init().screens.handOver(event));
        modBus.addListener(RegisterParticleProvidersEvent.class, event -> init().particles.handOver(event));
        modBus.addListener(RegisterColorHandlersEvent.Block.class, event -> init().blockColors.handOver(event));
        modBus.addListener(RegisterColorHandlersEvent.Item.class, event -> init().itemColors.handOver(event));
        modBus.addListener(ModelEvent.RegisterAdditional.class, event -> {
            for (ResourceLocation id : init().extraModels) {
                event.register(ModelResourceLocation.standalone(id));
            }
        });
        modBus.addListener(ModelEvent.ModifyBakingResult.class, event -> init().bakedModels.forEach(each -> each.accept(event.getModels())));
    }

    private synchronized NeoForgeClientRegistration init() {
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
        screens.add(event -> event.register(type, screen::create));
    }

    @Override
    public <T extends ParticleOptions> void particleProvider(
            ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider) {
        particles.add(event -> event.registerSpriteSet(type, provider::apply));
    }

    @Override
    public void blockColor(BlockColor color, Block... blocks) {
        blockColors.add(event -> event.register(color, blocks));
    }

    @Override
    public void itemColor(ItemColor color, ItemLike... items) {
        itemColors.add(event -> event.register(color, items));
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
        BakedModel model = models.getModel(ModelResourceLocation.standalone(id));
        return model == models.getMissingModel() ? null : model;
    }

    /**
     * NeoForge keys an item's own model by the item ({@code <id>#inventory}), so an {@code item/}
     * model id is looked up that way; the parts are baked as extra models.
     */
    @Override
    public void composedModel(ResourceLocation id, List<ResourceLocation> parts,
                              BiFunction<BakedModel, List<BakedModel>, BakedModel> compose) {
        extraModels(parts);
        ModelResourceLocation key = id.getPath().startsWith("item/")
                ? ModelResourceLocation.inventory(id.withPath(id.getPath().substring("item/".length())))
                : ModelResourceLocation.standalone(id);
        bakedModels.add(models -> {
            BakedModel model = models.get(key);
            if (model != null) {
                models.put(key, compose.apply(model,
                        parts.stream().map(part -> models.get(ModelResourceLocation.standalone(part))).toList()));
            }
        });
    }

    @Override
    public void onJoinWorld(Runnable listener) {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> listener.run());
    }
}
