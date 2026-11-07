package com.warlonmhite.hempdustry.test;

import net.minecraft.network.chat.TextComponent;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.loot.LootTableOrigin;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import com.warlonmhite.hempdustry.block.entity.NamedMachineBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * What hemp's blocks give back when broken, and where hemp turns up in vanilla's loot.
 *
 * <p>Every placement is an event hook into somebody else's table, so a typo in a table key or a pool
 * that never lands is invisible: the chest simply fills as vanilla fills it. Each is rolled a thousand
 * times from a seeded {@link Random} — never from a counter, whose consecutive seeds correlate the
 * first draw — and counted against the rate it was pitched at, with a band wide enough that only a
 * real change fails it. The snowy house is the negative: it must hold no hemp at all.
 */
public final class LootGameTest {
    private static final long SEED_SOURCE = 20260929L;
    private static final int ROLLS = 1000;
    private static final BlockPos AT = new BlockPos(1, 1, 1);

    private static final Predicate<ItemStack> PLAIN_SEEDS =
            s -> s.is(ModItems.INDICA_SEEDS) || s.is(ModItems.SATIVA_SEEDS);

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void hempTurnsUpWhereItWasKept(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        // Plains and taiga houses: a rare find, a roll at 10%.
        expect(context, BuiltInLootTables.VILLAGE_PLAINS_HOUSE, PLAIN_SEEDS, 60, 140, "a plains village house held hemp seeds");
        expect(context, BuiltInLootTables.VILLAGE_TAIGA_HOUSE, PLAIN_SEEDS, 60, 140, "a taiga village house held hemp seeds");
        // The desert house: Beldía's alone, as rare.
        expect(context, BuiltInLootTables.VILLAGE_DESERT_HOUSE, s -> s.is(ModItems.BELDIA_SEEDS), 60, 140,
                "a desert village house held Beldía seeds");
        expect(context, BuiltInLootTables.VILLAGE_DESERT_HOUSE, PLAIN_SEEDS, 0, 0,
                "a desert village house held a non-desert strain's seeds");
        // The fisher: weight 2 of 13 over 1-5 rolls comes to about 38% of chests.
        expect(context, BuiltInLootTables.VILLAGE_FISHER, s -> s.is(ModItems.TOASTED_HEMP_SEEDS), 300, 455,
                "a village fisher's chest held toasted hemp seeds");
        // The wreck: canvas is one in four of a 45% roll, about 11% of chests; fibre about 34%.
        expect(context, BuiltInLootTables.SHIPWRECK_SUPPLY, s -> s.is(ModItems.HEMP_CANVAS), 70, 160,
                "a shipwreck supply chest held hemp canvas");
        expect(context, BuiltInLootTables.SHIPWRECK_SUPPLY, s -> s.is(ModItems.HEMP_FIBER), 270, 410,
                "a shipwreck supply chest held hemp fibre");
        // The negative: a snowy house is not a hemp house.
        expect(context, BuiltInLootTables.VILLAGE_SNOWY_HOUSE, PLAIN_SEEDS, 0, 0, "a snowy village house held hemp seeds");

        // Fishing junk: weight 5 beside string's 5, about 4.5% of junk catches outside a jungle.
        LootTable junk = world.getServer().getLootTables().get(BuiltInLootTables.FISHING_JUNK);
        LootContext.Builder fishing = new LootContext.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(AT)))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD));
        int fibre = count(junk, fishing, LootContextParamSets.FISHING, s -> s.is(ModItems.HEMP_FIBER), 2 * ROLLS);
        Asserts.assertTrue(context, fibre >= 50 && fibre <= 140,
                fibre + " of " + 2 * ROLLS + " junk catches were hemp fibre, expected about 90");
        context.succeed();
    }

    /**
     * The rule the mod adds loot by, asked of the packs a real world loads. Vanilla's main pack and
     * the mod's own are the game's or a mod's. A datapack laid over them -- built as the game builds
     * one from a world's folder, a pack named {@code file/...} -- replaced the temple chest on purpose,
     * and the mod leaves it as its author wrote it; the chests it did not replace are still vanilla's.
     * (1.18.2 has no experiment pack replacing a chest, as the newer lines' Trade Rebalance does.)
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aDatapacksChestIsLeftAlone(GameTestHelper context) {
        ResourceManager world = context.getLevel().getServer().getResourceManager();
        ResourceLocation dungeon = BuiltInLootTables.SIMPLE_DUNGEON;
        ResourceLocation temple = BuiltInLootTables.DESERT_PYRAMID;
        ResourceLocation ours = ModBlocks.DECARBOXYLATOR.getLootTable();
        Asserts.assertTrue(context, LootTableOrigin.fromTheGameOrAMod(world, dungeon), "vanilla's dungeon chest does not count as the game's");
        Asserts.assertTrue(context, LootTableOrigin.fromTheGameOrAMod(world, temple), "vanilla's temple chest does not count as the game's");
        Asserts.assertTrue(context, LootTableOrigin.fromTheGameOrAMod(world, ours), "the mod's own " + ours + " does not count as a mod's");

        Path folder = context.getLevel().getServer().getServerDirectory().toPath().resolve("hempdustry-test-datapack");
        try {
            Path chest = folder.resolve("data/minecraft/loot_tables/chests/desert_pyramid.json");
            Files.createDirectories(chest.getParent());
            Files.writeString(chest, "{\"pools\": []}");
            // A datapack folder, as FolderRepositorySource opens one it finds in a world's folder.
            PackResources datapack = new FolderPackResources(folder.toFile());
            List<PackResources> packs = new ArrayList<>(world.listPacks().toList());
            packs.add(datapack);
            ResourceManager withDatapack = new MultiPackResourceManager(PackType.SERVER_DATA, packs);
            Asserts.assertFalse(context, LootTableOrigin.fromTheGameOrAMod(withDatapack, temple),
                    "a datapack's own temple chest counts as the game's: the mod would add to a chest its author replaced");
            Asserts.assertTrue(context, LootTableOrigin.fromTheGameOrAMod(withDatapack, dungeon),
                    "a datapack that replaced the temple chest made vanilla's dungeon chest count as the datapack's");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try (Stream<Path> files = Files.walk(folder)) {
                files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            } catch (IOException ignored) {
                // Nothing was written, or it is gone already.
            }
        }
        context.succeed();
    }

    /**
     * A Sifting Box broken with a batch ready gives the batch back, as a full composter gives its bone
     * meal — which batch following the box's state exactly as a click would — and a box still filling
     * gives only itself.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aReadyBoxGivesItsBatch(GameTestHelper context) {
        BlockState ready = ModBlocks.SIFTING_BOX.defaultBlockState().setValue(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL);
        assertDrops(context, ready.setValue(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.PLANT).setValue(SiftingBoxBlock.FILLED, false),
                ModItems.KIEF, "a ready dry sift");
        assertDrops(context, ready.setValue(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.PLANT).setValue(SiftingBoxBlock.FILLED, true),
                ModItems.BUBBLE_HASH, "a ready wash");
        assertDrops(context, ready.setValue(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.KIEF),
                ModItems.FILTERED_KIEF, "a ready re-sift");
        List<ItemStack> filling = Block.getDrops(
                ready.setValue(SiftingBoxBlock.LEVEL, SiftingBoxBlock.FULL_LEVEL), context.getLevel(), context.absolutePos(AT), null);
        Asserts.assertTrue(context, filling.size() == 1 && filling.get(0).is(ModBlocks.SIFTING_BOX.asItem()),
                "a box still filling dropped " + filling + " instead of only itself");
        context.succeed();
    }

    /**
     * A Decarboxylator placed from an item an anvil renamed keeps the name: on its screen, through a
     * save and a load, and back onto the item when broken — vanilla's furnace, in all three.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aNamedMachineKeepsItsName(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        BlockPos pos = context.absolutePos(AT);
        context.setBlock(AT, ModBlocks.DECARBOXYLATOR);
        ItemStack named = new ItemStack(ModBlocks.DECARBOXYLATOR);
        named.setHoverName(new TextComponent("The Oven"));
        // What the block's setPlacedBy does with the item it was placed from.
        NamedMachineBlockEntity.nameFromItem(world, pos, named);
        DecarboxylatorBlockEntity machine = ((DecarboxylatorBlockEntity) context.getBlockEntity(AT));

        Asserts.assertTrue(context, machine.getDisplayName().getString().equals("The Oven"),
                "a renamed Decarboxylator's screen is titled " + machine.getDisplayName().getString());
        BlockEntity reloaded = BlockEntity.loadStatic(pos, machine.getBlockState(),
                machine.saveWithFullMetadata());
        Asserts.assertTrue(context, reloaded instanceof DecarboxylatorBlockEntity loaded
                        && loaded.getDisplayName().getString().equals("The Oven"),
                "the Decarboxylator's name did not survive a save and a load");
        List<ItemStack> drops = Block.getDrops(machine.getBlockState(), world, pos, machine);
        Asserts.assertTrue(context, drops.size() == 1 && drops.get(0).hasCustomHoverName()
                        && drops.get(0).getHoverName().getString().equals("The Oven"),
                "a broken renamed Decarboxylator dropped " + drops + " without its name");
        context.succeed();
    }

    // ----- helpers -----

    private static void expect(GameTestHelper context, ResourceLocation key, Predicate<ItemStack> what,
                               int low, int high, String description) {
        ServerLevel world = context.getLevel();
        LootTable table = world.getServer().getLootTables().get(key);
        LootContext.Builder chest = new LootContext.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(AT)));
        int found = count(table, chest, LootContextParamSets.CHEST, what, ROLLS);
        Asserts.assertTrue(context, found >= low && found <= high,
                description + " " + found + " times in " + ROLLS + ", expected " + low + "-" + high);
    }

    private static ResourceLocation file(ResourceLocation table) {
        return new ResourceLocation(table.getNamespace(), "loot_tables/" + table.getPath() + ".json");
    }

    private static int count(LootTable table, LootContext.Builder params, LootContextParamSet set, Predicate<ItemStack> what, int rolls) {
        Random seeds = new Random(SEED_SOURCE);
        int found = 0;
        for (int i = 0; i < rolls; i++) {
            if (Line.roll(table, params, set, seeds.nextLong()).stream().anyMatch(what)) {
                found++;
            }
        }
        return found;
    }

    private static void assertDrops(GameTestHelper context, BlockState state, Item batch, String description) {
        List<ItemStack> drops = Block.getDrops(state, context.getLevel(), context.absolutePos(AT), null);
        boolean box = drops.stream().anyMatch(s -> s.is(ModBlocks.SIFTING_BOX.asItem()));
        int pieces = drops.stream().filter(s -> s.is(batch)).mapToInt(ItemStack::getCount).sum();
        Asserts.assertTrue(context, box && pieces == SiftingBoxBlock.YIELD && drops.size() == 2,
                description + " broken dropped " + drops + ", expected the box and " + SiftingBoxBlock.YIELD + " " + batch);
    }
}
