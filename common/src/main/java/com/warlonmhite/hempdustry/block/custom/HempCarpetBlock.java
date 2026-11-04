package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.mixin.LlamaAccessor;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.level.block.CarpetBlock;

/**
 * The hemp carpet. Vanilla's carpets are {@code WoolCarpetBlock}s, and a llama takes any carpet in
 * {@code #minecraft:wool_carpets} but shows its colour from the block's {@code DyeColor}, which ours
 * does not have. On 1.20.1 a llama's carpet stays in the server's inventory and the client only sees
 * that colour, so a llama in ours syncs {@link #SWAG_ID} instead ({@code LlamaMixin}), and
 * {@code LlamaDecorFeatureRendererMixin} draws it. A dispenser gives ours the behaviour vanilla gives
 * its carpets by name ({@code ModDispenserBehaviors}).
 */
public class HempCarpetBlock extends CarpetBlock {
    /** The swag value a llama in the hemp carpet syncs: past the sixteen dyes, and not vanilla's -1 for none. */
    public static final int SWAG_ID = 16;

    public HempCarpetBlock(Properties settings) {
        super(settings);
    }

    /** Whether this llama wears the hemp carpet, as its client knows it. */
    public static boolean isWornBy(Llama llama) {
        return llama.getEntityData().get(LlamaAccessor.hempdustry$swagId()) == SWAG_ID;
    }
}
