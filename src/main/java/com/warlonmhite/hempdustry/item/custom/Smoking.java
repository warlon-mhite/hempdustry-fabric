package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.advancement.ModCriteria;
import com.warlonmhite.hempdustry.api.HempdustryEvents;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.sound.ModSounds;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Shared "take a hit" feedback so the spliff, pipe and bong don't each re-implement it. Call from
 * the server side only. The strain-specific status effect will hook in here (or alongside it) once
 * the effects system lands.
 */
public final class Smoking {
    private Smoking() {
    }

    /**
     * Every item a hit can be taken from. Populated by the item classes themselves at construction,
     * so a device added later joins it by existing rather than by being remembered here.
     *
     * <p>An identity set: items are singletons, and this is read once per rendered slot.
     */
    private static final Set<Item> SMOKEABLES = Collections.newSetFromMap(new IdentityHashMap<>());

    /** Called from a smokeable item's constructor. See {@link #SMOKEABLES}. */
    public static void registerSmokeable(Item item) {
        SMOKEABLES.add(item);
    }

    public static boolean isSmokeable(Item item) {
        return SMOKEABLES.contains(item);
    }

    /**
     * Starts the post-hit cooldown: {@code ticks} on <b>every</b> smokeable at once, and a mark on
     * the one stack that was used.
     *
     * <p>The cooldown is deliberately global — you just smoked, so you have just smoked, whatever is
     * in the other hand. Keying it per stack instead would hand a player carrying three packed pipes
     * three back-to-back hits, which is the exploit the single-item-per-device design closed in the
     * first place.
     *
     * <p>The mark is what keeps the <em>swipe</em> honest about which stack was used:
     * {@link net.minecraft.entity.player.ItemCooldownManager} is keyed by {@code Item}, so the
     * overlay would otherwise sweep across every pipe in the inventory — including the empty ones,
     * which were never smoked and cannot be. See {@link ModComponents#COOLDOWN_UNTIL} and the
     * client's {@code DrawContextMixin}.
     */
    public static void startCooldown(PlayerEntity player, ItemStack used, int ticks) {
        if (ticks <= 0) {
            // EffectPolicy.cooldown treats 0 as "no cooldown"; a zero-length entry would be a
            // degenerate one (start == end) and a mark with nothing to mark.
            return;
        }
        for (Item item : SMOKEABLES) {
            // Cooldowns are keyed by a group Identifier since 1.21.2, defaulting to the item's own
            // id when it carries no use_cooldown component — which is what every smokeable here is.
            player.getItemCooldownManager().set(Registries.ITEM.getId(item), ticks);
        }
        used.set(ModComponents.COOLDOWN_UNTIL, player.getEntityWorld().getTime() + ticks);
    }

    /**
     * Drops a lapsed cooldown mark. Call from a smokeable's {@code inventoryTick}: without it the
     * component outlives the cooldown for ever, and a stack carrying one will not merge with an
     * otherwise identical stack that does not — which for spliffs means the inventory quietly
     * fragmenting a little more with every joint.
     *
     * <p>Server side only. The client is told about the removal like any other component change, and
     * by then the swipe it gated has finished drawing anyway.
     */
    public static void expire(ItemStack stack, World world) {
        if (!world.isClient() && stack.contains(ModComponents.COOLDOWN_UNTIL)
                && world.getTime() >= stack.getOrDefault(ModComponents.COOLDOWN_UNTIL, 0L)) {
            stack.remove(ModComponents.COOLDOWN_UNTIL);
        }
    }

    /**
     * Whether anything has vetoed this hit. Call before consuming, damaging or cooling anything
     * down: a refused hit must cost the player nothing at all.
     *
     * <p>Lives here rather than at the two call sites so the spliff and the devices cannot drift
     * apart on what a veto means, and so a smokeable added later gets it by using the same door.
     * See {@link HempdustryEvents#ALLOW_SMOKE}.
     */
    public static boolean allowed(PlayerEntity player, ItemStack stack, SmokeContents contents) {
        return HempdustryEvents.ALLOW_SMOKE.invoker().allowSmoke(player, contents, stack);
    }

    /** Ticks after the hit before the smoke puffs, to line up with the exhale in the sound (~1.5s). */
    private static final int EXHALE_DELAY_TICKS = 38;

    /**
     * A brief "greened out" wobble. Vanilla only ramps the nausea distortion up while the effect has
     * ≥60 ticks left (at ~1/150 per tick), then fades over the final 3s — so anything much shorter is
     * imperceptible. 140t ≈ 80t of build (peaks ~0.53 intensity) + the 3s fade: felt, but still short.
     */
    private static final int NAUSEA_DURATION_TICKS = 140; // 7s

