package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.strain.Potency;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.Strain;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * What one plant carries from planting to harvest beyond its block state: its potency band. Sits on
 * the plant's root only (the block that carries its age), and on the crops that grow buds.
 *
 * <h2>Why a block entity, and not more state</h2>
 *
 * A plant's own rolls are too many values to be block-state properties once all of them exist, and a
 * block entity is the one place vanilla's loot reads per-block data from: every break, by a player,
 * an explosion or a harvester, hands the block entity to the loot table, which is how the band
 * reaches the buds. Nothing is drawn: the band never changes how the plant looks.
 *
 * <h2>Rolled at planting</h2>
 *
 * The band is drawn from the strain's odds when the record joins its world ({@link #setLevel}): the
 * moment a seed is planted, a plant another mod placed, or a world generated ripe goes live with its
 * chunk. A record loaded with its band keeps it; one loaded without, from a plant sown before records
 * existed, rolls then. A plant set back to age 0 in place, by a harvester that replants, is a new
 * plant: its crop {@link #replant replants} the record, which rolls again.
 *
 * <p>The draw is on a random of its own: never the loot table's, so a harvest rolls exactly the
 * numbers it always did (a loot entry taking a variable number of draws shifts every pool after it),
 * and never the world's, since a record can join a chunk on a world-generation thread.
 */
public class PlantRecordBlockEntity extends BlockEntity {
    private static final String POTENCY_KEY = "potency";

    private @Nullable PotencyBand band;

    public PlantRecordBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLANT_RECORD, pos, state);
    }

    /** The band, if this plant has rolled it yet. */
    public Optional<PotencyBand> rolledBand() {
        return Optional.ofNullable(band);
    }

    /**
     * This plant's band, rolled now on {@code random} from its strain's odds if it has none yet (a
     * record that never joined a world). A plant no strain names is mid.
     */
    public PotencyBand band() {
        if (band == null) {
            band = potencyOf(level, getBlockState()).roll(RandomSource.create());
            setChanged();
        }
        return band;
    }

    /** Sets the band outright: what a test or a command does. */
    public void setBand(@Nullable PotencyBand band) {
        this.band = band;
        setChanged();
    }

    /**
     * Rolls on joining a server world, unless the save it was loaded from carried a band. A client's
     * copy rolls nothing: the band lives on the server.
     */
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (band == null && !level.isClientSide()) {
            band = potencyOf(level, getBlockState()).roll(RandomSource.create());
            setChanged();
        }
    }

    /** The plant at this root is a new plant: everything is rolled again. */
    public void replant() {
        band = potencyOf(level, getBlockState()).roll(RandomSource.create());
        setChanged();
    }

    /** The potency sheet of the strain {@code state}'s block is a plant of, the default with none. */
    public static Potency potencyOf(@Nullable Level level, BlockState state) {
        if (level == null) {
            return Potency.DEFAULT;
        }
        return Strain.ofPlant(state.getBlock(), level.registryAccess())
                .map(Holder::value)
                .map(Strain::potency)
                .orElse(Potency.DEFAULT);
    }

    /** The band of the plant whose root is at {@code rootPos}; mid where there is no record to ask. */
    public static PotencyBand bandAt(Level level, BlockPos rootPos) {
        return level.getBlockEntity(rootPos) instanceof PlantRecordBlockEntity record
                ? record.band()
                : PotencyBand.MID;
    }

    /** Rolls the record at {@code rootPos} again, if there is one: a new plant stands there. */
    public static void replantAt(Level level, BlockPos rootPos) {
        if (level.getBlockEntity(rootPos) instanceof PlantRecordBlockEntity record) {
            record.replant();
        }
    }

    @Override
    protected void loadAdditional(ValueInput view) {
        super.loadAdditional(view);
        band = view.read(POTENCY_KEY, PotencyBand.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (band != null) {
            view.store(POTENCY_KEY, PotencyBand.CODEC, band);
        }
    }
}
