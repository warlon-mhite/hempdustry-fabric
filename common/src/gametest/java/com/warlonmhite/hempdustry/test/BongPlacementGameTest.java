package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.block.custom.BongBlock;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.ModStrains;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Putting a bong down must not cost the device anything, and a packed one must not go down at all.
 *
 * <p>Both fail quietly. A placed bong that lost its components comes back as a fresh one — an
 * enchanted, anvil-named bong turned plain by being set on a table, and nothing on screen says
 * why. A packed bong that placed would eat its bowl the moment the player aimed at the floor.
 * Driven through the real interaction manager, since placement lives in {@code useOn} and
 * only the real click decides between it and {@code use}.
 */
public final class BongPlacementGameTest {

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void bongStandsAndComesBackWhole(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        ServerPlayer player = context.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Item red = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hempdustry", "red_bong"));

        ItemStack device = new ItemStack(red);
        device.setDamageValue(5);
        device.set(DataComponents.CUSTOM_NAME, Component.literal("Old Faithful"));
        device.enchant(world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING), 2);
        ItemStack expected = device.copy();

        BlockPos floor = new BlockPos(0, 1, 0);
        context.setBlock(floor, Blocks.STONE);
        click(player, world, context.absolutePos(floor), device);
        BlockPos stood = context.absolutePos(floor.above());
        BlockState state = world.getBlockState(stood);
        context.assertTrue(state.getBlock() instanceof BongBlock, "an empty bong did not stand on the floor");
        context.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "placing the bong did not use it up");
        context.assertTrue(!state.getValue(BongBlock.PACKED), "an empty bong stood as a packed one");

        // Through a save: what a chunk unload and reload does to it.
        BlockEntity placed = world.getBlockEntity(stood);
        CompoundTag saved = placed.saveWithFullMetadata(world.registryAccess());
        BlockEntity reloaded = BlockEntity.loadStatic(stood, state, saved, world.registryAccess());
        context.assertTrue(reloaded != null && reloaded.components().has(DataComponents.ENCHANTMENTS),
                "the placed bong's components did not survive a save");

        List<ItemStack> drops = Block.getDrops(state, world, stood, reloaded);
        context.assertTrue(drops.size() == 1, "a broken bong dropped " + drops.size() + " stacks, not itself");
        ItemStack back = drops.getFirst();
        context.assertTrue(ItemStack.isSameItemSameComponents(back, expected),
                "the bong came back as " + back.getComponentsPatch() + ", not as it went down: "
                        + expected.getComponentsPatch());

        // A packed bong never places: right-click on the floor is a hit, and the floor stays bare.
        BlockPos otherFloor = new BlockPos(2, 1, 0);
        context.setBlock(otherFloor, Blocks.STONE);
        ItemStack packed = new ItemStack(ModItems.BONG);
        packed.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(world.registryAccess()
                .lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA), 1));
        packed.set(ModComponents.CHARGES, DeviceType.BONG.builtInBowl().hits());
        click(player, world, context.absolutePos(otherFloor), packed);
        context.assertTrue(world.getBlockState(context.absolutePos(otherFloor.above())).isAir(),
                "a packed bong was set down, bowl and all");

        // Nor does a pipe, which has no block to stand as.
        ItemStack pipe = new ItemStack(ModItems.WOODEN_PIPE);
        click(player, world, context.absolutePos(otherFloor), pipe);
        context.assertTrue(world.getBlockState(context.absolutePos(otherFloor.above())).isAir(),
                "a pipe was placed as a block");

        // Sneaking, a packed bong does go down -- for show, bowl and all -- and comes back packed.
        BlockPos shelf = new BlockPos(4, 1, 0);
        context.setBlock(shelf, Blocks.STONE);
        ItemStack loaded = new ItemStack(ModItems.BONG);
        loaded.set(ModComponents.SMOKE_CONTENTS, SmokeContents.of(world.registryAccess()
                .lookupOrThrow(Strain.REGISTRY_KEY).getOrThrow(ModStrains.INDICA), 3));
        loaded.set(ModComponents.CHARGES, 2);
        ItemStack loadedBefore = loaded.copy();
        player.setShiftKeyDown(true);
        click(player, world, context.absolutePos(shelf), loaded);
        player.setShiftKeyDown(false);
        BlockPos shown = context.absolutePos(shelf.above());
        BlockState shownState = world.getBlockState(shown);
        context.assertTrue(shownState.getBlock() instanceof BongBlock, "sneaking did not set a packed bong down");
        context.assertTrue(shownState.getValue(BongBlock.PACKED), "a packed bong stood as an empty one");
        List<ItemStack> shownDrops = Block.getDrops(shownState, world, shown, world.getBlockEntity(shown));
        context.assertTrue(shownDrops.size() == 1
                        && ItemStack.isSameItemSameComponents(shownDrops.getFirst(), loadedBefore),
                "a packed bong did not come back with its bowl: " + shownDrops);
        context.succeed();
    }

    private static void click(ServerPlayer player, ServerLevel world, BlockPos floor, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        player.gameMode.useItemOn(player, world, stack, InteractionHand.MAIN_HAND, hit);
    }
}
