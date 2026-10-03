package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.List;

/**
 * The hemp outfit: what each piece does beyond its armour points. Every one of these is a tag entry
 * or a map entry that fails without a sound — a beanie that lets you freeze, a shirt the dye recipe
 * ignores, a cauldron that will not wash it, a set bonus that never fires.
 */
public final class HempOutfitGameTest {

    /**
     * The beanie alone keeps a player from freezing, and the other three pieces do not.
     *
     * <p>{@code canFreeze} is the whole of vanilla's check: it walks the armour slots for anything in
     * {@code #minecraft:freeze_immune_wearables}. The second half guards the other way, so the
     * flip-flops cannot drift into the tag along with the beanie.
     */
    public static void onlyTheBeanieKeepsOutTheCold(TestContext context) {
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        player.equipStack(EquipmentSlot.HEAD, new ItemStack(ModItems.HEMP_BEANIE));
        context.assertFalse(player.canFreeze(), "a player in a hemp beanie still freezes in powder snow");

        player.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.equipStack(EquipmentSlot.CHEST, new ItemStack(ModItems.HEMP_SHIRT));
        player.equipStack(EquipmentSlot.LEGS, new ItemStack(ModItems.HEMP_HAREM_PANTS));
        player.equipStack(EquipmentSlot.FEET, new ItemStack(ModItems.FLIP_FLOPS));
        context.assertTrue(player.canFreeze(),
                "the shirt, the harem pants or the flip-flops keep out the cold; only the beanie should");
        context.complete();
    }

    /**
     * The shirt and the harem pants take dye through vanilla's own armour-dye recipe and wash out in
     * a water cauldron; the beanie and the flip-flops take none. The wash is a real click on a real
     * cauldron, so the entry has to be in the map the cauldron actually reads.
     */
    public static void theGarmentsTakeDyeAndWashOut(TestContext context) {
        ServerWorld world = context.getWorld();
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        BlockPos cauldron = new BlockPos(1, 1, 1);
        BlockPos pos = context.getAbsolutePos(cauldron);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);

        for (Item garment : List.of(ModItems.HEMP_SHIRT, ModItems.HEMP_HAREM_PANTS)) {
            ItemStack dyed = dye(world, garment);
            DyedColorComponent colour = dyed.get(DataComponentTypes.DYED_COLOR);
            // The dye's colour carries full alpha and the component does not, so compare RGB only.
            context.assertTrue(dyed.isOf(garment) && colour != null
                            && (colour.rgb() & 0xFFFFFF) == (DyeColor.GREEN.getEntityColor() & 0xFFFFFF),
                    garment + " and green dye made " + dyed + " (" + colour + "), not a green " + garment);

            context.setBlockState(cauldron, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3));
            world.getBlockState(pos).onUseWithItem(dyed, world, player, Hand.MAIN_HAND, hit);
            context.assertFalse(dyed.contains(DataComponentTypes.DYED_COLOR),
                    "a water cauldron did not wash the dye out of " + garment);
            context.assertEquals(2, context.getBlockState(cauldron).get(LeveledCauldronBlock.LEVEL),
                    "the cauldron's water level after washing " + garment);
        }
        for (Item kept : List.of(ModItems.HEMP_BEANIE, ModItems.FLIP_FLOPS)) {
            context.assertTrue(dye(world, kept).isEmpty(), kept + " took dye; only the shirt and pants should");
        }
        context.complete();
    }

    /**
     * All four pieces double the harsh-smoke 1-in-N, so a bong's 1-in-3 cough becomes 1-in-6; three
     * pieces do nothing. Zero stays zero (a roll a server switched off stays off), and a huge
     * datapack value saturates instead of wrapping negative.
     */
    public static void aFullOutfitEasesTheHarshSmoke(TestContext context) {
        PlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);
        player.equipStack(EquipmentSlot.HEAD, new ItemStack(ModItems.HEMP_BEANIE));
        player.equipStack(EquipmentSlot.CHEST, new ItemStack(ModItems.HEMP_SHIRT));
        player.equipStack(EquipmentSlot.LEGS, new ItemStack(ModItems.HEMP_HAREM_PANTS));
        context.assertEquals(3, Smoking.easedByOutfit(player, 3), "a 1-in-3 roll under three pieces of the outfit");

        player.equipStack(EquipmentSlot.FEET, new ItemStack(ModItems.FLIP_FLOPS));
        context.assertEquals(6, Smoking.easedByOutfit(player, 3), "a 1-in-3 roll under the full outfit");
        context.assertEquals(0, Smoking.easedByOutfit(player, 0), "a switched-off roll under the full outfit");
        context.assertEquals(Integer.MAX_VALUE, Smoking.easedByOutfit(player, Integer.MAX_VALUE),
                "the largest 1-in-N under the full outfit");
        context.complete();
    }

    /** {@code item} beside a green dye in the crafting grid, crafted; empty if nothing matched. */
    private static ItemStack dye(ServerWorld world, Item item) {
        CraftingRecipeInput grid = CraftingRecipeInput.create(2, 1,
                List.of(new ItemStack(item), new ItemStack(Items.GREEN_DYE)));
        return world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, grid, world)
                .map(entry -> entry.value().craft(grid, world.getRegistryManager()))
                .orElse(ItemStack.EMPTY);
    }
}
