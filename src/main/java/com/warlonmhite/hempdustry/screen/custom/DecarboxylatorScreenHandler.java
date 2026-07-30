package com.warlonmhite.hempdustry.screen.custom;

import com.mojang.datafixers.util.Pair;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.DecarboxylatorBlockEntity;
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
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Menu for the {@link DecarboxylatorBlockEntity}: fuel on the left, the three trays across the top,
 * and the shared collection slot on the right.
 *
 * <p>The three cook timers and the fire's burn state come across in a {@link PropertyDelegate} so
 * the screen can draw each tray's own progress independently — that per-tray sync is the whole
 * reason this isn't just a furnace menu.
 */
public class DecarboxylatorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;

    /**
     * Greyed-out hints drawn in an empty slot, the same mechanism vanilla uses for the armour and
     * smithing-table slots. They live on the <b>block atlas</b> (hence the {@code item/} path) and
     * the game hides them automatically as soon as the slot holds something.
     */
    private static final Identifier EMPTY_SLOT_FUEL =
            Identifier.of(Hempdustry.MOD_ID, "item/empty_slot_fuel");
    private static final Identifier EMPTY_SLOT_HEMP =
            Identifier.of(Hempdustry.MOD_ID, "item/empty_slot_hemp");

    /**
     * Slot layout, shared with the screen and the GUI-texture generator so the art and the hitboxes
     * can't drift apart.
     *
     * <p>The three trays are centred on the panel (their span runs 61..115, midpoint 88, which is
     * exactly half of the 176-wide background) and the collection slot sits centred directly under
     * them, so the three arrows visibly funnel into it. Fuel is off to the left with its flame
     * above, the way a furnace arranges the same two things.
     */
    public static final int TRAY_X = 62, TRAY_Y = 17, TRAY_SPACING = 18;
    /** Centred under the middle tray — the three trays all feed this one slot. */
    public static final int OUTPUT_X = 80, OUTPUT_Y = 54;
    public static final int FUEL_X = 26, FUEL_Y = 54;

    /** Client-side constructor: the block position arrives via the extended screen handler type. */
    public DecarboxylatorScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, resolveInventory(playerInventory, pos),
                new ArrayPropertyDelegate(DecarboxylatorBlockEntity.PROPERTY_COUNT));
    }

    public DecarboxylatorScreenHandler(int syncId, PlayerInventory playerInventory,
                                       Inventory inventory, PropertyDelegate propertyDelegate) {
        super(ModScreenHandlers.DECARBOXYLATOR, syncId);
        checkSize(inventory, DecarboxylatorBlockEntity.SLOT_COUNT);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        inventory.onOpen(playerInventory.player);

        // Fuel: only things that actually burn. The flame hint tells the player that without a
        // tooltip — coal, a lava bucket, the mod's own hemp stem, anything in the fuel registry.
        this.addSlot(new Slot(inventory, DecarboxylatorBlockEntity.FUEL_SLOT, FUEL_X, FUEL_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return DecarboxylatorBlockEntity.isFuel(stack);
            }

            @Override
            public Pair<Identifier, Identifier> getBackgroundSprite() {
                return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, EMPTY_SLOT_FUEL);
            }
        });

        // The three trays, each with a leaf hint standing for "buds or hemp leaf".
        for (int tray = 0; tray < DecarboxylatorBlockEntity.TRAY_COUNT; tray++) {
            this.addSlot(new Slot(inventory, DecarboxylatorBlockEntity.FIRST_TRAY_SLOT + tray,
                    TRAY_X + tray * TRAY_SPACING, TRAY_Y) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return DecarboxylatorBlockEntity.isTrayInput(stack);
                }

                @Override
                public Pair<Identifier, Identifier> getBackgroundSprite() {
                    return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, EMPTY_SLOT_HEMP);
                }
            });
        }

        // Collection slot: take-only.
        this.addSlot(new Slot(inventory, DecarboxylatorBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return false;
            }
        });

        addPlayerSlots(playerInventory);
        this.addProperties(propertyDelegate);
    }

    private static Inventory resolveInventory(PlayerInventory playerInventory, BlockPos pos) {
        BlockEntity blockEntity = playerInventory.player.getWorld().getBlockEntity(pos);
        return blockEntity instanceof Inventory found
                ? found
                : new SimpleInventory(DecarboxylatorBlockEntity.SLOT_COUNT);
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

    /** Cook progress of one tray as a 0..1 fraction. */
    public float getCookProgress(int tray) {
        int value = this.propertyDelegate.get(DecarboxylatorBlockEntity.PROPERTY_FIRST_PROGRESS + tray);
        return MathHelper.clamp(value / (float) DecarboxylatorBlockEntity.COOK_TIME, 0.0F, 1.0F);
    }

    /** Remaining fuel as a 0..1 fraction of what the current fuel item was worth. */
    public float getBurnProgress() {
        int fuelTime = this.propertyDelegate.get(DecarboxylatorBlockEntity.PROPERTY_FUEL_TIME);
        if (fuelTime == 0) {
            return 0.0F;
        }
        int burnTime = this.propertyDelegate.get(DecarboxylatorBlockEntity.PROPERTY_BURN_TIME);
        return MathHelper.clamp(burnTime / (float) fuelTime, 0.0F, 1.0F);
    }

    public boolean isBurning() {
        return this.propertyDelegate.get(DecarboxylatorBlockEntity.PROPERTY_BURN_TIME) > 0;
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

        int blockSlots = DecarboxylatorBlockEntity.SLOT_COUNT;
        int playerStart = blockSlots;
        int playerEnd = this.slots.size();

        if (slotIndex < blockSlots) {
            // Machine -> player.
            if (!this.insertItem(inSlot, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player -> machine, routed by what the item actually is.
            if (DecarboxylatorBlockEntity.isTrayInput(inSlot)) {
                if (!this.insertItem(inSlot, DecarboxylatorBlockEntity.FIRST_TRAY_SLOT,
                        DecarboxylatorBlockEntity.FIRST_TRAY_SLOT + DecarboxylatorBlockEntity.TRAY_COUNT, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (DecarboxylatorBlockEntity.isFuel(inSlot)) {
                if (!this.insertItem(inSlot, DecarboxylatorBlockEntity.FUEL_SLOT,
                        DecarboxylatorBlockEntity.FUEL_SLOT + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Neither — shuffle between the main inventory and the hotbar, as vanilla does.
                int hotbarStart = playerEnd - 9;
                if (slotIndex < hotbarStart) {
                    if (!this.insertItem(inSlot, hotbarStart, playerEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.insertItem(inSlot, playerStart, hotbarStart, false)) {
                    return ItemStack.EMPTY;
                }
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
