package fr.lucreeper74.createmetallurgy.content.industrial_ladle.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public class BulkMeltingRecipeSerializer implements RecipeSerializer<BulkMeltingRecipe> {

    protected void writeToJson(JsonObject json, BulkMeltingRecipe recipe) {
        JsonArray jsonIngredients = new JsonArray();

        Ingredient ingredient = recipe.ingredient;
        if(!ingredient.isEmpty())
            jsonIngredients.add(ingredient.toJson());

        json.add("ingredients", jsonIngredients);
        json.add("result", FluidHelper.serializeFluidStack(recipe.getFluidResult()));

        int processingDuration = recipe.getProcessingDuration();
        if (processingDuration > 0)
            json.addProperty("processingTime", processingDuration);

    }

    @Override
    public BulkMeltingRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        BulkMeltingRecipe recipe = createRecipe(recipeId);

        for (JsonElement je : GsonHelper.getAsJsonArray(json, "ingredients")) {
            recipe.ingredient = Ingredient.fromJson(je);
        }

        recipe.processingDuration = GsonHelper.getAsInt(json, "processingTime");
        recipe.minHeat = GsonHelper.getAsInt(json, "minHeatRequirement");

        JsonObject je =  GsonHelper.getAsJsonObject(json, "result");
        if(je.isJsonObject() && !GsonHelper.isValidNode(je, "fluid"))
            recipe.fluidResult = FluidHelper.deserializeFluidStack(je);

        return recipe;
    }

    @Override
    public @Nullable BulkMeltingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        BulkMeltingRecipe recipe = createRecipe(recipeId);
        recipe.ingredient = Ingredient.fromNetwork(buffer);
        recipe.processingDuration = buffer.readInt();
        recipe.minHeat = buffer.readInt();
        recipe.fluidResult = FluidStack.readFromPacket(buffer);

        return recipe;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, BulkMeltingRecipe recipe) {
        recipe.ingredient.toNetwork(buffer);
        buffer.writeInt(recipe.processingDuration);
        buffer.writeInt(recipe.minHeat);
        recipe.fluidResult.writeToPacket(buffer);
    }

    public final void write(JsonObject json, BulkMeltingRecipe recipe) {
        writeToJson(json, recipe);
    }

    public BulkMeltingRecipe createRecipe(ResourceLocation id) {
        return new BulkMeltingRecipe(id);
    }
}