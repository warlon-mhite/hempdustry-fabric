package com.warlonmhite.hempdustry.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.warlonmhite.hempdustry.item.ModItems;
import java.util.Arrays;
import java.util.stream.Stream;
import net.minecraft.world.entity.animal.goat.GoatAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Goats browse, and a fan leaf is what a goat strips off a plant: the hemp leaf tempts one, as wheat
 * does. 1.20.1 builds each goat's temptation from this method (its tempting sensor); its breeding is
 * {@code GoatBreedsOnHempLeafMixin}. The newer lines join {@code #minecraft:goat_food} instead. Only
 * goats: a leaf feeding every farm animal would be a worse wheat.
 *
 * <p>{@code @ModifyReturnValue} adds to whatever the method returns, so another mod's goat food stands.
 */
@Mixin(GoatAi.class)
public abstract class GoatFoodMixin {
    @ModifyReturnValue(method = "getTemptations", at = @At("RETURN"))
    private static Ingredient hempdustry$hempLeafTempts(Ingredient original) {
        return Ingredient.of(Stream.concat(Arrays.stream(original.getItems()), Stream.of(new ItemStack(ModItems.HEMP_LEAF)))
                .distinct());
    }
}
