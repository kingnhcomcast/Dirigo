package io.drahlek.dirigo.datagen;

import io.drahlek.dirigo.Constants;
import io.drahlek.dirigo.annotation.Recipe;
import io.drahlek.dirigo.services.Services;
import net.minecraft.data.recipes.FinishedRecipe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Consumer;

public final class RecipeRegistrar {
    private RecipeRegistrar() {
    }

    public static void buildRecipes(String packageName, Consumer<FinishedRecipe> output) {
        invokeRecipes(Services.CLASS_DISCOVERY.getMethodsAnnotatedWith(packageName, Recipe.class), output);
    }

    private static void invokeRecipes(Set<Method> methods, Consumer<FinishedRecipe> output) {
        RecipeContext context = new RecipeContext(output);
        methods.stream()
                .sorted(Comparator.comparing(method -> method.getDeclaringClass().getName() + "#" + method.getName()))
                .forEach(method -> invokeRecipe(method, context));
    }

    private static void invokeRecipe(Method method, RecipeContext context) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw invalidRecipeMethod(method, "must be static");
        }
        if (method.getParameterCount() != 1 || !RecipeContext.class.isAssignableFrom(method.getParameterTypes()[0])) {
            throw invalidRecipeMethod(method, "must accept exactly one RecipeContext parameter");
        }

        try {
            method.setAccessible(true);
            method.invoke(null, context);
            Constants.LOG.info("Generated recipe from {}#{}", method.getDeclaringClass().getName(), method.getName());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access @Recipe method " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Failed to invoke @Recipe method " + method, cause);
        }
    }

    private static IllegalStateException invalidRecipeMethod(Method method, String reason) {
        return new IllegalStateException("@Recipe method " + method + " " + reason);
    }
}