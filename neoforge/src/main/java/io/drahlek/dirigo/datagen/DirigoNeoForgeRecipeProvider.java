package io.drahlek.dirigo.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

public class DirigoNeoForgeRecipeProvider extends RecipeProvider {
    private final String packageName;
    private final CompletableFuture<HolderLookup.Provider> registriesFuture;

    public DirigoNeoForgeRecipeProvider(String packageName, PackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
        this.packageName = packageName;
        this.registriesFuture = registriesFuture;
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        RecipeRegistrar.buildRecipes(packageName, registriesFuture.join(), output);
    }
}
