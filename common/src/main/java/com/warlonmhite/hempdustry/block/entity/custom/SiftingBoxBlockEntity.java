package com.warlonmhite.hempdustry.block.entity.custom;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.Chemotype;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * What a Sifting Box's batch is made of: how many of its levels each chemotype added, and the sum of
 * their tiers. A record and not an inventory: the box still keeps no item, has no screen and gives
 * nothing back but its yield. The fill level, the water and the kind of screen stay the block's 2.1
 * states.
 *
 * <p>Counts and a sum rather than more block states, because a block state cannot hold an exact
 * count: a share rounded at every piece drifts with the order the pieces came in, and a sum never
 * does. And a level this record never saw reads as what every 2.1 batch was, THC-dominant and Dank,
 * so a box a 2.1 world left half full (which has no record at all) sifts what it would have sifted
 * there, and so does a level a command set.
 */
public class SiftingBoxBlockEntity extends BlockEntity {
    private static final String LEVELS = "levels";
    private static final String TIER_SUM = "tier_sum";
    private static final Codec<Map<Chemotype, Integer>> LEVEL_COUNTS =
            Codec.unboundedMap(Chemotype.CODEC, ExtraCodecs.POSITIVE_INT);

    // How many levels each chemotype added, indexed by ordinal.
    private final int[] levels = new int[Chemotype.values().length];
    // The tiers (BudTier#rank) of those levels, summed: one per level a piece added.
    private int tierSum;

    public SiftingBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIFTING_BOX, pos, state);
    }

    /** The batch of the box at {@code pos}, or null where there is none to read (a client, a box being replaced). */
    public static @Nullable SiftingBoxBlockEntity of(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SiftingBoxBlockEntity box ? box : null;
    }

    /** {@code added} levels of {@code chemotype} at {@code tier} go into the batch. */
    public void add(Chemotype chemotype, BudTier tier, int added) {
        if (added <= 0) {
            return;
        }
        levels[chemotype.ordinal()] += added;
        tierSum += tier.rank() * added;
        setChanged();
    }

    /** An empty screen: the batch is forgotten. */
    public void clear() {
        if (seen() > 0 || tierSum != 0) {
            Arrays.fill(levels, 0);
            tierSum = 0;
            setChanged();
        }
    }

    private int seen() {
        int seen = 0;
        for (int count : levels) {
            seen += count;
        }
        return seen;
    }

    /** Levels of a screen at {@code boxLevel} this record never saw: they read as 2.1's THC-dominant, Dank. */
    private int unseen(int boxLevel) {
        return Math.max(0, Math.min(boxLevel, SiftingBoxBlock.FULL_LEVEL) - seen());
    }

    /** The batch's chemotype on a screen at {@code boxLevel}: the dosage of the levels each chemotype added. */
    public Chemotype chemotype(int boxLevel) {
        int[] counts = levels.clone();
        counts[Chemotype.THC_DOMINANT.ordinal()] += unseen(boxLevel);
        return Chemotype.dosage(counts);
    }

    /** The batch's tier on a screen at {@code boxLevel}: its levels' average, to the nearest tier, a half down. */
    public BudTier tier(int boxLevel) {
        int unseen = unseen(boxLevel);
        return SiftingBoxBlock.averageTier(tierSum + unseen * BudTier.DANK.rank(), seen() + unseen);
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        Arrays.fill(levels, 0);
        for (Map.Entry<Chemotype, Integer> entry : view.read(LEVELS, LEVEL_COUNTS).orElse(Map.of()).entrySet()) {
            levels[entry.getKey().ordinal()] = entry.getValue();
        }
        tierSum = Math.max(0, view.getIntOr(TIER_SUM, 0));
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        Map<Chemotype, Integer> counts = new EnumMap<>(Chemotype.class);
        for (Chemotype chemotype : Chemotype.values()) {
            if (levels[chemotype.ordinal()] > 0) {
                counts.put(chemotype, levels[chemotype.ordinal()]);
            }
        }
        if (!counts.isEmpty()) {
            view.store(LEVELS, LEVEL_COUNTS, counts);
            view.putInt(TIER_SUM, tierSum);
        }
    }
}
