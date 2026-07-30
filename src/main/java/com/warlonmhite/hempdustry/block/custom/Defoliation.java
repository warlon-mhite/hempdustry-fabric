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
import net.minecraft.util.ItemActionResult;
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
 *   <tr><td>0–3</td><td>too young — a single stalk with no canopy yet</td><td>—</td></tr>
 *   <tr><td>4–5</td><td>early / vegetative trim</td><td>{@link #TRIMMED_EARLY}</td></tr>
 *   <tr><td>6</td><td>late / pre-flower trim</td><td>{@link #TRIMMED_LATE}</td></tr>
 *   <tr><td>7</td><td>mature — harvest it by breaking, not by shearing</td><td>—</td></tr>
 * </table>
 *
 * <p><b>Two booleans rather than one 0–2 counter.</b> The windows are sequential, but a plant sits
 * at one age for many random ticks and a player may well meet a plant that is already past the
 * early window. A counter would either double-count a lingering age or lock out the late cut when
 * the early one was missed; two independent flags have neither problem.
 *
 * <p><b>The flags are canonical on the LOWER segment only</b>, exactly like {@code AGE}. Anything
 * that writes a crop's state has to carry them across — see {@link #carryOver} and its two callers:
 * each crop's {@code setAge}, and the bee mixin.
 */
public final class Defoliation {
    public static final BooleanProperty TRIMMED_EARLY = BooleanProperty.of("trimmed_early");
    public static final BooleanProperty TRIMMED_LATE = BooleanProperty.of("trimmed_late");

    /** First age at which the early/vegetative trim is accepted. */
    public static final int EARLY_MIN_AGE = 4;
    /** Last age at which the early/vegetative trim is accepted. */
    public static final int EARLY_MAX_AGE = 5;
    /** The single age at which the late/pre-flower trim is accepted. */
    public static final int LATE_AGE = 6;

    private Defoliation() {
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
                .with(TRIMMED_LATE, from.get(TRIMMED_LATE));
    }

    /** How many of the two cuts this plant has had — 0, 1 or 2. Drives the harvest payout. */
    public static int cutCount(BlockState lowerState) {
        return (lowerState.get(TRIMMED_EARLY) ? 1 : 0) + (lowerState.get(TRIMMED_LATE) ? 1 : 0);
    }

    /**
     * Handles a right-click on a hemp crop, having already resolved the plant down to its LOWER
     * segment. Returns {@code null} when this isn't a cut at all — not shears, wrong age, or this
     * plant's window has already been spent — leaving the caller to fall through to the default
     * block interaction.
     *
     * <p>Deliberately <b>not</b> Fortune-scaled: Fortune can't be applied to shears in vanilla, so
     * scaling this drop by it would be balancing against an enchantment the player can't get.
     * Fortune still applies to the crop's own harvest, which is broken with a hoe.
     */
    @Nullable
    public static ItemActionResult tryCut(World world, BlockPos lowerPos, BlockState lowerState,
                                          int age, ItemStack stack, PlayerEntity player, Hand hand) {
        // The convention tag rather than Items.SHEARS, so a modded pair of shears works too —
        // Fabric API already puts vanilla shears in it, and other mods join it themselves.
        if (!stack.isIn(ConventionalItemTags.SHEARS_TOOLS)) {
            return null;
        }

        BooleanProperty window;
        if (age >= EARLY_MIN_AGE && age <= EARLY_MAX_AGE) {
            window = TRIMMED_EARLY;
        } else if (age == LATE_AGE) {
            window = TRIMMED_LATE;
        } else {
            return null;
        }
        if (lowerState.get(window)) {
            return null;
        }

        if (!world.isClient) {
            world.setBlockState(lowerPos, lowerState.with(window, true), Block.NOTIFY_LISTENERS);
            Block.dropStack(world, lowerPos, new ItemStack(ModItems.HEMP_LEAF));
            stack.damage(1, player,
                    hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        // Passing the player rather than null means their own client plays this immediately instead
        // of waiting for the server to echo it back — the vanilla pattern for a two-sided use sound.
        world.playSound(player, lowerPos, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundCategory.BLOCKS, 1.0F, 0.8F + world.getRandom().nextFloat() * 0.4F);
        return ItemActionResult.success(world.isClient);
    }
}
