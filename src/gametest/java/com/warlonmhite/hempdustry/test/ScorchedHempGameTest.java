package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.EdibleEffects;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.screen.custom.InfuserScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Scorched hemp: what the furnace makes and the vaporizer hands back, and the Infuser's third input.
 *
 * <p>Every one of these fails the generous way, which is the way nobody reports. A scorched hemp
 * counted at full weight is a furnace making tier-IV butter with no Decarboxylator — the gate the
 * whole edible chain is paid at, gone, and the butter merely looks good. A purity clamped at zero
 * grades the furnace's butter like the oven's. A sentinel inside purity's range draws a finished
 * batch as a third of a bar. None of it throws.
 *
 * <p><b>The batches are loaded from NBT rather than simmered.</b> A real batch takes 18000 ticks,
 * and what is under test is the arithmetic the tub stamps on its preview, not the clock — so each
 * tub is read in exactly as a saved world would hand it back, with nothing under it to heat it, and
 * the preview it builds on the next tick is the thing asserted.
 */
public final class ScorchedHempGameTest {

    // Two apart, so no tub's spout faces another's side. The default facing points the spout north,
    // which from row z=0 is out of the structure and from row z=2 is an empty block.
    private static final BlockPos A = new BlockPos(0, 1, 0);
    private static final BlockPos B = new BlockPos(2, 1, 0);
    private static final BlockPos C = new BlockPos(0, 1, 2);
    private static final BlockPos D = new BlockPos(2, 1, 2);

    /**
     * Four scorched hemp are one strength, rounded up — so a whole batch of it is tier I and a single
     * one is still a butter.
     */
    public static void scorchedHempCountsAQuarter(GameTestHelper context) {
        int full = InfuserBlockEntity.fullTime();
        InfuserBlockEntity allScorched = tub(context, A, full, 0, 0, InfuserBlockEntity.BATCH_CAP);
        InfuserBlockEntity oneScorched = tub(context, B, full, 0, 0, 1);
        InfuserBlockEntity mixed = tub(context, C, full, 4, 0, 4);
        InfuserBlockEntity plain = tub(context, D, full, InfuserBlockEntity.BATCH_CAP, 0, 0);

        context.runAtTickTime(3, () -> {
            context.assertValueEqual(strength(allScorched), 6,
                    "a full batch of scorched hemp was not strength 6 — it is not being counted a quarter");
            context.assertValueEqual(EdibleEffects.tierFromStrength(strength(allScorched)), 1,
                    "a full batch of scorched hemp reached a potency tier above I — the furnace skips the oven");
            context.assertValueEqual(strength(oneScorched), 1,
                    "one scorched hemp made an empty butter — the quarter is not rounding up");
            context.assertValueEqual(strength(mixed), 5,
                    "4 decarboxylated + 4 scorched was not 4 + 1");
            // The control: plain hemp still counts one each, so the numbers above are the scorched
            // weighting and not the tub under-counting everything.
            context.assertValueEqual(strength(plain), InfuserBlockEntity.BATCH_CAP,
                    "plain decarboxylated hemp stopped counting one each");
            context.succeed();
        });
    }

    /**
     * Scorched hemp subtracts from purity: on its own it never beats Rough, and against washed hemp
     * it nets out to plain.
     */
    public static void scorchedHempHoldsTheGradeDown(GameTestHelper context) {
        int full = InfuserBlockEntity.fullTime();
        int half = InfuserBlockEntity.BATCH_CAP / 2;
        InfuserBlockEntity allScorched = tub(context, A, full, 0, 0, InfuserBlockEntity.BATCH_CAP);
        InfuserBlockEntity washedAndScorched = tub(context, B, full, 0, half, half);
        InfuserBlockEntity allUnwashed = tub(context, C, full, InfuserBlockEntity.BATCH_CAP, 0, 0);
        InfuserBlockEntity allWashed = tub(context, D, full, 0, InfuserBlockEntity.BATCH_CAP, 0);

        context.runAtTickTime(3, () -> {
            context.assertValueEqual(quality(allScorched), Quality.ROUGH,
                    "an all-scorched batch at a full simmer was not Rough — purity is being clamped at zero");
            context.assertValueEqual(quality(washedAndScorched), Quality.STANDARD,
                    "half washed, half scorched at a full simmer did not net out to plain hemp's Standard");
            // The two controls: plain hemp still reaches Standard on patience alone, so scorched
            // really is the worse of the two, and a spotless batch still reaches Perfect.
            context.assertValueEqual(quality(allUnwashed), Quality.STANDARD,
                    "an all-unwashed batch at a full simmer was not Standard, so the Rough above proves nothing");
            context.assertValueEqual(quality(allWashed), Quality.PERFECT,
                    "an all-washed full simmer was not Perfect — the Perfect gate broke");
            context.succeed();
        });
    }

