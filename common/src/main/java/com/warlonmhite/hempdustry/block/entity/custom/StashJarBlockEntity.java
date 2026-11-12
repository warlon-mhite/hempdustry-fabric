package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.custom.stash.StashFill;
import com.warlonmhite.hempdustry.block.custom.stash.StashJarBlock;
import com.warlonmhite.hempdustry.block.custom.stash.StashPieces;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.component.ModComponents;
import com.warlonmhite.hempdustry.component.StashContents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A placed Stash Jar's contents. The jar is the item set down, so this holds exactly the item's
 * {@code stash_contents} and hands it back to the loot table's {@code copy_components} when the block
 * breaks; any other component the item carried (a name from the anvil) rides through untouched.
 *
 * <p>Not a {@code Container}: its entries are an ordered list that grows and shrinks, which no fixed
 * slot view can show without its slots shifting under a pipe. Each loader's transfer API reaches it
 * through a storage of its own over {@link #contents()} and {@link #setContentsInTransaction}, and a
 * hopper through that same storage. A container would also spill on removal
 * ({@code BlockEntity#preRemoveSideEffects}), which a jar that drops itself whole must never do.
 *
 * <p>The client gets only what it draws: the stash pieces' colours and how many items are inside, never
 * the stash itself, so a full Diamond jar costs a chunk packet a few numbers. The pieces are tinted from
 * those colours when the chunk section is rebuilt, which happens only when they change; no renderer runs
 * each frame.
 */
public class StashJarBlockEntity extends BlockEntity {
    private static final String CONTENTS = "contents";
    // The client's view, in the update packet only: never saved, never read on the server.
    private static final String BODIES = "piece_bodies";
    private static final String PISTILS = "piece_pistils";
    private static final String COUNT = "count";

    private StashContents contents = StashContents.EMPTY;
    // On the client: what the server last sent. On the server: what it last sent, so it sends only a change.
    private StashPieces.Colours shownPieces = StashPieces.Colours.NONE;
    private int shownCount;

    public StashJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STASH_JAR, pos, state);
    }

    /** Items this jar holds at most: its block's, so a Gilded jar placed holds what the Gilded item holds. */
    public int capacity() {
        return getBlockState().getBlock() instanceof StashJarBlock jar ? jar.capacity() : 0;
    }

    /** What this jar holds. On the server only: the client is never sent the stash ({@link #shownCount}). */
    public StashContents contents() {
        return contents;
    }

    /** How many items this jar holds, on either side: the server's count, or the one it last sent. */
    public int shownCount() {
        return level != null && level.isClientSide() ? shownCount : contents.count();
    }

    /**
     * Puts as much of {@code stack} in as there is room for, shrinking it, and returns how many went
     * in. Anything outside {@code #hempdustry:stash_jar_storable} is refused.
     */
    public int insert(ItemStack stack) {
        StashContents.Mutable working = contents.mutable();
        int moved = working.insert(stack, capacity());
        if (moved > 0) {
            setContents(working.toImmutable());
        }
        return moved;
    }

    /** Takes out the last stack put in, or empty if the jar is. */
    public ItemStack removeLast() {
        StashContents.Mutable working = contents.mutable();
        ItemStack out = working.removeOne();
        if (!out.isEmpty()) {
            setContents(working.toImmutable());
        }
        return out;
    }

    /** Sets the contents and tells the world: the fill level, comparators, the client's copy. */
    public void setContents(StashContents next) {
        contents = next;
        contentsChanged();
    }

    /**
     * Sets the contents inside a transfer transaction, telling no one: the transaction may still roll
     * back, and the loader's journal calls {@link #contentsChanged} once its root commits.
     */
    public void setContentsInTransaction(StashContents next) {
        contents = next;
    }

    /**
     * A pipe's insert, inside its transaction: as much of {@code stack} as fits goes in, the stack is
     * shrunk by it and that many is returned. Refuses what the tag refuses. {@code beforeChange} runs
     * only when something moves (the transaction's snapshot); the world hears of it when the
     * transaction commits ({@link #contentsChanged}).
     */
    public int insertInTransaction(ItemStack stack, Runnable beforeChange) {
        StashContents.Mutable working = contents.mutable();
        int moved = working.insert(stack, capacity());
        if (moved > 0) {
            beforeChange.run();
            contents = working.toImmutable();
        }
        return moved;
    }

    /**
     * A pipe's extract, inside its transaction: up to {@code max} items {@code matches} accepts, newest
     * first. {@code beforeChange} runs only when something moves, so a pipe asking every tick for what
     * the jar does not hold takes no snapshot and wakes no one.
     */
    public int extractInTransaction(Predicate<ItemStack> matches, int max, Runnable beforeChange) {
        StashContents.Mutable working = contents.mutable();
        int taken = working.extract(matches, max);
        if (taken > 0) {
            beforeChange.run();
            contents = working.toImmutable();
        }
        return taken;
    }

    /** After the contents changed: the fill level, the comparator, a save, the client's copy. */
    public void contentsChanged() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(StashFill.PROPERTY)) {
            StashFill fill = StashFill.of(contents.count(), capacity());
            if (state.getValue(StashFill.PROPERTY) != fill) {
                state = state.setValue(StashFill.PROPERTY, fill);
                level.setBlock(worldPosition, state, Block.UPDATE_ALL);
            }
        }
        setChanged();
        // The client draws only the pieces and needs the count only to know an empty jar: anything
        // else changing sends nothing.
        StashPieces.Colours pieces = pieceColours();
        if (!pieces.equals(shownPieces) || (contents.count() == 0) != (shownCount == 0)) {
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * The body and pistil colours of each stash piece the model shows, largest share first: on the
     * server worked out from the contents (and kept on them), on the client the ones last sent.
     */
    public StashPieces.Colours pieceColours() {
        if (level != null && level.isClientSide()) {
            return shownPieces;
        }
        return contents.pieceColours(capacity(), level == null ? null : level.registryAccess());
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        contents = view.read(CONTENTS, StashContents.CODEC).orElse(StashContents.EMPTY).withoutSelection();
        // The client's update: what it draws. The pieces' colours are baked into the chunk section, so
        // it is rebuilt when they change, and only then.
        int[] bodies = view.getIntArray(BODIES).orElse(null);
        int[] pistils = view.getIntArray(PISTILS).orElse(null);
        if (bodies != null && pistils != null && bodies.length == pistils.length) {
            StashPieces.Colours next = new StashPieces.Colours(bodies, pistils);
            boolean changed = !next.equals(shownPieces);
            shownPieces = next;
            shownCount = view.getIntOr(COUNT, 0);
            if (changed && level != null && level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        view.store(CONTENTS, StashContents.CODEC, contents);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        // A scroll choice made in the inventory does not travel into the world: a placed jar gives its newest first.
        contents = components.getOrDefault(ModComponents.STASH_CONTENTS, StashContents.EMPTY).withoutSelection();
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!contents.isEmpty()) {
            components.set(ModComponents.STASH_CONTENTS, contents);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput view) {
        super.removeComponentsFromTag(view);
        view.discard(CONTENTS);
    }

    /** A jar drops itself with its contents (the loot table); nothing spills. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** The pieces' colours and the count, every time (empty too, so a client never keeps a stash that left). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        StashPieces.Colours pieces = pieceColours();
        shownPieces = pieces;
        shownCount = contents.count();
        CompoundTag tag = new CompoundTag();
        tag.putIntArray(BODIES, pieces.bodies());
        tag.putIntArray(PISTILS, pieces.pistils());
        tag.putInt(COUNT, shownCount);
        return tag;
    }
}
