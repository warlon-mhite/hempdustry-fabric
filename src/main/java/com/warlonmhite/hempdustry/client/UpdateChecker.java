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
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
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
 * Looks for a newer build than the one running, and tells the player once, on join.
 * Client-only: a dedicated server has no player to notify and shouldn't phone home.
 *
 * <h2>Two sources, because one of them cannot reach most players</h2>
 *
 * This mod is published on <b>CurseForge, NexusMods, GitHub and Modrinth</b>, and only two of those
 * can be asked at all:
 *
 * <ul>
 *   <li><b>CurseForge and NexusMods are impossible from inside a jar, and that is their rule, not a
 *       gap in this code.</b> Both gate their APIs behind a key whose terms forbid disclosing it to
 *       a third party — and a key shipped inside a mod is disclosed to every player who downloads
 *       it, extractable in seconds. There is no compliant way to query either.</li>
 *   <li><b>Modrinth</b> is public and keyless, but it only knows about Modrinth, so a CurseForge or
 *       Nexus player learns nothing from it.</li>
 * </ul>
 *
 * So the primary source is {@link #MANIFEST_URL} — <b>a static file this repo publishes about
 * itself</b>, served by raw.githubusercontent. No key, no approval, no review queue, no meaningful
 * rate limit (a raw file is CDN-served, unlike GitHub's 60-per-hour REST API), and it reaches a
 * player <em>whichever site they got the mod from</em>. That is also the shape Forge and NeoForge's
 * own update checkers have always used: a JSON manifest the author hosts, never a platform's API.
 * Ours is deliberately written in Forge's format so a NeoForge build can point {@code updateJSONURL}
 * at the very same file.
 *
 * <p><b>Modrinth stays as a second source</b>, asked straight after it, with the higher version
 * winning. It costs one request and covers the manifest's one weakness: the manifest is
 * hand-maintained and can be forgotten at release time, while Modrinth's answer is generated from
 * what was actually uploaded.
 *
 * <h2>Where the link points is data, not code</h2>
 *
 * The manifest's {@code homepage} decides where the chat line sends a player. So the day the
 * Modrinth listing clears review — or the day a new storefront is added — that is a one-line edit to
 * a JSON file rather than a new build of the mod. It is checked, though, because it ends up in a
 * click event: anything that is not an {@code http(s)} link with a host falls back to
 * {@link #RELEASES_URL}. The manifest lives in that repository, so that page is there whenever the
 * manifest is — unlike the Modrinth page, which does not exist until the listing clears review.
 */
public final class UpdateChecker {
    private static final String MODRINTH_PROJECT = "hempdustry";
    private static final String MODRINTH_PROJECT_URL = "https://modrinth.com/mod/" + MODRINTH_PROJECT;
    /** Where a manifest with no usable {@code homepage} sends a player. See the class javadoc. */
    private static final String RELEASES_URL = "https://github.com/warlon-mhite/hempdustry-fabric/releases";

    /**
     * The mod's own update manifest — the primary source. Pinned to {@code master} on purpose: there
     * is <b>one</b> manifest for every branch and every game version, keyed by Minecraft version
     * inside, so a 1.21.1 client reads the same file a 1.21.11 client does and there is one place to
     * bump when a release goes out.
     */
    private static final String MANIFEST_URL =
            "https://raw.githubusercontent.com/warlon-mhite/hempdustry-fabric/master/updates/hempdustry.json";

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
                    + URLEncoder.encode(gameVersion(), StandardCharsets.UTF_8)
                    + "%22%5D";

    /**
     * Which Modrinth release channels are worth interrupting a player for. Betas count as well as
     * releases, deliberately: this mod ships its features to be play-tested, and a player who wants
     * the newest one should hear about it. {@code alpha} is left out — that channel is for builds
     * that are not expected to work. Drop back to {@code Set.of("release")} to stop announcing
     * pre-releases.
     *
     * <p>The manifest's matching choice is its {@code -latest} key rather than {@code -recommended};
     * the two sources have to agree about what counts, or "prefer the higher" picks a fight between
     * them.
     */
    private static final Set<String> NOTIFY_CHANNELS = Set.of("release", "beta");

    /** Short: this runs while the player is waiting to get into a world. */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static volatile String availableVersion;
    private static volatile String downloadUrl = MODRINTH_PROJECT_URL;
    private static final AtomicBoolean NOTIFIED = new AtomicBoolean(false);

    private UpdateChecker() {
    }

    /** A version some source is offering, and where that source says to get it. */
    private record Candidate(String number, Version version, String homepage) {
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
     * <p><b>One switch covers both sources</b>, and a second one is not wanted: a player turning the
     * check off means "stop asking", not "ask somewhere else".
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

    /** "Hempdustry 2.1.0 is available. You have 2.0.0." followed by a clickable download link. */
    private static MutableText updateMessage(String latest) {
        MutableText link = Text.translatable("hempdustry.update.link")
                .styled(style -> style
                        .withColor(Formatting.GREEN)
                        .withUnderline(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(downloadUrl)))
                        .withHoverEvent(new HoverEvent.ShowText(
                                Text.translatable("hempdustry.update.tooltip"))));

        return Text.translatable("hempdustry.update.available", latest, currentVersion())
                .formatted(Formatting.GRAY)
                .append(Text.literal(" "))
                .append(link);
    }

    /**
     * Asks both sources and keeps the better answer.
     *
     * <p>Neither source failing is an error worth a log line at anything above debug: a player
     * offline, behind a firewall, or on a build newer than either source knows about is the ordinary
     * case, and an update check is the last thing that should shout.
     */
    private static void fetchLatest() {
        Version local = parseOrNull(currentVersion());
        if (local == null) {
            // Can't compare against a version we can't parse, so say nothing. This is also why a new
            // version format has to be run through SemanticVersion.parse before shipping it: it
            // turns the checker off silently, on exactly the build that most needs to announce its
            // successor. See release.md.
            return;
        }

        Candidate best = better(fetchFromManifest(), fetchFromModrinth());
        if (best == null || best.version().compareTo(local) <= 0) {
            return;
        }

        downloadUrl = best.homepage();
        availableVersion = best.number();
        Hempdustry.LOGGER.info("A new Hempdustry version is available: {} (you have {}). Get it at {}",
                best.number(), currentVersion(), best.homepage());
    }

    private static @Nullable Candidate better(@Nullable Candidate a, @Nullable Candidate b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.version().compareTo(b.version()) >= 0 ? a : b;
    }

    /**
     * The mod's own manifest: {@code promos}, keyed {@code <game version>-latest}.
     *
     * <p>An entry that does not parse as semver is skipped rather than failing the check — the file
     * is written by hand, and one fat-fingered key must not take the whole feature down with it.
     */
    private static @Nullable Candidate fetchFromManifest() {
        JsonObject root = fetchJson(MANIFEST_URL, "manifest");
        if (root == null || !root.has("promos") || !root.get("promos").isJsonObject()) {
            return null;
        }
        JsonElement number = root.getAsJsonObject("promos").get(gameVersion() + "-latest");
        if (number == null || !number.isJsonPrimitive()) {
            return null; // no build for this game version yet, which is a perfectly ordinary answer
        }
        Version parsed = parseOrNull(number.getAsString());
        if (parsed == null) {
            Hempdustry.LOGGER.debug("Update manifest: ignoring unparseable version {}", number);
            return null;
        }
        return new Candidate(number.getAsString(), parsed, webLinkOr(root.get("homepage"), RELEASES_URL));
    }

    /**
     * {@code element} if it is an {@code http} or {@code https} link with a host, else
     * {@code fallback}. The manifest is written by hand and the link lands in a click event on join,
     * so a typo in it must cost the link, not the message.
     */
    private static String webLinkOr(@Nullable JsonElement element, String fallback) {
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        try {
            URI uri = new URI(element.getAsString());
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null) {
                return uri.toString();
            }
        } catch (URISyntaxException ignored) {
            // falls through to the fallback, like any other link that is not a web page
        }
        Hempdustry.LOGGER.debug("Update manifest: ignoring homepage {}, not a web link", element);
        return fallback;
    }

    /**
     * Modrinth's own list of what was actually uploaded.
     *
     * <p>The endpoint's ordering isn't part of its contract, and a hotfix published for an older
     * game version can land first — so every entry is compared rather than trusting whichever one
     * came back at index 0.
     */
    private static @Nullable Candidate fetchFromModrinth() {
        JsonElement body = fetchJsonElement(MODRINTH_VERSIONS_URL, "Modrinth");
        if (body == null || !body.isJsonArray()) {
            return null;
        }
        JsonArray versions = body.getAsJsonArray();
        Candidate best = null;

        for (JsonElement element : versions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject version = element.getAsJsonObject();
            if (!isNotifiable(version) || !version.has("version_number")) {
                continue;
            }
            String number = version.get("version_number").getAsString();
            Version parsed = parseOrNull(number);
            if (parsed == null) {
                continue; // not semver, so there is nothing sensible to compare it against
            }
            Candidate candidate = new Candidate(number, parsed, MODRINTH_PROJECT_URL);
            if (best == null || candidate.version().compareTo(best.version()) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    private static @Nullable JsonObject fetchJson(String url, String what) {
        JsonElement element = fetchJsonElement(url, what);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    /**
     * One GET, or {@code null}. <b>The User-Agent is not optional</b>: Modrinth asks for a
     * uniquely-identifying one and treats a generic library string (which is what Java's HTTP client
     * sends by default) as grounds for blocking.
     */
    private static @Nullable JsonElement fetchJsonElement(String url, String what) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "warlon-mhite/hempdustry-fabric/" + currentVersion() + " (update checker)")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                Hempdustry.LOGGER.debug("Update check: {} returned HTTP {}", what, response.statusCode());
                return null;
            }
            return JsonParser.parseString(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            Hempdustry.LOGGER.debug("Update check against {} failed", what, e);
            return null;
        }
    }

    private static boolean isNotifiable(JsonObject version) {
        JsonElement type = version.get("version_type");
        return type != null && !type.isJsonNull() && NOTIFY_CHANNELS.contains(type.getAsString());
    }

    /**
     * Parsed as semver so that build metadata is ignored on comparison — {@code 2.0.0+1.21.1} and
     * {@code 2.0.0} are the same release, which is what lets the jar carry its game version, and
     * what lets the manifest write the full jar name a player actually downloaded.
     */
    private static @Nullable Version parseOrNull(String version) {
        try {
            return SemanticVersion.parse(version);
        } catch (VersionParsingException e) {
            return null;
        }
    }

    /** The Minecraft version this client is running, as both sources key their answers by it. */
    private static String gameVersion() {
        return SharedConstants.getGameVersion().name();
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(Hempdustry.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("0.0.0");
    }
}
