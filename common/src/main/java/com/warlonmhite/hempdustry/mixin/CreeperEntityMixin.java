package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.entity.ai.SeekHempCropGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives creepers a reason to walk into a hemp field.
 *
 * <p>Why this has to be a mixin: <b>there is no event for adding a goal to a vanilla mob.</b>
 * Fabric API exposes entity spawn and load events, but {@code Mob#goalSelector} is protected,
 * so reaching it from an event handler needs an accessor mixin anyway — at which point injecting
 * into {@code registerGoals} is both smaller and correct at the one moment vanilla itself builds the
 * list. What the goal actually does, and why it sits at priority 5, is in {@link SeekHempCropGoal}.
 *
 * <h2>Why {@code @Inject} here where the bee mixin needed {@code @WrapOperation}</h2>
 *
 * {@code BeeGrowCropsGoalMixin} wraps a specific instruction, and wrapping is what lets several mods
 * sit on the same call without one of them winning. This one adds nothing to an existing call — it
 * appends to a list at the end of a method — and <b>{@code @Inject} was never exclusive</b>: any
 * number of mods can inject into the same method at the same point and every one of them runs. So
 * the compatibility worry that shaped the bee mixin does not apply, and the ordinary injection is
 * the right tool.
 *
 * <p>Adding a goal at {@code TAIL} rather than {@code HEAD} is deliberate: a {@code GoalSelector}
 * iterates in insertion order when two goals share a priority, so appending means vanilla's own
 * {@code WaterAvoidingRandomStrollGoal} — which is also priority 5 — gets first refusal on every tick. That is
 * what keeps this a drift towards the field rather than a beeline.
 *
 * <p>The class extends {@code Monster} only so {@code goalSelector} resolves; the constructor
 * is never called, and Mixin discards it.
 */
@Mixin(Creeper.class)
public abstract class CreeperEntityMixin extends Monster {

    protected CreeperEntityMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void hempdustry$seekHempCrops(CallbackInfo ci) {
        this.goalSelector.addGoal(5, new SeekHempCropGoal((Creeper) (Object) this));
    }
}
