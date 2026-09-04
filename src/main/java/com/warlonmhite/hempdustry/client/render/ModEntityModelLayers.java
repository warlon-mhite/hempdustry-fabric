package com.warlonmhite.hempdustry.client.render;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

/**
 * The mod's entity model layers. Hemp gets its own pair rather than borrowing oak's, because since
 * 1.21.2 the boat renderer builds its model from the layer it is handed — one layer per boat.
 * The geometry is still vanilla's, which is the point: a hemp boat should be a boat.
 */
public final class ModEntityModelLayers {

    public static final EntityModelLayer HEMP_BOAT =
            new EntityModelLayer(Identifier.of(Hempdustry.MOD_ID, "boat/hemp"), "main");
    public static final EntityModelLayer HEMP_CHEST_BOAT =
            new EntityModelLayer(Identifier.of(Hempdustry.MOD_ID, "chest_boat/hemp"), "main");

    private ModEntityModelLayers() {
    }
}
