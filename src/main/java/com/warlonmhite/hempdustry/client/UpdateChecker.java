package com.warlonmhite.hempdustry.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.config.HempdustryConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.SharedConstants;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls the mod's Modrinth project for a newer release than the one currently running.
 * Client-only: a dedicated server has no player to notify and shouldn't phone home.
 */
public final class UpdateChecker {
    private static final String MODRINTH_PROJECT = "hempdustry";
    private static final String MODRINTH_PROJECT_URL = "https://modrinth.com/mod/" + MODRINTH_PROJECT;
    /**
     * Filtered to the game this client is actually running, <b>read from the game rather than
     * written down</b>. It used to be a {@code "1.21.1"} literal with a comment above it saying to
     * update it when porting — and the 1.21.11 port duly moved every other coordinate and left this
     * one behind, which would have had the checker asking Modrinth for 1.21.1 builds for ever and
     * failing silently, since a check that finds nothing is indistinguishable from one that is
     * up to date. A constant nobody can forget is better than a comment asking them not to.
     */
    private static final String MODRINTH_VERSIONS_URL =
            "https://api.modrinth.com/v2/project/" + MODRINTH_PROJECT
                    + "/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%22"
                    + URLEncoder.encode(SharedConstants.getGameVersion().name(), StandardCharsets.UTF_8)
                    + "%22%5D";

    /**
     * Which Modrinth release channels are worth interrupting a player for. Betas count as well as
     * releases, deliberately: this mod ships its features to be play-tested, and a player who wants
     * the newest one should hear about it. {@code alpha} is left out — that channel is for builds
     * that are not expected to work. Drop back to {@code Set.of("release")} to stop announcing
     * pre-releases.
     */
    private static final Set<String> NOTIFY_CHANNELS = Set.of("release", "beta");

    private static volatile String availableVersion;
    private static final AtomicBoolean NOTIFIED = new AtomicBoolean(false);

    private UpdateChecker() {
    }

    /**
     * Starts the check, unless {@code client.updateCheck} is off.
     *
     * <p><b>The switch is read here rather than at the call site</b> so there is exactly one place
     * that decides, and so nothing about the mod's start-up order has to be remembered: the config
     * is loaded by {@code Hempdustry.onInitialize}, and Fabric runs every {@code main} entrypoint
     * before any {@code client} one. If that ever stopped being true the fallback is
     * {@link HempdustryConfig#DEFAULT}, which checks — the safe way round for a switch whose only
     * job is to stop a nag.
     *
     * <p>Turning it off is what a pack curator generally wants: in a 500-mod pack with pinned
     * versions the announcement is noise <em>and</em> wrong, because the pack cannot take the
     * update anyway.
     */
    public static void init() {
        if (!HempdustryConfig.get().client().updateCheck()) {
            return;
        }
        CompletableFuture.runAsync(UpdateChecker::fetchLatest);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            String latest = availableVersion;
            if (latest != null && client.player != null && NOTIFIED.compareAndSet(false, true)) {
                client.player.sendMessage(updateMessage(latest), false);
            }
        });
    }

    /** "Hempdustry 2.1.0 is available. You have 2.0.0." followed by a clickable Modrinth link. */
    private static MutableText updateMessage(String latest) {
        MutableText link = Text.translatable("hempdustry.update.link")
                .styled(style -> style
                        .withColor(Formatting.GREEN)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(MODRINTH_PROJECT_URL)))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.translatable("hempdustry.update.tooltip"))));

        return Text.translatable("hempdustry.update.available", latest, currentVersion())
                .formatted(Formatting.GRAY)
                .append(Text.literal(" "))
                .append(link);
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

            Version local = parseOrNull(currentVersion());
            if (local == null) {
                return; // can't compare against a version we can't parse; say nothing
            }

            // The endpoint's ordering isn't part of its contract, and a hotfix published for an
            // older game version can land first — so compare every entry rather than trusting
            // whichever one came back at index 0.
            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            String bestNumber = null;
            Version best = null;

            for (JsonElement element : versions) {
                JsonObject version = element.getAsJsonObject();
                if (!isNotifiable(version)) {
                    continue;
                }
                String number = version.get("version_number").getAsString();
                Version parsed = parseOrNull(number);
                if (parsed == null) {
                    continue; // not semver, so there is nothing sensible to compare it against
                }
                if (best == null || parsed.compareTo(best) > 0) {
                    best = parsed;
                    bestNumber = number;
                }
            }

            if (best == null || best.compareTo(local) <= 0) {
                return;
            }

            availableVersion = bestNumber;
            Hempdustry.LOGGER.info("A new Hempdustry version is available: {} (you have {}). Get it at {}",
                    bestNumber, currentVersion(), MODRINTH_PROJECT_URL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Hempdustry.LOGGER.debug("Update check failed", e);
        }
    }

    private static boolean isNotifiable(JsonObject version) {
        JsonElement type = version.get("version_type");
        return type != null && !type.isJsonNull() && NOTIFY_CHANNELS.contains(type.getAsString());
    }

    /**
     * Parsed as semver so that build metadata is ignored on comparison — {@code 2.0.0+1.21.1} and
     * {@code 2.0.0} are the same release, which is what lets the jar carry its game version.
     */
    private static Version parseOrNull(String version) {
        try {
            return SemanticVersion.parse(version);
        } catch (VersionParsingException e) {
            return null;
        }
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Hempdustry.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }
}
