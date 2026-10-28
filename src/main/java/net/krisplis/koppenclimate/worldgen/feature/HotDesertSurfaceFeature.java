package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The deserts' ground. The hot desert (BWh): nothing but orange sand, a few blocks of it over orange sandstone, with
 * drifted sand dunes (layers) rippling over the top. The continental desert (BWk): the same sand and sandstone,
 * without the dunes. In both, water is gone: rivers, ponds and springs that would show at the surface (and any lava
 * there) are filled with sand, so the river beds are dry sandy valleys. The desert mountains (BWk): plain, one-
 * coloured terracotta from their tops down to y 48 (their creeks keep their water: they're river biome).
 */
public class HotDesertSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SAND_DEPTH = 4;
    private static final int SANDSTONE_DEPTH = 18;
    // the desert mountains are terracotta down to here
    private static final int MOUNTAIN_ROCK_BOTTOM = 48;
    // water no deeper than this is filled in (rivers and ponds), deeper is the sea
    private static final int FILL_DEPTH = 9;

    public HotDesertSurfaceFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int startX = context.origin().getX() & ~15, startZ = context.origin().getZ() & ~15;
        BlockState sand = ModBlocks.ORANGE_SAND.get().defaultBlockState();
        BlockState sandstone = ModBlocks.ORANGE_SANDSTONE.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (top <= level.getMinBuildHeight()) continue;
                var biome = level.getBiome(pos.set(x, top, z));
                boolean hot = biome.is(ModBiomes.HOT_DESERT), mountains = biome.is(ModBiomes.DESERT_MOUNTAINS);
                if (!hot && !mountains && !biome.is(ModBiomes.CONTINENTAL_DESERT)) continue;
                // dry up the water (and lava) over it
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (surface > top && surface - top <= FILL_DEPTH) {
                    for (int y = top + 1; y <= surface; y++) {
                        BlockState s = level.getBlockState(pos.set(x, y, z));
                        if (!s.getFluidState().isEmpty() || s.canBeReplaced() || !s.blocksMotion()) {
                            level.setBlock(pos, mountains ? Blocks.TERRACOTTA.defaultBlockState() : sand, Block.UPDATE_CLIENTS);
                        }
                    }
                    top = surface;
                }
                if (mountains) {
                    // the desert mountains: bare, one-coloured terracotta, right through
                    BlockState rock = Blocks.TERRACOTTA.defaultBlockState();
                    for (int y = top; y >= Math.min(top - SANDSTONE_DEPTH, MOUNTAIN_ROCK_BOTTOM); y--) {
                        BlockState s = level.getBlockState(pos.set(x, y, z));
                        if ((!s.getFluidState().isEmpty() && y > top - SAND_DEPTH) || isGround(s)) {
                            if (s != rock) {
                                level.setBlock(pos, rock, Block.UPDATE_CLIENTS);
                                changed = true;
                            }
                        }
                    }
                    continue;
                }
                // sand, then sandstone under it
                for (int depth = 0; depth <= SANDSTONE_DEPTH; depth++) {
                    BlockState s = level.getBlockState(pos.set(x, top - depth, z));
                    boolean wet = !s.getFluidState().isEmpty();
                    if (s.isAir() && !wet) continue;   // a cave
                    if (wet || isGround(s)) {
                        BlockState r = depth <= SAND_DEPTH ? sand : sandstone;
                        if (s != r) {
                            level.setBlock(pos, r, Block.UPDATE_CLIENTS);
                            changed = true;
                        }
                    }
                }
                // the dunes: sand drifted into long, gentle ripples
                int layers = hot ? dune(x, z) : 0;
                pos.set(x, top + 1, z);
                if (layers > 0 && level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, ModBlocks.ORANGE_SAND_DUNE.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
        return changed;
    }

    /** How many layers of drifted sand lie here (0-7), in long wind ripples. */
    static int dune(int x, int z) {
        double v = 0.5 + 0.3 * Mth.sin((float) (0.11 * x + 0.04 * z + 1.7 * Math.sin(0.031 * z)))
                + 0.2 * Mth.sin((float) (0.083 * z - 0.05 * x + 1.3 * Math.sin(0.027 * x)));
        return Mth.clamp((int) Math.round(v * 7.0), 0, 7);
    }

    private static boolean isGround(BlockState s) {
        return s.is(BlockTags.SAND) || s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.SANDSTONE) || s.is(Blocks.SMOOTH_SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.CLAY)
                || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.CALCITE);
    }
}
