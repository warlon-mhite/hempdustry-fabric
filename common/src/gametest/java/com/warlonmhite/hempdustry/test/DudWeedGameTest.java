package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.item.ItemEntity;
import java.util.ArrayList;
import com.mojang.serialization.JsonOps;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowLight;
import com.warlonmhite.hempdustry.block.custom.IndicaCropBlock;
import com.warlonmhite.hempdustry.block.custom.Mildew;
import com.warlonmhite.hempdustry.block.custom.SativaCropBlock;
import com.warlonmhite.hempdustry.block.custom.TriplePlantSegment;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.recipe.ModRecipes;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Potency;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.strain.Terpene;
import com.warlonmhite.hempdustry.util.ModTags;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Dud Weed, the cannabinoid-free plant: Purple Kush's body on farmland, the seed crop's harvest, a
 * smoke with nothing in it, found as a sport of ripe Industrial Hemp and in shipwrecks, and a joke
 * advancement for smoking it anyway. Each guard fails quietly: a seed that went down on grass, a
 * harvest one leaf too generous, a Hunger from a plant with no THC, an oven that bakes it into butter
 * that does nothing, a sport that never fires or fires under Fortune, its seeds in every meadow, a
 * potency line that contradicts the chemotype above it.
 */
public final class DudWeedGameTest {
    private static final BlockPos SOIL = new BlockPos(2, 1, 2);
    private static final BlockPos CROP = SOIL.above();
    private static final long SEED_SOURCE = 0xD0D5EEDL;

