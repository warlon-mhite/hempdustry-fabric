package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.balance.DeviceStats;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;

import java.util.Optional;

/**
 * A reusable smoking device (pipe, bong, vaporizer, …). One enum entry fully describes a device as
 * it ships, so the item classes stay strain- and device-agnostic; a datapack can change how it smokes
 * through its {@link DeviceStats} file, but not its durability or its enchantability.
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
    //        registry base  packedModel        maxDmg bowl maxDose duration ench cooldown cough nausea delay draw spent  exhale particle
    PIPE     ("wooden_pipe", "packed_pipe",       8,    2,    2,      700,   15,    60,      4,    50,    0,    0,    0,   ParticleTypes.CAMPFIRE_COSY_SMOKE),
    BONG     ("bong",        "packed_bong",      24,    4,    3,     1000,   10,   100,      3,     5,   10,   30,    0,   ParticleTypes.CAMPFIRE_COSY_SMOKE),
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
    VAPORIZER("vaporizer",   "packed_vaporizer", 32,    2,    1,      800,    5,    60,     50,   200,    0,    0,    1,   ParticleTypes.CLOUD);

    private final String baseName;
    private final String packedModel;
    private final int maxDamage;
    private final int enchantability;
    private final int soundDelayTicks;
    private final int drawTicks;
    private final ParticleEffect exhaleParticle;
    private final DeviceStats builtInStats;

    DeviceType(String baseName, String packedModel, int maxDamage, int bowlSize, int maxDose,
               int durationTicks, int enchantability, int cooldownTicks, int coughChanceOneIn,
               int nauseaChanceOneIn, int soundDelayTicks, int drawTicks, int spentYield,
               ParticleEffect exhaleParticle) {
        this.baseName = baseName;
        this.packedModel = packedModel;
        this.maxDamage = maxDamage;
        this.enchantability = enchantability;
        this.soundDelayTicks = soundDelayTicks;
        this.drawTicks = drawTicks;
        this.exhaleParticle = exhaleParticle;
        this.builtInStats = new DeviceStats(durationTicks, cooldownTicks, coughChanceOneIn, nauseaChanceOneIn,
                1.0F, Optional.of(new DeviceStats.Bowl(bowlSize, maxDose, spentYield)));
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

    public int enchantability() {
        return enchantability;
    }

    /**
     * The numbers this device ships with, which datagen writes out as its file under
     * {@code hempdustry/device/}. <b>The game reads the world's {@link DeviceStats}, never these</b>:
     * a datapack may have changed them, and a call site reading the table instead would quietly
     * ignore it.
     *
     * <ul>
     *   <li><b>Bowl and max dose.</b> The bowl is the hits one packing gives; the max dose is the
     *       most buds it takes, i.e. the highest level the device reaches. That is what gives the
     *       bong its identity honestly — not "stronger", but <em>capable of a bigger hit</em>. A bong
     *       at dose 1 is exactly as strong as a pipe at dose 1.</li>
     *   <li><b>Duration is purely the device and amplifier is purely the dose</b> — see
     *       {@link com.warlonmhite.hempdustry.strain.Strain#effects} for why the two stay orthogonal
     *       rather than trading off the way vanilla's glowstone does.</li>
     *   <li><b>Nausea</b> is per hit and dose-independent, the "harsh smoke" cost: pipe 1-in-50,
     *       bong 1-in-5.</li>
     *   <li><b>Spent yield</b> is the scorched hemp a finished <em>bowl</em> of plant matter hands
     *       back — AVB, "already vaped bud". A vaporizer runs under the temperature where plant
     *       matter burns, so what comes out is spent but decarboxylated: the "heat activates" rule
     *       arriving through a second door, not an exception to it. Only the vaporizer yields any,
     *       one per bowl so it cannot be farmed by taking more hits. Scorched hemp is worth a quarter
     *       of a decarboxylated one in the Infuser, so the depletion lives in the item rather than in
     *       this count, which makes 2 a safe knob if the vaporizer ever needs more reason to be
     *       built.</li>
     * </ul>
     */
    public DeviceStats builtInStats() {
        return builtInStats;
    }

    /** The bowl this device ships with; see {@link #builtInStats()}. */
    public DeviceStats.Bowl builtInBowl() {
        return builtInStats.bowl().orElseThrow();
    }

    /**
     * Ticks to hold the inhale sound (and the exhale puff that lines up with it) back from the hit
     * itself. The bong's own bubbling — {@link com.warlonmhite.hempdustry.sound.ModSounds#BONGHIT} —
     * plays immediately on the hit; the water-clearing take a beat, so the inhale sound needs to
     * trail behind it to stay in sync. The pipe has nothing playing first, so it stays at 0, and so
     * does the vaporizer, which reuses the plain inhale unchanged ({@code vaporizer.md} §6).
     */
    public int soundDelayTicks() {
        return soundDelayTicks;
    }

    /**
     * How long the use key is held for one hit, or {@code 0} for a hit on the click. Only the bong
     * draws: 30 ticks with the device raised to the mouth in vanilla's goat-horn pose
     * ({@code UseAction.TOOT_HORN}), its bubbling playing from the first tick and the hit landing
     * on the last. Let go early and nothing is spent, as with a potion put down half-drunk.
     *
     * <p>A draw is a cost as well as a picture — a player ripping a bong is a player standing
     * still with their hands full — which is the honest price of being the device that reaches
     * level III. The pipe and vaporizer keep the click they always had; a new device declares
     * its own draw here rather than being special-cased. {@link #soundDelayTicks()} is counted
     * from the start of the draw, since that is when the bubbling starts.
     */
    public int drawTicks() {
        return drawTicks;
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
