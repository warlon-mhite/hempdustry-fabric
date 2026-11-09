package com.warlonmhite.hempdustry.test.mixin;

import com.warlonmhite.hempdustry.test.HarvestingGround;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Test mod only: lets a watched farmland harvest what ripens on it ({@link HarvestingGround}). */
@Mixin(FarmBlock.class)
abstract class FarmBlockHarvestMixin {
    @Inject(method = "updateShape", at = @At("HEAD"))
    private void hempdustryTest$harvestWhatRipens(BlockState state, Direction direction, BlockState neighbor,
                                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos,
                                                  CallbackInfoReturnable<BlockState> cir) {
        HarvestingGround.answer(level, pos, direction, neighbor, neighborPos);
    }
}
