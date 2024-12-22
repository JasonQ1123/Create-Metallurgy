package fr.lucreeper74.createmetallurgy.content.industrial_ladle;

import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class LadleData {

    boolean controlled;
    int currentheat;

    public void tick(IndustrialLadleBlockEntity be) {
        if (updateTemperature(be))
            be.notifyUpdate();
    }

    public boolean updateTemperature(IndustrialLadleBlockEntity be) {
        BlockPos controllerPos = be.getBlockPos();
        Level level = be.getLevel();
        //needsHeatLevelUpdate = false;

        int prevActive = currentheat;
        currentheat = 0;

        for (int xOffset = 0; xOffset < be.getWidth(); xOffset++) {
            for (int zOffset = 0; zOffset < be.getWidth(); zOffset++) {
                BlockPos pos = controllerPos.offset(xOffset, -1, zOffset);
                BlockState blockState = level.getBlockState(pos);
                float heat = BoilerHeaters.getActiveHeat(level, pos, blockState);
                currentheat += heat;
            }
        }
        return prevActive != currentheat;
    }


    public CompoundTag write() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("Controlled", controlled);
        nbt.putInt("currentHeat", currentheat);
        return nbt;
    }

    public void read(CompoundTag nbt) {
        controlled = nbt.getBoolean("Controlled");
        currentheat = nbt.getInt("currentHeat");
    }

    public boolean isControlled() {
        return controlled;
    }

    public int getCurrentHeat() {
        return currentheat;
    }

    public void setControlled(boolean active) {
        this.controlled = active;
    }

    public LadleData.LadleFluidHandler createHandler() {
        return new LadleData.LadleFluidHandler();
    }

    public class LadleFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 10000;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return FluidHelper.isWater(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource))
                return 0;
            int amount = resource.getAmount();
//            if (action.execute())
//                gatheredSupply += amount;
            return amount;
        }
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }

    }
}
