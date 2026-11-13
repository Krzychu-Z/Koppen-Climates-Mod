package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The upper two blocks of a corn plant ({@link CornCropBlock}): the middle of the stalk, with the cobs, and the top,
 * with the tassel. They only ever stand on the plant below them, show its stage (AGE, kept in step by the plant), drop
 * nothing of their own and pass bone meal on to the plant's foot; breaking one breaks the whole plant, which then
 * gives what a corn plant gives.
 */
public class CornStalkBlock extends Block implements BonemealableBlock {
    public static final MapCodec<CornStalkBlock> CODEC = simpleCodec(CornStalkBlock::new);

    public enum Part implements StringRepresentable {
        MIDDLE("middle"), TOP("top");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public CornStalkBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PART, Part.MIDDLE).setValue(AGE, CornCropBlock.MIDDLE_AGE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    public static BlockState stalk(Part part, int age) {
        return ModBlocks.CORN_STALK.get().defaultBlockState().setValue(PART, part).setValue(AGE, age);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The plant's foot (the corn crop block) below this part of it. */
    private static BlockPos foot(BlockState state, BlockPos pos) {
        return state.getValue(PART) == Part.TOP ? pos.below(2) : pos.below();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (state.getValue(PART) == Part.TOP) {
            return below.is(this) && below.getValue(PART) == Part.MIDDLE;
        }
        return below.is(ModBlocks.CORN.get()) && ModBlocks.CORN.get().getAge(below) >= CornCropBlock.MIDDLE_AGE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !this.canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // the whole plant comes down, and gives what it gives from its foot
        if (!level.isClientSide) {
            BlockPos foot = foot(state, pos);
            if (level.getBlockState(foot).is(ModBlocks.CORN.get())) level.destroyBlock(foot, !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(ModItems.CORN.get());
    }

    // ---------------------------------------------------------------- bone meal: passed on to the foot

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos foot = foot(state, pos);
        BlockState plant = level.getBlockState(foot);
        return plant.is(ModBlocks.CORN.get()) && ModBlocks.CORN.get().isValidBonemealTarget(level, foot, plant);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos foot = foot(state, pos);
        BlockState plant = level.getBlockState(foot);
        if (plant.is(ModBlocks.CORN.get())) ModBlocks.CORN.get().growCrops(level, foot, plant);
    }
}
