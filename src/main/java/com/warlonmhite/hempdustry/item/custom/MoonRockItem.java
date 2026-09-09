package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

/**
 * A moon rock: a bud dipped in rosin and rolled in hashish.
 *
 * <h2>Why this is one item and not one per strain</h2>
 *
 * A moon rock is not a material — it is a <em>load</em>, three tiers of the chain stuck together,
 * and the mod already has an item shaped exactly like that: the spliff. So the strain rides in the
 * same {@code smoke_contents} component a rolled joint carries, and the name, the tint and the
 * recipe-viewer subtype all fall out of machinery that is already written. A third strain gets a
 * moon rock the day it gets a bud, with no new item, no new texture and no new lang key.
 *
 * <p>{@code concentrates.md} §5 proposed {@code indica_moon_rock} / {@code sativa_moon_rock}
 * instead, on the grounds that a coated bud is still a bud and material identity gets its own item.
 * That rule is right for a <em>material</em>; a moon rock is a composite, and the spliff — also a
 * plant strain wrapped in something — is the closer precedent. The player sees no difference: it is
 * still "Purple Kush Moon Rock", still tinted by its strain.
 *
 * <h2>What it does</h2>
 *
 * The component is a <b>two-entry load</b>: the plant at 3 and hashish at 1. Packing it copies that
 * load straight onto the device, which makes it the only hit in the mod that is a strain at level
 * III <em>and</em> the hash body at once — a bong of three buds is level III with no hash, a hash
 * spliff has the hash but only level II.
 *
 * <p>It goes in a bong and nowhere else, and no rule had to be written for that either: the plant
 * entry is 3 and only the bong's {@code maxDose} is 3. See {@code PackingRecipe}.
 */
public class MoonRockItem extends Item {

    public MoonRockItem(Settings settings) {
        super(settings);
    }

    /**
     * "Purple Kush Moon Rock", built the potion way — one format key per locale, filled in with the
     * load's own name.
     *
     * <p>Deliberately <b>not</b> {@code SmokeContents.packedName}, which appends the Roman numeral.
     * A moon rock is always the same thing; the numeral belongs on the device it goes into, where it
     * means "this is the level you packed". Putting a III on the ingredient would suggest there are
     * a I and a II.
     */
    @Override
    public Text getName(ItemStack stack) {
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        return contents.isEmpty()
                ? super.getName(stack)
                : Text.translatable(this.getTranslationKey() + ".of", contents.loadName());
    }

    /** The hashish coat, named the same italic way a hash spliff names its pinch. */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).hashAdditive()
                .ifPresent(hash -> textConsumer.accept(
                        Text.translatable("hempdustry.spliff.with",
                                        Text.translatable(hash.value().translationKey()))
                                .formatted(Formatting.GRAY, Formatting.ITALIC)));
    }
}
