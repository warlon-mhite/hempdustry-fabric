package com.warlonmhite.hempdustry.mixin.ganjacraft;

import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A GanjaCraft block in a chunk's palette (or anywhere else a block state is read from a save) reads
 * as the Hempdustry block it became, in the state it stood for -- a crop's stage, a stair's facing.
 */
@Mixin(NbtUtils.class)
public class BlockStateMixin {

    @Inject(method = "readBlockState", at = @At("HEAD"), cancellable = true)
    private static void hempdustry$readGanjaCraft(CompoundTag saved, CallbackInfoReturnable<BlockState> cir) {
        BlockState converted = GanjaCraftWorlds.block(saved);
        if (converted != null) {
            cir.setReturnValue(converted);
        }
    }
}
