package com.warlonmhite.hempdustry.test;

import net.minecraft.world.item.DyeableLeatherItem;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.Smoking;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void onlyTheBeanieKeepsOutTheCold(GameTestHelper context) {
        Player player = Line.mockPlayer(context, GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HEMP_BEANIE));
        context.assertFalse(player.canFreeze(), "a player in a hemp beanie still freezes in powder snow");

        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HEMP_SHIRT));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.HEMP_HAREM_PANTS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.FLIP_FLOPS));
        context.assertTrue(player.canFreeze(),
                "the shirt, the harem pants or the flip-flops keep out the cold; only the beanie should");
        context.succeed();
    }

    /**
     * The shirt and the harem pants take dye through vanilla's own armour-dye recipe and wash out in
     * a water cauldron; the beanie and the flip-flops take none. The wash is a real click on a real
     * cauldron, so the entry has to be in the map the cauldron actually reads.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void theGarmentsTakeDyeAndWashOut(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Player player = Line.mockPlayer(context, GameType.SURVIVAL);
        BlockPos cauldron = new BlockPos(1, 1, 1);
        BlockPos pos = context.absolutePos(cauldron);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);

        for (Item garment : List.of(ModItems.HEMP_SHIRT, ModItems.HEMP_HAREM_PANTS)) {
            ItemStack dyed = dye(world, garment);
            DyeableLeatherItem cloth = (DyeableLeatherItem) garment;
            // One dye on undyed cloth is that dye's colour, as vanilla's armour-dye recipe mixes it.
            float[] green = DyeColor.GREEN.getTextureDiffuseColors();
            int expected = ((int) (green[0] * 255.0F) << 16) | ((int) (green[1] * 255.0F) << 8) | (int) (green[2] * 255.0F);
            context.assertTrue(dyed.is(garment) && cloth.hasCustomColor(dyed) && cloth.getColor(dyed) == expected,
                    garment + " and green dye made " + dyed + " (" + Integer.toHexString(cloth.getColor(dyed))
                            + "), not a green " + garment);

            context.setBlock(cauldron, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            Line.useItemOn(world.getBlockState(pos),dyed, world, player, InteractionHand.MAIN_HAND, hit);
            context.assertFalse(cloth.hasCustomColor(dyed),
                    "a water cauldron did not wash the dye out of " + garment);
            Line.equal(context, context.getBlockState(cauldron).getValue(LayeredCauldronBlock.LEVEL), 2,
                    "the cauldron's water level after washing " + garment);
        }
        for (Item kept : List.of(ModItems.HEMP_BEANIE, ModItems.FLIP_FLOPS)) {
            context.assertTrue(dye(world, kept).isEmpty(), kept + " took dye; only the shirt and pants should");
        }
        context.succeed();
    }

    /**
     * All four pieces double the harsh-smoke 1-in-N, so a bong's 1-in-3 cough becomes 1-in-6; three
     * pieces do nothing. Zero stays zero (a roll a server switched off stays off), and a huge
     * datapack value saturates instead of wrapping negative.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void aFullOutfitEasesTheHarshSmoke(GameTestHelper context) {
        Player player = Line.mockPlayer(context, GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HEMP_BEANIE));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HEMP_SHIRT));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.HEMP_HAREM_PANTS));
        Line.equal(context, Smoking.easedByOutfit(player, 3), 3, "a 1-in-3 roll under three pieces of the outfit");

        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.FLIP_FLOPS));
        Line.equal(context, Smoking.easedByOutfit(player, 3), 6, "a 1-in-3 roll under the full outfit");
        Line.equal(context, Smoking.easedByOutfit(player, 0), 0, "a switched-off roll under the full outfit");
        Line.equal(context, Smoking.easedByOutfit(player, Integer.MAX_VALUE), Integer.MAX_VALUE,
                "the largest 1-in-N under the full outfit");
        context.succeed();
    }

    /** {@code item} beside a green dye in the crafting grid, crafted; empty if nothing matched. */
    private static ItemStack dye(ServerLevel world, Item item) {
        CraftingInput grid = CraftingInput.of(2, 1,
                List.of(new ItemStack(item), new ItemStack(Items.GREEN_DYE)));
        return world.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, world)
                .map(entry -> entry.assemble(grid, world.registryAccess()))
                .orElse(ItemStack.EMPTY);
    }
}
