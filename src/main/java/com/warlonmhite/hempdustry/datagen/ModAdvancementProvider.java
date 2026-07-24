package com.warlonmhite.hempdustry.datagen;

import com.warlonmhite.hempdustry.Hempdustry;
import com.warlonmhite.hempdustry.advancement.SmokeCriterion;
import com.warlonmhite.hempdustry.item.ModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
                .display(new AdvancementDisplay(new ItemStack(ModItems.INDICA_SEEDS),
                        Text.literal("High Hopes"), Text.literal("Plant a different kind of seed and see where it grows."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_stem", InventoryChangedCriterion.Conditions.items(ModItems.INDICA_SEEDS))
                .build(consumer, Hempdustry.MOD_ID + ":hempdustry");


        AdvancementEntry indicaStrain = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.INDICA_BUDS),
                        Text.literal("Purple Kush"), Text.literal("Not all buds are built for speed. Indica brings the chill."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_indica_buds", InventoryChangedCriterion.Conditions.items(ModItems.INDICA_BUDS))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":indica_strain");

        // Common child of every strain advancement (Purple Kush now, Lemon Haze etc. later): granted
        // the first time the player takes a hit, regardless of device or strain. Uses the custom
        // SmokeCriterion fired from Smoking.takeHit rather than an inventory check.
        AdvancementEntry firstContact = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.INDICA_SPLIFF),
                        Text.literal("First Contact"), Text.literal("Take your first hit and see the world through a new perspective."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("took_a_hit", SmokeCriterion.Conditions.any())
                .parent(indicaStrain)
                .build(consumer, Hempdustry.MOD_ID + ":first_contact");

        // Hidden easter-egg: take a hit while the in-game clock reads ~4:20 PM. Vanilla day = 24000
        // ticks, tick 0 = 6:00 AM, 1000 ticks/hour → 4:20 PM = 10333. Window ±210 ticks (10123–10543,
        // 420 ticks wide ≈ 21 real seconds) as a nod to the number. Time gate lives in the criterion
        // conditions; hidden = last display flag is true so it stays secret until earned.
        Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(Items.CLOCK),
                        Text.literal("Blaze It!"), Text.literal("You're not the only one lighting up at 4:20 right now."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.CHALLENGE,
                        true,true,true))
                .criterion("smoked_at_420", SmokeCriterion.Conditions.during(10123L, 10543L))
                .parent(firstContact)
                .build(consumer, Hempdustry.MOD_ID + ":blaze_it");


        AdvancementEntry hemprepreneurs = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_STEM),
                        Text.literal("Hemprepreneurs"), Text.literal("Harvest your first hemp stem and discover what the plant is really capable of."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_stem", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_STEM))
                .parent(rootAdvancement)
                .build(consumer, Hempdustry.MOD_ID + ":hemprepreneurs");

        AdvancementEntry hempBuilder = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_BRICK),
                        Text.literal("Hemp Builder"), Text.literal("Craft your first hemp brick and explore the building potential of hemp."),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_brick", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_BRICK))
                .parent(hemprepreneurs)
                .build(consumer, Hempdustry.MOD_ID + ":hemp_builder");

        AdvancementEntry greenThreads = Advancement.Builder.create()
                .display(new AdvancementDisplay(new ItemStack(ModItems.HEMP_FIBER),
                        Text.literal("Green Threads"), Text.literal("Turn hemp into fiber that's your gateway to new stuff"),
                        Optional.of(Identifier.of(Hempdustry.MOD_ID, "textures/block/hempcrete_powder_block.png")), AdvancementFrame.TASK,
                        true,true,false))
                .criterion("has_hemp_fiber", InventoryChangedCriterion.Conditions.items(ModItems.HEMP_FIBER))
                .parent(hemprepreneurs)
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

