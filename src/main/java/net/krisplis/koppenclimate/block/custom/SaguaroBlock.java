package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * A segment of a giant saguaro cactus, the big forked cactus of the Lush desert (BSh). Like the chorus plant it joins
 * up with the segments around it, so a trunk with arms reaching out and bending upwards (the saguaro's fork) is one
 * smooth, ribbed column (the arms' elbows are SaguaroArmBlock; segments never join sideways). Unlike the small cactus it doesn't mind blocks beside it, but it pricks just the same.
 * Flowers (SaguaroFlowerBlock) sit on top of its trunk and arms.
 */
public class SaguaroBlock extends PipeBlock {
    public static final MapCodec<SaguaroBlock> CODEC = simpleCodec(SaguaroBlock::new);

    public SaguaroBlock(Properties properties) {
        // a trunk 10 pixels across
        super(0.3125F, properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false)
                .setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends PipeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    /** The segment joined up with what's around it at pos. */
    public BlockState connected(BlockGetter level, BlockPos pos) {
        BlockState state = this.defaultBlockState();
        for (Direction d : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(d), this.joins(level, pos.relative(d), d));
        }
        return state;
    }

    private boolean joins(BlockGetter level, BlockPos neighbour, Direction towards) {
        // a saguaro never joins sideways: its arms grow out of it through their own elbow blocks (SaguaroArmBlock)
        if (towards.getAxis().isHorizontal()) return false;
        BlockState s = level.getBlockState(neighbour);
        if (s.is(this)) return true;
        // its flowers sit right on top of it
        if (towards == Direction.UP) return s.getBlock() instanceof SaguaroFlowerBlock;
        // the upright part of an arm stands on its elbow; the trunk stands on the ground
        return s.getBlock() instanceof SaguaroArmBlock || s.is(BlockTags.SAND) || s.is(BlockTags.DIRT) || s.is(BlockTags.TERRACOTTA)
                || s.isFaceSturdy(level, neighbour, Direction.UP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.connected(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor level,
                                     BlockPos pos, BlockPos neighbourPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), this.joins(level, neighbourPos, direction));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // only when actually touching the cactus, not just standing in a corner of its block
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
