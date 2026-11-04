package com.warlonmhite.hempdustry.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A goat breeds on the hemp leaf, as it is tempted by it ({@code GoatFoodMixin}). A goat's food is
 * {@code Animal#isFood} on 1.20.1, which the goat does not override; this adds the leaf for goats only.
 */
@Mixin(Animal.class)
public abstract class GoatBreedsOnHempLeafMixin {
    @ModifyReturnValue(method = "isFood", at = @At("RETURN"))
    private boolean hempdustry$goatsEatHempLeaf(boolean original, ItemStack stack) {
        return original || (Object) this instanceof Goat && stack.is(ModItems.HEMP_LEAF);
    }
}
