package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The lavender of the Lavender Field (Csa): on about every second block of its open ground, a lavender plant.
 * Runs once per chunk, over the columns that lie in the biome.
 */
public class LavenderFieldFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<Biome> LAVENDER_FIELD = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "lavender_field_csa"));
    private static final float COVER = 0.5F;

    public LavenderFieldFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        int startX = context.origin().getX() & ~15, startZ = context.origin().getZ() & ~15;
        BlockState lavender = ModBlocks.LAVENDER.get().defaultBlockState();
        boolean placed = false;
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                if (random.nextFloat() >= COVER) continue;
                BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
                if (!level.getBiome(pos).is(LAVENDER_FIELD)) continue;
                if (!level.isEmptyBlock(pos) || !level.getBlockState(pos.below()).is(BlockTags.DIRT)) continue;
                level.setBlock(pos, lavender, 2);
                placed = true;
            }
        }
        return placed;
    }
}
