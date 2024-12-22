package fr.lucreeper74.createmetallurgy.content.industrial_ladle;

import com.simibubi.create.Create;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.foundation.block.IBE;
import fr.lucreeper74.createmetallurgy.registries.CMBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IndustrialLadleBlock extends Block implements IBE<IndustrialLadleBlockEntity>, IWrenchable {

    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final EnumProperty<FluidTankBlock.Shape> SHAPE = EnumProperty.create("shape", FluidTankBlock.Shape.class);

    public IndustrialLadleBlock(Properties pProperties) {
        super(pProperties);
        registerDefaultState(defaultBlockState().setValue(TOP, true)
                .setValue(BOTTOM, true)
                .setValue(SHAPE, FluidTankBlock.Shape.WINDOW));
    }

    public static boolean isLadle(BlockState state) {
        return state.getBlock() instanceof IndustrialLadleBlock;
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock())
            return;
        if (moved)
            return;
        withBlockEntityDo(world, pos, IndustrialLadleBlockEntity::updateConnectivity);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM, SHAPE);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        IndustrialLadleBlockEntity ladle = ConnectivityHandler.partAt(getBlockEntityType(), world, pos);
        if (ladle == null)
            return 0;
        IndustrialLadleBlockEntity controllerBE = ladle.getControllerBE();
        if (controllerBE == null || !controllerBE.window)
            return 0;
        return 15;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        withBlockEntityDo(context.getLevel(), context.getClickedPos(), IndustrialLadleBlockEntity::toggleWindows);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (world.getBlockEntity(pos) instanceof IndustrialLadleBlockEntity ladleBE) {
            if (ladleBE.getControllerBE().ladle.isControlled()) {
                world.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, .2f,
                        1f + Create.RANDOM.nextFloat());
                ladleBE.getControllerBE().updateLadleState();
                if (!context.getPlayer().isCreative())
                    context.getPlayer().getInventory().placeItemBackInInventory(new ItemStack(Items.FURNACE));
            }
        }

        return InteractionResult.SUCCESS;
    }

    static final VoxelShape CAMPFIRE_SMOKE_CLIP = Block.box(0, 4, 0, 16, 16, 16);

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        if (context == CollisionContext.empty())
            return CAMPFIRE_SMOKE_CLIP;
        return state.getShape(level, pos);
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter reader, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult ray) {
        ItemStack heldItem = player.getItemInHand(handIn);

        if (world.isClientSide)
            return InteractionResult.SUCCESS;

        return onBlockEntityUse(world, pos, be -> {
            if (!heldItem.isEmpty()) {
                if (heldItem.getItem().equals(Items.BLAST_FURNACE)) {
                    updateLadleState(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, .2f,
                            1f + Create.RANDOM.nextFloat());
                    if (!player.isCreative())
                        heldItem.shrink(1);

                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        });
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (!(be instanceof IndustrialLadleBlockEntity))
                return;
            IndustrialLadleBlockEntity ladleBe = (IndustrialLadleBlockEntity) be;
            world.removeBlockEntity(pos);
            ConnectivityHandler.splitMulti(ladleBe);
        }
    }

    @Override
    public Class<IndustrialLadleBlockEntity> getBlockEntityClass() {
        return IndustrialLadleBlockEntity.class;
    }

    public BlockEntityType<? extends IndustrialLadleBlockEntity> getBlockEntityType() {
        return CMBlockEntityTypes.INDUSTRIAL_LADLE.get();
    }


    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE)
            return state;
        boolean x = mirror == Mirror.FRONT_BACK;
        return switch (state.getValue(SHAPE)) {
            case WINDOW_NE ->
                    state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SE);
            case WINDOW_NW ->
                    state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SW);
            case WINDOW_SE ->
                    state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_SW : FluidTankBlock.Shape.WINDOW_NE);
            case WINDOW_SW ->
                    state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_SE : FluidTankBlock.Shape.WINDOW_NW);
            default -> state;
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        for (int i = 0; i < rotation.ordinal(); i++)
            state = rotateOnce(state);
        return state;
    }

    private BlockState rotateOnce(BlockState state) {
        return switch (state.getValue(SHAPE)) {
            case WINDOW_NE -> state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_SE);
            case WINDOW_NW -> state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_NE);
            case WINDOW_SE -> state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_SW);
            case WINDOW_SW -> state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_NW);
            default -> state;
        };
    }

    @Override
    public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
        SoundType soundType = super.getSoundType(state, world, pos, entity);
        return soundType;
    }

    public static void updateLadleState(BlockState state, Level pLevel, BlockPos pos) {
        BlockState tankState = pLevel.getBlockState(pos);
        if (!(tankState.getBlock() instanceof IndustrialLadleBlock ladle))
            return;
        IndustrialLadleBlockEntity tankBE = ladle.getBlockEntity(pLevel, pos);
        if (tankBE == null)
            return;
        IndustrialLadleBlockEntity controllerBE = tankBE.getControllerBE();
        if (controllerBE == null)
            return;
        controllerBE.updateLadleState();
    }
}
