package net.krisplis.koppenclimate.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

/**
 * Leaves of a fruit tree (fig, lemon). FRUIT 0: bare leaves, 1: unripe (green) fruit, 2: ripe fruit. Ripe fruit is
 * picked like olives: by hand one, with shears 3-5; then the leaves are bare again until new fruit sets and ripens.
 */
public class FruitLeavesBlock extends LeavesBlock {
    public static final IntegerProperty FRUIT = IntegerProperty.create("fruit", 0, 2);
    private final Supplier<? extends Item> fruit;
    // 1 in this many random ticks the fruit moves on a stage
    private final int growChance;

    public FruitLeavesBlock(Supplier<? extends Item> fruit, int growChance, Properties properties) {
        super(properties);
        this.fruit = fruit;
        this.growChance = growChance;
        this.registerDefaultState(this.defaultBlockState().setValue(FRUIT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FRUIT);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        BlockState now = level.getBlockState(pos);
        if (now.is(this) && now.getValue(FRUIT) < 2 && random.nextInt(this.growChance) == 0) {
            level.setBlock(pos, now.setValue(FRUIT, now.getValue(FRUIT) + 1), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(FRUIT) < 2) return InteractionResult.PASS;
        this.pick(state, level, pos, player, 1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(FRUIT) == 2 && stack.is(Items.SHEARS)) {
            this.pick(state, level, pos, player, 3 + level.random.nextInt(3));
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    private void pick(BlockState state, Level level, BlockPos pos, Player player, int count) {
        if (!level.isClientSide) {
            Block.popResource(level, pos, new ItemStack(this.fruit.get(), count));
            level.setBlock(pos, state.setValue(FRUIT, 0), 2);
            level.gameEvent(player, GameEvent.SHEAR, pos);
        }
        level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
    }
}
