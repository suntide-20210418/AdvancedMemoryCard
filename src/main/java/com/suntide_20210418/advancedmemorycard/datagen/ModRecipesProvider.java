package com.suntide_20210418.advancedmemorycard.datagen;

import appeng.core.definitions.AEItems;
import com.suntide_20210418.advancedmemorycard.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ModRecipesProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipesProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> writer) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.ADVANCED_MEMORY_CARD.get())
                .requires(AEItems.MEMORY_CARD)
                .requires(AEItems.LOGIC_PROCESSOR)
                .requires(AEItems.CALCULATION_PROCESSOR)
                .requires(AEItems.ENGINEERING_PROCESSOR)
                .unlockedBy(getHasName(AEItems.MEMORY_CARD), has(AEItems.MEMORY_CARD))
                .save(writer);
    }
}
