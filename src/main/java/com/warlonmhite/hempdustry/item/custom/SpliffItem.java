package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A single-use joint. One item for every strain and every dose — what was rolled into it lives in
 * the {@code hempdustry:smoke_contents} component, same as the devices (CLAUDE.md §5b D10).
 *
 * <h2>Where it sits</h2>
 *
 * The spliff is the on-ramp and the only gear-free option: no device, no durability, no
 * crafting-table tether, and it stacks. It pays for that by being the dearest hit at every level and
 * the only one whose dose costs <b>two</b> resources — {@code N buds + N paper} for level N. That
 * second resource is what stops a level-III spliff undercutting the bong, which gets four hits out of
 * the same three buds.
 *
 * <p>It is also the mildest thing in the mod by a distance: 1-in-500 nausea against the pipe's 1-in-50
 * and the bong's 1-in-5, and {@link Smoking#greenOutChanceOneIn} halves its green-out odds on top —
 * you pace a joint, you don't pace a bong rip. At dose 1 it cannot green you out at all.
 */
public class SpliffItem extends Item {
    private static final int COOLDOWN_TICKS = 80;
    private static final int COUGH_CHANCE_ONE_IN = 6;
    private static final int NAUSEA_CHANCE_ONE_IN = 500; // 0.2%
    /** 45s — between the pipe's 35 and the bong's 50. Duration is the device's axis, not dose's. */
    private static final int DURATION_TICKS = 900;

    public SpliffItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        return contents.isEmpty()
                ? super.getName(stack)
                : SmokeContents.packedName(this.getTranslationKey() + ".packed", contents);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        if (contents.isEmpty() || player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (!world.isClient) {
            Smoking.takeHit(world, player, contents, DURATION_TICKS,
                    COUGH_CHANCE_ONE_IN, NAUSEA_CHANCE_ONE_IN,
                    Smoking.greenOutChanceOneIn(contents.dose(), true));
            player.getItemCooldownManager().set(this, COOLDOWN_TICKS);
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
