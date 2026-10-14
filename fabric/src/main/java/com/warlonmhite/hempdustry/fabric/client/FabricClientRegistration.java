package com.warlonmhite.hempdustry.fabric.client;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.platform.client.ClientRegistration;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
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

import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricClientRegistration implements ClientRegistration {
    @Override
    public void blockLayer(Block block, ChunkSectionLayer layer) {
        BlockRenderLayerMap.putBlock(block, layer);
    }

    @Override
    public void modelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        EntityModelLayerRegistry.registerModelLayer(layer, definition::get);
    }

    @Override
    public <T extends Entity> void entityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> renderer) {
        EntityRendererRegistry.register(type, renderer);
    }

    @Override
    public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
            BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> renderer) {
        BlockEntityRenderers.register(type, renderer);
    }

    @Override
    public <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void menuScreen(
            MenuType<? extends M> type, ScreenFactory<M, U> screen) {
        MenuScreens.register(type, screen::create);
    }

    @Override
    public <T extends ParticleOptions> void particleProvider(
            ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider) {
        ParticleFactoryRegistry.getInstance().register(type, provider::apply);
    }

    @Override
    public void blockColor(BlockColor color, Block... blocks) {
        ColorProviderRegistry.BLOCK.register(color, blocks);
    }

    @Override
    public void rangeSelectProperty(Identifier id, MapCodec<? extends RangeSelectItemModelProperty> codec) {
        RangeSelectItemModelProperties.ID_MAPPER.put(id, codec);
    }

    @Override
    public void itemTintSource(Identifier id, MapCodec<? extends ItemTintSource> codec) {
        ItemTintSources.ID_MAPPER.put(id, codec);
    }

    @Override
    public void onJoinWorld(Runnable listener) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> listener.run());
    }
}
