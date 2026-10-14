package com.warlonmhite.hempdustry.client.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
 * so this is a {@link ItemTintSource} listed in a model's {@code tints} array. Its position in that
 * array is the tint index the model's {@code layerN} quads carry — see
 * {@link com.warlonmhite.hempdustry.item.ModItemProperties#LOAD_TINT_INDEX}.
 *
 * <p>{@code opaque} is load-bearing, not decoration: a tint's alpha byte becomes the vertex
 * alpha, and a strain's colour is 24-bit RGB out of its JSON — returned raw it means alpha
 * {@code 0x00} and a layer that draws nothing at all, with no error anywhere.
 *
 * @param defaultColor what an empty (or component-less) stack is tinted with; the resource pack can
 *                     change it, and white is a multiply by 1.0, i.e. "as drawn".
 */
public record StrainTintSource(int defaultColor) implements ItemTintSource {
    public static final MapCodec<StrainTintSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(StrainTintSource::defaultColor)
            ).apply(instance, StrainTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity) {
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        return ARGB.opaque(contents.isEmpty() ? defaultColor : contents.color());
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return CODEC;
    }
}
