package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.block.custom.IndustrialHempCropBlock;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;

/**
 * A terpene: the smell a strain is known for, and the colour its smoke takes. A datapack registry,
 * {@code data/<namespace>/hempdustry/terpene/<id>.json}, synced to clients like the strains, so a pack
 * may add one or recolour one.
 *
 * <h2>A signature, not the dominant one</h2>
 *
 * A strain names one terpene: the note its smell is known for. Lab tests find only myrcene,
 * caryophyllene, limonene, terpinolene and sometimes pinene really dominating a profile, so most
 * strains' signature is not their most abundant terpene, and that is how growers describe them too.
 *
 * <h2>No effect</h2>
 *
 * The entourage effect is unproven, so a terpene changes nothing a strain does: it is a smell, a word
 * on the tooltip and a colour in the smoke. The colour follows the smell (citrus yellow, lavender
 * violet, pine green), never strength.
 *
 * @param translationKey lang key of its name, e.g. {@code hempdustry.terpene.limonene}
 * @param color          its colour, RGB; the smoke mixes a third of it into its grey, vapour a quarter
 *                       into its white
 */
public record Terpene(String translationKey, int color) {

    public static final ResourceKey<Registry<Terpene>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, "terpene"));

    public static final Codec<Terpene> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(Terpene::translationKey),
            Codec.INT.fieldOf("color").forGetter(Terpene::color)
    ).apply(instance, Terpene::new));

    public static final ResourceKey<Terpene> LIMONENE = key("limonene");
    public static final ResourceKey<Terpene> LINALOOL = key("linalool");
    public static final ResourceKey<Terpene> MYRCENE = key("myrcene");
    public static final ResourceKey<Terpene> CARYOPHYLLENE = key("caryophyllene");
    public static final ResourceKey<Terpene> PINENE = key("pinene");
    public static final ResourceKey<Terpene> EUCALYPTOL = key("eucalyptol");
    public static final ResourceKey<Terpene> TERPINOLENE = key("terpinolene");
    public static final ResourceKey<Terpene> GERANIOL = key("geraniol");
    /**
     * Hops' terpene, the fibre family's: Industrial Hemp and Ditchweed, its feral descendant, which are
     * no strain (see {@link #ofPlant}). No smoking strain names it.
     */
    public static final ResourceKey<Terpene> HUMULENE = key("humulene");
    /** A spare, woody and floral, for a strain to come. No 2.2 strain names it. */
    public static final ResourceKey<Terpene> NEROLIDOL = key("nerolidol");

    /** The share of a terpene's colour that goes into smoke, and into vapour. */
    public static final float SMOKE_SHARE = 0.33F;
    public static final float VAPOUR_SHARE = 0.25F;

    /**
     * The ten, each coloured from its smell: limonene the lemon, linalool lavender, myrcene ripe mango,
     * caryophyllene black pepper's warm red, pinene the pine needle, eucalyptol the cool blue of
     * eucalyptus, terpinolene green apple and lilac's fresh green, geraniol rose pink, humulene the
     * dried hop, nerolidol bark.
     */
    public static void bootstrap(BootstrapContext<Terpene> context) {
        register(context, LIMONENE, 0xF5D547);
        register(context, LINALOOL, 0xB9A0F2);
        register(context, MYRCENE, 0xE8923A);
        register(context, CARYOPHYLLENE, 0xD0603F);
        register(context, PINENE, 0x6DB360);
        register(context, EUCALYPTOL, 0x3FA7C4);
        register(context, TERPINOLENE, 0xA7D84E);
        register(context, GERANIOL, 0xF592B8);
        register(context, HUMULENE, 0xB3AE5C);
        register(context, NEROLIDOL, 0xB0835A);
    }

    private static void register(BootstrapContext<Terpene> context, ResourceKey<Terpene> key, int color) {
        context.register(key, new Terpene("hempdustry.terpene." + key.identifier().getPath(), color));
    }

    /** The terpene the world loaded under {@code key}, if it defines one. */
    public static Optional<Terpene> resolve(HolderLookup.Provider registries, ResourceKey<Terpene> key) {
        return registries.lookup(REGISTRY_KEY).flatMap(lookup -> lookup.get(key)).map(holder -> holder.value());
    }

    /**
     * The terpene a plant of {@code block} smells of: its strain's, or humulene for the fibre family
     * (Industrial Hemp and Ditchweed), which flowers and smells like any hemp but is grown for stalk and
     * seed, so it has no strain record. Empty for anything else, or a terpene the world does not define.
     */
    public static Optional<Terpene> ofPlant(Block block, HolderLookup.Provider registries) {
        Optional<? extends net.minecraft.core.Holder<Strain>> strain = Strain.ofPlant(block, registries);
        if (strain.isPresent()) {
            return strain.get().value().loadedTerpene(registries);
        }
        return isFibreHemp(block) ? resolve(registries, HUMULENE) : Optional.empty();
    }

    private static boolean isFibreHemp(Block block) {
        return block instanceof IndustrialHempCropBlock || block == ModBlocks.DITCHWEED;
    }

    /** The tooltip line: its name, in its own colour. */
    public Component tooltipLine() {
        return Component.translatable(translationKey).withStyle(style -> style.withColor(TextColor.fromRgb(color)));
    }

    /** {@code color} mixed {@code share} of the way into {@code base}, both RGB. */
    public static int mix(int base, int color, float share) {
        int r = Math.round(((base >> 16) & 0xFF) * (1 - share) + ((color >> 16) & 0xFF) * share);
        int g = Math.round(((base >> 8) & 0xFF) * (1 - share) + ((color >> 8) & 0xFF) * share);
        int b = Math.round((base & 0xFF) * (1 - share) + (color & 0xFF) * share);
        return (r << 16) | (g << 8) | b;
    }

    private static ResourceKey<Terpene> key(String name) {
        return ResourceKey.create(REGISTRY_KEY, Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, name));
    }
}
