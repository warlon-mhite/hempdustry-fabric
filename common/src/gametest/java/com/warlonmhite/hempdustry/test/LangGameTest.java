package com.warlonmhite.hempdustry.test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.strain.Strain;
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
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.advancements.Advancement;
import com.warlonmhite.hempdustry.registry.DataRegistries;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.core.Registry;
import com.warlonmhite.hempdustry.test.framework.GameTest;
import com.warlonmhite.hempdustry.test.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.WallSignBlock;

/**
 * Every string in every lang file is one the game actually asks for — <b>and this test exists
 * because six of them were not.</b>
 *
 * <h2>The bug, and why nothing caught it</h2>
 *
 * The paintings' titles and authors are lang keys, {@code painting.hempdustry.<id>.title} and
 * {@code .author}, present in all twelve locales. On this version the game builds those keys from
 * the variant's id, so they show. From 1.21.2 a {@code painting_variant} carries its {@code title}
 * and {@code author} as optional text components <em>in its JSON</em>, and the 1.21.11 build carried
 * neither — so there every painting shipped with no title and no author. The fields are optional, so
 * nothing logged; the keys existed, so the locale gate and {@code lang_coverage.py} were satisfied. A
 * key the game never reads looks exactly like a key that works, until a player hovers over it.
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

    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyStringIsReadByTheGame(GameTestHelper context) {
        ServerLevel world = context.getLevel();
        Set<String> read = new HashSet<>();

        for (Item item : Registry.ITEM) {
            if (Sweep.ours(Registry.ITEM.getKey(item))) keys(new ItemStack(item).getHoverName(), read);
        }
        for (Block block : Registry.BLOCK) {
            if (!Sweep.ours(Registry.BLOCK.getKey(block))) continue;
            keys(new TranslatableComponent(block.getDescriptionId()), read);
            // On this version a wall sign is named after its item, the standing sign, so its own key
            // goes unread here (WallSignBlock#getDescriptionId); from 1.21.2 it names itself. Kept,
            // so the locales match the 1.21.11 build's, and counted as read.
            if (block instanceof WallSignBlock) {
                read.add(Util.makeDescriptionId("block", Registry.BLOCK.getKey(block)));
            }
            // And a sign's item is named after its block (BlockItem#getTranslationKey), so the item
            // key goes unread too; from 1.21.2 an item names itself. Kept and counted the same way.
            if (block instanceof SignBlock && !(block instanceof WallSignBlock)) {
                read.add(Util.makeDescriptionId("item", Registry.ITEM.getKey(block.asItem())));
            }
        }
        for (EntityType<?> type : Registry.ENTITY_TYPE) {
            if (Sweep.ours(Registry.ENTITY_TYPE.getKey(type))) keys(type.getDescription(), read);
        }
        // A loaded spliff, device or moon rock names itself from what is in it, so the stacks the
        // creative tab builds are named too, not just each item fresh.
        for (ItemStack stack : CreativeTab.search()) keys(stack.getHoverName(), read);
        keys(CreativeTab.title(), read);
        // 1.16.5 shows no painting's title or author anywhere: the keys are kept so the locales match
        // the newer lines', where a painting names itself from them, and counted as read for each
        // painting of ours.
        for (ResourceLocation painting : Registry.MOTIVE.keySet()) {
            if (!Sweep.ours(painting)) continue;
            read.add("painting." + painting.getNamespace() + "." + painting.getPath() + ".title");
            read.add("painting." + painting.getNamespace() + "." + painting.getPath() + ".author");
        }
        // On this version a disc's "Now Playing" line is its item's .desc key (RecordItem#getDisplayName,
        // client-only, so built here as it builds it), and a banner pattern's name per colour is built
        // from its file name (BannerItem), a field the server keeps but has no getter for.
        for (Item item : Registry.ITEM) {
            if (item instanceof RecordItem && Sweep.ours(Registry.ITEM.getKey(item))) read.add(item.getDescriptionId() + ".desc");
        }
        for (BannerPattern pattern : BannerPattern.values()) {
            String file = bannerFile(pattern);
            if (!file.startsWith(Hempdustry.MOD_ID)) continue;
            for (DyeColor colour : DyeColor.values()) {
                read.add("block.minecraft.banner." + file + "." + colour.getName());
            }
        }
        DataRegistries.of(world).lookupOrThrow(Strain.REGISTRY_KEY).listElements()
                .forEach(strain -> read.add(strain.value().translationKey()));
        for (Quality quality : Quality.values()) read.add(quality.getTranslationKey());
        for (Advancement entry : world.getServer().getAdvancements().getAllAdvancements()) {
            if (entry.getDisplay() != null) {
                keys(entry.getDisplay().getTitle(), read);
                keys(entry.getDisplay().getDescription(), read);
            }
        }
        // Nothing on 1.16.5 shows an item tag's name (no viewer of this line names tags); the keys are
        // kept so the locales match the newer lines', and counted as read for every tag that exists.
        world.getServer().getTags().getItems().getAvailableTags().forEach(id -> {
            if (!id.getNamespace().equals("minecraft")) {
                read.add("tag.item." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
            }
        });

        Set<String> literals = new HashSet<>();
        Set<String> langKeys = new TreeSet<>();
        Set<String> english = new HashSet<>();
        Sweep.modFiles().forEach((name, file) -> {
            if (name.endsWith(".class")) {
                Matcher m = LITERAL.matcher(new String(Sweep.bytes(file), StandardCharsets.ISO_8859_1));
                while (m.find()) literals.add(m.group());
            } else if (name.startsWith("assets/hempdustry/lang/") && name.endsWith(".json")) {
                Set<String> keys = keySet(Sweep.json(file).getAsJsonObject());
                langKeys.addAll(keys);
                // English is the fallback every other locale shows a missing string in.
                if (name.endsWith("/en_us.json")) english.addAll(keys);
            } else if (name.equals("assets/hempdustry/sounds.json")) {
                // The client reads a subtitle only for a sound event the server registered.
                Sweep.json(file).getAsJsonObject().entrySet().forEach(e -> {
                    if (Registry.SOUND_EVENT.keySet().contains(new ResourceLocation(Hempdustry.MOD_ID, e.getKey()))
                            && e.getValue().getAsJsonObject().has("subtitle")) {
                        read.add(e.getValue().getAsJsonObject().get("subtitle").getAsString());
                    }
                });
            }
        });
        context.assertTrue(literals.size() > 30, "found only " + literals.size() + " strings in the classes: the scan missed them");
        context.assertTrue(langKeys.size() > 100, "found only " + langKeys.size() + " lang keys: the lang files moved");
        context.assertTrue(english.size() > 100, "found only " + english.size() + " en_us keys: en_us.json moved");

        Set<String> unread = new TreeSet<>();
        for (String key : langKeys) {
            if (!read.contains(key) && !literals.contains(key) && !builtFromPrefix(key, literals)) unread.add(key);
        }

        Set<String> missing = new TreeSet<>();
        for (String key : read) {
            if (key.contains(Hempdustry.MOD_ID) && !english.contains(key)) missing.add(key);
        }
        context.assertTrue(unread.isEmpty() && missing.isEmpty(), "nothing in the game reads these lang keys: "
                + unread + "; the game asks for these and en_us does not have them: " + missing);
        context.succeed();
    }

    // everyTooltipNamesARealKey is not on 1.16.5's test server: a tooltip is client-only there
    // (TooltipFlag, Item#appendHoverText and ItemStack#getTooltipLines are stripped from a dedicated
    // server), so only a client can build one.

    /**
     * Every translation of a string keeps the string's placeholders: the same {@code %s},
     * {@code %d} and {@code %1$s} as {@code en_us}, as many of each.
     *
     * <p>A translation that drops one shows the player a sentence with a hole in it; one that adds a
     * {@code %s} English never had prints a literal {@code %s}. Translations arrive in packets from
     * people who do not run the game, so nothing else would see it.
     */
    @GameTest(template = HempdustryGameTests.EMPTY, timeoutTicks = 200)
    public void everyTranslationKeepsItsPlaceholders(GameTestHelper context) {
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
        Sweep.report(context, "Every translation keeps its placeholders", findings, ImmutableMap.of());
        context.succeed();
    }

    private static List<String> placeholders(Pattern placeholder, String text) {
        List<String> found = new ArrayList<>();
        Matcher m = placeholder.matcher(text);
        while (m.find()) found.add(m.group());
        found.sort(null);
        return found;
    }

    /**
     * The names of items, blocks and entities, whose every key the registries above already ask for.
     * {@code "item." + MOD_ID + "." + name} compiles to the literal {@code item.hempdustry.}, which
     * would otherwise stand for every item key there is, a dead one included.
     */
    private static final Set<String> REGISTRY_PREFIXES = ImmutableSet.of(
            "item." + Hempdustry.MOD_ID + ".", "block." + Hempdustry.MOD_ID + ".", "entity." + Hempdustry.MOD_ID + ".");

    // ponytail: a literal like "hempdustry.category." stands for every key one segment below it, so
    // a stale key under a built prefix goes unseen; list the ids the code appends if that ever bites.
    private static boolean builtFromPrefix(String key, Set<String> literals) {
        int dot = key.lastIndexOf('.');
        String prefix = dot > 0 ? key.substring(0, dot + 1) : null;
        return prefix != null && !REGISTRY_PREFIXES.contains(prefix) && literals.contains(prefix);
    }

    /** Gson 2.8.0, 1.16.5's, has no {@code JsonObject#keySet}. */
    private static Set<String> keySet(JsonObject object) {
        Set<String> keys = new HashSet<>();
        object.entrySet().forEach(entry -> keys.add(entry.getKey()));
        return keys;
    }

    private static String bannerFile(BannerPattern pattern) {
        try {
            java.lang.reflect.Field file = BannerPattern.class.getDeclaredField("filename");
            file.setAccessible(true);
            return (String) file.get(pattern);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("a banner pattern no longer keeps its file name where the test reads it", e);
        }
    }

    private static void keys(Component text, Set<String> into) {
        if (text instanceof TranslatableComponent translatable) {
            into.add(translatable.getKey());
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component inner) keys(inner, into);
            }
        }
        for (Component sibling : text.getSiblings()) keys(sibling, into);
    }
}
