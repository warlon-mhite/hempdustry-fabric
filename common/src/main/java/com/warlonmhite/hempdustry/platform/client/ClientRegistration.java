package com.warlonmhite.hempdustry.platform.client;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What the client registers to draw the mod: each loader has its own place for every one of these,
 * and some are only open while the loader says so. Called once, while the client starts.
 */
public interface ClientRegistration {

    void modelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition);

    <T extends Entity> void entityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> renderer);

    <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntityRenderer(
            BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> renderer);

    <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void menuScreen(
            MenuType<? extends M> type, ScreenFactory<M, U> screen);

    <T extends ParticleOptions> void particleProvider(
            ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider);

    /** The tint sources a block model's {@code tintindex} picks from, in order, for each of {@code blocks}. */
    void blockTint(List<BlockTintSource> tints, Block... blocks);

    /** A numeric item-model property of our own, named in client item definitions by {@code id}. */
    void rangeSelectProperty(Identifier id, MapCodec<? extends RangeSelectItemModelProperty> codec);

    /** An item tint source of our own, named in client item definitions by {@code id}. */
    void itemTintSource(Identifier id, MapCodec<? extends ItemTintSource> codec);

    /** Each time the player joins a world or a server. */
    void onJoinWorld(Runnable listener);

    /** A menu screen's constructor. Vanilla's own interface for it is not public. */
    @FunctionalInterface
    interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, Inventory inventory, Component title);
    }
}
