package com.warlonmhite.hempdustry.screen.custom;

import com.mojang.datafixers.util.Pair;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.screen.ModScreenHandlers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Menu for the {@link InfuserBlockEntity}: milk on the left, the two hemp slots beside it, and the
 * batch preview on the right.
 *
 * <p>The output slot is the unusual one. It shows a <em>preview</em> of the batch rather than a
 * finished item, so taking it is what commits the batch — {@link PreviewSlot#onTakeItem} tells the
 * block entity to spend the ingredients and restart the timer. That is the whole early-pull
 * mechanic, expressed with no extra input verb and therefore no clash with the right-click that
 * opens this screen.
 */
public class InfuserScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    /** Layout, shared with the screen and the GUI-texture generator. */
    public static final int MILK_X = 26, MILK_Y = 35;
    public static final int HEMP_X = 62, HEMP_Y = 17;
    public static final int WASHED_X = 62, WASHED_Y = 53;
    public static final int OUTPUT_X = 134, OUTPUT_Y = 35;

    private static final Identifier EMPTY_SLOT_MILK =
            Identifier.of(Hempdustry.MOD_ID, "item/empty_slot_milk");
    private static final Identifier EMPTY_SLOT_HEMP =
            Identifier.of(Hempdustry.MOD_ID, "item/empty_slot_hemp");

    public InfuserScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, resolveInventory(playerInventory, pos),
                new ArrayPropertyDelegate(InfuserBlockEntity.PROPERTY_COUNT));
    }

    public InfuserScreenHandler(int syncId, PlayerInventory playerInventory,
                                Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.INFUSER, syncId);
        checkSize(inventory, InfuserBlockEntity.SLOT_COUNT);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        inventory.onOpen(playerInventory.player);

        this.addSlot(new HintSlot(inventory, InfuserBlockEntity.MILK_SLOT, MILK_X, MILK_Y, EMPTY_SLOT_MILK));
        this.addSlot(new HintSlot(inventory, InfuserBlockEntity.HEMP_SLOT, HEMP_X, HEMP_Y, EMPTY_SLOT_HEMP));
        this.addSlot(new HintSlot(inventory, InfuserBlockEntity.WASHED_SLOT, WASHED_X, WASHED_Y, EMPTY_SLOT_HEMP));
        this.addSlot(new PreviewSlot(inventory, InfuserBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));

        addPlayerSlots(playerInventory);
        this.addProperties(propertyDelegate);
    }

    private static Inventory resolveInventory(PlayerInventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.getWorld().getBlockEntity(pos);
        return blockEntity instanceof Inventory found
                ? found
                : new SimpleInventory(InfuserBlockEntity.SLOT_COUNT);
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

    /** An input slot that shows a greyed hint while empty, the way vanilla's armour slots do. */
    private static class HintSlot extends Slot {
        private final Identifier hint;

        HintSlot(Inventory inventory, int index, int x, int y, Identifier hint) {
            super(inventory, index, x, y);
            this.hint = hint;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return this.inventory.isValid(this.getIndex(), stack);
        }

        @Override
        public Pair<Identifier, Identifier> getBackgroundSprite() {
            return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, this.hint);
        }
    }

    /** Take-only, and taking it is what actually spends the batch. */
    private static class PreviewSlot extends Slot {
        PreviewSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            if (this.inventory instanceof InfuserBlockEntity infuser) {
                infuser.onPreviewTaken();
            }
            super.onTakeItem(player, stack);
        }
    }

    // ----- synced state, for the screen -----

    /** Overall simmer progress, 0..1 of a full batch. */
    public float getProgress() {
        int progress = this.propertyDelegate.get(InfuserBlockEntity.PROPERTY_PROGRESS);
        return MathHelper.clamp(progress / (float) InfuserBlockEntity.FULL_TIME, 0.0F, 1.0F);
    }

    /**
     * Where along the bar the "you may pull it now, but it will be Rough" mark sits. Constant, but
     * it lives here so the screen doesn't have to know the two timings itself.
     */
    public float getMinimumMark() {
        return InfuserBlockEntity.MIN_TIME / (float) InfuserBlockEntity.FULL_TIME;
    }

    public boolean isHeated() {
        return this.propertyDelegate.get(InfuserBlockEntity.PROPERTY_HEATED) != 0;
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

        int blockSlots = InfuserBlockEntity.SLOT_COUNT;
        int playerStart = blockSlots;
        int playerEnd = this.slots.size();

        if (slotIndex < blockSlots) {
            if (!this.insertItem(inSlot, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (InfuserBlockEntity.isMilk(inSlot)) {
            if (!this.insertItem(inSlot, InfuserBlockEntity.MILK_SLOT, InfuserBlockEntity.MILK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (InfuserBlockEntity.isUnwashedHemp(inSlot)) {
            if (!this.insertItem(inSlot, InfuserBlockEntity.HEMP_SLOT, InfuserBlockEntity.HEMP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (InfuserBlockEntity.isWashedHemp(inSlot)) {
            if (!this.insertItem(inSlot, InfuserBlockEntity.WASHED_SLOT, InfuserBlockEntity.WASHED_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
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
