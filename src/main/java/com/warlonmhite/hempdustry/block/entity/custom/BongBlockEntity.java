package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

/**
 * Empty on purpose. A placed bong has to give back the device it was — durability, enchantments,
 * repair cost, a name from the anvil — and {@link BlockEntity} already does exactly that for any
 * component it does not read itself: {@code readComponents(ItemStack)} keeps them, they are saved
 * with the chunk, and {@code createComponentMap()} hands them to the loot table's
 * {@code copy_components}. So there is no field here and none is needed; a component added to
 * devices later rides through too.
 */
public class BongBlockEntity extends BlockEntity {
    public BongBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BONG, pos, state);
    }
}