    /** Farmland and a bed take its seed, grass and dirt do not; it keeps the light and mildew records. */
    public static void dudWeedPlantsOnFarmland(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos soil = context.absolutePos(SOIL);
        BlockPos crop = context.absolutePos(CROP);
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                context.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                context.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
            }
        }
        context.setBlock(SOIL.east(2), Blocks.WATER);

        ServerPlayer player = Players.survival(context);
        ItemStack seeds = new ItemStack(ModItems.DUD_WEED_SEEDS, 8);
        for (Block ok : new Block[]{Blocks.FARMLAND, ModBlocks.GROW_POT, ModBlocks.HYDRO_TRAY}) {
            context.setBlock(SOIL, ok);
            int before = seeds.getCount();
            plant(player, world, soil, seeds);
            context.assertTrue(world.getBlockState(crop).is(ModBlocks.DUD_WEED_CROP), "a seed would not go down on " + ok);
            context.assertValueEqual(seeds.getCount(), before - 1, "seeds left after planting on " + ok);
            context.setBlock(CROP, Blocks.AIR);
        }
        for (Block no : new Block[]{Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL, Blocks.SAND}) {
            context.setBlock(SOIL, no);
            int before = seeds.getCount();
            plant(player, world, soil, seeds);
            context.assertTrue(!world.getBlockState(crop).is(ModBlocks.DUD_WEED_CROP), "a seed went down on " + no);
            context.assertValueEqual(seeds.getCount(), before, "seeds left after " + no + " refused one");
        }

        BlockState seedling = ModBlocks.DUD_WEED_CROP.defaultBlockState();
        context.assertTrue(seedling.hasProperty(GrowLight.PROPERTY),
                "Dud Weed keeps no light record, so its light gene would mean nothing");
        context.assertTrue(ModBlocks.DUD_WEED_CROP instanceof Mildew.Host host && host.catchesMildew(),
                "Dud Weed does not catch Powdery Mildew, so its mildew gene would mean nothing");

        // No wild plant: the strain's "flower" is its own crop, which nothing generates.
        Strain dud = strain(world, ModStrains.DUD_WEED).value();
        context.assertTrue(dud.wildFlower().filter(f -> f == ModBlocks.DUD_WEED_CROP).isPresent(),
                "Dud Weed's wild flower is " + dud.wildFlower());
        context.assertTrue(world.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).listElementIds()
                        .noneMatch(key -> key.identifier().getPath().contains("dud_weed")),
                "a placed feature grows Dud Weed wild");
        context.succeed();
    }

    /**
     * The seed crop's harvest: 2 buds, 5 seeds, 3 leaves, 2 stems untouched; worked all three ways, 4
     * buds and no leaves, as many leaves in all as untouched. Each count is a base plus Fortune's
     * binomial, which rolls even with no Fortune (vanilla's wheat-seed rule: n = level + extra), so
     * over many harvests the least is the base and the most the base plus the extra. Purple Kush
     * beside it keeps its 3 seeds and 5 leaves.
     */
    public static void dudWeedHarvestsSeeds(GameTestHelper context) {
        // Grown in the sun: a lit record adds the light ladder's buds on top.
        BlockState ripe = Defoliation.unworked(ModBlocks.DUD_WEED_CROP.defaultBlockState()
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER).setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE))
                .setValue(GrowLight.PROPERTY, GrowLight.NATURAL);
        context.setBlock(SOIL, Blocks.FARMLAND);
        // {buds and schwag, seeds, leaves, stems}: base, extra
        expectRange(context, "an untouched Dud Weed", ripe, new int[][]{{2, 2}, {5, 3}, {3, 3}, {2, 3}});
        BlockState worked = ripe.setValue(Defoliation.TRIMMED_EARLY, true).setValue(Defoliation.TRIMMED_LATE, true)
                .setValue(Defoliation.RUBBED, true);
        // Its leaves' base is 0: the three it gave while growing came back off its harvest, or it hands
        // a free one over.
        expectRange(context, "a Dud Weed trimmed twice and rubbed", worked, new int[][]{{4, 2}, {5, 3}, {0, 3}, {2, 3}});
        BlockState kush = Defoliation.unworked(ModBlocks.INDICA_CROP.defaultBlockState()
                .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER).setValue(IndicaCropBlock.AGE, IndicaCropBlock.MAX_AGE))
                .setValue(GrowLight.PROPERTY, GrowLight.NATURAL);
        expectRange(context, "an untouched Purple Kush", kush, new int[][]{{2, 2}, {3, 3}, {5, 3}, {2, 3}});
        context.succeed();
    }

    private static void expectRange(GameTestHelper context, String what, BlockState state, int[][] want) {
        String[] names = {"buds", "seeds", "leaves", "stems"};
        int[] least = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE};
        int[] most = new int[4];
        Random seeds = new Random(SEED_SOURCE);
        for (int i = 0; i < 400; i++) {
            int[] got = harvest(context, state, seeds.nextLong(), 0);
            int[] four = {got[0] + got[4], got[1], got[2], got[3]};
            for (int k = 0; k < 4; k++) {
                least[k] = Math.min(least[k], four[k]);
                most[k] = Math.max(most[k], four[k]);
            }
        }
        for (int k = 0; k < 4; k++) {
            context.assertTrue(least[k] == want[k][0] && most[k] > want[k][0] && most[k] <= want[k][0] + want[k][1],
                    what + "'s " + names[k] + " ran " + least[k] + ".." + most[k] + " over 400 harvests, expected "
                            + want[k][0] + " plus up to " + want[k][1]);
        }
    }

    /**
     * Type V in play: a bowl of it is THC dose 0 and grants nothing, the oven refuses its bud and no
     * oven page exists for it, its kief presses into stamped hash that smokes at dose 0, and its
     * schwag keeps Poison and loses Hunger.
     */
    public static void dudWeedSmokesNothing(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Holder<Strain> dud = strain(world, ModStrains.DUD_WEED);
        context.assertTrue(dud.value().chemotype() == Chemotype.CANNABINOID_FREE, "Dud Weed is not cannabinoid-free");
        context.assertValueEqual(dud.value().terpene(), Optional.of(Terpene.HUMULENE), "Dud Weed's terpene");
        context.assertValueEqual(dud.value().potency(), Potency.odds(60, 40, 0, 0), "Dud Weed's potency odds");
        context.assertValueEqual(SmokeContents.of(dud, 3).thcDose(), 0, "a bong of Dud Weed's THC dose");

        ServerPlayer smoker = Players.survival(context);
        smoke(smoker, world, dud);
        context.assertTrue(smoker.getActiveEffects().isEmpty(), "a Dud Weed hit gave " + smoker.getActiveEffects());

        ItemStack bud = new ItemStack(ModItems.DUD_WEED_BUDS);
        context.assertTrue(Chemotype.ofMatter(bud, world.registryAccess()) == Chemotype.CANNABINOID_FREE,
                "a Dud Weed bud reads " + Chemotype.ofMatter(bud, world.registryAccess()) + " to a machine");
        context.assertTrue(DecarboxylatorBlockEntity.resultFor(world, bud).isEmpty(), "the oven baked a Dud Weed bud");
        context.assertTrue(ModRecipes.allOfType(world, ModRecipes.DECARBOXYLATING_TYPE).stream()
                        .noneMatch(entry -> entry.value().ingredient().test(bud)),
                "an oven recipe takes Dud Weed buds, a page in a viewer that the oven refuses");
        context.assertTrue(world.recipeAccess().getRecipeFor(ModRecipes.DECARBOXYLATING_TYPE, new SingleRecipeInput(
                        new ItemStack(ModItems.INDICA_BUDS)), world).isPresent(), "the control, Purple Kush, has no oven recipe");
        context.assertValueEqual(SmokeContents.of(strain(world, ModStrains.HASHISH), 2, Chemotype.CANNABINOID_FREE).thcDose(), 0,
                "cannabinoid-free hash's THC dose");

        // Hash or scorched hemp stamped cannabinoid-free has none of its strain's own effects, where the
        // THC-dominant one has them; nothing makes such hash, but a command or an older world can.
        java.util.random.RandomGenerator always = () -> 0L;
        for (ResourceKey<Strain> key : List.of(ModStrains.HASHISH, ModStrains.CHARAS, ModStrains.SCORCHED_HEMP)) {
            Strain resin = strain(world, key).value();
            List<MobEffectInstance> typeV = new ArrayList<>(resin.effects(1, Chemotype.CANNABINOID_FREE, 700, false, 0, always));
            typeV.addAll(resin.effects(1, Chemotype.CANNABINOID_FREE, 700, true, 0, always));
            context.assertTrue(typeV.isEmpty(), "cannabinoid-free " + key.identifier().getPath() + " gave " + typeV);
            context.assertTrue(!resin.effects(1, Chemotype.THC_DOMINANT, 700, false, 0, always).isEmpty()
                            || !resin.effects(1, Chemotype.THC_DOMINANT, 700, true, 0, always).isEmpty(),
                    "the control, THC-dominant " + key.identifier().getPath() + ", gave nothing");
        }

        // Its schwag: Poison is the smoke's own and stays, Hunger is the THC's and goes.
        Strain schwag = strain(world, ModStrains.SCHWAG).value();
        List<MobEffectInstance> dudSchwag = schwag.effects(1, Chemotype.CANNABINOID_FREE, 700, false, 0, always);
        context.assertTrue(dudSchwag.stream().anyMatch(e -> e.is(MobEffects.POISON)), "Dud Weed's schwag lost its Poison");
        context.assertTrue(dudSchwag.stream().noneMatch(e -> e.is(MobEffects.HUNGER)), "Dud Weed's schwag gave Hunger");
        context.assertTrue(schwag.effects(1, Chemotype.THC_DOMINANT, 700, false, 0, always).stream()
                .anyMatch(e -> e.is(MobEffects.HUNGER)), "the control, THC schwag, gave no Hunger");
        context.succeed();
    }

    /**
     * No cannabinoids, no resin: the Sifting Box takes neither Dud Weed's buds nor its leaves (and its
     * buds are in no siftable tag, so no viewer page offers them), a rub of a ripe plant pays its leaf
     * and never charas, and the Press takes no cannabinoid-free resin. Purple Kush beside it sifts.
     */
    public static void dudWeedHasNoResin(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = Players.survival(context);
        context.assertTrue(!new ItemStack(ModItems.DUD_WEED_BUDS).is(ModTags.Items.SIFTABLE_FLOWER)
                        && !new ItemStack(ModItems.DUD_WEED_BUDS).is(ModTags.Items.SIFTABLE_RESINOUS),
                "Dud Weed's buds are in a siftable tag");

        BlockPos boxAt = SOIL.east(2);
        context.setBlock(boxAt, ModBlocks.SIFTING_BOX);
        BlockPos box = context.absolutePos(boxAt);
        BlockHitResult boxHit = new BlockHitResult(Vec3.atCenterOf(box), Direction.UP, box, false);
        for (ItemStack matter : List.of(new ItemStack(ModItems.DUD_WEED_BUDS, 8),
                Chemotype.stamp(new ItemStack(ModItems.HEMP_LEAF, 64), Chemotype.CANNABINOID_FREE))) {
            int before = matter.getCount();
            for (int i = 0; i < 8; i++) {
                world.getBlockState(box).useItemOn(matter, world, player, InteractionHand.MAIN_HAND, boxHit);
            }
            context.assertValueEqual(world.getBlockState(box).getValue(SiftingBoxBlock.LEVEL), 0, "the box level after " + matter);
            context.assertValueEqual(matter.getCount(), before, "what the box left of " + matter.getItem());
        }
        world.getBlockState(box).useItemOn(new ItemStack(ModItems.INDICA_BUDS), world, player, InteractionHand.MAIN_HAND, boxHit);
        context.assertValueEqual(world.getBlockState(box).getValue(SiftingBoxBlock.LEVEL), 1, "the control, a Purple Kush bud, sifted to");

        context.setBlock(SOIL, Blocks.FARMLAND);
        BlockPos crop = context.absolutePos(CROP);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(crop), Direction.UP, crop, false);
        int rubs = 48; // at one in four, a plant with resin pays none in 48 rubs once in a million
        for (int i = 0; i < rubs; i++) {
            context.setBlock(CROP, Defoliation.unworked(ModBlocks.DUD_WEED_CROP.defaultBlockState()
                    .setValue(IndicaCropBlock.HALF, DoubleBlockHalf.LOWER).setValue(IndicaCropBlock.AGE, Defoliation.RUB_MAX_AGE)));
            ItemStack shears = new ItemStack(Items.SHEARS);
            player.setItemInHand(InteractionHand.MAIN_HAND, shears);
            world.getBlockState(crop).useItemOn(shears, world, player, InteractionHand.MAIN_HAND, hit);
        }
        int charas = 0;
        int leaves = 0;
        for (ItemEntity item : world.getEntitiesOfClass(ItemEntity.class, new AABB(crop).inflate(4))) {
            if (item.getItem().is(ModItems.CHARAS)) {
                charas += item.getItem().getCount();
            } else if (item.getItem().is(ModItems.HEMP_LEAF)) {
                leaves += item.getItem().getCount();
            }
        }
        context.assertValueEqual(leaves, rubs, "sugar leaves off " + rubs + " rubs of ripe Dud Weed");
        context.assertValueEqual(charas, 0, "charas off " + rubs + " rubs of ripe Dud Weed");

        for (Item resin : List.of(ModItems.KIEF, ModItems.BUBBLE_HASH, ModItems.FILTERED_KIEF, ModItems.FILTERED_HASHISH)) {
            context.assertTrue(HempPressBlockEntity.resultFor(world, Chemotype.stamp(new ItemStack(resin), Chemotype.CANNABINOID_FREE)).isEmpty(),
                    "the Press took cannabinoid-free " + resin);
            context.assertTrue(!HempPressBlockEntity.resultFor(world, new ItemStack(resin)).isEmpty(), "the control, plain " + resin + ", did not press");
        }
        context.assertTrue(!HempPressBlockEntity.resultFor(world, new ItemStack(ModItems.RETTED_HEMP_STEM)).isEmpty(),
                "the Press refused a retted stem");
        context.succeed();
    }

    /**
     * A Type V bud shows no potency line, whatever band it carries; a CBD bud of the same band keeps
     * its line. Its seeds read cannabinoid-free, then humulene, and join the seed foods' tag.
     */
    public static void dudWeedShowsNoPotency(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        for (PotencyBand band : PotencyBand.values()) {
            List<String> dud = keys(world, PotencyBand.stamp(new ItemStack(ModItems.DUD_WEED_BUDS), band));
            context.assertTrue(dud.stream().noneMatch(key -> key.startsWith("hempdustry.potency.")),
                    "a " + band + " Dud Weed bud shows a potency line: " + dud);
            context.assertTrue(dud.contains("hempdustry.chemotype.cannabinoid_free"), "a Dud Weed bud does not say what it is: " + dud);
            context.assertTrue(dud.contains("hempdustry.terpene.humulene"), "a Dud Weed bud names no terpene: " + dud);
            List<String> cherry = keys(world, PotencyBand.stamp(new ItemStack(ModItems.CHERRY_WINE_BUDS), band));
            context.assertTrue(cherry.contains(band.getTranslationKey()), "a " + band + " Cherry Wine bud lost its potency line");
        }
        List<String> seeds = keys(world, new ItemStack(ModItems.DUD_WEED_SEEDS));
        int chemotype = seeds.indexOf("hempdustry.chemotype.cannabinoid_free");
        context.assertTrue(chemotype > 0 && seeds.indexOf("hempdustry.terpene.humulene") == chemotype + 1,
                "Dud Weed seeds do not read cannabinoid-free then humulene: " + seeds);
        context.assertTrue(new ItemStack(ModItems.DUD_WEED_SEEDS).is(ModTags.Items.HEMP_SEEDS),
                "Dud Weed seeds are not in #hempdustry:hemp_seeds, so no seed food takes them");
        context.succeed();
    }

    /**
     * One ripe Industrial Hemp harvest in 64 drops one Dud Weed seed, never more, Fortune or not; an
     * unripe plant, an upper segment and Ditchweed never do.
     */
    public static void industrialHempSportsDudWeed(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockState ripe = ModBlocks.INDUSTRIAL_HEMP_CROP.defaultBlockState()
                .setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.LOWER).setValue(SativaCropBlock.AGE, SativaCropBlock.MAX_AGE);
        int harvests = 6400;
        for (int fortune : new int[]{0, 3}) {
            Random seeds = new Random(SEED_SOURCE + fortune);
            int sports = 0;
            for (int i = 0; i < harvests; i++) {
                int got = harvest(context, ripe, seeds.nextLong(), fortune)[5];
                context.assertTrue(got <= 1, "one Industrial Hemp harvest gave " + got + " Dud Weed seeds");
                sports += got;
            }
            // 1 in 64 of 6400 is 100, with a standard deviation near 10.
            context.assertTrue(sports >= 65 && sports <= 135, sports + " of " + harvests + " ripe Industrial Hemp harvests"
                    + " with Fortune " + fortune + " gave a Dud Weed seed, expected about 100 (1 in 64)");
        }
        Random seeds = new Random(SEED_SOURCE);
        for (int i = 0; i < 1000; i++) {
            long seed = seeds.nextLong();
            context.assertValueEqual(harvest(context, ripe.setValue(SativaCropBlock.AGE, 0), seed, 0)[5], 0,
                    "an unripe Industrial Hemp gave a Dud Weed seed");
            context.assertValueEqual(harvest(context, ripe.setValue(SativaCropBlock.SEGMENT, TriplePlantSegment.UPPER), seed, 0)[5], 0,
                    "an upper segment gave a Dud Weed seed, so a tall plant would sport twice");
        }
        context.assertTrue(!encode(world, ModBlocks.DITCHWEED.getLootTable().orElseThrow()).contains("\"hempdustry:dud_weed_seeds\""),
                "Ditchweed sports Dud Weed");
        context.succeed();
    }

    /**
     * Its seeds come out of a shipwreck's supply chest one wreck in ten, one or two of them, and out of
     * no grass, shared chest or village house.
     */
    public static void dudWeedSeedsComeFromWrecks(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        String seeds = "\"hempdustry:dud_weed_seeds\"";
        for (ResourceKey<LootTable> none : List.of(Blocks.TALL_GRASS.getLootTable().orElseThrow(),
                Blocks.LARGE_FERN.getLootTable().orElseThrow(), BuiltInLootTables.SIMPLE_DUNGEON,
                BuiltInLootTables.ABANDONED_MINESHAFT, BuiltInLootTables.WOODLAND_MANSION, BuiltInLootTables.PILLAGER_OUTPOST,
                BuiltInLootTables.VILLAGE_PLAINS_HOUSE, BuiltInLootTables.VILLAGE_TAIGA_HOUSE, BuiltInLootTables.SHIPWRECK_TREASURE)) {
            String json = encode(world, none);
            context.assertTrue(!json.contains(seeds), none.identifier() + " names Dud Weed seeds");
        }
        context.assertTrue(encode(world, BuiltInLootTables.SIMPLE_DUNGEON).contains("\"hempdustry:indica_seeds\""),
                "the control, the dungeon, names no Purple Kush seeds");

        LootTable wreck = world.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SHIPWRECK_SUPPLY);
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(SOIL)))
                .create(LootContextParamSets.CHEST);
        Random random = new Random(SEED_SOURCE);
        int chests = 2000;
        int found = 0;
        for (int i = 0; i < chests; i++) {
            int count = wreck.getRandomItems(params, random.nextLong()).stream()
                    .filter(stack -> stack.is(ModItems.DUD_WEED_SEEDS)).mapToInt(ItemStack::getCount).sum();
            context.assertTrue(count <= 2, "a shipwreck held " + count + " Dud Weed seeds");
            if (count > 0) {
                found++;
            }
        }
        // 10 % of 2000 is 200, with a standard deviation near 13.
        context.assertTrue(found >= 160 && found <= 240, found + " of " + chests
                + " shipwreck supply chests held Dud Weed seeds, expected about 200 (10 %)");
        context.succeed();
    }

    /** "Damp Squib": loaded, hidden, under First Contact; a Dud Weed hit grants it, Purple Kush does not. */
    public static void dampSquib(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        AdvancementHolder squib = world.getServer().getAdvancements()
                .get(Identifier.fromNamespaceAndPath("hempdustry", "damp_squib"));
        context.assertTrue(squib != null, "hempdustry:damp_squib is not loaded");
        context.assertTrue(squib.value().display().map(DisplayInfo::hidden).orElse(false), "Damp Squib is not hidden");
        context.assertTrue(squib.value().parent().equals(Optional.of(Identifier.fromNamespaceAndPath("hempdustry", "first_contact"))),
                "Damp Squib does not hang under First Contact");
        ServerPlayer dud = Players.survival(context);
        smoke(dud, world, strain(world, ModStrains.DUD_WEED));
        context.assertTrue(dud.getAdvancements().getOrStartProgress(squib).isDone(), "a Dud Weed hit did not grant Damp Squib");
        ServerPlayer kush = Players.survival(context);
        smoke(kush, world, strain(world, ModStrains.INDICA));
        context.assertTrue(!kush.getAdvancements().getOrStartProgress(squib).isDone(), "a Purple Kush hit granted Damp Squib");
        // A spliff of Dud Weed and THC hashish goes off: it is no damp squib.
        ServerPlayer laced = Players.survival(context);
        smoke(laced, world, new SmokeContents(List.of(new SmokeContents.Entry(strain(world, ModStrains.DUD_WEED), 1),
                new SmokeContents.Entry(strain(world, ModStrains.HASHISH), 1))));
        context.assertTrue(!laced.getAdvancements().getOrStartProgress(squib).isDone(),
                "a hit of Dud Weed with THC hashish granted Damp Squib");
        context.succeed();
    }

    // ----- helpers -----

    private static Holder<Strain> strain(ServerLevel world, ResourceKey<Strain> key) {
        return world.registryAccess().lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(key);
    }

    private static List<String> keys(ServerLevel world, ItemStack stack) {
        List<Component> lines = stack.getTooltipLines(Item.TooltipContext.of(world), null, TooltipFlag.NORMAL);
        return lines.stream()
                .map(line -> line.getContents() instanceof TranslatableContents translatable ? translatable.getKey() : "")
                .toList();
    }

    /** {Dud Weed buds, Dud Weed seeds, leaves, stems, schwag, Dud Weed seeds off another plant} from one roll. */
    private static int[] harvest(GameTestHelper context, BlockState state, long seed, int fortune) {
        ServerLevel world = context.getLevel();
        LootTable table = world.getServer().reloadableRegistries().getLootTable(state.getBlock().getLootTable().orElseThrow());
        ItemStack tool = new ItemStack(Items.DIAMOND_HOE);
        if (fortune > 0) {
            Holder<Enchantment> enchantment = world.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
            tool.enchant(enchantment, fortune);
        }
        LootParams params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(CROP)))
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, tool)
                .create(LootContextParamSets.BLOCK);
        boolean dud = state.is(ModBlocks.DUD_WEED_CROP);
        Item buds = dud ? ModItems.DUD_WEED_BUDS : ModItems.INDICA_BUDS;
        Item seeds = dud ? ModItems.DUD_WEED_SEEDS : ModItems.INDICA_SEEDS;
        int[] counts = new int[6];
        for (ItemStack stack : table.getRandomItems(params, seed)) {
            if (stack.is(buds)) {
                counts[0] += stack.getCount();
            } else if (stack.is(seeds)) {
                counts[1] += stack.getCount();
            } else if (stack.is(ModItems.HEMP_LEAF)) {
                counts[2] += stack.getCount();
            } else if (stack.is(ModItems.HEMP_STEM)) {
                counts[3] += stack.getCount();
            } else if (stack.is(ModItems.SCHWAG)) {
                counts[4] += stack.getCount();
            }
            if (!dud && stack.is(ModItems.DUD_WEED_SEEDS)) {
                counts[5] += stack.getCount();
            }
        }
        return counts;
    }

    private static void plant(ServerPlayer player, ServerLevel world, BlockPos soil, ItemStack seeds) {
        player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
        player.gameMode.useItemOn(player, world, seeds, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soil).add(0, 0.5, 0), Direction.UP, soil, false));
    }

    private static void smoke(ServerPlayer player, ServerLevel world, Holder<Strain> strain) {
        smoke(player, world, SmokeContents.of(strain, 1));
    }

    private static void smoke(ServerPlayer player, ServerLevel world, SmokeContents load) {
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        pipe.set(ModComponents.SMOKE_CONTENTS, load);
        pipe.set(ModComponents.CHARGES, DeviceType.PIPE.builtInBowl().hits());
        player.setItemInHand(InteractionHand.MAIN_HAND, pipe);
        player.getCooldowns().removeCooldown(BuiltInRegistries.ITEM.getKey(ModItems.WOODEN_PIPE));
        player.gameMode.useItem(player, world, pipe, InteractionHand.MAIN_HAND);
    }

    private static String encode(ServerLevel world, ResourceKey<LootTable> key) {
        LootTable table = world.getServer().reloadableRegistries().getLootTable(key);
        return LootTable.DIRECT_CODEC.encodeStart(world.registryAccess().createSerializationContext(JsonOps.INSTANCE), table)
                .getOrThrow().toString();
    }
}
