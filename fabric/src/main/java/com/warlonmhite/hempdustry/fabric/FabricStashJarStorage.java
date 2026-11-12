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
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A placed Stash Jar to Fabric's transfer API, and so to a hopper (Fabric API moves a hopper's items
 * through this when the block is no container) and to every pipe. In: only what
 * {@code #hempdustry:stash_jar_storable} takes, as many as the jar has room for. Out: newest first, the
 * order an empty hand takes them, so the views list the entries in that order.
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
        for (ItemStackTemplate entry : jar.contents().items()) {
            views.add(new Entry(ItemVariant.of(entry.create()), entry.count()));
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

    /** One entry as it stood when the views were listed; taking from it takes that kind, newest first. */
    private final class Entry implements StorageView<ItemVariant> {
        private final ItemVariant resource;
        private final long amount;

        private Entry(ItemVariant resource, long amount) {
            this.resource = resource;
            this.amount = amount;
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return resource.equals(this.resource) ? FabricStashJarStorage.this.extract(resource, maxAmount, transaction) : 0;
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
            return resource.getItem().getDefaultMaxStackSize();
        }
    }
}
