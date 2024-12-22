package fr.lucreeper74.createmetallurgy.content.industrial_ladle;

import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.fluid.FluidRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.animation.LerpedFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class IndustrialLadleRenderer extends SafeBlockEntityRenderer<IndustrialLadleBlockEntity> {
    public IndustrialLadleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    protected void renderSafe(IndustrialLadleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                              int light, int overlay) {
        if (!be.isController())
            return;
        if (be.ladle.isControlled()) {
            renderAsController(be, partialTicks, ms, buffer, light, overlay);
        }

        if (!be.window)
            return;

        renderItems(be, partialTicks, ms, buffer, light, overlay);

        LerpedFloat fluidLevel = be.getFluidLevel();
        if (fluidLevel == null)
            return;

        float capHeight = 1 / 4f;
        float tankHullWidth = 1 / 16f + 1 / 128f;
        float minPuddleHeight = 1 / 16f;
        float totalHeight = be.height - 2 * capHeight - minPuddleHeight;

        float level = fluidLevel.getValue(partialTicks);
        if (level < 1 / (512f * totalHeight))
            return;
        float clampedLevel = Mth.clamp(level * totalHeight, 0, totalHeight);

        FluidTank tank = be.tankInventory;
        FluidStack fluidStack = tank.getFluid();

        if (fluidStack.isEmpty())
            return;

        boolean top = fluidStack.getFluid()
                .getFluidType()
                .isLighterThanAir();

        float xMin = tankHullWidth;
        float xMax = xMin + be.width - 2 * tankHullWidth;
        float yMin = totalHeight + capHeight + minPuddleHeight - clampedLevel;
        float yMax = yMin + clampedLevel;

        if (top) {
            yMin += totalHeight - clampedLevel;
            yMax += totalHeight - clampedLevel;
        }

        float zMin = tankHullWidth;
        float zMax = zMin + be.width - 2 * tankHullWidth;

        ms.pushPose();
        ms.translate(0, clampedLevel - totalHeight, 0);
        FluidRenderer.renderFluidBox(fluidStack, xMin, yMin, zMin, xMax, yMax, zMax, buffer, ms, light, false);
        ms.popPose();
    }

    protected void renderAsController(IndustrialLadleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                                  int light, int overlay) {
        BlockState blockState = be.getBlockState();
        VertexConsumer vb = buffer.getBuffer(RenderType.solid());
        ms.pushPose();
        TransformStack msr = TransformStack.cast(ms);
        msr.translate(be.width / 2f, 0.5, be.width / 2f);

        float dialPivot = 5.75f / 16;
        float progress = 0; // = be.boiler.gauge.getValue(partialTicks);

        for (Direction d : Iterate.horizontalDirections) {
            ms.pushPose();
            CachedBufferer.partial(AllPartialModels.BOILER_GAUGE, blockState)
                    .rotateY(d.toYRot())
                    .unCentre()
                    .translate(be.width / 2f - 6 / 16f, 0, 0)
                    .light(light)
                    .renderInto(ms, vb);
            CachedBufferer.partial(AllPartialModels.BOILER_GAUGE_DIAL, blockState)
                    .rotateY(d.toYRot())
                    .unCentre()
                    .translate(be.width / 2f - 6 / 16f, 0, 0)
                    .translate(0, dialPivot, dialPivot)
                    .rotateX(-90 * progress)
                    .translate(0, -dialPivot, -dialPivot)
                    .light(light)
                    .renderInto(ms, vb);
            ms.popPose();
        }

        ms.popPose();
    }

    protected void renderItems(IndustrialLadleBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer,
                               int light, int overlay) {
        int tankIndex = 0;
        for (int yOffset = 0; yOffset < be.height; yOffset++) {
            for (int xOffset = 0; xOffset < be.width; xOffset++) {
                for (int zOffset = 0; zOffset < be.width; zOffset++) {

                    ItemStack stack = be.getControllerBE().inputInv.getStackInSlot(tankIndex);
                    if (stack.isEmpty())
                        continue;

                    ms.pushPose();
                    if (stack.getItem() instanceof BlockItem) {
                        ms.translate(xOffset + .5f, yOffset, zOffset + .5f);
                        ms.scale(3.5f, 3.5f, 3.5f);
                    } else
                        ms.translate(xOffset + .5f, yOffset + .5f, zOffset + .5f);




                    Minecraft.getInstance()
                            .getItemRenderer()
                            .renderStatic(stack, ItemTransforms.TransformType.GROUND, light, overlay, ms, buffer, 0);
                    tankIndex++;
                    ms.popPose();
                }
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(IndustrialLadleBlockEntity be) {
        return be.isController();
    }
}
