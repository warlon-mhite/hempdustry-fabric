package com.warlonmhite.hempdustry.screen.custom;

import com.warlonmhite.hempdustry.block.entity.custom.HempPressBlockEntity;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * Menu for the {@link HempPressBlockEntity}: one thing in on the left, an arrow, one thing out on
 * the right.
 *
 * <p>A furnace's layout with the fuel column taken out, because there is no fuel — the heat comes
 * from the block underneath, and the flame under the arrow is a report about that rather than a
 * gauge. Same arrangement, and the same reasoning, as the Infuser's.
 */
public class HempPressScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    /** What the press accepts is a recipe lookup, and a lookup needs a world. Held on both sides. */
    private final World world;

    /** Slot layout, shared with the screen and the GUI-texture generator so they cannot drift. */
    public static final int INPUT_X = 56, INPUT_Y = 35;
    public static final int OUTPUT_X = 116, OUTPUT_Y = 35;

    /** Client-side constructor: the block position arrives via the extended screen handler type. */
    public HempPressScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, resolveInventory(playerInventory, pos),
                new ArrayPropertyDelegate(HempPressBlockEntity.PROPERTY_COUNT));
    }

    public HempPressScreenHandler(int syncId, PlayerInventory playerInventory,
                                  Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.HEMP_PRESS, syncId);
        checkSize(inventory, HempPressBlockEntity.SLOT_COUNT);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.world = playerInventory.player.getEntityWorld();
        inventory.onOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, HempPressBlockEntity.INPUT_SLOT, INPUT_X, INPUT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return HempPressBlockEntity.isInput(playerInventory.player.getEntityWorld(), stack);
            }
        });

        this.addSlot(new Slot(inventory, HempPressBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        addPlayerSlots(playerInventory);
        this.addProperties(propertyDelegate);
    }

    private static Inventory resolveInventory(PlayerInventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.getEntityWorld().getBlockEntity(pos);
        return blockEntity instanceof Inventory found
                ? found
                : new SimpleInventory(HempPressBlockEntity.SLOT_COUNT);
    }

    private void addPlayerSlots(PlayerInventory playerInventory) {
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
        return MathHelper.clamp(value / (float) pressTime, 0.0F, 1.0F);
    }

    public boolean isHeated() {
        return this.propertyDelegate.get(HempPressBlockEntity.PROPERTY_HEATED) != 0;
    }

    // ----- shift-click -----

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasStack()) {
            return moved;
        }

        ItemStack inSlot = slot.getStack();
        moved = inSlot.copy();

        int blockSlots = HempPressBlockEntity.SLOT_COUNT;
        int playerStart = blockSlots;
        int playerEnd = this.slots.size();

        if (slotIndex < blockSlots) {
            if (!this.insertItem(inSlot, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (HempPressBlockEntity.isInput(this.world, inSlot)) {
            if (!this.insertItem(inSlot, HempPressBlockEntity.INPUT_SLOT,
                    HempPressBlockEntity.INPUT_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Not something the press takes — shuffle between the main inventory and the hotbar.
            int hotbarStart = playerEnd - 9;
            if (slotIndex < hotbarStart) {
                if (!this.insertItem(inSlot, hotbarStart, playerEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(inSlot, playerStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (inSlot.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        if (inSlot.getCount() == moved.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTakeItem(player, inSlot);
        return moved;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
