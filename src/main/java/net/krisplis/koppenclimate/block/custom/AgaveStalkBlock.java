package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The agave's woody flower stalk: a tall pole rising from a grown agave, crowned (TOP) with branches of yellow
 * flower clusters. It stands only on its agave (or more stalk); break the agave and the whole stalk comes down.
 */
public class AgaveStalkBlock extends Block {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    private static final VoxelShape STALK = Block.box(7, 0, 7, 9, 16, 9);
    private static final VoxelShape CROWN = Block.box(1, 0, 1, 15, 15, 15);

    public AgaveStalkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(TOP, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TOP) ? CROWN : STALK;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return STALK;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return (below.is(this) && !below.getValue(TOP))
                || (below.is(ModBlocks.AGAVE.get()) && below.getValue(AgaveBlock.AGE) == 3);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !this.canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
