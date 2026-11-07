package com.warlonmhite.hempdustry.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.warlonmhite.hempdustry.Hempdustry;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * An item predicate naming several items, as the newer lines' {@code "items": [...]} does: 1.16.5's own
 * predicate names one item or one tag. Written {@code {"type": "hempdustry:any_of", "items": [...], ...}};
 * every other field is vanilla's and is read by vanilla. It keeps an advancement's criterion one criterion
 * -- "smoked a bong", whichever glass it was -- so a world taken on to a newer line keeps the progress
 * under the same name. Forge 36 reads a predicate's {@code type} (vanilla has none).
 */
public final class AnyOfItemPredicate extends ItemPredicate {
    public static final ResourceLocation TYPE = new ResourceLocation(Hempdustry.MOD_ID, "any_of");

    private final Set<Item> items;
    private final ItemPredicate rest;
    private final JsonObject json;

    private AnyOfItemPredicate(Set<Item> items, ItemPredicate rest, JsonObject json) {
        this.items = items;
        this.rest = rest;
        this.json = json;
    }

    public static void register() {
        ItemPredicate.register(TYPE, AnyOfItemPredicate::fromJson);
    }

    private static ItemPredicate fromJson(JsonObject json) {
        JsonArray list = GsonHelper.getAsJsonArray(json, "items");
        Set<Item> items = new LinkedHashSet<>();
        for (JsonElement each : list) {
            ResourceLocation id = new ResourceLocation(GsonHelper.convertToString(each, "item"));
            items.add(Registry.ITEM.getOptional(id).orElseThrow(() -> new JsonSyntaxException("Unknown item id '" + id + "'")));
        }
        JsonObject rest = new JsonObject();
        for (Map.Entry<String, JsonElement> field : json.entrySet()) {
            if (!field.getKey().equals("type") && !field.getKey().equals("items")) {
                rest.add(field.getKey(), field.getValue());
            }
        }
        return new AnyOfItemPredicate(items, ItemPredicate.fromJson(rest), json);
    }

    @Override
    public boolean matches(ItemStack stack) {
        return items.contains(stack.getItem()) && rest.matches(stack);
    }

    @Override
    public JsonElement serializeToJson() {
        return json;
    }
}
