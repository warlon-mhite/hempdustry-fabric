package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.SiftingBoxBlock;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

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
            s -> s.isOf(ModItems.INDICA_SEEDS) || s.isOf(ModItems.SATIVA_SEEDS);

    public static void hempTurnsUpWhereItWasKept(TestContext context) {
        ServerWorld world = context.getWorld();
        // Plains and taiga houses: a rare find, a roll at 10%.
        expect(context, LootTables.VILLAGE_PLAINS_CHEST, PLAIN_SEEDS, 60, 140, "a plains village house held hemp seeds");
        expect(context, LootTables.VILLAGE_TAIGA_HOUSE_CHEST, PLAIN_SEEDS, 60, 140, "a taiga village house held hemp seeds");
        // The desert house: Beldía's alone, as rare.
        expect(context, LootTables.VILLAGE_DESERT_HOUSE_CHEST, s -> s.isOf(ModItems.BELDIA_SEEDS), 60, 140,
                "a desert village house held Beldía seeds");
        expect(context, LootTables.VILLAGE_DESERT_HOUSE_CHEST, PLAIN_SEEDS, 0, 0,
                "a desert village house held a non-desert strain's seeds");
        // The fisher: weight 2 of 13 over 1-5 rolls comes to about 38% of chests.
        expect(context, LootTables.VILLAGE_FISHER_CHEST, s -> s.isOf(ModItems.TOASTED_HEMP_SEEDS), 300, 455,
                "a village fisher's chest held toasted hemp seeds");
        // The wreck: canvas is one in four of a 45% roll, about 11% of chests; fibre about 34%.
        expect(context, LootTables.SHIPWRECK_SUPPLY_CHEST, s -> s.isOf(ModItems.HEMP_CANVAS), 70, 160,
                "a shipwreck supply chest held hemp canvas");
        expect(context, LootTables.SHIPWRECK_SUPPLY_CHEST, s -> s.isOf(ModItems.HEMP_FIBER), 270, 410,
                "a shipwreck supply chest held hemp fibre");
        // The negative: a snowy house is not a hemp house.
        expect(context, LootTables.VILLAGE_SNOWY_HOUSE_CHEST, PLAIN_SEEDS, 0, 0, "a snowy village house held hemp seeds");

        // Fishing junk: weight 5 beside string's 5, about 4.5% of junk catches outside a jungle.
        LootTable junk = world.getServer().getReloadableRegistries().getLootTable(LootTables.FISHING_JUNK_GAMEPLAY);
        LootWorldContext fishing = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(context.getAbsolutePos(AT)))
                .add(LootContextParameters.TOOL, new ItemStack(Items.FISHING_ROD))
                .build(LootContextTypes.FISHING);
        int fibre = count(junk, fishing, s -> s.isOf(ModItems.HEMP_FIBER), 2 * ROLLS);
        context.assertTrue(fibre >= 50 && fibre <= 140,
                fibre + " of " + 2 * ROLLS + " junk catches were hemp fibre, expected about 90");
        context.complete();
    }

    /**
     * A Sifting Box broken with a batch ready gives the batch back, as a full composter gives its bone
     * meal — which batch following the box's state exactly as a click would — and a box still filling
     * gives only itself.
     */
    public static void aReadyBoxGivesItsBatch(TestContext context) {
        BlockState ready = ModBlocks.SIFTING_BOX.getDefaultState().with(SiftingBoxBlock.LEVEL, SiftingBoxBlock.READY_LEVEL);
        assertDrops(context, ready.with(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.PLANT).with(SiftingBoxBlock.FILLED, false),
                ModItems.KIEF, "a ready dry sift");
        assertDrops(context, ready.with(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.PLANT).with(SiftingBoxBlock.FILLED, true),
                ModItems.BUBBLE_HASH, "a ready wash");
        assertDrops(context, ready.with(SiftingBoxBlock.CONTENT, SiftingBoxBlock.Content.KIEF),
                ModItems.FILTERED_KIEF, "a ready re-sift");
        List<ItemStack> filling = Block.getDroppedStacks(
                ready.with(SiftingBoxBlock.LEVEL, SiftingBoxBlock.FULL_LEVEL), context.getWorld(), context.getAbsolutePos(AT), null);
        context.assertTrue(filling.size() == 1 && filling.getFirst().isOf(ModBlocks.SIFTING_BOX.asItem()),
                "a box still filling dropped " + filling + " instead of only itself");
        context.complete();
    }

    /**
     * A Decarboxylator placed from an item an anvil renamed keeps the name: on its screen, through a
     * save and a load, and back onto the item when broken — vanilla's furnace, in all three.
     */
    public static void aNamedMachineKeepsItsName(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos pos = context.getAbsolutePos(AT);
        context.setBlockState(AT, ModBlocks.DECARBOXYLATOR);
        ItemStack named = new ItemStack(ModBlocks.DECARBOXYLATOR);
        named.set(DataComponentTypes.CUSTOM_NAME, Text.literal("The Oven"));
        DecarboxylatorBlockEntity machine = context.getBlockEntity(AT, DecarboxylatorBlockEntity.class);
        machine.readComponents(named);

        context.assertTrue(machine.getDisplayName().getString().equals("The Oven"),
                "a renamed Decarboxylator's screen is titled " + machine.getDisplayName().getString());
        BlockEntity reloaded = BlockEntity.createFromNbt(pos, machine.getCachedState(),
                machine.createNbtWithIdentifyingData(world.getRegistryManager()), world.getRegistryManager());
        context.assertTrue(reloaded instanceof DecarboxylatorBlockEntity loaded
                        && loaded.getDisplayName().getString().equals("The Oven"),
                "the Decarboxylator's name did not survive a save and a load");
        List<ItemStack> drops = Block.getDroppedStacks(machine.getCachedState(), world, pos, machine);
        context.assertTrue(drops.size() == 1 && Text.literal("The Oven").equals(drops.getFirst().get(DataComponentTypes.CUSTOM_NAME)),
                "a broken renamed Decarboxylator dropped " + drops + " without its name");
        context.complete();
    }

    // ----- helpers -----

    private static void expect(TestContext context, RegistryKey<LootTable> key, Predicate<ItemStack> what,
                               int low, int high, String description) {
        ServerWorld world = context.getWorld();
        LootTable table = world.getServer().getReloadableRegistries().getLootTable(key);
        LootWorldContext chest = new LootWorldContext.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(context.getAbsolutePos(AT)))
                .build(LootContextTypes.CHEST);
        int found = count(table, chest, what, ROLLS);
        context.assertTrue(found >= low && found <= high,
                description + " " + found + " times in " + ROLLS + ", expected " + low + "-" + high);
    }

    private static int count(LootTable table, LootWorldContext params, Predicate<ItemStack> what, int rolls) {
        Random seeds = new Random(SEED_SOURCE);
        int found = 0;
        for (int i = 0; i < rolls; i++) {
            if (table.generateLoot(params, seeds.nextLong()).stream().anyMatch(what)) {
                found++;
            }
        }
        return found;
    }

    private static void assertDrops(TestContext context, BlockState state, Item batch, String description) {
        List<ItemStack> drops = Block.getDroppedStacks(state, context.getWorld(), context.getAbsolutePos(AT), null);
        boolean box = drops.stream().anyMatch(s -> s.isOf(ModBlocks.SIFTING_BOX.asItem()));
        int pieces = drops.stream().filter(s -> s.isOf(batch)).mapToInt(ItemStack::getCount).sum();
        context.assertTrue(box && pieces == SiftingBoxBlock.YIELD && drops.size() == 2,
                description + " broken dropped " + drops + ", expected the box and " + SiftingBoxBlock.YIELD + " " + batch);
    }
}
