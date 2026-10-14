package com.warlonmhite.hempdustry.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that keeps the name an anvil gave it, as vanilla's furnace, smoker and brewing stand do:
 * the name comes in from the placed item, shows as the screen's title, is saved with the block, and
 * goes back onto the item when the block is broken (the loot table's {@code copy_components}). This
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

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        customName = parseCustomNameSafe(view, CUSTOM_NAME);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.storeNullable(CUSTOM_NAME, ComponentSerialization.CODEC, customName);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        customName = components.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CUSTOM_NAME, customName);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard(CUSTOM_NAME);
    }
}
