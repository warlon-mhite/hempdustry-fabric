package com.warlonmhite.hempdustry.forge.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.warlonmhite.hempdustry.forge.ForgeEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forge 36 fires its loot-table event only for a table read from vanilla's own pack: any other file
 * counts as "custom" (its source must be named {@code Default}), and the event is skipped. Forge's own
 * jar ships {@code tall_grass} and {@code large_fern}, so on 1.16.5 the grass's seed pool was never
 * added, and no table a mod's jar replaces would get the mod's loot either. The newer lines' loaders
 * fire for every table.
 *
 * <p>So the tables Forge skips are handed to the mod's own listener here, just before Forge freezes
 * them, with the same answer to "is this the game's or a mod's table" the event path gets
 * ({@code LootTableOrigin}), which leaves a datapack's tables alone. Forge's event and other mods'
 * listeners are not touched: they see exactly what they saw before.
 */
@Mixin(value = ForgeHooks.class, remap = false)
public abstract class ForgeHooksLootMixin {

    private static final ThreadLocal<ResourceLocation> hempdustry$skipped = new ThreadLocal<>();

    @Inject(method = "loadLootTable", at = @At("HEAD"))
    private static void hempdustry$noteSkipped(Gson gson, ResourceLocation name, JsonElement data, boolean custom,
                                               LootTables tables, CallbackInfoReturnable<LootTable> cir) {
        hempdustry$skipped.set(custom ? name : null);
    }

    @ModifyVariable(method = "loadLootTable", at = @At(value = "INVOKE", target = "freeze"))
    private static LootTable hempdustry$addToSkipped(LootTable table) {
        ResourceLocation name = hempdustry$skipped.get();
        hempdustry$skipped.remove();
        if (name != null) {
            ForgeEvents.loadedWithoutEvent(name, table);
        }
        return table;
    }
}
