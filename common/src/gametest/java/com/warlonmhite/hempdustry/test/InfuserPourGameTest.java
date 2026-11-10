package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.InfuserBlock;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

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
public final class InfuserPourGameTest {

    private static final BlockPos TUB = new BlockPos(1, 1, 1);
    /** West of the tub, facing east into it. */
    private static final BlockPos DISPENSER = new BlockPos(0, 1, 1);
    /** Above the dispenser: a redstone block here is the pulse. */
    private static final BlockPos PULSE = new BlockPos(0, 2, 1);

    private static ServerPlayer survivalPlayer(GameTestHelper context) {
        return Players.survival(context);
    }

    /**
     * A real right-click through {@code ServerPlayerGameMode}. Anything more convenient is
     * more forgiving than a player's click — see SiftingBoxGameTest for the bug that taught this.
     */
    private static InteractionResult rightClick(GameTestHelper context, ServerPlayer player, ItemStack held) {
        return Players.click(context, player, TUB, held);
    }

    private static boolean screenOpen(ServerPlayer player) {
        return player.containerMenu instanceof InfuserScreenHandler;
    }

    private static boolean filled(GameTestHelper context) {
        return context.getBlockState(TUB).getValue(InfuserBlock.FILLED);
    }

    public static void milkIsPouredInByHand(GameTestHelper context) {
        context.setBlock(TUB, ModBlocks.INFUSER);
        ServerPlayer player = survivalPlayer(context);

        // Anything that is not milk opens the screen, exactly as a right-click always has.
        rightClick(context, player, new ItemStack(Items.IRON_PICKAXE));
        context.assertTrue(screenOpen(player), "right-clicking with a pickaxe did not open the screen");
        context.assertValueEqual(player.containerMenu.slots.size(), 3 + 36,
                "the menu should hold the two hemp slots, the preview and the player's 36 — a milk or bucket slot is back");
        player.closeContainer();

        // Milk into an empty tub: poured, the empty handed back, and no screen.
        InteractionResult poured = rightClick(context, player, new ItemStack(Items.MILK_BUCKET));
        context.assertTrue(poured.consumesAction(), "pouring milk was not accepted: " + poured);
        context.assertTrue(filled(context), "pouring milk did not fill the tub");
        context.assertTrue(player.getMainHandItem().is(Items.BUCKET),
                "the empty did not come back: holding " + player.getMainHandItem());
        context.assertFalse(screenOpen(player), "pouring milk opened the screen as well");

        // Milk into a full tub: the bucket is kept and the click falls through to the screen — the
        // lectern's rule. Refusing with a plain PASS would swallow it, and a player holding milk
        // could never look inside a busy tub.
        rightClick(context, player, new ItemStack(Items.MILK_BUCKET));
        context.assertTrue(player.getMainHandItem().is(Items.MILK_BUCKET), "a full tub took a second milk");
        context.assertTrue(screenOpen(player),
                "milk on a full tub did not open the screen — the fall-through is swallowing the click");
        player.closeContainer();

        // Hemp milk is milk too, through #hempdustry:milk_buckets.
        context.setBlock(TUB, Blocks.AIR);
        context.setBlock(TUB, ModBlocks.INFUSER);
        rightClick(context, player, new ItemStack(ModItems.HEMP_MILK_BUCKET));
        context.assertTrue(filled(context), "hemp milk did not fill the tub");
        context.assertTrue(player.getMainHandItem().is(Items.BUCKET),
                "hemp milk's empty did not come back: holding " + player.getMainHandItem());

        context.succeed();
    }

    /**
     * Pours, keeps the empty, refuses a full tub without spitting the milk out, and still drops milk
     * like vanilla when there is no tub in front. One item in the dispenser per pulse, because a
     * dispenser fires a random occupied slot.
     */
    public static void dispenserPoursMilkAndKeepsTheBucket(GameTestHelper context) {
        context.setBlock(TUB, ModBlocks.INFUSER);
        context.setBlock(DISPENSER, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        DispenserBlockEntity dispenser = context.getBlockEntity(DISPENSER, DispenserBlockEntity.class);

        dispenser.setItem(0, new ItemStack(Items.MILK_BUCKET));
        context.pulseRedstone(PULSE, 1);

        context.runAtTickTime(10, () -> {
            context.assertTrue(filled(context), "the dispenser did not pour its milk into the tub");
            context.assertTrue(dispenser.getItem(0).is(Items.BUCKET),
                    "the dispenser did not keep the empty: slot 0 holds " + dispenser.getItem(0));

            dispenser.setItem(0, new ItemStack(Items.MILK_BUCKET));
            context.pulseRedstone(PULSE, 1);
        });

        context.runAtTickTime(20, () -> {
            context.assertTrue(dispenser.getItem(0).is(Items.MILK_BUCKET),
                    "a dispenser facing a full tub did not keep its milk: slot 0 holds " + dispenser.getItem(0));
            context.assertValueEqual(count(context, Items.MILK_BUCKET), 0, "milk was dropped in front of a full tub");

            context.setBlock(TUB, Blocks.AIR);
            context.pulseRedstone(PULSE, 1);
        });

        context.runAtTickTime(30, () -> {
            context.assertTrue(dispenser.getItem(0).isEmpty(),
                    "with no tub in front the milk was not dispensed at all: slot 0 holds " + dispenser.getItem(0));
            context.assertValueEqual(count(context, Items.MILK_BUCKET), 1,
                    "with no tub in front the milk was not dropped, as vanilla does");
            context.succeed();
        });
    }

    /**
     * What a 2.0.0-beta save can hold in the two slots that are gone — a milk bucket parked for the
     * next batch and some empties waiting to be collected — comes out of the top of the tub. Set with
     * setItem, which is exactly what reading an old save does to the same array.
     */
    public static void retiredSlotsHandBackTheirItems(GameTestHelper context) {
        context.setBlock(TUB, ModBlocks.INFUSER);
        InfuserBlockEntity infuser = context.getBlockEntity(TUB, InfuserBlockEntity.class);
        infuser.setItem(InfuserBlockEntity.RETIRED_MILK_SLOT, new ItemStack(Items.MILK_BUCKET));
        infuser.setItem(InfuserBlockEntity.RETIRED_BUCKET_SLOT, new ItemStack(Items.BUCKET, 3));

        context.runAtTickTime(5, () -> {
            context.assertTrue(infuser.getItem(InfuserBlockEntity.RETIRED_MILK_SLOT).isEmpty(),
                    "the old milk slot still holds " + infuser.getItem(InfuserBlockEntity.RETIRED_MILK_SLOT));
            context.assertTrue(infuser.getItem(InfuserBlockEntity.RETIRED_BUCKET_SLOT).isEmpty(),
                    "the old bucket slot still holds " + infuser.getItem(InfuserBlockEntity.RETIRED_BUCKET_SLOT));
            context.assertValueEqual(count(context, Items.MILK_BUCKET), 1, "the parked milk bucket was not handed back");
            context.assertValueEqual(count(context, Items.BUCKET), 3, "the parked empties were not all handed back");
            context.assertFalse(filled(context), "the parked milk was poured in rather than handed back");
            context.succeed();
        });
    }

    private static int count(GameTestHelper context, Item item) {
        int total = 0;
        for (ItemEntity entity : context.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) {
                total += entity.getItem().getCount();
            }
        }
        return total;
    }
}
