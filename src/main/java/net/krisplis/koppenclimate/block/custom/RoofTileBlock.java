package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

/**
 * Terracotta roof tiles (coppi): a single sheet of tiles tilted at 45 degrees, from the bottom of one side of the
 * block to the top of the other. Like stairs, it rises toward FACING, the way the player looked when placing it,
 * and like stairs it turns the corner (SHAPE): where two slopes meet at right angles it bends into an outer (hip)
 * corner or an inner (valley) corner. Laid diagonally one after another they make a seamless pitched roof. You can
 * walk on it: its collision is a fine staircase of thin steps under the tiles.
 */
public class RoofTileBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RoofTileBlock> CODEC = simpleCodec(RoofTileBlock::new);
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
    private static final Map<String, VoxelShape> SHAPES = new HashMap<>();

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (StairsShape shape : StairsShape.values()) {
                VoxelShape s = Shapes.empty();
                // the roof's height over a grid of 2x2 columns, as a thin step at each
                for (int i = 0; i < 8; i++) {
                    for (int k = 0; k < 8; k++) {
                        double h = height(facing, shape, 2 * i + 1, 2 * k + 1);
                        double top = Math.min(16, Math.ceil(h)), bottom = Math.max(0, top - 3);
                        s = Shapes.or(s, Block.box(2 * i, bottom, 2 * k, 2 * i + 2, top, 2 * k + 2));
                    }
                }
                SHAPES.put(facing.getName() + shape.getSerializedName(), s.optimize());
            }
        }
    }

    /** The roof's height (0-16) at (x, z) in the block (0-16). */
    private static double height(Direction facing, StairsShape shape, double x, double z) {
        // turn (x, z) so that the roof rises to the north, as the models are drawn
        double rx = x, rz = z;
        switch (facing) {
            case EAST -> { rx = z; rz = 16 - x; }
            case SOUTH -> { rx = 16 - x; rz = 16 - z; }
            case WEST -> { rx = 16 - z; rz = x; }
            default -> { }
        }
        // a right corner is a left one turned a quarter clockwise
        if (shape == StairsShape.OUTER_RIGHT || shape == StairsShape.INNER_RIGHT) {
            double t = rx;
            rx = rz;
            rz = 16 - t;
        }
        double north = 16 - rz, west = 16 - rx;
        return switch (shape) {
            case STRAIGHT -> north;
            case OUTER_LEFT, OUTER_RIGHT -> Math.min(north, west);
            default -> Math.max(north, west);
        };
    }

    public RoofTileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, StairsShape.STRAIGHT));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
        return state.setValue(SHAPE, shapeAt(state, context.getLevel(), context.getClickedPos()));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction.getAxis().isHorizontal() ? state.setValue(SHAPE, shapeAt(state, level, pos))
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    /** Which way it bends, from the roof tiles beside it, just as stairs do. */
    public static StairsShape shapeAt(BlockState state, BlockGetter level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockState ahead = level.getBlockState(pos.relative(facing));
        if (ahead.getBlock() instanceof RoofTileBlock) {
            Direction other = ahead.getValue(FACING);
            if (other.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, other.getOpposite())) {
                return other == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
            }
        }
        BlockState behind = level.getBlockState(pos.relative(facing.getOpposite()));
        if (behind.getBlock() instanceof RoofTileBlock) {
            Direction other = behind.getValue(FACING);
            if (other.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, other)) {
                return other == facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
            }
        }
        return StairsShape.STRAIGHT;
    }

    private static boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        BlockState neighbor = level.getBlockState(pos.relative(side));
        return !(neighbor.getBlock() instanceof RoofTileBlock) || neighbor.getValue(FACING) != state.getValue(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING).getName() + state.getValue(SHAPE).getSerializedName());
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    // as stairs mirror
    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction facing = state.getValue(FACING);
        StairsShape shape = state.getValue(SHAPE);
        switch (mirror) {
            case LEFT_RIGHT -> {
                if (facing.getAxis() == Direction.Axis.Z) {
                    return switch (shape) {
                        case INNER_LEFT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                        case INNER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                        case OUTER_LEFT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                        case OUTER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                        default -> state.rotate(Rotation.CLOCKWISE_180);
                    };
                }
            }
            case FRONT_BACK -> {
                if (facing.getAxis() == Direction.Axis.X) {
                    return switch (shape) {
                        case INNER_LEFT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_LEFT);
                        case INNER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.INNER_RIGHT);
                        case OUTER_LEFT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_RIGHT);
                        case OUTER_RIGHT -> state.rotate(Rotation.CLOCKWISE_180).setValue(SHAPE, StairsShape.OUTER_LEFT);
                        case STRAIGHT -> state.rotate(Rotation.CLOCKWISE_180);
                    };
                }
            }
            default -> { }
        }
        return super.mirror(state, mirror);
    }
}
