package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.DeadBushBlock;

/**
 * A tuft of silvery-green desert grass, growing out of the sand of the continental desert (BWk) and the desert
 * mountains' terracotta. It takes root wherever a dead bush would: sand, terracotta or dirt.
 */
public class SilverGrassBlock extends DeadBushBlock {
    public static final MapCodec<SilverGrassBlock> CODEC = simpleCodec(SilverGrassBlock::new);

    public SilverGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<DeadBushBlock> codec() {
        return (MapCodec) CODEC;
    }
}
