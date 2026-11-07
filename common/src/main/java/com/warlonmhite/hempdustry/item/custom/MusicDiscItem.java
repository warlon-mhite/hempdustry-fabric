package com.warlonmhite.hempdustry.item.custom;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.RecordItem;

/**
 * One of the mod's music discs: vanilla 1.20.1's {@link RecordItem}, whose constructor is protected.
 * The disc carries its song — comparator output, sound and length in seconds — and its "Now Playing"
 * label is the item's {@code .desc} key.
 */
public class MusicDiscItem extends RecordItem {
    public MusicDiscItem(int comparatorOutput, SoundEvent sound, Properties properties, int lengthInSeconds) {
        // 1.18.2's jukebox knows no length: the disc plays until its sound ends.
        super(comparatorOutput, sound, properties);
    }
}
