package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.PlantStress;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Industrial Hemp: a fibre crop that must stay one. Every guard here fails in the generous
 * direction and silently — a trimmable plant, a bud in the harvest, a field that crowds like the
 * others or a pot that takes it would all look like working content.
 */
public final class IndustrialHempGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos CROP = SOIL.above();
    private static final int ROLLS = 60;
    private static final long SEED_SOURCE = 0x1DB5EEDL;

    public static void industrialHempIsAFieldCrop(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos crop = context.absolutePos(CROP);

        // Crowding: a plant in the middle of a moist 3x3 of its own kind reads full moisture, where
        // Lemon Haze in the same field is halved.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                context.setBlock(SOIL.offset(dx, 0, dz),
                        Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE));
            }
        }
        context.setBlock(CROP.east(2), Blocks.GLOWSTONE);
        float alone = fieldMoisture(context, ModBlocks.INDUSTRIAL_HEMP_CROP, false);
        float packed = fieldMoisture(context, ModBlocks.INDUSTRIAL_HEMP_CROP, true);
        context.assertTrue(alone == 10.0F, "a lone Industrial Hemp plant on moist farmland reads moisture " + alone);
        context.assertTrue(packed == alone, "Industrial Hemp in a packed field reads moisture " + packed
                + " against " + alone + " alone — it is sown dense and must not be crowded");
        float sativaPacked = fieldMoisture(context, ModBlocks.SATIVA_CROP, true);
        context.assertTrue(sativaPacked == alone / 2, "Lemon Haze in a packed field reads " + sativaPacked
                + " — the control lost its crowding penalty");

        // Farmland only: no bed of ours takes it.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                context.setBlock(CROP.offset(dx, 0, dz), Blocks.AIR);
            }
        }
        BlockState seedling = ModBlocks.INDUSTRIAL_HEMP_CROP.defaultBlockState();
        context.assertTrue(seedling.canSurvive(world, crop), "Industrial Hemp cannot be planted on farmland");
        context.setBlock(SOIL, ModBlocks.GROW_POT);
        context.assertTrue(!seedling.canSurvive(world, crop), "Industrial Hemp can be planted in a Grow Pot");
        context.setBlock(SOIL, ModBlocks.HYDRO_TRAY);
        context.assertTrue(!seedling.canSurvive(world, crop), "Industrial Hemp can be planted in a Hydro Tray");
        context.setBlock(SOIL, Blocks.FARMLAND);

        // Shears: nothing to trim, nothing to rub. The click falls through, no flag is set, no leaf drops.
        Player player = context.makeMockPlayer(GameType.SURVIVAL);
        AABB area = AABB.encapsulatingFullBlocks(crop.offset(-3, -2, -3), crop.offset(3, 4, 3));
        for (int age : new int[]{Defoliation.EARLY_MIN_AGE, 5, SativaCropBlock.MAX_AGE}) {
            world.getEntitiesOfClass(ItemEntity.class, area, e -> true).forEach(Entity::discard);
            context.setBlock(CROP, seedling.setValue(SativaCropBlock.AGE, age));
            ItemStack shears = new ItemStack(Items.SHEARS);
            player.setItemInHand(InteractionHand.MAIN_HAND, shears);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false);
            InteractionResult result = world.getBlockState(crop).useItemOn(shears, world, player, InteractionHand.MAIN_HAND, hit);
            BlockState after = world.getBlockState(crop);
            context.assertTrue(!result.consumesAction(), "shears were accepted by Industrial Hemp at age " + age);
            context.assertTrue(after == seedling.setValue(SativaCropBlock.AGE, age),
                    "shears changed Industrial Hemp's state at age " + age + ": " + after);
            context.assertTrue(world.getEntitiesOfClass(ItemEntity.class, area, e -> true).isEmpty(),
                    "shears took something off Industrial Hemp at age " + age);
        }
        context.succeed();
    }

    public static void industrialHempHarvestsStemsNotBuds(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(CROP));
        LootTable table = world.getServer().reloadableRegistries()
                .getLootTable(ModBlocks.INDUSTRIAL_HEMP_CROP.getLootTable().orElseThrow());
        BlockState ripe = ModBlocks.INDUSTRIAL_HEMP_CROP.defaultBlockState()
                .setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                .setValue(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE);
        ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
        hoe.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE), 3);

        java.util.Random seeds = new java.util.Random(SEED_SOURCE);
        int stems = 0, fortuneStems = 0, leafPaid = 0, fortuneLeaves = 0, maxFortuneStems = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            int[] plain = harvest(context, table, world, origin, ripe, ItemStack.EMPTY, seed);
            int[] fortune = harvest(context, table, world, origin, ripe, hoe, seed);
            context.assertTrue(plain[1] >= 5 && plain[1] <= 8, "a ripe plant dropped " + plain[1] + " stems, expected 5-8");
            context.assertTrue(plain[2] <= 1 && fortune[2] <= 1, "a ripe plant dropped more than one leaf");
            context.assertTrue(plain[3] >= 1 && plain[3] <= 4, "a ripe plant dropped " + plain[3] + " seeds, expected 1-4");
            context.assertTrue(fortune[1] >= 5 && fortune[1] <= 11, "Fortune III dropped " + fortune[1] + " stems, expected 5-11");
            stems += plain[1];
            fortuneStems += fortune[1];
            maxFortuneStems = Math.max(maxFortuneStems, fortune[1]);
            leafPaid += plain[2];
            fortuneLeaves += fortune[2];
        }
        context.assertTrue(leafPaid > 0 && leafPaid < ROLLS,
                "the leaf paid " + leafPaid + " times in " + ROLLS + " — it has to be a coin flip");
        context.assertTrue(fortuneStems > stems && maxFortuneStems > 8,
                "Fortune III did not raise the stems: " + fortuneStems + " against " + stems + " (best " + maxFortuneStems + ")");
        context.assertTrue(fortuneLeaves < ROLLS * 3 / 4,
                "Fortune III paid " + fortuneLeaves + " leaves in " + ROLLS + " — Fortune must not touch the leaf");

        // Unripe: one seed back and nothing else. Any segment but the LOWER: nothing at all.
        int[] unripe = harvest(context, table, world, origin, ripe.setValue(SativaCropBlock.AGE, 3), ItemStack.EMPTY, 1L);
        context.assertTrue(unripe[1] == 0 && unripe[2] == 0 && unripe[3] == 1 && unripe[4] == 0,
                "an unripe plant dropped stems/leaves/seeds/other " + unripe[1] + "/" + unripe[2] + "/" + unripe[3] + "/" + unripe[4]);
        int[] upper = harvest(context, table, world, origin,
                ripe.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.UPPER), ItemStack.EMPTY, 1L);
        context.assertTrue(upper[1] + upper[2] + upper[3] + upper[4] == 0, "the UPPER segment rolled loot of its own");
        context.succeed();
    }

    /**
     * Schwag is the fibre crop's only flower, so it is an extra drop read off the light record, which
     * Fortune never touches: always under a Grow Lamp, a roll under a lamp, glowstone or the sun. And
     * the plant is never stressed — not by a lost lamp, not by bone-dry ground, not by a bee — with
     * Lemon Haze in the same spot as the control. Ditchweed gives schwag when broken, never lifted.
     */
    public static void industrialHempGivesSchwagAndIsNeverStressed(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(CROP));
        LootTable table = world.getServer().reloadableRegistries()
                .getLootTable(ModBlocks.INDUSTRIAL_HEMP_CROP.getLootTable().orElseThrow());
        BlockState ripe = ModBlocks.INDUSTRIAL_HEMP_CROP.defaultBlockState()
                .setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER)
                .setValue(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE);
        ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
        hoe.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE), 3);
        java.util.Random seeds = new java.util.Random(SEED_SOURCE);
        int[] paid = new int[GrowLight.values().length];
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            for (GrowLight light : new GrowLight[]{GrowLight.NATURAL, GrowLight.AMBIENT, GrowLight.LAMP, GrowLight.GROW_LAMP}) {
                int schwag = harvest(context, table, world, origin, ripe.setValue(GrowLight.PROPERTY, light), ItemStack.EMPTY, seed)[4];
                int fortune = harvest(context, table, world, origin, ripe.setValue(GrowLight.PROPERTY, light), hoe, seed)[4];
                context.assertTrue(schwag <= 1 && fortune <= 1, "a " + light + " plant dropped " + schwag + "/" + fortune
                        + " schwag — one at most, and Fortune never adds to it");
                if (light == GrowLight.GROW_LAMP) {
                    context.assertTrue(schwag == 1 && fortune == 1, "a Grow Lamp plant dropped no schwag");
                }
                paid[light.ordinal()] += schwag;
            }
        }
        for (GrowLight light : new GrowLight[]{GrowLight.NATURAL, GrowLight.AMBIENT, GrowLight.LAMP}) {
            context.assertTrue(paid[light.ordinal()] > 0 && paid[light.ordinal()] < ROLLS, "a " + light
                    + " plant paid schwag " + paid[light.ordinal()] + " times in " + ROLLS + " — it has to be a roll");
        }
        context.assertTrue(paid[GrowLight.NATURAL.ordinal()] < paid[GrowLight.LAMP.ordinal()],
                "the sun paid as much schwag as a lamp: " + paid[GrowLight.NATURAL.ordinal()] + " against "
                        + paid[GrowLight.LAMP.ordinal()] + " (one in four against one in two)");

        // Never stressed. Each step is decided to hit, and Lemon Haze takes the same step as the control.
        BlockPos crop = context.absolutePos(CROP);
        for (net.minecraft.world.level.block.Block block : new net.minecraft.world.level.block.Block[]{ModBlocks.SATIVA_CROP, ModBlocks.INDUSTRIAL_HEMP_CROP}) {
            boolean control = block == ModBlocks.SATIVA_CROP;
            GrowLight dry = stepOn(context, block, Blocks.FARMLAND.defaultBlockState(), GrowLight.NATURAL);
            GrowLight lampLost = stepOn(context, block, moist(), GrowLight.GROW_LAMP);
            BlockState current = world.getBlockState(crop).setValue(GrowLight.PROPERTY, GrowLight.NATURAL);
            context.setBlock(SOIL, Blocks.FARMLAND);
            BlockState bee = PlantStress.beeStep(world, crop, current,
                    ((CropBlock) block).getStateForAge(current.getValue(SativaCropBlock.AGE) + 1), HIT);
            GrowLight expected = control ? GrowLight.STRESSED : GrowLight.NATURAL;
            String what = control ? "Lemon Haze (the control)" : "Industrial Hemp";
            context.assertTrue(dry == expected, what + " flowering on bone-dry farmland was left " + dry);
            context.assertTrue(lampLost == expected, what + " losing its Grow Lamp while flowering was left " + lampLost);
            context.assertTrue(bee.getValue(GrowLight.PROPERTY) == expected, what + " grown by a bee on bone-dry farmland was left "
                    + bee.getValue(GrowLight.PROPERTY));
        }

        // Ditchweed: schwag when broken, one time in four; none when lifted with shears.
        LootTable ditchweed = world.getServer().reloadableRegistries()
                .getLootTable(ModBlocks.DITCHWEED.getLootTable().orElseThrow());
        BlockState lower = ModBlocks.DITCHWEED.defaultBlockState();
        int broken = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            broken += count(ditchweed, world, origin, lower, ItemStack.EMPTY, seed);
            context.assertTrue(count(ditchweed, world, origin, lower, new ItemStack(Items.SHEARS), seed) == 0,
                    "Ditchweed lifted with shears dropped schwag");
            context.assertTrue(count(ditchweed, world, origin,
                    lower.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),
                    ItemStack.EMPTY, seed) == 0, "the top half of Ditchweed dropped schwag — it would pay twice");
        }
        context.assertTrue(broken > 0 && broken < ROLLS / 2, "broken Ditchweed gave schwag " + broken + " times in "
                + ROLLS + ", expected about a quarter");
        context.succeed();
    }

    private static final net.minecraft.util.RandomSource HIT = new net.minecraft.world.level.levelgen.SingleThreadedRandomSource(0L) {
        @Override
        public float nextFloat() {
            return 0.0F;
        }
    };

    private static BlockState moist() {
        return Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
    }

    /**
     * One bone meal step from age 4 on {@code soil}, the roll decided to hit. With {@code record} a Grow
     * Lamp's, no lamp hangs overhead, so the step also loses the light.
     */
    private static GrowLight stepOn(GameTestHelper context, net.minecraft.world.level.block.Block block, BlockState soil, GrowLight record) {
        for (int y = 3; y >= 1; y--) {
            context.setBlock(CROP.above(y), Blocks.AIR);
        }
        context.setBlock(SOIL, soil);
        context.setBlock(CROP, block.defaultBlockState().setValue(SativaCropBlock.AGE, GrowLight.FLOWERING_AGE)
                .setValue(GrowLight.PROPERTY, record));
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(CROP);
        ((CropBlock) block).performBonemeal(world, HIT, pos, world.getBlockState(pos));
        return world.getBlockState(pos).getValue(GrowLight.PROPERTY);
    }

    private static int count(LootTable table, ServerLevel world, Vec3 origin, BlockState state, ItemStack tool, long seed) {
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        int schwag = 0;
        for (ItemStack stack : table.getRandomItems(params, seed)) {
            if (stack.is(ModItems.SCHWAG)) {
                schwag += stack.getCount();
            }
        }
        return schwag;
    }

    /**
     * The Ditchweed patch places, and only beside water. Sugar cane's filter on a two-block plant is
     * new ground for this mod, and a patch whose predicate never passes generates nothing, silently.
     */
    public static void ditchweedGrowsBesideWater(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        // A 9x9 of grass at y 1 with a water channel down the middle, open air above.
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                context.setBlock(new BlockPos(x, 1, z), x == 4 ? Blocks.WATER : Blocks.GRASS_BLOCK);
                context.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
                context.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
            }
        }
        var feature = world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(com.warlonmhite.hempdustry.world.ModConfiguredFeatures.DITCHWEED_KEY).value();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(42L);
        for (int i = 0; i < 20; i++) {
            feature.place(world, world.getChunkSource().getGenerator(), random, context.absolutePos(new BlockPos(4, 2, 4)));
        }
        int placed = 0;
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                BlockState state = context.getBlockState(new BlockPos(x, 2, z));
                if (state.is(ModBlocks.DITCHWEED)) {
                    placed++;
                    context.assertTrue(x == 3 || x == 5, "Ditchweed grew at x " + x + ", away from the water");
                    context.assertTrue(context.getBlockState(new BlockPos(x, 3, z)).is(ModBlocks.DITCHWEED),
                            "Ditchweed was placed without its top half");
                }
            }
        }
        context.assertTrue(placed > 0, "twenty Ditchweed patches beside a river placed nothing");
        context.succeed();
    }

    /** The moisture Industrial Hemp or {@code block} reads at CROP, alone or with its own kind all round. */
    private static float fieldMoisture(GameTestHelper context, net.minecraft.world.level.block.Block block, boolean packed) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                context.setBlock(CROP.offset(dx, 0, dz),
                        (dx == 0 && dz == 0) || packed ? block.defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
        return ((SativaCropBlock) block).fieldMoisture(context.getLevel(), context.absolutePos(CROP));
    }

    /**
     * {buds, stems, leaves, seeds, anything else} from one roll. "Buds" is any item whose id ends in
     * {@code _buds}, so a bud of any strain — or of a strain added later — fails the fibre crop.
     */
    private static int[] harvest(GameTestHelper context, LootTable table, ServerLevel world, Vec3 origin,
                                 BlockState state, ItemStack tool, long seed) {
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        int[] counts = new int[5];
        for (ItemStack stack : table.getRandomItems(params, seed)) {
            if (stack.getItemHolder().getRegisteredName().endsWith("_buds")) {
                counts[0] += stack.getCount();
            } else if (stack.is(ModItems.HEMP_STEM)) {
                counts[1] += stack.getCount();
            } else if (stack.is(ModItems.HEMP_LEAF)) {
                counts[2] += stack.getCount();
            } else if (stack.is(ModItems.INDUSTRIAL_HEMP_SEEDS)) {
                counts[3] += stack.getCount();
            } else {
                counts[4] += stack.getCount();
            }
        }
        context.assertTrue(counts[0] == 0, "Industrial Hemp dropped " + counts[0] + " buds");
        return counts;
    }
}
