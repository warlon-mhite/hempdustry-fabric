package com.warlonmhite.hempdustry.forge.mixin;

import java.util.List;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * A pool's entries, which Forge's loot event gives no way to add to: it hands over a pool already
 * built. Forge 36 keeps them in a list of its own, so an entry is added to it.
 */
@Mixin(LootPool.class)
public interface LootPoolAccessor {
    @Accessor("entries")
    List<LootPoolEntryContainer> hempdustry$entries();
}
