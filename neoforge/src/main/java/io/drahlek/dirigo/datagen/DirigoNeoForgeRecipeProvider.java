package io.drahlek.dirigo.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DirigoNeoForgeRecipeProvider implements DataProvider {
    private final String packageName;
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registriesFuture;

    public DirigoNeoForgeRecipeProvider(String packageName, PackOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        this.packageName = packageName;
        this.output = output;
        this.registriesFuture = registriesFuture;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        return registriesFuture.thenCompose(registries -> {
            Set<Identifier> generatedRecipes = new HashSet<>();
            List<CompletableFuture<?>> futures = new ArrayList<>();
            RecipeOutput recipeOutput = new RecipeOutput() {
                @Override
                public void accept(
                        ResourceKey<Recipe<?>> recipeKey,
                        Recipe<?> recipe,
                        @Nullable AdvancementHolder advancement
                ) {
                    Identifier identifier = recipeKey.identifier();
                    if (!generatedRecipes.add(identifier)) {
                        throw new IllegalStateException("Duplicate recipe " + identifier);
                    }

                    RegistryOps<JsonElement> registryOps = registries.createSerializationContext(JsonOps.INSTANCE);
                    JsonObject recipeJson = Recipe.CODEC.encodeStart(registryOps, recipe)
                            .getOrThrow(IllegalStateException::new)
                            .getAsJsonObject();

                    PackOutput.PathProvider recipesPathResolver = output.createRegistryElementsPathProvider(Registries.RECIPE);
                    PackOutput.PathProvider advancementsPathResolver = output.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
                    futures.add(DataProvider.saveStable(cachedOutput, recipeJson, recipesPathResolver.json(identifier)));

                    if (advancement != null) {
                        JsonObject advancementJson = Advancement.CODEC.encodeStart(registryOps, advancement.value())
                                .getOrThrow(IllegalStateException::new)
                                .getAsJsonObject();
                        futures.add(DataProvider.saveStable(cachedOutput, advancementJson, advancementsPathResolver.json(advancement.id())));
                    }
                }

                @Override
                public void accept(
                        ResourceKey<Recipe<?>> recipeKey,
                        Recipe<?> recipe,
                        @Nullable AdvancementHolder advancement,
                        ICondition... conditions
                ) {
                    accept(recipeKey, recipe, advancement);
                }

                @Override
                public Advancement.Builder advancement() {
                    return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                }

                @Override
                public void includeRootAdvancement() {
                }
            };

            new AnnotatedRecipeProvider(packageName, registries, recipeOutput).buildAnnotatedRecipes();
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    private static final class AnnotatedRecipeProvider extends RecipeProvider {
        private final String packageName;

        private AnnotatedRecipeProvider(String packageName, HolderLookup.Provider registries, RecipeOutput output) {
            super(registries, output);
            this.packageName = packageName;
        }

        private void buildAnnotatedRecipes() {
            buildRecipes();
        }

        @Override
        public void buildRecipes() {
            RecipeRegistrar.buildRecipes(packageName, registries, output);
        }
    }

    @Override
    public String getName() {
        return "Dirigo annotated recipes";
    }
}
