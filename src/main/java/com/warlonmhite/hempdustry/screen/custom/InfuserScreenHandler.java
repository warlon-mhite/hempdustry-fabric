package com.warlonmhite.hempdustry.screen.custom;

import com.mojang.datafixers.util.Pair;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.custom.InfuserBlockEntity;
import com.warlonmhite.hempdustry.item.custom.Quality;
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

    /**
     * Layout, shared with the screen and the GUI-texture generator.
     *
     * <p>Milk, heat indicator and bucket return are stacked in the left column in exactly a furnace's
     * arrangement — what goes in on top, the fire in the middle, what comes back out underneath — so
     * the return slot needs no explaining.
     */
    public static final int MILK_X = 26, MILK_Y = 17;
    public static final int BUCKET_X = 26, BUCKET_Y = 53;
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
        // Both hemp slots take either type; they exist so a batch can mix washed and unwashed, not
        // so each type has a home. Hence the identical hint sprite on both.
        this.addSlot(new HintSlot(inventory, InfuserBlockEntity.FIRST_HEMP_SLOT, HEMP_X, HEMP_Y, EMPTY_SLOT_HEMP));
        this.addSlot(new HintSlot(inventory, InfuserBlockEntity.FIRST_HEMP_SLOT + 1, WASHED_X, WASHED_Y, EMPTY_SLOT_HEMP));
        this.addSlot(new PreviewSlot(inventory, InfuserBlockEntity.OUTPUT_SLOT, OUTPUT_X, OUTPUT_Y));
        // Take-only, and it shows the same bucket hint as the milk slot: what lands here is exactly
        // what you put in above, minus the milk.
        this.addSlot(new TakeOnlySlot(inventory, InfuserBlockEntity.BUCKET_SLOT, BUCKET_X, BUCKET_Y,
                EMPTY_SLOT_MILK));

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

    /** A {@link HintSlot} the player may only take from — the bucket return. */
    private static class TakeOnlySlot extends HintSlot {
        TakeOnlySlot(Inventory inventory, int index, int x, int y, Identifier hint) {
            super(inventory, index, x, y, hint);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }

    /**
     * Take-only, and taking it is what closes the batch out.
     *
     * <p>The real guard lives on {@link InfuserBlockEntity#removeStack}, which catches every route
     * out of the slot including hoppers; this hook stays because it also covers the shift-click path,
     * where {@code quickMove} empties the slot with {@code setStack} rather than {@code removeStack}.
     * Closing a batch out twice is harmless — it just clears already-cleared bookkeeping.
     */
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

    /**
     * The tick at which <em>this</em> batch is finished — when it reaches the best grade its washed
     * ratio allows, which is what the spout waits for. Everything drawn on the bar is scaled against
     * this rather than against {@link InfuserBlockEntity#FULL_TIME}.
     *
     * <p><b>The bar measures the job, not the clock.</b> Only an all-washed batch actually runs the
     * full timer, because Perfect is the one grade gated on it; a half-washed batch is done at 67% of
     * it. Scaling to the clock meant the bar simply never filled for most batches, which threw away
     * the most learnable thing a progress bar has to offer — <b>full means done</b>.
     *
     * <p>Falls back to the full clock when there is no batch yet to measure.
     */
    private int finishProgress() {
        int washed = this.propertyDelegate.get(InfuserBlockEntity.PROPERTY_WASHED_PERCENT);
        if (washed < 0) {
            return InfuserBlockEntity.FULL_TIME;
        }
        int needed = Quality.timeNeededFor(Quality.of(100, washed), washed);
        if (needed < 0) {
            return InfuserBlockEntity.FULL_TIME;
        }
        int span = InfuserBlockEntity.FULL_TIME - InfuserBlockEntity.MIN_TIME;
        return InfuserBlockEntity.MIN_TIME + needed * span / 100;
    }

    /** How far through this batch's job the simmer is, 0..1. Reaches 1 exactly when it is done. */
    public float getProgress() {
        int progress = this.propertyDelegate.get(InfuserBlockEntity.PROPERTY_PROGRESS);
        return MathHelper.clamp(progress / (float) finishProgress(), 0.0F, 1.0F);
    }

    /**
     * Where along the bar the "you may pull it now" mark sits.
     *
     * <p><b>It moves</b>, because the bar is scaled to the job: the minimum is a third of an
     * all-washed batch's run but half of a half-washed one's. That is fine — the player never needs
     * to memorise where it sits, only to see whether the fill has passed it, which is a comparison
     * and not a memory. And its position now says something real: how much of this particular job is
     * the mandatory part.
     */
    public float getMinimumMark() {
        return MathHelper.clamp(InfuserBlockEntity.MIN_TIME / (float) finishProgress(), 0.0F, 1.0F);
    }

    /**
     * Where along the bar this batch would earn its <b>next grade up</b>, as a fraction of a full
     * simmer, or {@code -1} if there is no batch or no further grade within reach.
     *
     * <p>This mark has to exist. Under the old all-or-nothing grading the answer to "when does it get
     * better?" was always "the full timer", so there was nothing to show; with score-based grading it
     * moves with the washed ratio — a spotless batch earns Clean at 73% of the cook while a
     * three-quarters-washed one waits until 87% — and a player has no way to work that out from the
     * inside. The washed share is synced for exactly this.
     */
    public float getNextGradeMark() {
        int washed = this.propertyDelegate.get(InfuserBlockEntity.PROPERTY_WASHED_PERCENT);
        if (washed < 0) {
            return -1.0F;
        }
        Quality next = Quality.of(timePercent(), washed).next();
        while (next != null) {
            int needed = Quality.timeNeededFor(next, washed);
            if (needed >= 0) {
                int span = InfuserBlockEntity.FULL_TIME - InfuserBlockEntity.MIN_TIME;
                // Scaled against the job, like everything else on the bar — so the *last* upgrade's
                // mark lands exactly on the bar's end, and the fill arriving there is the batch
                // finishing. Intermediate upgrades still fall part-way along.
                return MathHelper.clamp(
                        (InfuserBlockEntity.MIN_TIME + needed / 100.0F * span) / finishProgress(),
                        0.0F, 1.0F);
            }
            next = next.next();
        }
        return -1.0F;
    }

    private int timePercent() {
        int progress = this.propertyDelegate.get(InfuserBlockEntity.PROPERTY_PROGRESS);
        int span = InfuserBlockEntity.FULL_TIME - InfuserBlockEntity.MIN_TIME;
        return MathHelper.clamp((progress - InfuserBlockEntity.MIN_TIME) * 100 / span, 0, 100);
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
        } else if (InfuserBlockEntity.isHemp(inSlot)) {
            // Either type into either hemp slot — insertItem walks the range and takes the first
            // that will have it, which is what makes shift-clicking a mixed batch in work at all.
            int firstHemp = InfuserBlockEntity.FIRST_HEMP_SLOT;
            if (!this.insertItem(inSlot, firstHemp, firstHemp + InfuserBlockEntity.HEMP_SLOT_COUNT, false)) {
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
