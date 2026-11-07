package com.warlonmhite.hempdustry.mixin.ganjacraft;

import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A GanjaCraft block entity reads as the one it became (a hemp sign's, vanilla's sign), or not at all
 * when its block, converted, needs none (a crop stage's growth timer).
 */
@Mixin(BlockEntity.class)
public class BlockEntityMixin {

    @Inject(method = "loadStatic", at = @At("HEAD"), cancellable = true)
    private static void hempdustry$dropGanjaCraft(BlockPos pos, BlockState state, CompoundTag saved,
                                                 CallbackInfoReturnable<BlockEntity> cir) {
        Optional<String> becomes = GanjaCraftWorlds.blockEntity(saved.getString("id"));
        if (becomes != null && !becomes.isPresent()) {
            cir.setReturnValue(null);
        }
    }

    @ModifyVariable(method = "loadStatic", at = @At("HEAD"), argsOnly = true)
    private static CompoundTag hempdustry$readGanjaCraft(CompoundTag saved) {
        Optional<String> becomes = GanjaCraftWorlds.blockEntity(saved.getString("id"));
        if (becomes == null || !becomes.isPresent()) {
            return saved;
        }
        CompoundTag converted = saved.copy();
        converted.putString("id", becomes.get());
        return converted;
    }
}
