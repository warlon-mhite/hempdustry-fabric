package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Every string in every lang file is one the game actually asks for — <b>and this test exists
 * because six of them were not.</b>
 *
 * <h2>The bug, and why nothing caught it</h2>
 *
 * The paintings' titles and authors were lang keys, {@code painting.hempdustry.<id>.title} and
 * {@code .author}, present in all twelve locales. On 1.21.1 the game builds those keys from the
 * variant's id, so they showed. Since 1.21.2 a {@code painting_variant} carries its {@code title} and
 * {@code author} as optional text components <em>in its JSON</em>, and ours carried neither — so on
 * 1.21.11 every painting shipped with no title and no author. The fields are optional, so nothing
 * logged; the keys existed, so the locale gate and {@code lang_coverage.py} were satisfied. A key the
 * game never reads looks exactly like a key that works, until a player hovers over it.
 *
 * <h2>What this checks</h2>
 *
 * It asks the running game, not the files, which keys it reads: the name of every item, block and
 * entity of ours and of every stack the creative tab builds (a loaded spliff names its load), the
 * tab itself, every painting variant's title and author, every jukebox song, banner pattern (in all
 * sixteen colours), strain, quality grade and advancement, the name of every item tag outside
 * {@code minecraft}, and the subtitle of every sound event we register. What is left
 * can only be asked for by code — a tooltip line, a refusal, a recipe-viewer page — so those keys must
 * appear as a string in the mod's compiled classes. Any key in any locale reached by none of that is a
 * failure, and so is any key the game asks for that {@code en_us} does not have.
 */
public final class LangGameTest {

    /** Anything in a class's constant pool that could be one of our keys. */
    private static final Pattern LITERAL = Pattern.compile("[a-z0-9_.]*hempdustry[a-z0-9_.]*");

    public static void everyStringIsReadByTheGame(TestContext context) {
        ServerWorld world = context.getWorld();
        DynamicRegistryManager registries = world.getRegistryManager();
        Set<String> read = new HashSet<>();

        for (Item item : Registries.ITEM) {
            if (Sweep.ours(Registries.ITEM.getId(item))) keys(new ItemStack(item).getName(), read);
        }
        for (Block block : Registries.BLOCK) {
            if (Sweep.ours(Registries.BLOCK.getId(block))) keys(block.getName(), read);
        }
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            if (Sweep.ours(Registries.ENTITY_TYPE.getId(type))) keys(type.getName(), read);
        }
        // A loaded spliff, device or moon rock names itself from what is in it, so the stacks the
        // creative tab builds are named too, not just each item fresh.
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.updateEntries(new ItemGroup.DisplayContext(
                world.getEnabledFeatures(), true, registries));
        for (ItemStack stack : ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabStacks()) keys(stack.getName(), read);
        keys(ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getDisplayName(), read);
        for (PaintingVariant variant : registries.getOrThrow(RegistryKeys.PAINTING_VARIANT)) {
            variant.title().ifPresent(text -> keys(text, read));
            variant.author().ifPresent(text -> keys(text, read));
        }
        registries.getOrThrow(RegistryKeys.JUKEBOX_SONG).forEach(song -> keys(song.description(), read));
        registries.getOrThrow(RegistryKeys.BANNER_PATTERN).forEach(pattern -> {
            for (DyeColor colour : DyeColor.values()) read.add(pattern.translationKey() + "." + colour.getId());
        });
        registries.getOrThrow(Strain.REGISTRY_KEY).forEach(strain -> read.add(strain.translationKey()));
        for (Quality quality : Quality.values()) read.add(quality.getTranslationKey());
        for (AdvancementEntry entry : world.getServer().getAdvancementLoader().getAdvancements()) {
            entry.value().display().ifPresent(display -> {
                keys(display.getTitle(), read);
                keys(display.getDescription(), read);
            });
        }
        Registries.ITEM.streamTags().forEach(tag -> {
            Identifier id = tag.getTag().id();
            if (!id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                read.add("tag.item." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
            }
        });

        Set<String> literals = new HashSet<>();
        Set<String> langKeys = new TreeSet<>();
        Sweep.modFiles().forEach((name, file) -> {
            if (name.endsWith(".class")) {
                Matcher m = LITERAL.matcher(new String(Sweep.bytes(file), StandardCharsets.ISO_8859_1));
                while (m.find()) literals.add(m.group());
            } else if (name.startsWith("assets/hempdustry/lang/") && name.endsWith(".json")) {
                langKeys.addAll(Sweep.json(file).getAsJsonObject().keySet());
            } else if (name.equals("assets/hempdustry/sounds.json")) {
                // The client reads a subtitle only for a sound event the server registered.
                Sweep.json(file).getAsJsonObject().entrySet().forEach(e -> {
                    if (Registries.SOUND_EVENT.containsId(Identifier.of(Hempdustry.MOD_ID, e.getKey()))
                            && e.getValue().getAsJsonObject().has("subtitle")) {
                        read.add(e.getValue().getAsJsonObject().get("subtitle").getAsString());
                    }
                });
            }
        });
        context.assertTrue(literals.size() > 30, "found only " + literals.size() + " strings in the classes: the scan missed them");
        context.assertTrue(langKeys.size() > 100, "found only " + langKeys.size() + " lang keys: the lang files moved");

        Set<String> unread = new TreeSet<>();
        for (String key : langKeys) {
            if (!read.contains(key) && !literals.contains(key) && !builtFromPrefix(key, literals)) unread.add(key);
        }

        Set<String> missing = new TreeSet<>();
        for (String key : read) {
            if (key.contains(Hempdustry.MOD_ID) && !langKeys.contains(key)) missing.add(key);
        }
        context.assertTrue(unread.isEmpty() && missing.isEmpty(), "nothing in the game reads these lang keys: "
                + unread + "; the game asks for these and no lang file has them: " + missing);
        context.complete();
    }

