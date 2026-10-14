package com.warlonmhite.hempdustry.client.item;

import com.mojang.serialization.MapCodec;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * {@code hempdustry:strain} — the loaded strain's {@code model_index}, or {@code 0} for "no bespoke
 * art of my own", which is every strain a datapack can add.
 *
 * <p>Since 1.21.4 an item's look is a <b>client item definition</b> in
 * {@code assets/hempdustry/items/*.json} rather than an {@code overrides} array on the model, and
 * the properties those definitions read are registered rather than invented per mod. This is the
 * numeric one; {@code minecraft:has_component} answers the other question (is anything loaded), so
 * the old {@code hempdustry:packed} property is gone — vanilla now ships it.
 *
 * <p>It replaces the 1.21.1 {@code ModelPredicateProvider} pair and takes their trap with it: the
 * old registry only accepted a <em>clamped</em> provider, so a {@code model_index} above 1 silently
 * collapsed onto the first strain's art. A {@link RangeSelectItemModelProperty} is unclamped, and the mod's
 * accessor mixin into {@code ModelPredicateProviderRegistry} is no longer needed.
 */
public record StrainModelIndexProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<StrainModelIndexProperty> CODEC =
            MapCodec.unit(new StrainModelIndexProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel world, ItemOwner context, int seed) {
        Holder<Strain> loaded =
                stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).primaryStrain();
        return loaded == null ? 0f : loaded.value().modelIndex();
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return CODEC;
    }
}
