package com.warlonmhite.hempdustry.block;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BannerPattern;

/**
 * The cannabis-leaf banner pattern. On 1.20.1 a banner pattern is a built-in registry entry, and a
 * banner saves each of its layers by the pattern's hash name; from 1.20.5 on a pattern is a datapack
 * file, and a layer is saved by the pattern's id. The hash name here is that id, so Minecraft's own
 * 1.20.5 upgrade, which keeps a hash name it does not know as the layer's pattern, lands on the
 * newer lines' {@code hempdustry:hemp_leaf}. Its art is {@code textures/entity/banner/hemp_leaf.png}
 * and {@code textures/entity/shield/hemp_leaf.png}; its name, per colour, is
 * {@code block.minecraft.banner.hempdustry.hemp_leaf.<colour>}.
 */
public final class ModBannerPatterns {
    public static final ResourceLocation HEMP_LEAF_ID = new ResourceLocation(Hempdustry.MOD_ID, "hemp_leaf");
    public static final BannerPattern HEMP_LEAF = Registry.register(BuiltInRegistries.BANNER_PATTERN, HEMP_LEAF_ID,
            new BannerPattern(HEMP_LEAF_ID.toString()));

    private ModBannerPatterns() {
    }

    public static void registerBannerPatterns() {
        Hempdustry.LOGGER.info("Registering Banner Patterns for " + Hempdustry.MOD_ID);
    }
}
