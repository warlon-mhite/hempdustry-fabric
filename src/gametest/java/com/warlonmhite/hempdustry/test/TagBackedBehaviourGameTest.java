package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

/**
 * Features whose whole behaviour is one entry in a tag file or one call into a registry, and which
 * therefore fail <em>silently</em> — the block simply does the old thing, with nothing in any log.
 *
 * <p>The rule they all share: <b>one unresolvable {@code required} entry drops a whole tag</b> and
 * takes its valid entries with it, and a tag that has quietly gone empty is indistinguishable from a
 * feature nobody built. A composting chance that never registered reads the same way — the composter
 * just refuses the item. Hence the negative assertions here: a test that only proves the tag matches
 * what it should would still pass against a tag that had become "everything".
 */
public final class TagBackedBehaviourGameTest implements FabricGameTest {

    /**
     * Lava heats the Infuser — the fluid and a lava cauldron alike.
     *
     * <p>Asserted through {@link InfuserBlockEntity#isHeatedFrom} rather than by standing a tub over
     * lava and waiting, because that method <em>is</em> the feature: it is the one place the tag is
     * read. Flowing lava is checked too, since {@code Blocks.LAVA} is both, and the LIT clause in
     * {@code isHeatedFrom} must not swallow a block that has no LIT property at all.
     *
     * <p>The two negatives at the end are what stop this passing against a tag that has accidentally
     * become "every block".
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void lavaHeatsTheInfuser(TestContext context) {
        context.assertTrue(InfuserBlockEntity.isHeatedFrom(Blocks.LAVA.getDefaultState()),
                "a lava source does not heat the Infuser — minecraft:lava has left #hempdustry:heat_sources");
        context.assertTrue(
                InfuserBlockEntity.isHeatedFrom(Blocks.LAVA.getDefaultState().with(net.minecraft.state.property.Properties.LEVEL_15, 3)),
                "flowing lava does not heat the Infuser, though it is the same block as the source");
        context.assertTrue(InfuserBlockEntity.isHeatedFrom(Blocks.LAVA_CAULDRON.getDefaultState()),
                "a lava cauldron does not heat the Infuser — minecraft:lava_cauldron has left the tag");

        context.assertFalse(InfuserBlockEntity.isHeatedFrom(Blocks.WATER_CAULDRON.getDefaultState()),
                "a WATER cauldron heats the Infuser, so the tag is matching more than it should");
        context.assertFalse(InfuserBlockEntity.isHeatedFrom(Blocks.STONE.getDefaultState()),
                "plain stone heats the Infuser, so the tag is matching more than it should");

        context.complete();
    }

    /**
     * Both storage blocks compost, at their vanilla counterpart's rate.
     *
     * <p>Read straight out of {@code ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE}, which is the map
     * the composter itself consults — so this fails if the Fabric registry call stops landing, and it
     * fails if the numbers drift. A composting chance that never registered is invisible: the
     * composter simply refuses the item, exactly as it does for something never meant to go in.
     *
     * <p>The two rates are deliberately different — vanilla puts hay at 0.85 and the dried kelp block,
     * its other compacted plant, at 0.5 — so asserting them separately is the point, not duplication.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void storageBlocksCompost(TestContext context) {
        assertComposts(context, ModBlocks.HEMP_BALE, 0.85F, "hay's rate");
        assertComposts(context, ModBlocks.HEMP_LEAVES, 0.5F, "the dried kelp block's rate");
        context.complete();
    }

    private static void assertComposts(TestContext context, Block block, float expected, String why) {
        float actual = ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(block.asItem());
        context.assertTrue(actual > 0.0F,
                block.asItem() + " has no composting chance at all — a composter will refuse it");
        context.assertTrue(Math.abs(actual - expected) < 0.0001F,
                block.asItem() + " composts at " + actual + " rather than " + expected + " (" + why + ")");
    }

    /**
     * Shears mine the Block of Hemp Leaves at the leaf rate.
     *
     * <p>Checked as a mining-speed number, not as tag membership, because the tag is only the means:
     * shears' 15× rule names {@code #minecraft:leaves} inside their {@code tool} component
     * ({@code ShearsItem#createToolComponent}), so this fails if either half moves — the block
     * leaving the tag, or a future vanilla version rewriting how shears declare their rules.
     */
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void shearsMineHempLeavesFast(TestContext context) {
        var leaves = ModBlocks.HEMP_LEAVES.getDefaultState();

        context.assertTrue(leaves.isIn(BlockTags.LEAVES),
                "hemp_leaves is not in #minecraft:leaves, so shears, hoes and swords all treat it as stone");

        float shears = new ItemStack(Items.SHEARS).getMiningSpeedMultiplier(leaves);
        context.assertTrue(shears > 1.0F,
                "shears mine hemp_leaves at " + shears + "x — no faster than a bare hand");

        float pickaxe = new ItemStack(Items.DIAMOND_PICKAXE).getMiningSpeedMultiplier(leaves);
        context.assertTrue(shears > pickaxe,
                "a diamond pickaxe (" + pickaxe + "x) matches or beats shears (" + shears
                        + "x) on hemp_leaves, which is not how any leaf block behaves");

        context.complete();
    }
}
