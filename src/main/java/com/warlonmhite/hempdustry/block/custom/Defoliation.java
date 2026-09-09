package com.warlonmhite.hempdustry.block.custom;

import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Leaf-cutting (defoliation) shared by every hemp crop.
 *
 * <p>A growing plant can be sheared at two points in its life. Each cut hands over a
 * {@code hemp_leaf} on the spot and flips a flag that the crop's loot table reads at harvest,
 * shifting that plant's payout towards buds and away from leaves. Ignoring the mechanic entirely
 * is a valid way to play — the plant still matures, it just finishes leaf-heavy instead of
 * bud-heavy. This mirrors real defoliation, where fan leaves are stripped mid-cycle to open the
 * bud sites to light; cannabis is one of only three known plants with no light-saturation point,
 * so "more light on the flowers" pays off for it more than it would for most crops.
 *
 * <table>
 *   <tr><th>age</th><th>window</th><th>flag</th></tr>
 *   <tr><td>0–2</td><td>too young — a seedling with no canopy to take</td><td>—</td></tr>
 *   <tr><td>3</td><td>late vegetative trim — the last age the plant is one block tall</td><td>{@link #TRIMMED_EARLY}</td></tr>
 *   <tr><td>4–5</td><td>early flowering trim — after the stretch into two blocks</td><td>{@link #TRIMMED_LATE}</td></tr>
 *   <tr><td>6–7</td><td><b>ripe — rub it for resin.</b> A 1-in-{@value #CHARAS_CHANCE_ONE_IN} pinch of charas, once per plant</td><td>{@link #RUBBED}</td></tr>
 * </table>
 *
 * <p><b>The two windows are split on the moment the plant becomes two blocks tall</b>
 * ({@code DOUBLE_BLOCK_AGE} = 4), which is the only cue either crop gives without new models: trim it
 * once while it is still short, once after it has shot up. That is also where real practice puts the
 * two cuts — late veg, just before the flip, and early flower once the stretch is over — and cannabis
 * really does roughly double in height in the first fortnight of flowering, which is exactly what the
 * second block is. Growers stop defoliating well before harvest, so ages 6–7 take no leaf.
 *
 * <p><b>Ages 6–7 are the rub window instead</b> (added 2026-09-09; they were dead before that).
 * Shearing a ripe plant cuts nothing off it — it takes the resin that comes away on the blades,
 * which is only worth doing once the plant is <em>full</em> of it. See
 * {@link #CHARAS_CHANCE_ONE_IN}, which carries the whole argument.
 *
 * <p><b>Windows moved 2026-08-22</b>, from 4–5 / 6. The old split had all of its cuts inside the
 * two-block phase (so neither window had a visible boundary), made the late window half as wide as
 * the early one (so a two-cut harvest was much rarer than the payout table implies), and put the last
 * cut one single growth tick before maturity with no signal that it had lapsed.
 *
 * <p><b>Separate booleans rather than one counter.</b> The windows are sequential, but a plant sits
 * at one age for many random ticks and a player may well meet a plant that is already past the
 * early window. A counter would either double-count a lingering age or lock out the late cut when
 * the early one was missed; independent flags have neither problem — and the rub is not a cut at
 * all, so it could never have shared a counter with the two that are.
 *
 * <p><b>The flags are canonical on the LOWER segment only</b>, exactly like {@code AGE}. Anything
 * that writes a crop's state has to carry them across — see {@link #carryOver} and its two callers:
 * each crop's {@code setAge}, and the bee mixin. <b>All three flags, every time.</b>
 */
public final class Defoliation {
    public static final BooleanProperty TRIMMED_EARLY = BooleanProperty.of("trimmed_early");
    public static final BooleanProperty TRIMMED_LATE = BooleanProperty.of("trimmed_late");

    /** First age at which the early/vegetative trim is accepted. */
    public static final int EARLY_MIN_AGE = 3;
    /** Last age at which the early/vegetative trim is accepted. */
    public static final int EARLY_MAX_AGE = 3;
    /** First age at which the late/flowering trim is accepted. */
    public static final int LATE_MIN_AGE = 4;
    /** Last age at which the late/flowering trim is accepted. */
    public static final int LATE_MAX_AGE = 5;

    /**
     * Whether this plant has already been rubbed for resin. Ripe plants only, once each.
     *
     * <p><b>It must be stamped false in {@link #unworked}</b>, exactly like the two trim flags and
     * for exactly the same reason: {@code BooleanProperty}'s value set is
     * {@code ImmutableSet.of(true, false)}, so a boolean left out of {@code setDefaultState}
     * defaults to <b>true</b>. Left unset, every hemp plant would be born already rubbed and charas
     * would simply never drop — with nothing on screen to say why.
     */
    public static final BooleanProperty RUBBED = BooleanProperty.of("rubbed");

    /** First age at which a plant is ripe enough to rub. */
    public static final int RUB_MIN_AGE = 6;
    /** Last age at which a plant can be rubbed — maturity. */
    public static final int RUB_MAX_AGE = 7;

    /**
     * Odds, as 1-in-N, that a rub yields {@link ModItems#CHARAS}.
     *
     * <p><b>One rub per plant, at ages {@value #RUB_MIN_AGE}–{@value #RUB_MAX_AGE}</b>, so ~4 plants
     * per pinch and around thirty-six for nine — deliberately 20× dearer than hashish. Unlimited
     * rubbing would be a different mechanic entirely: one pair of shears is 238 uses, so a single
     * ripe plant would pay out about thirty charas and the scarcity would be gone.
     *
     * <p><b>Why ripe, and not while trimming</b> (moved 2026-09-09). Resin lives in the trichomes
     * and those are on the flowers, so a plant is only worth rubbing once it is <em>full</em> of it;
     * fan leaves at ages 3–5 are the least resinous material at the least resinous moment. Ages 6–7
     * were also the one dead window in the plant's life — defoliation deliberately stops before
     * harvest — so this fills it rather than crowding anything.
     *
     * <p><b>The plant survives, and that is the mechanic rather than a concession.</b> Real charas
     * is rubbed off <em>living, flowering</em> plants by hand, in the field, day after day; the
     * plant is not harvested. So this is the one material in the mod that comes from looking after a
     * plant instead of killing one — a different verb from everything else, and it deepens
     * defoliation instead of adding a system beside it.
     *
     * <p><b>It is a right-click and not a harvest drop, and that is load-bearing.</b> The scarcity
     * has to be <em>structural</em> rather than tuned: a rub needs a right-click on one specific
     * block state, so there is no hopper for it and there never will be. A drop on breaking a ripe
     * plant would instead be trivially automated by any of the block breakers a kitchen-sink pack
     * ships — and Slow Falling is vanilla's "do not sleep for three nights" effect, so farmable
     * charas would collapse the rarity match the whole signature rests on. The Dry Sifter is only
     * <em>not</em> automated yet; this cannot be, ever.
     *
     * <p>The realism and the mechanic agree exactly here, which is rare — hand-rubbed charas is
     * eight or nine grams for a day's labour <em>precisely because</em> it cannot be mechanised.
     *
     * <p>A side effect worth knowing: a plant boxed in with no headroom is capped below
     * {@code DOUBLE_BLOCK_AGE} and so can be trimmed early but <b>never rubbed</b>. That is correct
     * — it never flowers.
     */
    public static final int CHARAS_CHANCE_ONE_IN = 4;

    private Defoliation() {
    }

    /**
     * Stamps <b>every boolean this class owns</b> to {@code false} — both trim flags and
     * {@link #RUBBED}. Every crop carrying them has to run its default state through this in its
     * constructor, and <b>any boolean added here later must be added to this method in the same
     * commit</b>.
     *
     * <p>Renamed from {@code untrimmed} on 2026-09-09, when {@link #RUBBED} joined: the old name
     * invited the reading that a non-trim flag did not belong here, which is the one mistake that
     * silently re-arms the trap below.
     *
     * <p><b>This is not redundant.</b> A block's default state is {@code states.get(0)} — the first
     * entry of the cartesian product of its properties, which takes each property's <i>first</i>
     * value. {@code BooleanProperty}'s value set is built as {@code ImmutableSet.of(true, false)},
     * so a boolean property left out of {@code setDefaultState} defaults to <b>true</b>, not false.
     * Left unset, every hemp crop would be planted already fully trimmed <em>and</em> already
     * rubbed: both trim windows spent, the two-cut harvest payout handed over for free, and charas
     * never dropping at all. This is why vanilla always spells out {@code .with(WATERLOGGED, false)}
     * and friends rather than relying on the default.
     */
    public static BlockState unworked(BlockState state) {
        return state.with(TRIMMED_EARLY, false).with(TRIMMED_LATE, false).with(RUBBED, false);
    }

    /**
     * Copies the trim flags from {@code from} onto {@code to}, when both states actually carry
     * them. States that don't (vanilla wheat, or one of our own upper segments) come back untouched,
     * which is what makes this safe to call from the bee mixin on any crop in the game.
     *
     * <p>This exists because several things rebuild a crop's state from
     * {@code getDefaultState()} rather than mutating the state in place — most importantly
     * {@link net.minecraft.block.CropBlock#withAge(int)}, which is what a pollinating bee writes
     * through. Without this the flags would silently reset to {@code false} and the player's work
     * would vanish with no feedback.
     */
    public static BlockState carryOver(BlockState from, BlockState to) {
        if (!from.contains(TRIMMED_EARLY) || !to.contains(TRIMMED_EARLY)) {
            return to;
        }
        return to.with(TRIMMED_EARLY, from.get(TRIMMED_EARLY))
                .with(TRIMMED_LATE, from.get(TRIMMED_LATE))
                .with(RUBBED, from.get(RUBBED));
    }

    /** How many of the two cuts this plant has had — 0, 1 or 2. Drives the harvest payout. */
    public static int cutCount(BlockState lowerState) {
        return (lowerState.get(TRIMMED_EARLY) ? 1 : 0) + (lowerState.get(TRIMMED_LATE) ? 1 : 0);
    }

    /**
     * Handles a right-click on a hemp crop, having already resolved the plant down to its LOWER
     * segment. Returns {@code null} when nothing happens — not shears, wrong age, or this plant's
     * window has already been spent — leaving the caller to fall through to the default block
     * interaction.
     *
     * <p><b>Three windows live here, not two</b>, and this is the only place any of them is
     * evaluated. Ages 3 and 4–5 cut leaf; ages 6–7 rub for resin. Keeping the rub in this function
     * rather than beside it is what stops the shears check, the age arithmetic, the spent-window
     * guard, the durability cost and the two-sided sound being written twice — and both crops
     * already call exactly this one entry point.
     *
     * <p>Deliberately <b>not</b> Fortune-scaled: Fortune can't be applied to shears in vanilla, so
     * scaling this drop by it would be balancing against an enchantment the player can't get.
     * Fortune still applies to the crop's own harvest, which is broken with a hoe. The same goes
     * for the {@link #CHARAS_CHANCE_ONE_IN} roll.
     */
    @Nullable
    public static ActionResult tryCut(World world, BlockPos lowerPos, BlockState lowerState,
                                          int age, ItemStack stack, PlayerEntity player, Hand hand) {
        // The convention tag rather than Items.SHEARS, so a modded pair of shears works too.
        // SHEAR_TOOLS is #c:tools/shear, which is the widest of the three: Fabric puts vanilla
        // shears in it AND pulls in the older #c:tools/shears and #c:shears as optional sub-tags,
        // so a mod that joined any of the three matches. Nothing forwards into the older two.
        if (!stack.isIn(ConventionalItemTags.SHEAR_TOOLS)) {
            return null;
        }

        // Three windows, one flag each, and the flag is what closes the window. Which window an age
        // falls in decides both what comes off the plant and whether it is certain.
        BooleanProperty window;
        boolean rub;
        if (age >= EARLY_MIN_AGE && age <= EARLY_MAX_AGE) {
            window = TRIMMED_EARLY;
            rub = false;
        } else if (age >= LATE_MIN_AGE && age <= LATE_MAX_AGE) {
            window = TRIMMED_LATE;
            rub = false;
        } else if (age >= RUB_MIN_AGE && age <= RUB_MAX_AGE) {
            window = RUBBED;
            rub = true;
        } else {
            return null;
        }
        if (lowerState.get(window)) {
            return null;
        }

        if (!world.isClient()) {
            world.setBlockState(lowerPos, lowerState.with(window, true), Block.NOTIFY_LISTENERS);
            if (rub) {
                // A rub takes nothing off the plant but resin -- no leaf, and the age is untouched.
                //
                // THE ROLL AND THE SOUND THAT REPORTS IT ARE BOTH SERVER-SIDE, and they have to be.
                // A rub that yields nothing still costs a durability point and still closes the
                // window, so it must sound different from one that pays out or the player is
                // guessing -- but the outcome is a server roll, and `world.getRandom()` on a client
                // is a DIFFERENT generator. Rolling on both sides to get an immediate sound (which
                // this did at first) means the client picks its own answer and plays the wrong noise
                // about a third of the time, on an integrated server as readily as a dedicated one.
                //
                // So it is emitted from the server with a null "except" player, which costs the
                // actor about a tick of latency and is exactly what SiftingBoxBlock does for the same
                // success-or-not distinction (COMPOSTER_FILL vs COMPOSTER_FILL_SUCCESS), and what
                // vanilla's composter does before it. Honey is the mod's resin sound -- a hashish
                // bar is cut to it too -- so "that noise means resin" is one thing to learn.
                boolean gotCharas = world.getRandom().nextInt(CHARAS_CHANCE_ONE_IN) == 0;
                if (gotCharas) {
                    Block.dropStack(world, lowerPos, new ItemStack(ModItems.CHARAS));
                }
                world.playSound(null, lowerPos,
                        gotCharas ? SoundEvents.BLOCK_HONEY_BLOCK_BREAK
                                : SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES,
                        SoundCategory.BLOCKS, gotCharas ? 0.8F : 1.0F,
                        (gotCharas ? 0.7F : 0.6F) + world.getRandom().nextFloat() * 0.2F);
            } else {
                Block.dropStack(world, lowerPos, new ItemStack(ModItems.HEMP_LEAF));
            }
            stack.damage(1, player,
                    hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }

        // A TRIM's sound is unconditional, so it keeps the two-sided immediate pattern: passing the
        // player means their own client plays it on the frame they clicked instead of waiting for
        // the server to echo it back, and the server's broadcast excludes them. Every listener
        // hears exactly one, and the randomised pitch differing between the two sides is invisible
        // because only one of them ever reaches a given pair of ears.
        if (!rub) {
            world.playSound(player, lowerPos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES,
                    SoundCategory.BLOCKS, 1.0F, 0.8F + world.getRandom().nextFloat() * 0.4F);
        }
        return ActionResult.SUCCESS;
    }
}
