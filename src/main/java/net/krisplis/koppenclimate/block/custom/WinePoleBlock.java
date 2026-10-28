package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wine pole: a thin post that a grapevine climbs. Use red or green wine grapes on a bare pole to plant a vine
 * (GRAPE); it grows through four stages (AGE 0 young, 1 leafy, 2 unripe green bunches, 3 ripe). Ripe grapes are
 * picked by hand, 2-3 at a time, and the vine fruits again; broken while ripe, it drops its grapes too. A leafy vine
 * climbs onto a bare pole stacked on top of it. Tomatoes are grown the same way (TOMATO): a ripe plant gives 4 tomatoes.
 */
public class WinePoleBlock extends Block implements BonemealableBlock {
    public enum Grape implements StringRepresentable {
        NONE("none"), RED("red"), GREEN("green"), TOMATO("tomato");

        private final String name;

        Grape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public static final EnumProperty<Grape> GRAPE = EnumProperty.create("grape", Grape.class);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape POST = Block.box(7, 0, 7, 9, 16, 9);
    private static final VoxelShape VINE = Block.box(3, 0, 3, 13, 16, 13);

    public WinePoleBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GRAPE, Grape.NONE).setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRAPE, AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(GRAPE) == Grape.NONE ? POST : VINE;
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

    public static Item grapes(Grape grape) {
        return grape == Grape.GREEN ? ModItems.GREEN_WINE_GRAPES.get() : grape == Grape.TOMATO ? ModItems.TOMATO.get()
                : ModItems.RED_WINE_GRAPES.get();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(GRAPE) == Grape.NONE) {
            Grape grape = stack.is(ModItems.RED_WINE_GRAPES.get()) ? Grape.RED : stack.is(ModItems.GREEN_WINE_GRAPES.get()) ? Grape.GREEN
                    : stack.is(ModItems.TOMATO.get()) ? Grape.TOMATO : null;
            if (grape != null) {
                if (!level.isClientSide) {
                    level.setBlock(pos, state.setValue(GRAPE, grape).setValue(AGE, 0), 2);
                    stack.consume(1, player);
                    level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                }
                level.playSound(player, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(GRAPE) == Grape.NONE || state.getValue(AGE) < 3) return InteractionResult.PASS;
        if (!level.isClientSide) {
            Block.popResource(level, pos, new ItemStack(grapes(state.getValue(GRAPE)),
                    state.getValue(GRAPE) == Grape.TOMATO ? 4 : 2 + level.random.nextInt(2)));
            level.setBlock(pos, state.setValue(AGE, 1), 2);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(GRAPE) != Grape.NONE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 3 && level.getRawBrightness(pos, 0) >= 9 && random.nextInt(7) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
        // a leafy vine climbs onto a bare pole above
        BlockState above = level.getBlockState(pos.above());
        if (age >= 1 && above.is(this) && above.getValue(GRAPE) == Grape.NONE && random.nextInt(10) == 0) {
            level.setBlock(pos.above(), above.setValue(GRAPE, state.getValue(GRAPE)).setValue(AGE, 0), 2);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(GRAPE) != Grape.NONE && state.getValue(AGE) < 3;
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
