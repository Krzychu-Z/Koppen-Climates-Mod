package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.WinePoleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The vineyard of the Wine Terraces (Csa): on every block of its terraced ground stand two wine poles, one on the
 * other, with a red grapevine grown up them. Runs once per chunk, over the columns that lie in the biome.
 */
public class WineTerraceFeature extends Feature<NoneFeatureConfiguration> {
    private static final ResourceKey<Biome> WINE_TERRACES = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "wine_terraces_csa"));

    public WineTerraceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int startX = context.origin().getX() & ~15, startZ = context.origin().getZ() & ~15;
        BlockState pole = ModBlocks.WINE_POLE.get().defaultBlockState().setValue(WinePoleBlock.GRAPE, WinePoleBlock.Grape.RED);
        boolean placed = false;
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                BlockPos pos = new BlockPos(x, y, z);
                if (!level.getBiome(pos).is(WINE_TERRACES)) continue;
                if (!level.getBlockState(pos.below()).is(ModBlocks.CINNAMON_DIRT.get())) continue;
                if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
                level.setBlock(pos, pole.setValue(WinePoleBlock.AGE, 2 + context.random().nextInt(2)), 2);
                level.setBlock(pos.above(), pole.setValue(WinePoleBlock.AGE, 2 + context.random().nextInt(2)), 2);
                placed = true;
            }
        }
        return placed;
    }
}
