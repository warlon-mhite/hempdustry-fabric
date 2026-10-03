package com.warlonmhite.hempdustry.balance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.custom.DeviceType;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;

import java.util.Optional;

/**
 * How one smokeable smokes, as data: {@code data/<namespace>/hempdustry/device/<id>.json}, one file
 * for the pipe, the bong (all seventeen bongs share it), the vaporizer and the spliff.
 *
 * <p>Every chance is a 1-in-N where 0 means never. {@code green_out_factor} widens the green-out
 * odds the way a strain's does — 2 halves the risk, 0 or below rules it out — and it is the
 * spliff's 2 that says you pace a joint, where you don't pace a bong rip.
 *
 * <p><b>The spliff has no bowl.</b> It is burnt whole in one hit, and how big a spliff rolls is
 * decided by its crafting recipes, so its file carries no {@code bowl} and one there would not be
 * read. A pipe, bong or vaporizer file without one keeps the built-in bowl: a pipe that packs no
 * hits is a broken pipe, not a rebalanced one.
 */
public record DeviceStats(int durationTicks, int cooldownTicks, int coughOneIn, int nauseaOneIn,
                          float greenOutFactor, Optional<Bowl> bowl) {

    /**
     * A reusable device's bowl: the hits one packing gives, the most buds it holds (which is the
     * highest level the device reaches), and the scorched hemp a finished bowl of plant matter hands
     * back.
     */
    public record Bowl(int hits, int maxDose, int spentYield) {
        public static final Codec<Bowl> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codecs.POSITIVE_INT.fieldOf("hits").forGetter(Bowl::hits),
                Codecs.POSITIVE_INT.fieldOf("max_dose").forGetter(Bowl::maxDose),
                Codecs.NON_NEGATIVE_INT.fieldOf("spent_yield").forGetter(Bowl::spentYield)
        ).apply(instance, Bowl::new));
    }

    public static final RegistryKey<Registry<DeviceStats>> REGISTRY_KEY =
            RegistryKey.ofRegistry(Identifier.of(Hempdustry.MOD_ID, "device"));

    public static final Codec<DeviceStats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codecs.POSITIVE_INT.fieldOf("duration_ticks").forGetter(DeviceStats::durationTicks),
            Codecs.NON_NEGATIVE_INT.fieldOf("cooldown_ticks").forGetter(DeviceStats::cooldownTicks),
            Codecs.NON_NEGATIVE_INT.fieldOf("cough_one_in").forGetter(DeviceStats::coughOneIn),
            Codecs.NON_NEGATIVE_INT.fieldOf("nausea_one_in").forGetter(DeviceStats::nauseaOneIn),
            Codec.FLOAT.fieldOf("green_out_factor").forGetter(DeviceStats::greenOutFactor),
            Bowl.CODEC.optionalFieldOf("bowl").forGetter(DeviceStats::bowl)
    ).apply(instance, DeviceStats::new));

    public static final RegistryKey<DeviceStats> SPLIFF = key("spliff");

    /**
     * The spliff's own numbers. 45 s sits between the pipe's 35 and the bong's 50 — duration is the
     * device's axis, not the dose's — and 1-in-500 nausea makes it the mildest thing in the mod.
     */
    public static final DeviceStats SPLIFF_BUILT_IN = new DeviceStats(900, 80, 6, 500, 2.0F, Optional.empty());

    public static RegistryKey<DeviceStats> key(DeviceType type) {
        return key(type.baseName());
    }

    private static RegistryKey<DeviceStats> key(String id) {
        return RegistryKey.of(REGISTRY_KEY, Identifier.of(Hempdustry.MOD_ID, id));
    }

    public static void bootstrap(Registerable<DeviceStats> registerable) {
        for (DeviceType type : DeviceType.values()) {
            registerable.register(key(type), type.builtInStats());
        }
        registerable.register(SPLIFF, SPLIFF_BUILT_IN);
    }

    /** {@code type}'s numbers in this world. */
    public static DeviceStats of(RegistryWrapper.WrapperLookup registries, DeviceType type) {
        return ModBalance.entry(registries, key(type), type.builtInStats());
    }

    /** The spliff's numbers in this world. */
    public static DeviceStats spliff(RegistryWrapper.WrapperLookup registries) {
        return ModBalance.entry(registries, SPLIFF, SPLIFF_BUILT_IN);
    }

    /** {@code type}'s bowl in this world, or the built-in one when its file has none. */
    public static Bowl bowl(RegistryWrapper.WrapperLookup registries, DeviceType type) {
        return of(registries, type).bowl().orElseGet(() -> type.builtInStats().bowl().orElseThrow());
    }
}
