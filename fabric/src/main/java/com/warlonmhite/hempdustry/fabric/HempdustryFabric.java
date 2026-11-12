package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.block.entity.ModBlockEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;

/** Fabric's entry point: the mod, as the rest of the code registers it. */
public class HempdustryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Hempdustry.registerDynamicRegistries();
        Hempdustry.init();
        // Every other block entity of the mod is a container, which the transfer API wraps on its own;
        // the Stash Jar is not, so it says what it is.
        ItemStorage.SIDED.registerForBlockEntity((jar, side) -> new FabricStashJarStorage(jar), ModBlockEntities.STASH_JAR);
    }
}
