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

    /**
     * The tint index carrying the strain's colour, and therefore <b>which texture layer gets tinted</b>.
     *
     * <h2>How item tinting actually works, because it is not obvious</h2>
     *
     * An item model does not write {@code "tintindex"} anywhere. {@code ItemModelGenerator} builds one
     * set of quads per {@code layerN} in the {@code textures} map and passes <b>the layer number as
     * the tint index</b> — {@code layer0} is tint 0, {@code layer1} is tint 1, up to {@code layer4}.
     * An {@code ItemColorProvider} then returns a colour per index, or {@code -1} for "leave this one
     * alone".
     *
     * <p>So the contract for anything strain-tinted is:
     *
     * <ul>
     *   <li><b>{@code layer0}</b> — the object itself, full colour, <b>never tinted</b>. The provider
     *       returns {@code -1} for index 0.</li>
     *   <li><b>{@code layer1}</b> — a mask covering only the part that should take the strain's
     *       colour, painted near-white where the colour should read at full strength. Tinting is a
     *       multiply, so a grey pixel yields a darker shade and <b>no pixel can come out lighter than
     *       the tint colour</b> — which is why a strain's {@code color} is the brightest point of its
     *       range rather than its midpoint.</li>
     * </ul>
     *
     * This is exactly the shape vanilla gives wolf armour, whose provider reads
     * {@code tintIndex != 1 ? -1 : dyeColour}.
     *
     * <p><b>This is a resource-pack contract.</b> A pack replacing the smoking gear's art has to keep
     * the layer split, and a pack adding art for a new strain relies on it. Changing which layer is
     * tinted would break every such pack silently.
     */
    public static final int LOAD_TINT_INDEX = 1;

    private ModItemProperties() {
    }
}
