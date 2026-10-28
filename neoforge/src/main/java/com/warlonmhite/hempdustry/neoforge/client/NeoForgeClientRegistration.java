package com.warlonmhite.hempdustry.neoforge.client;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.HempdustryClient;
import com.warlonmhite.hempdustry.neoforge.Pending;
import com.warlonmhite.hempdustry.platform.client.ClientRegistration;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Each of these has an event of its own on NeoForge, fired once while the client starts; the mod's
 * client registers in one pass ({@link HempdustryClient#init}). That pass runs at the first of these
 * events -- after the game's registries are done, so every block and item exists -- and each event
 * then takes what was left for it ({@link Pending}).
 */
public final class NeoForgeClientRegistration implements ClientRegistration {
    private final Pending<EntityRenderersEvent.RegisterLayerDefinitions> modelLayers = new Pending<>("model layers");
    private final Pending<EntityRenderersEvent.RegisterRenderers> renderers = new Pending<>("renderers");
    private final Pending<RegisterMenuScreensEvent> screens = new Pending<>("menu screens");
    private final Pending<RegisterParticleProvidersEvent> particles = new Pending<>("particle providers");
    private final Pending<RegisterColorHandlersEvent.BlockTintSources> blockTints = new Pending<>("block tints");
    private final Pending<RegisterRangeSelectItemModelPropertyEvent> itemProperties = new Pending<>("item model properties");
    private final Pending<RegisterColorHandlersEvent.ItemTintSources> tintSources = new Pending<>("item tint sources");
    private boolean registered;

    /** Listens for every event the client's registrations go to; called by the client's entry point. */
    void listen(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> init());
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> init().modelLayers.handOver(event));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> init().renderers.handOver(event));
        modBus.addListener(RegisterMenuScreensEvent.class, event -> init().screens.handOver(event));
        modBus.addListener(RegisterParticleProvidersEvent.class, event -> init().particles.handOver(event));
        modBus.addListener(RegisterColorHandlersEvent.BlockTintSources.class, event -> init().blockTints.handOver(event));
        modBus.addListener(RegisterRangeSelectItemModelPropertyEvent.class, event -> init().itemProperties.handOver(event));
        modBus.addListener(RegisterColorHandlersEvent.ItemTintSources.class, event -> init().tintSources.handOver(event));
    }

    private synchronized NeoForgeClientRegistration init() {
        if (!registered) {
            registered = true;
            HempdustryClient.init();
        }
        return this;
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
    public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
            BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> renderer) {
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
    public void blockTint(List<BlockTintSource> tints, Block... blocks) {
        blockTints.add(event -> event.register(tints, blocks));
    }

    @Override
    public void rangeSelectProperty(Identifier id, MapCodec<? extends RangeSelectItemModelProperty> codec) {
        itemProperties.add(event -> event.register(id, codec));
    }

    @Override
    public void itemTintSource(Identifier id, MapCodec<? extends ItemTintSource> codec) {
        tintSources.add(event -> event.register(id, codec));
    }

    @Override
    public void onJoinWorld(Runnable listener) {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> listener.run());
    }
}
