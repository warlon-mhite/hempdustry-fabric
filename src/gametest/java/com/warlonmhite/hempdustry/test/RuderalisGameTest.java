package com.warlonmhite.hempdustry.test;

import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.HempGrowth;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.util.ModTags;
import com.warlonmhite.hempdustry.world.ModConfiguredFeatures;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ruderalis: the one plant nothing accelerates, and the one strain that grows nowhere until it is dug
 * up. Every guard here fails in the generous direction and silently -- a crop that took bone meal, sat
 * on farmland or in a pot, kept a lamp record, paid out more with Fortune, or dropped its seeds from
 * tall grass would all look like working content.
 */
public final class RuderalisGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos CROP = SOIL.above();
    private static final int ROLLS = 40;
    private static final long SEED_SOURCE = 0x2D0E2A115L;
    /** Sniffer digs to sample: enough that a 25% pool firing at 10% or 50% is plainly out. */
    private static final int DIGS = 800;

    public static void ruderalisGrowsWhereTheSnifferDigs(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        CropBlock ruderalis = (CropBlock) ModBlocks.RUDERALIS_CROP;
        BlockPos crop = context.absolutePos(CROP);
        BlockState seedling = ruderalis.defaultBlockState();

        // Soil: every block the sniffer digs, and nothing else -- no farmland, no bed of ours.
        for (Block soil : new Block[]{Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.COARSE_DIRT,
                Blocks.ROOTED_DIRT, Blocks.MOSS_BLOCK, Blocks.PALE_MOSS_BLOCK, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS}) {
            context.setBlock(SOIL, soil);
            context.assertTrue(seedling.canSurvive(world, crop), "Ruderalis cannot be planted on " + soil);
        }
        for (Block soil : new Block[]{Blocks.FARMLAND, Blocks.MYCELIUM, Blocks.SAND, Blocks.GRAVEL,
                ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(SOIL, soil);
            context.assertTrue(!seedling.canSurvive(world, crop), "Ruderalis can be planted on " + soil);
        }

        // Planted through the real click: seeds on grass take, seeds on farmland stay in the hand.
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        context.setBlock(CROP, Blocks.AIR);
        context.setBlock(SOIL, Blocks.FARMLAND);
        ItemStack seeds = new ItemStack(ModItems.RUDERALIS_SEEDS, 2);
        plant(player, world, context.absolutePos(SOIL), seeds);
        context.assertTrue(context.getBlockState(CROP).isAir() && seeds.getCount() == 2,
                "Ruderalis seeds were planted in farmland");
        context.setBlock(SOIL, Blocks.GRASS_BLOCK);
        plant(player, world, context.absolutePos(SOIL), seeds);
        context.assertTrue(context.getBlockState(CROP).is(ModBlocks.RUDERALIS_CROP) && seeds.getCount() == 1,
                "Ruderalis seeds did not plant on grass");

        // Speed: 1-in-4, however it is packed -- there is no farmland under it to be crowded on.
        float odds = HempGrowth.chance(3.0F, 1.0F, 1.0);
        context.assertTrue(Math.abs(odds - 0.25F) < 1e-6, "Ruderalis grows at " + odds + " a tick, expected 1-in-4");
        int grown = 0;
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(4L);
        for (int i = 0; i < 4000; i++) {
            context.setBlock(CROP, seedling);
            world.getBlockState(crop).randomTick(world, crop, random);
            if (world.getBlockState(crop).getValue(CropBlock.AGE) == 1) {
                grown++;
            }
        }
        context.assertTrue(grown > 850 && grown < 1150, "Ruderalis grew on " + grown + " of 4000 random ticks, expected about 1000");

        // Nothing accelerates it: bone meal refused and kept, no bees, no light record at all.
        context.setBlock(CROP, seedling);
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 4);
        context.assertTrue(!BoneMealItem.growCrop(meal, world, crop), "Ruderalis took bone meal");
        context.assertTrue(meal.getCount() == 4, "Ruderalis ate the bone meal it refused");
        context.assertTrue(!seedling.is(BlockTags.BEE_GROWABLES), "bees can grow Ruderalis");
        context.assertTrue(!seedling.is(BlockTags.MAINTAINS_FARMLAND), "Ruderalis maintains farmland");
        context.assertTrue(!seedling.hasProperty(GrowLight.PROPERTY), "Ruderalis keeps a light record");
        context.assertTrue(seedling.is(ModTags.Blocks.HEMP_CROPS), "Ruderalis is not a hemp crop");

        // Shears do nothing to it: no trim, no rub, no leaf.
        BlockState ripe = seedling.setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        context.setBlock(CROP, ripe);
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        player.gameMode.useItemOn(player, world, shears, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(crop), Direction.NORTH, crop, false));
        context.assertTrue(context.getBlockState(CROP) == ripe, "shears changed a Ruderalis plant");
        context.assertTrue(!player.getInventory().contains(new ItemStack(ModItems.HEMP_LEAF))
                        && !player.getInventory().contains(new ItemStack(ModItems.CHARAS)),
                "shears took something off a Ruderalis plant");
        context.succeed();
    }

    public static void ruderalisHarvestsSeedHeavy(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Vec3 origin = Vec3.atCenterOf(context.absolutePos(CROP));
        LootTable table = world.getServer().reloadableRegistries()
                .getLootTable(ModBlocks.RUDERALIS_CROP.getLootTable().orElseThrow());
        BlockState seedling = ModBlocks.RUDERALIS_CROP.defaultBlockState();
        BlockState ripe = seedling.setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        ItemStack fortune = new ItemStack(Items.DIAMOND_HOE);
        fortune.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.FORTUNE), 3);

        // Flat, on every seed and with any tool: 1 bud, 4 seeds, 1 leaf, 1 stem. Unripe, one seed.
        java.util.Random seeds = new java.util.Random(SEED_SOURCE);
        for (int roll = 0; roll < ROLLS; roll++) {
            long seed = seeds.nextLong();
            for (ItemStack tool : List.of(ItemStack.EMPTY, fortune)) {
                int[] got = harvest(table, world, origin, ripe, tool, seed);
                context.assertTrue(got[0] == 1 && got[1] == 4 && got[2] == 1 && got[3] == 1,
                        "a ripe Ruderalis paid buds/seeds/leaves/stems " + got[0] + "/" + got[1] + "/" + got[2] + "/" + got[3]
                                + (tool.isEmpty() ? "" : " with Fortune III") + ", expected 1/4/1/1");
                int[] young = harvest(table, world, origin, seedling.setValue(CropBlock.AGE, 6), tool, seed);
                context.assertTrue(young[0] == 0 && young[1] == 1 && young[2] == 0 && young[3] == 0,
                        "an unripe Ruderalis paid " + young[0] + "/" + young[1] + "/" + young[2] + "/" + young[3]);
            }
        }

        // The oven takes its bud at half; Purple Kush's is the control.
        context.assertTrue(decarboxylates(context, ModItems.RUDERALIS_BUDS) == 2,
                "a Ruderalis bud decarboxylates to " + decarboxylates(context, ModItems.RUDERALIS_BUDS) + ", expected 2");
        context.assertTrue(decarboxylates(context, ModItems.INDICA_BUDS) == 4, "a Purple Kush bud no longer decarboxylates to 4");
        context.assertTrue(new ItemStack(ModItems.RUDERALIS_BUDS).is(ModTags.Items.SIFTABLE_FLOWER),
                "Ruderalis buds do not sift as flower");
        context.assertTrue(new ItemStack(ModItems.RUDERALIS_SEEDS).is(ModTags.Items.HEMP_SEEDS), "Ruderalis seeds are not hemp seeds");
        context.succeed();
    }

    public static void ruderalisSmokesLucky(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> ruderalis = world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY)
                .getOrThrow(ModStrains.RUDERALIS);
        Strain strain = ruderalis.value();
        context.assertTrue(strain.modelIndex() == 9, "Ruderalis wears art slot " + strain.modelIndex() + ", expected 9");
        context.assertTrue(strain.greenOutFactor() == 2.0F, "Ruderalis does not green you out at half the odds");
        context.assertTrue(strain.coughFactor() == 0.5F, "Ruderalis does not cough at twice the odds");

        // Nothing scales: a bong of three is the pipe of one.
        List<MobEffectInstance> three = strain.effects(3, DeviceStats.of(world.registryAccess(), DeviceType.BONG).durationTicks(), false,
                new java.util.Random(0L));
        context.assertTrue(three.size() == 2, "a Ruderalis hit applies " + three);
        for (MobEffectInstance effect : three) {
            context.assertTrue(effect.getAmplifier() == 0, "a bong of three Ruderalis gave " + effect);
            context.assertTrue(effect.getEffect().equals(MobEffects.LUCK) || effect.getEffect().equals(MobEffects.HUNGER),
                    "a Ruderalis hit gave " + effect);
        }

        // And a real pipe hit lands Luck for the pipe's whole duration.
        int pipeTicks = DeviceStats.of(world.registryAccess(), DeviceType.PIPE).durationTicks();
        ServerPlayer smoker = context.makeMockServerPlayerInLevel();
        smoker.setGameMode(GameType.SURVIVAL);
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        pipe.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(ruderalis, 1));
        pipe.set(ModComponents.CHARGES, DeviceStats.bowl(world.registryAccess(), DeviceType.PIPE).hits());
        smoker.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        smoker.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(ModItems.WOODEN_PIPE));
        smoker.gameMode.useItem(smoker, world, pipe, InteractionHand.MAIN_HAND);
        MobEffectInstance luck = smoker.getEffect(MobEffects.LUCK);
        context.assertTrue(luck != null && luck.getDuration() == pipeTicks && luck.getAmplifier() == 0,
                "a pipe of Ruderalis did not give " + pipeTicks + " ticks of Luck I: " + luck);
        context.succeed();
    }

    /** Only ever dug up: never in grass or a chest, one weight in the trail ruins, a pool of its own on a sniffer. */
    public static void ruderalisIsOnlyEverDugUp(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        String seeds = "hempdustry:ruderalis_seeds";
        expect(context, world, Blocks.TALL_GRASS.getLootTable().orElseThrow(), seeds, false, "hempdustry:sativa_seeds");
        expect(context, world, Blocks.LARGE_FERN.getLootTable().orElseThrow(), seeds, false, "hempdustry:sativa_seeds");
        // Not the mineshaft or the outpost: this world turns Villager Trade Rebalance on, whose feature
        // pack replaces both, so neither holds any of the mod's seeds here to be a control.
        for (ResourceKey<LootTable> chest : List.of(BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.SHIPWRECK_SUPPLY,
                BuiltInLootTables.WOODLAND_MANSION)) {
            expect(context, world, chest, seeds, false, "hempdustry:indica_seeds");
        }
        expect(context, world, BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_RARE, seeds, false, null);

        // Trail ruins: inside vanilla's one pool, beside its wheat seeds -- a brushed block keeps one item.
        var ruins = encode(world, BuiltInLootTables.TRAIL_RUINS_ARCHAEOLOGY_COMMON).getAsJsonObject().getAsJsonArray("pools");
        context.assertTrue(ruins.size() == 1, "the common trail ruins table has " + ruins.size() + " pools, expected vanilla's one");
        String pool = ruins.get(0).toString();
        context.assertTrue(pool.contains("\"" + seeds + "\"") && pool.contains("\"minecraft:wheat_seeds\""),
                "the trail ruins pool does not hold Ruderalis seeds beside wheat seeds");

        // The sniffer, through the real gift path its dig uses: vanilla's even split is still one of
        // two every dig, and a quarter of digs add one Ruderalis seed on top -- two stacks at once.
        Sniffer sniffer = EntityType.SNIFFER.create(world, EntitySpawnReason.COMMAND);
        context.assertTrue(sniffer != null, "no sniffer to dig with");
        sniffer.snapTo(context.absolutePos(CROP), 0.0F, 0.0F);
        int found = 0;
        for (int dig = 0; dig < DIGS; dig++) {
            int[] counts = new int[2];
            sniffer.dropFromGiftLootTable(world, BuiltInLootTables.SNIFFER_DIGGING, (w, stack) -> {
                if (stack.is(ModItems.RUDERALIS_SEEDS)) {
                    counts[0] += stack.getCount();
                } else if (stack.is(Items.TORCHFLOWER_SEEDS) || stack.is(Items.PITCHER_POD)) {
                    counts[1] += stack.getCount();
                }
            });
            context.assertTrue(counts[1] == 1, "a sniffer dug up " + counts[1] + " of vanilla's ancient seeds, expected exactly one");
            context.assertTrue(counts[0] <= 1, "a sniffer dug up " + counts[0] + " Ruderalis seeds at once");
            found += counts[0];
        }
        context.assertTrue(found > DIGS * 0.18 && found < DIGS * 0.32,
                "Ruderalis turned up on " + found + " of " + DIGS + " digs, expected about a quarter");
        context.succeed();
    }

    /**
     * The wild stand: a relict patch in the taigas, ripe, and only where the plant could live. It is
     * the one strain whose wild plant is a bonus rather than a source, so this guards the shape of
     * the patch rather than a rate — the rate is a rarity filter, measured in real terrain.
     */
    public static void wildRuderalisGrowsRipe(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                context.setBlock(new BlockPos(x, 1, z), x < 5 ? Blocks.PODZOL : Blocks.STONE);
                context.setBlock(new BlockPos(x, 2, z), Blocks.AIR);
            }
        }
        var feature = world.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(ModConfiguredFeatures.RUDERALIS_KEY).value();
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(9L);
        var generator = world.getChunkSource().getGenerator();

        // The soil filter, asked directly: a live world's shape updates pop a plant off stone at once,
        // and real chunk generation runs none, so counting blocks afterwards cannot see it.
        var plant = ((net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration) feature.config()).feature().value();
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(7, 2, 8))),
                "the wild Ruderalis feature accepted a spot on stone");
        context.assertTrue(plant.place(world, generator, random, context.absolutePos(new BlockPos(1, 2, 8))),
                "the wild Ruderalis feature refused a clear spot on podzol");
        // And it never writes over what is already standing there -- a taiga is full of ferns.
        context.setBlock(new BlockPos(2, 2, 8), Blocks.LARGE_FERN);
        context.assertTrue(!plant.place(world, generator, random, context.absolutePos(new BlockPos(2, 2, 8))),
                "the wild Ruderalis feature replaced a fern");
        context.setBlock(new BlockPos(1, 2, 8), Blocks.AIR);
        context.setBlock(new BlockPos(2, 2, 8), Blocks.AIR);

        for (int i = 0; i < 20; i++) {
            feature.place(world, generator, random, context.absolutePos(new BlockPos(4, 2, 4)));
        }
        int placed = 0;
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                BlockState state = context.getBlockState(new BlockPos(x, 2, z));
                if (!state.is(ModBlocks.RUDERALIS_CROP)) {
                    continue;
                }
                placed++;
                context.assertTrue(x < 5, "wild Ruderalis grew on stone at x " + x);
                context.assertTrue(state.getValue(CropBlock.AGE) == CropBlock.MAX_AGE,
                        "wild Ruderalis was placed unripe: " + state);
            }
        }
        context.assertTrue(placed > 0, "twenty Ruderalis patches on podzol placed nothing");
        context.succeed();
    }

    /**
     * The dark: a wild stand under a canopy, or a plant a player roofs over, <b>stops and waits</b>.
     * Vanilla's crop rule would break it off the block instead, and that is the half this overrides —
     * growth still wants light 9, which is the control here.
     */
    public static void ruderalisWaitsOutTheDark(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos crop = context.absolutePos(CROP);
        BlockState seedling = ModBlocks.RUDERALIS_CROP.defaultBlockState();
        context.setBlock(SOIL, Blocks.PODZOL);
        context.setBlock(CROP, seedling);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= 3; dy++) {
                    context.setBlock(CROP.offset(dx, dy, dz), Blocks.STONE);
                }
                if (dx != 0 || dz != 0) {
                    context.setBlock(CROP.offset(dx, 0, dz), Blocks.STONE);
                }
            }
        }
        context.runAtTickTime(20, () -> {
            context.assertTrue(world.getRawBrightness(crop, 0) < 8,
                    "the sealed box is lit at " + world.getRawBrightness(crop, 0));
            // Purple Kush is the control: vanilla's rule breaks it off in the same box.
            context.assertTrue(!ModBlocks.INDICA_CROP.defaultBlockState().canSurvive(world, crop),
                    "Purple Kush survives the dark — the control proves nothing");
            context.assertTrue(seedling.canSurvive(world, crop), "Ruderalis broke off in the dark");
            context.assertTrue(context.getBlockState(CROP).is(ModBlocks.RUDERALIS_CROP),
                    "the Ruderalis plant did not survive being sealed in");
            // It waits, though: no light, no growth, however long it stands there.
            for (int i = 0; i < 2000; i++) {
                world.getBlockState(crop).randomTick(world, crop, world.getRandom());
            }
            context.assertTrue(world.getBlockState(crop).getValue(CropBlock.AGE) == 0,
                    "Ruderalis grew in the dark, at age " + world.getBlockState(crop).getValue(CropBlock.AGE));
            context.succeed();
        });
    }

    // ----- helpers -----

    private static void plant(ServerPlayer player, ServerLevel world, BlockPos soil, ItemStack seeds) {
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        player.gameMode.useItemOn(player, world, seeds, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false));
    }

    /**
     * {buds, seeds, leaves, stems} from one roll. A bud that went to schwag is still the bud the plant
     * grew, so it counts as one; {@code SchwagGameTest} owns the split.
     */
    private static int[] harvest(LootTable table, ServerLevel world, Vec3 origin, BlockState state, ItemStack tool, long seed) {
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        int[] counts = new int[4];
        for (ItemStack stack : table.getRandomItems(params, seed)) {
            int slot = stack.is(ModItems.RUDERALIS_BUDS) || stack.is(ModItems.SCHWAG) ? 0
                    : stack.is(ModItems.RUDERALIS_SEEDS) ? 1
                    : stack.is(ModItems.HEMP_LEAF) ? 2 : stack.is(ModItems.HEMP_STEM) ? 3 : -1;
            if (slot < 0) {
                throw new AssertionError("a Ruderalis harvest dropped " + stack);
            }
            counts[slot] += stack.getCount();
        }
        return counts;
    }

    private static int decarboxylates(GameTestHelper context, Item input) {
        SingleRecipeInput one = new SingleRecipeInput(new ItemStack(input));
        return context.getLevel().recipeAccess()
                .getRecipeFor(ModRecipes.DECARBOXYLATING_TYPE, one, context.getLevel())
                .map(entry -> entry.value().assemble(one, context.getLevel().registryAccess()).getCount())
                .orElse(0);
    }

    private static com.google.gson.JsonElement encode(ServerLevel world, ResourceKey<LootTable> key) {
        LootTable table = world.getServer().reloadableRegistries().getLootTable(key);
        return LootTable.DIRECT_CODEC.encodeStart(world.registryAccess().createSerializationContext(JsonOps.INSTANCE), table).getOrThrow();
    }

    /** As in WarpedKushGameTest: the encoded table names {@code item}, or does not, beside a control that it must. */
    private static void expect(GameTestHelper context, ServerLevel world, ResourceKey<LootTable> key, String item,
                               boolean present, String control) {
        String json = encode(world, key).toString();
        context.assertTrue(json.contains("\"" + item + "\"") == present,
                key.identifier() + (present ? " does not name " : " names ") + item);
        if (control != null) {
            context.assertTrue(json.contains("\"" + control + "\""), key.identifier() + " does not name its control " + control);
        }
    }
}
