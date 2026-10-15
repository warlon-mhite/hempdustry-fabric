package com.warlonmhite.hempdustry.platform.client;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * What the client registers to draw the mod: each loader has its own place for every one of these,
 * and some are only open while the loader says so. Called once, while the client starts.
 */
public interface ClientRegistration {

    /** Draws {@code block} in {@code layer} rather than solid — cut-out holes, see-through glass. */
    void blockLayer(Block block, RenderType layer);

    void modelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition);

    <T extends Entity> void entityRenderer(EntityType<? extends T> type, EntityRendererProvider<T> renderer);

    <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> renderer);

    <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void menuScreen(
            MenuType<? extends M> type, ScreenFactory<M, U> screen);

    <T extends ParticleOptions> void particleProvider(
            ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider);

    void blockColor(BlockColor color, Block... blocks);

    void itemColor(ItemColor color, ItemLike... items);

    /**
     * An item model property of {@code item}, which its model's overrides key on. Not clamped to
     * 0..1 unless {@code property} clamps itself, as vanilla's {@code ClampedItemPropertyFunction} does.
     */
    void itemProperty(Item item, ResourceLocation id, ItemPropertyFunction property);

    /** Models no block state or item names, loaded by id so code can draw them ({@link #extraModel}). */
    void extraModels(Collection<ResourceLocation> ids);

    /** One of the {@link #extraModels} as baked; {@code null} before the models have baked. */
    @Nullable
    BakedModel extraModel(ResourceLocation id);

    /**
     * Replaces the baked model {@code id} with what {@code compose} makes of it and of {@code parts},
     * baked too, in that order.
     */
    void composedModel(ResourceLocation id, List<ResourceLocation> parts,
                       BiFunction<BakedModel, List<BakedModel>, BakedModel> compose);

    /** Each time the player joins a world or a server. */
    void onJoinWorld(Runnable listener);

    /** A menu screen's constructor. Vanilla's own interface for it is not public. */
    @FunctionalInterface
    interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, Inventory inventory, Component title);
    }
}
