package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.advancement.ModCriteria;
import com.warlonmhite.hempdustry.api.HempdustryEvents;
import com.warlonmhite.hempdustry.balance.EdibleBundle;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.sound.ModSounds;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
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

    /**
     * Green-out odds for a hit of {@code dose} out of a device, as 1-in-N: the world's odds for the
     * dose ({@link GreenOut}), widened by the device's {@code green_out_factor} — the spliff's 2
     * halves them, because you pace a joint and you don't pace a bong rip. As with a strain's
     * factor, zero or below rules a green-out out.
     */
    public static int greenOutChanceOneIn(GreenOut greenOut, int dose, float deviceFactor) {
        int base = greenOut.oneIn(dose);
        if (base <= 0 || deviceFactor <= 0.0F) {
            return 0;
        }
        return Math.max(1, Math.round(base * deviceFactor));
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
     * @return whether the hit was a full green-out, in which case the caller starts the green-out's
     *         lockout instead of its own cooldown. It has to be the caller: it starts the cooldown
     *         after this returns, and would overwrite one set here
     */
    public static boolean takeHit(World world, PlayerEntity player, ItemStack stack,
                                  SmokeContents contents, int durationTicks, int coughChanceOneIn,
                                  int nauseaChanceOneIn, int greenOutChanceOneIn,
                                  ParticleEffect exhaleParticle) {
        return takeHit(world, player, stack, contents, durationTicks, coughChanceOneIn,
                nauseaChanceOneIn, greenOutChanceOneIn, exhaleParticle, 0);
    }

    /**
     * As above, but the inhale sound (and the exhale puff timed off it) trail the hit by
     * {@code soundDelayTicks}. The bong uses this to let its own bubbling — played by the caller,
     * not here — clear before the inhale sound starts.
     */
    public static boolean takeHit(World world, PlayerEntity player, ItemStack stack,
                                  SmokeContents contents, int durationTicks, int coughChanceOneIn,
                                  int nauseaChanceOneIn, int greenOutChanceOneIn,
                                  ParticleEffect exhaleParticle, int soundDelayTicks) {
        if (soundDelayTicks > 0) {
            SmokeScheduler.scheduleSound(player, soundDelayTicks, ModSounds.SMOKING);
        } else {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.SMOKING, SoundCategory.PLAYERS, 1f, 1f);
        }

        // Every chance and every effect below goes through EffectPolicy, which is where the
        // server's config knobs are applied — once, rather than at each site that hands one out.
        int greenOutOdds = EffectPolicy.greenOutChanceOneIn(smoothed(greenOutChanceOneIn, contents));
        boolean greenedOut = greenOutOdds > 0
                && ThreadLocalRandom.current().nextInt(greenOutOdds) == 0;

        if (greenedOut) {
            greenOut(player, contents.dose());
        } else {
            for (StatusEffectInstance effect : EffectPolicy.filter(contents.effects(durationTicks, false, ThreadLocalRandom.current()), contents.buffBonus())) {
                player.addStatusEffect(effect);
            }
            // What a strain holds back to the exhale -- Beldía's streaming eyes -- rides the same
            // timer as the puff, filtered now so a server's debuffs switch still removes it.
            if (!world.isClient()) {
                List<StatusEffectInstance> exhaled = EffectPolicy.filter(contents.effects(durationTicks, true, ThreadLocalRandom.current()), contents.buffBonus());
                if (!exhaled.isEmpty()) {
                    SmokeScheduler.scheduleEffects(player, exhaled, EXHALE_DELAY_TICKS + soundDelayTicks);
                }
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
            SmokeScheduler.schedule(player, exhaleParticle, EXHALE_DELAY_TICKS + soundDelayTicks);
        }

        int coughOdds = easedByOutfit(player, harshened(coughChanceOneIn, contents));
        if (coughOdds > 0 && ThreadLocalRandom.current().nextInt(coughOdds) == 0) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.COUGHING, SoundCategory.PLAYERS, 1f, 1f);
        }

        // Nausea is its own roll and stays per-device, dose-independent — it is the "harsh smoke"
        // cost, not the "too much" cost. A green-out already brings its own, longer nausea.
        int nauseaOdds = easedByOutfit(player, EffectPolicy.nauseaChanceOneIn(nauseaChanceOneIn));
        if (!greenedOut && nauseaOdds > 0 && ThreadLocalRandom.current().nextInt(nauseaOdds) == 0) {
            apply(player, new StatusEffectInstance(StatusEffects.NAUSEA, NAUSEA_DURATION_TICKS, 0));
        }

        // Last, so a listener sees the hit exactly as the player did — effects on, sound played.
        // The stack is still packed here; a spliff's contents are gone a few lines later, which is
        // why contents is handed over as its own argument.
        HempdustryEvents.AFTER_SMOKE.invoker().afterSmoke(player, contents, stack);
        return greenedOut && contents.dose() >= GreenOut.of(world.getRegistryManager()).fullFromDose();
    }

    /**
     * The load's own green-out odds: the device's 1-in-N, widened by the primary material's
     * {@code green_out_factor}.
     *
     * <p><b>Purity buys smoothness, not power.</b> Filtration is why people filter — less leaf, less
     * chlorophyll, less coughing — so filtered hash carries {@code 2.0} and halves the risk
     * while keeping the exact same effect list. Everything else carries {@code 1.0}, which makes
     * this a no-op, and it is datapack-exposed because that is where a knob like this belongs.
     *
     * <p>Read off {@link SmokeContents#primaryStrain}, so on a hash spliff it is the <em>plant</em>
     * that decides: two buds and a pinch smoke like a joint of that plant, and the smoothness of the
     * pinch does not redeem the paper around it. A pipe or bong packed with the hash alone gets it.
     */
    private static int smoothed(int greenOutChanceOneIn, SmokeContents contents) {
        RegistryEntry<Strain> primary = contents.primaryStrain();
        if (greenOutChanceOneIn <= 0 || primary == null) {
            return greenOutChanceOneIn;
        }
        float factor = primary.value().greenOutFactor();
        // A DATAPACK writes this number and nothing validates it, so zero and negatives have to mean
        // something sane rather than something arithmetic. The field widens the 1-in-N, so "0" reads
        // as infinitely smooth -- and 0 is already how EffectPolicy spells "no green-out at all".
        // Without this branch Math.round(n * 0) is 0, Math.max(1, 0) is 1, and a server owner who
        // typed 0 to turn green-outs OFF would have turned them on for every single hit.
        if (factor <= 0.0F) {
            return 0;
        }
        return Math.max(1, Math.round(greenOutChanceOneIn * factor));
    }

    /**
     * The load's own cough odds: the device's 1-in-N, widened or narrowed by the primary material's
     * {@code cough_factor} exactly as {@link #smoothed} does the green-out. Beldía carries {@code 0.5}
     * and coughs twice as often. A factor of zero or below is read as "never coughs", for the same
     * datapack reason given there.
     */
    private static int harshened(int coughChanceOneIn, SmokeContents contents) {
        RegistryEntry<Strain> primary = contents.primaryStrain();
        if (coughChanceOneIn <= 0 || primary == null) {
            return coughChanceOneIn;
        }
        float factor = primary.value().coughFactor();
        if (factor <= 0.0F) {
            return 0;
        }
        return Math.max(1, Math.round(coughChanceOneIn * factor));
    }

    /**
     * The whole hemp outfit — beanie, shirt, harem pants and flip-flops — doubles a harsh-smoke
     * 1-in-N: half the coughs, half the nausea. <i>The Chill Set</i> made literal. Green-outs are
     * left alone on purpose: the full one is the price of a big hit, and cheap cloth does not buy it
     * off. Zero still means "never", and the doubling saturates rather than overflowing into a
     * negative bound for {@code nextInt}.
     *
     * <p>Silent, as leather's freeze immunity is: no tooltip says so.
     */
    public static int easedByOutfit(PlayerEntity player, int oneIn) {
        if (oneIn <= 0
                || !player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.HEMP_BEANIE)
                || !player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.HEMP_SHIRT)
                || !player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.HEMP_HAREM_PANTS)
                || !player.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.FLIP_FLOPS)) {
            return oneIn;
        }
        return (int) Math.min(Integer.MAX_VALUE, 2L * oneIn);
    }

    /**
     * A green-out at {@code dose}. Below the green-out's {@code full_from_dose} it is the spins:
     * sweaty, wobbly, useless, but brief, and it costs nothing but the buds.
     *
     * <p>From it, the full one, and the high already running goes with it. Measured before this
     * existed: the old green-out laid its debuffs under a Speed III and Haste III that kept running,
     * so stacking three strains at dose three cost fifteen seconds and nothing else. Now every buff
     * the mod hands out ends, the saturation goes (the cold sweat, and the next Hunger lands on the
     * food bar at once), and the caller locks smoking out for a minute. Effects of a type the mod
     * never grants, a potion of Fire Resistance say, are left alone: a green-out is not milk.
     */
    public static void greenOut(PlayerEntity player, int dose) {
        GreenOut greenOut = GreenOut.of(player.getRegistryManager());
        boolean full = dose >= greenOut.fullFromDose();
        if (full) {
            for (RegistryEntry<StatusEffect> buff : buffsTheModGrants(player)) {
                player.removeStatusEffect(buff);
            }
            player.getHungerManager().setSaturationLevel(0f);
        }
        // Through the filter, which also hands back fresh instances: the listed ones belong to the
        // registry, and a status effect ticks down in place once a player wears it.
        for (StatusEffectInstance effect : EffectPolicy.filter(full ? greenOut.full() : greenOut.spins())) {
            player.addStatusEffect(effect);
        }
    }

    /**
     * Every beneficial effect a loaded strain or an edible can grant. Read off the registry, so a
     * datapack strain's buffs are ended by a green-out the day they exist.
     */
    private static Set<RegistryEntry<StatusEffect>> buffsTheModGrants(PlayerEntity player) {
        Set<RegistryEntry<StatusEffect>> out = new HashSet<>();
        for (EdibleBundle.Effect effect : EdibleBundle.of(player.getRegistryManager()).effects()) {
            if (effect.effect().value().getCategory() == StatusEffectCategory.BENEFICIAL) {
                out.add(effect.effect());
            }
        }
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

    /**
     * A small puff at the player's mouth, drifting the way they're facing. What it puffs is the
     * device's own: smoke for anything that burns, {@code CLOUD} for the vaporizer, which does not.
     *
     * <p>{@code ServerWorld.spawnParticles} sends a packet to every player in range rather than to
     * the smoker alone, which is what makes this visible to everyone else on a server.
     */
    static void spawnExhale(ServerWorld world, PlayerEntity player, ParticleEffect particle) {
        Vec3d look = player.getRotationVector();
        double x = player.getX() + look.x * 0.5;
        double y = player.getEyeY() - 0.1 + look.y * 0.5;
        double z = player.getZ() + look.z * 0.5;
        world.spawnParticles(particle, x, y, z, 8, 0.02, 0.02, 0.02, 0.005);
    }
}
