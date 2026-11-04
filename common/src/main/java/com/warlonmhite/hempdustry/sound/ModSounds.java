package com.warlonmhite.hempdustry.sound;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {

    public static final SoundEvent SMOKING = registerSoundEvent("smoking");
    public static final SoundEvent BONGHIT = registerSoundEvent("bonghit");
    public static final SoundEvent COUGHING = registerSoundEvent("coughing");

    /**
     * The Infuser's simmer — a 20-second loop, played while and only while a batch is actually
     * infusing (milk in the tub, heat underneath).
     *
     * <p><b>It is a loop, not a one-shot, and that decides how it is played.</b> The brewing-stand
     * bubble it replaced was fired from {@code animateTick} at a 5% chance per tick, which is
     * fine for a half-second noise and catastrophic for a twenty-second one — a fresh copy would
     * start every ~20 ticks and twenty of them would end up layered on top of each other. It is
     * played instead as a repeating client-side sound instance that ends itself when the block stops
     * infusing. See {@code client/sound/InfuserSoundInstance}.
     *
     * <p>The file is <b>mono</b>, which is not optional: Minecraft plays a stereo sound
     * non-positionally, so a stereo simmer would follow the player around the world at full volume.
     */
    public static final SoundEvent INFUSER_SIMMER = registerSoundEvent("infuser_simmer");

    // Music discs, vanilla 1.20.1-style: the sound event is a normal registry entry, and the disc
    // item itself (a RecordItem, ModItems) carries the length and comparator output.
    public static final SoundEvent MUSIC_DISC_MOONLIGHT = registerSoundEvent("music_disc.moonlight");

    public static final SoundEvent MUSIC_DISC_ROBADOB = registerSoundEvent("music_disc.robadob");

    private static SoundEvent registerSoundEvent(String id) {
        ResourceLocation identifier = new ResourceLocation(Hempdustry.MOD_ID, id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
    }

    public static void registerSounds(){
        Hempdustry.LOGGER.info("Registering Sounds for " + Hempdustry.MOD_ID);
    }
}