    /** How long the spins hold you down. Short on purpose — a setback, not a punishment. */
    private static final int GREEN_OUT_DURATION_TICKS = 300; // 15s
    /** The wobble outlasts the rest of it, so you feel it after you can move again. */
    private static final int GREEN_OUT_NAUSEA_TICKS = 400;   // 20s

    /**
     * From this dose a green-out is the full one rather than the spins. Three is past the default
     * buff cap, so it is the dose a player takes for more than a buff can give them: a longer high.
     */
    public static final int FULL_GREEN_OUT_DOSE = 3;
    private static final int FULL_GREEN_OUT_TICKS = 600;        // 30s
    private static final int FULL_GREEN_OUT_NAUSEA_TICKS = 700; // 35s
    /** How long a full green-out keeps every smokeable cooling down: a minute, before the multiplier. */
    public static final int GREEN_OUT_LOCKOUT_TICKS = 1200;

    /**
     * Odds of greening out, as 1-in-N, indexed by dose. <b>Dose 1 can never green you out</b> — the
     * cheap everyday hit carries no tail risk at all, which is what makes taking a big one a real
     * decision rather than a free upgrade. A spliff halves these (see {@code greenOutChanceOneIn}):
     * you pace a joint, you don't pace a bong rip.
     */
    private static final int[] GREEN_OUT_ONE_IN = {0, 0, 12, 4};

    /** Green-out odds for {@code dose}, doubled (i.e. halved risk) when {@code gentle}. */
    public static int greenOutChanceOneIn(int dose, boolean gentle) {
        int index = Math.min(Math.max(dose, 0), GREEN_OUT_ONE_IN.length - 1);
        int base = GREEN_OUT_ONE_IN[index];
        return base == 0 ? 0 : (gentle ? base * 2 : base);
    }

    /**
     * The full server-side reaction to one hit: applies what is loaded at the dose that was packed,
     * for as long as this device lasts, plays the smoking sound, schedules the exhale puff, and rolls
     * the (separate) cough, nausea and green-out chances. Shared by the spliff, pipe and bong.
     *
     * <p>A green-out <b>replaces</b> the hit's effects rather than stacking on top of them. That is
     * what makes it a real loss and instantly readable — you spent three buds and got none of the
     * good part — instead of a debuff quietly layered under the buffs you were expecting.
     *
     * @return whether the hit was a full green-out, in which case the caller starts
     *         {@link #GREEN_OUT_LOCKOUT_TICKS} instead of its own cooldown. It has to be the
     *         caller: it starts the cooldown after this returns, and would overwrite one set here
     */
    public static boolean takeHit(World world, PlayerEntity player, ItemStack stack,
                                  SmokeContents contents, int durationTicks, int coughChanceOneIn,
                                  int nauseaChanceOneIn, int greenOutChanceOneIn) {
        return takeHit(world, player, stack, contents, durationTicks, coughChanceOneIn,
                nauseaChanceOneIn, greenOutChanceOneIn, 0);
    }

    /**
     * As above, but the inhale sound (and the exhale puff timed off it) trail the hit by
     * {@code soundDelayTicks}. The bong uses this to let its own bubbling — played by the caller,
     * not here — clear before the inhale sound starts.
     */
    public static boolean takeHit(World world, PlayerEntity player, ItemStack stack,
                                  SmokeContents contents, int durationTicks, int coughChanceOneIn,
                                  int nauseaChanceOneIn, int greenOutChanceOneIn, int soundDelayTicks) {
        if (soundDelayTicks > 0) {
            SmokeScheduler.scheduleSound(player, soundDelayTicks, ModSounds.SMOKING);
        } else {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SMOKING, SoundCategory.PLAYERS, 1f, 1f);
        }

        // Every chance and every effect below goes through EffectPolicy, which is where the
        // server's config knobs are applied — once, rather than at each site that hands one out.
        int greenOutOdds = EffectPolicy.greenOutChanceOneIn(greenOutChanceOneIn);
        boolean greenedOut = greenOutOdds > 0
                && ThreadLocalRandom.current().nextInt(greenOutOdds) == 0;

        if (greenedOut) {
            greenOut(player, contents.dose());
        } else {
            for (StatusEffectInstance effect : EffectPolicy.filter(contents.effects(durationTicks))) {
                player.addStatusEffect(effect);
            }
        }

