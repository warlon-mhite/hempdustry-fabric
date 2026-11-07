package com.warlonmhite.hempdustry.platform.client;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
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

/**
 * What the client registers to draw the mod: each loader has its own place for every one of these,
 * and some are only open while the loader says so. Called once, while the client starts.
 *
 * <p>1.16.5 has no entity model layers and no renderer contexts: a model builds its parts in its own
 * constructor, an entity renderer is made from the {@link EntityRenderDispatcher} and a block entity
 * renderer from the {@link BlockEntityRenderDispatcher}, so those are what the factories here take.
 */
public interface ClientRegistration {

    /** Draws {@code block} in {@code layer} rather than solid — cut-out holes, see-through glass. */
    void blockLayer(Block block, RenderType layer);

    <T extends Entity> void entityRenderer(EntityType<T> type,
                                           Function<EntityRenderDispatcher, ? extends EntityRenderer<? super T>> renderer);

    <T extends BlockEntity> void blockEntityRenderer(
            BlockEntityType<T> type,
            Function<BlockEntityRenderDispatcher, ? extends BlockEntityRenderer<? super T>> renderer);

    <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void menuScreen(
            MenuType<? extends M> type, ScreenFactory<M, U> screen);

    <T extends ParticleOptions> void particleProvider(
            ParticleType<T> type, Function<SpriteSet, ParticleProvider<T>> provider);

    void blockColor(BlockColor color, Block... blocks);

    void itemColor(ItemColor color, ItemLike... items);

    /**
     * An item model property of {@code item}, which its model's overrides key on. Never clamped:
     * 1.16.5 hands the overrides whatever the function returns.
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

    /**
     * Stitches the texture {@code sprite} into the atlas {@code atlas} ({@code Sheets.BED_SHEET} and
     * the like). The newer lines' atlases list their sprites in data, so any namespace's
     * {@code entity/bed/} is in the beds atlas; 1.16.5's take only what vanilla's code names, so a
     * modded bed's texture has to be asked for.
     */
    void atlasSprite(ResourceLocation atlas, ResourceLocation sprite);

    /** Each time the player joins a world or a server. */
    void onJoinWorld(Runnable listener);

    /**
     * Rebuilds the creative inventory's search from the items as they fill the search tab now.
     * Vanilla builds that search once, at resource load, before any world -- so stacks that depend
     * on what a server sent (every strain's) are missing from it until it is rebuilt. Call it on the
     * client thread once the server's data is in.
     */
    void refreshCreativeSearch();

    /** A menu screen's constructor. */
    @FunctionalInterface
    interface ScreenFactory<M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> {
        U create(M menu, Inventory inventory, Component title);
    }
}
