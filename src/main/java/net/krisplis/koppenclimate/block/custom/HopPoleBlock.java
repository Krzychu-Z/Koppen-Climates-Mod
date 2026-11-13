package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hop pole: a tall, thin pole for a hop bine to wind up - grown in the German towns' hop gardens (Dfb), where the
 * poles stand six blocks high, twice a grapevine's. Use hops (the cones) on a bare pole to plant a bine; it grows
 * through four stages (AGE 0 young, 1 leafy, 2 flowering, 3 ripe cones). Ripe cones are picked by hand, 2-3 at a time,
 * and the bine bears again; broken while ripe, it drops its cones too. A leafy bine climbs onto a bare pole stacked
 * on top of it - quickly, as hops do.
 */
public class HopPoleBlock extends Block implements BonemealableBlock {
    public static final BooleanProperty HOPS = BooleanProperty.create("hops");
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape POST = Block.box(7, 0, 7, 9, 16, 9);
    private static final VoxelShape BINE = Block.box(3, 0, 3, 13, 16, 13);

    public HopPoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HOPS, false).setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HOPS, AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HOPS) ? BINE : POST;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return POST;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || below.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !this.canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(HOPS) && stack.is(ModItems.HOPS.get())) {
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(HOPS, true).setValue(AGE, 0), 2);
                stack.consume(1, player);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            level.playSound(player, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(HOPS) || state.getValue(AGE) < 3) return InteractionResult.PASS;
        if (!level.isClientSide) {
            Block.popResource(level, pos, new ItemStack(ModItems.HOPS.get(), 2 + level.random.nextInt(2)));
            level.setBlock(pos, state.setValue(AGE, 1), 2);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HOPS);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 3 && level.getRawBrightness(pos, 0) >= 9 && random.nextInt(7) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
        BlockState above = level.getBlockState(pos.above());
        if (age >= 1 && above.is(this) && !above.getValue(HOPS) && random.nextInt(5) == 0) {
            level.setBlock(pos.above(), above.setValue(HOPS, true).setValue(AGE, 0), 2);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(HOPS) && state.getValue(AGE) < 3;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, Math.min(3, state.getValue(AGE) + 1)), 2);
    }
}
