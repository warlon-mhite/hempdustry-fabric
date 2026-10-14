package com.warlonmhite.hempdustry.block.custom;

import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.level.block.CarpetBlock;

/**
 * The hemp carpet, as something a llama wears. Vanilla's {@code WoolCarpetBlock} is a carpet that is
 * also {@link Equipable} — body slot, the llama's swag sound — and that, not the carpet tag, is what
 * {@code /item replace} and anything else asking {@code Equipable.get} go by. A dispenser asks
 * neither: {@code ModDispenserBehaviors} gives ours the behaviour vanilla gives its carpets by name.
 * This is that block without the {@code DyeColor}, which ours does not have; the drawing is
 * {@code LlamaDecorFeatureRendererMixin}'s.
 */
public class HempCarpetBlock extends CarpetBlock implements Equipable {
    public HempCarpetBlock(Properties settings) {
        super(settings);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.BODY;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.LLAMA_SWAG;
    }
}
