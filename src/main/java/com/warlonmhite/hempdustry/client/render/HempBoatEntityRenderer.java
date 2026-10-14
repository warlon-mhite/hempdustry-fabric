package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Vanilla's boat renderer with one fixed texture.
 *
 * <p>It used to be a full reimplementation, because 1.21.1's {@code BoatRenderer} looked its
 * texture up in a map keyed by the closed {@code Boat.Type} enum, which a mod cannot join.
 * Since 1.21.2 the renderer takes an {@link ModelLayerLocation} and exposes a single overridable
 * {@code getRenderLayer()}, so all that is left here is the texture — the geometry, the damage
 * wobble, the paddles and the water mask are vanilla's again.
 */
public class HempBoatEntityRenderer extends BoatRenderer {
    private final RenderType layer;

    public HempBoatEntityRenderer(EntityRendererProvider.Context ctx, boolean chest) {
        super(ctx, chest ? ModEntityModelLayers.HEMP_CHEST_BOAT : ModEntityModelLayers.HEMP_BOAT);
        this.layer = RenderTypes.entitySolid(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID,
                chest ? "textures/entity/chest_boat/hemp.png" : "textures/entity/boat/hemp.png"));
    }

    @Override
    protected RenderType renderType() {
        return this.layer;
    }
}
