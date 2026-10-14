package com.warlonmhite.hempdustry.client.sound;

import com.warlonmhite.hempdustry.item.custom.SmokingDeviceItem;
import com.warlonmhite.hempdustry.sound.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * A bong's bubbling, following the one drawing it — and cut off if they let go before the hit.
 * The shape is vanilla's elytra wind: a sound that belongs to a player's state rather than to a
 * moment, played by each client that can see the player.
 *
 * <p>Letting go and finishing both end the draw, and only letting go stops the sound: a finished
 * draw is the hit, and the bubbling runs out on its own over the inhale. The two are told apart by
 * how much of the draw was left when it ended.
 */
public class BongDrawSoundInstance extends AbstractTickableSoundInstance {
    // The client counts the draw down on its own clock and hears that it ended a moment later, so a
    // draw that got within a couple of ticks of the end was finished, not abandoned.
    private static final int FINISH_SLACK_TICKS = 2;

    private final LivingEntity drawer;
    private int left;
    private boolean finished;

    public BongDrawSoundInstance(LivingEntity drawer) {
        super(ModSounds.BONGHIT, SoundSource.PLAYERS, RandomSource.create());
        this.drawer = drawer;
        this.left = drawer.getUseItemRemainingTicks();
        follow();
    }

    @Override
    public void tick() {
        if (drawer.isRemoved()) {
            stop();
            return;
        }
        if (!finished) {
            if (drawer.isUsingItem() && drawer.getUseItem().getItem() instanceof SmokingDeviceItem) {
                left = drawer.getUseItemRemainingTicks();
            } else if (left > FINISH_SLACK_TICKS) {
                stop();
                return;
            } else {
                finished = true;
            }
        }
        follow();
    }

    private void follow() {
        x = drawer.getX();
        y = drawer.getY();
        z = drawer.getZ();
    }
}
