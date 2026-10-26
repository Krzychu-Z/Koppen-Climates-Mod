package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.item.ModItems;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Olive leaves, silvery and bright green. Now and then a leaf block grows olives (OLIVES = true). Picking them by
 * hand gives one olive; shears strip 3-5. Either way the leaves are bare again until the olives grow back.
 */
public class OliveLeavesBlock extends LeavesBlock {
    public static final BooleanProperty OLIVES = BooleanProperty.create("olives");
    // 1 in this many random ticks, bare leaves grow olives
    private static final int GROW_CHANCE = 25;

    public OliveLeavesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(OLIVES, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OLIVES);
    }

    // unlike plain leaves, olive leaves always tick: they have olives to grow
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        BlockState now = level.getBlockState(pos);
        if (now.is(this) && !now.getValue(OLIVES) && random.nextInt(GROW_CHANCE) == 0) {
            level.setBlock(pos, now.setValue(OLIVES, true), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(OLIVES)) return InteractionResult.PASS;
        this.pick(state, level, pos, player, 1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(OLIVES) && stack.is(Items.SHEARS)) {
            this.pick(state, level, pos, player, 3 + level.random.nextInt(3));
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // empty hand: fall through to useWithoutItem
        return stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    private void pick(BlockState state, Level level, BlockPos pos, Player player, int count) {
        if (!level.isClientSide) {
            Block.popResource(level, pos, new ItemStack(ModItems.OLIVE.get(), count));
            level.setBlock(pos, state.setValue(OLIVES, false), 2);
            level.gameEvent(player, GameEvent.SHEAR, pos);
        }
        level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + level.random.nextFloat() * 0.2F);
    }
}
