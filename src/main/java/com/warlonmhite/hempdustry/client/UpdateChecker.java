package com.warlonmhite.hempdustry.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.warlonmhite.hempdustry.Hempdustry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the mod's Modrinth project for a newer release than the one currently running.
 * Client-only: a dedicated server has no player to notify and shouldn't phone home.
 * Update the {@code game_versions} filter below when porting to a new Minecraft version.
 */
public final class UpdateChecker {
    private static final String MODRINTH_PROJECT = "hempdustry";
    private static final String MODRINTH_PROJECT_URL = "https://modrinth.com/project/" + MODRINTH_PROJECT;
    private static final String MODRINTH_VERSIONS_URL =
            "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT
                    + "/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%221.21.1%22%5D";

    private static volatile String availableVersion;
    private static final AtomicBoolean NOTIFIED = new AtomicBoolean(false);

    private UpdateChecker() {
    }

    public static void init() {
        CompletableFuture.runAsync(UpdateChecker::fetchLatest);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String latest = availableVersion;
            if (latest != null && client.player != null && NOTIFIED.compareAndSet(false, true)) {
                client.player.sendMessage(Text.literal("[Hempdustry] A new version is available: " + latest
                        + " (you have " + currentVersion() + "). Get it at " + MODRINTH_PROJECT_URL), false);
            }
        });
    }

    private static void fetchLatest() {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MODRINTH_VERSIONS_URL))
                    .header("User-Agent", "warlon-mhite/hempdustry-fabric/" + currentVersion() + " (update checker)")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Hempdustry.LOGGER.debug("Update check skipped: Modrinth returned HTTP {}", response.statusCode());
                return;
            }

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            if (versions.isEmpty()) {
                return;
            }

            String latest = versions.get(0).getAsJsonObject().get("version_number").getAsString();
            if (isNewer(latest, currentVersion())) {
                availableVersion = latest;
                Hempdustry.LOGGER.info("A new Hempdustry version is available: {} (you have {}). Get it at {}",
                        latest, currentVersion(), MODRINTH_PROJECT_URL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Hempdustry.LOGGER.debug("Update check failed", e);
        }
    }

    private static boolean isNewer(String remote, String local) {
        try {
            Version remoteVersion = SemanticVersion.parse(remote);
            Version localVersion = SemanticVersion.parse(local);
            return remoteVersion.compareTo(localVersion) > 0;
        } catch (VersionParsingException e) {
            return false;
        }
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Hempdustry.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }
}
