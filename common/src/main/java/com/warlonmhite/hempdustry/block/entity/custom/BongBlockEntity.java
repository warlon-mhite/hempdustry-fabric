package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A placed bong has to give back the device it was — durability, enchantments, repair cost, a name
 * from the anvil — and {@link BlockEntity} already does exactly that for any component it does not
 * read itself: {@code applyComponentsFromItemStack(ItemStack)} keeps them, they are saved with the
 * chunk, and {@code collectComponents()} hands them to the loot table's {@code copy_components}. So
 * there is no field for them here; a component added to devices later rides through too.
 *
 * <p>The one thing kept is a bong placed on Minecraft 1.20.1, which has no components: it kept the
 * device as a whole item stack under {@code Item}, which the data fixer has carried into this
 * version's shape ({@code Schema1460Mixin}). It is taken up here the way placing it would have been.
 * Not at load, because the components a block entity loads with are read after its own data and
 * replace whatever was set; once the block entity is in its level, which every loaded one is before
 * anything asks it for its components.
 */
public class BongBlockEntity extends BlockEntity {
    private static final String OLD_ITEM = "Item";

    private ItemStack oldItem = ItemStack.EMPTY;

    public BongBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BONG, pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        oldItem = tag.contains(OLD_ITEM, Tag.TAG_COMPOUND)
                ? ItemStack.parseOptional(registries, tag.getCompound(OLD_ITEM))
                : ItemStack.EMPTY;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!oldItem.isEmpty()) {
            applyComponentsFromItemStack(oldItem);
            oldItem = ItemStack.EMPTY;
        }
    }
}
