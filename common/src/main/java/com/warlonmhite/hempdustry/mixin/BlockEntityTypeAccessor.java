package com.warlonmhite.hempdustry.mixin;

import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The blocks a vanilla block entity type accepts. The hemp signs reuse vanilla's sign and hanging-sign
 * types, which list their blocks; a block not on the list throws when it is placed. On 1.20.1 neither
 * loader offers a way to add one, so the list is replaced by one that also holds ours.
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {
    @Accessor("validBlocks")
    Set<Block> hempdustry$validBlocks();

    @Mutable
    @Accessor("validBlocks")
    void hempdustry$setValidBlocks(Set<Block> blocks);
}
