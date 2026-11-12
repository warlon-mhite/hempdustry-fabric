package com.warlonmhite.hempdustry.component;

import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import com.warlonmhite.hempdustry.strain.Chemotype;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import java.util.function.UnaryOperator;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

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
    public static final DataComponentType<SmokeContents> SMOKE_CONTENTS = register("smoke_contents",
            builder -> builder.persistent(SmokeContents.CODEC).networkSynchronized(SmokeContents.PACKET_CODEC));

    /**
     * An edible's potency tier, 1–4. Distinct from {@link #STRENGTH}, which is cannabutter's raw
     * 1–24 hemp count: the tier is what the butter's strength collapses to, plus the edible's own
     * offset. Four values rather than twenty-four is what keeps edible stacks merging.
     */
    public static final DataComponentType<Integer> POTENCY = register("potency",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    public static final DataComponentType<Integer> CHARGES = register("charges",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * World time at which this stack's smoking cooldown ends. Present only on the one stack the
     * player actually took a hit from, and cleared again the moment it lapses (see
     * {@link com.warlonmhite.hempdustry.item.custom.Smoking#expire}).
     *
     * <p>It is <b>not</b> what blocks the next hit — that is the vanilla {@link
     * net.minecraft.world.item.ItemCooldowns}, which a hit arms for every smokeable at
     * once. This exists purely so the cooldown <em>swipe</em> can be drawn on the stack that was
     * used and on nothing else: the cooldown manager is keyed by {@code Item}, so without a mark
     * living on the stack itself the overlay has no way to tell one packed pipe from the next.
     *
     * <p>Storing the end tick rather than the start is what keeps the client out of the config: it
     * needs no knowledge of how long the cooldown was to know whether this mark is still live.
     */
    public static final DataComponentType<Long> COOLDOWN_UNTIL = register("cooldown_until",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** Total hemp items that went into the batch. Drives dose; see the Infuser. */
    public static final DataComponentType<Integer> STRENGTH = register("strength",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /**
     * Cannabutter's CBD sum, beside {@link #STRENGTH}, which is its THC sum: one per CBD-dominant
     * hemp, half per balanced one, capped like strength. Absent is 0, every butter 2.1 made. Edibles
     * carry the tier it reads as in {@link #CBD_POTENCY}, as they carry strength's in {@link #POTENCY}.
     */
    public static final DataComponentType<Integer> CBD_STRENGTH = register("cbd_strength",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** An edible's CBD tier, 1–4, read off its butter's {@link #CBD_STRENGTH}. Absent: no CBD. */
    public static final DataComponentType<Integer> CBD_POTENCY = register("cbd_potency",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** How well the batch was made — patience plus ingredient prep. */
    public static final DataComponentType<Quality> QUALITY = register("quality",
            builder -> builder.persistent(Quality.CODEC).networkSynchronized(Quality.PACKET_CODEC));

    /**
     * What a piece of shared hemp matter carries from its plant: THC-dominant, balanced, CBD-dominant
     * and the two that make neither. Absent reads THC-dominant, everything 2.1 made, and that value is
     * never written ({@link Chemotype#stamp}). Leaves, schwag, decarboxylated and scorched hemp and the
     * whole hash family carry it, because the matter is the same item whatever plant it came from; a
     * bud and a seed never do, their strain is their chemotype. The stamped items, butter and the
     * edibles carry THC-dominant on their prototype (see {@link #putDefaulted}); a bud and a seed don't.
     */
    public static final DataComponentType<Chemotype> CHEMOTYPE = register("chemotype",
            builder -> builder.persistent(Chemotype.CODEC).networkSynchronized(Chemotype.PACKET_CODEC));

    /**
     * The potency band of the plant a bud came from, or a leaf's mark ({@code high}, from a high or
     * extra-high plant). Absent is mid, every bud and leaf 2.1 made; mid is never written, so a new mid
     * bud stacks with an old one. Nothing after the oven carries it. Buds and the leaf carry mid on their
     * prototype (see {@link #putDefaulted}). See {@link PotencyBand}.
     */
    public static final DataComponentType<PotencyBand> POTENCY_BAND = register("potency_band",
            builder -> builder.persistent(PotencyBand.CODEC).networkSynchronized(PotencyBand.PACKET_CODEC));

    /** What a Stash Jar holds, on the item and in the placed block's entity. See {@link StashContents}. */
    public static final DataComponentType<StashContents> STASH_CONTENTS = register("stash_contents",
            builder -> builder.persistent(StashContents.CODEC).networkSynchronized(StashContents.STREAM_CODEC));

    /**
     * Puts {@code value} on {@code stack} so that a default never costs a slot.
     *
     * <p>An item that carries {@code absent} on its prototype ({@code Item.Properties#component}) gets
     * a {@code set}: vanilla keeps a value equal to the prototype's out of the stack's patch, so the
     * default writes nothing. A {@code remove} there would be wrong, as it writes a removal
     * ({@code !hempdustry:chemotype}) that no plain stack has. An item without the prototype value
     * gets the default removed and any other value set, as before the prototypes existed.
     */
    public static <T> ItemStack putDefaulted(ItemStack stack, DataComponentType<T> type, T value, T absent) {
        if (value.equals(absent) && !stack.getPrototype().has(type)) {
            stack.remove(type);
        } else {
            stack.set(type, value);
        }
        return stack;
    }

    private static <T> DataComponentType<T> register(String id, UnaryOperator<DataComponentType.Builder<T>> builderOp) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Hempdustry.MOD_ID, id),
                builderOp.apply(DataComponentType.<T>builder()).build());
    }

    public static void registerModComponents() {
        Hempdustry.LOGGER.info("Registering Data Components for " + Hempdustry.MOD_ID);
    }
}
