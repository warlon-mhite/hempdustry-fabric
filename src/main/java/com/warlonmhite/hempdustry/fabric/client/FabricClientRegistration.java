package com.warlonmhite.hempdustry.fabric.client;

import com.warlonmhite.hempdustry.mixin.client.ModelPredicateProviderRegistryAccessor;
import com.warlonmhite.hempdustry.platform.client.ClientRegistration;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.client.resources.model.BakedModel;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class FabricClientRegistration implements ClientRegistration {
    @Override
    public void blockLayer(Block block, RenderType layer) {
        BlockRenderLayerMap.INSTANCE.putBlock(block, layer);
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
    public <T extends BlockEntity> void blockEntityRenderer(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> renderer) {
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
    public void itemColor(ItemColor color, ItemLike... items) {
        ColorProviderRegistry.ITEM.register(color, items);
    }

    @Override
    public void itemProperty(Item item, ResourceLocation id, ItemPropertyFunction property) {
        // Straight onto vanilla's map, as its own register does: that one takes only a clamped function.
        ModelPredicateProviderRegistryAccessor.getItemSpecific()
                .computeIfAbsent(item, key -> new HashMap<>())
                .put(id, property);
    }

    @Override
    public void extraModels(Collection<ResourceLocation> ids) {
        List<ResourceLocation> models = List.copyOf(ids);
        ModelLoadingPlugin.register(context -> context.addModels(models));
    }

    @Override
    public @Nullable BakedModel extraModel(ResourceLocation id) {
        return Minecraft.getInstance().getModelManager().getModel(id);
    }

    @Override
    public void composedModel(ResourceLocation id, List<ResourceLocation> parts,
                              BiFunction<BakedModel, List<BakedModel>, BakedModel> compose) {
        ModelLoadingPlugin.register(context -> context.modifyModelAfterBake().register((model, bake) -> {
            if (!id.equals(bake.resourceId())) {
                return model;
            }
            return compose.apply(model, parts.stream().map(part -> bake.baker().bake(part, bake.settings())).toList());
        }));
    }

    @Override
    public void onJoinWorld(Runnable listener) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> listener.run());
    }
}
