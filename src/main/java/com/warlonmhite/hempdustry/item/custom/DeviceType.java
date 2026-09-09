package com.warlonmhite.hempdustry.item.custom;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

/**
 * A reusable smoking device (pipe, bong, vaporizer, …). One enum entry fully describes a device's
 * balance, so the item classes stay strain- and device-agnostic.
 *
 * <p>Durability is measured in <em>hits</em>: a device takes 1 damage per hit and is repaired with
 * the material it's built from. The numbers below keep the original 1:3 pipe:bong fragility ratio
 * and divide into whole bowls (pipe = 4 packs × 2, bong = 6 packs × 4, vaporizer = 16 × 2) so a
 * device never shatters mid-bowl. Tune freely.
 *
 * <p><b>Adding a row here is most of a new device.</b> Everything that iterates devices goes through
 * {@link com.warlonmhite.hempdustry.item.ModItems#devices()} — the creative tab, the model provider
 * and the recipe-viewer pages — so a fourth entry is picked up without any of them being edited.
 */
public enum DeviceType {
    //        registry base  packedModel        maxDmg bowl maxDose duration ench cooldown cough nausea spent  exhale particle
    PIPE     ("wooden_pipe", "packed_pipe",       8,    2,    2,      700,   15,    60,      4,    50,    0,   ParticleTypes.CAMPFIRE_COSY_SMOKE),
    BONG     ("bong",        "packed_bong",      24,    4,    3,     1000,   10,   100,      3,     5,    0,   ParticleTypes.CAMPFIRE_COSY_SMOKE),
    /**
     * The dry-herb vaporizer. Heat below combustion, so it is the mildest device in the mod and the
     * only one that hands the bud back — see {@code .claude/docs/vaporizer.md} for the full design.
     *
     * <p>Three of its numbers are load-bearing rather than tuned:
     * <ul>
     *   <li><b>{@code maxDose} 1</b> is the whole trade-away — it can never reach level II or III,
     *       and because {@link Smoking#greenOutChanceOneIn} can never green you out at dose 1, "a
     *       vaporizer cannot put you on the floor" is true by construction rather than by a special
     *       case beside it.</li>
     *   <li><b>{@code durationTicks} 800</b> against the pipe's 700, at the same bowl size. With
     *       only two hits a bowl, duration is the sole axis left to state the efficiency claim
     *       with — vaporizing really does extract more per gram than burning does.</li>
     *   <li><b>{@code cooldownTicks} 60</b>, level with the pipe and deliberately not lower. The
     *       cooldown is shared across every smokeable, so a shorter one here would be a way to shave
     *       time off a bong rotation.</li>
     * </ul>
     */
    VAPORIZER("vaporizer",   "packed_vaporizer", 32,    2,    1,      800,    5,    60,     50,   200,    1,   ParticleTypes.CLOUD);

    private final String baseName;
    private final String packedModel;
    private final int maxDamage;
    private final int bowlSize;
    private final int maxDose;
    private final int durationTicks;
    private final int enchantability;
    private final int cooldownTicks;
    private final int coughChanceOneIn;
    private final int nauseaChanceOneIn;
    private final int spentYield;
    private final ParticleEffect exhaleParticle;

    DeviceType(String baseName, String packedModel, int maxDamage, int bowlSize, int maxDose,
               int durationTicks, int enchantability, int cooldownTicks, int coughChanceOneIn,
               int nauseaChanceOneIn, int spentYield, ParticleEffect exhaleParticle) {
        this.baseName = baseName;
        this.packedModel = packedModel;
        this.maxDamage = maxDamage;
        this.bowlSize = bowlSize;
        this.maxDose = maxDose;
        this.durationTicks = durationTicks;
        this.enchantability = enchantability;
        this.cooldownTicks = cooldownTicks;
        this.coughChanceOneIn = coughChanceOneIn;
        this.nauseaChanceOneIn = nauseaChanceOneIn;
        this.spentYield = spentYield;
        this.exhaleParticle = exhaleParticle;
    }

    /** Registry id of the empty device; packed variants are {@code baseName + "_" + strainId}. */
    public String baseName() {
        return baseName;
    }

    /**
     * Name of the packed <b>model</b> ({@code item/packed_pipe}) and the stem of its load-mask
     * texture ({@code item/packed_pipe_load}), shared by every packed variant of this device.
     *
     * <p>No longer a texture in its own right: a packed device is now drawn as the empty device on
     * {@code layer0} plus a greyscale mask of the load on {@code layer1}, which the strain's colour
     * tints. That is what lets a strain a datapack invented look like itself — see
     * {@link com.warlonmhite.hempdustry.item.ModItemProperties#LOAD_TINT_INDEX}.
     */
    public String packedModel() {
        return packedModel;
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

    /**
     * Decarboxylated hemp handed back when a <em>bowl</em> is finished, or {@code 0} for a device
     * that leaves nothing usable behind. Only the vaporizer yields any.
     *
     * <p>This is <b>AVB</b> — "already vaped bud". A vaporizer runs at roughly 185–210 °C, under the
     * ~230 °C where plant matter starts to burn, so what comes out is spent but decarboxylated, and
     * saving it for edibles is standard practice precisely because that step is already done. It is
     * therefore the "heat activates" rule arriving through a second door, <b>not</b> an exception to
     * it: nothing raw is ever handed back.
     *
     * <p><b>1, not 2.</b> The Decarboxylator gives four per bud, in bulk, unattended, and takes hemp
     * leaf besides; a 25% return sits inside real AVB's 10–30% residual and keeps the oven the gate
     * the whole edible chain is paid at. Two would be half, and would start reading as an
     * alternative bud → decarb route. Per <em>bowl</em>, and a bowl is one bud, so it cannot be
     * farmed by taking more hits.
     */
    public int spentYield() {
        return spentYield;
    }

    /**
     * The particle for this device's delayed exhale. Everything that burns puffs
     * {@code CAMPFIRE_COSY_SMOKE}; the vaporizer puffs {@code CLOUD}, because <b>vapour is not
     * smoke</b> and this is the cheapest honest signal that the device does not combust.
     */
    public ParticleEffect exhaleParticle() {
        return exhaleParticle;
    }
}
