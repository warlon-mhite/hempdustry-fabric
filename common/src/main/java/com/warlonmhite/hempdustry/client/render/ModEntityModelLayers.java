package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

/**
 * The mod's entity model layers. Hemp gets its own pair rather than borrowing oak's, because since
 * 1.21.2 the boat renderer builds its model from the layer it is handed — one layer per boat.
 * The geometry is still vanilla's, which is the point: a hemp boat should be a boat.
 */
public final class ModEntityModelLayers {

    public static final ModelLayerLocation HEMP_BOAT =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "boat/hemp"), "main");
    public static final ModelLayerLocation HEMP_CHEST_BOAT =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "chest_boat/hemp"), "main");
    /** The flip-flops' soles and straps, drawn in place of boots (FlipFlopsModel). */
    public static final ModelLayerLocation FLIP_FLOPS =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "flip_flops"), "main");
    /** The same on a baby's feet (FlipFlopsModel#createBabyLayer). */
    public static final ModelLayerLocation FLIP_FLOPS_BABY =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "flip_flops"), "baby");

    private ModEntityModelLayers() {
    }
}
