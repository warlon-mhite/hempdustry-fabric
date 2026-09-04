package com.warlonmhite.hempdustry.client.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import net.minecraft.client.render.item.tint.TintSource;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.ColorHelper;
import org.jetbrains.annotations.Nullable;

/**
 * {@code hempdustry:strain} — paints the loaded strain's own colour onto the smoking gear.
 *
 * <p>This is what makes a strain a <b>datapack</b> feature rather than a code one. Bespoke art needs
 * a texture and a datapack cannot ship one, so without a tint every datapack strain would render
 * identically to whichever strain the mod happens to carry art for. With it, a strain is
 * distinguishable from the colour in its own JSON and nothing else.
 *
 * <p>Since 1.21.4 tints are declared by the client item definition rather than registered in code,
 * so this is a {@link TintSource} listed in a model's {@code tints} array. Its position in that
 * array is the tint index the model's {@code layerN} quads carry — see
 * {@link com.warlonmhite.hempdustry.item.ModItemProperties#LOAD_TINT_INDEX}.
 *
 * <p>{@code fullAlpha} is load-bearing, not decoration: a tint's alpha byte becomes the vertex
 * alpha, and a strain's colour is 24-bit RGB out of its JSON — returned raw it means alpha
 * {@code 0x00} and a layer that draws nothing at all, with no error anywhere.
 *
 * @param defaultColor what an empty (or component-less) stack is tinted with; the resource pack can
 *                     change it, and white is a multiply by 1.0, i.e. "as drawn".
 */
public record StrainTintSource(int defaultColor) implements TintSource {
    public static final MapCodec<StrainTintSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codecs.RGB.fieldOf("default").forGetter(StrainTintSource::defaultColor)
            ).apply(instance, StrainTintSource::new));

    @Override
    public int getTint(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity) {
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        return ColorHelper.fullAlpha(contents.isEmpty() ? defaultColor : contents.color());
    }

    @Override
    public MapCodec<? extends TintSource> getCodec() {
        return CODEC;
    }
}
