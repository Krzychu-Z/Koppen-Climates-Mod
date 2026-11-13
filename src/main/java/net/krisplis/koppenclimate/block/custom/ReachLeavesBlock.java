package net.krisplis.koppenclimate.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import javax.annotation.Nullable;
import java.util.OptionalInt;

/**
 * Leaves that decay like vanilla's - once no wood is near enough, they drop away on their own, a little at a time -
 * but whose reach from the wood fits their tree's shape. Vanilla's leaves hold on six leaves from a log at most; the
 * broad, flat canopy of the Italian (stone) pine reaches ten leaves out along its boughs, the plane tree's and the
 * fig's spreading crowns eight. With a reach of six these are vanilla leaves in every way.
 *
 * <p>The distance from the wood is kept in {@link #REACH} (up to {@link #MAX_REACH} + 1, the "decaying" value), and
 * mirrored, capped at 7, in vanilla's {@link LeavesBlock#DISTANCE}, so vanilla's leaves beside them still count
 * through them as they always do. It's worked out the vanilla way: one more than the least of the six neighbours'
 * (a log counting 0), on every change beside it.
 */
public class ReachLeavesBlock extends LeavesBlock {
    public static final int MAX_REACH = 10;
    public static final IntegerProperty REACH = IntegerProperty.create("reach", 1, MAX_REACH + 1);
    private final int reach;

    public ReachLeavesBlock(int reach, Properties properties) {
        super(properties);
        this.reach = Math.max(DECAY_DISTANCE - 1, Math.min(MAX_REACH, reach));
        this.registerDefaultState(this.defaultBlockState().setValue(REACH, MAX_REACH + 1));
    }

    /** How many leaves out from the wood these leaves hold on: 6 (vanilla's) up to MAX_REACH. */
    public int reach() {
        return this.reach;
    }

    private boolean vanilla() {
        return this.reach <= DECAY_DISTANCE - 1;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(REACH);
    }

    @Override
    protected boolean decaying(BlockState state) {
        if (this.vanilla()) return super.decaying(state);
        return !state.getValue(PERSISTENT) && state.getValue(REACH) > this.reach;
    }

    // vanilla's looks at its own DISTANCE (7), which these leaves also show from 7 leaves out to their reach
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        if (this.vanilla()) return super.isRandomlyTicking(state);
        return this.decaying(state);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.vanilla()) {
            super.tick(state, level, pos, random);
            return;
        }
        level.setBlock(pos, this.updateReach(state, level, pos), Block.UPDATE_ALL);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || this.vanilla()) return state;
        return this.updateReach(state, context.getLevel(), context.getClickedPos());
    }

    /** The distance from the wood, counted the vanilla way: one more than the nearest neighbour's. */
    private BlockState updateReach(BlockState state, LevelAccessor level, BlockPos pos) {
        int best = MAX_REACH + 1;
        BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
        for (Direction d : Direction.values()) {
            n.setWithOffset(pos, d);
            best = Math.min(best, distanceAt(level.getBlockState(n)) + 1);
            if (best == 1) break;
        }
        return withDistance(state, best);
    }

    /** A neighbour's distance from the wood: 0 for a log, a leaf's own count, or "none" for anything else. */
    private static int distanceAt(BlockState neighbour) {
        if (neighbour.is(BlockTags.LOGS)) return 0;
        if (neighbour.hasProperty(REACH) && neighbour.getBlock() instanceof ReachLeavesBlock r && !r.vanilla()) {
            return neighbour.getValue(REACH);
        }
        OptionalInt vanilla = LeavesBlock.getOptionalDistanceAt(neighbour);
        return vanilla.isPresent() ? vanilla.getAsInt() : MAX_REACH + 1;
    }

    /** These leaves at this distance from the wood (in both counts, vanilla's capped at 7). */
    public static BlockState withDistance(BlockState state, int distance) {
        if (state.hasProperty(REACH)) state = state.setValue(REACH, Math.max(1, Math.min(MAX_REACH + 1, distance)));
        if (state.hasProperty(DISTANCE)) state = state.setValue(DISTANCE, Math.max(1, Math.min(DECAY_DISTANCE, distance)));
        return state;
    }

    /** How far out from the wood leaves of this kind hold on (6 for vanilla's and any others). */
    public static int reachOf(BlockState leaves) {
        return leaves.getBlock() instanceof ReachLeavesBlock r ? r.reach() : DECAY_DISTANCE - 1;
    }
}
