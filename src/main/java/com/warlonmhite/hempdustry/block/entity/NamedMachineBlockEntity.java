package com.warlonmhite.hempdustry.block.entity;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Nameable;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that keeps the name an anvil gave it, as vanilla's furnace, smoker and brewing stand do:
 * the name comes in from the placed item, shows as the screen's title, is saved with the block, and
 * goes back onto the item when the block is broken (the loot table's {@code copy_components}). This
 * is {@code LockableContainerBlockEntity}'s name handling, call for call, for the three machines that
 * are not vanilla containers. {@code CustomName} is absent from every machine saved before it, and
 * absent means unnamed, which is what those machines were.
 */
public abstract class NamedMachineBlockEntity extends BlockEntity implements Nameable {
    private static final String CUSTOM_NAME = "CustomName";

    @Nullable
    private Text customName;

    protected NamedMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The machine's own name, for when nobody has renamed it. */
    protected abstract Text getContainerName();

    @Override
    public Text getName() {
        return customName != null ? customName : getContainerName();
    }

    @Override
    public Text getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public Text getCustomName() {
        return customName;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        customName = nbt.contains(CUSTOM_NAME, NbtElement.STRING_TYPE)
                ? tryParseCustomName(nbt.getString(CUSTOM_NAME), registries) : null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        if (customName != null) {
            nbt.putString(CUSTOM_NAME, Text.Serialization.toJsonString(customName, registries));
        }
    }

    @Override
    protected void readComponents(BlockEntity.ComponentsAccess components) {
        super.readComponents(components);
        customName = components.get(DataComponentTypes.CUSTOM_NAME);
    }

    @Override
    protected void addComponents(ComponentMap.Builder builder) {
        super.addComponents(builder);
        builder.add(DataComponentTypes.CUSTOM_NAME, customName);
    }

    @Override
    public void removeFromCopiedStackNbt(NbtCompound nbt) {
        super.removeFromCopiedStackNbt(nbt);
        nbt.remove(CUSTOM_NAME);
    }
}
