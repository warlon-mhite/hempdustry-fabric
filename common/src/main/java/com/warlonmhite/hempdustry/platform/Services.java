package com.warlonmhite.hempdustry.platform;

import java.util.ServiceLoader;

/**
 * The mod loader, as the rest of the mod sees it.
 *
 * <p>Everything outside this package and the loaders' own entry points is plain Minecraft, so the
 * same code runs on Fabric and on NeoForge. What a loader does its own way — opening a menu with
 * extra data, a server tick listener, a block entity type with more than vanilla's blocks — is asked
 * of one of the interfaces below, and each loader ships one implementation of each, named in its
 * jar's {@code META-INF/services}. Each is looked up once, the first time any of them is needed.
 */
public final class Services {
    public static final Platform PLATFORM = load(Platform.class);
    public static final Registration REGISTRATION = load(Registration.class);
    public static final Events EVENTS = load(Events.class);
    public static final Menus MENUS = load(Menus.class);
    public static final ItemTransfer TRANSFER = load(ItemTransfer.class);
    public static final PlayerData PLAYER_DATA = load(PlayerData.class);
    public static final AddonEvents ADDON_EVENTS = load(AddonEvents.class);

    private Services() {
    }

    /** The one implementation of {@code type} on the classpath; a missing one fails loudly at load. */
    public static <T> T load(Class<T> type) {
        return ServiceLoader.load(type, Services.class.getClassLoader()).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No implementation of " + type.getName() + " for this loader"));
    }
}
