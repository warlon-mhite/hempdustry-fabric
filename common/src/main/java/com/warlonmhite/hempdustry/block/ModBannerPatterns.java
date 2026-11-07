package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * The cannabis-leaf banner pattern. On 1.20.1 a banner pattern is a built-in registry entry, and a
 * banner saves each of its layers by the pattern's hash name; from 1.20.5 on a pattern is a datapack
 * file, and a layer is saved by the pattern's id. The hash name here is that id, so Minecraft's own
 * 1.20.5 upgrade, which keeps a hash name it does not know as the layer's pattern, lands on the
 * newer lines' {@code hempdustry:hemp_leaf}. Its art is vanilla's path for a pattern's file name,
 * {@code minecraft:textures/entity/banner/hempdustry_hemp_leaf.png} and
 * {@code minecraft:textures/entity/shield/hempdustry_hemp_leaf.png}; its name, per colour, is
 * {@code block.minecraft.banner.hempdustry.hemp_leaf.<colour>}.
 */
public final class ModBannerPatterns {
    public static final ResourceLocation HEMP_LEAF_ID = new ResourceLocation(Hempdustry.MOD_ID, "hemp_leaf");
    /**
     * The leaf. Its hash -- what a banner stores -- is the newer lines' id, {@code hempdustry:hemp_leaf},
     * so a 1.16.5 banner keeps its leaf in a world upgraded to them. It needs its item in the loom: the
     * leaf is the stencil ({@code ModItems.HEMP_LEAF}).
     */
    public static final BannerPattern HEMP_LEAF = Services.REGISTRATION.bannerPattern("HEMPDUSTRY_HEMP_LEAF",
            Hempdustry.MOD_ID + "_hemp_leaf", HEMP_LEAF_ID.toString(), true);

    private ModBannerPatterns() {
    }

    public static void registerBannerPatterns() {
        Hempdustry.LOGGER.info("Registering Banner Patterns for " + Hempdustry.MOD_ID);
    }
}
