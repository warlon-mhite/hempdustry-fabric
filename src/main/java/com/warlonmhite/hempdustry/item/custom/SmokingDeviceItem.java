package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
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
 *       independent cooldowns and double the smoke rate.</li>
 * </ul>
 */
public class SmokingDeviceItem extends Item {
    private final DeviceType device;

    public SmokingDeviceItem(DeviceType device, Settings settings) {
        super(settings);
        this.device = device;
    }

    public DeviceType device() {
        return device;
    }

    /** What is currently loaded, or {@link SmokeContents#EMPTY} for an unpacked device. */
    public static SmokeContents contentsOf(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SMOKE_CONTENTS, SmokeContents.EMPTY);
    }

    @Override
    public int getEnchantability() {
        return device.enchantability();
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        // Vanilla-style material repair: pipe = its build material (planks), bong = glass. Allowed
        // packed or empty — durability is the same component either way.
        return switch (device) {
            case PIPE -> ingredient.isIn(ItemTags.PLANKS);
            case BONG -> ingredient.isOf(Items.GLASS);
        };
    }

    @Override
    public Text getName(ItemStack stack) {
        SmokeContents contents = contentsOf(stack);
        return contents.isEmpty()
                ? super.getName(stack)
                : SmokeContents.packedName(this.getTranslationKey() + ".packed", contents);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        SmokeContents contents = contentsOf(stack);
        if (contents.isEmpty() || player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (!world.isClient) {
            Smoking.takeHit(world, player, contents, device.durationTicks(),
                    device.coughChanceOneIn(), device.nauseaChanceOneIn(),
                    Smoking.greenOutChanceOneIn(contents.dose(), false));
            player.getItemCooldownManager().set(this, device.cooldownTicks());

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
        return TypedActionResult.success(stack, world.isClient());
    }
}
