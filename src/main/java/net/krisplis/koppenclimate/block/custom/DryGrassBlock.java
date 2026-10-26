package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The savanna's dry yellow grass. Like grass, but bone meal grows it into tall dry grass. */
public class DryGrassBlock extends TallGrassBlock {
    public static final MapCodec<TallGrassBlock> CODEC = simpleCodec(DryGrassBlock::new);

    public DryGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TallGrassBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        DoublePlantBlock.placeAt(level, ModBlocks.TALL_DRY_GRASS.get().defaultBlockState(), pos, 2);
    }
}
