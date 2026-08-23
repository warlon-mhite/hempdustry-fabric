package com.warlonmhite.hempdustry.api;

import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.item.custom.SmokeContents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The mod's <b>outbound</b> integration points: how another mod hears about, and can refuse, things
 * that happen in Hempdustry.
 *
 * <h2>Why this exists</h2>
 *
 * Integration has two directions. Inbound is all data and needs no code from anyone — a strain goes
 * in the {@code hempdustry:strain} registry, milk in {@code #hempdustry:milk_buckets}, a forge in
 * {@code #hempdustry:heat_sources}, a machine recipe in {@code hempdustry:decarboxylating} or
 * {@code hempdustry:infusing}.
 *
 * <p>Outbound there was nothing at all, and the only way to hook a hit, a meal or a finished batch
 * was a mixin into {@code Smoking}, {@code EdibleEffects} or {@code InfuserBlockEntity} — which
 * breaks the moment this mod refactors its own internals, and turns into "we cannot change our own
 * code without breaking mod X". These events exist so that never has to happen.
 *
 * <p><b>Consumption has two doors</b>, and anything counting it needs both: {@link #AFTER_SMOKE} for
 * a hit and {@link #AFTER_EAT} for an edible.
 *
 * <h2>What is promised</h2>
 *
 * <b>This package will not break.</b> Signatures here are additive-only from the first stable
 * release: an event may be added, and never removed or re-shaped. The same promise covers the types
 * these signatures name — {@link SmokeContents}, {@link Quality} and
 * {@code com.warlonmhite.hempdustry.strain.Strain} — which is what makes a listener compilable.
 * Everything else in the mod stays internal and may move without warning; see
 * {@code .claude/docs/compat.md}.
 *
 * <h2>Threading and sides</h2>
 *
 * Every event here fires <b>on the server thread only</b>. A listener may read and change world
 * state directly; it must not block.
 *
 * <h2>Compiling against it</h2>
 *
 * <pre>{@code
 * repositories { maven { url = "https://api.modrinth.com/maven" } }
 * dependencies { modImplementation "maven.modrinth:hempdustry:2.0.0+1.21.1" }
 * }</pre>
 */
public final class HempdustryEvents {

    private HempdustryEvents() {
    }

    /**
     * Vetoes a hit before anything happens — no effects, no cooldown, nothing consumed. For a sober
     * zone, a jail plugin, or an accessibility mod suppressing the whole mechanic.
     *
     * <p>Any listener returning {@code false} refuses it; the item simply passes, exactly as it does
     * when it is empty or still cooling down. The check is server-side, so a client may swing its
     * arm before the server declines — the same thing vanilla does with any refused use.
     */
    public static final Event<AllowSmoke> ALLOW_SMOKE =
            EventFactory.createArrayBacked(AllowSmoke.class, listeners -> (player, contents, device) -> {
                for (AllowSmoke listener : listeners) {
                    if (!listener.allowSmoke(player, contents, device)) {
                        return false;
                    }
                }
                return true;
            });

    /**
     * Fires after a hit has landed — effects applied (or a green-out rolled instead), sound played,
     * cooldown not yet started. For an addon's own "stoned" effect, a tolerance or addiction system,
     * or a quest task like "smoke a Lemon Haze spliff" that would otherwise have to scrape
     * advancements.
     *
     * <p>{@code device} is the stack that was smoked, still packed at this point — read
     * {@code contents} from the parameter rather than from the stack, because a spliff's is about to
     * be gone.
     */
    public static final Event<AfterSmoke> AFTER_SMOKE =
            EventFactory.createArrayBacked(AfterSmoke.class, listeners -> (player, contents, device) -> {
                for (AfterSmoke listener : listeners) {
                    listener.afterSmoke(player, contents, device);
                }
            });

    /**
     * Fires when a player eats an edible — a brownie, a dawamesk, a flapjack, cannabutter toast,
     * bhang, or a slice of Space Cake.
     *
     * <p><b>Smoking and eating are two separate doors into the same room, and an addon that watches
     * only {@link #AFTER_SMOKE} sees half the traffic.</b> A tolerance or addiction system built on
     * the hit alone would count every joint, miss every brownie, and read a player as sober while
     * they are four tiers into a cake.
     *
     * <p>It fires at the <b>moment of eating</b>, not at onset. An edible's effects are queued
     * behind a 30 s–3 min come-up and then ramp in stages, so there is no single later instant that
     * means "this happened"; the swallow is the event. A listener wanting to act when the high
     * actually lands should schedule its own delay.
     *
     * <p>{@code tier} is the potency, 1..4 — the same dial the tooltip shows, already resolved from
     * the cannabutter that went in. Nothing fires for a tier-0 edible, which is one nobody dosed.
     */
    public static final Event<EdibleEaten> AFTER_EAT =
            EventFactory.createArrayBacked(EdibleEaten.class, listeners -> (player, tier, quality) -> {
                for (EdibleEaten listener : listeners) {
                    listener.onEaten(player, tier, quality);
                }
            });

    /**
     * Fires when a batch of cannabutter is collected from an Infuser, by any route — a player, a
     * hopper, a pipe or the tub's own spout. The batch is closed out by then, so this is the only
     * moment its {@code strength} and {@code quality} are still knowable.
     */
    public static final Event<CannabutterInfused> AFTER_INFUSE =
            EventFactory.createArrayBacked(CannabutterInfused.class, listeners -> (world, pos, strength, quality) -> {
                for (CannabutterInfused listener : listeners) {
                    listener.onInfused(world, pos, strength, quality);
                }
            });

    @FunctionalInterface
    public interface AllowSmoke {
        /** @return {@code false} to refuse the hit. */
        boolean allowSmoke(PlayerEntity player, SmokeContents contents, ItemStack device);
    }

    @FunctionalInterface
    public interface AfterSmoke {
        void afterSmoke(PlayerEntity player, SmokeContents contents, ItemStack device);
    }

    @FunctionalInterface
    public interface EdibleEaten {
        /**
         * @param tier    potency, 1..4
         * @param quality the grade of the cannabutter it was baked with
         */
        void onEaten(PlayerEntity player, int tier, Quality quality);
    }

    @FunctionalInterface
    public interface CannabutterInfused {
        /**
         * @param strength how much hemp went into the batch, 1..{@code BATCH_CAP}
         * @param quality  the grade it was collected at
         */
        void onInfused(World world, BlockPos pos, int strength, Quality quality);
    }
}
