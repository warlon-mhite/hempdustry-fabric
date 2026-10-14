package com.warlonmhite.hempdustry.neoforge.mixin;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/**
 * A pool's entries, which NeoForge's loot event gives no way to add to: Fabric's hands the mod a
 * builder it can add an entry to, NeoForge's a pool already built.
 */
@Mixin(LootPool.class)
public interface LootPoolAccessor {
    @Accessor("entries")
    List<LootPoolEntryContainer> hempdustry$entries();

    @Mutable
    @Accessor("entries")
    void hempdustry$setEntries(List<LootPoolEntryContainer> entries);
}
