package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The elbow of a saguaro's arm: the one place where a saguaro grows sideways. It sticks out of the trunk on the side
 * it FACES away from, and (UP) carries the rest of the arm, a column of saguaro, upwards. Saguaro segments themselves
 * never join sideways, so two saguaros side by side stay two separate columns. Only grown, never placed (it drops a
 * saguaro).
 */
public class SaguaroArmBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<SaguaroArmBlock> CODEC = simpleCodec(SaguaroArmBlock::new);
    public static final BooleanProperty UP = BlockStateProperties.UP;
    private static final VoxelShape CORE = Block.box(3, 3, 3, 13, 13, 13), TOP = Block.box(3, 13, 3, 13, 16, 13);
    private static final VoxelShape[] TO_TRUNK = {
            Block.box(3, 3, 13, 13, 13, 16),   // south
            Block.box(0, 3, 3, 3, 13, 13),     // west
            Block.box(3, 3, 0, 13, 13, 3),     // north
            Block.box(13, 3, 3, 16, 13, 13)};  // east

    public SaguaroArmBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(UP, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, UP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.or(CORE, TO_TRUNK[state.getValue(FACING).get2DDataValue()]);
        return state.getValue(UP) ? Shapes.or(shape, TOP) : shape;
    }

    static boolean carries(BlockState above) {
        return above.getBlock() instanceof SaguaroBlock || above.getBlock() instanceof SaguaroFlowerBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        return direction == Direction.UP ? state.setValue(UP, carries(neighbour)) : state;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (Shapes.joinIsNotEmpty(state.getShape(level, pos).move(pos.getX(), pos.getY(), pos.getZ()),
                Shapes.create(entity.getBoundingBox().inflate(0.02)), BooleanOp.AND)) {
            entity.hurt(level.damageSources().cactus(), 1.0F);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
