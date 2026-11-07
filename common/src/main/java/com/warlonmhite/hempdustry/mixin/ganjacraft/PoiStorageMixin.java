package com.warlonmhite.hempdustry.mixin.ganjacraft;

import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A chunk's saved points of interest, converted as they are read from disk. A point whose type the game
 * does not know fails to decode and is logged as an error; the old mod's points -- Hemp-Craft's Hemp
 * Farmer job site, its hemp bricks -- are renamed or, when their block is no point of interest any more,
 * let go first. Each section changed so is marked for saving once the column is loaded, so the file holds
 * the new points from the next save on (vanilla saves a section only when something marked it). Only the
 * points of interest are kept in this storage on 1.18.2.
 */
@Mixin(SectionStorage.class)
public abstract class PoiStorageMixin {

    @Unique
    private final List<Long> hempdustry$converted = new ArrayList<>();

    @Shadow
    protected abstract void setDirty(long sectionPos);

    @Inject(method = "tryRead", at = @At("RETURN"))
    private void hempdustry$readGanjaCraft(ChunkPos pos, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag saved = cir.getReturnValue();
        if (saved == null || !saved.contains("Sections", Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag sections = saved.getCompound("Sections");
        for (String key : sections.getAllKeys()) {
            ListTag records = sections.getCompound(key).getList("Records", Tag.TAG_COMPOUND);
            boolean changed = false;
            for (int i = records.size() - 1; i >= 0; i--) {
                CompoundTag record = records.getCompound(i);
                Optional<String> becomes = GanjaCraftWorlds.pointOfInterest(record.getString("type"));
                if (becomes == null) {
                    continue;
                }
                if (becomes.isPresent()) {
                    record.putString("type", becomes.get());
                } else {
                    records.remove(i);
                }
                changed = true;
            }
            if (changed) {
                try {
                    hempdustry$converted.add(SectionPos.asLong(pos.x, Integer.parseInt(key), pos.z));
                } catch (NumberFormatException notASection) {
                    // vanilla reads only numbered sections; there is nothing to mark
                }
            }
        }
    }

    @Inject(method = "readColumn(Lnet/minecraft/world/level/ChunkPos;)V", at = @At("TAIL"))
    private void hempdustry$saveConverted(ChunkPos pos, CallbackInfo ci) {
        for (long section : hempdustry$converted) {
            setDirty(section);
        }
        hempdustry$converted.clear();
    }
}