    /**
     * Every tooltip line the creative tab's stacks build — in both the basic and the advanced view —
     * names only keys some lang file has: ours, or vanilla's.
     *
     * <p>{@link #everyStringIsReadByTheGame} finds a tooltip's keys only as strings in the compiled
     * classes, so a key built at run time ({@code "tooltip.hempdustry." + something}) is invisible to
     * it until a player reads the raw key off the screen. This builds every tooltip for real.
     */
    public static void everyTooltipNamesARealKey(TestContext context) {
        ServerWorld world = context.getWorld();
        ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.updateEntries(new ItemGroup.DisplayContext(
                world.getEnabledFeatures(), true, world.getRegistryManager()));
        Set<String> ours = Sweep.json(Sweep.modFiles().get("assets/hempdustry/lang/en_us.json")).getAsJsonObject().keySet();
        Map<String, String> findings = new TreeMap<>();
        int lines = 0;
        for (ItemStack stack : ModItemGroups.HEMPDUSTRY_ITEMS_GROUP.getSearchTabStacks()) {
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            for (TooltipType type : List.of(TooltipType.BASIC, TooltipType.ADVANCED)) {
                List<Text> tooltip;
                try {
                    tooltip = stack.getTooltip(Item.TooltipContext.create(world), null, type);
                } catch (RuntimeException | LinkageError e) {
                    findings.putIfAbsent(id, "building its tooltip threw " + e);
                    continue;
                }
                Set<String> asked = new TreeSet<>();
                tooltip.forEach(line -> keys(line, asked));
                lines += tooltip.size();
                for (String key : asked) {
                    if (!ours.contains(key) && !Language.getInstance().hasTranslation(key)) {
                        findings.putIfAbsent(id + " " + key, "its tooltip asks for a key no lang file has");
                    }
                }
            }
        }
        Sweep.atLeast(context, "tooltip lines", lines, 300);
        Sweep.report(context, "Every tooltip names a real key", findings, Map.of());
        context.complete();
    }

    /**
     * Every translation of a string keeps the string's placeholders: the same {@code %s},
     * {@code %d} and {@code %1$s} as {@code en_us}, as many of each.
     *
     * <p>A translation that drops one shows the player a sentence with a hole in it; one that adds a
     * {@code %s} English never had prints a literal {@code %s}. Translations arrive in packets from
     * people who do not run the game, so nothing else would see it.
     */
    public static void everyTranslationKeepsItsPlaceholders(TestContext context) {
        Pattern placeholder = Pattern.compile("%(?:\\d+\\$)?[sd]|%%");
        JsonObject english = Sweep.json(Sweep.modFiles().get("assets/hempdustry/lang/en_us.json")).getAsJsonObject();
        Map<String, String> findings = new TreeMap<>();
        int compared = 0;
        for (Map.Entry<String, java.nio.file.Path> file : Sweep.modFiles().entrySet()) {
            if (!file.getKey().startsWith("assets/hempdustry/lang/") || file.getKey().endsWith("/en_us.json")) continue;
            String locale = file.getKey().substring(file.getKey().lastIndexOf('/') + 1).replace(".json", "");
            for (Map.Entry<String, JsonElement> entry : Sweep.json(file.getValue()).getAsJsonObject().entrySet()) {
                if (!english.has(entry.getKey())) continue;
                compared++;
                List<String> want = placeholders(placeholder, english.get(entry.getKey()).getAsString());
                List<String> got = placeholders(placeholder, entry.getValue().getAsString());
                if (!want.equals(got)) {
                    findings.put(locale + " " + entry.getKey(), "has " + got + " where en_us has " + want);
                }
            }
        }
        Sweep.atLeast(context, "translated strings", compared, 1000);
        Sweep.report(context, "Every translation keeps its placeholders", findings, Map.of());
        context.complete();
    }

    private static List<String> placeholders(Pattern placeholder, String text) {
        List<String> found = new ArrayList<>();
        Matcher m = placeholder.matcher(text);
        while (m.find()) found.add(m.group());
        found.sort(null);
        return found;
    }

    // ponytail: a literal like "hempdustry.category." stands for every key one segment below it, so
    // a stale key under a built prefix goes unseen; list the ids the code appends if that ever bites.
    private static boolean builtFromPrefix(String key, Set<String> literals) {
        int dot = key.lastIndexOf('.');
        return dot > 0 && literals.contains(key.substring(0, dot + 1));
    }

    private static void keys(Text text, Set<String> into) {
        if (text.getContent() instanceof TranslatableTextContent translatable) {
            into.add(translatable.getKey());
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Text inner) keys(inner, into);
            }
        }
        for (Text sibling : text.getSiblings()) keys(sibling, into);
    }
}
