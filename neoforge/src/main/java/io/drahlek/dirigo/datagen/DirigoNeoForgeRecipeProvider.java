package io.drahlek.dirigo.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.function.Consumer;

public class DirigoNeoForgeRecipeProvider extends RecipeProvider {
    private final String packageName;

    public DirigoNeoForgeRecipeProvider(String packageName, PackOutput output) {
        super(output);
        this.packageName = packageName;
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> output) {
        RecipeRegistrar.buildRecipes(packageName, output);
    }
}