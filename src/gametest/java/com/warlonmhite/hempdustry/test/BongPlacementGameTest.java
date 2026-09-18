package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.custom.BongBlock;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.List;

/**
 * Putting a bong down must not cost the device anything, and a packed one must not go down at all.
 *
 * <p>Both fail quietly. A placed bong that lost its components comes back as a fresh one — an
 * enchanted, anvil-named bong turned plain by being set on a table, and nothing on screen says
 * why. A packed bong that placed would eat its bowl the moment the player aimed at the floor.
 * Driven through the real interaction manager, since placement lives in {@code useOnBlock} and
 * only the real click decides between it and {@code use}.
 */
public final class BongPlacementGameTest {

    public static void bongStandsAndComesBackWhole(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);
        Item red = net.minecraft.registry.Registries.ITEM.get(
                net.minecraft.util.Identifier.of("hempdustry", "red_bong"));

        ItemStack device = new ItemStack(red);
        device.setDamage(5);
        device.set(DataComponentTypes.CUSTOM_NAME, Text.literal("Old Faithful"));
        device.addEnchantment(world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING), 2);
        ItemStack expected = device.copy();

        BlockPos floor = new BlockPos(0, 1, 0);
        context.setBlockState(floor, Blocks.STONE);
        click(player, world, context.getAbsolutePos(floor), device);
        BlockPos stood = context.getAbsolutePos(floor.up());
        BlockState state = world.getBlockState(stood);
        context.assertTrue(state.getBlock() instanceof BongBlock, "an empty bong did not stand on the floor");
        context.assertTrue(player.getStackInHand(Hand.MAIN_HAND).isEmpty(), "placing the bong did not use it up");
        context.assertTrue(!state.get(BongBlock.PACKED), "an empty bong stood as a packed one");

        // Through a save: what a chunk unload and reload does to it.
        BlockEntity placed = world.getBlockEntity(stood);
        NbtCompound saved = placed.createNbtWithIdentifyingData(world.getRegistryManager());
        BlockEntity reloaded = BlockEntity.createFromNbt(stood, state, saved, world.getRegistryManager());
        context.assertTrue(reloaded != null && reloaded.getComponents().contains(DataComponentTypes.ENCHANTMENTS),
                "the placed bong's components did not survive a save");

        List<ItemStack> drops = Block.getDroppedStacks(state, world, stood, reloaded);
        context.assertTrue(drops.size() == 1, "a broken bong dropped " + drops.size() + " stacks, not itself");
        ItemStack back = drops.getFirst();
        context.assertTrue(ItemStack.areItemsAndComponentsEqual(back, expected),
                "the bong came back as " + back.getComponentChanges() + ", not as it went down: "
                        + expected.getComponentChanges());

        // A packed bong never places: right-click on the floor is a hit, and the floor stays bare.
        BlockPos otherFloor = new BlockPos(2, 1, 0);
        context.setBlockState(otherFloor, Blocks.STONE);
        ItemStack packed = new ItemStack(ModItems.BONG);
        packed.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(world.getRegistryManager()
                .getOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA), 1));
        packed.set(ModComponents.CHARGES, DeviceType.BONG.bowlSize());
        click(player, world, context.getAbsolutePos(otherFloor), packed);
        context.assertTrue(world.getBlockState(context.getAbsolutePos(otherFloor.up())).isAir(),
                "a packed bong was set down, bowl and all");

        // Nor does a pipe, which has no block to stand as.
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        click(player, world, context.getAbsolutePos(otherFloor), pipe);
        context.assertTrue(world.getBlockState(context.getAbsolutePos(otherFloor.up())).isAir(),
                "a pipe was placed as a block");

        // Sneaking, a packed bong does go down -- for show, bowl and all -- and comes back packed.
        BlockPos shelf = new BlockPos(4, 1, 0);
        context.setBlockState(shelf, Blocks.STONE);
        ItemStack loaded = new ItemStack(ModItems.BONG);
        loaded.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(world.getRegistryManager()
                .getOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA), 3));
        loaded.set(ModComponents.CHARGES, 2);
        ItemStack loadedBefore = loaded.copy();
        player.setSneaking(true);
        click(player, world, context.getAbsolutePos(shelf), loaded);
        player.setSneaking(false);
        BlockPos shown = context.getAbsolutePos(shelf.up());
        BlockState shownState = world.getBlockState(shown);
        context.assertTrue(shownState.getBlock() instanceof BongBlock, "sneaking did not set a packed bong down");
        context.assertTrue(shownState.get(BongBlock.PACKED), "a packed bong stood as an empty one");
        List<ItemStack> shownDrops = Block.getDroppedStacks(shownState, world, shown, world.getBlockEntity(shown));
        context.assertTrue(shownDrops.size() == 1
                        && ItemStack.areItemsAndComponentsEqual(shownDrops.getFirst(), loadedBefore),
                "a packed bong did not come back with its bowl: " + shownDrops);
        context.complete();
    }

    private static void click(ServerPlayerEntity player, ServerWorld world, BlockPos floor, ItemStack stack) {
        player.setStackInHand(Hand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        player.interactionManager.interactBlock(player, world, stack, Hand.MAIN_HAND, hit);
    }
}
