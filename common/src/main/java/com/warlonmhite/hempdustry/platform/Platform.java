package com.warlonmhite.hempdustry.platform;

import java.nio.file.Path;

/** Facts about the running game that only the loader knows. */
public interface Platform {

    /** The folder every mod's config file goes in. */
    Path configDir();

    /** This mod's version, as the loader read it from the mod's manifest. */
    String modVersion();
}
