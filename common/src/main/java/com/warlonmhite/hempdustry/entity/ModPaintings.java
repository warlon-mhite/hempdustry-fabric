package com.warlonmhite.hempdustry.entity;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.Motive;

/**
 * The mod's paintings. On 1.18.2 a painting is a built-in registry entry ({@code Motive}) sized in
 * pixels, sixteen to a block; from 1.21 on it is a datapack file sized in blocks, under the same id.
 * The art is
 * {@code textures/painting/<id>.png}; the title and author are {@code painting.hempdustry.<id>.title}
 * and {@code .author} on the newer lines, where a painting item shows them -- a 1.18.2 painting shows
 * none. 1.18.2 hangs any registered painting on a random wall; there is no {@code #minecraft:placeable}.
 */
public final class ModPaintings {
    public static final Motive B_RAPPAZ = register("b_rappaz", 4, 4);
    public static final Motive BROWNIE_MARY = register("brownie_mary", 4, 4);
    public static final Motive HENDRIX = register("hendrix", 2, 2);
    public static final Motive MARLEY = register("marley", 2, 2);
    public static final Motive PINEAPPLE_EXPRESS = register("pineapple_express", 2, 3);
    public static final Motive RIHANNA = register("rihanna", 1, 2);
    public static final Motive SNOOP = register("snoop", 3, 3);

    private ModPaintings() {
    }

    private static Motive register(String id, int widthInBlocks, int heightInBlocks) {
        return Registry.register(Registry.MOTIVE, new ResourceLocation(Hempdustry.MOD_ID, id),
                new Motive(widthInBlocks * 16, heightInBlocks * 16));
    }

    public static void registerPaintings() {
        Hempdustry.LOGGER.info("Registering Paintings for " + Hempdustry.MOD_ID);
    }
}
