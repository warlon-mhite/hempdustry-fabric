package com.warlonmhite.hempdustry.test;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItemGroups;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The creative tab's stacks, built on the test server the way a 1.18.2 client builds them.
 *
 * <p>On 1.18.2 a tab fills its list in {@code CreativeModeTab#fillItemList}, and the search tab asks
 * every item for its own stacks ({@code Item#fillItemCategory}, which {@code CreativeSearchMixin}
 * answers for ours from {@link ModItemGroups#searchStacks}). Only a client ever calls
 * {@code fillItemList} and {@code getDisplayName}; the mod's tab overrides both, and its own methods
 * are reached by reflection, so the test leans on nothing a dedicated server's classes may lack. The newer lines'
 * {@code buildContents} / {@code getDisplayItems} / {@code getSearchTabDisplayItems} map to
 * {@link #tab()} and {@link #search()}.
 */
final class CreativeTab {

    private CreativeTab() {
    }

    /** The tab's own list, in order: what a player sees on opening it. */
    @SuppressWarnings("unchecked")
    static List<ItemStack> tab() {
        NonNullList<ItemStack> list = NonNullList.create();
        call("fillItemList", new Class<?>[]{NonNullList.class}, list);
        return new ArrayList<>(list);
    }

    /** What the search tab lists for our items, in the registry's order, as the search tab asks for them. */
    static List<ItemStack> search() {
        ModItemGroups.clearSearchCache();
        List<ItemStack> out = new ArrayList<>();
        for (Item item : Registry.ITEM) {
            if (Hempdustry.MOD_ID.equals(Registry.ITEM.getKey(item).getNamespace())) {
                out.addAll(ModItemGroups.searchStacks(item));
            }
        }
        return out;
    }

    /** The tab's title. */
    static Component title() {
        return (Component) call("getDisplayName", new Class<?>[0]);
    }

    private static Object call(String name, Class<?>[] types, Object... args) {
        CreativeModeTab tab = ModItemGroups.HEMPDUSTRY_ITEMS_GROUP;
        for (Class<?> c = tab.getClass(); c != null && c != CreativeModeTab.class; c = c.getSuperclass()) {
            try {
                Method method = c.getDeclaredMethod(name, types);
                method.setAccessible(true);
                return method.invoke(tab, args);
            } catch (NoSuchMethodException e) {
                // a superclass of the tab's may hold it
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new IllegalStateException(e.getCause());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalStateException("the mod's creative tab does not override " + name
                + ", and the test reads it nowhere else");
    }
}
