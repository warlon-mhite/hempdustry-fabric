package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

/** The mod's entity model layers. */
public final class ModEntityModelLayers {

    /** The flip-flops' soles and straps, drawn in place of boots (FlipFlopsModel). */
    public static final ModelLayerLocation FLIP_FLOPS =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Hempdustry.MOD_ID, "flip_flops"), "main");

    private ModEntityModelLayers() {
    }
}
