package com.warlonmhite.hempdustry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

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
 * <p>Amending the state the bee writes is the narrowest fix: it leaves the bee's decision to grow,
 * and the age it grows to, entirely alone, and only restores state that vanilla discarded because it
 * has no concept of it. {@link Defoliation#carryOver} no-ops on any state without the properties, so
 * every other crop in the game — vanilla or modded — passes through untouched.
 *
 * <h2>Why {@code @WrapOperation} and not {@code @Redirect}</h2>
 *
 * There is exactly one {@code setBlockState} call in {@code tick()}, so either would find its target
 * unambiguously. But <b>{@code @Redirect} is exclusive</b>: it replaces the call outright, so a
 * second mod redirecting the same instruction is an unresolvable conflict, and with
 * {@code defaultRequire: 1} the result is a <em>startup crash</em> for the whole pack rather than one
 * feature quietly not working. Bee-behaviour mods are common in the kitchen-sink packs this mod is
 * meant to live in, and losing that coin flip costs the player their world, not their defoliation.
 *
 * <p>{@code @WrapOperation} wraps the call instead of replacing it: several mods can wrap the same
 * instruction and they compose, each seeing the previous one's arguments. Passing the amended state
 * to {@link Operation#call} rather than calling {@code world.setBlockState} directly is what keeps
 * that chain intact — a direct call would jump the queue and skip every wrapper underneath. Ships
 * inside Fabric Loader (0.15+; this mod requires 0.16.5), so it costs no new dependency.
 */
@Mixin(targets = "net.minecraft.entity.passive.BeeEntity$GrowCropsGoal")
public class BeeGrowCropsGoalMixin {
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"
            )
    )
    private boolean hempdustry$preserveDefoliation(World world, BlockPos pos, BlockState newState,
                                                   Operation<Boolean> original) {
        return original.call(world, pos, Defoliation.carryOver(world.getBlockState(pos), newState));
    }
}
