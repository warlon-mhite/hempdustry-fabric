package com.warlonmhite.hempdustry.block.custom;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.item.Item;

/**
 * The blonde bar: nine filtered hashish, crafted together in the grid.
 *
 * <p><b>Identical to {@link HashishBarBlock} in every respect but what it cuts into.</b> Same 8×4×14
 * slab shrinking 14 → 11 → 8 → 5 → 2 across five cuts, same 2+2+2+2+1 = nine pieces, same 9↔9
 * reverse craft, same blade tag, same honey sound. That symmetry is the point: <b>every pressed hash
 * gets a bar</b>, so both of the Hemp Press's hash outputs are bars, and a player who has learned one
 * has learned the other.
 *
 * <p>What differs is the price, and it is paid at the screen. A plant screen fills at
 * {@code FLOWER_CHANCE = 1.0} — seven buds, a number you can count — while a kief screen fills at
 * {@code KIEF_CHANCE}, which is deliberately low: <b>about three kief make one filtered kief</b>, and
 * the wet road needs an ice jacket instead. That is what a further sieve pass costs, and it is the
 * whole reason filtering is a sidegrade rather than an upgrade. See {@code SiftingBoxBlock}.
 */
public class FilteredHashishBarBlock extends HashishBarBlock {

    public static final MapCodec<FilteredHashishBarBlock> CODEC = createCodec(FilteredHashishBarBlock::new);

    public FilteredHashishBarBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends HashishBarBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected Item piece() {
        return ModItems.FILTERED_HASHISH;
    }
}