        // Smoke criterion: fires on every hit, from every smokeable. What (if anything) narrows it
        // lives in the advancement's conditions — a time window for "Blaze It!" and "Wake and Bake",
        // an item predicate for "Pipe Dream" and "Bong Voyage" — so all this does is hand over the
        // time of day (tick 0 = 6:00 AM) and the stack, still packed at this point.
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ModCriteria.SMOKE.trigger(serverPlayer, world.getTimeOfDay() % 24000L, stack);
        }

        if (!world.isClient()) {
            SmokeScheduler.schedule(player, EXHALE_DELAY_TICKS + soundDelayTicks);
        }

        if (coughChanceOneIn > 0 && ThreadLocalRandom.current().nextInt(coughChanceOneIn) == 0) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.COUGHING, SoundCategory.PLAYERS, 1f, 1f);
        }

        // Nausea is its own roll and stays per-device, dose-independent — it is the "harsh smoke"
        // cost, not the "too much" cost. A green-out already brings its own, longer nausea.
        int nauseaOdds = EffectPolicy.nauseaChanceOneIn(nauseaChanceOneIn);
        if (!greenedOut && nauseaOdds > 0 && ThreadLocalRandom.current().nextInt(nauseaOdds) == 0) {
            apply(player, new StatusEffectInstance(StatusEffects.NAUSEA, NAUSEA_DURATION_TICKS, 0));
        }

        // Last, so a listener sees the hit exactly as the player did — effects on, sound played.
        // The stack is still packed here; a spliff's contents are gone a few lines later, which is
        // why contents is handed over as its own argument.
        HempdustryEvents.AFTER_SMOKE.invoker().afterSmoke(player, contents, stack);
        return greenedOut && contents.dose() >= FULL_GREEN_OUT_DOSE;
    }

    /**
     * A green-out at {@code dose}. Below {@link #FULL_GREEN_OUT_DOSE} it is the spins: sweaty,
     * wobbly, useless, but brief, and it costs nothing but the buds.
     *
     * <p>From it, the full one, and the high already running goes with it. Measured before this
     * existed: the old green-out laid its debuffs under a Speed III and Haste III that kept running,
     * so stacking three strains at dose three cost fifteen seconds and nothing else. Now every buff
     * the mod hands out ends, the saturation goes (the cold sweat, and the next Hunger lands on the
     * food bar at once), and the caller locks smoking out for a minute. Effects of a type the mod
     * never grants, a potion of Fire Resistance say, are left alone: a green-out is not milk.
     */
    public static void greenOut(PlayerEntity player, int dose) {
        boolean full = dose >= FULL_GREEN_OUT_DOSE;
        if (full) {
            for (RegistryEntry<StatusEffect> buff : buffsTheModGrants(player)) {
                player.removeStatusEffect(buff);
            }
            player.getHungerManager().setSaturationLevel(0f);
        }
        int ticks = full ? FULL_GREEN_OUT_TICKS : GREEN_OUT_DURATION_TICKS;
        int nausea = full ? FULL_GREEN_OUT_NAUSEA_TICKS : GREEN_OUT_NAUSEA_TICKS;
        for (StatusEffectInstance effect : EffectPolicy.filter(List.of(
                new StatusEffectInstance(StatusEffects.NAUSEA, nausea, 0),
                new StatusEffectInstance(StatusEffects.SLOWNESS, ticks, 1),
                new StatusEffectInstance(StatusEffects.WEAKNESS, ticks, 1),
                new StatusEffectInstance(StatusEffects.MINING_FATIGUE, ticks, 1)))) {
            player.addStatusEffect(effect);
        }
    }

    /**
     * Every beneficial effect a loaded strain or an edible can grant. Read off the registry, so a
     * datapack strain's buffs are ended by a green-out the day they exist.
     */
    private static Set<RegistryEntry<StatusEffect>> buffsTheModGrants(PlayerEntity player) {
        Set<RegistryEntry<StatusEffect>> out = new HashSet<>(EdibleEffects.BUFFS);
        for (RegistryEntry<Strain> strain : Strain.registry(player.getRegistryManager()).streamEntries().toList()) {
            for (Strain.SmokeEffect effect : strain.value().smokeEffects()) {
                if (effect.effect().value().getCategory() == StatusEffectCategory.BENEFICIAL) {
                    out.add(effect.effect());
                }
            }
        }
        return out;
    }

    /** One effect, through the same gate. */
    private static void apply(PlayerEntity player, StatusEffectInstance effect) {
        EffectPolicy.filter(List.of(effect)).forEach(player::addStatusEffect);
    }

    /** A small smoke puff at the player's mouth, drifting the way they're facing. */
    static void spawnExhale(ServerWorld world, PlayerEntity player) {
        Vec3d look = player.getRotationVector();
        double x = player.getX() + look.x * 0.5;
        double y = player.getEyeY() - 0.1 + look.y * 0.5;
        double z = player.getZ() + look.z * 0.5;
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 8, 0.02, 0.02, 0.02, 0.005);
    }
}
