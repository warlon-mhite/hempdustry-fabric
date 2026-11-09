package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.strain.Chemotype;
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
 * strain's chemotype in a word, grey, then its terpene's name in the terpene's colour. Read off the
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
     * record, then the strain's terpene if the world defines it.
     */
    public static void appendProfile(ItemStack stack, Item.TooltipContext context, Consumer<Component> tooltip,
                                     Optional<Chemotype> fallback) {
        Optional<Strain> strain = strainOf(stack.getItem(), context);
        strain.map(Strain::chemotype).or(() -> fallback).ifPresent(value -> tooltip.accept(value.tooltipLine()));
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
            super(settings);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                    Consumer<Component> tooltip, TooltipFlag flag) {
            appendProfile(stack, context, tooltip, Optional.empty());
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
            appendProfile(stack, context, tooltip, fallback);
            super.appendHoverText(stack, context, display, tooltip, flag);
        }
    }
}
