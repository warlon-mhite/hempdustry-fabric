package com.warlonmhite.hempdustry.sound;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent SMOKING = registerSoundEvent("smoking");
    public static final SoundEvent COUGHING = registerSoundEvent("coughing");

    // Music discs, vanilla-style: the sound event is a normal registry entry, but the *song*
    // (length, comparator output, "Now Playing" label) is a datapack entry in the JUKEBOX_SONG
    // dynamic registry — see resources/data/hempdustry/jukebox_song/.
    public static final SoundEvent MUSIC_DISC_MOONLIGHT = registerSoundEvent("music_disc.moonlight");
    public static final RegistryKey<JukeboxSong> MOONLIGHT_SONG = jukeboxSong("moonlight");

    public static final SoundEvent MUSIC_DISC_ROBADOB = registerSoundEvent("music_disc.robadob");
    public static final RegistryKey<JukeboxSong> ROBADOB_SONG = jukeboxSong("robadob");

    private static SoundEvent registerSoundEvent(String id) {
        Identifier identifier = Identifier.of(Hempdustry.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }

    /** Key into the JUKEBOX_SONG dynamic registry; the entry itself lives in {@code data/…/jukebox_song/}. */
    private static RegistryKey<JukeboxSong> jukeboxSong(String id) {
        return RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of(Hempdustry.MOD_ID, id));
    }

    public static void registerSounds(){
        Hempdustry.LOGGER.info("Registering Sounds for " + Hempdustry.MOD_ID);
    }
}