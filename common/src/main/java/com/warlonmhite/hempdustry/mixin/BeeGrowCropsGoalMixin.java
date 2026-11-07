package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.block.custom.Defoliation;
import com.warlonmhite.hempdustry.block.custom.GrowMedium;
import com.warlonmhite.hempdustry.block.custom.PlantStress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
 * property back to its default. Our crops already dodge the half of this that would behead a tall
 * plant, by overriding {@code getAge} so only the LOWER segment is ever a bee's target (see
 * {@code IndicaCropBlock#getAge}). But the LOWER is exactly where
 * {@link Defoliation#TRIMMED_EARLY}/{@link Defoliation#TRIMMED_LATE} live, so a bee flying over a
 * plant the player had just sheared would silently reset it to untrimmed — no message, no particle,
 * nothing to tell them the trip was wasted.
 *
 * <p>Amending the state the bee writes is the narrowest fix: it leaves the bee's decision to grow,
 * and the age it grows to, entirely alone, and only restores state that vanilla discarded because it
 * has no concept of it. {@link Defoliation#carryOver} no-ops on any state without the properties, so
 * every other crop in the game — vanilla or modded — passes through untouched.
 *
 * <p>The same redirect makes a bee's step count as a growth step for our plants: it reads the light and
 * rolls for stress like any other ({@link PlantStress#beeStep}), and a plant a bee ripens drinks and
 * eats from its bed, so a bee can neither carry a dry field through its flowering nor ripen a potted
 * plant for free.
 *
 * <h2>Why a {@code @Redirect} with {@code require = 0}</h2>
 *
 * There is exactly one {@code setBlockAndUpdate} call in {@code tick()}, so the target is
 * unambiguous. The newer lines wrap it with MixinExtras' {@code @WrapOperation}, which composes with
 * other mods'; Forge 36 ships no MixinExtras and has no jar-in-jar to carry it, and a plain
 * {@code @ModifyArg} cannot see the level the bee writes into. So this is a {@code @Redirect}, which
 * is exclusive: a second mod redirecting the same call wins or loses outright. {@code require = 0} is
 * what keeps that from being a startup crash for the whole pack -- the mod that loses is skipped with
 * a warning, and the worst case is a bee resetting a sheared plant in that one pack.
 */
@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeGrowCropGoal")
public class BeeGrowCropsGoalMixin {
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"
            ),
            require = 0
    )
    private boolean hempdustry$preserveDefoliation(Level world, BlockPos pos, BlockState newState) {
        BlockState current = world.getBlockState(pos);
        BlockState next = PlantStress.beeStep(world, pos, current, Defoliation.carryOver(current, newState),
                world.getRandom());
        boolean set = world.setBlockAndUpdate(pos, next);
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
}
