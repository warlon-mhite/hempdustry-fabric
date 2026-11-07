package com.warlonmhite.hempdustry.mixin;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The search tab's stacks for each of our items: the creative tab's search entries, every dose and
 * coat, where vanilla would list the bare item alone. A 1.16.5 search tab is built by asking every item
 * for its stacks; the newer lines' tabs say it themselves ({@code SEARCH_TAB_ONLY}).
 */
@Mixin(Item.class)
public abstract class CreativeSearchMixin {

    @Inject(method = "fillItemCategory", at = @At("HEAD"), cancellable = true)
    private void hempdustry$searchStacks(CreativeModeTab tab, NonNullList<ItemStack> list, CallbackInfo ci) {
        if (tab != CreativeModeTab.TAB_SEARCH) {
            return;
        }
        Item self = (Item) (Object) this;
        ResourceLocation id = Registry.ITEM.getKey(self);
        if (!Hempdustry.MOD_ID.equals(id.getNamespace())) {
            return;
        }
        list.addAll(ModItemGroups.searchStacks(self));
        ci.cancel();
    }
}
