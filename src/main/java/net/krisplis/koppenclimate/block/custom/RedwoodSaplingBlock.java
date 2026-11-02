package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.worldgen.feature.RedwoodTreeFeature;
import net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * The redwood's sapling. A redwood is never small - the least of them is four blocks across - so one sapling alone
 * never grows: it takes nine of them, planted in a 3x3 square, every one of them on podzol. When one of the nine is
 * ready (by itself or with bone meal), all nine give way to a young giant (RedwoodTreeFeature), centred on the square.
 */
public class RedwoodSaplingBlock extends SaplingBlock {
    public static final MapCodec<RedwoodSaplingBlock> CODEC = simpleCodec(RedwoodSaplingBlock::new);

    public RedwoodSaplingBlock(Properties properties) {
        super(ModTreeGrowers.REDWOOD, properties);
    }

    @Override
    public MapCodec<? extends SaplingBlock> codec() {
        return CODEC;
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
            return;
        }
        BlockPos corner = this.square(level, pos);
        if (corner == null) return;
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) level.setBlock(corner.offset(dx, 0, dz), Blocks.AIR.defaultBlockState(), 4);
        }
        if (!RedwoodTreeFeature.grow(level, corner.offset(1, 0, 1), random, true)) {
            // no room for it: the saplings stay as they were
            for (int dx = 0; dx < 3; dx++) {
                for (int dz = 0; dz < 3; dz++) level.setBlock(corner.offset(dx, 0, dz), state, 4);
            }
        }
    }

    /** The north-west corner of a full 3x3 square of redwood saplings on podzol that includes this one, if any. */
    @Nullable
    private BlockPos square(ServerLevel level, BlockPos pos) {
        for (int ox = -2; ox <= 0; ox++) {
            for (int oz = -2; oz <= 0; oz++) {
                BlockPos corner = pos.offset(ox, 0, oz);
                if (this.fullSquare(level, corner)) return corner;
            }
        }
        return null;
    }

    private boolean fullSquare(ServerLevel level, BlockPos corner) {
        for (int dx = 0; dx < 3; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                BlockPos p = corner.offset(dx, 0, dz);
                if (!level.getBlockState(p).is(this) || !level.getBlockState(p.below()).is(Blocks.PODZOL)) return false;
            }
        }
        return true;
    }
}
