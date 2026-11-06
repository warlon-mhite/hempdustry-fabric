package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.effect.ModEffects;
import com.warlonmhite.hempdustry.entity.ai.SeekHempCropGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Endermen and Ender Haze.
 *
 * <ul>
 *   <li><b>They loiter in it.</b> The creepers' wander ({@link SeekHempCropGoal}), pointed at Ender
 *       Haze alone, at the enderman's own wandering speed. Priority 6: under swimming, freezing when
 *       looked at and the attack (0–2), so an enderman with something to do does it, and over its own wander (7),
 *       which it would never get past at the same number — the goal already running keeps the
 *       movement control against an equal one.</li>
 *   <li><b>They do not see a player under Averted Gaze.</b> Vanilla's stare test,
 *       {@code isBeingStaredBy}, is the one place an enderman decides whether a look provokes it, and
 *       a carved pumpkin answers it before anything else; the effect answers it the same way, at the
 *       head of the method. An enderman already angry at the player stays angry: the effect hides
 *       your eyes, not your sword.</li>
 * </ul>
 *
 * <p>An {@code @Inject} on each, as on the creeper: neither replaces a call, so any number of mods can
 * sit on the same methods. The class extends {@code Monster} only so {@code goalSelector} resolves.
 */
@Mixin(Enderman.class)
public abstract class EnderManMixin extends Monster {
    /** {@code WaterAvoidingRandomStrollGoal}'s speed on an enderman. */
    private static final double HEMPDUSTRY$WANDER_SPEED = 1.0;

    protected EnderManMixin(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void hempdustry$loiterInEnderHaze(CallbackInfo ci) {
        this.goalSelector.addGoal(6, new SeekHempCropGoal((Enderman) (Object) this, HEMPDUSTRY$WANDER_SPEED,
                state -> state.is(ModBlocks.ENDER_HAZE_CROP)));
    }

    @Inject(method = "isBeingStaredBy", at = @At("HEAD"), cancellable = true)
    private void hempdustry$avertedGaze(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player.hasEffect(ModEffects.AVERTED_GAZE)) {
            cir.setReturnValue(false);
        }
    }
}
