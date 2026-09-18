package com.warlonmhite.hempdustry.test;

import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/**
 * Registers the game tests.
 *
 * <p>Since 1.21.5 a game test is <b>not</b> an annotated method any more: {@code @GameTest} is gone
 * and a test is a {@code Consumer<TestContext>} in {@link Registries#TEST_FUNCTION} paired with a
 * {@code test_instance} JSON in the test mod's datapack, which is what names the structure, the
 * environment and the tick budget. Fabric's {@code fabric-gametest} entrypoint still exists, but it
 * only records which mod a test class belongs to.
 *
 * <p>So the wiring is: one entry here, one file under
 * {@code src/gametest/resources/data/hempdustry-gametest/test_instance/}. A test registered here
 * with no JSON beside it does not run, and a JSON naming a function nobody registered is a hard
 * startup failure -- so the two are added and removed together.
 *
 * <p><b>This list is branch-specific, and that is deliberate.</b> A feature branch adds its own
 * tests here and brings them along when it merges; nothing on this branch may reference a class
 * that only exists on another one, or the whole source set stops compiling.
 */
public class HempdustryGameTests implements ModInitializer {
    public static final String NAMESPACE = "hempdustry-gametest";

    @Override
    public void onInitialize() {
        register("freshly_planted_crop_is_untrimmed", DefoliationGameTest::freshlyPlantedCropIsUntrimmed);
        register("trim_survives_growth_to_maturity", DefoliationGameTest::trimSurvivesGrowthToMaturity);
        register("every_leaf_taken_comes_off_the_harvest",
                DefoliationGameTest::everyLeafTakenComesOffTheHarvest);
        register("tall_grass_seed_pool_fires_once_per_plant",
                TallPlantLootGameTest::tallGrassSeedPoolFiresOncePerPlant);
        register("lava_heats_the_infuser", TagBackedBehaviourGameTest::lavaHeatsTheInfuser);
        register("shears_mine_hemp_leaves_fast", TagBackedBehaviourGameTest::shearsMineHempLeavesFast);
        register("storage_blocks_compost", TagBackedBehaviourGameTest::storageBlocksCompost);
        register("milk_is_poured_in_by_hand", InfuserPourGameTest::milkIsPouredInByHand);
        register("dispenser_pours_milk_and_keeps_the_bucket",
                InfuserPourGameTest::dispenserPoursMilkAndKeepsTheBucket);
        register("retired_slots_hand_back_their_items", InfuserPourGameTest::retiredSlotsHandBackTheirItems);
        register("growth_multiplier_scales_the_odds", CropGrowthGameTest::growthMultiplierScalesTheOdds);
    }

    private static void register(String name, Consumer<TestContext> test) {
        Registry.register(Registries.TEST_FUNCTION, Identifier.of(NAMESPACE, name), test);
    }
}