    /**
     * An all-scorched batch has nothing to wait for, so it is done at the minimum — and the screen
     * has to agree. Its purity is -100, a real reading that the old "-1 means no batch" sentinel
     * would have mistaken for an empty tub, drawing a finished batch as a third of a bar.
     *
     * <p>The menu is built through {@code createMenu}, which is the real server-side handler holding
     * the real property delegate — the same numbers a client is sent.
     */
    public static void allScorchedBatchIsDoneAtTheMinimum(GameTestHelper context) {
        InfuserBlockEntity infuser = tub(context, A, InfuserBlockEntity.minTime(), 0, 0,
                InfuserBlockEntity.BATCH_CAP);
        ServerPlayer player = context.makeMockServerPlayerInLevel();

        context.runAtTickTime(3, () -> {
            context.assertTrue(infuser.isAtBestQuality(),
                    "an all-scorched batch at the minimum was not at its best grade, so the spout would wait for nothing");
            context.assertValueEqual(infuser.getComparatorOutput(), 15,
                    "the comparator did not read 15 for a batch with nothing left to wait for");

            InfuserScreenHandler menu = (InfuserScreenHandler) infuser.createMenu(0, player.getInventory(), player);
            context.assertTrue(menu.getProgress() >= 1.0F,
                    "the bar shows " + menu.getProgress() + " for a finished all-scorched batch — "
                            + "a negative purity is being read as \"no batch\"");
            context.assertTrue(menu.getNextGradeMark() < 0.0F,
                    "the screen offers a next grade to an all-scorched batch, which can never have one");
            context.succeed();
        });
    }

    /**
     * The furnace takes plant matter and nothing else, and the result goes nowhere it should not:
     * not into a cauldron to be washed, and into a composter at the leaf's rate.
     */
    public static void scorchedHempSmeltsFromPlantOnly(GameTestHelper context) {
        for (Item plant : new Item[]{ModItems.HEMP_LEAF, ModItems.INDICA_BUDS, ModItems.SATIVA_BUDS, ModItems.SCHWAG}) {
            context.assertTrue(smeltsTo(context, plant).is(ModItems.SCORCHED_HEMP),
                    plant + " did not smelt to scorched hemp");
            context.assertValueEqual(smeltsTo(context, plant).getCount(), 1,
                    plant + " smelted to more than one scorched hemp — the oven's four-per-bud is the point");
        }
        // Hash never reaches butter, and a furnace would otherwise be the door it walks through.
        for (Item resin : new Item[]{ModItems.HASHISH, ModItems.CHARAS, ModItems.FILTERED_HASHISH, ModItems.ROSIN}) {
            context.assertTrue(smeltsTo(context, resin).isEmpty(), resin + " smelts — hash reaches the Infuser");
        }
        context.assertFalse(CauldronInteraction.WATER.map().containsKey(ModItems.SCORCHED_HEMP),
                "scorched hemp can be washed, which makes it a route to Clean and Perfect butter");
        context.assertValueEqual(ComposterBlock.COMPOSTABLES.getFloat(ModItems.SCORCHED_HEMP), 0.3F,
                "scorched hemp does not compost at the leaf's 0.3");
        context.succeed();
    }

    /** A tub read in from NBT, the way a saved world hands one back: milk in, batch counted, cold. */
    private static InfuserBlockEntity tub(GameTestHelper context, BlockPos pos, int progress,
                                          int unwashed, int washed, int scorched) {
        context.setBlock(pos, ModBlocks.INFUSER);
        InfuserBlockEntity infuser = context.getBlockEntity(pos, InfuserBlockEntity.class);
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("Progress", progress);
        nbt.putBoolean("HaveMilk", true);
        nbt.putInt("BatchUnwashed", unwashed);
        nbt.putInt("BatchWashed", washed);
        nbt.putInt("BatchScorched", scorched);
        infuser.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, context.getLevel().registryAccess(), nbt));
        return infuser;
    }

    private static ItemStack preview(InfuserBlockEntity infuser) {
        return infuser.getItem(InfuserBlockEntity.OUTPUT_SLOT);
    }

    private static int strength(InfuserBlockEntity infuser) {
        return preview(infuser).getOrDefault(ModComponents.STRENGTH, 0);
    }

    private static Quality quality(InfuserBlockEntity infuser) {
        return preview(infuser).get(ModComponents.QUALITY);
    }

    private static ItemStack smeltsTo(GameTestHelper context, Item input) {
        return context.getLevel().recipeAccess()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(input)), context.getLevel())
                .map(entry -> entry.value().assemble(new SingleRecipeInput(new ItemStack(input)),
                        context.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
