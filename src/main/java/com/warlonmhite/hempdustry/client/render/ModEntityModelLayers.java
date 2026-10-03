package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/** The mod's entity model layers. */
public final class ModEntityModelLayers {

    /** The flip-flops' soles and straps, drawn in place of boots (FlipFlopsRenderer). */
    public static final EntityModelLayer FLIP_FLOPS =
            new EntityModelLayer(Identifier.of(Hempdustry.MOD_ID, "flip_flops"), "main");

    private ModEntityModelLayers() {
    }
}
