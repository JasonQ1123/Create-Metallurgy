package fr.lucreeper74.createmetallurgy.content.industrial_ladle;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.item.SmartInventory;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import fr.lucreeper74.createmetallurgy.content.industrial_ladle.melting_unit.MeltingInventory;
import fr.lucreeper74.createmetallurgy.content.industrial_ladle.melting_unit.MeltingSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import static java.lang.Math.abs;

public class IndustrialLadleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IMultiBlockEntityContainer.Fluid {
    private static final int MAX_SIZE = 5;
    private static final int MAX_HEIGHT = 4;
    private static final int CAPACITY_FACTOR = 1000;

    protected FluidTank tankInventory;
    protected MeltingInventory inputInv;
    protected LazyOptional<IFluidHandler> fluidCapability;
    protected LazyOptional<IItemHandlerModifiable> itemCapability;

    protected boolean forceFluidLevelUpdate;
    protected boolean updateConnectivity;
    protected boolean updateCapability;
    protected boolean window;
    protected int luminosity;
    protected BlockPos controller;
    protected int width;
    protected int height;


    public LadleData ladle;


    private static final int SYNC_RATE = 8;
    protected int syncCooldown;
    protected boolean queuedSync;

    // For rendering purposes only
    private LerpedFloat fluidLevel;

