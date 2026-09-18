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
        register("dry_screen_fills_and_yields_kief",
                SiftingBoxGameTest::dryScreenFillsAndYieldsKief);
        register("creeper_seeks_hemp_crop", CreeperSeeksHempGameTest::creeperSeeksHempCrop);
        register("hemp_leaf_is_a_loom_pattern_item",
                BannerPatternGameTest::hempLeafIsALoomPatternItem);
        register("bar_cuts_into_nine_pieces", HashishBarGameTest::barCutsIntoNinePieces);
        register("broken_bar_drops_what_is_left", HashishBarGameTest::brokenBarDropsWhatIsLeft);
        register("hash_cutters_tag_resolves", HashishBarGameTest::hashCuttersTagResolves);
        register("rubbing_a_ripe_plant_sometimes_yields_charas",
                CharasGameTest::rubbingARipePlantSometimesYieldsCharas);
        register("rubbing_does_not_grant_the_trim_advancement",
                CharasGameTest::rubbingDoesNotGrantTheTrimAdvancement);
        register("ice_wash_needs_its_jacket", SiftingBoxGameTest::iceWashNeedsItsJacket);
        register("screen_refuses_a_mixed_load", SiftingBoxGameTest::screenRefusesAMixedLoad);
        register("vaporizer_bowl_returns_one_avb", VaporizerGameTest::vaporizerBowlReturnsOneAvb);
        register("vaporizer_refuses_two_buds", VaporizerGameTest::vaporizerRefusesTwoBuds);
        register("vaporizer_only_returns_what_grew_on_a_plant",
                VaporizerGameTest::vaporizerOnlyReturnsWhatGrewOnAPlant);
        register("scorched_hemp_counts_a_quarter", ScorchedHempGameTest::scorchedHempCountsAQuarter);
        register("scorched_hemp_holds_the_grade_down", ScorchedHempGameTest::scorchedHempHoldsTheGradeDown);
        register("all_scorched_batch_is_done_at_the_minimum",
                ScorchedHempGameTest::allScorchedBatchIsDoneAtTheMinimum);
        register("scorched_hemp_smelts_from_plant_only", ScorchedHempGameTest::scorchedHempSmeltsFromPlantOnly);
        register("one_rosin_fills_a_bong", ConcentrateGameTest::oneRosinFillsABong);
        register("moon_rock_packs_as_a_two_entry_bowl",
                ConcentrateGameTest::moonRockPacksAsATwoEntryBowl);
        register("lava_heats_the_infuser", TagBackedBehaviourGameTest::lavaHeatsTheInfuser);
        register("shears_mine_hemp_leaves_fast", TagBackedBehaviourGameTest::shearsMineHempLeavesFast);
        register("storage_blocks_compost", TagBackedBehaviourGameTest::storageBlocksCompost);
        register("creative_tab_populates", CreativeTabGameTest::creativeTabPopulates);
        register("milk_is_poured_in_by_hand", InfuserPourGameTest::milkIsPouredInByHand);
        register("dispenser_pours_milk_and_keeps_the_bucket",
                InfuserPourGameTest::dispenserPoursMilkAndKeepsTheBucket);
        register("retired_slots_hand_back_their_items", InfuserPourGameTest::retiredSlotsHandBackTheirItems);
        register("every_coloured_bong_is_a_bong", GlassBongGameTest::everyColouredBongIsABong);
        register("bong_stands_and_comes_back_whole", BongPlacementGameTest::bongStandsAndComesBackWhole);
        register("bong_is_drawn_not_clicked", BongRipGameTest::bongIsDrawnNotClicked);
        register("growth_multiplier_scales_the_odds", CropGrowthGameTest::growthMultiplierScalesTheOdds);
        register("the_plant_keeps_its_light_record", IndoorGrowGameTest::thePlantKeepsItsLightRecord);
        register("light_is_read_off_the_block", IndoorGrowGameTest::lightIsReadOffTheBlock);
        register("grow_lamp_lights_when_powered", IndoorGrowGameTest::growLampLightsWhenPowered);
        register("grow_pot_feeds_and_is_spent", IndoorGrowGameTest::growPotFeedsAndIsSpent);
        register("hydro_tray_runs_on_its_pump", IndoorGrowGameTest::hydroTrayRunsOnItsPump);
        register("a_water_bucket_fills_a_tray", IndoorGrowGameTest::aWaterBucketFillsATray);
        register("hydro_tray_spends_a_level_per_plant", IndoorGrowGameTest::hydroTraySpendsALevelPerPlant);
        register("tagged_fertiliser_feeds_both_beds", IndoorGrowGameTest::taggedFertiliserFeedsBothBeds);
        register("a_tray_holds_nothing_in_the_nether", IndoorGrowGameTest::aTrayHoldsNothingInTheNether);
        register("light_and_pot_change_the_harvest", IndoorGrowGameTest::lightAndPotChangeTheHarvest);
        register("beldia_grows_on_watered_sand", BeldiaGameTest::beldiaGrowsOnWateredSand);
        register("beldia_harvests_leafy_and_sifts_double", BeldiaGameTest::beldiaHarvestsLeafyAndSiftsDouble);
        register("beldia_smokes_a_mirage", BeldiaGameTest::beldiaSmokesAMirage);
        register("beldia_stays_in_the_desert", BeldiaGameTest::beldiaStaysInTheDesert);
        register("wild_beldia_grows_ripe", BeldiaGameTest::wildBeldiaGrowsRipe);
    }

    private static void register(String name, Consumer<TestContext> test) {
        Registry.register(Registries.TEST_FUNCTION, Identifier.of(NAMESPACE, name), test);
    }
}
