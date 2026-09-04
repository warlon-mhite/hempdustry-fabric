package com.warlonmhite.hempdustry.item;

import com.warlonmhite.hempdustry.Hempdustry;
import net.minecraft.util.Identifier;

/**
 * The ids the smoking gear's <b>client item definitions</b> key on.
 *
 * <h2>Why these live in a holder of their own</h2>
 *
 * Each id has <b>two</b> readers that must agree exactly: the datagen model provider writes it into
 * {@code assets/hempdustry/items/*.json}, and the client registers the implementation under it. They used to be two
 * separate string literals, which is a silent-failure waiting to happen — a typo on either side
 * produces a model that simply never matches, with no error anywhere.
 *
 * <p>They are not on the model provider itself because that class extends a datagen API type, and
 * the client has no business class-loading a data generator to read a constant.
 *
 * <h2>The two properties answer different questions</h2>
 *
 * <ul>
 *   <li>{@link #STRAIN} — a numeric property carrying the loaded strain's {@code model_index}, for
 *       a strain that ships bespoke art rather than taking the shared look. {@code 0} means "no art
 *       of my own", which is every strain a datapack can add.</li>
 *   <li>{@link #STRAIN_TINT} — the tint source that paints a strain's own colour onto the shared
 *       art, and the half of the system a datapack can actually reach.</li>
 * </ul>
 *
 * <p>There is no {@code hempdustry:packed} any more. "Is anything loaded" is
 * {@code minecraft:has_component} on {@code hempdustry:smoke_contents} since 1.21.4 — vanilla ships
 * the question, so the mod stopped answering it. (Conflating the two is what broke the devices
 * once: they discriminated empty from packed with {@code strain >= 1}, which held only while every
 * strain carried a non-zero index.)
 *
 * <p>{@link #STRAIN} is dispatched on with {@code minecraft:range_dispatch}, whose thresholds match
 * {@code >=} and must therefore be listed ascending — in the datagen'd definitions and in any
 * resource pack that extends them.
 */
public final class ModItemProperties {

    /** {@code hempdustry:strain} — the loaded strain's {@code model_index}; 0 for "no bespoke art". */
    public static final Identifier STRAIN = Identifier.of(Hempdustry.MOD_ID, "strain");

    /** {@code hempdustry:strain} as a tint source. Same path, a different registry. */
    public static final Identifier STRAIN_TINT = Identifier.of(Hempdustry.MOD_ID, "strain");

    /**
     * The tint index carrying the strain's colour, and therefore <b>which texture layer gets tinted</b>.
     *
     * <h2>How item tinting actually works, because it is not obvious</h2>
     *
     * A model does not write {@code "tintindex"} anywhere for a flat item. The model builds one set of
     * quads per {@code layerN} in the {@code textures} map and passes <b>the layer number as the tint
     * index</b> — {@code layer0} is tint 0, {@code layer1} is tint 1, up to {@code layer4}. The
     * client item definition's {@code tints} array then answers <b>by position</b>: entry 0 colours
     * index 0, entry 1 colours index 1. So a layer that must stay as drawn takes a constant white.
     *
     * <p>So the contract for anything strain-tinted is:
     *
     * <ul>
     *   <li><b>{@code layer0}</b> — the object itself, full colour, <b>never tinted</b>. Its entry in
     *       {@code tints} is a constant white, which multiplies to no change.</li>
     *   <li><b>{@code layer1}</b> — a mask covering only the part that should take the strain's
     *       colour, painted near-white where the colour should read at full strength. Tinting is a
     *       multiply, so a grey pixel yields a darker shade and <b>no pixel can come out lighter than
     *       the tint colour</b> — which is why a strain's {@code color} is the brightest point of its
     *       range rather than its midpoint.</li>
     * </ul>
     *
     * This is exactly the shape vanilla gives wolf armour: a constant white first, its dye tint
     * second.
     *
     * <p><b>This is a resource-pack contract.</b> A pack replacing the smoking gear's art has to keep
     * the layer split, and a pack adding art for a new strain relies on it. Changing which layer is
     * tinted would break every such pack silently.
     */
    public static final int LOAD_TINT_INDEX = 1;

    private ModItemProperties() {
    }
}
