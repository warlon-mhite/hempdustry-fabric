package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.entity.custom.PlantRecordBlockEntity;
import com.warlonmhite.hempdustry.strain.ResistanceGenes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;

/**
 * Pins a test plant's resistances. Every plant rolls its own at planting, so a test whose verdict rests on a
 * stress, a lost light or mildew — and drives its roll with a decided random — sets the plant's genes first,
 * or it would pass or fail with the plant's roll.
 */
final class Genes {
    private Genes() {
    }

    /** The plant whose root is at {@code root} (relative to the test) is neutral on all four phenomena. */
    static void neutral(GameTestHelper context, BlockPos root) {
        set(context, root, ResistanceGenes.NEUTRAL);
    }

    /** The plant whose root is at {@code root} (relative to the test) has {@code genes}. */
    static void set(GameTestHelper context, BlockPos root, ResistanceGenes genes) {
        if (!(context.getLevel().getBlockEntity(context.absolutePos(root)) instanceof PlantRecordBlockEntity record)) {
            throw context.assertionException(Component.literal("no plant record at " + root + " to set the genes of"));
        }
        record.setResistances(genes);
    }
}
