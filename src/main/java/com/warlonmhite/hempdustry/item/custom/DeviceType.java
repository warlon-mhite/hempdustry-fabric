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
    //   registry base   packedTexture   maxDamage  bowlSize  maxDose  duration  enchantability  cooldown  cough(1-in-N)  nausea(1-in-N)
    PIPE("wooden_pipe",  "packed_pipe",  8,         2,        2,       700,      15,             60,       4,             50),
    BONG("bong",         "packed_bong",  24,        4,        3,       1000,     10,             100,      3,             5);

    private final String baseName;
    private final String packedTexture;
    private final int maxDamage;
    private final int bowlSize;
    private final int maxDose;
    private final int durationTicks;
    private final int enchantability;
    private final int cooldownTicks;
    private final int coughChanceOneIn;
    private final int nauseaChanceOneIn;

    DeviceType(String baseName, String packedTexture, int maxDamage, int bowlSize, int maxDose,
               int durationTicks, int enchantability, int cooldownTicks, int coughChanceOneIn,
               int nauseaChanceOneIn) {
        this.baseName = baseName;
        this.packedTexture = packedTexture;
        this.maxDamage = maxDamage;
        this.bowlSize = bowlSize;
        this.maxDose = maxDose;
        this.durationTicks = durationTicks;
        this.enchantability = enchantability;
        this.cooldownTicks = cooldownTicks;
        this.coughChanceOneIn = coughChanceOneIn;
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

    /**
     * Most buds this device's bowl will take, i.e. the highest effect level it can reach.
     *
     * <p>This is what gives the bong its identity honestly — not "stronger", but <em>capable of a
     * bigger hit</em>. A bong at dose 1 is exactly as strong as a pipe at dose 1.
     */
    public int maxDose() {
        return maxDose;
    }

    /**
     * How long this device's effects last. <b>Duration is purely the device and amplifier is purely
     * the dose</b> — see {@link Strain#effects} for why the two must stay orthogonal here rather
     * than trading off the way vanilla's glowstone does.
     */
    public int durationTicks() {
        return durationTicks;
    }

    /** Odds of nausea per hit, as 1-in-N (pipe 1-in-50 = 2%, bong 1-in-5 = 20%). */
    public int nauseaChanceOneIn() {
        return nauseaChanceOneIn;
    }
}
