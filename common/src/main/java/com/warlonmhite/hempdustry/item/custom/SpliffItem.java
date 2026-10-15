package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.balance.DeviceStats;
import com.warlonmhite.hempdustry.balance.GreenOut;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

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
 * and the bong's 1-in-5, and its {@code green_out_factor} of 2 halves its green-out odds on top —
 * you pace a joint, you don't pace a bong rip. At dose 1 it cannot green you out at all. Its numbers
 * are the {@code spliff} entry of {@link DeviceStats}.
 */
public class SpliffItem extends Item {
    public SpliffItem(Properties settings) {
        super(settings);
        Smoking.registerSmokeable(this);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        Smoking.expire(stack, world);
    }

    @Override
    public Component getName(ItemStack stack) {
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        return contents.isEmpty()
                ? super.getName(stack)
                : SmokeContents.packedName(this.getDescriptionId() + ".packed", contents);
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag type) {
        super.appendHoverText(stack, context, tooltip, type);
        stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY).hashAdditive()
                .ifPresent(hash -> tooltip.add(
                        Component.translatable("hempdustry.spliff.with",
                                        Component.translatable(hash.value().translationKey()))
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SmokeContents contents = stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
        if (contents.isEmpty() || player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }
        if (!world.isClientSide) {
            // See SmokingDeviceItem: a vetoed hit must not burn the spliff.
            if (!Smoking.allowed(player, stack, contents)) {
                return InteractionResultHolder.pass(stack);
            }
            DeviceStats stats = DeviceStats.spliff(world.registryAccess());
            GreenOut greenOut = GreenOut.of(world.registryAccess());
            boolean lockedOut = Smoking.takeHit(world, player, stack, contents, stats.durationTicks(),
                    stats.coughOneIn(), stats.nauseaOneIn(),
                    Smoking.greenOutChanceOneIn(greenOut, contents.dose(), stats.greenOutFactor()),
                    ParticleTypes.CAMPFIRE_COSY_SMOKE);
            // Marks the stack before it shrinks: what is left of it is what the player smoked
            // from, and that is what the cooldown swipe should sit on.
            Smoking.startCooldown(player, stack,
                    EffectPolicy.cooldown(lockedOut ? greenOut.lockoutTicks() : stats.cooldownTicks()));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }
}
