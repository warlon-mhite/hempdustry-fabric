package com.warlonmhite.hempdustry.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;

/**
 * Opens vanilla's own way of telling the game a block state is a point of interest. A villager's
 * home is the {@code minecraft:home} point, built from a fixed list of the sixteen vanilla bed heads;
 * the list is only read once, into the state-to-point map, and that map is what the game asks
 * afterwards. {@code registerStates} is what fills it, and it refuses a state that already has a
 * point — so a second mod claiming our bed fails loudly rather than silently winning.
 */
@Mixin(PointOfInterestTypes.class)
public interface PointOfInterestTypesAccessor {
    @Invoker("registerStates")
    static void hempdustry$registerStates(RegistryEntry<PointOfInterestType> poiType, Set<BlockState> states) {
        throw new AssertionError();
    }
}
