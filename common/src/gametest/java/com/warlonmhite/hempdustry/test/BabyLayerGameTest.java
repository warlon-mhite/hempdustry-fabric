package com.warlonmhite.hempdustry.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * Since 26.1 a baby zombie, husk, drowned, zombie villager or piglin draws what it wears from the
 * equipment asset's {@code humanoid_baby} layer: one texture for every slot, laid out for the baby
 * armour model ({@code HumanoidModel#createBabyArmorMesh}, 64×64). An asset with no such layer draws
 * nothing on a baby, and says nothing — a baby zombie that picked up a hemp shirt wears it
 * invisibly. Vanilla gives every armour set one, leather included.
 */
public final class BabyLayerGameTest {

    private static final String EQUIPMENT = "assets/hempdustry/equipment/";

    public static void everyOutfitComesInABabySize(GameTestHelper context) {
        Map<String, String> findings = new TreeMap<>();
        int assets = 0;
        int textures = 0;
        for (Map.Entry<String, Path> file : Sweep.modFiles().entrySet()) {
            if (!file.getKey().startsWith(EQUIPMENT) || !file.getKey().endsWith(".json")) continue;
            assets++;
            String asset = file.getKey().substring(EQUIPMENT.length(), file.getKey().length() - ".json".length());
            JsonObject layers = Sweep.json(file.getValue()).getAsJsonObject().getAsJsonObject("layers");
            if (layers.has("humanoid") && !layers.has("humanoid_baby")) {
                findings.put(asset, "dresses a grown-up but has no humanoid_baby layer, so a baby wears it invisibly");
            }
            for (Map.Entry<String, JsonElement> type : layers.entrySet()) {
                for (JsonElement layer : type.getValue().getAsJsonArray()) {
                    // EquipmentClientInfo.Layer#getTextureLocation
                    Identifier texture = Identifier.parse(layer.getAsJsonObject().get("texture").getAsString());
                    if (!Sweep.ours(texture)) continue;
                    textures++;
                    String path = "assets/" + texture.getNamespace() + "/textures/entity/equipment/" + type.getKey()
                            + "/" + texture.getPath() + ".png";
                    Path png = Sweep.modFiles().get(path);
                    if (png == null) {
                        findings.put(asset + " " + type.getKey(), "names " + texture + ", but " + path + " is not there");
                    } else if (type.getKey().equals("humanoid_baby") && !size(png).equals("64x64")) {
                        findings.put(asset + " " + type.getKey(), path + " is " + size(png)
                                + ", where the baby armour model is laid out on 64x64");
                    }
                }
            }
        }
        Sweep.atLeast(context, "equipment assets", assets, 3);
        Sweep.atLeast(context, "equipment textures", textures, 6);
        Sweep.report(context, "every equipment asset that dresses a grown-up dresses a baby too, from textures that are there",
                findings, Map.of());
        context.succeed();
    }

    /** A PNG's width and height, from its IHDR chunk. */
    private static String size(Path png) {
        ByteBuffer header = ByteBuffer.wrap(Sweep.bytes(png), 16, 8);
        return header.getInt() + "x" + header.getInt();
    }
}
