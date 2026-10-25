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

// A bunch of bananas hanging on the side of a banana stem. Everything else comes from CocoaBlock:
// three growth stages (AGE 0-2), random growth, bone meal, placing it from the banana item onto a stem's
// side, and popping off when the stem is gone. What it drops is in loot_table/blocks/banana_bunch.json.
// FACING points from the bunch to the stem it hangs on.
public class BananaBunchBlock extends CocoaBlock {
    // {width, height, depth} of the bunch per stage, in pixels; it hangs from the top of the block
    private static final int[][] SIZES = {{6, 11, 6}, {10, 14, 8}, {12, 16, 9}};
    private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape[] shapes = new VoxelShape[SIZES.length];
            for (int age = 0; age < SIZES.length; age++) {
                double w = SIZES[age][0], h = SIZES[age][1], d = SIZES[age][2];
                double a = 8 - w / 2, b = 8 + w / 2, top = 16, bottom = 16 - h;
                shapes[age] = switch (facing) {
                    case NORTH -> Block.box(a, bottom, 1, b, top, 1 + d);
                    case SOUTH -> Block.box(a, bottom, 15 - d, b, top, 15);
                    case WEST -> Block.box(1, bottom, a, 1 + d, top, b);
                    default -> Block.box(15 - d, bottom, a, 15, top, b);   // EAST
                };
            }
            SHAPES.put(facing, shapes);
        }
    }

    public BananaBunchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.relative(state.getValue(FACING))).is(ModBlockTags.BANANA_STEMS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING))[state.getValue(AGE)];
    }
}
