package com.warlonmhite.hempdustry.world.gen;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.util.ModTags;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Wild Beldía: a patch of the ripe crop, both halves at once, on sand with water beside it.
 *
 * <p>The newer lines build this from vanilla parts — a random patch of 20 tries within 4 blocks, all on
 * the centre's level; a block column that puts the LOWER and the UPPER down together; a filter wanting
 * air for both, {@code #hempdustry:beldia_soil} below, water on one of vanilla's four sides of the
 * ground (sugar cane's own test), and each plant in a Beldía biome. Minecraft 1.16.5 has no block
 * column and no filter per plant, so this feature asks the same questions in the same order.
 *
 * <p>The ground test is the plant's own survival rule, so a modded desert's sands work, and it is
 * asked directly rather than through {@code canSurvive}, because a crop's also reads light and
 * worldgen has none yet. <b>The biome is asked for each plant</b>, not only the patch's centre: a patch
 * spreads four blocks, and without it a plant could land across a desert's border — on the beach
 * beside it, which is the one place Beldía is kept off. A desert that runs straight into the sea is a
 * desert, and grows it on the shore (Warlon Mhite's call, 2026-09-17). The biome is read from its exact
 * 4×4×4 cell rather than through {@code getBiome}, whose zoom jitter can answer for a neighbour at a
 * border.
 */
public class BeldiaPatchFeature extends Feature<NoneFeatureConfiguration> {

    /** Vanilla sugar cane's patch shape: 20 tries within 4 blocks, all on the centre's level. */
    private static final int TRIES = 20;
    private static final int SPREAD = 4;

    public BeldiaPatchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, Random random, BlockPos origin,
                         NoneFeatureConfiguration config) {
        BlockState ripe = ModBlocks.BELDIA_CROP.defaultBlockState().setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE);
        BlockState lower = ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upper = ripe.setValue(IndicaCropBlock.HALF, DoubleBlockHalf.UPPER);
        int placed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < TRIES; i++) {
            pos.setWithOffset(origin, random.nextInt(SPREAD + 1) - random.nextInt(SPREAD + 1), 0,
                    random.nextInt(SPREAD + 1) - random.nextInt(SPREAD + 1));
            if (canGrowAt(level, pos)) {
                // Flags 2, as vanilla's column: no neighbour updates while the chunk is being built.
                level.setBlock(pos, lower, 2);
                level.setBlock(pos.above(), upper, 2);
                placed++;
            }
        }
        return placed > 0;
    }

    private static boolean canGrowAt(WorldGenLevel level, BlockPos pos) {
        BlockPos ground = pos.below();
        return level.getBlockState(pos).is(Blocks.AIR)
                && level.getBlockState(pos.above()).is(Blocks.AIR)
                && level.getBlockState(ground).is(ModTags.Blocks.BELDIA_SOIL)
                && (isWater(level, ground.east()) || isWater(level, ground.west())
                        || isWater(level, ground.south()) || isWater(level, ground.north()))
                && inBeldiaBiome(level, pos);
    }

    private static boolean isWater(WorldGenLevel level, BlockPos pos) {
        Fluid fluid = level.getFluidState(pos).getType();
        return fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER;
    }

    private static boolean inBeldiaBiome(WorldGenLevel level, BlockPos pos) {
        Biome biome = level.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2);
        ResourceLocation id = level.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(biome);
        return id != null && ModTags.Biomes.BELDIA_GEN.test(id, biome.getBiomeCategory());
    }
}
