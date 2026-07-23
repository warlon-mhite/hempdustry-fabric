package com.warlonmhite.hempdustry.item.custom;

/**
 * A reusable smoking device (pipe, bong, …). One enum entry fully describes a device's balance, so
 * the item classes stay strain- and device-agnostic.
 *
 * <p>Durability is measured in <em>hits</em>: a device takes 1 damage per hit and is repaired with
 * the material it's built from. The numbers below keep the original 1:3 pipe:bong fragility ratio
 * and divide into whole bowls (pipe = 4 packs × 2, bong = 6 packs × 4) so a device never shatters
 * mid-bowl. Tune freely.
 */
public enum DeviceType {
    //   registry base   packedTexture   maxDamage  bowlSize  enchantability  cooldown  cough(1-in-N)  potency        nausea(1-in-N)
    PIPE("wooden_pipe",  "packed_pipe",  8,         2,        15,             60,       4,             Potency.LIGHT, 50),
    BONG("bong",         "packed_bong",  24,        4,        10,             100,      3,             Potency.HEAVY, 5);

    private final String baseName;
    private final String packedTexture;
    private final int maxDamage;
    private final int bowlSize;
    private final int enchantability;
    private final int cooldownTicks;
    private final int coughChanceOneIn;
    private final Potency potency;
    private final int nauseaChanceOneIn;

    DeviceType(String baseName, String packedTexture, int maxDamage, int bowlSize, int enchantability,
               int cooldownTicks, int coughChanceOneIn, Potency potency, int nauseaChanceOneIn) {
        this.baseName = baseName;
        this.packedTexture = packedTexture;
        this.maxDamage = maxDamage;
        this.bowlSize = bowlSize;
        this.enchantability = enchantability;
        this.cooldownTicks = cooldownTicks;
        this.coughChanceOneIn = coughChanceOneIn;
        this.potency = potency;
        this.nauseaChanceOneIn = nauseaChanceOneIn;
    }

    /** Registry id of the empty device; packed variants are {@code baseName + "_" + strainId}. */
    public String baseName() {
        return baseName;
    }

    /** Item-texture name (under {@code textures/item/}) shared by all packed variants of this device. */
    public String packedTexture() {
        return packedTexture;
    }

    /** Total hits before the device breaks (vanilla {@code max_damage}). */
    public int maxDamage() {
        return maxDamage;
    }

    /** Hits granted by packing one bowl. */
    public int bowlSize() {
        return bowlSize;
    }

    public int enchantability() {
        return enchantability;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public int coughChanceOneIn() {
        return coughChanceOneIn;
    }

    /** How hard this device hits, selecting effect amplifiers/durations. */
    public Potency potency() {
        return potency;
    }

    /** Odds of nausea per hit, as 1-in-N (pipe 1-in-50 = 2%, bong 1-in-5 = 20%). */
    public int nauseaChanceOneIn() {
        return nauseaChanceOneIn;
    }
}
