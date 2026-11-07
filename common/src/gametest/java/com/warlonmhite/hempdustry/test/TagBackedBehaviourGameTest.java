package com.warlonmhite.hempdustry.test;

import net.minecraft.core.Registry;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.HempCarpetBlock;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.warlonmhite.hempdustry.registry.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.ArrayList;
import java.util.List;

/**
 * Features whose whole behaviour is one entry in a tag file or one call into a registry, and which
 * therefore fail <em>silently</em> — the block simply does the old thing, with nothing in any log.
 *
 * <p>The rule they all share: <b>one unresolvable {@code required} entry drops a whole tag</b> and
 * takes its valid entries with it, and a tag that has quietly gone empty is indistinguishable from a
 * feature nobody built. A composting chance that never registered reads the same way — the composter
 * just refuses the item. Hence the negative assertions here: a test that only proves the tag matches
 * what it should would still pass against a tag that had become "everything".
 */
public final class TagBackedBehaviourGameTest {

    /**
     * Lava heats the Infuser — the fluid and a lava cauldron alike.
     *
     * <p>Asserted through {@link InfuserBlockEntity#isHeatedFrom} rather than by standing a tub over
     * lava and waiting, because that method <em>is</em> the feature: it is the one place the tag is
     * read. Flowing lava is checked too, since {@code Blocks.LAVA} is both, and the LIT clause in
     * {@code isHeatedFrom} must not swallow a block that has no LIT property at all.
     *
     * <p>The two negatives at the end are what stop this passing against a tag that has accidentally
     * become "every block".
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void lavaHeatsTheInfuser(GameTestHelper context) {
        Asserts.assertTrue(context, InfuserBlockEntity.isHeatedFrom(Blocks.LAVA.defaultBlockState()),
                "a lava source does not heat the Infuser — minecraft:lava has left #hempdustry:heat_sources");
        Asserts.assertTrue(context, 
                InfuserBlockEntity.isHeatedFrom(Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL, 3)),
                "flowing lava does not heat the Infuser, though it is the same block as the source");
        Asserts.assertTrue(context, InfuserBlockEntity.isHeatedFrom(Blocks.LAVA_CAULDRON.defaultBlockState()),
                "a lava cauldron does not heat the Infuser — minecraft:lava_cauldron has left the tag");

        Asserts.assertFalse(context, InfuserBlockEntity.isHeatedFrom(Blocks.WATER_CAULDRON.defaultBlockState()),
                "a WATER cauldron heats the Infuser, so the tag is matching more than it should");
        Asserts.assertFalse(context, InfuserBlockEntity.isHeatedFrom(Blocks.STONE.defaultBlockState()),
                "plain stone heats the Infuser, so the tag is matching more than it should");

        context.succeed();
    }

    /**
     * Both storage blocks compost, at their vanilla counterpart's rate.
     *
     * <p>Read straight out of {@code ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE}, which is the map
     * the composter itself consults — so this fails if the Fabric registry call stops landing, and it
     * fails if the numbers drift. A composting chance that never registered is invisible: the
     * composter simply refuses the item, exactly as it does for something never meant to go in.
     *
     * <p>The two rates are deliberately different — vanilla puts hay at 0.85 and the dried kelp block,
     * its other compacted plant, at 0.5 — so asserting them separately is the point, not duplication.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void storageBlocksCompost(GameTestHelper context) {
        assertComposts(context, ModBlocks.HEMP_BALE, 0.85F, "hay's rate");
        assertComposts(context, ModBlocks.HEMP_LEAVES, 0.5F, "the dried kelp block's rate");
        context.succeed();
    }

    private static void assertComposts(GameTestHelper context, Block block, float expected, String why) {
        float actual = TestPlatform.INSTANCE.compostChance(block.asItem());
        Asserts.assertTrue(context, actual > 0.0F,
                block.asItem() + " has no composting chance at all — a composter will refuse it");
        Asserts.assertTrue(context, Math.abs(actual - expected) < 0.0001F,
                block.asItem() + " composts at " + actual + " rather than " + expected + " (" + why + ")");
    }

    /**
     * Shears mine the Block of Hemp Leaves at the leaf rate.
     *
     * <p>Checked as a mining-speed number, not as tag membership, because the tag is only the means:
     * shears' 15× rule names {@code #minecraft:leaves} inside their {@code tool} component
     * ({@code ShearsItem#createToolComponent}), so this fails if either half moves — the block
     * leaving the tag, or a future vanilla version rewriting how shears declare their rules.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void shearsMineHempLeavesFast(GameTestHelper context) {
        var leaves = ModBlocks.HEMP_LEAVES.defaultBlockState();

        Asserts.assertTrue(context, leaves.is(BlockTags.LEAVES),
                "hemp_leaves is not in #minecraft:leaves, so shears, hoes and swords all treat it as stone");

        float shears = new ItemStack(Items.SHEARS).getDestroySpeed(leaves);
        Asserts.assertTrue(context, shears > 1.0F,
                "shears mine hemp_leaves at " + shears + "x — no faster than a bare hand");

        float pickaxe = new ItemStack(Items.DIAMOND_PICKAXE).getDestroySpeed(leaves);
        Asserts.assertTrue(context, shears > pickaxe,
                "a diamond pickaxe (" + pickaxe + "x) matches or beats shears (" + shears
                        + "x) on hemp_leaves, which is not how any leaf block behaves");

        context.succeed();
    }

    /**
     * A tamed llama takes the hemp carpet, wears it where its clients can see it, and a horse does not
     * take it.
     *
     * <p>Two separate things on this line, both silent when missing. A llama's own check is
     * {@code #minecraft:wool_carpets} ({@code Llama#isArmor}); but the carpet stays in the llama's
     * inventory on the server, and all its clients learn is the synced swag colour, which vanilla
     * takes from a {@code WoolCarpetBlock}'s dye. So a llama in ours syncs a value of its own
     * ({@code LlamaMixin}, {@code HempCarpetBlock#isWornBy}). A dispenser asks the tag too: see the
     * next test. <b>Drawing it is the client's job and is not tested here</b> —
     * {@code LlamaDecorFeatureRendererMixin} does that.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aLlamaWearsHempCarpet(GameTestHelper context) {
        ItemStack carpet = new ItemStack(ModBlocks.HEMP_CARPET);
        Llama llama = context.spawn(EntityType.LLAMA, new BlockPos(1, 1, 1));
        llama.setTamed(true);
        Asserts.assertTrue(context, llama.isArmor(carpet), "a tamed llama will not wear hemp carpet");
        // Slot 401 is a llama's carpet, which /item replace and a player's click both fill.
        Asserts.assertTrue(context, llama.getSlot(401).set(carpet.copy()) && HempCarpetBlock.isWornBy(llama),
                "a llama in hemp carpet does not tell its clients it wears one, so they draw it bare");
        Horse horse = context.spawn(EntityType.HORSE, new BlockPos(4, 1, 1));
        horse.setTamed(true);
        Asserts.assertFalse(context, horse.isArmor(carpet), "a horse will wear hemp carpet");
        context.succeed();
    }

    /**
     * A dispenser puts the hemp carpet on a tamed llama in front of it, as it does any vanilla carpet.
     *
     * <p>On this line a dispenser looks its behaviour up by item, and vanilla hands its "equip a horse
     * or a llama" behaviour to its sixteen carpets by name — the carpet tag has no say there — so a
     * carpet without that entry is thrown out in front of the llama instead. This also proves vanilla's behaviours are registered before ours borrows one.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aDispenserPutsHempCarpetOnALlama(GameTestHelper context) {
        BlockPos dispenserPos = new BlockPos(0, 1, 1);
        context.setBlock(dispenserPos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        Llama llama = context.spawn(EntityType.LLAMA, new BlockPos(1, 1, 1));
        llama.setTamed(true);
        llama.setNoAi(true);
        DispenserBlockEntity dispenser = (DispenserBlockEntity) context.getBlockEntity(dispenserPos);
        dispenser.setItem(0, new ItemStack(ModBlocks.HEMP_CARPET));
        // Above the dispenser, as in InfuserPourGameTest. Beside it at (0, 1, 0) the redstone block
        // powers the test's own structure block, which reloads the empty structure over the dispenser.
        context.pulseRedstone(new BlockPos(0, 2, 1), 1);

        context.runAtTickTime(10, () -> {
            ItemStack worn = llama.getSlot(401).get();
            Asserts.assertTrue(context, worn.is(ModBlocks.HEMP_CARPET.asItem()),
                    "the dispenser did not put the hemp carpet on the llama, which wears " + worn);
            Asserts.assertTrue(context, dispenser.getItem(0).isEmpty(), "the dispenser kept the carpet: " + dispenser.getItem(0));
            context.succeed();
        });
    }

    /**
     * Every block of ours drops by a loot table that exists.
     *
     * <p>A block with no table of its own drops nothing, with nothing in any log: the game looks the
     * table up under the block's id, finds none and uses the empty one. That is how the hemp trapdoor
     * and both potted hemp flowers shipped in 2.0.1 — breaking one gave back nothing. Copying a vanilla
     * block's settings does not help, because the table is looked up under the copy's own id. Wall
     * signs pass because they point at their standing sign's table on purpose.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyBlockHasALootTable(GameTestHelper context) {
        var loot = context.getLevel().getServer().getLootTables();
        List<String> missing = new ArrayList<>();
        for (Block block : Registry.BLOCK) {
            if (!Registry.BLOCK.getKey(block).getNamespace().equals(Hempdustry.MOD_ID)) {
                continue;
            }
            if (loot.get(block.getLootTable()) == LootTable.EMPTY) {
                missing.add(Registry.BLOCK.getKey(block).getPath());
            }
        }
        Asserts.assertTrue(context, missing.isEmpty(), "these blocks have no loot table and drop nothing: " + missing);
        context.succeed();
    }

    /**
     * No potted plant or crop of ours is an item, as none of vanilla's is.
     *
     * <p>Vanilla's potted plants and crops are blocks only: the flower, the sapling or the seeds are
     * the item, and pick-block on a potted plant hands back the plant. A block registered with a
     * {@code BlockItem} anyway gets one silently, named and modelled, in every recipe viewer's list
     * — the crops had one until 2.0.1-beta and the potted flowers until 2.0.2 — and an item id cannot
     * be taken back once a stable release has shipped it.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void pottedPlantsAndCropsHaveNoItem(GameTestHelper context) {
        List<String> items = new ArrayList<>();
        for (Block block : Registry.BLOCK) {
            ResourceLocation id = Registry.BLOCK.getKey(block);
            if (id.getNamespace().equals(Hempdustry.MOD_ID)
                    && (block instanceof FlowerPotBlock || block instanceof CropBlock)
                    && Registry.ITEM.containsKey(id)) {
                items.add(id.getPath());
            }
        }
        Asserts.assertTrue(context, items.isEmpty(), "these potted plants or crops have an item, which vanilla's never do: " + items);
        context.succeed();
    }

    /**
     * Hemp fibre repairs every piece of the hemp set on an anvil, and string does not.
     *
     * <p>On this version the repair material is the armour material's Ingredient; the 1.21.11 build
     * names a tag instead, and until 2.0.3 no file filled it, so the anvil there refused everything
     * and nothing said why. The same rule, held on both.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void hempFiberRepairsTheOutfit(GameTestHelper context) {
        ItemStack fiber = new ItemStack(ModItems.HEMP_FIBER);
        for (Item piece : List.of(ModItems.HEMP_BEANIE, ModItems.HEMP_SHIRT, ModItems.HEMP_HAREM_PANTS,
                ModItems.FLIP_FLOPS)) {
            Asserts.assertTrue(context, piece.isValidRepairItem(new ItemStack(piece), fiber),
                    "hemp fibre does not repair " + piece + " — the material's repair ingredient lost it");
            Asserts.assertFalse(context, piece.isValidRepairItem(new ItemStack(piece), new ItemStack(Items.STRING)),
                    "string repairs " + piece + ", so the repair ingredient has grown past hemp fibre");
        }
        context.succeed();
    }

    /**
     * All four pieces take armour's enchantments, on an anvil and at the table.
     *
     * <p>On the newer lines only the shirt and the harem pants take a trim (the beanie and the
     * flip-flops are drawn by models of their own); 1.18.2 has no trims (1.20), so that half is not
     * here. An armour piece takes its slot's enchantments by its class, and the
     * enchanting table opens to it by the material's enchantability.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void onlyTheGarmentsTakeATrim(GameTestHelper context) {
        Enchantment protection = Enchantments.ALL_DAMAGE_PROTECTION;
        for (Item piece : List.of(ModItems.HEMP_BEANIE, ModItems.HEMP_SHIRT, ModItems.HEMP_HAREM_PANTS,
                ModItems.FLIP_FLOPS)) {
            ItemStack stack = new ItemStack(piece);
            for (Enchantment enchantment : List.of(Enchantments.ALL_DAMAGE_PROTECTION, Enchantments.UNBREAKING,
                    Enchantments.MENDING, Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE)) {
                Asserts.assertTrue(context, enchantment.canEnchant(stack),
                        piece + " refuses " + Registry.ENCHANTMENT.getKey(enchantment) + " on an anvil");
            }
            Asserts.assertTrue(context, stack.isEnchantable() && protection.category.canEnchant(piece),
                    piece + " is offered no Protection at the enchanting table");
        }
        Asserts.assertTrue(context, Enchantments.RESPIRATION.canEnchant(new ItemStack(ModItems.HEMP_BEANIE)),
                "the beanie refuses Respiration");
        Asserts.assertTrue(context, Enchantments.FALL_PROTECTION.canEnchant(new ItemStack(ModItems.FLIP_FLOPS)),
                "the flip-flops refuse Feather Falling");
        context.succeed();
    }

    /**
     * Every damageable smokeable takes Unbreaking and Mending: each device and each coloured bong.
     * The tag was once listed by hand, and the vaporizer, added later, was never in it.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyDeviceTakesUnbreaking(GameTestHelper context) {
        List<Item> devices = new ArrayList<>(ModItems.devices().values());
        devices.addAll(ModItems.COLORED_BONGS);
        for (Item device : devices) {
            for (Enchantment enchantment : List.of(Enchantments.UNBREAKING, Enchantments.MENDING)) {
                Asserts.assertTrue(context, enchantment.canEnchant(new ItemStack(device)),
                        device + " refuses " + Registry.ENCHANTMENT.getKey(enchantment) + " — it is not damageable");
            }
        }
        context.succeed();
    }
}
