package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.HashishBarBlock;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.util.ModTags;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

/**
 * The hashish bar: the cut, what breaking a part-cut bar gives back, and whether the blade tag
 * resolves at all.
 *
 * <p>All three failures are quiet ones. <b>A cut that yields the wrong count</b> is a balance bug
 * nobody counts their way to. <b>A loot table that drops nine pieces off a half-cut bar</b> is a
 * duplication bug that presents as good luck. And <b>an unresolvable entry in
 * {@code #hempdustry:hash_cutters} drops the whole tag</b> — swords included — leaving the bar
 * uncuttable with nothing but a server-log line to say why, which is exactly how {@code #c:is_lush}
 * stopped wild indica generating in caves.
 */
public final class HashishBarGameTest {

    private static final BlockPos BAR = new BlockPos(1, 1, 1);

    /**
     * A real right-click, through {@code ServerPlayerInteractionManager}, and the result it gives.
     *
     * <p><b>Not {@code TestContext#useBlock}.</b> The bug this shape exists to catch — a refusal
     * returning {@code ActionResult.PASS} instead of {@code PASS_TO_DEFAULT_BLOCK_ACTION}, which
     * swallows the click so {@code onUse} never runs — is invisible to {@code useBlock}, which falls
     * through on <em>any</em> non-accepted result. That is how it shipped in the Dry Sifter and was
     * found by a player rather than by a test.
     */
    private static ActionResult rightClick(TestContext context, ItemStack held) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.setStackInHand(Hand.MAIN_HAND, held);
        BlockPos pos = context.getAbsolutePos(BAR);
        return player.interactionManager.interactBlock(player, context.getWorld(), held, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));
    }

    /** Five cuts give 2 + 2 + 2 + 2 + 1 = nine pieces, and the fifth takes the block with it. */
    public static void barCutsIntoNinePieces(TestContext context) {
        context.setBlockState(BAR, ModBlocks.HASHISH_BAR.getDefaultState());

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        int expected = 0;
        for (int cut = 0; cut < 5; cut++) {
            expected += cut < 4 ? 2 : 1;
            ActionResult result = rightClick(context, sword);
            // Asserted on the returned value, not just on the world: a block that mutates state and
            // then hands back PASS still swallows the click for everything that comes after it.
            context.assertTrue(result.isAccepted(),
                    "cut " + (cut + 1) + " was not accepted by the interaction manager");
            if (cut < 4) {
                context.assertEquals(context.getBlockState(BAR).get(HashishBarBlock.CUTS), cut + 1,
                        "the cut count did not advance");
            }
        }
        context.assertEquals(expected, 9, "the yield table no longer sums to nine");
        context.assertTrue(context.getBlockState(BAR).isAir(),
                "the fifth cut left the bar standing instead of removing it");
        context.assertTrue(sword.getDamage() == 5,
                "the blade should have taken one damage per cut, took " + sword.getDamage());

        context.waitAndRun(2, () -> {
            context.expectItemsAt(ModItems.HASHISH, BAR, 3.0, 9);
            context.complete();
        });
    }

    /**
     * Breaking a bar hands back what is left in it, not what a whole one holds.
     *
     * <p>At {@code cuts = 2} four pieces have already been cut off, so five remain. Nine here would
     * be the loot table ignoring the state — a dupe, and one in the generous direction.
     */
    public static void brokenBarDropsWhatIsLeft(TestContext context) {
        context.setBlockState(BAR, ModBlocks.HASHISH_BAR.getDefaultState()
                .with(HashishBarBlock.CUTS, 2));
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
        context.getWorld().breakBlock(context.getAbsolutePos(BAR), true, player);

        context.waitAndRun(2, () -> {
            context.expectItemsAt(ModItems.HASHISH, BAR, 3.0, 5);
            context.complete();
        });
    }

    /**
     * The blade tag resolves, and holds a sword.
     *
     * <p>{@code #hempdustry:hash_cutters} names {@code #c:tools/knife}, which no vanilla item joins
     * and which therefore does not exist on a client with no cooking mod installed. <b>One
     * unresolvable <em>required</em> entry drops the entire tag</b>, taking {@code #minecraft:swords}
     * with it — so this test runs in exactly the configuration that breaks it (no cooking mod on the
     * gametest runtime) and asserts the tag still has a sword in it. The failure mode it guards
     * against is a bar nobody can cut and nothing on screen to say why.
     */
    public static void hashCuttersTagResolves(TestContext context) {
        RegistryEntry<net.minecraft.item.Item> sword = Registries.ITEM.getEntry(Items.DIAMOND_SWORD);
        context.assertTrue(sword.isIn(ModTags.Items.HASH_CUTTERS),
                "#hempdustry:hash_cutters does not contain a diamond sword — an unresolvable "
                        + "required entry has almost certainly dropped the whole tag");
        // Shears deliberately stay out of it: they mean "trim a plant" in this mod and nothing else.
        context.assertFalse(Registries.ITEM.getEntry(Items.SHEARS).isIn(ModTags.Items.HASH_CUTTERS),
                "shears joined the hash cutters — they already mean 'trim a plant'");
        context.complete();
    }
}
