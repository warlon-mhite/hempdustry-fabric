package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.strain.Strain;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
            if (ours(Registries.ITEM.getId(item))) keys(new ItemStack(item).getName(), read);
        }
        for (Block block : Registries.BLOCK) {
            if (ours(Registries.BLOCK.getId(block))) keys(block.getName(), read);
        }
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            if (ours(Registries.ENTITY_TYPE.getId(type))) keys(type.getName(), read);
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
        // In a dev run the classes are a classpath entry of their own, beside the mod's resources.
        Set<Path> roots = new HashSet<>(FabricLoader.getInstance().getModContainer(Hempdustry.MOD_ID).orElseThrow().getRootPaths());
        try {
            roots.add(Path.of(Hempdustry.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        for (Path root : roots) {
            for (Path file : files(root)) {
                String name = file.toString();
                if (name.endsWith(".class")) {
                    Matcher m = LITERAL.matcher(new String(bytes(file), StandardCharsets.ISO_8859_1));
                    while (m.find()) literals.add(m.group());
                } else if (name.contains("assets/hempdustry/lang/") && name.endsWith(".json")) {
                    langKeys.addAll(json(file).keySet());
                } else if (name.endsWith("assets/hempdustry/sounds.json")) {
                    // The client reads a subtitle only for a sound event the server registered.
                    json(file).entrySet().forEach(e -> {
                        if (Registries.SOUND_EVENT.containsId(Identifier.of(Hempdustry.MOD_ID, e.getKey()))
                                && e.getValue().getAsJsonObject().has("subtitle")) {
                            read.add(e.getValue().getAsJsonObject().get("subtitle").getAsString());
                        }
                    });
                }
            }
        }
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

    // ponytail: a literal like "hempdustry.category." stands for every key one segment below it, so
    // a stale key under a built prefix goes unseen; list the ids the code appends if that ever bites.
    private static boolean builtFromPrefix(String key, Set<String> literals) {
        int dot = key.lastIndexOf('.');
        return dot > 0 && literals.contains(key.substring(0, dot + 1));
    }

    private static boolean ours(Identifier id) {
        return id.getNamespace().equals(Hempdustry.MOD_ID);
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

    private static Iterable<Path> files(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] bytes(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonObject json(Path file) {
        return JsonParser.parseString(new String(bytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
