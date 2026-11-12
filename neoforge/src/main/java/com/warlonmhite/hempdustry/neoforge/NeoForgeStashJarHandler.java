package com.warlonmhite.hempdustry.neoforge;

import com.warlonmhite.hempdustry.block.entity.custom.StashJarBlockEntity;
import com.warlonmhite.hempdustry.component.StashContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.List;

/**
 * A placed Stash Jar to NeoForge's transfer API, and so to a hopper (NeoForge moves a hopper's items
 * through the capability when the block is no container) and to every pipe. In: only what
 * {@code #hempdustry:stash_jar_storable} takes, as many as the jar has room for. Out: newest first, the
 * order an empty hand takes them.
 *
 * <p>The handler's indices are the jar's entries, newest first, plus one empty index past them for a
 * new kind. An insert lands wherever the jar puts it, whatever index it names: the jar keeps its own
 * order, and the API allows a handler to accept into another index than the one asked. The snapshot is
 * the contents, one immutable value; the world hears of a change once the root transaction commits.
 */
final class NeoForgeStashJarHandler extends SnapshotJournal<StashContents> implements ResourceHandler<ItemResource> {
    private final StashJarBlockEntity jar;

    NeoForgeStashJarHandler(StashJarBlockEntity jar) {
        this.jar = jar;
    }

    private List<ItemStackTemplate> entries() {
        return jar.contents().items();
    }

    @Override
    public int size() {
        return entries().size() + 1;
    }

    @Override
    public ItemResource getResource(int index) {
        List<ItemStackTemplate> entries = entries();
        return index >= 0 && index < entries.size() ? ItemResource.of(entries.get(index)) : ItemResource.EMPTY;
    }

    @Override
    public long getAmountAsLong(int index) {
        List<ItemStackTemplate> entries = entries();
        return index >= 0 && index < entries.size() ? entries.get(index).count() : 0;
    }

    @Override
    public long getCapacityAsLong(int index, ItemResource resource) {
        int room = Math.max(0, jar.capacity() - jar.contents().count());
        long amount = getAmountAsLong(index);
        int stack = resource.isEmpty() ? 64 : resource.getMaxStackSize();
        return Math.min(stack, amount + room);
    }

    @Override
    public boolean isValid(int index, ItemResource resource) {
        return resource.isEmpty() || StashContents.storable(resource.toStack());
    }

    @Override
    public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        ItemStack stack = resource.toStack(amount);
        if (!StashContents.storable(stack)) {
            return 0;
        }
        return jar.insertInTransaction(stack, () -> updateSnapshots(transaction));
    }

    @Override
    public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return resource.equals(getResource(index)) ? extract(resource, amount, transaction) : 0;
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        return jar.extractInTransaction(resource::matches, amount, () -> updateSnapshots(transaction));
    }

    @Override
    protected StashContents createSnapshot() {
        return jar.contents();
    }

    @Override
    protected void revertToSnapshot(StashContents snapshot) {
        jar.setContentsInTransaction(snapshot);
    }

    @Override
    protected void onRootCommit(StashContents original) {
        jar.contentsChanged();
    }
}
