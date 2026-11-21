package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Ice cap (EF), once per chunk: a perfectly flat ice shelf laid over the frozen sea - 20 blocks of packed ice
 * from just above the water, 5 of snow on top - over every column of the chunk that is the Ice cap's. The sea goes
 * on under it (dig down through the ice and you come out into the water), and where the Ice cap ends, the shelf
 * ends in a sheer wall of ice over the open sea.
 */
public class IceCapFeature extends Feature<NoneFeatureConfiguration> {
    public static final int ICE_FROM = 64, ICE = 20, SNOW = 5;

    public IceCapFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState ice = Blocks.PACKED_ICE.defaultBlockState(), snow = Blocks.SNOW_BLOCK.defaultBlockState();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (!level.getBiome(pos.set(x, ICE_FROM, z)).is(ModBiomes.ICE_CAP)) continue;
                for (int y = ICE_FROM; y < ICE_FROM + ICE + SNOW; y++) {
                    level.setBlock(pos.set(x, y, z), y < ICE_FROM + ICE ? ice : snow, 2);
                }
                // (nothing left standing on it)
                for (int y = ICE_FROM + ICE + SNOW; y < ICE_FROM + ICE + SNOW + 8; y++) {
                    if (level.getBlockState(pos.set(x, y, z)).isAir()) break;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                any = true;
            }
        }
        return any;
    }
}
