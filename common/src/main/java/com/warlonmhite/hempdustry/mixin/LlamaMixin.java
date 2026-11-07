package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.HempCarpetBlock;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tells the clients when a llama wears the hemp carpet. On 1.20.1 a llama's carpet stays in its
 * server-side inventory; all a client learns is the synced swag colour, which vanilla takes from a
 * {@code WoolCarpetBlock}'s {@code DyeColor} and which ours does not have. So a llama in the hemp
 * carpet syncs {@link HempCarpetBlock#SWAG_ID}, a value no dye has, and the decor layer draws ours
 * for it ({@code LlamaDecorFeatureRendererMixin}).
 */
@Mixin(Llama.class)
public abstract class LlamaMixin extends AbstractChestedHorse {
    @Shadow
    @Final
    private static EntityDataAccessor<Integer> DATA_SWAG_ID;

    protected LlamaMixin(EntityType<? extends AbstractChestedHorse> type, Level level) {
        super(type, level);
    }

    @Inject(method = "updateContainerEquipment", at = @At("TAIL"))
    private void hempdustry$wearHempCarpet(CallbackInfo ci) {
        if (!this.level.isClientSide && this.inventory.getItem(1).is(ModBlocks.HEMP_CARPET.asItem())) {
            this.entityData.set(DATA_SWAG_ID, HempCarpetBlock.SWAG_ID);
        }
    }
}