    public IndustrialLadleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tankInventory = createTank();
        inputInv = createInventory();
        fluidCapability = LazyOptional.of(() -> tankInventory);
        itemCapability = LazyOptional.of(() -> inputInv);
        forceFluidLevelUpdate = true;
        updateConnectivity = false;
        updateCapability = false;
        window = true;
        height = 1;
        width = 1;
        ladle = new LadleData();
        refreshCapability();
    }

    protected SmartFluidTank createTank() {
        return new SmartFluidTank(CAPACITY_FACTOR, this::onFluidStackChanged);
    }

    protected MeltingInventory createInventory() {
        return new MeltingInventory(this, getMaxWidth() * getMaxWidth() * getMaxHeight());
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket) {
        super.read(compound, clientPacket);

        BlockPos controllerBefore = controller;
        int prevSize = width;
        int prevHeight = height;
        int prevLum = luminosity;

        luminosity = compound.getInt("Luminosity");
        controller = null;

        if (compound.contains("Controller"))
            controller = NbtUtils.readBlockPos(compound.getCompound("Controller"));

        if (isController()) {
            window = compound.getBoolean("Window");
            width = compound.getInt("Size");
            height = compound.getInt("Height");
            tankInventory.setCapacity(getTotalTankSize() * CAPACITY_FACTOR);
            tankInventory.readFromNBT(compound.getCompound("TankContent"));

            inputInv.setFirstLimitedSlot(getTotalTankSize());
            inputInv.deserializeNBT(compound.getCompound("MeltingInv"));


            if (tankInventory.getSpace() < 0)
                tankInventory.drain(-tankInventory.getSpace(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (luminosity != prevLum && hasLevel())
            level.getChunkSource()
                    .getLightEngine()
                    .checkBlock(worldPosition);

        ladle.read(compound.getCompound("Ladle"));

        if (compound.contains("ForceFluidLevel") || fluidLevel == null)
            fluidLevel = LerpedFloat.linear()
                    .startWithValue(getFillState());

        updateCapability = true;

        if (!clientPacket)
            return;

        boolean changeOfController = !Objects.equals(controllerBefore, controller);
        if (changeOfController || prevSize != width || prevHeight != height) {
            if (hasLevel())
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
            if (isController()) {
                tankInventory.setCapacity(CAPACITY_FACTOR * getTotalTankSize());
                inputInv.setFirstLimitedSlot(getTotalTankSize());

            }
            invalidateRenderBoundingBox();
        }
        if (isController()) {
            float fillState = getFillState();
            if (compound.contains("ForceFluidLevel") || fluidLevel == null)
                fluidLevel = LerpedFloat.linear()
                        .startWithValue(fillState);
            fluidLevel.chase(fillState, .5f, LerpedFloat.Chaser.EXP);
        }
        if (compound.contains("LazySync"))
            fluidLevel.chase(fluidLevel.getChaseTarget(), .125f, LerpedFloat.Chaser.EXP);
    }

    @Override
    public void write(CompoundTag compound, boolean clientPacket) {
        compound.put("Ladle", ladle.write());

        if (isController()) {
            compound.putBoolean("Window", window);
            compound.put("TankContent", tankInventory.writeToNBT(new CompoundTag()));
            compound.put("MeltingInv", inputInv.serializeNBT());
            compound.putInt("Size", width);
            compound.putInt("Height", height);
        } else {
            compound.put("Controller", NbtUtils.writeBlockPos(controller));
        }
        compound.putInt("Luminosity", luminosity);
        super.write(compound, clientPacket);

        if (!clientPacket)
            return;
        if (forceFluidLevelUpdate)
            compound.putBoolean("ForceFluidLevel", true);
        if (queuedSync)
            compound.putBoolean("LazySync", true);
        forceFluidLevelUpdate = false;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return itemCapability.cast();
        if (!fluidCapability.isPresent())
            refreshCapability();
        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)
            return fluidCapability.cast();
        return super.getCapability(cap, side);
    }

    public float getFillState() {
        return (float) tankInventory.getFluidAmount() / tankInventory.getCapacity();
    }

    protected void updateConnectivity() {
        updateConnectivity = false;
        if (level.isClientSide)
            return;
        if (!isController())
            return;
        ConnectivityHandler.formMulti(this);
    }

    @Override
    public void tick() {
        super.tick();
        if (syncCooldown > 0) {
            syncCooldown--;
            if (syncCooldown == 0 && queuedSync)
                sendData();
        }

        if (updateCapability) {
            updateCapability = false;
            refreshCapability();
        }
        if (updateConnectivity)
            updateConnectivity();
        if (fluidLevel != null)
            fluidLevel.tickChaser();

        // Melting Slot recipe
        if (!level.isClientSide() && isController()) {
            ladle.tick(this);

            for (int slot = 0; slot < getTotalTankSize(); slot++) {
                MeltingSlot meltingSlot = inputInv.getSlot(slot);
                if (meltingSlot.canMelt())
                    meltingSlot.heatItem();
                else
                    meltingSlot.coolItem();
            }
        }
    }

    @Override
    public void sendData() {
        if (syncCooldown > 0) {
            queuedSync = true;
            return;
        }
        super.sendData();
        queuedSync = false;
        syncCooldown = SYNC_RATE;
    }

    public void setWindows(boolean window) {
        this.window = window;
        for (int yOffset = 0; yOffset < height; yOffset++) {
            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {

                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    BlockState blockState = level.getBlockState(pos);
                    if (!IndustrialLadleBlock.isLadle(blockState))
                        continue;

                    FluidTankBlock.Shape shape = FluidTankBlock.Shape.PLAIN;
                    if (window) {
                        // SIZE 1: Every tank has a window
                        if (width == 1)
                            shape = FluidTankBlock.Shape.WINDOW;
                        // SIZE 2: Every tank has a corner window
                        if (width == 2)
                            shape = xOffset == 0 ? zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SW
                                    : zOffset == 0 ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SE;
                        // SIZE 3: Tanks in the center have a window
                        if (width == 3 && abs(abs(xOffset) - abs(zOffset)) == 1)
                            shape = FluidTankBlock.Shape.WINDOW;
                    }

                    level.setBlock(pos, blockState.setValue(FluidTankBlock.SHAPE, shape), 22);
                    level.getChunkSource()
                            .getLightEngine()
                            .checkBlock(pos);
                }
            }
        }
    }

    private void refreshCapability() {
        LazyOptional<IFluidHandler> oldFCap = fluidCapability;
        fluidCapability = LazyOptional.of(this::handlerForFluidCapability);
        oldFCap.invalidate();

        LazyOptional<IItemHandlerModifiable> oldICap = itemCapability;
        itemCapability = LazyOptional.of(this::handlerForItemCapability);
        oldICap.invalidate();
    }

    private IFluidHandler handlerForFluidCapability() {
        return isController() ? tankInventory
                : getControllerBE() != null ? getControllerBE().handlerForFluidCapability() : new FluidTank(0);
    }

    private IItemHandlerModifiable handlerForItemCapability() {
        return isController() ? inputInv
                : getControllerBE() != null ? getControllerBE().handlerForItemCapability() : new SmartInventory(0, this, 0, false);
    }

    @Override
    public BlockPos getController() {
        return isController() ? worldPosition : controller;
    }

    protected void onFluidStackChanged(FluidStack newFluidStack) {
        if (!hasLevel())
            return;

        FluidType attributes = newFluidStack.getFluid()
                .getFluidType();
        int luminosity = (int) (attributes.getLightLevel(newFluidStack) / 1.2f);
        boolean reversed = attributes.isLighterThanAir();
        int maxY = (int) ((getFillState() * height) + 1);

        for (int yOffset = 0; yOffset < height; yOffset++) {
            boolean isBright = reversed ? (height - yOffset <= maxY) : (yOffset < maxY);
            int actualLuminosity = isBright ? luminosity : luminosity > 0 ? 1 : 0;

            for (int xOffset = 0; xOffset < width; xOffset++) {
                for (int zOffset = 0; zOffset < width; zOffset++) {
                    BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
                    IndustrialLadleBlockEntity tankAt = ConnectivityHandler.partAt(getType(), level, pos);
                    if (tankAt == null)
                        continue;
                    level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState()
                            .getBlock());
                    if (tankAt.luminosity == actualLuminosity)
                        continue;
                    tankAt.setLuminosity(actualLuminosity);
                }
            }
        }

        if (!level.isClientSide) {
            setChanged();
            sendData();
        }

        if (isVirtual()) {
            if (fluidLevel == null)
                fluidLevel = LerpedFloat.linear()
                        .startWithValue(getFillState());
            fluidLevel.chase(getFillState(), .5f, LerpedFloat.Chaser.EXP);
        }
    }

    protected void setLuminosity(int luminosity) {
        if (level.isClientSide)
            return;
        if (this.luminosity == luminosity)
            return;
        this.luminosity = luminosity;
        sendData();
    }

    @SuppressWarnings("unchecked")
    @Override
    public IndustrialLadleBlockEntity getControllerBE() {
        if (isController())
            return this;
        BlockEntity blockEntity = level.getBlockEntity(controller);
        if (blockEntity instanceof IndustrialLadleBlockEntity)
            return (IndustrialLadleBlockEntity) blockEntity;
        return null;
    }

    public void applyFluidTankSize(int blocks) {
        tankInventory.setCapacity(blocks * CAPACITY_FACTOR);
        inputInv.setFirstLimitedSlot(blocks);
        int overflow = tankInventory.getFluidAmount() - tankInventory.getCapacity();
        if (overflow > 0)
            tankInventory.drain(overflow, IFluidHandler.FluidAction.EXECUTE);
        forceFluidLevelUpdate = true;
    }


    @Override
    public boolean isController() {
        return controller == null || worldPosition.getX() == controller.getX()
                && worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
    }

    public void updateLadleState() {
        if (!isController())
            return;

        ladle.setControlled(!ladle.isControlled());

        if (ladle.isControlled())
            setWindows(false);

        for (int yOffset = 0; yOffset < height; yOffset++)
            for (int xOffset = 0; xOffset < width; xOffset++)
                for (int zOffset = 0; zOffset < width; zOffset++)
                    if (level.getBlockEntity(
                            worldPosition.offset(xOffset, yOffset, zOffset)) instanceof IndustrialLadleBlockEntity LaddleBE)
                        setWindows(true);

        notifyUpdate();
    }

    @Override
    public void setController(BlockPos controller) {
        if (level.isClientSide && !isVirtual())
            return;
        if (controller.equals(this.controller))
            return;
        this.controller = controller;
        refreshCapability();
        setChanged();
        sendData();
    }

    @Override
    public void removeController(boolean keepContents) {
        if (level.isClientSide)
            return;
        updateConnectivity = true;
        controller = null;
        width = 1;
        height = 1;

        BlockState state = getBlockState();
        if (IndustrialLadleBlock.isLadle(state)) {
            state = state.setValue(FluidTankBlock.BOTTOM, true);
            state = state.setValue(FluidTankBlock.TOP, true);
            state = state.setValue(FluidTankBlock.SHAPE, window ? FluidTankBlock.Shape.WINDOW : FluidTankBlock.Shape.PLAIN);
            getLevel().setBlock(worldPosition, state, 22);
        }
        setChanged();
        sendData();
    }

    public void toggleWindows() {
        IndustrialLadleBlockEntity be = getControllerBE();
        if (be == null)
            return;
        if (be.ladle.isControlled())
            return;
        be.setWindows(!be.window);
    }

    @Override
    public BlockPos getLastKnownPos() {
        return null;
    }

    @Override
    public void preventConnectivityUpdate() {
        updateConnectivity = false;
    }

    @Override
    public void notifyMultiUpdated() {
        BlockState state = this.getBlockState();
        if (IndustrialLadleBlock.isLadle(state)) { // safety
            state = state.setValue(IndustrialLadleBlock.BOTTOM, getController().getY() == getBlockPos().getY());
            state = state.setValue(IndustrialLadleBlock.TOP, getController().getY() + height - 1 == getBlockPos().getY());
            level.setBlock(getBlockPos(), state, 6);
        }
        if (isController())
            setWindows(window);
        updateLadleState();
        setChanged();
    }

    @Override
    public Direction.Axis getMainConnectionAxis() {
        return Direction.Axis.Y;
    }

    @Override
    public int getMaxLength(Direction.Axis longAxis, int width) {
        if (longAxis == Direction.Axis.Y)
            return getMaxHeight();
        return getMaxWidth();
    }

    @Override
    public int getMaxWidth() {
        return MAX_SIZE;
    }


    public int getMaxHeight() {
        return MAX_HEIGHT;
    }

    public LerpedFloat getFluidLevel() {
        return fluidLevel;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public void setTankSize(int tank, int blocks) {
        applyFluidTankSize(blocks);
    }

    @Override
    public boolean hasTank() {
        return true;
    }

    @Override
    public IFluidTank getTank(int tank) {
        return tankInventory;
    }

    public int getTotalTankSize() {
        return width * width * height;
    }

    @Override
    public FluidStack getFluid(int tank) {
        return tankInventory.getFluid()
                .copy();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        IndustrialLadleBlockEntity controllerBE = getControllerBE();
        if (controllerBE == null)
            return false;
        if (controllerBE.ladle.addToGoggleTooltip(tooltip, isPlayerSneaking, controllerBE.getTotalTankSize()))
            return true;
        return containedFluidTooltip(tooltip, isPlayerSneaking,
                controllerBE.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY));
    }
}
