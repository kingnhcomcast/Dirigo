package io.drahlek.dirigo.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

import java.util.concurrent.CompletableFuture;

public class DirigoFabricRecipeProvider extends FabricRecipeProvider {
    private final String packageName;
    private final CompletableFuture<HolderLookup.Provider> registriesFuture;

    public DirigoFabricRecipeProvider(String packageName, FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
        this.packageName = packageName;
        this.registriesFuture = registriesFuture;
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        RecipeRegistrar.buildRecipes(packageName, registriesFuture.join(), output);
    }

    @Override
    public String getName() {
        return "Dirigo annotated recipes";
    }
}
