package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A Space Cake — vanilla's cake, baked with cannabutter. Seven bites, same shapes, same comparator
 * output; the only mechanical difference for now is that it refuses candles.
 *
 * <p><b>Why the candle override is not optional.</b> {@link CakeBlock#onUseWithItem} matches the
 * held item against {@code CandleCakeBlock.getCandleCake(...)} and, on a hit, <em>replaces the block
 * outright</em> with a vanilla candle cake. Inherited unchanged that would let a player quietly
 * convert a Space Cake into an ordinary candle cake and lose it — a duplication of vanilla content
 * and a destruction of ours in one click. Refusing the interaction and falling through to the eat
 * path is the cheap fix; a real {@code space_cake_with_candle} family would be sixteen more blocks
 * for a decoration nobody asked for.
 *
 * <p>Eating is still vanilla's: 2 hunger and 0.4 saturation per bite, and only when actually hungry.
 * What a Space Cake <em>does</em> beyond feeding you belongs to the effects pass — see CLAUDE.md
 * §5 #14.
 */
public class SpaceCakeBlock extends CakeBlock {
    public SpaceCakeBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                             PlayerEntity player, Hand hand, BlockHitResult hit) {
        // Deliberately never the candle branch. Falling through to onUse means a player holding any
        // item still gets to eat, which is what they meant by right-clicking a cake.
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
