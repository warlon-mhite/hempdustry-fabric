package com.warlonmhite.hempdustry.component;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.custom.Quality;
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

    public static final ComponentType<Integer> CHARGES = register("charges",
            builder -> builder.codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT));

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
