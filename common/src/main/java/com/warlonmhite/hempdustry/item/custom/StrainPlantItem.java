package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.ResinStep;
import com.warlonmhite.hempdustry.strain.Strain;
import com.warlonmhite.hempdustry.strain.Terpene;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * The tooltip a strain's buds and seeds share, the way a seed bank prints it on the packet: the
 * strain's chemotype in a word, grey, then — on a bud — its potency band and its harvest's tier, then its terpene's name in
 * the terpene's colour. A cannabinoid-free bud has no band line: "Cannabinoid-free · Low potency"
 * contradicts itself, so its band is kept on the bud and never shown. Read off the
 * registries the world loaded, so a datapack that makes a strain CBD-dominant, or gives it another
 * terpene, changes the lines on every bud and seed already in a chest. Nothing further down the
 * chain (leaves, hash, butter, edibles) names a terpene: the smell is the flower's.
 */
public final class StrainPlantItem {
    private StrainPlantItem() {
    }

    /** The strain whose buds or seeds {@code item} is, if the tooltip has registries to ask. */
    public static Optional<Strain> strainOf(Item item, Item.TooltipContext context) {
        HolderLookup.Provider registries = context.registries();
        if (registries == null) {
            return Optional.empty();
        }
        for (Holder.Reference<Strain> strain : Strain.registry(registries).listElements().toList()) {
            Strain value = strain.value();
            if (value.wildFlower().isPresent()
                    && (value.buds() == item || value.seedItem().filter(seed -> seed == item).isPresent())) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    /**
     * Adds the chemotype line, from the strain, or from {@code fallback} for a plant with no strain
     * record, then {@code band}'s line if there is one and the plant makes any cannabinoid, then the
     * strain's terpene if the world defines it.
     */
    public static void appendProfile(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip,
                                     Optional<Chemotype> fallback, Optional<PotencyBand> band) {
        appendProfile(stack, context, tooltip, fallback, band, Optional.empty());
    }

    /**
     * As above, with {@code tier}'s line after the band's, before the terpene: a bud names its harvest's
     * tier always, Dank included, grey; the colour is the name's.
     */
    public static void appendProfile(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip,
                                     Optional<Chemotype> fallback, Optional<PotencyBand> band, Optional<BudTier> tier) {
        appendProfile(stack, context, tooltip, fallback, band, Optional.empty(), tier);
    }

    /**
     * As above, with {@code resin}'s line between the band's and the tier's: a bud names its plant's resin step,
     * grey, unless it is average.
     */
    public static void appendProfile(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip,
                                     Optional<Chemotype> fallback, Optional<PotencyBand> band, Optional<ResinStep> resin,
                                     Optional<BudTier> tier) {
        Optional<Strain> strain = strainOf(stack.getItem(), context);
        Optional<Chemotype> chemotype = strain.map(Strain::chemotype).or(() -> fallback);
        chemotype.ifPresent(value -> tooltip.accept(value.tooltipLine()));
        // Only the plant with nothing in it at all loses the lines: a CBG-dominant bud's band is how much
        // CBG it carries, so that one keeps it. A cannabinoid-free bud has no resin worth the sieve either.
        if (chemotype.filter(value -> value == Chemotype.CANNABINOID_FREE).isEmpty()) {
            band.ifPresent(value -> tooltip.accept(value.tooltipLine()));
            resin.flatMap(ResinStep::tooltipLine).ifPresent(tooltip);
        }
        tier.ifPresent(value -> tooltip.accept(value.tooltipLine()));
        HolderLookup.Provider registries = context.registries();
        Optional<Terpene> terpene = strain.flatMap(value -> value.loadedTerpene(registries));
        if (terpene.isEmpty() && registries != null && stack.getItem() instanceof BlockItem plants) {
            // A plant with no strain record (the fibre family) still smells of something.
            terpene = Terpene.ofPlant(plants.getBlock(), registries);
        }
        terpene.ifPresent(value -> tooltip.accept(value.tooltipLine()));
    }

    /** A strain's buds. */
    public static class Buds extends Item {
        public Buds(Properties settings) {
            // Mid and Dank on the prototype, so an explicit mid or Dank stacks with a plain bud; never a chemotype.
            super(settings.component(ModComponents.POTENCY_BAND, PotencyBand.MID)
                    .component(ModComponents.BUD_TIER, BudTier.DANK));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                    Consumer<Component> tooltip, TooltipFlag flag) {
            // Mid included: an unmarked bud is mid, and saying so tells it apart from a high one.
            appendProfile(stack, context, tooltip, Optional.empty(), Optional.of(PotencyBand.of(stack)),
                    Optional.of(ResinStep.of(stack)), Optional.of(BudTier.of(stack)));
            super.appendHoverText(stack, context, display, tooltip, flag);
        }
    }

    /**
     * A strain's seeds, which plant its crop, or another item that places a hemp plant (Ditchweed's).
     * {@code fallback} speaks for a plant with no strain record.
     */
    public static class Seeds extends net.minecraft.world.item.BlockItem {
        private final Optional<Chemotype> fallback;

        public Seeds(net.minecraft.world.level.block.Block crop, Properties settings) {
            this(crop, settings, Optional.empty());
        }

        public Seeds(net.minecraft.world.level.block.Block crop, Properties settings, Optional<Chemotype> fallback) {
            super(crop, settings);
            this.fallback = fallback;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                    Consumer<Component> tooltip, TooltipFlag flag) {
            appendProfile(stack, context, tooltip, fallback, Optional.empty());
            super.appendHoverText(stack, context, display, tooltip, flag);
        }
    }
}
