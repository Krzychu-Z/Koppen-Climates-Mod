package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A lone limestone outcrop standing up out of the English countryside's (Cfb) fields: a few weathered boulders of
 * white limestone piled one on another, four to nine blocks high, with moss and rubble about its foot.
 */
public class LimestoneTorFeature extends Feature<NoneFeatureConfiguration> {
    public LimestoneTorFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) - 1;
        BlockPos ground = new BlockPos(origin.getX(), top, origin.getZ());
        if (top < 63 || !level.getBlockState(ground).is(BlockTags.DIRT)) return false;
        BlockState rock = ModBlocks.WHITE_LIMESTONE.get().defaultBlockState();
        BlockState rubble = ModBlocks.COBBLED_WHITE_LIMESTONE.get().defaultBlockState();
        BlockState mossy = ModBlocks.MOSSY_COBBLED_WHITE_LIMESTONE.get().defaultBlockState();
        // the boulders, each sitting on the last, a little off to one side, growing smaller upwards
        int boulders = 2 + random.nextInt(3);
        float radius = 2.0F + random.nextFloat() * 0.8F;
        double cx = ground.getX() + 0.5, cz = ground.getZ() + 0.5;
        int y = top - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int b = 0; b < boulders; b++) {
            float squash = 0.7F + random.nextFloat() * 0.3F;
            int r = Mth.ceil(radius);
            int h = Math.max(2, Math.round(radius * 2.0F * squash));
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int dy = 0; dy < h; dy++) {
                        double ny = (dy + 0.5 - h / 2.0) / (h / 2.0);
                        double d = (dx * dx + dz * dz) / (double) (radius * radius) + ny * ny * 0.8;
                        if (d > 1.0 - random.nextFloat() * 0.12) continue;
                        pos.set(Mth.floor(cx) + dx, y + dy, Mth.floor(cz) + dz);
                        BlockState here = level.getBlockState(pos);
                        if (!here.isAir() && !here.canBeReplaced() && !here.is(BlockTags.DIRT)) continue;
                        level.setBlock(pos, random.nextInt(9) == 0 ? rubble : rock, Block.UPDATE_CLIENTS);
                    }
                }
            }
            y += h - 1;
            cx += (random.nextFloat() - 0.5F) * radius;
            cz += (random.nextFloat() - 0.5F) * radius;
            radius *= 0.7F + random.nextFloat() * 0.15F;
            if (radius < 1.0F) break;
        }
        // rubble and moss about its foot
        for (int i = 0; i < 6; i++) {
            int dx = random.nextInt(7) - 3, dz = random.nextInt(7) - 3;
            int t = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, ground.getX() + dx, ground.getZ() + dz);
            pos.set(ground.getX() + dx, t, ground.getZ() + dz);
            if (level.getBlockState(pos.below()).is(BlockTags.DIRT) && level.getBlockState(pos).canBeReplaced()) {
                level.setBlock(pos, random.nextBoolean() ? mossy : rubble, Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }
}
