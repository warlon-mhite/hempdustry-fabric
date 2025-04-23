package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModAdvancementProvider extends FabricAdvancementProvider {

    public ModAdvancementProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(output, registryLookup);
    }

    @Override
    public void generateAdvancement(RegistryWrapper.WrapperLookup registryLookup, Consumer<AdvancementEntry> consumer) {

        AdvancementEntry rootAdvancement = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_STEM),
                        Text.literal("Hemprepreneurs"), Text.literal("Harvest your first hemp stem and discover what the plant is really capable of."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_stem", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_STEM))
                .build(consumer, Hempdustry.MOD_ID + ":hempdustry");

        AdvancementEntry hempBuilder = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_BRICK),
                        Text.literal("Hemp Builder"), Text.literal("Craft your first hemp brick and explore the building potential of hemp."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_brick", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_BRICK))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":hemp_builder");

        AdvancementEntry greenThreads = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_FIBER),
                        Text.literal("Green Threads"), Text.literal("Turn hemp into fiber—your gateway to new stuff"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_fiber", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_FIBER))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":green_threads");

        AdvancementEntry chillSet = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_BEANNIE),
                        Text.literal("The Chill Set"), Text.literal("You’re geared up in 100% plant-based protection... fashion."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("full_hemp_armor", InventoryChangedCriterion.Conditions.items(
                        ModItems.HEMP_BEANNIE,
                        ModItems.HEMP_SHIRT,
                        ModItems.HEMP_HAREM_PANTS,
                        ModItems.FLIP_FLOPS
                ))
                .parent(greenThreads)
                .build(consumer, Hempdustry.MOD_ID + ":chill_set");
    }
}

