package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A layer of drifted sand, one to eight eighths of a block deep, like a snow layer but it never
 * melts: the wind-blown ripples and dunes on top of the hot desert's sand. Placed on itself it piles up a layer.
 */
public class SandDuneBlock extends SnowLayerBlock {
    public static final MapCodec<SandDuneBlock> CODEC = simpleCodec(SandDuneBlock::new);

    public SandDuneBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<SnowLayerBlock> codec() {
        return (MapCodec) CODEC;
    }

    // sand doesn't melt in the light
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }
}
