package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Charas comes off the shears when a <em>ripe</em> plant is rubbed, and it does so <em>sometimes</em>.
 *
 * <p>Three failures here, all silent. <b>A roll that never fires</b> — a typo'd condition, a bound
 * off by one — looks exactly like bad luck, and a player who has rubbed thirty plants without seeing
 * one has no way to tell the difference. <b>A roll that fires every time</b> looks like good luck
 * and is a 4× balance error in the generous direction, which is the sort nobody reports. And
 * <b>a rub accepted outside the ripe window</b> would quietly move the whole mechanic back onto the
 * trim, which is exactly what this change was undoing.
 *
 * <p><b>This deliberately does not assert the rate.</b> {@code CHARAS_CHANCE_ONE_IN} is a balance
 * number, not a mechanism, and a test that pinned it would be measuring the RNG and would go red the
 * day the number is tuned — which is exactly the reasoning {@code SiftingBoxGameTest} gives for
 * driving its fill with buds rather than trim. What is asserted is that the roll is a roll: it
 * happens, and it does not always happen. Across {@value #RUBS} plants at any sane probability the
 * chance of a false red is far below the chance of the harness itself failing.
 */
public final class CharasGameTest {

    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();

    /** Enough plants that "never" and "always" are both unmistakable at any plausible rate. */
    private static final int RUBS = 240;

    public static void rubbingARipePlantSometimesYieldsCharas(GameTestHelper context) {
        context.setBlock(SOIL, Blocks.FARMLAND);
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        // Creative-mode shears would still take damage in survival; a fresh stack each time keeps
        // the tool out of the measurement entirely.
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        // A ripe plant refuses a second rub: the flag is what makes it once-per-plant, and without
        // that a single plant plus one pair of shears would pay out ~30 charas.
        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState()
                .setValue(IndicaCropBlock.AGE, Defoliation.RUB_MAX_AGE));
        ItemStack probe = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, probe);
        world.getBlockState(pos).useItemOn(probe, world, player, InteractionHand.MAIN_HAND, hit);
        context.assertTrue(context.getBlockState(CROP).getValue(Defoliation.RUBBED),
                "rubbing a ripe plant did not set RUBBED");
        context.assertTrue(!world.getBlockState(pos)
                        .useItemOn(probe, world, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "a plant that has already been rubbed accepted a second rub");

        // A plant past its late-trim window but not yet ripe is refused: the spent-window guard.
        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState()
                .setValue(IndicaCropBlock.AGE, Defoliation.RUB_MIN_AGE - 1)
                .setValue(Defoliation.TRIMMED_LATE, true));
        context.assertTrue(!world.getBlockState(pos)
                        .useItemOn(probe, world, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "a plant whose late-trim window is spent accepted another interaction");

        // A SEEDLING is the probe that matters for the rub window's lower bound. Ages below
        // EARLY_MIN_AGE fall past every trim branch, so they land on the rub branch's guard and
        // nothing else -- which makes this the only age that goes red if that guard is widened.
        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState()
                .setValue(IndicaCropBlock.AGE, Defoliation.EARLY_MIN_AGE - 1));
        context.assertTrue(!world.getBlockState(pos)
                        .useItemOn(probe, world, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "a seedling accepted a rub — the ripe window's lower bound is not holding");
        context.assertTrue(!context.getBlockState(CROP).getValue(Defoliation.RUBBED),
                "a seedling was marked rubbed");

        int accepted = 0;
        for (int i = 0; i < RUBS; i++) {
            // A fresh ripe plant each pass: one rub per plant is the rule, so many rubs means many
            // plants, which is exactly the cost the mechanic is priced on.
            context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState()
                    .setValue(IndicaCropBlock.AGE, Defoliation.RUB_MAX_AGE));
            ItemStack shears = new ItemStack(Items.SHEARS);
            player.setItemInHand(InteractionHand.MAIN_HAND, shears);
            if (world.getBlockState(pos).useItemOn(shears, world, player, InteractionHand.MAIN_HAND, hit)
                    .consumesAction()) {
                accepted++;
            }
        }
        context.assertValueEqual(accepted, RUBS, "not every rub on a ripe plant was accepted");

        // Counted off the ground rather than through assertItemEntityCountIs, which asserts an exact number:
        // the whole point here is that the number is random and only its extremes are wrong.
        AABB area = AABB.encapsulatingFullBlocks(pos.offset(-4, -2, -4), pos.offset(4, 3, 4));
        int charas = 0;
        int leaves = 0;
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, area, e -> true)) {
            if (item.getItem().is(ModItems.CHARAS)) {
                charas += item.getItem().getCount();
            } else if (item.getItem().is(ModItems.HEMP_LEAF)) {
                leaves += item.getItem().getCount();
            }
        }

        // Every rub cuts exactly one sugar leaf, charas or not -- the RUBS in the loop plus the probe
        // rub at the top. The refused interactions above must add none. That the leaf then comes off
        // the harvest is DefoliationGameTest's everyLeafTakenComesOffTheHarvest.
        context.assertValueEqual(RUBS + 1, leaves, "rubbing a ripe plant did not drop exactly one hemp leaf");
        context.assertTrue(charas > 0,
                "no charas in " + RUBS + " rubs — the roll never fires");
        context.assertTrue(charas < RUBS,
                "charas on all " + RUBS + " rubs — the roll is not a roll");
        context.succeed();
    }

    /**
     * A rub is not a trim, so it must not grant the trimming advancement.
     *
     * <p>{@code hempdustry:trim_season} fires on vanilla's {@code item_used_on_block}, which
     * triggers whenever {@code useItemOn} returns an accepted result — and since the rub landed,
     * {@code Defoliation.tryCut} accepts in <b>three</b> windows rather than two. Without an age
     * filter on the criterion, rubbing a ripe plant hands over "Trim Season" to a player who has
     * never trimmed anything.
     *
     * <p><b>Driven against the real advancement as the server loaded it</b>, not against a rebuilt
     * predicate: the whole risk lives in the generated JSON, so a test that re-derived the condition
     * would agree with a wrong one. Both halves are asserted — a rub must not grant it, and a trim
     * must — because a filter that is merely too narrow would break the advancement outright and
     * look, from the first assertion alone, exactly like a fix.
     *
     * <p>Age is the discriminator for a reason worth keeping: it is the one property synced onto
     * <b>both halves</b> of a two-block plant, and the criterion tests whichever block the player
     * actually clicked. The trim flags are canonical on the lower half only.
     */
    public static void rubbingDoesNotGrantTheTrimAdvancement(GameTestHelper context) {
        context.setBlock(SOIL, Blocks.FARMLAND);
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        AdvancementHolder trimSeason = world.getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath("hempdustry", "trim_season"));
        context.assertTrue(trimSeason != null, "hempdustry:trim_season is not loaded at all");

        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        context.assertTrue(!player.getAdvancements().getOrStartProgress(trimSeason).isDone(),
                "the mock player already had Trim Season before doing anything");

        // Rub a ripe plant, through the real interaction manager so the vanilla trigger actually
        // fires -- calling useItemOn directly would never reach ServerPlayerGameMode.
        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState()
                .setValue(IndicaCropBlock.AGE, Defoliation.RUB_MAX_AGE));
        player.gameMode.useItemOn(player, world, shears, InteractionHand.MAIN_HAND, hit);
        context.assertTrue(context.getBlockState(CROP).getValue(Defoliation.RUBBED),
                "the rub did not happen, so this test proves nothing");
        context.assertTrue(!player.getAdvancements().getOrStartProgress(trimSeason).isDone(),
                "rubbing a ripe plant granted Trim Season — the criterion is not age-filtered");

        // ...and the advancement still works. A filter that is too narrow breaks it outright, and
        // from the assertion above alone that is indistinguishable from a fix.
        context.setBlock(CROP, ModBlocks.INDICA_CROP.defaultBlockState()
                .setValue(IndicaCropBlock.AGE, Defoliation.EARLY_MIN_AGE));
        player.gameMode.useItemOn(player, world, shears, InteractionHand.MAIN_HAND, hit);
        context.assertTrue(context.getBlockState(CROP).getValue(Defoliation.TRIMMED_EARLY),
                "the trim did not happen, so this test proves nothing");
        context.assertTrue(player.getAdvancements().getOrStartProgress(trimSeason).isDone(),
                "trimming a growing plant no longer grants Trim Season");
        context.succeed();
    }
}
