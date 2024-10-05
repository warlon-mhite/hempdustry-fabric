package com.warlonmhite.hempdustry.sound;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent SMOKING = registerSoundEvent("smoking");
    public static final SoundEvent COUGHING = registerSoundEvent("coughing");

    private static SoundEvent registerSoundEvent(String id) {
        Identifier identifier = Identifier.of(Hempdustry.MOD_ID, id);
        return Registry.register(Registries.SOUND_EVENT, identifier, SoundEvent.of(identifier));
    }

    public static void registerSounds(){
        Hempdustry.LOGGER.info("Registering Sounds for " + Hempdustry.MOD_ID);
    }
}