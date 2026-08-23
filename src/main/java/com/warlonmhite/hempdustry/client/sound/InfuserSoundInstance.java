package com.warlonmhite.hempdustry.client.sound;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.InfuserBlock;
import com.warlonmhite.hempdustry.sound.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * The Infuser's simmering loop, for as long as one particular tub is simmering.
 *
 * <h2>Why a sound instance rather than a one-shot</h2>
 *
 * The brewing-stand bubble this replaced was played straight out of
 * {@link InfuserBlock#randomDisplayTick} on a 5% roll, which is the right shape for a half-second
 * noise and the wrong one for a twenty-second loop: a fresh copy would start roughly every twenty
 * ticks and layer twenty of them on top of each other, getting louder for as long as the batch ran.
 *
 * <p>A repeating {@link MovingSoundInstance} is the engine's own answer. It is handed to the sound
 * manager once, loops seamlessly (the sound is not streamed, so {@code SoundSystem} sets OpenAL
 * looping on a static buffer — the most reliable kind of seam), and <b>ends itself</b> from
 * {@link #tick()} the moment the block stops saying {@code infusing}. Nothing has to remember to
 * stop it: not the block entity, not the server, and not a state-change hook that would have to
 * cover the batch finishing, the heat going out, the hemp running dry, the tub being broken and the
 * chunk unloading separately. Asking the blockstate covers all six.
 *
 * <h2>Starting it</h2>
 *
 * {@code randomDisplayTick} is still the trigger, which means a loop starts within a second or so of
 * a player coming into range rather than instantly — the same budget vanilla's campfire crackle
 * runs on, and the same call that already draws this block's bubbles and steam.
 *
 * <p>{@link #ACTIVE} is what keeps that from starting a second copy on the next roll. It is keyed by
 * position and <b>re-checked against the sound manager</b> rather than trusted: a world change calls
 * {@code SoundSystem.stopAll}, which drops every ticking sound without ticking it, so an instance
 * can stop without this class ever hearing about it. Asking {@link SoundManager#isPlaying} instead
 * of trusting the map means the loop simply starts again next roll.
 */
@Environment(EnvType.CLIENT)
public class InfuserSoundInstance extends MovingSoundInstance {

    /** One live instance per tub. See the class note on why membership is verified, not trusted. */
    private static final Map<BlockPos, InfuserSoundInstance> ACTIVE = new HashMap<>();

    private final BlockPos pos;

    /**
     * Well below the sound as authored, because <b>a continuous sound is not judged like an
     * intermittent one</b>. The brewing-stand bubble this replaced played at {@code 0.35F} and was
     * rolled for about once a second; this plays without a gap for as long as a batch cooks, next to
     * a block you build a base around. Matching that 0.35 was still too loud in a play test, so it
     * sits under it: audible standing at the tub, gone a few blocks away, and survivable in a room
     * of them.
     */
    private static final float VOLUME = 0.3F;

    /** Starts the loop for this tub unless it is already running. Client thread only. */
    public static void startIfNeeded(BlockPos pos) {
        SoundManager sounds = MinecraftClient.getInstance().getSoundManager();
        InfuserSoundInstance running = ACTIVE.get(pos);
        if (running != null && sounds.isPlaying(running)) {
            return;
        }
        InfuserSoundInstance instance = new InfuserSoundInstance(pos.toImmutable());
        ACTIVE.put(instance.pos, instance);
        sounds.play(instance);
    }

    private InfuserSoundInstance(BlockPos pos) {
        super(ModSounds.INFUSER_SIMMER, SoundCategory.BLOCKS, SoundInstance.createRandom());
        this.pos = pos;
        // repeat with no delay is what SoundSystem reads as "loop this seamlessly"; a non-zero
        // repeatDelay would make it a metronome instead, and would also cost the loop its
        // OpenAL-level looping.
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = VOLUME;
        // Centre of the block. Not the brew's surface: this is a whole tub of liquid making the
        // noise, and a listener a block away cannot tell the difference anyway.
        this.x = pos.getX() + 0.5D;
        this.y = pos.getY() + 0.5D;
        this.z = pos.getZ() + 0.5D;
    }

    @Override
    public void tick() {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            stop();
            return;
        }
        BlockState state = world.getBlockState(pos);
        // An unloaded chunk answers void air here, so leaving the area stops the loop through this
        // same branch — there is no separate unload case to handle.
        if (!state.isOf(ModBlocks.INFUSER) || !state.get(InfuserBlock.INFUSING)) {
            stop();
        }
    }

    private void stop() {
        ACTIVE.remove(pos, this);
        setDone();
    }
}
