package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.util.Identifier;

/**
 * The item-model properties the smoking gear's models key on.
 *
 * <h2>Why these live in a holder of their own</h2>
 *
 * Each id has <b>two</b> readers that must agree exactly: the datagen model provider writes it into
 * the {@code overrides} array, and the client registers a provider under it. They used to be two
 * separate string literals, which is a silent-failure waiting to happen — a typo on either side
 * produces a model that simply never matches, with no error anywhere.
 *
 * <p>They are not on the model provider itself because that class extends a datagen API type, and
 * the client has no business class-loading a data generator to read a constant.
 *
 * <h2>The two properties answer different questions</h2>
 *
 * <ul>
 *   <li>{@link #PACKED} — 0 or 1, "is anything loaded in this device". Switches a pipe or bong
 *       between its empty and packed models.</li>
 *   <li>{@link #STRAIN} — the loaded strain's {@code model_index}, for a strain that ships bespoke
 *       art rather than taking the shared look. {@code 0} means "no art of my own".</li>
 * </ul>
 *
 * Conflating the two is what broke the devices: they discriminated empty from packed with
 * {@code strain >= 1}, which held only while every strain carried a non-zero index.
 *
 * <p>Both are matched with {@code >=}, so overrides using them must be listed in ascending order —
 * in the datagen'd models and in any resource pack that extends them.
 */
public final class ModItemProperties {

    /** {@code hempdustry:packed} — 0 when the device is empty, 1 when something is loaded. */
    public static final Identifier PACKED = Identifier.of(Hempdustry.MOD_ID, "packed");

    /** {@code hempdustry:strain} — the loaded strain's {@code model_index}; 0 for "no bespoke art". */
    public static final Identifier STRAIN = Identifier.of(Hempdustry.MOD_ID, "strain");

    private ModItemProperties() {
    }
}
