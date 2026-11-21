package net.krisplis.koppenclimate.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The sauna stove (kiuas) of the Viking villages' dry saunas: an iron firebox heaped with stones. Light it (flint and
 * steel, a fire charge, or just a hand on its door) and it glows and crackles; while it burns, anyone in the sauna
 * with it is warmed through (SaunaWarmth: their wounds heal, any chill leaves them). A hand on it again puts it out.
 * Throw water on the hot stones (a water bottle, or a bucket's ladleful - the bucket stays full) for löyly: a hiss and
 * a cloud of steam, and everyone in the sauna healing twice as fast for a while.
 *
 * <p>Burning, it is as hot as lava: it glows as brightly (light 15, so the snow and ice round it melt), and
 * whoever touches it - leans on it, steps on it - is burnt as by lava and catches fire.
 */
public class SaunaStoveBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public SaunaStoveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(LIT) && (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            this.setLit(level, pos, state, true, player);
            if (!level.isClientSide) {
                if (stack.is(Items.FLINT_AND_STEEL)) stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
                else stack.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (state.getValue(LIT) && (stack.is(Items.WATER_BUCKET) || isWaterBottle(stack))) {
            level.playSound(player, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.7F, 1.4F);
            if (level.isClientSide) {
                RandomSource random = level.getRandom();
                for (int i = 0; i < 24; i++) {
                    level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.9,
                            pos.getZ() + 0.2 + random.nextDouble() * 0.6, (random.nextDouble() - 0.5) * 0.08, 0.06 + random.nextDouble() * 0.06,
                            (random.nextDouble() - 0.5) * 0.08);
                }
            } else {
                for (Player near : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos).inflate(3.0))) {
                    near.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 1, true, true));
                    near.setTicksFrozen(0);
                }
                if (isWaterBottle(stack) && !player.getAbilities().instabuild) {
                    player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE)));
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
        var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(net.minecraft.world.item.alchemy.Potions.WATER);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        this.setLit(level, pos, state, !state.getValue(LIT), player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void setLit(Level level, BlockPos pos, BlockState state, boolean lit, Player player) {
        level.playSound(player, pos, lit ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                lit ? 1.0F : 0.5F, 1.0F);
        level.setBlock(pos, state.setValue(LIT, lit), 11);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    // as hot as lava to the touch while it burns
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!state.getValue(LIT) || level.isClientSide || !(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;
        living.lavaHurt();
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        this.entityInside(state, level, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        if (random.nextInt(8) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE,
                    SoundSource.BLOCKS, 0.6F + random.nextFloat() * 0.4F, 0.8F + random.nextFloat() * 0.3F, false);
        }
        // the fire behind the grate, the shimmer of heat over the hot stones
        level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.25,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.0, 0.0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.95,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.04, 0.0);
        }
    }
}
