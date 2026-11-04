package com.warlonmhite.hempdustry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that keeps the name an anvil gave it, as vanilla's furnace, smoker and brewing stand do:
 * the name comes in from the placed item, shows as the screen's title, is saved with the block, and
 * goes back onto the item when the block is broken (the loot table's {@code copy_name}). This
 * is {@code BaseContainerBlockEntity}'s name handling, call for call, for the three machines that
 * are not vanilla containers. {@code CustomName} is absent from every machine saved before it, and
 * absent means unnamed, which is what those machines were.
 */
public abstract class NamedMachineBlockEntity extends BlockEntity implements Nameable {
    private static final String CUSTOM_NAME = "CustomName";

    @Nullable
    private Component customName;

    protected NamedMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The machine's own name, for when nobody has renamed it. */
    protected abstract Component getContainerName();

    @Override
    public Component getName() {
        return customName != null ? customName : getContainerName();
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return customName;
    }

    /** Renames the machine — what an anvil-named item does to it when placed. */
    public void setCustomName(@Nullable Component name) {
        customName = name;
        setChanged();
    }

    /**
     * Hands a placed item's anvil name to the machine it placed, as vanilla's containers do in their
     * block's {@code setPlacedBy} on 1.20.1. Called from each machine block's own.
     */
    public static void nameFromItem(Level world, BlockPos pos, ItemStack stack) {
        if (stack.hasCustomHoverName() && world.getBlockEntity(pos) instanceof NamedMachineBlockEntity machine) {
            machine.setCustomName(stack.getHoverName());
        }
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        customName = nbt.contains(CUSTOM_NAME, Tag.TAG_STRING) ? parseName(nbt.getString(CUSTOM_NAME)) : null;
    }

    @Override
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (customName != null) {
            nbt.putString(CUSTOM_NAME, Component.Serializer.toJson(customName));
        }
    }

    /** A name that does not parse is no name, rather than a machine that will not load. */
    @Nullable
    private static Component parseName(String json) {
        try {
            return Component.Serializer.fromJson(json);
        } catch (Exception e) {
            return null;
        }
    }
}
