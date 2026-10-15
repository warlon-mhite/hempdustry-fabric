package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.List;

/**
 * What the game tests ask of the loader they run on, beyond what the mod itself asks of it
 * ({@link Services}). Each loader's test source set ships one implementation, named in its
 * {@code META-INF/services}.
 */
public interface TestPlatform {
    TestPlatform INSTANCE = Services.load(TestPlatform.class);

    /**
     * Every datapack registry the game loads. Vanilla's own list leaves out the ones a mod adds, so
     * the strains and the balance files are only in the loader's.
     */
    List<RegistryDataLoader.RegistryData<?>> dynamicRegistries();

    /** The folders, or the jar, the mod's resources are read from. */
    List<Path> modRoots();

    /** The same for this test mod: its classes, its test files and the frozen release baselines. */
    List<Path> testModRoots();

    /** How readily {@code block} burns, as the loader's fire reads it: zero and zero for a block that does not. */
    Flammability flammability(Block block);

    /** The odds of catching fire and of burning away, vanilla's {@code igniteOdds} and {@code burnOdds}. */
    record Flammability(int igniteOdds, int burnOdds) {
    }
}
