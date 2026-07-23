package com.warlonmhite.hempdustry.component;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.UnaryOperator;

/**
 * Custom data components. Currently just {@code charges}: how many hits are left in a packed
 * pipe/bong's bowl. Durability is handled by the vanilla {@code minecraft:damage} component, so it
 * survives the empty ⇄ packed conversions on its own — this only tracks the current bowl.
 */
public class ModComponents {

    public static final ComponentType<Integer> CHARGES = register("charges",
            builder -> builder.codec(Codec.INT).packetCodec(PacketCodecs.VAR_INT));

    private static ComponentType<Integer> register(String id, UnaryOperator<ComponentType.Builder<Integer>> builderOp) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE,
                Identifier.of(Hempdustry.MOD_ID, id),
                builderOp.apply(ComponentType.builder()).build());
    }

    public static void registerModComponents() {
        Hempdustry.LOGGER.info("Registering Data Components for " + Hempdustry.MOD_ID);
    }
}
