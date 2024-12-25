package fr.lucreeper74.createmetallurgy.content.industrial_ladle.melting_unit;

import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.foundation.recipe.RecipeConditions;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import fr.lucreeper74.createmetallurgy.content.industrial_ladle.BulkMeltingRecipe;
import fr.lucreeper74.createmetallurgy.content.industrial_ladle.IndustrialLadleBlockEntity;
import fr.lucreeper74.createmetallurgy.registries.CMRecipeTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.List;
import java.util.function.Predicate;

public class MeltingSlot implements ContainerData {

    private ItemStack stack;
    private final IndustrialLadleBlockEntity be;
    private ProcessingRecipe<?> currentRecipe;
    public int processingTime;

    public MeltingSlot(IndustrialLadleBlockEntity be) {
        this.be = be;
        processingTime = 0;
        stack = ItemStack.EMPTY;
    }

    public ItemStack getStack() {
        return stack;
    }

    private void reset() {
        processingTime = 0;
        currentRecipe = null;
    }

    private void startRecipe() {
        ProcessingRecipe<?> recipe = getMatchingRecipes();
        if (recipe != null) {
            processingTime = recipe.getProcessingDuration();
            currentRecipe = recipe;
        }
    }

    public void setStack(ItemStack newStack) {
        if (stack.isEmpty())
            reset();

        this.stack = newStack;
        startRecipe();
        be.notifyUpdate();
    }

    public boolean canMelt() {
        if (processingTime > 0) {
            if (stack.isEmpty()) {
                reset();
                return false;
            }
            return true;
        }
        return false;
    }

    public void heatItem() {
        processingTime--;

        if (processingTime <= 0)
            tryMeltItem();
    }

    public void coolItem() {
        if (currentRecipe != null)
            if (processingTime < currentRecipe.getProcessingDuration())
                processingTime++;
    }

    public void tryMeltItem() {
        if (currentRecipe == null)
            return;

        IFluidHandler fluidHandler = be.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
                .orElse(null);

        FluidStack output = currentRecipe.getFluidResults().get(0);
        if (fluidHandler.fill(output.copy(), IFluidHandler.FluidAction.SIMULATE) >= output.getAmount()) {
            fluidHandler.fill(output.copy(), IFluidHandler.FluidAction.EXECUTE);
            setStack(ItemStack.EMPTY);
            reset();
        }
    }

    private ProcessingRecipe<?> getMatchingRecipes() {
        Level level = be.getLevel();
        if (level == null)
            return null;

        Predicate<Recipe<?>> type = RecipeConditions.isOfType(CMRecipeTypes.BULK_MELTING.getType(), CMRecipeTypes.MELTING.getType());
        List<Recipe<?>> recipes = RecipeFinder.get(BulkMeltingCacheKey, level, type).stream()
                .filter(this::matchCastingRecipe)
                .sorted((r1, r2) -> r2.getIngredients()
                        .size()
                        - r1.getIngredients()
                        .size())
                .toList();
        if (!recipes.isEmpty())
            return (ProcessingRecipe<?>) recipes.get(0);
        return null;
    }

    protected <C extends Container> boolean matchCastingRecipe(Recipe<C> recipe) {
        if (recipe == null)
            return false;
        return BulkMeltingRecipe.match(be, this, recipe);
    }

    @Override
    public int get(int pIndex) {
        return 0;
    }

    @Override
    public void set(int pIndex, int pValue) {
    }

    @Override
    public int getCount() {
        return 0;
    }

    public void deserializeNBT(CompoundTag nbt) {
        stack = ItemStack.of(nbt);
        processingTime = nbt.getInt("processingTime");
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        stack.save(nbt);
        nbt.putInt("processingTime", processingTime);
        return nbt;
    }

    private static final Object BulkMeltingCacheKey = new Object();
}
