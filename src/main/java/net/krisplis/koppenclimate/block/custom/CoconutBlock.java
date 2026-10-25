package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

// Coconuts hanging on the side of a coconut palm log, under the crown. Like the banana bunch, everything
// else comes from CocoaBlock: three growth stages, random growth, bone meal, planting from the item,
// and dropping off when the log goes. Drops are in loot_table/blocks/coconut.json.
// FACING points from the coconuts to the log they hang on.
public class CoconutBlock extends CocoaBlock {
    // {width, height, depth} of the cluster per stage, in pixels; it hangs from the top of the block
    private static final int[][] SIZES = {{5, 5, 5}, {8, 7, 7}, {11, 9, 8}};
    private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape[] shapes = new VoxelShape[SIZES.length];
            for (int age = 0; age < SIZES.length; age++) {
                double w = SIZES[age][0], h = SIZES[age][1], d = SIZES[age][2];
                double a = 8 - w / 2, b = 8 + w / 2, top = 16, bottom = 16 - h;
                shapes[age] = switch (facing) {
                    case NORTH -> Block.box(a, bottom, 0, b, top, d);
                    case SOUTH -> Block.box(a, bottom, 16 - d, b, top, 16);
                    case WEST -> Block.box(0, bottom, a, d, top, b);
                    default -> Block.box(16 - d, bottom, a, 16, top, b);   // EAST
                };
            }
            SHAPES.put(facing, shapes);
        }
    }

    public CoconutBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(ModBlockTags.COCONUT_PALM_LOGS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING))[state.getValue(AGE)];
    }
}
