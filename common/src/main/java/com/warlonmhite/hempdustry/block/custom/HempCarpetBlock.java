package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.mixin.LlamaAccessor;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.WoolCarpetBlock;

/**
 * The hemp carpet. Vanilla's carpets are {@code WoolCarpetBlock}s, and a llama takes any carpet in
 * {@code #minecraft:carpets} but shows its colour from the block's {@code DyeColor}. On 1.16.5 a
 * carpet must be a {@code WoolCarpetBlock} to be worn at all, so ours is one, nominally white; a
 * llama's carpet stays in the server's inventory and the client only sees that colour, so a llama in
 * ours syncs {@link #SWAG_ID} instead ({@code LlamaMixin}), and
 * {@code LlamaDecorFeatureRendererMixin} draws it. A dispenser gives ours the behaviour vanilla gives
 * its carpets by name ({@code ModDispenserBehaviors}).
 */
public class HempCarpetBlock extends WoolCarpetBlock {
    /** The swag value a llama in the hemp carpet syncs: past the sixteen dyes, and not vanilla's -1 for none. */
    public static final int SWAG_ID = 16;

    public HempCarpetBlock(Properties settings) {
        super(DyeColor.WHITE, settings);
    }

    /** Whether this llama wears the hemp carpet, as its client knows it. */
    public static boolean isWornBy(Llama llama) {
        return llama.getEntityData().get(LlamaAccessor.hempdustry$swagId()) == SWAG_ID;
    }
}
