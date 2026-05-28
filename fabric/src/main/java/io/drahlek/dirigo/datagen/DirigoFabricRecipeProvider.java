package io.drahlek.dirigo.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

public class DirigoFabricRecipeProvider extends FabricRecipeProvider {
    private final String packageName;

    public DirigoFabricRecipeProvider(String packageName, FabricDataOutput output) {
        super(output);
        this.packageName = packageName;
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> output) {
        RecipeRegistrar.buildRecipes(packageName, output);
    }
}