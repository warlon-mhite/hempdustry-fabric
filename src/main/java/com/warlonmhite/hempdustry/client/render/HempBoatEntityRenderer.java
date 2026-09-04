package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/**
 * Vanilla's boat renderer with one fixed texture.
 *
 * <p>It used to be a full reimplementation, because 1.21.1's {@code BoatEntityRenderer} looked its
 * texture up in a map keyed by the closed {@code BoatEntity.Type} enum, which a mod cannot join.
 * Since 1.21.2 the renderer takes an {@link EntityModelLayer} and exposes a single overridable
 * {@code getRenderLayer()}, so all that is left here is the texture — the geometry, the damage
 * wobble, the paddles and the water mask are vanilla's again.
 */
public class HempBoatEntityRenderer extends BoatEntityRenderer {
    private final RenderLayer layer;

    public HempBoatEntityRenderer(EntityRendererFactory.Context ctx, boolean chest) {
        super(ctx, chest ? ModEntityModelLayers.HEMP_CHEST_BOAT : ModEntityModelLayers.HEMP_BOAT);
        this.layer = RenderLayers.entitySolid(Identifier.of(Hempdustry.MOD_ID,
                chest ? "textures/entity/chest_boat/hemp.png" : "textures/entity/boat/hemp.png"));
    }

    @Override
    protected RenderLayer getRenderLayer() {
        return this.layer;
    }
}
