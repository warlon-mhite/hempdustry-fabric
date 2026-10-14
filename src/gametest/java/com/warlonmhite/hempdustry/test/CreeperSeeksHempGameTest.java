package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.entity.ai.SeekHempCropGoal;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Creepers now and then wander into an open, grown hemp field — and never gather at one.
 *
 * <p>Everything random about the goal is stepped over, not waited for: {@code trigger}
 * skips the per-tick roll (and the despawn-counter rule, which a test world with no player would
 * otherwise trip), and {@link SeekHempCropGoal#findCover} is asked with a sample count that cannot
 * miss a 7×7 field inside a range-3 square. What is left is the rules, which is what can break.
 *
 * <p>Every rule here fails the same way when it breaks: a creeper that goes to a field it should not,
 * or one that never leaves. Neither is an error in a log, and both were the old goal's behaviour.
 */
public final class CreeperSeeksHempGameTest {

    private static final int FIELD = 7;
    private static final BlockPos CENTRE = new BlockPos(4, 2, 4);
    /** Samples enough to hit a 7×7 field in a 7×7 search square every time. */
    private static final int ALWAYS = 256;
    /** The longest lurk is 120 ticks. */
    private static final int LURK_OVER_BY_TICK = 140;

    public static void creeperSeeksHempCrop(GameTestHelper context) {
        floor(context);
        openSky(context);
        Creeper creeper = interested(context, CENTRE);

        context.assertTrue(goalOf(creeper) != null,
                "CreeperEntityMixin did not add SeekHempCropGoal to a creeper");

        context.assertTrue(SeekHempCropGoal.findCover(creeper, 3, ALWAYS) == null,
                "a creeper found cover in bare ground");

        field(context, 1);
        context.assertTrue(SeekHempCropGoal.findCover(creeper, 3, ALWAYS) == null,
                "a field of seedlings drew a creeper; nothing a creeper could hide in grows yet");

        field(context, IndicaCropBlock.MAX_AGE);
        BlockPos cover = SeekHempCropGoal.findCover(creeper, 3, ALWAYS);
        context.assertTrue(cover != null, "a grown field under open sky did not draw the creeper");
        context.assertTrue(context.getLevel().getBlockState(cover).is(ModBlocks.INDICA_CROP),
                "findCover returned " + cover + ", which is not the root of a hemp plant");

        roof(context, Blocks.GLASS);
        context.assertTrue(SeekHempCropGoal.findCover(creeper, 3, ALWAYS) == null,
                "a field under a glass roof drew a creeper; only open fields may");
        roof(context, Blocks.AIR);

        Creeper first = interested(context, CENTRE.offset(1, 0, 1));
        context.assertTrue(SeekHempCropGoal.findCover(creeper, 3, ALWAYS) == null,
                "a field another creeper is already in drew a second one");
        first.discard();


        // It notices the field sooner or later -- a glance is only 8 columns in a 49x49 square, so
        // this retries rather than expecting the first one to land. A miss has no side effects.
        SeekHempCropGoal goal = goalOf(creeper);
        boolean noticed = false;
        for (int glance = 0; glance < 2000 && !noticed; glance++) {
            goal.trigger();
            noticed = goal.canUse();
        }
        context.assertTrue(noticed, "a creeper standing in a grown field never noticed it in 2000 glances");
        goal.start();
        goal.stop();

        // Then it prowls at most MAX_PROWLS times and walks away on its own. A prowl that misses
        // the field ends the visit early, which is allowed; staying for ever is not.
        int prowls = 0;
        while (prowls < 20) {
            goal.trigger();
            if (!goal.canUse()) {
                break;
            }
            goal.start();
            goal.stop();
            prowls++;
        }
        context.assertTrue(prowls <= 6, "a creeper prowled a field " + prowls
                + " times in a row; it should stop after 3-6 -- 20 means it never leaves");
        for (int glance = 0; glance < 500; glance++) {
            goal.trigger();
            context.assertFalse(goal.canUse(),
                    "a creeper that has just left a field went straight back to it instead of resting");
        }

        // An indifferent creeper never notices, however many times it looks. Alone in the field:
        // with another creeper in it the "taken" rule refuses first and this proves nothing.
        creeper.discard();
        Creeper indifferent = context.spawn(EntityType.CREEPER, CENTRE.offset(-1, 0, -1));
        indifferent.setUUID(new UUID(0L, 1L));
        SeekHempCropGoal indifferentGoal = goalOf(indifferent);
        for (int glance = 0; glance < 2000; glance++) {
            indifferentGoal.trigger();
            context.assertFalse(indifferentGoal.canUse(),
                    "a creeper whose UUID says it ignores hemp noticed a field");
        }

        indifferent.discard();

        // A creeper that has arrived lurks -- the goal holds on with the navigation idle -- and
        // then lets go by itself. A lurk with no end is the old goal's parking, back again. The
        // AI is off so nothing else moves the navigation; the goal is driven by hand.
        Creeper lurker = interested(context, CENTRE);
        lurker.setNoAi(true);
        SeekHempCropGoal lurk = new SeekHempCropGoal(lurker);
        lurk.start();
        lurker.getNavigation().stop();
        context.assertTrue(lurk.canContinueToUse(), "a creeper that arrived did not lurk at all");
        context.runAtTickTime(LURK_OVER_BY_TICK, () -> {
            context.assertFalse(lurk.canContinueToUse(),
                    "a creeper was still lurking " + LURK_OVER_BY_TICK + " ticks after it arrived; a lurk is 2-6 s");
            context.succeed();
        });
    }

    /** A creeper whose UUID puts it in the half that notices hemp. */
    private static Creeper interested(GameTestHelper context, BlockPos at) {
        // spawn, NOT spawnWithNoFreeWill: GameTestHelper.spawnWithNoFreeWill strips every goal off the mob.
        Creeper creeper = context.spawn(EntityType.CREEPER, at);
        creeper.setUUID(new UUID(creeper.getUUID().getMostSignificantBits(), 2L));
        context.assertTrue(SeekHempCropGoal.caresAboutHemp(creeper), "test setup: UUID parity");
        return creeper;
    }

    private static SeekHempCropGoal goalOf(Mob mob) {
        try {
            var field = Mob.class.getDeclaredField("goalSelector");
            field.setAccessible(true);
            return ((GoalSelector) field.get(mob)).getAvailableGoals().stream()
                    .map(prioritized -> prioritized.getGoal())
                    .filter(SeekHempCropGoal.class::isInstance)
                    .map(SeekHempCropGoal.class::cast)
                    .findFirst().orElse(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void floor(GameTestHelper context) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                context.setBlock(new BlockPos(x, 1, z), Blocks.FARMLAND);
            }
        }
    }

    /**
     * The test runner roofs every test box with barriers, so no field in a game test has open sky
     * until the roof over this one is lifted. The goal refusing roofed fields is the point of the
     * glass check below, so the barrier has to go rather than the check.
     */
    private static void openSky(GameTestHelper context) {
        var world = context.getLevel();
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                BlockPos column = context.absolutePos(new BlockPos(x, 0, z));
                int top = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
                for (int y = column.getY() + 3; y < top; y++) {
                    world.setBlockAndUpdate(new BlockPos(column.getX(), y, column.getZ()), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /** A 7×7 field around the creeper, at {@code age}. Seedlings are one block tall. */
    private static void field(GameTestHelper context, int age) {
        for (int x = 1; x <= FIELD; x++) {
            for (int z = 1; z <= FIELD; z++) {
                BlockPos root = new BlockPos(x, 2, z);
                context.setBlock(root.above(), Blocks.AIR);
                context.setBlock(root, ModBlocks.INDICA_CROP.defaultBlockState()
                        .setValue(IndicaCropBlock.AGE, age).setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER));
                if (age >= 4) {
                    context.setBlock(root.above(), ModBlocks.INDICA_CROP.defaultBlockState()
                            .setValue(IndicaCropBlock.AGE, age).setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER));
                }
            }
        }
    }

    private static void roof(GameTestHelper context, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                context.setBlock(new BlockPos(x, 6, z), block);
            }
        }
    }
}
