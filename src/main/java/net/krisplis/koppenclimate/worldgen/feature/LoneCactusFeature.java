package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A single one-block cactus standing alone on the Mediterranean's grass. Cactus only grows on sand, so the one
 * block under it (hidden beneath the cactus) is turned to sand.
 */
public class LoneCactusFeature extends Feature<NoneFeatureConfiguration> {
    public LoneCactusFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        if (!level.isEmptyBlock(pos) || !level.getBlockState(pos.below()).is(BlockTags.DIRT)) return false;
        // no neighbours: a cactus breaks next to anything solid
        for (BlockPos n : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
            if (!level.isEmptyBlock(n)) return false;
        }
        level.setBlock(pos.below(), Blocks.SAND.defaultBlockState(), 2);
        level.setBlock(pos, Blocks.CACTUS.defaultBlockState(), 2);
        return true;
    }
}
