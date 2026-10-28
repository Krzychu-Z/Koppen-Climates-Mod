package net.krisplis.koppenclimate.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Nerium (oleander): a dense, poisonous flowering bush built of these blocks. Planted it stands one block wide and
 * two tall; it grows on its own (or with bone meal) up to two by two blocks across and three tall. Falling into it
 * from a height poisons you a little. Broken, it gives nerium twigs, or the bush itself with shears or Silk Touch.
 * It doesn't grow wild: it'll come from Italian villages.
 */
public class NeriumBlock extends Block implements BonemealableBlock {
    // the most blocks one bush grows to: 2 x 2 across, 3 tall
    private static final int MAX_SPAN = 2, MAX_HEIGHT = 3, MAX_BLOCKS = MAX_SPAN * MAX_SPAN * MAX_HEIGHT;
    // falls from further than this poison
    private static final float POISON_FALL = 3.0F;

    public NeriumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || below.is(BlockTags.DIRT);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.DOWN && !this.canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    // a planted nerium is a young bush already two blocks tall
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level.getBlockState(pos.above()).isAir()) {
            level.setBlock(pos.above(), this.defaultBlockState(), 3);
        }
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!level.isClientSide && fallDistance > POISON_FALL && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(8) == 0) this.grow(level, pos, random);
    }

    /** The blocks of the bush this one belongs to. */
    private Set<BlockPos> bush(LevelReader level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && seen.size() <= MAX_BLOCKS + 4) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!seen.contains(n) && level.getBlockState(n).is(this)) {
                    seen.add(n);
                    queue.add(n);
                }
            }
        }
        return seen;
    }

    /** Adds one block to the bush, keeping it within 2 x 2 across and 3 tall; returns whether it grew. */
    private boolean grow(Level level, BlockPos pos, RandomSource random) {
        Set<BlockPos> bush = this.bush(level, pos);
        if (bush.size() >= MAX_BLOCKS) return false;
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, base = Integer.MAX_VALUE;
        for (BlockPos p : bush) {
            minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
            minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
            base = Math.min(base, p.getY());
        }
        List<BlockPos> options = new ArrayList<>();
        for (BlockPos p : bush) {
            for (Direction d : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos n = p.relative(d);
                if (n.getY() >= base + MAX_HEIGHT || !level.getBlockState(n).isAir()) continue;
                if (Math.max(maxX, n.getX()) - Math.min(minX, n.getX()) >= MAX_SPAN) continue;
                if (Math.max(maxZ, n.getZ()) - Math.min(minZ, n.getZ()) >= MAX_SPAN) continue;
                if (this.canSurvive(this.defaultBlockState(), level, n)) options.add(n);
            }
        }
        if (options.isEmpty()) return false;
        level.setBlock(options.get(random.nextInt(options.size())), this.defaultBlockState(), 3);
        return true;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return this.bush(level, pos).size() < MAX_BLOCKS;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        this.grow(level, pos, random);
    }
}
