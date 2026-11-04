package com.warlonmhite.hempdustry.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.animal.horse.Llama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The llama's synced swag colour, read raw so the hemp carpet's value is not lost to {@code DyeColor.byId}. */
@Mixin(Llama.class)
public interface LlamaAccessor {
    @Accessor("DATA_SWAG_ID")
    static EntityDataAccessor<Integer> hempdustry$swagId() {
        throw new AssertionError();
    }
}
