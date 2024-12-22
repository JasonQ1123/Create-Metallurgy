package fr.lucreeper74.createmetallurgy.content.industrial_ladle.recipe;

import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import fr.lucreeper74.createmetallurgy.CreateMetallurgy;
import fr.lucreeper74.createmetallurgy.content.foundry_lids.lid.MeltingRecipe;
import fr.lucreeper74.createmetallurgy.content.industrial_ladle.IndustrialLadleBlockEntity;
import fr.lucreeper74.createmetallurgy.content.industrial_ladle.melting_unit.MeltingSlot;
import fr.lucreeper74.createmetallurgy.registries.CMRecipeTypes;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.wrapper.RecipeWrapper;

public class BulkMeltingRecipe implements Recipe<RecipeWrapper> {

    protected final ResourceLocation id;
    protected Ingredient ingredient;
    protected FluidStack fluidResult;
    protected int processingDuration;
    protected int minHeat;

    public BulkMeltingRecipe(ResourceLocation id) {
        this.id = id;
        this.ingredient = Ingredient.EMPTY;
        this.processingDuration = 0;
        this.minHeat = 0;
        this.fluidResult = FluidStack.EMPTY;
    }

    public static boolean match(IndustrialLadleBlockEntity be, MeltingSlot meltingSlot, Recipe<?> recipe) {
        if (recipe instanceof BulkMeltingRecipe bulkRecipe)
            return bulkRecipe.getIngredient().test(meltingSlot.getStack())
                   && be.ladle.getCurrentHeat() >= bulkRecipe.getMinHeat();
        else
            if (recipe instanceof MeltingRecipe meltingRecipe)
                return meltingRecipe.getIngredients().get(0).test(meltingSlot.getStack());

        return false;
    }

    @Override
    public boolean matches(RecipeWrapper inv, Level worldIn) {
        return getIngredient()
                .test(inv.getItem(0));
    }

    @Override
    public ItemStack assemble(RecipeWrapper pContainer) {
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public ItemStack getResultItem() {
        return null;
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

    public FluidStack getFluidResult() {
        return fluidResult;
    }

    public int getProcessingDuration() {
        return processingDuration;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public int getMinHeat() {
        return minHeat;
    }
}