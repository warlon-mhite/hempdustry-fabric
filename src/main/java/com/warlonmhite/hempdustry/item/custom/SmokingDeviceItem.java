package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.config.EffectPolicy;
import com.warlonmhite.hempdustry.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * A smoking device — wooden pipe or bong — in <em>either</em> state. Empty and packed are the same
 * item; what is loaded lives in the {@code hempdustry:smoke_contents} component, the way a potion
 * carries {@code potion_contents}. See CLAUDE.md §5b D10.
 *
 * <p>That collapses what used to be one registered item per device × strain, and it makes strain
 * mixing representable at all — a separate item per combination is combinatorially hopeless. Three
 * things fall out of the unification for free:
 *
 * <ul>
 *   <li><b>Durability and enchantments never move.</b> Packing and emptying are a component being
 *       set and cleared on the same stack, so the old {@code copyComponentsToNewStack} dance between
 *       two items — and the class of bug that came with it — is gone.</li>
 *   <li><b>Repair works in either state.</b> Deliberate, and vanilla-consistent: a loaded crossbow
 *       carrying {@code charged_projectiles} is anvil-repairable too.</li>
 *   <li><b>The cooldown is shared across strains</b>, because {@code ItemCooldownManager} is keyed
 *       by {@code Item}. This closes an exploit — carrying one bong per strain used to give
 *       independent cooldowns and double the smoke rate. {@link Smoking#startCooldown} now widens
 *       it further, to every smokeable at once, so a spliff in the other hand is no way round it
 *       either.</li>
 * </ul>
 */
public class SmokingDeviceItem extends Item {
    private final DeviceType device;

    public SmokingDeviceItem(DeviceType device, Settings settings) {
        super(settings);
        this.device = device;
        Smoking.registerSmokeable(this);
    }

    public DeviceType device() {
        return device;
    }

    /** What is currently loaded, or {@link SmokeContents#EMPTY} for an unpacked device. */
    public static SmokeContents contentsOf(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
    }

    // Enchantability and material repair are data components since 1.21.5, not overrides — both
    // are set on the settings in ModItems#registerDevice. Repair stays allowed packed or empty,
    // because durability is the same component either way.

    @Override
    public Text getName(ItemStack stack) {
        SmokeContents contents = contentsOf(stack);
        return contents.isEmpty()
                ? super.getName(stack)
                : SmokeContents.packedName(this.getTranslationKey() + ".packed", contents);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, world, entity, slot);
        Smoking.expire(stack, world);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        SmokeContents contents = contentsOf(stack);
        if (contents.isEmpty() || player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.PASS;
        }
        if (!world.isClient()) {
            // A veto costs the player nothing: no effects, no charge spent, no cooldown. Checked
            // here rather than beside the emptiness test because it is the expensive one of the
            // three and the only one another mod can answer.
            if (!Smoking.allowed(player, stack, contents)) {
                return ActionResult.PASS;
            }
            Smoking.takeHit(world, player, stack, contents, device.durationTicks(),
                    device.coughChanceOneIn(), device.nauseaChanceOneIn(),
                    Smoking.greenOutChanceOneIn(contents.dose(), false), device.soundDelayTicks());
            if (device == DeviceType.BONG) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        ModSounds.BONGHIT, SoundCategory.PLAYERS, 1f, 1f);
            }
            Smoking.startCooldown(player, stack, EffectPolicy.cooldown(device.cooldownTicks()));

            if (!player.getAbilities().creativeMode) {
                int remaining = stack.getOrDefault(ModComponents.CHARGES, 0) - 1;
                EquipmentSlot slot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.damage(1, player, slot);
                if (!stack.isEmpty()) {
                    if (remaining <= 0) {
                        // Bowl spent. Clearing the two components is the whole "revert to empty" —
                        // durability and enchantments are already where they need to be.
                        stack.remove(ModComponents.SMOKE_CONTENTS);
                        stack.remove(ModComponents.CHARGES);
                    } else {
                        stack.set(ModComponents.CHARGES, remaining);
                    }
                }
            }
        }
        return ActionResult.SUCCESS;
    }
}
