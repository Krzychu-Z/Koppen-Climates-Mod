package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.worldgen.biome.AlpineTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Alpine valley's stream (once per chunk): AlpineTerrain sinks the stream's bed a block (in a pond two) under the
 * valley floor and the surface rule lines it with white gravel; this fills it with water up to the valley's level,
 * flush with the grass either side, and now and then lets a couple of cod into it.
 */
public class AlpineStreamFeature extends Feature<NoneFeatureConfiguration> {
    public AlpineStreamFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        int top = AlpineTerrain.VALLEY_Y;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos fishAt = null;
        int water = 0;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int bed = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (bed < top - 2 || bed > top - 1) continue;
                if (!level.getBlockState(pos.set(x, bed, z)).is(ModBlocks.WHITE_GRAVEL.get())) continue;
                for (int y = bed + 1; y <= top; y++) {
                    if (!level.getBlockState(pos.set(x, y, z)).isAir()) break;
                    level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                    water++;
                    if (bed == top - 2 || random.nextInt(water) == 0) fishAt = pos.immutable();
                }
            }
        }
        if (water == 0) return false;
        // a couple of cod in the stream in a chunk out of five, more often in a pond
        if (fishAt != null && random.nextInt(5) == 0) {
            int n = 1 + random.nextInt(2);
            for (int i = 0; i < n; i++) {
                Cod cod = EntityType.COD.create(level.getLevel());
                if (cod == null) break;
                cod.moveTo(fishAt.getX() + 0.5, fishAt.getY() + 0.1, fishAt.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                cod.finalizeSpawn(level, level.getCurrentDifficultyAt(fishAt), MobSpawnType.CHUNK_GENERATION, null);
                cod.setPersistenceRequired();
                level.addFreshEntity(cod);
            }
        }
        return true;
    }
}
