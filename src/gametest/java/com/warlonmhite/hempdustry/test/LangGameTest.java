package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import com.warlonmhite.hempdustry.item.custom.Quality;
import com.warlonmhite.hempdustry.strain.Strain;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.block.Block;
import net.minecraft.block.WallHangingSignBlock;
import net.minecraft.block.WallSignBlock;
import net.minecraft.entity.EntityType;
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
import net.minecraft.util.Util;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;

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
public final class LangGameTest implements FabricGameTest {

    /** Anything in a class's constant pool that could be one of our keys. */
    private static final Pattern LITERAL = Pattern.compile("[a-z0-9_.]*hempdustry[a-z0-9_.]*");

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void everyStringIsReadByTheGame(TestContext context) {
        ServerWorld world = context.getWorld();
        DynamicRegistryManager registries = world.getRegistryManager();
        Set<String> read = new HashSet<>();

        for (Item item : Registries.ITEM) {
            if (Sweep.ours(Registries.ITEM.getId(item))) keys(new ItemStack(item).getName(), read);
        }
        for (Block block : Registries.BLOCK) {
            if (!Sweep.ours(Registries.BLOCK.getId(block))) continue;
            keys(block.getName(), read);
            // On this version a wall sign is named after its item, the standing sign, so its own key
            // goes unread here (WallSignBlock#getTranslationKey); from 1.21.2 it names itself. Kept,
            // so the locales match the 1.21.11 build's, and counted as read.
            if (block instanceof WallSignBlock || block instanceof WallHangingSignBlock) {
                read.add(Util.createTranslationKey("block", Registries.BLOCK.getId(block)));
            }
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
        // On this version a painting's title and author are built from its id (DecorationItem).
        for (Identifier painting : registries.get(RegistryKeys.PAINTING_VARIANT).getIds()) {
            read.add(painting.toTranslationKey("painting", "title"));
            read.add(painting.toTranslationKey("painting", "author"));
        }
        registries.get(RegistryKeys.JUKEBOX_SONG).forEach(song -> keys(song.description(), read));
        registries.get(RegistryKeys.BANNER_PATTERN).forEach(pattern -> {
            for (DyeColor colour : DyeColor.values()) read.add(pattern.translationKey() + "." + colour.asString());
        });
        registries.get(Strain.REGISTRY_KEY).forEach(strain -> read.add(strain.translationKey()));
        for (Quality quality : Quality.values()) read.add(quality.getTranslationKey());
        for (AdvancementEntry entry : world.getServer().getAdvancementLoader().getAdvancements()) {
            entry.value().display().ifPresent(display -> {
                keys(display.getTitle(), read);
                keys(display.getDescription(), read);
            });
        }
        Registries.ITEM.streamTags().forEach(tag -> {
            Identifier id = tag.id();
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
