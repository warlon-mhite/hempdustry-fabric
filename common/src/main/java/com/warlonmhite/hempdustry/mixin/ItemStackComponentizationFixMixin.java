package com.warlonmhite.hempdustry.mixin;

import com.mojang.serialization.Dynamic;
import com.warlonmhite.hempdustry.Hempdustry;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives back the mod's values on item stacks from a world made before Minecraft 1.20.5.
 *
 * <p>Minecraft 1.20.1 has no data components: the mod's build for it keeps each value in the stack's
 * tag, under the component's own id ({@code hempdustry:charges}) and in the component's own shape.
 * Opened here, every stack goes through vanilla's 1.20.5 fix, which turns the tag entries it knows
 * into components and leaves the rest in {@code minecraft:custom_data} — where the mod never looks,
 * so a packed pipe came up unpacked and cannabutter lost its strength. This moves each of our
 * entries to the component of the same id, the way the fix moves vanilla's own, just before the
 * leftovers are written. An entry with no component of that id stays in the custom data.
 *
 * <p>It runs only on data older than 1.20.5, and only once: a world saved by this version is never
 * passed through that fix again.
 */
@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixMixin {
    private static List<String> hempdustry$components;

    @Inject(method = "fixItemStack", at = @At("TAIL"))
    private static void hempdustry$liftOurValues(ItemStackComponentizationFix.ItemStackData data, Dynamic<?> tag,
                                                 CallbackInfo ci) {
        if (hempdustry$components == null) {
            hempdustry$components = BuiltInRegistries.DATA_COMPONENT_TYPE.keySet().stream()
                    .filter(id -> id.getNamespace().equals(Hempdustry.MOD_ID))
                    .map(Object::toString)
                    .toList();
        }
        for (String id : hempdustry$components) {
            data.moveTagToComponent(id, id);
        }
    }
}
