package fr.lucreeper74.createmetallurgy.content.industrial_ladle.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.data.SimpleDatagenIngredient;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.tterrag.registrate.util.DataIngredient;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.ModLoadedCondition;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class BulkMeltingRecipeBuilder {

    private BulkMeltingRecipe recipe;
    protected List<ICondition> recipeConditions;

    public BulkMeltingRecipeBuilder(ResourceLocation id) {
        recipe = new BulkMeltingRecipe(id);
        recipeConditions = new ArrayList<>();
    }

    // For Inputs
    public BulkMeltingRecipeBuilder require(TagKey<Item> tag) {
        return require(Ingredient.of(tag));
    }

    public BulkMeltingRecipeBuilder require(ItemLike item) {
        return require(Ingredient.of(item));
    }

    public BulkMeltingRecipeBuilder require(Ingredient ingredient) {
        recipe.ingredient = ingredient;
        return this;
    }

    public BulkMeltingRecipeBuilder require(Mods mod, String id) {
        recipe.ingredient = new SimpleDatagenIngredient(mod, id);
        return this;
    }

    public BulkMeltingRecipeBuilder require(ResourceLocation ingredient) {
        recipe.ingredient = DataIngredient.ingredient(null, ingredient);
        return this;
    }

    // For Output FLuid
    public BulkMeltingRecipeBuilder output(FluidStack fluid) {
        recipe.fluidResult = fluid;
        return this;
    }

    // Others
    public BulkMeltingRecipeBuilder duration(int ticks) {
        recipe.processingDuration = ticks;
        return this;
    }

    public BulkMeltingRecipeBuilder minHeatRequirement(int heat) {
        recipe.minHeat = heat;
        return this;
    }

    public BulkMeltingRecipeBuilder whenModLoaded(String modid) {
        return withCondition(new ModLoadedCondition(modid));
    }

    public BulkMeltingRecipeBuilder withCondition(ICondition condition) {
        recipeConditions.add(condition);
        return this;
    }

    // Build Datagen
    public BulkMeltingRecipe build() {
        return recipe;
    }

    public void build(Consumer<FinishedRecipe> consumer) {
        consumer.accept(new DataGenResult(build(), recipeConditions));
    }

    public static class DataGenResult implements FinishedRecipe {

        private BulkMeltingRecipe recipe;
        private List<ICondition> recipeConditions;
        private ResourceLocation id;
        private BulkMeltingRecipeSerializer serializer;

        public DataGenResult(BulkMeltingRecipe recipe, List<ICondition> recipeConditions) {
            this.recipe = recipe;
            this.recipeConditions = recipeConditions;
            this.id = new ResourceLocation(recipe.getId().getNamespace(),
                    this.recipe.getTypeInfo().getId().getPath() + "/" + recipe.getId().getPath());
            this.serializer = (BulkMeltingRecipeSerializer) recipe.getSerializer();
        }

        @Override
        public void serializeRecipeData(JsonObject json) {
            serializer.write(json, recipe);
            if (recipeConditions.isEmpty())
                return;

            JsonArray conds = new JsonArray();
            recipeConditions.forEach(c -> conds.add(CraftingHelper.serialize(c)));
            json.add("conditions", conds);
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return serializer;
        }

        @Override
        public JsonObject serializeAdvancement() {
            return null;
        }

        @Override
        public ResourceLocation getAdvancementId() {
            return null;
        }

    }
}