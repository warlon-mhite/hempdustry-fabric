package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.platform.Services;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;

/**
 * Since 26.1 vanilla's crop soil is two block tags: {@code #minecraft:supports_crops} says where a crop
 * may stand ({@code CropBlock#mayPlaceOn}) and {@code #minecraft:grows_crops}, with the soil's
 * {@code moisture}, how fast it grows ({@code CropBlock#getGrowthSpeed}). Both hemp crops ask vanilla
 * those two questions, so another mod's farmland that joins the tags grows hemp the way it grows wheat,
 * on either loader. NeoForge asks the soil's {@code canSustainPlant} first and falls back to the same
 * tags when the soil has no opinion.
 *
 * <p>The test datapack puts a moss block in both tags to stand in for that farmland; nothing the mod
 * ships does.
 */
public final class SoilTagsGameTest {
    private static final BlockPos SOIL = new BlockPos(1, 1, 1);
    private static final BlockPos CROP = SOIL.above();
    private static final BlockState OTHER_FARMLAND = Blocks.MOSS_BLOCK.defaultBlockState();

    /** A random whose every float is 0, so any growth roll with odds above zero lands. */
    private static final RandomSource HIT = new SingleThreadedRandomSource(0L) {
        @Override
        public float nextFloat() {
            return 0.0F;
        }
    };

    public static void hempTakesAnySoilInVanillasCropTags(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos crop = context.absolutePos(CROP);
        context.assertTrue(OTHER_FARMLAND.is(BlockTags.SUPPORTS_CROPS) && OTHER_FARMLAND.is(BlockTags.GROWS_CROPS),
                "the test datapack's moss block is not in vanilla's crop tags, so this test proves nothing");

        for (Block hemp : List.of(ModBlocks.INDICA_CROP, ModBlocks.SATIVA_CROP)) {
            BlockState seedling = Defoliation.unworked(hemp.defaultBlockState());

            context.setBlock(SOIL, OTHER_FARMLAND);
            context.assertTrue(seedling.canSurvive(world, crop),
                    hemp + " cannot stand on a soil in #minecraft:supports_crops");
            // 1 for the plant, 1 for dry crop soil under it (3 if it held water), nothing for the air around.
            float onTaggedSoil = Services.PLATFORM.cropGrowthSpeed(seedling, world, crop);
            context.assertTrue(onTaggedSoil == 2.0F,
                    hemp + " reads " + onTaggedSoil + " for a soil in #minecraft:grows_crops, not dry farmland's 2");

            // End to end: a seedling there grows on a random tick whose roll lands.
            context.setBlock(CROP, seedling);
            world.getBlockState(crop).randomTick(world, crop, HIT);
            int age = context.getBlockState(CROP).getValue(BlockStateProperties.AGE_7);
            context.assertTrue(age == 1, hemp + " on a soil in vanilla's crop tags did not grow: age " + age);
            context.setBlock(CROP, Blocks.AIR);

            // The negatives, so a rule that had become "any ground" cannot pass.
            context.setBlock(SOIL, Blocks.STONE);
            context.assertFalse(seedling.canSurvive(world, crop), hemp + " can stand on stone");
            context.setBlock(SOIL, Blocks.DIRT);
            float onDirt = Services.PLATFORM.cropGrowthSpeed(seedling, world, crop);
            context.assertTrue(onDirt == 1.0F, hemp + " reads " + onDirt + " for plain dirt, which is not crop soil");
        }
        context.succeed();
    }
}
