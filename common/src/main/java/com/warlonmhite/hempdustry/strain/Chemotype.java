package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.component.ModComponents;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;
import com.warlonmhite.hempdustry.block.custom.IndustrialHempCropBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * What a plant makes: THC, CBD, both, or neither. The five chemotypes are the real ones, named the
 * way the literature names them (de Meijer et al. 2003 and 2009), and never numbered in front of the
 * player.
 *
 * <h2>Mostly one gene</h2>
 *
 * Chemotype is the B locus: THCA synthase and CBDA synthase, co-dominant. Two THC copies make a
 * THC-dominant plant, two CBD copies a CBD-dominant one, one of each a balanced one. CBG-dominant
 * plants make neither enzyme's product (the French fibre cultivar Santhica 27), and cannabinoid-free
 * lines were bred out of fibre hemp. So a strain has one chemotype, and it is the strain's, never
 * rolled per plant.
 *
 * <h2>The weights</h2>
 *
 * How much THC and CBD a unit of the material carries, counted in halves so that every sum stays a
 * whole number: THC-dominant is 2 halves of THC, balanced 1 of each, CBD-dominant 2 of CBD. The two
 * that make neither weigh nothing in both, which is why the oven and the Infuser refuse them: they
 * would make an edible that does nothing.
 *
 * <h2>Absent means THC-dominant</h2>
 *
 * Everything 2.1 made — every leaf, schwag, decarboxylated hemp and hash in every world — was THC by
 * definition, so an item with no {@code hempdustry:chemotype} component reads THC-dominant, and
 * {@link #stamp} never writes that value. A new THC-dominant leaf therefore still stacks with an old
 * one: only the chemotypes 2.1 never had cost a slot.
 *
 * <p>The stamped items carry THC-dominant on their prototype as well, the way vanilla's potions carry
 * an empty {@code potion_contents}: a stack that names the default explicitly (a {@code /give}, a
 * datapack's {@code set_components}, another mod's copy) then loses it on the way in, as vanilla drops
 * any value equal to the prototype's, and still stacks with a plain one.
 */
public enum Chemotype implements StringRepresentable {
    THC_DOMINANT("thc_dominant", 2, 0),
    BALANCED("balanced", 1, 1),
    CBD_DOMINANT("cbd_dominant", 0, 2),
    CBG_DOMINANT("cbg_dominant", 0, 0),
    CANNABINOID_FREE("cannabinoid_free", 0, 0);

    /** Halves of THC or CBD in one unit of a material of the strongest chemotype. */
    public static final int FULL_WEIGHT = 2;

    public static final Codec<Chemotype> CODEC = StringRepresentable.fromEnum(Chemotype::values);
    /**
     * Ordinal-based: this only travels over the wire, never to disk. An id out of range (a bad
     * creative packet) reads THC-dominant, the absent default, rather than throwing.
     */
    public static final StreamCodec<ByteBuf, Chemotype> PACKET_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(Chemotype::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), Chemotype::ordinal);

    private final String name;
    private final int thcHalves;
    private final int cbdHalves;

    Chemotype(String name, int thcHalves, int cbdHalves) {
        this.name = name;
        this.thcHalves = thcHalves;
        this.cbdHalves = cbdHalves;
    }

    /** THC in one unit, in halves: 2, 1 or 0. */
    public int thcHalves() {
        return thcHalves;
    }

    /** CBD in one unit, in halves: 0, 1 or 2. */
    public int cbdHalves() {
        return cbdHalves;
    }

    public boolean hasThc() {
        return thcHalves > 0;
    }

    /**
     * Whether it can become an edible. The two chemotypes that make neither THC nor CBD are turned
     * away at the oven and the Infuser, the two doors into one; everywhere else they work.
     */
    public boolean infusable() {
        return thcHalves > 0 || cbdHalves > 0;
    }

    /**
     * The THC dose of {@code units} of this material: a whole number of THC-dominant units' worth,
     * rounded down, but never below one when there is any THC in it at all. So a balanced bud is a
     * dose of one, as is a pipe of two, and a bong of three balanced buds is still one.
     */
    public int thcDose(int units) {
        if (units <= 0 || thcHalves == 0) {
            return 0;
        }
        return Math.max(1, units * thcHalves / FULL_WEIGHT);
    }

    /** The item's stamp, or THC-dominant when it carries none. */
    public static Chemotype of(ItemStack stack) {
        return stack.getOrDefault(ModComponents.CHEMOTYPE, THC_DOMINANT);
    }

    /**
     * What a piece of hemp matter is, whatever it is: a plant's bud is its strain's chemotype (a bud
     * never carries a stamp), anything else its stamp. This is the question every machine asks of
     * what goes into it.
     */
    public static Chemotype ofMatter(ItemStack stack, HolderLookup.Provider registries) {
        // A bud's item carries no chemotype, not even on its prototype: its strain answers.
        if (!stack.has(ModComponents.CHEMOTYPE)) {
            Optional<Holder.Reference<Strain>> strain = Strain.fromBuds(registries, stack.getItem());
            if (strain.isPresent() && strain.get().value().wildFlower().isPresent()) {
                return strain.get().value().chemotype();
            }
        }
        return of(stack);
    }

    /**
     * What a growing plant makes: its strain's chemotype, the strain found by the crop its seeds
     * plant or by its wild flower. Industrial Hemp has no strain record and is CBD-dominant, as real
     * fibre hemp is; Ditchweed, its feral descendant, is balanced, as its drops are; anything else
     * unknown reads THC-dominant.
     */
    public static Chemotype ofPlant(Block block, HolderLookup.Provider registries) {
        if (block instanceof IndustrialHempCropBlock) {
            return CBD_DOMINANT;
        }
        if (block == ModBlocks.DITCHWEED) {
            return BALANCED;
        }
        return Strain.ofPlant(block, registries).map(strain -> strain.value().chemotype()).orElse(THC_DOMINANT);
    }

    /**
     * Writes {@code chemotype} on {@code stack}; THC-dominant writes nothing, so the stack still merges
     * with everything 2.1 made. Returns the stack. See {@link ModComponents#putDefaulted}.
     */
    public static ItemStack stamp(ItemStack stack, Chemotype chemotype) {
        return ModComponents.putDefaulted(stack, ModComponents.CHEMOTYPE, chemotype, THC_DOMINANT);
    }

    /**
     * The chemotype of a batch: what an Infuser, a sieve or a bhang makes of mixed material.
     *
     * <p>The batch's share of THC among its cannabinoids decides: two thirds or more is THC-dominant,
     * a third or less CBD-dominant, anything between balanced — so four THC-dominant hemp and four
     * CBD-dominant make a balanced batch, as a dosage would. A batch of one chemotype is that
     * chemotype, the two that make neither included; material with neither cannabinoid only dilutes
     * a mixed batch, and a mix of nothing but those two is CBG-dominant, since only the cannabinoid-free
     * one has nothing at all.
     *
     * @param counts how many units of each chemotype, indexed by {@link #ordinal()}
     */
    public static Chemotype dosage(int[] counts) {
        Chemotype only = null;
        int kinds = 0;
        long thc = 0;
        long cbd = 0;
        for (Chemotype chemotype : values()) {
            int count = counts[chemotype.ordinal()];
            if (count <= 0) {
                continue;
            }
            only = chemotype;
            kinds++;
            thc += (long) count * chemotype.thcHalves;
            cbd += (long) count * chemotype.cbdHalves;
        }
        if (kinds == 0) {
            return THC_DOMINANT;
        }
        if (kinds == 1) {
            return only;
        }
        return thc + cbd == 0 ? CBG_DOMINANT : ofShares(thc, cbd);
    }

    /**
     * The chemotype of material carrying {@code thc} and {@code cbd} (any unit, the same for both),
     * by the dosage rule: two thirds THC or more is THC-dominant, a third or less CBD-dominant,
     * between balanced. Nothing at all reads THC-dominant, the absent default.
     */
    public static Chemotype ofShares(long thc, long cbd) {
        long total = thc + cbd;
        if (total <= 0) {
            return THC_DOMINANT;
        }
        if (3 * thc >= 2 * total) {
            return THC_DOMINANT;
        }
        return 3 * thc <= total ? CBD_DOMINANT : BALANCED;
    }

    /**
     * Marks a cannabutter with what its batch was: its chemotype word, and its CBD sum beside the THC
     * sum its {@code strength} already is. THC-dominant and no CBD write nothing, so butter made the
     * 2.1 way is still the stack 2.1 made.
     */
    public static ItemStack stampButter(ItemStack butter, Chemotype chemotype, int cbdStrength) {
        stamp(butter, chemotype);
        if (cbdStrength > 0) {
            butter.set(ModComponents.CBD_STRENGTH, cbdStrength);
        } else {
            butter.remove(ModComponents.CBD_STRENGTH);
        }
        return butter;
    }

    /** Translation key of the word the player reads, e.g. {@code hempdustry.chemotype.balanced}. */
    public String getTranslationKey() {
        return "hempdustry.chemotype." + name;
    }

    /** The grey tooltip line. */
    public Component tooltipLine() {
        return Component.translatable(getTranslationKey()).withStyle(ChatFormatting.GRAY);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
