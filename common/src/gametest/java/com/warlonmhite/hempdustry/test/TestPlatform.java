package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.platform.Services;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.ArrayList;
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

    /** The same for this test mod: its test files and the frozen release baselines. */
    List<Path> testModRoots();

    /**
     * The chance {@code item} raises a composter's level, as the loader's composter reads it -- a
     * table in code on Fabric, a data map on NeoForge -- or {@code -1} when it does not compost.
     */
    float compostChance(ItemLike item);

    /** How readily {@code block} burns, as the loader's fire reads it: zero and zero for a block that does not. */
    Flammability flammability(Block block);

    /** The odds of catching fire and of burning away, vanilla's {@code igniteOdds} and {@code burnOdds}. */
    record Flammability(int igniteOdds, int burnOdds) {
    }

    /**
     * A listener on the loader's own bus for each of the four addon events -- Fabric's
     * {@code HempdustryEvents}, NeoForge's classes in {@code com.warlonmhite.hempdustry.api} -- which
     * writes down what each carried. Registered the first time it is asked for, and kept.
     */
    AddonProbe addonProbe();

    /** What the addon events carried, as another mod's listener would have heard it. */
    final class AddonProbe {
        private final List<Heard> heard = new ArrayList<>();
        private boolean refuse;

        /** While set, the probe refuses every hit, as a listener may. */
        public void refuseHits(boolean refuse) {
            this.refuse = refuse;
        }

        /** Every event heard since the last call, in order; forgets them. */
        public List<Heard> take() {
            List<Heard> taken = List.copyOf(heard);
            heard.clear();
            return taken;
        }

        /** For the loader's listeners: an event, named as Fabric's field is, and its payload in order. */
        public void heard(String event, Object... payload) {
            heard.add(new Heard(event, List.of(payload)));
        }

        /** For the loader's listeners: whether to refuse the hit being asked about. */
        public boolean refusing() {
            return refuse;
        }
    }

    record Heard(String event, List<Object> payload) {
    }
}
