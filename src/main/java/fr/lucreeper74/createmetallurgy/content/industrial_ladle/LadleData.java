package fr.lucreeper74.createmetallurgy.content.industrial_ladle;

import com.simibubi.create.content.fluids.tank.BoilerHeaters;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

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

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking, int boilerSize) {
        if (!isControlled())
            return false;

        Lang.builder().add(Lang.number(getCurrentHeat())
                        .style(ChatFormatting.GOLD)).forGoggles(tooltip);

        tooltip.add(Components.immutableEmpty());

        Lang.translate("tooltip.capacityProvided")
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);

        return true;
    }
}
