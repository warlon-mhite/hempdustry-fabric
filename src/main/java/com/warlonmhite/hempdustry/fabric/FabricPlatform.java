package com.warlonmhite.hempdustry.fabric;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.platform.Platform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class FabricPlatform implements Platform {
    @Override
    public Path configDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Hempdustry.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }
}
