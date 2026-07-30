package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.block.custom.Defoliation;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Stops a pollinating bee from wiping a hemp plant's defoliation state.
 *
 * <p>{@code BeeEntity.GrowCropsGoal} fertilises any {@code CropBlock} in
 * {@code #minecraft:bee_growables} with, in effect:
 *
 * <pre>{@code if (!crop.isMature(state)) world.setBlockState(pos, crop.withAge(crop.getAge(state) + 1));}</pre>
 *
 * <p>Two things there are hostile to a crop that keeps state of its own. The write is <b>direct</b>,
 * so it never runs the crop's own growth path, and {@code CropBlock#withAge} is
 * {@code getDefaultState().with(AGE, n)} — it rebuilds the state from scratch and drops every other
 * property back to its default. Our crops already dodge the half of this that would behead a tall
 * plant, by overriding {@code getAge} so only the LOWER segment is ever a bee's target (see
 * CLAUDE.md's bee/{@code withAge} note). But the LOWER is exactly where
 * {@link Defoliation#TRIMMED_EARLY}/{@link Defoliation#TRIMMED_LATE} live, so a bee flying over a
 * plant the player had just sheared would silently reset it to untrimmed — no message, no particle,
 * nothing to tell them the trip was wasted.
 *
 * <p>Redirecting the write and copying the flags forward is the narrowest fix: it leaves the bee's
 * decision to grow, and the age it grows to, entirely alone, and only restores state that vanilla
 * discarded because it has no concept of it. {@link Defoliation#carryOver} no-ops on any state
 * without the properties, so every other crop in the game — vanilla or modded — passes through
 * untouched.
 *
 * <p>There is exactly one {@code setBlockState} call in {@code tick()}, so the redirect is
 * unambiguous. If it ever collides with another mod, the MixinExtras {@code @WrapOperation}
 * equivalent bundled with Fabric Loader is the drop-in replacement.
 */
@Mixin(targets = "net.minecraft.entity.passive.BeeEntity$GrowCropsGoal")
public class BeeGrowCropsGoalMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
            )
    )
    private boolean hempdustry$preserveDefoliation(World world, BlockPos pos, BlockState newState) {
        return world.setBlockState(pos, Defoliation.carryOver(world.getBlockState(pos), newState));
    }
}
