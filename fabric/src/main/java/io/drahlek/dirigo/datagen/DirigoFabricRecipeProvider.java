package io.drahlek.dirigo.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.concurrent.CompletableFuture;

public class DirigoFabricRecipeProvider extends FabricRecipeProvider {
    private final String packageName;

    public DirigoFabricRecipeProvider(String packageName, FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
        this.packageName = packageName;
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
            @Override
            public void buildRecipes() {
                RecipeRegistrar.buildRecipes(packageName, registries, output);
            }
        };
    }

    @Override
    public String getName() {
        return "Dirigo annotated recipes";
    }
}
