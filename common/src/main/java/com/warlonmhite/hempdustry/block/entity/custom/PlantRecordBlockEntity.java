package com.warlonmhite.hempdustry.block.entity.custom;

import com.warlonmhite.hempdustry.block.custom.GrowLuck;
import com.warlonmhite.hempdustry.block.custom.IndustrialHempCropBlock;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import com.warlonmhite.hempdustry.strain.BudTier;
import com.warlonmhite.hempdustry.strain.GrowQuality;
import com.warlonmhite.hempdustry.strain.Potency;
import com.warlonmhite.hempdustry.strain.PotencyBand;
import com.warlonmhite.hempdustry.strain.QualityGene;
import com.warlonmhite.hempdustry.strain.ResinSheet;
import com.warlonmhite.hempdustry.strain.ResinStep;
import com.warlonmhite.hempdustry.strain.ResinTexture;
import com.warlonmhite.hempdustry.strain.ResistanceGenes;
import com.warlonmhite.hempdustry.strain.Resistances;
import com.warlonmhite.hempdustry.strain.Strain;
import com.mojang.serialization.Codec;
import java.lang.ref.WeakReference;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * What one plant carries from planting to harvest beyond its block state: its potency band, its quality
 * gene, its four resistances, its resin and whether it has grown under open sky. Sits on the plant's root only (the
 * block that carries its age), on every crop that grows buds and on Industrial Hemp.
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
 * The band and the gene are drawn from the strain's odds, each on its own, when the record joins its
 * world ({@link #setLevel}): the moment a seed is planted, a plant another mod placed, or a world
 * generated ripe goes live with its chunk. A record loaded with its band and gene keeps them; one loaded
 * without, from a plant sown before records existed, rolls what it lacks then. A plant set back to age 0
 * in place, by a harvester that replants, is a new plant: its crop {@link #replant replants} the record,
 * which rolls again.
 *
 * <p>Industrial Hemp grows no bud: its record rolls its resistances and its resin, from its own sheets
 * ({@link IndustrialHempCropBlock#RESISTANCES}, {@link IndustrialHempCropBlock#RESIN}, since it is no strain),
 * and no band and no gene; its band reads mid. Nothing in its life reads them yet (it is never stressed, never
 * catches mildew and is never rubbed); they are what it will pass on.
 *
 * <h2>Resistances</h2>
 *
 * Four steps, one per phenomenon ({@link ResistanceGenes}), each rolled on its own from the strain's odds
 * ({@link Resistances}) and hidden. Every cause of stress and of mildew asks them ({@link #resistancesAt});
 * a plant with no record is neutral on all four.
 *
 * <p>The draw is on a random of its own: never the loot table's, so a harvest rolls exactly the
 * numbers it always did (a loot entry taking a variable number of draws shifts every pool after it),
 * and never the world's, since a record can join a chunk on a world-generation thread.
 *
 * <h2>Resin</h2>
 *
 * One step ({@link ResinStep}), rolled like the others from the strain's odds ({@link ResinSheet}). The harvest
 * hands it to the buds ({@code hempdustry:set_resin}) and the rub reads it off the living plant
 * ({@link #resinAt}). A plant with no record rubs and gives buds as before resin.
 *
 * <h2>Open sky</h2>
 *
 * A plant grown in the sun is luckier at its harvest than one grown under glowstone. Whether it was is
 * read like every light ({@link #recordSky}): at each growth step, sky light 15 at its full-grown top
 * ({@code canSeeSky}, which passes glass and stops at leaves and roofs), the record keeping the weaker of
 * before and now. Sky light is the same by night, so the hour plays no part. Losing the sun never
 * stresses a plant. A plant that keeps no light record never steps it, and a wild plant generated ripe
 * never took a step: both read shade.
 *
 * <h2>The harvest's tier</h2>
 *
 * {@link #harvestTier} rolls the {@link BudTier} every bud of one harvest carries, Fortune's extras
 * included: once per harvest, on a random of its own (so the loot table's draws are exactly what they
 * were), from the plant's grow luck ({@link GrowLuck}). Asked again by another stack of the same harvest
 * (the same loot context), it answers the same tier.
 */
public class PlantRecordBlockEntity extends BlockEntity {
    private static final String POTENCY_KEY = "potency";
    private static final String QUALITY_KEY = "quality";
    private static final String OPEN_SKY_KEY = "open_sky";
    private static final String RESISTANCES_KEY = "resistances";
    private static final String RESIN_KEY = "resin";

    private @Nullable PotencyBand band;
    private @Nullable QualityGene gene;
    private @Nullable Boolean openSky;
    private @Nullable ResistanceGenes resistances;
    private @Nullable ResinStep resin;
    private WeakReference<Object> harvest = new WeakReference<>(null);
    private @Nullable BudTier harvestTier;

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
        if (!growsBuds(getBlockState())) {
            return PotencyBand.MID;
        }
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
        if (level.isClientSide()) {
            return;
        }
        boolean buds = growsBuds(getBlockState());
        if (buds && (band == null || gene == null) || resistances == null || resin == null) {
            if (buds && band == null) {
                band = potencyOf(level, getBlockState()).roll(RandomSource.create());
            }
            if (buds && gene == null) {
                gene = qualityOf(level, getBlockState()).roll(RandomSource.create());
            }
            if (resistances == null) {
                resistances = resistancesOf(level, getBlockState()).roll(RandomSource.create());
            }
            if (resin == null) {
                resin = resinOf(level, getBlockState()).roll(RandomSource.create());
            }
            setChanged();
        }
    }

    /** The plant at this root is a new plant: everything is rolled again. */
    public void replant() {
        boolean buds = growsBuds(getBlockState());
        band = buds ? potencyOf(level, getBlockState()).roll(RandomSource.create()) : null;
        gene = buds ? qualityOf(level, getBlockState()).roll(RandomSource.create()) : null;
        resistances = resistancesOf(level, getBlockState()).roll(RandomSource.create());
        resin = resinOf(level, getBlockState()).roll(RandomSource.create());
        openSky = null;
        harvestTier = null;
        harvest = new WeakReference<>(null);
        setChanged();
    }

    /** The gene, if this plant has rolled it yet. */
    public Optional<QualityGene> rolledGene() {
        return Optional.ofNullable(gene);
    }

    /** This plant's quality gene, rolled now from its strain's odds if it has none yet; average on Industrial Hemp. */
    public QualityGene gene() {
        if (!growsBuds(getBlockState())) {
            return QualityGene.AVERAGE;
        }
        if (gene == null) {
            gene = qualityOf(level, getBlockState()).roll(RandomSource.create());
            setChanged();
        }
        return gene;
    }

    /** Sets the gene outright: what a test or a command does. */
    public void setGene(@Nullable QualityGene gene) {
        this.gene = gene;
        setChanged();
    }

    /** The resistances, if this plant has rolled them yet. */
    public Optional<ResistanceGenes> rolledResistances() {
        return Optional.ofNullable(resistances);
    }

    /** This plant's four resistances, rolled now from its strain's odds if it has none yet. */
    public ResistanceGenes resistances() {
        if (resistances == null) {
            resistances = resistancesOf(level, getBlockState()).roll(RandomSource.create());
            setChanged();
        }
        return resistances;
    }

    /** Sets the resistances outright: what a test or a command does. */
    public void setResistances(@Nullable ResistanceGenes resistances) {
        this.resistances = resistances;
        setChanged();
    }

    /** The resin step, if this plant has rolled it yet. */
    public Optional<ResinStep> rolledResin() {
        return Optional.ofNullable(resin);
    }

    /** This plant's resin step, rolled now from its strain's odds if it has none yet. */
    public ResinStep resin() {
        if (resin == null) {
            resin = resinOf(level, getBlockState()).roll(RandomSource.create());
            setChanged();
        }
        return resin;
    }

    /** Sets the resin step outright: what a test or a command does. */
    public void setResin(@Nullable ResinStep resin) {
        this.resin = resin;
        setChanged();
    }

    /** Whether this plant has grown under open sky at every step so far; false before its first step. */
    public boolean openSky() {
        return Boolean.TRUE.equals(openSky);
    }

    /** The open-sky record as kept: empty before the plant's first growth step. */
    public Optional<Boolean> skyRecord() {
        return Optional.ofNullable(openSky);
    }

    /** Sets the open-sky record outright: what a test does. */
    public void setOpenSky(@Nullable Boolean openSky) {
        this.openSky = openSky;
        setChanged();
    }

    /**
     * One growth step of the plant whose root is at {@code root}, taken from {@code fromAge}, standing
     * {@code height} blocks tall once grown: its open-sky record becomes the sky now at its full-grown top
     * on its first step, and the weaker of before and now after. Called from the one step every growth
     * route takes ({@code PlantStress#afterStep}); a root with no record (a plant that keeps none) is left
     * alone.
     */
    public static void recordSky(Level level, BlockPos root, int fromAge, int height) {
        if (level.isClientSide() || !(level.getBlockEntity(root) instanceof PlantRecordBlockEntity record)) {
            return;
        }
        boolean now = level.canSeeSky(root.above(Math.max(0, height - 1)));
        boolean next = fromAge == 0 || record.openSky == null ? now : record.openSky && now;
        if (record.openSky == null || record.openSky != next) {
            record.openSky = next;
            record.setChanged();
        }
    }

    /**
     * The tier of this plant's harvest now: Mids for a stressed or mildewed plant, else one roll from its
     * grow luck with {@code harvester}'s luck in it ({@link GrowLuck}). {@code root} is the plant's root as
     * it stood when it was broken. {@code harvestKey} names the harvest — the loot context rolling it — and
     * every ask with the same key answers the same tier, so every bud of a harvest shares it however many
     * stacks the loot hands out.
     */
    public BudTier harvestTier(Object harvestKey, Level level, BlockState root, @Nullable Entity harvester) {
        if (harvestTier == null || harvest.get() != harvestKey) {
            harvestTier = GrowLuck.givesMids(root)
                    ? BudTier.MIDS
                    : BudTier.roll(GrowLuck.of(this, root, level, harvester), RandomSource.create());
            harvest = new WeakReference<>(harvestKey);
        }
        return harvestTier;
    }

    /** The quality sheet of the strain {@code state}'s block is a plant of, the default with none. */
    public static GrowQuality qualityOf(@Nullable Level level, BlockState state) {
        if (level == null) {
            return GrowQuality.DEFAULT;
        }
        return Strain.ofPlant(state.getBlock(), level.registryAccess())
                .map(Holder::value)
                .map(Strain::quality)
                .orElse(GrowQuality.DEFAULT);
    }

    /**
     * The resistance sheet of the plant {@code state} is: Industrial Hemp's own, else its strain's, neutral with
     * none (a plant no strain names stresses as every plant did before resistances).
     */
    public static Resistances resistancesOf(@Nullable Level level, BlockState state) {
        if (state.getBlock() instanceof IndustrialHempCropBlock) {
            return IndustrialHempCropBlock.RESISTANCES;
        }
        if (level == null) {
            return Resistances.DEFAULT;
        }
        return Strain.ofPlant(state.getBlock(), level.registryAccess())
                .map(Holder::value)
                .map(Strain::resistances)
                .orElse(Resistances.DEFAULT);
    }

    /**
     * The resin sheet of the plant {@code state} is: Industrial Hemp's own, else its strain's, always average with
     * none.
     */
    public static ResinSheet resinOf(@Nullable Level level, BlockState state) {
        if (state.getBlock() instanceof IndustrialHempCropBlock) {
            return IndustrialHempCropBlock.RESIN;
        }
        if (level == null) {
            return ResinSheet.DEFAULT;
        }
        return Strain.ofPlant(state.getBlock(), level.registryAccess())
                .map(Holder::value)
                .map(Strain::resin)
                .orElse(ResinSheet.DEFAULT);
    }

    /** The resin texture of the plant {@code state} is, even with none to ask. */
    public static ResinTexture textureOf(@Nullable Level level, BlockState state) {
        return resinOf(level, state).texture();
    }

    /**
     * The resin step of the plant whose root is at {@code rootPos}; empty where there is no record to ask, or on a
     * client, where none is rolled.
     */
    public static Optional<ResinStep> resinAt(Level level, BlockPos rootPos) {
        return !level.isClientSide() && level.getBlockEntity(rootPos) instanceof PlantRecordBlockEntity record
                ? Optional.of(record.resin())
                : Optional.empty();
    }

    /** Whether the plant {@code state} is grows buds, and so rolls a band and a gene: all but Industrial Hemp. */
    private static boolean growsBuds(BlockState state) {
        return !(state.getBlock() instanceof IndustrialHempCropBlock);
    }

    /**
     * The resistances of the plant whose root is at {@code rootPos}; neutral on all four where there is no
     * record to ask, or on a client, where none is rolled.
     */
    public static ResistanceGenes resistancesAt(Level level, BlockPos rootPos) {
        return !level.isClientSide() && level.getBlockEntity(rootPos) instanceof PlantRecordBlockEntity record
                ? record.resistances()
                : ResistanceGenes.NEUTRAL;
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
        gene = view.read(QUALITY_KEY, QualityGene.CODEC).orElse(null);
        openSky = view.read(OPEN_SKY_KEY, Codec.BOOL).orElse(null);
        resistances = view.read(RESISTANCES_KEY, ResistanceGenes.CODEC).orElse(null);
        resin = view.read(RESIN_KEY, ResinStep.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput view) {
        super.saveAdditional(view);
        if (band != null) {
            view.store(POTENCY_KEY, PotencyBand.CODEC, band);
        }
        if (gene != null) {
            view.store(QUALITY_KEY, QualityGene.CODEC, gene);
        }
        if (openSky != null) {
            view.store(OPEN_SKY_KEY, Codec.BOOL, openSky);
        }
        if (resistances != null) {
            view.store(RESISTANCES_KEY, ResistanceGenes.CODEC, resistances);
        }
        if (resin != null) {
            view.store(RESIN_KEY, ResinStep.CODEC, resin);
        }
    }
}
