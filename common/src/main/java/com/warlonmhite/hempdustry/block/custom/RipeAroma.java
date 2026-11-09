package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.config.HempdustryConfig;
import com.warlonmhite.hempdustry.particle.ModParticles;
import com.warlonmhite.hempdustry.strain.Terpene;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

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
 * {@code animateTick}, which only {@code ClientLevel} calls. Nothing is stored, nothing
 * random-ticks, and no packet is sent.
 *
 * <p><b>Its cost does not grow with the field.</b> A client samples a fixed 667 blocks within 16 of
 * the player and 667 within 32 each tick ({@code ClientLevel#animateTick}), and a
 * plant only gets a say when it is one of them. However big the farm, no more than 1334 blocks are
 * asked, so no more than 1334 / {@link #CHANCE} wisps can start in a tick — and a real field, whose
 * plant tops fill one layer in every few, starts far fewer. Each wisp skips the world collision
 * query that vanilla particles run every tick; see {@code AromaParticle}.
 */
public final class RipeAroma {

    /** One sampling of a ripe plant's top in this many lets off a wisp. */
    public static final int CHANCE = 15;
    /**
     * The fibre crop's: half as often. Fibre hemp flowers and smells like any hemp, but it is sown for
     * stalk, with fewer and smaller flowers, so its field is fainter.
     */
    public static final int FAINT_CHANCE = 2 * CHANCE;

    private RipeAroma() {
    }

    /**
     * Maybe lets off one wisp from the block at {@code pos}, which the caller has found belongs to
     * a ripe plant. Only the block with open air above it smells — the top of the plant — so the
     * wisp never starts inside a block, and a plant with something on its head gives off nothing.
     */
    public static void randomDisplayTick(Level world, BlockPos pos, RandomSource random) {
        randomDisplayTick(world, pos, random, Direction.UP);
    }

    /**
     * As above, for a plant that grows {@code growth}-wards. One hanging from a ceiling has its open
     * end below, so its wisp starts low in the tip and rises past the plant.
     */
    public static void randomDisplayTick(Level world, BlockPos pos, RandomSource random, Direction growth) {
        randomDisplayTick(world, pos, random, growth, CHANCE);
    }

    /** As above, one sampling in {@code chance} letting off a wisp. */
    public static void randomDisplayTick(Level world, BlockPos pos, RandomSource random, Direction growth,
                                         int chance) {
        // The roll first: it is the cheapest test and fails fourteen times in fifteen, so the block
        // lookup above almost never happens.
        if (random.nextInt(chance) != 0
                || !HempdustryConfig.get().client().ripeAroma()
                || !world.getBlockState(pos.relative(growth)).isAir()) {
            return;
        }
        double low = growth == Direction.DOWN ? 0.05 : 0.55;
        world.addParticle(wisp(world, world.getBlockState(pos).getBlock()),
                pos.getX() + 0.25 + random.nextDouble() * 0.5,
                pos.getY() + low + random.nextDouble() * 0.3,
                pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                0.0, 0.0, 0.0);
    }

    /**
     * The wisp a plant of {@code block} lets off: tinted by its terpene ({@link Terpene#ofPlant}), mixed
     * toward white by the smoke's share so it stays a faint cast, or the plain one for a plant with none.
     */
    public static ParticleOptions wisp(Level world, Block block) {
        return Terpene.ofPlant(block, world.registryAccess())
                .<ParticleOptions>map(terpene -> ColorParticleOption.create(ModParticles.TERPENE_AROMA,
                        ARGB.opaque(Terpene.mix(0xFFFFFF, terpene.color(), Terpene.SMOKE_SHARE))))
                .orElse(ModParticles.AROMA);
    }
}
