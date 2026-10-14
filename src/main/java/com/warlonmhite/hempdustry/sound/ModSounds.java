package com.warlonmhite.hempdustry.sound;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.JukeboxSong;

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

    // Music discs, vanilla-style: the sound event is a normal registry entry, but the *song*
    // (length, comparator output, "Now Playing" label) is a datapack entry in the JUKEBOX_SONG
    // dynamic registry — see resources/data/hempdustry/jukebox_song/.
    public static final SoundEvent MUSIC_DISC_MOONLIGHT = registerSoundEvent("music_disc.moonlight");
    public static final ResourceKey<JukeboxSong> MOONLIGHT_SONG = jukeboxSong("moonlight");

    public static final SoundEvent MUSIC_DISC_ROBADOB = registerSoundEvent("music_disc.robadob");
    public static final ResourceKey<JukeboxSong> ROBADOB_SONG = jukeboxSong("robadob");

    private static SoundEvent registerSoundEvent(String id) {
        Identifier identifier = Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, identifier, SoundEvent.createVariableRangeEvent(identifier));
    }

    /** Key into the JUKEBOX_SONG dynamic registry; the entry itself lives in {@code data/…/jukebox_song/}. */
    private static ResourceKey<JukeboxSong> jukeboxSong(String id) {
        return ResourceKey.create(Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, id));
    }

    public static void registerSounds(){
        Hempdustry.LOGGER.info("Registering Sounds for " + Hempdustry.MOD_ID);
    }
}