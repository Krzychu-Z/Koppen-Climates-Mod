package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The dry bush of the Lush desert (BSh): a dense, woody shrub like wild rosemary (just as hard to push through, and
 * it gives sticks), but grey-green, in the colour of the grass around it, and at home on sand.
 */
public class DryBushBlock extends RosemaryBlock {
    public static final MapCodec<DryBushBlock> CODEC = simpleCodec(DryBushBlock::new);

    public DryBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.TERRACOTTA);
    }
}
