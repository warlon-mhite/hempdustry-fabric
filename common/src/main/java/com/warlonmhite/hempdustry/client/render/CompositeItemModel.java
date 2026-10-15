package com.warlonmhite.hempdustry.client.render;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An item model made of other baked models, drawn as one: what 1.21.11's {@code minecraft:composite}
 * item model does, for a version that has none.
 *
 * <p>The item's own model is the frame — its display transforms, lighting and particle — and the
 * parts are its quads, gathered once when the models bake. So the icon is as cheap to draw as any
 * item model, lit the way vanilla lights a block icon, and it is always the parts as they are: a
 * resource pack that redraws or reshapes one of them changes the icon with it, with no second copy
 * of their elements to keep in step.
 */
public final class CompositeItemModel implements BakedModel {
    private final BakedModel frame;
    private final List<BakedQuad> unculled;
    private final Map<Direction, List<BakedQuad>> culled = new EnumMap<>(Direction.class);

    public CompositeItemModel(BakedModel frame, List<BakedModel> parts) {
        this.frame = frame;
        // Plain JSON models: their quads depend on neither the block state nor the random.
        RandomSource random = RandomSource.create(42L);
        List<BakedQuad> faceless = new ArrayList<>();
        for (BakedModel part : parts) {
            faceless.addAll(part.getQuads(null, null, random));
        }
        this.unculled = List.copyOf(faceless);
        for (Direction side : Direction.values()) {
            List<BakedQuad> quads = new ArrayList<>();
            for (BakedModel part : parts) {
                quads.addAll(part.getQuads(null, side, random));
            }
            culled.put(side, List.copyOf(quads));
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random) {
        return face == null ? unculled : culled.get(face);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return frame.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return frame.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return frame.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return frame.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return frame.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
