package com.warlonmhite.hempdustry.item.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * What is loaded in a spliff, pipe or bong — the payload of the {@code hempdustry:smoke_contents}
 * data component.
 *
 * <p>This is the mod's answer to {@code minecraft:potion_contents}: <b>one item per device</b> with
 * the strain carried as data, rather than a separate registered item per device × strain. See
 * CLAUDE.md §5b D10. Empty contents means an unpacked device.
 *
 * <h2>Why this is a list when nothing mixes yet</h2>
 *
 * It will always hold exactly one entry until strain mixing lands. It is a list anyway because
 * <b>changing a component's codec after release is a world migration</b>, and a list costs nothing
 * now. {@code minecraft:firework_explosion}'s star list is the same shape for the same reason.
 *
 * <p>Until mixing is designed properly, a multi-entry blend resolves the obvious way: <b>each
 * strain applies its own effects at its own bud count</b>, so a 2+1 mix is one strain at level II
 * and another at level I. That falls out as a broader-but-weaker trade against a single strain at
 * level III, which is roughly the fixed-budget shape mixing wants anyway — but it has not been
 * balanced, so treat it as a sane default rather than a design.
 */
public record SmokeContents(List<Entry> entries) {

    public static final SmokeContents EMPTY = new SmokeContents(List.of());

    public static final Codec<SmokeContents> CODEC = Entry.CODEC.listOf()
            .xmap(SmokeContents::new, SmokeContents::entries);

    public static final PacketCodec<RegistryByteBuf, SmokeContents> PACKET_CODEC =
            Entry.PACKET_CODEC.collect(PacketCodecs.toList())
                    .xmap(SmokeContents::new, SmokeContents::entries)
                    .cast();

    /** A single strain's share of the load. */
    public record Entry(Strain strain, int count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Strain.CODEC.fieldOf("strain").forGetter(Entry::strain),
                Codec.INT.fieldOf("count").forGetter(Entry::count)
        ).apply(instance, Entry::new));

        public static final PacketCodec<ByteBuf, Entry> PACKET_CODEC = PacketCodec.tuple(
                Strain.PACKET_CODEC, Entry::strain,
                PacketCodecs.VAR_INT, Entry::count,
                Entry::new);
    }

    /** A single-strain load — the only shape anything produces today. */
    public static SmokeContents of(Strain strain, int count) {
        return new SmokeContents(List.of(new Entry(strain, count)));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Total buds loaded. This is the <em>dose</em>, and it drives the amplifier and green-out odds. */
    public int dose() {
        int total = 0;
        for (Entry entry : entries) {
            total += entry.count();
        }
        return total;
    }

    /** The dominant strain, for naming and tinting. {@code null} only when empty. */
    public Strain primaryStrain() {
        Entry best = null;
        for (Entry entry : entries) {
            if (best == null || entry.count() > best.count()) {
                best = entry;
            }
        }
        return best == null ? null : best.strain();
    }

    /** True once more than one strain is loaded — naming and tinting both branch on this. */
    public boolean isBlend() {
        return entries.size() > 1;
    }

    /**
     * Average of the loaded strains' colours, weighted by bud count — the same approach
     * {@code PotionContentsComponent#getColor} takes across a potion's effects. Falls back to a
     * neutral tint when empty so a colour provider never has to null-check.
     */
    public int color() {
        if (entries.isEmpty()) {
            return 0xFFFFFF;
        }
        int r = 0, g = 0, b = 0, total = 0;
        for (Entry entry : entries) {
            int color = entry.strain().color();
            int weight = Math.max(1, entry.count());
            r += ((color >> 16) & 0xFF) * weight;
            g += ((color >> 8) & 0xFF) * weight;
            b += (color & 0xFF) * weight;
            total += weight;
        }
        return ((r / total) << 16) | ((g / total) << 8) | (b / total);
    }

    /** The display name of what is loaded — a strain name, or "Mixed" for a blend. */
    public Text loadName() {
        if (isBlend()) {
            return Text.translatable("hempdustry.strain.blend");
        }
        Strain strain = primaryStrain();
        return strain == null ? Text.empty() : Text.translatable(strain.getTranslationKey());
    }

    /**
     * The item name for a loaded device or spliff — "Purple Kush Bong III".
     *
     * <p>Built potion-style from a per-device format key plus the load's own name, so a new strain
     * costs <b>one</b> lang key across all eight locales instead of one per device. The format string
     * being per-locale is what lets French say "Bang de %s". The level suffix reuses vanilla's
     * {@code enchantment.level.N} keys and is omitted at dose 1, exactly as vanilla writes "Potion of
     * Strength" and "Potion of Strength II".
     */
    public static Text packedName(String formatKey, SmokeContents contents) {
        MutableText name = Text.translatable(formatKey, contents.loadName());
        int dose = contents.dose();
        if (dose > 1) {
            name.append(ScreenTexts.SPACE).append(Text.translatable("enchantment.level." + dose));
        }
        return name;
    }

    /** Every status effect one hit of this load applies, lasting {@code durationTicks}. */
    public List<StatusEffectInstance> effects(int durationTicks) {
        List<StatusEffectInstance> out = new ArrayList<>();
        for (Entry entry : entries) {
            out.addAll(entry.strain().effects(entry.count(), durationTicks));
        }
        return out;
    }
}
