package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.block.CarpetBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Equipment;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * The hemp carpet, as something a llama wears. Vanilla's {@code DyedCarpetBlock} is a carpet that is
 * also {@link Equipment} — body slot, the llama's swag sound — and that, not the carpet tag, is what
 * {@code /item replace}, a dispenser and anything else asking {@code Equipment.fromStack} go by. This is
 * that block without the {@code DyeColor}, which ours does not have; the drawing is
 * {@code LlamaDecorFeatureRendererMixin}'s.
 */
public class HempCarpetBlock extends CarpetBlock implements Equipment {
    public HempCarpetBlock(Settings settings) {
        super(settings);
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.BODY;
    }

    @Override
    public RegistryEntry<SoundEvent> getEquipSound() {
        return SoundEvents.ENTITY_LLAMA_SWAG;
    }
}
