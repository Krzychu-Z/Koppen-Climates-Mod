package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * A woven sleeping mat of reeds and bamboo, laid on the floor. It works exactly like a bed (two blocks long,
 * sleep through the night, set your spawn), but it is a flat ordinary block model rather than a bed's
 * animated one, and has no block entity.
 */
public class SleepingMatBlock extends BedBlock {
    public static final MapCodec<SleepingMatBlock> CODEC = simpleCodec(SleepingMatBlock::new);
    private static final VoxelShape MAT = box(0, 0, 0, 16, 1, 16);
    // the foot end is just the mat; the head end has a rolled-up pillow on it
    private static final VoxelShape HEAD_NORTH = Shapes.or(MAT, box(1, 1, 1, 15, 3, 5));
    private static final VoxelShape HEAD_SOUTH = Shapes.or(MAT, box(1, 1, 11, 15, 3, 15));
    private static final VoxelShape HEAD_WEST = Shapes.or(MAT, box(1, 1, 1, 5, 3, 15));
    private static final VoxelShape HEAD_EAST = Shapes.or(MAT, box(11, 1, 1, 15, 3, 15));

    public SleepingMatBlock(Properties properties) {
        super(DyeColor.YELLOW, properties);
    }

    @Override
    public MapCodec<BedBlock> codec() {
        @SuppressWarnings("unchecked")
        MapCodec<BedBlock> codec = (MapCodec<BedBlock>) (MapCodec<?>) CODEC;
        return codec;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(PART) == BedPart.FOOT) {
            return MAT;
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> HEAD_SOUTH;
            case WEST -> HEAD_WEST;
            case EAST -> HEAD_EAST;
            default -> HEAD_NORTH;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }
}
