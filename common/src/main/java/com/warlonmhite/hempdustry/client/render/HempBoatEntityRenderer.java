package com.warlonmhite.hempdustry.client.render;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * Vanilla's boat renderer, on the hemp boats' model layers.
 *
 * <p>It used to be a full reimplementation, because 1.21.1's {@code BoatRenderer} looked its
 * texture up in a map keyed by the closed {@code Boat.Type} enum, which a mod cannot join. From
 * 1.21.2 the renderer took a {@link ModelLayerLocation} and an overridable render type, which is all
 * this class overrode; since 26.2 it reads the texture off the layer's id as well
 * ({@code textures/entity/<layer path>.png}), and the layers' ids — {@code boat/hemp},
 * {@code chest_boat/hemp} — already name the textures. So only the choice of layer is left here.
 */
public class HempBoatEntityRenderer extends BoatRenderer {
    public HempBoatEntityRenderer(EntityRendererProvider.Context ctx, boolean chest) {
        super(ctx, chest ? ModEntityModelLayers.HEMP_CHEST_BOAT : ModEntityModelLayers.HEMP_BOAT);
    }
}
