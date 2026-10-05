package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.passive.TraderLlamaEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

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
    public static void lavaHeatsTheInfuser(TestContext context) {
        context.assertTrue(InfuserBlockEntity.isHeatedFrom(Blocks.LAVA.getDefaultState()),
                "a lava source does not heat the Infuser — minecraft:lava has left #hempdustry:heat_sources");
        context.assertTrue(
                InfuserBlockEntity.isHeatedFrom(Blocks.LAVA.getDefaultState().with(net.minecraft.state.property.Properties.LEVEL_15, 3)),
                "flowing lava does not heat the Infuser, though it is the same block as the source");
        context.assertTrue(InfuserBlockEntity.isHeatedFrom(Blocks.LAVA_CAULDRON.getDefaultState()),
                "a lava cauldron does not heat the Infuser — minecraft:lava_cauldron has left the tag");

        context.assertFalse(InfuserBlockEntity.isHeatedFrom(Blocks.WATER_CAULDRON.getDefaultState()),
                "a WATER cauldron heats the Infuser, so the tag is matching more than it should");
        context.assertFalse(InfuserBlockEntity.isHeatedFrom(Blocks.STONE.getDefaultState()),
                "plain stone heats the Infuser, so the tag is matching more than it should");

        context.complete();
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
    public static void storageBlocksCompost(TestContext context) {
        assertComposts(context, ModBlocks.HEMP_BALE, 0.85F, "hay's rate");
        assertComposts(context, ModBlocks.HEMP_LEAVES, 0.5F, "the dried kelp block's rate");
        context.complete();
    }

    private static void assertComposts(TestContext context, Block block, float expected, String why) {
        float actual = ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.getFloat(block.asItem());
        context.assertTrue(actual > 0.0F,
                block.asItem() + " has no composting chance at all — a composter will refuse it");
        context.assertTrue(Math.abs(actual - expected) < 0.0001F,
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
    public static void shearsMineHempLeavesFast(TestContext context) {
        var leaves = ModBlocks.HEMP_LEAVES.getDefaultState();

        context.assertTrue(leaves.isIn(BlockTags.LEAVES),
                "hemp_leaves is not in #minecraft:leaves, so shears, hoes and swords all treat it as stone");

        float shears = new ItemStack(Items.SHEARS).getMiningSpeedMultiplier(leaves);
        context.assertTrue(shears > 1.0F,
                "shears mine hemp_leaves at " + shears + "x — no faster than a bare hand");

        float pickaxe = new ItemStack(Items.DIAMOND_PICKAXE).getMiningSpeedMultiplier(leaves);
        context.assertTrue(shears > pickaxe,
                "a diamond pickaxe (" + pickaxe + "x) matches or beats shears (" + shears
                        + "x) on hemp_leaves, which is not how any leaf block behaves");

        context.complete();
    }

    /**
     * A tamed llama wears the hemp carpet, and so does a trader's; a horse does not.
     *
     * <p>On 1.21.11 a llama asks the carpet's own equip data whether it may wear it — the
     * {@code #minecraft:wool_carpets} tag the carpet is in no longer decides anything there — so
     * a carpet without that data is simply refused, with nothing in any log. That is how the hemp
     * carpet shipped in 2.0.1-beta. The horse is the negative, so this cannot pass against equip data
     * that lets anything wear it.
     */
    public static void aLlamaWearsHempCarpet(TestContext context) {
        ItemStack carpet = new ItemStack(ModBlocks.HEMP_CARPET);
        LlamaEntity llama = context.spawnEntity(EntityType.LLAMA, new BlockPos(1, 1, 1));
        llama.setTame(true);
        context.assertTrue(llama.canEquip(carpet, EquipmentSlot.BODY), "a tamed llama will not wear hemp carpet");
        TraderLlamaEntity trader = context.spawnEntity(EntityType.TRADER_LLAMA, new BlockPos(3, 1, 1));
        trader.setTame(true);
        context.assertTrue(trader.canEquip(carpet, EquipmentSlot.BODY), "a trader llama will not wear hemp carpet");
        HorseEntity horse = context.spawnEntity(EntityType.HORSE, new BlockPos(5, 1, 1));
        horse.setTame(true);
        context.assertFalse(horse.canEquip(carpet, EquipmentSlot.BODY),
                "a horse will wear hemp carpet, so its equip data lets in more than llamas");
        context.complete();
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
    public static void everyBlockHasALootTable(TestContext context) {
        var loot = context.getWorld().getServer().getReloadableRegistries();
        List<String> missing = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            if (!Registries.BLOCK.getId(block).getNamespace().equals(Hempdustry.MOD_ID)) {
                continue;
            }
            if (block.getLootTableKey().map(loot::getLootTable).orElse(LootTable.EMPTY) == LootTable.EMPTY) {
                missing.add(Registries.BLOCK.getId(block).getPath());
            }
        }
        context.assertTrue(missing.isEmpty(), "these blocks have no loot table and drop nothing: " + missing);
        context.complete();
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
    public static void pottedPlantsAndCropsHaveNoItem(TestContext context) {
        List<String> items = new ArrayList<>();
        for (Block block : Registries.BLOCK) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id.getNamespace().equals(Hempdustry.MOD_ID)
                    && (block instanceof FlowerPotBlock || block instanceof CropBlock)
                    && Registries.ITEM.containsId(id)) {
                items.add(id.getPath());
            }
        }
        context.assertTrue(items.isEmpty(), "these potted plants or crops have an item, which vanilla's never do: " + items);
        context.complete();
    }

    /**
     * Hemp fibre repairs every piece of the hemp set on an anvil, and string does not.
     *
     * <p>Since 1.21.4 an armour material names its repair material as a tag. The set named
     * {@code #hempdustry:hemp_armor_repair} from the 1.21.11 port onwards, but no file ever filled it,
     * so the anvil refused everything and nothing said why.
     */
    public static void hempFiberRepairsTheOutfit(TestContext context) {
        ItemStack fiber = new ItemStack(ModItems.HEMP_FIBER);
        for (Item piece : List.of(ModItems.HEMP_BEANIE, ModItems.HEMP_SHIRT, ModItems.HEMP_HAREM_PANTS,
                ModItems.FLIP_FLOPS)) {
            context.assertTrue(new ItemStack(piece).canRepairWith(fiber),
                    "hemp fibre does not repair " + piece + " — #hempdustry:hemp_armor_repair is empty or gone");
            context.assertFalse(new ItemStack(piece).canRepairWith(new ItemStack(Items.STRING)),
                    "string repairs " + piece + ", so the repair tag has grown past hemp fibre");
        }
        context.complete();
    }

    /**
     * Only the shirt and the harem pants take a trim, and all four pieces still take armour's
     * enchantments, on an anvil and at the table.
     *
     * <p>The beanie and the flip-flops are drawn by models of their own, so a trim on either would
     * never show. Vanilla builds {@code #trimmable_armor} from {@code #head_armor} and
     * {@code #foot_armor}, which is why those two reach the enchantable tags one by one instead of
     * through the base tags; putting them back there would put them back on the smithing table.
     */
    public static void onlyTheGarmentsTakeATrim(TestContext context) {
        Registry<Enchantment> enchantments = context.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        Enchantment protection = enchantments.getOrThrow(Enchantments.PROTECTION).value();
        for (Item piece : List.of(ModItems.HEMP_BEANIE, ModItems.HEMP_SHIRT, ModItems.HEMP_HAREM_PANTS,
                ModItems.FLIP_FLOPS)) {
            ItemStack stack = new ItemStack(piece);
            boolean garment = piece == ModItems.HEMP_SHIRT || piece == ModItems.HEMP_HAREM_PANTS;
            context.assertTrue(stack.isIn(ItemTags.TRIMMABLE_ARMOR) == garment, garment
                    ? piece + " is no longer in #minecraft:trimmable_armor"
                    : piece + " takes a trim at the smithing table, which its own model never draws");
            for (RegistryKey<Enchantment> key : List.of(Enchantments.PROTECTION, Enchantments.UNBREAKING,
                    Enchantments.MENDING, Enchantments.BINDING_CURSE, Enchantments.VANISHING_CURSE)) {
                context.assertTrue(enchantments.getOrThrow(key).value().isAcceptableItem(stack),
                        piece + " refuses " + key.getValue() + " on an anvil");
            }
            context.assertTrue(protection.isPrimaryItem(stack), piece + " is offered no Protection at the enchanting table");
        }
        context.assertTrue(enchantments.getOrThrow(Enchantments.RESPIRATION).value()
                .isAcceptableItem(new ItemStack(ModItems.HEMP_BEANIE)), "the beanie refuses Respiration");
        context.assertTrue(enchantments.getOrThrow(Enchantments.FEATHER_FALLING).value()
                .isAcceptableItem(new ItemStack(ModItems.FLIP_FLOPS)), "the flip-flops refuse Feather Falling");
        context.complete();
    }

    /**
     * Every damageable smokeable takes Unbreaking and Mending: each device and each coloured bong.
     * The tag was once listed by hand, and the vaporizer, added later, was never in it.
     */
    public static void everyDeviceTakesUnbreaking(TestContext context) {
        Registry<Enchantment> enchantments = context.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        List<Item> devices = new ArrayList<>(ModItems.devices().values());
        devices.addAll(ModItems.COLORED_BONGS);
        for (Item device : devices) {
            for (RegistryKey<Enchantment> key : List.of(Enchantments.UNBREAKING, Enchantments.MENDING)) {
                context.assertTrue(enchantments.getOrThrow(key).value().isAcceptableItem(new ItemStack(device)),
                        device + " refuses " + key.getValue() + " — it is missing from #minecraft:enchantable/durability");
            }
        }
        context.complete();
    }
}
