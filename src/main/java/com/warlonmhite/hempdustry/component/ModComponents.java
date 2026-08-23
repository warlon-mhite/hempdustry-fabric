package com.warlonmhite.hempdustry.component;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.UnaryOperator;

/**
 * Custom data components.
 *
 * <ul>
 *   <li>{@code charges} — hits left in a packed pipe/bong's bowl. Durability is the vanilla
 *       {@code minecraft:damage} component, so it survives the empty ⇄ packed conversions on its
 *       own; this only tracks the current bowl.</li>
 *   <li>{@code strength} / {@code quality} — cannabutter's two independent axes (see
 *       {@link Quality}). Strength is how much hemp went into the batch; Quality is how well it was
 *       made. They are separate components rather than one because they are genuinely orthogonal:
 *       a rushed batch can be strong, and a patient one can be weak.</li>
 * </ul>
 *
 * <p><b>Consequence worth knowing:</b> stacks differing in any component don't stack together, so
 * cannabutter of different grades won't merge. That is deliberate and vanilla-precedented —
 * potions and suspicious stew behave the same way — but it does mean a player juggling several
 * grades will use several inventory slots.
 */
public class ModComponents {

    /**
     * What is loaded in a spliff / pipe / bong. Absent or empty means an unpacked device.
     *
     * <p>This is the mod's {@code potion_contents}: one item per device carrying the strain as data,
     * rather than a registered item per device × strain. Kept separate from {@link #CHARGES} because
     * the two have different lifetimes — charges deplete per hit, contents don't.
     */
    public static final ComponentType<SmokeContents> SMOKE_CONTENTS = register("smoke_contents",
            builder -> builder.codec(SmokeContents.CODEC).packetCodec(SmokeContents.PACKET_CODEC));

    /**
     * An edible's potency tier, 1–4. Distinct from {@link #STRENGTH}, which is cannabutter's raw
     * 1–24 hemp count: the tier is what the butter's strength collapses to, plus the edible's own
     * offset. Four values rather than twenty-four is what keeps edible stacks merging.
     */
    public static final ComponentType<Integer> POTENCY = register("potency",
            builder -> builder.codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT));

    public static final ComponentType<Integer> CHARGES = register("charges",
            builder -> builder.codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT));

    /**
     * World time at which this stack's smoking cooldown ends. Present only on the one stack the
     * player actually took a hit from, and cleared again the moment it lapses (see
     * {@link com.warlonmhite.hempdustry.item.custom.Smoking#expire}).
     *
     * <p>It is <b>not</b> what blocks the next hit — that is the vanilla {@link
     * net.minecraft.entity.player.ItemCooldownManager}, which a hit arms for every smokeable at
     * once. This exists purely so the cooldown <em>swipe</em> can be drawn on the stack that was
     * used and on nothing else: the cooldown manager is keyed by {@code Item}, so without a mark
     * living on the stack itself the overlay has no way to tell one packed pipe from the next.
     *
     * <p>Storing the end tick rather than the start is what keeps the client out of the config: it
     * needs no knowledge of how long the cooldown was to know whether this mark is still live.
     */
    public static final ComponentType<Long> COOLDOWN_UNTIL = register("cooldown_until",
            builder -> builder.codec(Codec.LONG).packetCodec(PacketCodecs.VAR_LONG));

    /** Total hemp items that went into the batch. Drives dose; see the Infuser. */
    public static final ComponentType<Integer> STRENGTH = register("strength",
            builder -> builder.codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT));

    /** How well the batch was made — patience plus ingredient prep. */
    public static final ComponentType<Quality> QUALITY = register("quality",
            builder -> builder.codec(Quality.CODEC).packetCodec(Quality.PACKET_CODEC));

    private static <T> ComponentType<T> register(String id, UnaryOperator<ComponentType.Builder<T>> builderOp) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE,
                Identifier.of(Hempdustry.MOD_ID, id),
                builderOp.apply(ComponentType.<T>builder()).build());
    }

    public static void registerModComponents() {
        Hempdustry.LOGGER.info("Registering Data Components for " + Hempdustry.MOD_ID);
    }
}
