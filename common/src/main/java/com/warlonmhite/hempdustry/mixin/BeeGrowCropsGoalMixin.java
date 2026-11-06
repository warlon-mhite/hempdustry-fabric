package com.warlonmhite.hempdustry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowMedium;
import com.warlonmhite.hempdustry.block.custom.PlantStress;
import com.warlonmhite.hempdustry.block.custom.TallHemp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops a pollinating bee from wiping a hemp plant's defoliation state.
 *
 * <p>{@code Bee.BeeGrowCropGoal} fertilises any {@code CropBlock} in
 * {@code #minecraft:bee_growables} with, in effect:
 *
 * <pre>{@code if (!crop.isMaxAge(state)) world.setBlockAndUpdate(pos, crop.getStateForAge(crop.getAge(state) + 1));}</pre>
 *
 * <p>Two things there are hostile to a crop that keeps state of its own. The write is <b>direct</b>,
 * so it never runs the crop's own growth path, and {@code CropBlock#getStateForAge} is
 * {@code defaultBlockState().setValue(AGE, n)} — it rebuilds the state from scratch and drops every other
 * property back to its default. On a tall plant's upper blocks that rebuild would turn a segment into a
 * LOWER in mid-air and behead the plant, so {@link #hempdustry$onlyTheRoot} makes every segment above
 * the root look ripe to the bee — and only to the bee: other mods' harvesters ask the same
 * {@code isMaxAge} and must get the truth, or they cut the top off a growing plant. But the LOWER is
 * exactly where
 * {@link Defoliation#TRIMMED_EARLY}/{@link Defoliation#TRIMMED_LATE} live, so a bee flying over a
 * plant the player had just sheared would silently reset it to untrimmed — no message, no particle,
 * nothing to tell them the trip was wasted.
 *
 * <p>Amending the state the bee writes is the narrowest fix: it leaves the bee's decision to grow,
 * and the age it grows to, entirely alone, and only restores state that vanilla discarded because it
 * has no concept of it. {@link Defoliation#carryOver} no-ops on any state without the properties, so
 * every other crop in the game — vanilla or modded — passes through untouched.
 *
 * <p>The same wrap makes a bee's step count as a growth step for our plants: it reads the light and
 * rolls for stress like any other ({@link PlantStress#beeStep}), and a plant a bee ripens drinks and
 * eats from its bed, so a bee can neither carry a dry field through its flowering nor ripen a potted
 * plant for free.
 *
 * <h2>Why {@code @WrapOperation} and not {@code @Redirect}</h2>
 *
 * There is exactly one {@code setBlockAndUpdate} call in {@code tick()}, so either would find its target
 * unambiguously. But <b>{@code @Redirect} is exclusive</b>: it replaces the call outright, so a
 * second mod redirecting the same instruction is an unresolvable conflict, and with
 * {@code defaultRequire: 1} the result is a <em>startup crash</em> for the whole pack rather than one
 * feature quietly not working. Bee-behaviour mods are common in the kitchen-sink packs this mod is
 * meant to live in, and losing that coin flip costs the player their world, not their defoliation.
 *
 * <p>{@code @WrapOperation} wraps the call instead of replacing it: several mods can wrap the same
 * instruction and they compose, each seeing the previous one's arguments. Passing the amended state
 * to {@link Operation#call} rather than calling {@code world.setBlockAndUpdate} directly is what keeps
 * that chain intact — a direct call would jump the queue and skip every wrapper underneath. Ships
 * inside Fabric Loader (0.15 and later) and NeoForge alike, so it costs no new dependency.
 */
@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeGrowCropGoal")
public class BeeGrowCropsGoalMixin {
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean hempdustry$preserveDefoliation(Level world, BlockPos pos, BlockState newState,
                                                   Operation<Boolean> original) {
        BlockState current = world.getBlockState(pos);
        BlockState next = PlantStress.beeStep(world, pos, current, Defoliation.carryOver(current, newState),
                world.getRandom());
        boolean set = original.call(world, pos, next);
        if (set && next.getBlock() instanceof CropBlock crop && current.is(crop)
                && crop.isMaxAge(next) && !crop.isMaxAge(current)) {
            BlockPos floorPos = pos.below();
            BlockState floor = world.getBlockState(floorPos);
            if (floor.getBlock() instanceof GrowMedium medium) {
                medium.spend(world, floorPos, floor);
            }
        }
        return set;
    }

    /**
     * The bee's "is there anything left to grow here?". A segment above a hemp plant's root answers yes,
     * it is ripe, so the bee leaves it alone exactly as it always did, and moves on to the block below.
     * Only the bee is told this: the segment itself reports its real age to everyone else.
     */
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/CropBlock;isMaxAge(Lnet/minecraft/world/level/block/state/BlockState;)Z"
            )
    )
    private boolean hempdustry$onlyTheRoot(CropBlock crop, BlockState state, Operation<Boolean> original) {
        return !TallHemp.isRoot(state) || original.call(crop, state);
    }
}
