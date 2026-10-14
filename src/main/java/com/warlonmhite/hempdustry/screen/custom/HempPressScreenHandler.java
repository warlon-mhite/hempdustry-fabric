package com.warlonmhite.hempdustry.screen.custom;

import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menu for the {@link HempPressBlockEntity}: one thing in on the left, an arrow, one thing out on
 * the right.
 *
 * <p>A furnace's layout with the fuel column taken out, because there is no fuel — the heat comes
 * from the block underneath, and the flame under the arrow is a report about that rather than a
 * gauge. Same arrangement, and the same reasoning, as the Infuser's.
 */
public class HempPressScreenHandler extends AbstractContainerMenu {
    private final Container inventory;
    private final ContainerData propertyDelegate;
    /** What the press accepts is a recipe lookup, and a lookup needs a world. Held on both sides. */
    private final Level world;

    /** Slot layout, shared with the screen and the GUI-texture generator so they cannot drift. */
    public static final int INPUT_X = 56, INPUT_Y = 35;
    public static final int OUTPUT_X = 116, OUTPUT_Y = 35;

    /** Client-side constructor: the block position arrives via the extended screen handler type. */
    public HempPressScreenHandler(int syncId, Inventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, resolveInventory(playerInventory, pos),
                new SimpleContainerData(HempPressBlockEntity.PROPERTY_COUNT));
    }

    public HempPressScreenHandler(int syncId, Inventory playerInventory,
                                  Container inventory, ContainerData propertyDelegate) {
        super(ModScreenHandlers.HEMP_PRESS, syncId);
        checkContainerSize(inventory, HempPressBlockEntity.SLOT_COUNT);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.world = playerInventory.player.level();
        inventory.startOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, HempPressBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HempPressBlockEntity.isInput(playerInventory.player.level(), stack);
            }
        });

        this.addSlot(new Slot(inventory, HempPressBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerSlots(playerInventory);
        this.addDataSlots(propertyDelegate);
    }

    private static Container resolveInventory(Inventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof Container found
                ? found
                : new SimpleContainer(HempPressBlockEntity.SLOT_COUNT);
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // ----- synced state, for the screen -----

    /** Press progress as a 0..1 fraction, scaled by the press time the <em>server</em> is running. */
    public float getProgress() {
        int value = this.propertyDelegate.get(HempPressBlockEntity.PROPERTY_PROGRESS);
        int pressTime = Math.max(1, this.propertyDelegate.get(HempPressBlockEntity.PROPERTY_PRESS_TIME));
        return Mth.clamp(value / (float) pressTime, 0.0F, 1.0F);
    }

    public boolean isHeated() {
        return this.propertyDelegate.get(HempPressBlockEntity.PROPERTY_HEATED) != 0;
    }

    // ----- shift-click -----

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return moved;
        }

        ItemStack inSlot = slot.getItem();
        moved = inSlot.copy();

        int blockSlots = HempPressBlockEntity.SLOT_COUNT;
        int playerStart = blockSlots;
        int playerEnd = this.slots.size();

        if (slotIndex < blockSlots) {
            if (!this.moveItemStackTo(inSlot, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (HempPressBlockEntity.isInput(this.world, inSlot)) {
            if (!this.moveItemStackTo(inSlot, HempPressBlockEntity.INPUT_SLOT,
                    HempPressBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Not something the press takes — shuffle between the main inventory and the hotbar.
            int hotbarStart = playerEnd - 9;
            if (slotIndex < hotbarStart) {
                if (!this.moveItemStackTo(inSlot, hotbarStart, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(inSlot, playerStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (inSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (inSlot.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, inSlot);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.inventory.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.inventory.stopOpen(player);
    }
}
