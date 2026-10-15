package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
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
        LootTable junk = world.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING_JUNK);
        LootParams fishing = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(AT)))
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        int fibre = count(junk, fishing, s -> s.is(ModItems.HEMP_FIBER), 2 * ROLLS);
        context.assertTrue(fibre >= 50 && fibre <= 140,
                fibre + " of " + 2 * ROLLS + " junk catches were hemp fibre, expected about 90");
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
        context.assertTrue(filling.size() == 1 && filling.getFirst().is(ModBlocks.SIFTING_BOX.asItem()),
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
        named.set(DataComponents.CUSTOM_NAME, Component.literal("The Oven"));
        DecarboxylatorBlockEntity machine = context.<DecarboxylatorBlockEntity>getBlockEntity(AT);
        machine.applyComponentsFromItemStack(named);

        context.assertTrue(machine.getDisplayName().getString().equals("The Oven"),
                "a renamed Decarboxylator's screen is titled " + machine.getDisplayName().getString());
        BlockEntity reloaded = BlockEntity.loadStatic(pos, machine.getBlockState(),
                machine.saveWithFullMetadata(world.registryAccess()), world.registryAccess());
        context.assertTrue(reloaded instanceof DecarboxylatorBlockEntity loaded
                        && loaded.getDisplayName().getString().equals("The Oven"),
                "the Decarboxylator's name did not survive a save and a load");
        List<ItemStack> drops = Block.getDrops(machine.getBlockState(), world, pos, machine);
        context.assertTrue(drops.size() == 1 && Component.literal("The Oven").equals(drops.getFirst().get(DataComponents.CUSTOM_NAME)),
                "a broken renamed Decarboxylator dropped " + drops + " without its name");
        context.succeed();
    }

    // ----- helpers -----

    private static void expect(GameTestHelper context, ResourceKey<LootTable> key, Predicate<ItemStack> what,
                               int low, int high, String description) {
        ServerLevel world = context.getLevel();
        LootTable table = world.getServer().reloadableRegistries().getLootTable(key);
        LootParams chest = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(context.absolutePos(AT)))
                .create(LootContextParamSets.CHEST);
        int found = count(table, chest, what, ROLLS);
        context.assertTrue(found >= low && found <= high,
                description + " " + found + " times in " + ROLLS + ", expected " + low + "-" + high);
    }

    private static int count(LootTable table, LootParams params, Predicate<ItemStack> what, int rolls) {
        Random seeds = new Random(SEED_SOURCE);
        int found = 0;
        for (int i = 0; i < rolls; i++) {
            if (table.getRandomItems(params, seeds.nextLong()).stream().anyMatch(what)) {
                found++;
            }
        }
        return found;
    }

    private static void assertDrops(GameTestHelper context, BlockState state, Item batch, String description) {
        List<ItemStack> drops = Block.getDrops(state, context.getLevel(), context.absolutePos(AT), null);
        boolean box = drops.stream().anyMatch(s -> s.is(ModBlocks.SIFTING_BOX.asItem()));
        int pieces = drops.stream().filter(s -> s.is(batch)).mapToInt(ItemStack::getCount).sum();
        context.assertTrue(box && pieces == SiftingBoxBlock.YIELD && drops.size() == 2,
                description + " broken dropped " + drops + ", expected the box and " + SiftingBoxBlock.YIELD + " " + batch);
    }
}
