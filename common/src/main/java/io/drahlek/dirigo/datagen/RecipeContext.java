package io.drahlek.dirigo.datagen;

import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

public record RecipeContext(HolderLookup.Provider registries, RecipeOutput output) {

    public HolderGetter<Item> items() {
        return registries.lookupOrThrow(Registries.ITEM);
    }

    public Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }

    public Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> tag) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(ItemPredicate.Builder.item().of(tag));
    }

    public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result) {
        return ShapedRecipeBuilder.shaped(category, result);
    }

    public ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count) {
        return ShapedRecipeBuilder.shaped(category, result, count);
    }

    public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result) {
        return ShapelessRecipeBuilder.shapeless(category, result);
    }

    public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int count) {
        return ShapelessRecipeBuilder.shapeless(category, result, count);
    }
}
