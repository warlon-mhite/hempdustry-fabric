package com.warlonmhite.hempdustry.item.custom;

/**
 * How hard a smoking device hits. Spliffs and pipes are {@link #LIGHT}; bongs are {@link #HEAVY}.
 * A strain reads this to pick each effect's amplifier and duration (see {@link Strain#effects}).
 */
public enum Potency {
    LIGHT,
    HEAVY
}
