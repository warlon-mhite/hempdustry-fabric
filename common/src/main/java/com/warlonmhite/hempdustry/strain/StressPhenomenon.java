package com.warlonmhite.hempdustry.strain;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * The four things a plant can be more or less hardy against, each with its own {@link Resistance} rolled at
 * planting. Every cause of stress belongs to one:
 *
 * <table>
 *   <tr><th>phenomenon</th><th>causes</th></tr>
 *   <tr><td>{@link #DRY_GROUND}</td><td>a step in a dry bed; farmland at moisture 0</td></tr>
 *   <tr><td>{@link #OVERWATERING}</td><td>a bottle into a wet pot; the pump off over water; bone meal on a
 *       flowering plant in a fed bed</td></tr>
 *   <tr><td>{@link #LIGHT}</td><td>losing the light</td></tr>
 *   <tr><td>{@link #MILDEW}</td><td>powdery mildew's onset and spread</td></tr>
 * </table>
 *
 * <p>Overwatering and overfeeding differ in reality, but both hurt the roots and look alike, and growers mix
 * them up: one phenomenon is honest. Mildew exists only on the 26.x lines, but the resistance is rolled on
 * every line, so a world moved between lines keeps its plants' genetics.
 */
public enum StressPhenomenon implements StringRepresentable {
    DRY_GROUND("dry_ground"),
    OVERWATERING("overwatering"),
    LIGHT("light"),
    MILDEW("mildew");

    public static final Codec<StressPhenomenon> CODEC = StringRepresentable.fromEnum(StressPhenomenon::values);

    private final String name;

    StressPhenomenon(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
