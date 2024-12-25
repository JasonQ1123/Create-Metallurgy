package fr.lucreeper74.createmetallurgy.content.industrial_ladle;

import com.google.gson.JsonObject;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import fr.lucreeper74.createmetallurgy.CreateMetallurgy;
import fr.lucreeper74.createmetallurgy.content.foundry_lids.lid.MeltingRecipe;
import fr.lucreeper74.createmetallurgy.content.industrial_ladle.melting_unit.MeltingSlot;
import fr.lucreeper74.createmetallurgy.registries.CMRecipeTypes;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class BulkMeltingRecipe extends ProcessingRecipe<RecipeWrapper> {

    protected int minHeat;

    public BulkMeltingRecipe(ProcessingRecipeBuilder.ProcessingRecipeParams params) {
        super(CMRecipeTypes.BULK_MELTING, params);
        this.minHeat = -50;
    }

    public static boolean match(IndustrialLadleBlockEntity be, MeltingSlot meltingSlot, Recipe<?> recipe) {
        if (recipe instanceof BulkMeltingRecipe bulkRecipe)
            return bulkRecipe.getIngredients().get(0).test(meltingSlot.getStack())
                && be.ladle.getCurrentHeat() >= bulkRecipe.getMinHeat();
        else
            if (recipe instanceof MeltingRecipe meltingRecipe)
                return meltingRecipe.getIngredients().get(0).test(meltingSlot.getStack());
        return false;
    }

    @Override
    public boolean matches(RecipeWrapper inv, Level worldIn) {
        return getIngredients().get(0)
                .test(inv.getItem(0));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Registry.RECIPE_SERIALIZER.get(CreateMetallurgy.genRL("bulk_melting"));
    }

    @Override
    public RecipeType<?> getType() {
        return getTypeInfo().getType();
    }

    public IRecipeTypeInfo getTypeInfo() {
        return CMRecipeTypes.BULK_MELTING;
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    public int getMinHeat() {
        return minHeat;
    }

    @Override
    public void readAdditional(JsonObject json) {
        super.readAdditional(json);
        minHeat = GsonHelper.getAsInt(json, "minHeatRequirement", -50);
    }

    @Override
    public void writeAdditional(JsonObject json) {
        super.writeAdditional(json);
        json.addProperty("minHeatRequirement", minHeat);
    }

    @Override
    public void readAdditional(FriendlyByteBuf buffer) {
        super.readAdditional(buffer);
        minHeat = buffer.readInt();
    }

    @Override
    public void writeAdditional(FriendlyByteBuf buffer) {
        super.writeAdditional(buffer);
        buffer.writeInt(minHeat);
    }
}