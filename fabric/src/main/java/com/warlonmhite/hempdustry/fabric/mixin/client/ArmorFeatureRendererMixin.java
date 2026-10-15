package com.warlonmhite.hempdustry.fabric.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.warlonmhite.hempdustry.item.ModArmorMaterials;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * An undyed hemp shirt or pair of harem pants is worn in unbleached hemp, not leather's brown.
 *
 * <p>Vanilla tints every dyeable armour layer with {@code DyedItemColor.getOrDefault(stack,
 * -6265536)}, leather's brown written into the call ({@code HumanoidArmorLayer#renderArmorPiece}), so
 * an undyed garment would come out brown. This hands that call
 * {@link ModArmorMaterials#HEMP_UNDYED_COLOR} for our two items instead. A dyed one is unchanged:
 * its own colour wins either way. A default {@code dyed_color} on the items would do the same in the
 * world but not at the crafting table, where the dye recipe mixes the colour already there into
 * every new dye, and the cauldron wash would strip it back to brown.
 *
 * <p>Fabric's alone: NeoForge rewrites that call into an item hook of its own, which its client entry
 * point answers for the same two items.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class ArmorFeatureRendererMixin {

    @WrapOperation(method = "renderArmorPiece", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/DyedItemColor;getOrDefault(Lnet/minecraft/world/item/ItemStack;I)I"))
    private int hempdustry$hempUndyed(ItemStack stack, int undyed, Operation<Integer> original) {
        boolean hemp = stack.is(ModItems.HEMP_SHIRT) || stack.is(ModItems.HEMP_HAREM_PANTS);
        return original.call(stack, hemp ? ModArmorMaterials.HEMP_UNDYED_COLOR : undyed);
    }
}
