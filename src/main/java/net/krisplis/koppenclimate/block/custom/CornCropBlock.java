package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Corn (maize): a crop of eight stages, like wheat, on farmland, planted with corn kernels (the corn itself) - but it
 * shoots up three blocks tall. This is its foot, which grows; from stage MIDDLE_AGE it carries a second block of
 * stalk above it ({@link CornStalkBlock}, part middle), from stage TOP_AGE a third (part top), which ends in the
 * tassel. It only grows on into a stage that needs a block above it when that block is free. Broken (at any of its
 * three blocks) a ripe plant gives four cobs of corn, an unripe one the kernel it was planted from.
 */
public class CornCropBlock extends CropBlock {
    public static final MapCodec<CornCropBlock> CODEC = simpleCodec(CornCropBlock::new);
    // the stages from which the stalk reaches into the second and the third block
    public static final int MIDDLE_AGE = 3, TOP_AGE = 5;
    private static final VoxelShape TALL = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public CornCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CornCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.CORN.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getAge(state) >= MIDDLE_AGE ? TALL : super.getShape(state, level, pos, context);
    }

    /** A whole plant of this age, its foot at pos (used by world generation; the blocks above must be free). */
    public static void placeColumn(LevelAccessor level, BlockPos pos, int age, int flags) {
        CornCropBlock corn = ModBlocks.CORN.get();
        level.setBlock(pos, corn.getStateForAge(age), flags);
        if (age >= MIDDLE_AGE) level.setBlock(pos.above(), CornStalkBlock.stalk(CornStalkBlock.Part.MIDDLE, age), flags);
        if (age >= TOP_AGE) level.setBlock(pos.above(2), CornStalkBlock.stalk(CornStalkBlock.Part.TOP, age), flags);
    }

    /** Whether the stalk can take (or already has) this block. */
    private static boolean room(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.CORN_STALK.get()) || state.isAir() || state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide) return;
        int age = this.getAge(state);
        // it can only grow as tall as the room above it lets it
        int cap = !room(level, pos.above()) ? MIDDLE_AGE - 1 : !room(level, pos.above(2)) ? TOP_AGE - 1 : this.getMaxAge();
        if (age > cap) {
            level.setBlock(pos, this.getStateForAge(cap), Block.UPDATE_CLIENTS);
            return;
        }
        // its stalk above, at its stage
        this.sync(level, pos.above(), age >= MIDDLE_AGE, CornStalkBlock.Part.MIDDLE, age);
        this.sync(level, pos.above(2), age >= TOP_AGE, CornStalkBlock.Part.TOP, age);
    }

    private void sync(Level level, BlockPos at, boolean wanted, CornStalkBlock.Part part, int age) {
        BlockState there = level.getBlockState(at);
        boolean has = there.is(ModBlocks.CORN_STALK.get());
        if (wanted) {
            BlockState stalk = CornStalkBlock.stalk(part, age);
            if (there != stalk) level.setBlock(at, stalk, Block.UPDATE_CLIENTS);
        } else if (has) {
            level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        // its stalk above knocked away (an explosion, say): it falls back to a stage short of it, and grows again
        if (direction == Direction.UP && this.getAge(state) >= MIDDLE_AGE && !neighbor.is(ModBlocks.CORN_STALK.get())) {
            return this.getStateForAge(MIDDLE_AGE - 1);
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
}
