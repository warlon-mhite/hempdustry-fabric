package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
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
        Smoking.registerSmokeable(this);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        Smoking.expire(stack, world);
    }

    @Override
    public Text getName(ItemStack stack) {
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        return contents.isEmpty()
                ? super.getName(stack)
                : SmokeContents.packedName(this.getTranslationKey() + ".packed", contents);
    }

    /**
     * The contents line: <i>With Hashish</i>, italic grey, under the name.
     *
     * <p>A departure from {@code smoking.md}, which recorded that no tooltip was added because "the
     * name already carries strain and level". Still true of a plain spliff; it stops being true the
     * moment a second material is in there and the name can only hold one. <b>This is a contents
     * line, not an effect list</b> — a packed spliff is a container, and vanilla's own dividing line
     * puts contents in the tooltip (a shulker box, a firework's stars) and identity in the name.
     *
     * <p>Deliberately not "Laced with": in cannabis usage "laced" means <em>adulterated with a
     * different drug</em>, which would read as an accusation in the one place the mod is describing
     * something the player did on purpose.
     */
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent,
                              Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
        stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).hashAdditive()
                .ifPresent(hash -> textConsumer.accept(
                        Text.translatable("hempdustry.spliff.with",
                                        Text.translatable(hash.value().translationKey()))
                                .formatted(Formatting.GRAY, Formatting.ITALIC)));
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        if (contents.isEmpty() || player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.PASS;
        }
        if (!world.isClient()) {
            // See SmokingDeviceItem: a vetoed hit must not burn the spliff.
            if (!Smoking.allowed(player, stack, contents)) {
                return ActionResult.PASS;
            }
            Smoking.takeHit(world, player, stack, contents, DURATION_TICKS,
                    COUGH_CHANCE_ONE_IN, NAUSEA_CHANCE_ONE_IN,
                    Smoking.greenOutChanceOneIn(contents.dose(), true),
                    ParticleTypes.CAMPFIRE_COSY_SMOKE);
            // Marks the stack before it shrinks: what is left of it is what the player smoked
            // from, and that is what the cooldown swipe should sit on.
            Smoking.startCooldown(player, stack, EffectPolicy.cooldown(COOLDOWN_TICKS));
            if (!player.getAbilities().creativeMode) {
                stack.decrement(1);
            }
        }
        return ActionResult.SUCCESS;
    }
}
