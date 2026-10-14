package com.warlonmhite.hempdustry.platform.client;

import com.warlonmhite.hempdustry.platform.Services;

/**
 * The loader, as the client code sees it. Kept apart from {@link Services} so a dedicated server
 * never loads a client class.
 */
public final class ClientServices {
    public static final ClientRegistration CLIENT = Services.load(ClientRegistration.class);

    private ClientServices() {
    }
}
