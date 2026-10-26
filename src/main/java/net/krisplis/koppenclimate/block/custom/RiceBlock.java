package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;

/**
 * Rice: a crop with wheat's eight growth stages that stands in water, the way seagrass does, so the block is
 * always full of water and leaves water behind when it is harvested. It needs exactly one block of water
 * (still water above any kind of dirt, with nothing but air over it) and grows only where it is warm: in a
 * biome whose temperature is lukewarm or warmer and where water never freezes.
 */
public class RiceBlock extends CropBlock implements LiquidBlockContainer {
    public static final MapCodec<RiceBlock> CODEC = simpleCodec(RiceBlock::new);
    // biome temperature from which it's warm enough: plains (0.8), forests (0.7), swamps, jungles, savannas,
    // deserts... but not birch forests' cooler edge, meadows, taigas or anything snowy
    private static final float MIN_TEMPERATURE = 0.6F;
    // chance (1 in N) per random tick to grow a stage: about as fast as wheat on well-watered farmland
    private static final int GROWTH_CHANCE = 4;

    public RiceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.RICE_SEEDS.get();
    }

    /** Warm enough to grow rice here: lukewarm or warmer, and water never freezes. */
    public static boolean isWarmEnough(LevelReader level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        return biome.getBaseTemperature() >= MIN_TEMPERATURE && !biome.coldEnoughToSnow(pos);
    }

    // rooted in any kind of dirt (grass, podzol, mud, oxisol...) or farmland
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
    }

    // still water in its own block, dirt under it, and open air above: water exactly one block deep
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        FluidState here = level.getFluidState(pos);
        BlockPos below = pos.below();
        return here.is(FluidTags.WATER) && here.isSource()
                && level.getFluidState(pos.above()).isEmpty()
                && this.mayPlaceOn(level.getBlockState(below), level, below);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return fluid.is(FluidTags.WATER) && fluid.isSource() ? super.getStateForPlacement(context) : null;
    }

    // losing its footing, or the water getting deeper, washes it away and leaves the water behind
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.WATER.defaultBlockState();
        }
        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.WATER.getSource(false);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable Player player, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return false;
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return false;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || this.isMaxAge(state) || !isWarmEnough(level, pos)) return;
        if (level.getRawBrightness(pos, 0) < 9) return;
        if (ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt(GROWTH_CHANCE) == 0)) {
            level.setBlock(pos, this.getStateForAge(this.getAge(state) + 1), 2);
            ForgeHooks.onCropsGrowPost(level, pos, state);
        }
    }

    // bone meal does nothing in the cold either
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return super.isValidBonemealTarget(level, pos, state) && isWarmEnough(level, pos);
    }

    @Override
    public void growCrops(Level level, BlockPos pos, BlockState state) {
        if (isWarmEnough(level, pos)) {
            super.growCrops(level, pos, state);
        }
    }
}
