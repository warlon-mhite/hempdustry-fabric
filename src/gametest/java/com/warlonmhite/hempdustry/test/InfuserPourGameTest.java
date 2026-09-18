package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.InfuserBlock;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreenHandler;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * Milk goes into the Infuser in the world, like water into a cauldron — by hand or by dispenser —
 * and the two slots it used to go through are gone.
 *
 * <p>Every failure here is quiet. A pour that eats the bucket looks like a pour. A full tub that
 * swallows the click instead of opening the screen looks like the tub being busy, and a player
 * holding milk could then never look inside it. A dispenser that drops the milk in front of a full
 * tub looks like a dispenser. And a beta world's parked bucket left in a slot nothing can reach any
 * more is simply gone, with nothing on screen to say it ever existed.
 */
public final class InfuserPourGameTest implements FabricGameTest {

    private static final BlockPos TUB = new BlockPos(1, 1, 1);
    /** West of the tub, facing east into it. */
    private static final BlockPos DISPENSER = new BlockPos(0, 1, 1);
    /** Above the dispenser: a redstone block here is the pulse. */
    private static final BlockPos PULSE = new BlockPos(0, 2, 1);

    private static ServerPlayerEntity survivalPlayer(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        return player;
    }

    /**
     * A real right-click through {@code ServerPlayerInteractionManager}. Anything more convenient is
     * more forgiving than a player's click — see SiftingBoxGameTest for the bug that taught this.
     */
    private static ActionResult rightClick(TestContext context, ServerPlayerEntity player, ItemStack held) {
        player.setStackInHand(Hand.MAIN_HAND, held);
        BlockPos pos = context.getAbsolutePos(TUB);
        return player.interactionManager.interactBlock(player, context.getWorld(), held, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
    }

    private static boolean screenOpen(ServerPlayerEntity player) {
        return player.currentScreenHandler instanceof InfuserScreenHandler;
    }

    private static boolean filled(TestContext context) {
        return context.getBlockState(TUB).get(InfuserBlock.FILLED);
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void milkIsPouredInByHand(TestContext context) {
        context.setBlockState(TUB, ModBlocks.INFUSER);
        ServerPlayerEntity player = survivalPlayer(context);

        // Anything that is not milk opens the screen, exactly as a right-click always has.
        rightClick(context, player, new ItemStack(Items.IRON_PICKAXE));
        context.assertTrue(screenOpen(player), "right-clicking with a pickaxe did not open the screen");
        context.assertEquals(player.currentScreenHandler.slots.size(), 3 + 36,
                "the menu should hold the two hemp slots, the preview and the player's 36 — a milk or bucket slot is back");
        player.closeHandledScreen();

        // Milk into an empty tub: poured, the empty handed back, and no screen.
        ActionResult poured = rightClick(context, player, new ItemStack(Items.MILK_BUCKET));
        context.assertTrue(poured.isAccepted(), "pouring milk was not accepted: " + poured);
        context.assertTrue(filled(context), "pouring milk did not fill the tub");
        context.assertTrue(player.getMainHandStack().isOf(Items.BUCKET),
                "the empty did not come back: holding " + player.getMainHandStack());
        context.assertFalse(screenOpen(player), "pouring milk opened the screen as well");

        // Milk into a full tub: the bucket is kept and the click falls through to the screen — the
        // lectern's rule. Refusing with a plain PASS would swallow it, and a player holding milk
        // could never look inside a busy tub.
        rightClick(context, player, new ItemStack(Items.MILK_BUCKET));
        context.assertTrue(player.getMainHandStack().isOf(Items.MILK_BUCKET), "a full tub took a second milk");
        context.assertTrue(screenOpen(player),
                "milk on a full tub did not open the screen — the fall-through is swallowing the click");
        player.closeHandledScreen();

        // Hemp milk is milk too, through #hempdustry:milk_buckets.
        context.setBlockState(TUB, Blocks.AIR);
        context.setBlockState(TUB, ModBlocks.INFUSER);
        rightClick(context, player, new ItemStack(ModItems.HEMP_MILK_BUCKET));
        context.assertTrue(filled(context), "hemp milk did not fill the tub");
        context.assertTrue(player.getMainHandStack().isOf(Items.BUCKET),
                "hemp milk's empty did not come back: holding " + player.getMainHandStack());

        context.complete();
    }

    /**
     * Pours, keeps the empty, refuses a full tub without spitting the milk out, and still drops milk
     * like vanilla when there is no tub in front. One item in the dispenser per pulse, because a
     * dispenser fires a random occupied slot.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void dispenserPoursMilkAndKeepsTheBucket(TestContext context) {
        context.setBlockState(TUB, ModBlocks.INFUSER);
        context.setBlockState(DISPENSER, Blocks.DISPENSER.getDefaultState().with(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = (DispenserBlockEntity) context.getBlockEntity(DISPENSER);

        dispenser.setStack(0, new ItemStack(Items.MILK_BUCKET));
        context.putAndRemoveRedstoneBlock(PULSE, 1);

        context.runAtTick(10, () -> {
            context.assertTrue(filled(context), "the dispenser did not pour its milk into the tub");
            context.assertTrue(dispenser.getStack(0).isOf(Items.BUCKET),
                    "the dispenser did not keep the empty: slot 0 holds " + dispenser.getStack(0));

            dispenser.setStack(0, new ItemStack(Items.MILK_BUCKET));
            context.putAndRemoveRedstoneBlock(PULSE, 1);
        });

        context.runAtTick(20, () -> {
            context.assertTrue(dispenser.getStack(0).isOf(Items.MILK_BUCKET),
                    "a dispenser facing a full tub did not keep its milk: slot 0 holds " + dispenser.getStack(0));
            context.assertEquals(count(context, Items.MILK_BUCKET), 0, "milk was dropped in front of a full tub");

            context.setBlockState(TUB, Blocks.AIR);
            context.putAndRemoveRedstoneBlock(PULSE, 1);
        });

        context.runAtTick(30, () -> {
            context.assertTrue(dispenser.getStack(0).isEmpty(),
                    "with no tub in front the milk was not dispensed at all: slot 0 holds " + dispenser.getStack(0));
            context.assertEquals(count(context, Items.MILK_BUCKET), 1,
                    "with no tub in front the milk was not dropped, as vanilla does");
            context.complete();
        });
    }

    /**
     * What a 2.0.0-beta save can hold in the two slots that are gone — a milk bucket parked for the
     * next batch and some empties waiting to be collected — comes out of the top of the tub. Set with
     * setStack, which is exactly what reading an old save does to the same array.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void retiredSlotsHandBackTheirItems(TestContext context) {
        context.setBlockState(TUB, ModBlocks.INFUSER);
        InfuserBlockEntity infuser = (InfuserBlockEntity) context.getBlockEntity(TUB);
        infuser.setStack(InfuserBlockEntity.RETIRED_MILK_SLOT, new ItemStack(Items.MILK_BUCKET));
        infuser.setStack(InfuserBlockEntity.RETIRED_BUCKET_SLOT, new ItemStack(Items.BUCKET, 3));

        context.runAtTick(5, () -> {
            context.assertTrue(infuser.getStack(InfuserBlockEntity.RETIRED_MILK_SLOT).isEmpty(),
                    "the old milk slot still holds " + infuser.getStack(InfuserBlockEntity.RETIRED_MILK_SLOT));
            context.assertTrue(infuser.getStack(InfuserBlockEntity.RETIRED_BUCKET_SLOT).isEmpty(),
                    "the old bucket slot still holds " + infuser.getStack(InfuserBlockEntity.RETIRED_BUCKET_SLOT));
            context.assertEquals(count(context, Items.MILK_BUCKET), 1, "the parked milk bucket was not handed back");
            context.assertEquals(count(context, Items.BUCKET), 3, "the parked empties were not all handed back");
            context.assertFalse(filled(context), "the parked milk was poured in rather than handed back");
            context.complete();
        });
    }

    private static int count(TestContext context, Item item) {
        int total = 0;
        for (ItemEntity entity : context.getEntities(EntityType.ITEM)) {
            if (entity.getStack().isOf(item)) {
                total += entity.getStack().getCount();
            }
        }
        return total;
    }
}
