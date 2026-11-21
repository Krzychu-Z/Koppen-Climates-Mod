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

import java.util.Optional;

/**
 * The Alpine valley's stream (once per chunk): cut block by block into the valley's flat floor where AlpineTerrain
 * says it runs (meandering about the valley's middle, swelling into pools), a bed of white gravel under 1-3 blocks
 * of water standing flush with the grass either side, a few gravel and cobble stones along its banks; and now and
 * then a couple of cod in it.
 */
public class AlpineStreamFeature extends Feature<NoneFeatureConfiguration> {
    public AlpineStreamFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        Optional<AlpineTerrain> alps = AlpineTerrain.of(level.getLevel().getChunkSource().randomState());
        if (alps.isEmpty()) return false;
        int x0 = context.origin().getX() & ~15, z0 = context.origin().getZ() & ~15;
        int top = AlpineTerrain.VALLEY_Y;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos fishAt = null;
        int water = 0;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                if (level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1 != top) continue;
                int depth = alps.get().streamDepth(x, z);
                if (depth <= 0) {
                    // the banks: here and there a patch of white gravel or a stone at the water's edge
                    if (alps.get().nearStream(x, z, 1.3) && random.nextInt(3) == 0) {
                        level.setBlock(pos.set(x, top, z), random.nextInt(4) == 0 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                                : ModBlocks.WHITE_GRAVEL.get().defaultBlockState(), 2);
                        level.setBlock(pos.set(x, top + 1, z), Blocks.AIR.defaultBlockState(), 2);
                    }
                    continue;
                }
                level.setBlock(pos.set(x, top - depth, z), ModBlocks.WHITE_GRAVEL.get().defaultBlockState(), 2);
                for (int y = top - depth + 1; y <= top; y++) {
                    level.setBlock(pos.set(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                    water++;
                }
                // (and nothing standing in it)
                level.setBlock(pos.set(x, top + 1, z), Blocks.AIR.defaultBlockState(), 2);
                if (depth >= 2 && random.nextInt(water) == 0) fishAt = new BlockPos(x, top - 1, z);
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
