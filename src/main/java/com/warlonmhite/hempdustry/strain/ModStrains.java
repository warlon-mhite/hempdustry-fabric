package com.warlonmhite.hempdustry.strain;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.ModBlocks;
import com.warlonmhite.hempdustry.item.ModItems;
import com.warlonmhite.hempdustry.strain.Strain.SmokeEffect;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The strains this mod ships, and the registration of the registry itself.
 *
 * <h2>Two lists, and the difference matters</h2>
 *
 * <ul>
 *   <li>{@link #BUILT_IN} — the strains the mod ships <b>art and recipes</b> for. Code-side, ordered,
 *       and read by datagen: the spliff models and their override indices, the per-strain recipes,
 *       the item tags. A datapack cannot extend it, because a datapack cannot add a texture.</li>
 *   <li><b>The registry</b> — every strain <em>loaded</em>, whatever a datapack has done to it. Read
 *       at runtime by the creative tab, the loot pools, packing and smoking. This is the one that
 *       decides behaviour.</li>
 * </ul>
 *
 * The two are the same at the moment, and they are meant to drift: editing Purple Kush's effects
 * changes the registry and not this file.
 *
 * <h2>Model indices are assigned here, once</h2>
 *
 * {@link #modelIndex} is a strain's position in {@link #BUILT_IN} plus one, and that number is
 * written into the entry the bootstrap builds. It is <b>not</b> a registry ordinal, which would not
 * be stable — see {@link Strain} for what that would break. Both the datagen'd model overrides and
 * the client's item property read this same number, so there is one source for it.
 */
public class ModStrains {

    public static final RegistryKey<Strain> INDICA = key("indica");
    public static final RegistryKey<Strain> SATIVA = key("sativa");

    /** The strains with a full chain — crop, flower, worldgen, loot, art, recipes. Order is stable. */
    public static final List<RegistryKey<Strain>> BUILT_IN = List.of(INDICA, SATIVA);

    /**
     * Registers the registry itself. <b>Synced</b>, so a datapack's strain definitions reach every
     * client without a packet of our own; the tinting and the naming both read the loaded entry.
     *
     * <p>No {@code SKIP_WHEN_EMPTY}: the mod's own strains are always present, and a client without
     * the registry is a client without the mod.
     */
    public static void registerStrains() {
        DynamicRegistries.registerSynced(Strain.REGISTRY_KEY, Strain.CODEC);
        Hempdustry.LOGGER.info("Registering Strains for " + Hempdustry.MOD_ID);
    }

    /** The art index a built-in strain gets. Datagen and the bootstrap both read this. */
    public static int modelIndex(RegistryKey<Strain> key) {
        int index = BUILT_IN.indexOf(key);
        return index < 0 ? 0 : index + 1;
    }

    /** The registry id's path — {@code indica} — which is what texture and recipe names are built on. */
    public static String id(RegistryKey<Strain> key) {
        return key.getValue().getPath();
    }

    public static void bootstrap(Registerable<Strain> context) {
        // Purple Kush — the body high: hard to hurt, hard to get anything done.
        context.register(INDICA, new Strain("hempdustry.strain.indica", 0x8E6FB5, modelIndex(INDICA),
                ModItems.INDICA_SEEDS, ModItems.INDICA_BUDS, ModBlocks.INDICA_FLOWER,
                List.of(
                        new SmokeEffect(StatusEffects.RESISTANCE, 0, true),
                        new SmokeEffect(StatusEffects.HUNGER, 0, false),
                        new SmokeEffect(StatusEffects.MINING_FATIGUE, 0, true))));

        // Lemon Haze — the head high, and a deliberate mirror of Purple Kush: where indica buffs
        // defence and taxes mining, sativa buffs movement and mining and taxes melee damage. Hunger
        // is in both because the munchies don't care which strain you smoked.
        context.register(SATIVA, new Strain("hempdustry.strain.sativa", 0xC7D14A, modelIndex(SATIVA),
                ModItems.SATIVA_SEEDS, ModItems.SATIVA_BUDS, ModBlocks.SATIVA_FLOWER,
                List.of(
                        new SmokeEffect(StatusEffects.SPEED, 0, true),
                        new SmokeEffect(StatusEffects.HASTE, 0, true),
                        new SmokeEffect(StatusEffects.HUNGER, 0, false),
                        new SmokeEffect(StatusEffects.WEAKNESS, 0, true))));
    }

    private static RegistryKey<Strain> key(String name) {
        return RegistryKey.of(Strain.REGISTRY_KEY, Identifier.of(Hempdustry.MOD_ID, name));
    }
}
