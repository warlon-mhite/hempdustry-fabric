package com.warlonmhite.hempdustry.test;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.warlonmhite.hempdustry.platform.Services;
import com.warlonmhite.hempdustry.registry.DataRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What the game tests ask of the loader they run on, beyond what the mod itself asks of it
 * ({@link Services}). Each loader's test source set ships one implementation, named in its
 * {@code META-INF/services}.
 */
public interface TestPlatform {
    TestPlatform INSTANCE = Services.load(TestPlatform.class);

    /** The loader this test mod was built for, as Modrinth names it -- written down here, not asked of the mod. */
    String loaderName();

    /**
     * Every datapack registry the mod loads, with the codec its files are read with. Minecraft 1.16.5
     * has no datapack registries a mod can add, so the strains and the balance files are the mod's own
     * {@link DataRegistries} on every loader; they are read off its declarations, which it keeps to
     * itself.
     */
    static List<RegistryData<?>> dynamicRegistries() {
        try {
            Field declaredField = DataRegistries.class.getDeclaredField("DECLARED");
            declaredField.setAccessible(true);
            List<RegistryData<?>> out = new ArrayList<>();
            for (Object declared : ((Map<?, ?>) declaredField.get(null)).values()) {
                Field key = declared.getClass().getDeclaredField("key");
                Field codec = declared.getClass().getDeclaredField("codec");
                key.setAccessible(true);
                codec.setAccessible(true);
                out.add(registryData(key.get(declared), codec.get(declared)));
            }
            return out;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("DataRegistries no longer keeps its declarations where the tests read them", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static RegistryData<?> registryData(Object key, Object codec) {
        return new RegistryData((ResourceKey) key, (Codec) codec);
    }

    /** A datapack registry and the codec its files are read with: vanilla's {@code RegistryDataLoader.RegistryData} from 1.19.3 on. */
    @Desugar
    record RegistryData<T>(ResourceKey<? extends Registry<T>> key, Codec<T> elementCodec) {
    }

    /**
     * Leaves on a mock player's connection what the loader's login handshake leaves on a real
     * client's, so the player list takes the player as it takes one who logged in.
     */
    void markAsHandshaken(net.minecraft.network.Connection connection);

    /** The folders, or the jar, the mod's resources are read from. */
    List<Path> modRoots();

    /** The same for this test mod: its classes, its test files and the frozen release baselines. */
    List<Path> testModRoots();

    /**
     * The chance {@code item} raises a composter's level, as the loader's composter reads it -- a
     * table in code on Fabric, a data map on NeoForge -- or {@code -1} when it does not compost.
     */
    float compostChance(ItemLike item);

    /**
     * How many ticks {@code stack} burns in a furnace, as the loader's furnace reads it -- Fabric's fuel
     * registry, NeoForge's data map -- or {@code 0} when it is no fuel.
     */
    int burnTime(ItemStack stack);

    /** How readily {@code block} burns, as the loader's fire reads it: zero and zero for a block that does not. */
    Flammability flammability(Block block);

    /** The odds of catching fire and of burning away, vanilla's {@code igniteOdds} and {@code burnOdds}. */
    @Desugar
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
            List<Heard> taken = ImmutableList.copyOf(heard);
            heard.clear();
            return taken;
        }

        /** For the loader's listeners: an event, named as Fabric's field is, and its payload in order. */
        public void heard(String event, Object... payload) {
            heard.add(new Heard(event, java.util.Arrays.asList(payload.clone())));
        }

        /** For the loader's listeners: whether to refuse the hit being asked about. */
        public boolean refusing() {
            return refuse;
        }
    }

    @Desugar
    record Heard(String event, List<Object> payload) {
    }
}
