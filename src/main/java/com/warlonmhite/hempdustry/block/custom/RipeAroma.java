package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.particle.ModParticles;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * The smell of a ripe plant, shared by every hemp crop: now and then a wisp drifts up off the top
 * of a plant that is ready to harvest, so a field that is finished can be told from one that is
 * still growing without walking up to it. Cannabis smells strongest late in flowering, which is
 * exactly the stage this marks.
 *
 * <p><b>A second cue, never the only one.</b> The ripe-stage art says the same thing and must go on
 * saying it alone: a player on Particles: Minimal never sees a wisp, and one on Decreased sees two
 * in three. Vanilla's full beehive is the model — it drips honey, and its model changes too.
 *
 * <p><b>The server never runs a line of this.</b> It is reached only from a crop's
 * {@code randomDisplayTick}, which only {@code ClientWorld} calls. Nothing is stored, nothing
 * random-ticks, and no packet is sent.
 *
 * <p><b>Its cost does not grow with the field.</b> A client samples a fixed 667 blocks within 16 of
 * the player and 667 within 32 each tick ({@code ClientWorld#doRandomBlockDisplayTicks}), and a
 * plant only gets a say when it is one of them. However big the farm, no more than 1334 blocks are
 * asked, so no more than 1334 / {@link #CHANCE} wisps can start in a tick — and a real field, whose
 * plant tops fill one layer in every few, starts far fewer. Each wisp skips the world collision
 * query that vanilla particles run every tick; see {@code AromaParticle}.
 */
public final class RipeAroma {

    /** One sampling of a ripe plant's top in this many lets off a wisp. */
    private static final int CHANCE = 15;

    private RipeAroma() {
    }

    /**
     * Maybe lets off one wisp from the block at {@code pos}, which the caller has found belongs to
     * a ripe plant. Only the block with open air above it smells — the top of the plant — so the
     * wisp never starts inside a block, and a plant with something on its head gives off nothing.
     */
    public static void randomDisplayTick(World world, BlockPos pos, Random random) {
        // The roll first: it is the cheapest test and fails fourteen times in fifteen, so the block
        // lookup above almost never happens.
        if (random.nextInt(CHANCE) != 0
                || !HempdustryConfig.get().client().ripeAroma()
                || !world.getBlockState(pos.up()).isAir()) {
            return;
        }
        world.addParticle(ModParticles.AROMA,
                pos.getX() + 0.25 + random.nextDouble() * 0.5,
                pos.getY() + 0.55 + random.nextDouble() * 0.3,
                pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                0.0, 0.0, 0.0);
    }
}
