package com.warlonmhite.hempdustry.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;

/**
 * The mod's paintings. On 1.20.1 a painting is a built-in registry entry sized in pixels, sixteen to a
 * block; from 1.21 on it is a datapack file sized in blocks, under the same id. The art is
 * {@code textures/painting/<id>.png}; the title and author are {@code painting.hempdustry.<id>.title}
 * and {@code .author}; {@code #minecraft:placeable} puts them on a random wall.
 */
public final class ModPaintings {
    public static final PaintingVariant B_RAPPAZ = register("b_rappaz", 4, 4);
    public static final PaintingVariant BROWNIE_MARY = register("brownie_mary", 4, 4);
    public static final PaintingVariant HENDRIX = register("hendrix", 2, 2);
    public static final PaintingVariant MARLEY = register("marley", 2, 2);
    public static final PaintingVariant PINEAPPLE_EXPRESS = register("pineapple_express", 2, 3);
    public static final PaintingVariant RIHANNA = register("rihanna", 1, 2);
    public static final PaintingVariant SNOOP = register("snoop", 3, 3);

    private ModPaintings() {
    }

    private static PaintingVariant register(String id, int widthInBlocks, int heightInBlocks) {
        return Registry.register(BuiltInRegistries.PAINTING_VARIANT, new ResourceLocation(Hempdustry.MOD_ID, id),
                new PaintingVariant(widthInBlocks * 16, heightInBlocks * 16));
    }

    public static void registerPaintings() {
        Hempdustry.LOGGER.info("Registering Paintings for " + Hempdustry.MOD_ID);
    }
}
