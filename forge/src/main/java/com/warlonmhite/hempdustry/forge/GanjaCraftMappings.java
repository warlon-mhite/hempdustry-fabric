package com.warlonmhite.hempdustry.forge;

import com.google.common.collect.ImmutableList;
import com.warlonmhite.hempdustry.ganjacraft.GanjaCraftWorlds;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.Motive;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.loot.GlobalLootModifierSerializer;
import net.minecraftforge.event.RegistryEvent;

/**
 * A world played with GanjaCraft or Hemp-Craft remembers every id that mod registered, in Forge's
 * registry snapshot, and Forge warns of each one this game does not have. Every one is handled
 * ({@code GanjaCraftWorlds}, which converts them as the world is read), so Forge is told to let them go
 * quietly. Only those two namespaces: any other mod's missing ids stay Forge's to report.
 */
final class GanjaCraftMappings {

    /** Every registry GanjaCraft or Hemp-Craft wrote into (all their versions together, as Forge's snapshot names them). */
    private static final List<Class<?>> REGISTRIES = ImmutableList.of(Block.class, Item.class,
            BlockEntityType.class, EntityType.class, SoundEvent.class, ParticleType.class, Feature.class,
            Motive.class, MobEffect.class, Potion.class, RecipeSerializer.class, VillagerProfession.class, PoiType.class,
            GlobalLootModifierSerializer.class);

    private GanjaCraftMappings() {
    }

    static void register() {
        for (Class<?> registry : REGISTRIES) {
            listen(registry);
        }
    }

    // Forge fires one event per registry, typed by the registry's entries; the listener is the same
    // for all of them, so it is added once per type, untyped.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void listen(Class registry) {
        MinecraftForge.EVENT_BUS.addGenericListener(registry, (Consumer<RegistryEvent.MissingMappings>) GanjaCraftMappings::letGo);
    }

    @SuppressWarnings("rawtypes")
    private static void letGo(RegistryEvent.MissingMappings event) {
        for (Object each : event.getAllMappings()) {
            RegistryEvent.MissingMappings.Mapping mapping = (RegistryEvent.MissingMappings.Mapping) each;
            if (GanjaCraftWorlds.NAMESPACES.contains(mapping.key.getNamespace())) {
                mapping.ignore();
            }
        }
    }
}
