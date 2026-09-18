package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import net.minecraft.block.Blocks;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

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
     * A real right-click on the screen, through {@code ServerPlayerInteractionManager}.
     *
     * <p><b>This has to be the interaction manager and nothing else.</b> The bug this test exists
     * for — the block refusing a click with {@code ActionResult.PASS}, which swallows it so
     * {@code onUse} never runs and a full screen can never be emptied — is invisible to both of the
     * convenient ways of faking a click. Calling {@code onUse} directly never asks the question, and
     * {@code TestContext#useBlock} falls through to {@code onUse} on <em>any</em> non-accepted
     * result where the real manager insists on {@code PassToDefaultBlockAction} specifically. Both
     * were perfectly happy with the broken block while a player could not get the bar out.
     */
    private static ActionResult rightClick(TestContext context, ItemStack held) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.setStackInHand(Hand.MAIN_HAND, held);
        BlockPos pos = context.getAbsolutePos(BOX);
        return player.interactionManager.interactBlock(player, context.getWorld(), held, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
    }

    public static void dryScreenFillsAndYieldsKief(TestContext context) {
        context.setBlockState(BOX, ModBlocks.SIFTING_BOX.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(BOX);
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);

        // A pickaxe, not a block: right-clicking with a BlockItem legitimately *places* it, which
        // is vanilla behaviour and would be measuring the wrong thing.
        ItemStack notPlantMatter = new ItemStack(Items.IRON_PICKAXE);
        rightClick(context, notPlantMatter);
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 0,
                "the screen took something that is not plant matter");

        ItemStack buds = new ItemStack(ModItems.INDICA_BUDS, 64);
        for (int i = 0; i < SiftingBoxBlock.FULL_LEVEL; i++) {
            world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        }
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL),
                SiftingBoxBlock.FULL_LEVEL,
                "buds fill at chance 1.0, so " + SiftingBoxBlock.FULL_LEVEL + " of them should fill the screen");
        context.assertEquals(buds.getCount(), 64 - SiftingBoxBlock.FULL_LEVEL,
                "the screen did not charge for exactly what it took");

        // A full screen takes nothing more: the powder is waiting to be pressed, not to be topped up.
        world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        context.assertEquals(buds.getCount(), 64 - SiftingBoxBlock.FULL_LEVEL,
                "a full screen went on eating buds");

        context.runAtTick(READY_BY_TICK, () -> {
            context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL),
                    SiftingBoxBlock.READY_LEVEL,
                    "the scheduled tick never pressed the powder into a lump");

            // The reported bug, driven exactly as a player drives it: a real right-click, holding
            // something the screen will not take. Empty-handed works too, but this is the case that
            // was broken — and the case a player is in, since they arrive holding trim.
            rightClick(context, new ItemStack(Items.IRON_PICKAXE));
            context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 0,
                    "collecting the lump did not empty the screen");

            // And it is empty for real: a second collection must produce nothing.
            rightClick(context, ItemStack.EMPTY);

            context.waitAndRun(2, () -> {
                // Nine loose pieces of KIEF, and NOT a bar. This block does not press; that is the
                // Hemp Press's job and the whole reason the two are separate blocks.
                context.expectItemsAt(ModItems.KIEF, BOX, 3.0, SiftingBoxBlock.YIELD);
                context.dontExpectItemAt(ModBlocks.HASHISH_BAR.asItem(), BOX, 3.0);
                context.dontExpectItemAt(ModItems.BUBBLE_HASH, BOX, 3.0);
                context.complete();
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
    public static void iceWashNeedsItsJacket(TestContext context) {
        context.setBlockState(BOX, ModBlocks.SIFTING_BOX.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(BOX);
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
        ItemStack buds = new ItemStack(ModItems.INDICA_BUDS, 64);

        // A real right-click with a bucket. The box is empty, so it takes the water.
        rightClick(context, new ItemStack(Items.WATER_BUCKET));
        context.assertTrue(context.getBlockState(BOX).get(SiftingBoxBlock.FILLED),
                "a water bucket did not fill the box");

        // Filled but not jacketed: the wash cannot run, and the refusal must PASS THE CLICK ON
        // rather than swallow it -- see sifterRefusesAnUnjacketedWash's sibling case below.
        ActionResult bare = world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        context.assertTrue(bare instanceof ActionResult.PassToDefaultBlockAction,
                "an unjacketed wash swallowed the click instead of passing it on");
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 0,
                "a filled box with no ice around it washed a bud");
        context.assertEquals(buds.getCount(), 64, "an unjacketed box charged for a bud it refused");

        // Jacket it in blue ice and the same box runs at rate 1.0.
        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(BOX.offset(side), Blocks.BLUE_ICE.getDefaultState());
        }
        for (int i = 0; i < SiftingBoxBlock.FULL_LEVEL - 1; i++) {
            world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        }
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL),
                SiftingBoxBlock.FULL_LEVEL - 1,
                "a blue-ice jacket is rate 1.0, so every bud should have advanced the box");

        // ...and now the failure the design invites: the jacket melts mid-batch. One side to air is
        // what a torch leaves behind after vanilla's IceBlock melts.
        context.setBlockState(BOX.north(), Blocks.AIR.getDefaultState());
        ActionResult melted = world.getBlockState(pos)
                .onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        context.assertTrue(melted instanceof ActionResult.PassToDefaultBlockAction,
                "a box whose jacket melted swallowed the click");
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL),
                SiftingBoxBlock.FULL_LEVEL - 1,
                "the wash went on running with a hole in its jacket");

        context.setBlockState(BOX.north(), Blocks.BLUE_ICE.getDefaultState());
        world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL),
                SiftingBoxBlock.FULL_LEVEL,
                "the box did not resume once its jacket was repaired");

        context.runAtTick(READY_BY_TICK, () -> {
            rightClick(context, ItemStack.EMPTY);
            // The water is spent WITH the batch, not before it: a finished wash has to still
            // remember which process made it, and FILLED is how it does.
            context.assertTrue(!context.getBlockState(BOX).get(SiftingBoxBlock.FILLED),
                    "collecting the batch did not spend the box's water");

            context.waitAndRun(2, () -> {
                // The washed powder, and never the dry one. Same box, same inputs, different product
                // -- which is the entire reason the two blocks became one.
                context.expectItemsAt(ModItems.BUBBLE_HASH, BOX, 3.0, SiftingBoxBlock.YIELD);
                context.dontExpectItemAt(ModItems.KIEF, BOX, 3.0);
                context.complete();
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
     * if a refusal returns {@code ActionResult.PASS} rather than {@code super}, it <em>swallows</em>
     * the click — the box stops accepting anything and can never be emptied, which is precisely the
     * bug that shipped in this block once already and was found by a player.
     *
     * <p>So this asserts the returned {@code ActionResult} as well as the level: a block that
     * silently ignores a click and one that correctly passes it on are indistinguishable from the
     * world state alone, and only one of them still lets {@code onUse} run.
     */
    public static void screenRefusesAMixedLoad(TestContext context) {
        context.setBlockState(BOX, ModBlocks.SIFTING_BOX.getDefaultState());

        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(BOX);
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);

        // Two buds in: the screen is now a PLANT screen, and stamped as one.
        ItemStack buds = new ItemStack(ModItems.INDICA_BUDS, 64);
        world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 2,
                "two buds did not fill two levels");
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.CONTENT),
                SiftingBoxBlock.Content.PLANT, "the first bud did not stamp the screen as PLANT");

        ItemStack kief = new ItemStack(ModItems.KIEF, 64);
        ActionResult refused = world.getBlockState(pos)
                .onUseWithItem(kief, world, player, Hand.MAIN_HAND, hit);
        context.assertTrue(refused instanceof ActionResult.PassToDefaultBlockAction,
                "a refused load must pass the click on, not swallow it with PASS");
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 2,
                "kief went onto a plant screen");
        context.assertEquals(kief.getCount(), 64, "the screen charged for a load it refused");

        // ...and the same screen still takes plant matter, so the refusal cost nothing.
        world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 3,
                "the refusal left the screen unable to take its own content kind");

        // A fresh box takes kief happily, and stamps itself KIEF.
        context.setBlockState(BOX, ModBlocks.SIFTING_BOX.getDefaultState());
        for (int i = 0; i < 40 && context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL) == 0; i++) {
            world.getBlockState(pos).onUseWithItem(kief, world, player, Hand.MAIN_HAND, hit);
        }
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.CONTENT),
                SiftingBoxBlock.Content.KIEF,
                "kief did not stamp an empty screen as KIEF — the re-sift is unreachable");

        // A WASH TAKES PLANT MATTER AND NOTHING ELSE. Kief into a filled box must be refused, or
        // there is a third road to filtered hashish and one of the three becomes a trap.
        context.setBlockState(BOX, ModBlocks.SIFTING_BOX.getDefaultState()
                .with(SiftingBoxBlock.FILLED, true));
        for (Direction side : Direction.Type.HORIZONTAL) {
            context.setBlockState(BOX.offset(side), Blocks.BLUE_ICE.getDefaultState());
        }
        ItemStack washKief = new ItemStack(ModItems.KIEF, 64);
        ActionResult wet = world.getBlockState(pos)
                .onUseWithItem(washKief, world, player, Hand.MAIN_HAND, hit);
        context.assertTrue(wet instanceof ActionResult.PassToDefaultBlockAction,
                "a wash refusing kief swallowed the click");
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 0,
                "the ice wash accepted kief — that is a third road to filtered hashish");
        context.assertEquals(washKief.getCount(), 64, "the wash charged for kief it refused");

        // The control: the same jacketed, filled box still takes buds, so the refusal above is
        // about the kief and not about the box being broken.
        world.getBlockState(pos).onUseWithItem(buds, world, player, Hand.MAIN_HAND, hit);
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 1,
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
        context.setBlockState(BOX, ModBlocks.SIFTING_BOX.getDefaultState()
                .with(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL)
                .with(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.KIEF));
        rightClick(context, ItemStack.EMPTY);
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.LEVEL), 0,
                "collecting the re-sift did not empty the screen");
        // ...and the screen goes back to taking plant matter, so a powder batch does not strand it.
        context.assertEquals(context.getBlockState(BOX).get(SiftingBoxBlock.CONTENT),
                SiftingBoxBlock.Content.PLANT,
                "an emptied screen kept its KIEF content kind and now refuses buds");

        context.waitAndRun(2, () -> {
            context.expectItemsAt(ModItems.FILTERED_KIEF, BOX, 3.0, SiftingBoxBlock.YIELD);
            // Not plain kief, which is the mutation that slipped through, and not bubble hash --
            // a dry screen cannot make that however fine its mesh, because "bubble" IS the water.
            context.dontExpectItemAt(ModItems.KIEF, BOX, 3.0);
            context.dontExpectItemAt(ModItems.BUBBLE_HASH, BOX, 3.0);
            context.complete();
        });
    }
}
