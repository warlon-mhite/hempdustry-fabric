package com.warlonmhite.hempdustry.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Opens vanilla's own way of telling the game a block state is a point of interest. A villager's
 * home is the {@code minecraft:home} point, built from a fixed list of the sixteen vanilla bed heads;
 * the list is only read once, into the state-to-point map, and that map is what the game asks
 * afterwards. {@code registerBlockStates} is what fills it, and it refuses a state that already has a
 * point — so a second mod claiming our bed fails loudly rather than silently winning.
 */
@Mixin(PoiTypes.class)
public interface PointOfInterestTypesAccessor {
    @Invoker("registerBlockStates")
    static void hempdustry$registerStates(Holder<PoiType> poiType, Set<BlockState> states) {
        throw new AssertionError();
    }
}
