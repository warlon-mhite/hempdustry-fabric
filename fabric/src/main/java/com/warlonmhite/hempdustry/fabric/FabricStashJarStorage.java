package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.block.entity.custom.StashJarBlockEntity;
import com.warlonmhite.hempdustry.component.StashContents;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A placed Stash Jar to Fabric's transfer API, and so to a hopper (Fabric API moves a hopper's items
 * through this when the block is no container) and to every pipe. In: only what
 * {@code #hempdustry:stash_jar_storable} takes, as many as the jar has room for, blended into its
 * strain. Out: newest strain first and its best buds first, the order an empty hand takes them, so the
 * views list the stacks each strain would give back in that order, then one blank view for the room
 * left: the views' capacities add up to the jar's, never more.
 *
 * <p>The jar's contents are one immutable value, so the snapshot is that value and a rollback puts it
 * back; the world hears of a change once the outermost transaction commits.
 */
final class FabricStashJarStorage extends SnapshotParticipant<StashContents> implements Storage<ItemVariant> {
    private final StashJarBlockEntity jar;

    FabricStashJarStorage(StashJarBlockEntity jar) {
        this.jar = jar;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        ItemStack stack = resource.toStack((int) Math.min(maxAmount, Integer.MAX_VALUE));
        if (!StashContents.storable(stack)) {
            return 0;
        }
        return jar.insertInTransaction(stack, () -> updateSnapshots(transaction));
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        StoragePreconditions.notBlankNotNegative(resource, maxAmount);
        return jar.extractInTransaction(resource::matches, (int) Math.min(maxAmount, Integer.MAX_VALUE),
                () -> updateSnapshots(transaction));
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        List<StorageView<ItemVariant>> views = new ArrayList<>();
        for (ItemStack part : jar.contents().parts()) {
            views.add(new Entry(ItemVariant.of(part), part.getCount(), part.getCount()));
        }
        int room = jar.capacity() - jar.contents().count();
        if (room > 0) {
            views.add(new Entry(ItemVariant.blank(), 0, room));
        }
        return views.iterator();
    }

    @Override
    protected StashContents createSnapshot() {
        return jar.contents();
    }

    @Override
    protected void readSnapshot(StashContents snapshot) {
        jar.setContentsInTransaction(snapshot);
    }

    @Override
    protected void onFinalCommit() {
        jar.contentsChanged();
    }

    /** One stack as it stood when the views were listed, or the room left; taking from it takes that kind. */
    private final class Entry implements StorageView<ItemVariant> {
        private final ItemVariant resource;
        private final long amount;
        private final long capacity;

        private Entry(ItemVariant resource, long amount, long capacity) {
            this.resource = resource;
            this.amount = amount;
            this.capacity = capacity;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return !resource.isBlank() && resource.equals(this.resource)
                    ? FabricStashJarStorage.this.extract(resource, maxAmount, transaction) : 0;
        }

        @Override
        public boolean isResourceBlank() {
            return resource.isBlank();
        }

        @Override
        public ItemVariant getResource() {
            return resource;
        }

        @Override
        public long getAmount() {
            return amount;
        }

        @Override
        public long getCapacity() {
            return capacity;
        }
    }
}
