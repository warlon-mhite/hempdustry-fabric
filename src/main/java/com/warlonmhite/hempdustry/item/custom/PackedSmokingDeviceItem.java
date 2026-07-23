package com.warlonmhite.hempdustry.item.custom;

import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * A pipe/bong packed with a specific {@link Strain} — a distinct item per strain so it reads
 * clearly in the creative tab and tooltips. Right-click to take a hit: it plays the smoke feedback,
 * spends 1 durability (respecting Unbreaking/Mending) and 1 bowl charge. When the bowl runs out it
 * reverts to the empty {@link SmokingDeviceItem}, carrying its remaining durability (and any
 * enchantments) across via a full component copy.
 *
 * <p>These carry no repair recipe — repairing happens on the empty device only.
 */
public class PackedSmokingDeviceItem extends Item {
    private final DeviceType device;
    private final Strain strain;

    public PackedSmokingDeviceItem(DeviceType device, Strain strain, Settings settings) {
        super(settings);
        this.device = device;
        this.strain = strain;
    }

    public DeviceType device() {
        return device;
    }

    public Strain strain() {
        return strain;
    }

    @Override
    public int getEnchantability() {
        return device.enchantability();
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }
        if (!world.isClient) {
            Smoking.takeHit(world, player, strain, device.potency(), device.coughChanceOneIn(), device.nauseaChanceOneIn());
            player.getItemCooldownManager().set(this, device.cooldownTicks());

            if (!player.getAbilities().creativeMode) {
                int remaining = stack.getOrDefault(ModComponents.CHARGES, 0) - 1;
                EquipmentSlot slot = hand == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.damage(1, player, slot);
                if (!stack.isEmpty()) {
                    if (remaining <= 0) {
                        // Bowl spent: revert to the empty device, keeping durability/enchantments.
                        ItemStack empty = stack.copyComponentsToNewStack(ModItems.emptyDevice(device), 1);
                        empty.remove(ModComponents.CHARGES);
                        player.setStackInHand(hand, empty);
                    } else {
                        stack.set(ModComponents.CHARGES, remaining);
                    }
                }
            }
        }
        return TypedActionResult.success(stack, world.isClient());
    }
}
