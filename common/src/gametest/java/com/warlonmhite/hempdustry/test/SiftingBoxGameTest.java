package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Dry Sifter's whole cycle, and the two ways it could quietly pay out wrong.
 *
 * <p>Both failures this guards are invisible in play. <b>A screen that does not charge for what it
 * takes</b> looks exactly like a screen that does, because the plant matter is going in either way
 * and nobody counts their trim. And <b>a full screen that can be collected twice</b> is a
 * duplication bug that presents as good luck: the second right-click would hand over a second lump
 * and the level would already be back at zero, so there is nothing on screen to notice.
 *
 * <p>Buds rather than leaf on purpose: {@code FLOWER_CHANCE} is 1.0, so the fill is exactly
 * {@code FULL_LEVEL} uses and the test has no randomness in it at all. The trim rate is a balance
 * number, not a mechanism, and a test that rolled dice for it would be measuring the RNG.
 */
public final class SiftingBoxGameTest {

    private static final BlockPos BOX = new BlockPos(1, 1, 1);
    /** Comfortably past {@code PRESS_DELAY}, which is 20. */
    private static final int READY_BY_TICK = 40;

    /**
     * A real right-click on the screen, through {@code ServerPlayerGameMode}.
     *
     * <p><b>This has to be the interaction manager and nothing else.</b> The bug this test exists
     * for — the block refusing a click with {@code InteractionResult.PASS}, which swallows it so
     * {@code useWithoutItem} never runs and a full screen can never be emptied — is invisible to both of the
     * convenient ways of faking a click. Calling {@code useWithoutItem} directly never asks the question, and
     * {@code GameTestHelper#useBlock} falls through to {@code useWithoutItem} on <em>any</em> non-accepted
     * result where the real manager insists on {@code PASS_TO_DEFAULT_BLOCK_INTERACTION} specifically. Both
     * were perfectly happy with the broken block while a player could not get the bar out.
     */
    private static InteractionResult rightClick(GameTestHelper context, ItemStack held) {
        return Players.click(context, Players.survival(context), BOX, held);
    }

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void dryScreenFillsAndYieldsKief(GameTestHelper context) {
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState());

        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(BOX);
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        // A pickaxe, not a block: right-clicking with a BlockItem legitimately *places* it, which
        // is vanilla behaviour and would be measuring the wrong thing.
        ItemStack notPlantMatter = new ItemStack(Items.IRON_PICKAXE);
        rightClick(context, notPlantMatter);
        context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), 0,
                "the screen took something that is not plant matter");

        ItemStack buds = new ItemStack(ModItems.INDICA_BUDS, 64);
        for (int i = 0; i < SiftingBoxBlock.FULL_LEVEL; i++) {
            world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        }
        context.assertValueEqual(SiftingBoxBlock.FULL_LEVEL,
                context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                "buds fill at chance 1.0, so " + SiftingBoxBlock.FULL_LEVEL + " of them should fill the screen");
        context.assertValueEqual(64 - SiftingBoxBlock.FULL_LEVEL, buds.getCount(),
                "the screen did not charge for exactly what it took");

        // A full screen takes nothing more: the powder is waiting to be pressed, not to be topped up.
        world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertValueEqual(64 - SiftingBoxBlock.FULL_LEVEL, buds.getCount(),
                "a full screen went on eating buds");

        context.runAtTickTime(READY_BY_TICK, () -> {
            context.assertValueEqual(SiftingBoxBlock.READY_LEVEL,
                    context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                    "the scheduled tick never pressed the powder into a lump");

            // The reported bug, driven exactly as a player drives it: a real right-click, holding
            // something the screen will not take. Empty-handed works too, but this is the case that
            // was broken — and the case a player is in, since they arrive holding trim.
            rightClick(context, new ItemStack(Items.IRON_PICKAXE));
            context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), 0,
                    "collecting the lump did not empty the screen");

            // And it is empty for real: a second collection must produce nothing.
            rightClick(context, ItemStack.EMPTY);

            context.runAfterDelay(2, () -> {
                // Nine loose pieces of KIEF, and NOT a bar. This block does not press; that is the
                // Hemp Press's job and the whole reason the two are separate blocks.
                context.assertItemEntityCountIs(ModItems.KIEF, BOX, 3.0, SiftingBoxBlock.YIELD);
                context.assertItemEntityNotPresent(ModBlocks.HASHISH_BAR.asItem(), BOX, 3.0);
                context.assertItemEntityNotPresent(ModItems.BUBBLE_HASH, BOX, 3.0);
                context.succeed();
            });
        });
    }

    /**
     * The water changes the product, and the jacket is what makes the wash run at all.
     *
     * <p><b>Both halves fail silently.</b> A jacket check that is missing or inverted turns the box
     * into a free upgrade — no error, no message, nothing on screen that looks wrong. And a jacket
     * that melts <em>while the player is using it</em>, because a torch went up, is the failure the
     * design actually invites: vanilla melts the ice, the box quietly refuses, and the only tell is
     * that the frost stopped.
     *
     * <p>Blue ice throughout, so the rate is 1.0 and every roll is a certainty. The tiered rates are
     * balance numbers; a test that rolled them would be measuring the RNG rather than the mechanism.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void iceWashNeedsItsJacket(GameTestHelper context) {
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState());

        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(BOX);
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        ItemStack buds = new ItemStack(ModItems.INDICA_BUDS, 64);

        // A real right-click with a bucket. The box is empty, so it takes the water.
        rightClick(context, new ItemStack(Items.WATER_BUCKET));
        context.assertTrue(context.getBlockState(BOX).getValue(SiftingBoxBlock.FILLED),
                "a water bucket did not fill the box");

        // Filled but not jacketed: the wash cannot run, and the refusal must PASS THE CLICK ON
        // rather than swallow it -- see sifterRefusesAnUnjacketedWash's sibling case below.
        ItemInteractionResult bare = world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertTrue(bare == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                "an unjacketed wash swallowed the click instead of passing it on");
        context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), 0,
                "a filled box with no ice around it washed a bud");
        context.assertValueEqual(64, buds.getCount(), "an unjacketed box charged for a bud it refused");

        // Jacket it in blue ice and the same box runs at rate 1.0.
        for (Direction side : Direction.Plane.HORIZONTAL) {
            context.setBlock(BOX.relative(side), Blocks.BLUE_ICE.defaultBlockState());
        }
        for (int i = 0; i < SiftingBoxBlock.FULL_LEVEL - 1; i++) {
            world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        }
        context.assertValueEqual(SiftingBoxBlock.FULL_LEVEL - 1,
                context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                "a blue-ice jacket is rate 1.0, so every bud should have advanced the box");

        // ...and now the failure the design invites: the jacket melts mid-batch. One side to air is
        // what a torch leaves behind after vanilla's IceBlock melts.
        context.setBlock(BOX.north(), Blocks.AIR.defaultBlockState());
        ItemInteractionResult melted = world.getBlockState(pos)
                .useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertTrue(melted == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                "a box whose jacket melted swallowed the click");
        context.assertValueEqual(SiftingBoxBlock.FULL_LEVEL - 1,
                context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                "the wash went on running with a hole in its jacket");

        context.setBlock(BOX.north(), Blocks.BLUE_ICE.defaultBlockState());
        world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertValueEqual(SiftingBoxBlock.FULL_LEVEL,
                context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                "the box did not resume once its jacket was repaired");

        context.runAtTickTime(READY_BY_TICK, () -> {
            rightClick(context, ItemStack.EMPTY);
            // The water is spent WITH the batch, not before it: a finished wash has to still
            // remember which process made it, and FILLED is how it does.
            context.assertTrue(!context.getBlockState(BOX).getValue(SiftingBoxBlock.FILLED),
                    "collecting the batch did not spend the box's water");

            context.runAfterDelay(2, () -> {
                // The washed powder, and never the dry one. Same box, same inputs, different product
                // -- which is the entire reason the two blocks became one.
                context.assertItemEntityCountIs(ModItems.BUBBLE_HASH, BOX, 3.0, SiftingBoxBlock.YIELD);
                context.assertItemEntityNotPresent(ModItems.KIEF, BOX, 3.0);
                context.succeed();
            });
        });
    }

    /**
     * The re-sift: <b>plant matter and powder do not share a screen, and a wash takes neither of
     * them but plant.</b>
     *
     * <p>Three things break silently here. If the {@code CONTENT} check is missing or inverted, a
     * player tops up a half-full plant screen with kief and gets a batch out of a load that was part
     * powder — <b>no error, and the wrong item</b>. If the wash accepted kief there would be a third
     * road to a product that already has two, and one of the three would inevitably be a trap. And
     * if a refusal returns {@code InteractionResult.PASS} rather than {@code super}, it <em>swallows</em>
     * the click — the box stops accepting anything and can never be emptied, which is precisely the
     * bug that shipped in this block once already and was found by a player.
     *
     * <p>So this asserts the returned {@code InteractionResult} as well as the level: a block that
     * silently ignores a click and one that correctly passes it on are indistinguishable from the
     * world state alone, and only one of them still lets {@code useWithoutItem} run.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void screenRefusesAMixedLoad(GameTestHelper context) {
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState());

        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(BOX);
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        // Two buds in: the screen is now a PLANT screen, and stamped as one.
        ItemStack buds = new ItemStack(ModItems.INDICA_BUDS, 64);
        world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertValueEqual(2, context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                "two buds did not fill two levels");
        context.assertValueEqual(SiftingBoxBlock.Content.PLANT,
                context.getBlockState(BOX).getValue(SiftingBoxBlock.CONTENT), "the first bud did not stamp the screen as PLANT");

        ItemStack kief = new ItemStack(ModItems.KIEF, 64);
        ItemInteractionResult refused = world.getBlockState(pos)
                .useItemOn(kief, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertTrue(refused == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                "a refused load must pass the click on, not swallow it with PASS");
        context.assertValueEqual(2, context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                "kief went onto a plant screen");
        context.assertValueEqual(64, kief.getCount(), "the screen charged for a load it refused");

        // ...and the same screen still takes plant matter, so the refusal cost nothing.
        world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertValueEqual(3, context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                "the refusal left the screen unable to take its own content kind");

        // A fresh box takes kief happily, and stamps itself KIEF.
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState());
        for (int i = 0; i < 40 && context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL) == 0; i++) {
            world.getBlockState(pos).useItemOn(kief, world, player, InteractionHand.MAIN_HAND, hit);
        }
        context.assertValueEqual(SiftingBoxBlock.Content.KIEF,
                context.getBlockState(BOX).getValue(SiftingBoxBlock.CONTENT),
                "kief did not stamp an empty screen as KIEF — the re-sift is unreachable");

        // A WASH TAKES PLANT MATTER AND NOTHING ELSE. Kief into a filled box must be refused, or
        // there is a third road to filtered hashish and one of the three becomes a trap.
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState()
                .setValue(SiftingBoxBlock.FILLED, true));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            context.setBlock(BOX.relative(side), Blocks.BLUE_ICE.defaultBlockState());
        }
        ItemStack washKief = new ItemStack(ModItems.KIEF, 64);
        ItemInteractionResult wet = world.getBlockState(pos)
                .useItemOn(washKief, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertTrue(wet == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION,
                "a wash refusing kief swallowed the click");
        context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), 0,
                "the ice wash accepted kief — that is a third road to filtered hashish");
        context.assertValueEqual(64, washKief.getCount(), "the wash charged for kief it refused");

        // The control: the same jacketed, filled box still takes buds, so the refusal above is
        // about the kief and not about the box being broken.
        world.getBlockState(pos).useItemOn(buds, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertValueEqual(1, context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL),
                "the wash refused plant matter too, so the refusal above proves nothing");

        // ---- and the re-sift actually pays out FILTERED kief ----
        //
        // THIS ASSERTION IS THE POINT OF THE WHOLE RE-SIFT, and it was missing on the first pass:
        // three mutations were run against this test and the one that made the re-sift yield plain
        // kief PASSED, because nothing here had ever collected a KIEF batch. Everything above tests
        // what the screen REFUSES; without this, what it produces was covered by nothing.
        //
        // Set to READY_LEVEL directly rather than filled by hand, because KIEF_CHANCE is a rate:
        // filling it for real would be measuring the RNG, and what is under test is which branch
        // collect() takes.
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState()
                .setValue(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL)
                .setValue(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.KIEF));
        rightClick(context, ItemStack.EMPTY);
        context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), 0,
                "collecting the re-sift did not empty the screen");
        // ...and the screen goes back to taking plant matter, so a powder batch does not strand it.
        context.assertValueEqual(SiftingBoxBlock.Content.PLANT,
                context.getBlockState(BOX).getValue(SiftingBoxBlock.CONTENT),
                "an emptied screen kept its KIEF content kind and now refuses buds");

        context.runAfterDelay(2, () -> {
            context.assertItemEntityCountIs(ModItems.FILTERED_KIEF, BOX, 3.0, SiftingBoxBlock.YIELD);
            // Not plain kief, which is the mutation that slipped through, and not bubble hash --
            // a dry screen cannot make that however fine its mesh, because "bubble" IS the water.
            context.assertItemEntityNotPresent(ModItems.KIEF, BOX, 3.0);
            context.assertItemEntityNotPresent(ModItems.BUBBLE_HASH, BOX, 3.0);
            context.succeed();
        });
    }

    /**
     * A water bucket the box will not take is turned down with an accepted result, so the click ends
     * there; a ready box still hands its batch to whoever holds one.
     *
     * <p>The pour itself happens on the client, which goes on to the bucket's own use after any
     * result that is not accepted — so the result is what this asserts. Two refusals: a box that
     * already holds water, and a dry screen part-way through a batch.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aRefusedBucketIsNotPoured(GameTestHelper context) {
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.FILLED, true));
        InteractionResult full = rightClick(context, new ItemStack(Items.WATER_BUCKET));
        context.assertTrue(full.consumesAction(), "a box already holding water passed a water bucket on, so it pours: " + full);

        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.LEVEL, 3));
        InteractionResult midBatch = rightClick(context, new ItemStack(Items.WATER_BUCKET));
        context.assertTrue(midBatch.consumesAction(), "a part-sifted dry screen passed a water bucket on, so it pours: " + midBatch);
        context.assertFalse(context.getBlockState(BOX).getValue(SiftingBoxBlock.FILLED),
                "a part-sifted dry screen took water");

        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL));
        rightClick(context, new ItemStack(Items.WATER_BUCKET));
        context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), 0,
                "a ready box kept its batch from a player holding a water bucket");

        // And played out the way the client plays it, by a player looking down at the box: the water
        // a full box turned down is not then poured on top of it.
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.FILLED, true));
        BlockPos above = context.absolutePos(BOX).above();
        ServerPlayer player = Players.survival(context);
        player.moveTo(above.getX() + 0.5, above.getY(), above.getZ() + 0.5, 0, 90);
        Players.clickLikeAClient(context, player, BOX, new ItemStack(Items.WATER_BUCKET));
        context.assertFalse(context.getLevel().getFluidState(above).is(FluidTags.WATER),
                "a full box turned a water bucket down and the bucket poured on top of it");
        context.succeed();
    }

    /**
     * A box that becomes full by any road but a sift still settles. A piston push drops the tick the
     * last sift scheduled, and the box then sat full for ever; a command or a structure leaves it the
     * same way. Placed full here, which is the same road a pushed block takes back into the world.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aFullBoxSettlesHoweverItArrived(GameTestHelper context) {
        context.setBlock(BOX, ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.LEVEL, SiftingBoxBlock.FULL_LEVEL));
        context.runAtTickTime(READY_BY_TICK, () -> {
            context.assertValueEqual(context.getBlockState(BOX).getValue(SiftingBoxBlock.LEVEL), SiftingBoxBlock.READY_LEVEL,
                    "a box placed full never settled");
            context.succeed();
        });
    }
}
