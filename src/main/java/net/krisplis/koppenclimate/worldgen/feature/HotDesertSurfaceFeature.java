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
 * coloured terracotta from their tops down to y 48 (their creeks keep their water: they're river biome). The Lush
 * desert (BSh) and its ghost towns: the continental desert's sand and sandstone, patched with coarse dirt; its rivers
 * keep a little water, but only down low (at sea level), and only a block deep - any higher up, they're dry too.
 * The Dry steppe (BSk): the continental desert's ground and dry rivers, but of ordinary (yellow) sand over the
 * sandstone, with a tuft of grass growing straight out of the sand on every single block (BushBlockSandMixin).
 */
public class HotDesertSurfaceFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SAND_DEPTH = 4;
    private static final int SANDSTONE_DEPTH = 18;
    // the Dry steppe: sand this deep (counting from the top), then the sandstone
    private static final int STEPPE_SAND_DEPTH = 3;
    // the desert mountains are terracotta down to here
    private static final int MOUNTAIN_ROCK_BOTTOM = 48;
    // water no deeper than this is filled in (rivers and ponds), deeper is the sea
    private static final int FILL_DEPTH = 9;
    // the Lush desert's rivers keep their water at this height and below
    private static final int LUSH_WATER_MAX_Y = 62;

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
                boolean lush = ModBiomes.isLushDesert(biome), steppe = biome.is(ModBiomes.DRY_STEPPE);
                if (!hot && !mountains && !lush && !steppe && !biome.is(ModBiomes.CONTINENTAL_DESERT)) continue;
                // dry up the water (and lava) over it
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                boolean shallowRiver = lush && surface <= LUSH_WATER_MAX_Y
                        && level.getBlockState(pos.set(x, surface, z)).getFluidState().is(net.minecraft.tags.FluidTags.WATER);
                if (surface > top && surface - top <= FILL_DEPTH) {
                    // (the Lush desert's low rivers: filled up to a block under their surface)
                    int fillTo = shallowRiver ? surface - 1 : surface;
                    for (int y = top + 1; y <= fillTo; y++) {
                        BlockState s = level.getBlockState(pos.set(x, y, z));
                        if (!s.getFluidState().isEmpty() || s.canBeReplaced() || !s.blocksMotion()) {
                            level.setBlock(pos, mountains ? Blocks.TERRACOTTA.defaultBlockState()
                                    : steppe ? Blocks.SAND.defaultBlockState() : sand, Block.UPDATE_CLIENTS);
                        }
                    }
                    top = fillTo;
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
                if (steppe) {
                    if (this.steppeGround(level, pos, x, z, top, sandstone)) changed = true;
                    continue;
                }
                // sand, then sandstone under it
                for (int depth = 0; depth <= SANDSTONE_DEPTH; depth++) {
                    BlockState s = level.getBlockState(pos.set(x, top - depth, z));
                    boolean wet = !s.getFluidState().isEmpty();
                    if (s.isAir() && !wet) continue;   // a cave
                    if (wet || isGround(s)) {
                        BlockState r = depth <= SAND_DEPTH ? sand : sandstone;
                        // the Lush desert's ground is patched with coarse dirt (never under water)
                        if (lush && depth <= 1 && !wet && top >= LUSH_WATER_MAX_Y && coarse(x, z)) r = Blocks.COARSE_DIRT.defaultBlockState();
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

    /** The Dry steppe's ground: sand, then sandstone, and a tuft of grass on top of it. */
    private boolean steppeGround(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int top, BlockState sandstone) {
        boolean changed = false;
        for (int depth = 0; depth <= SANDSTONE_DEPTH; depth++) {
            BlockState s = level.getBlockState(pos.set(x, top - depth, z));
            boolean wet = !s.getFluidState().isEmpty();
            if (s.isAir() && !wet) continue;   // a cave
            if (wet || isGround(s)) {
                BlockState r = depth <= STEPPE_SAND_DEPTH ? Blocks.SAND.defaultBlockState() : sandstone;
                if (s != r) {
                    level.setBlock(pos, r, Block.UPDATE_CLIENTS);
                    changed = true;
                }
            }
        }
        // the grass: one block tall, on every block of it
        if (level.getBlockState(pos.set(x, top, z)).is(Blocks.SAND) && level.getBlockState(pos.set(x, top + 1, z)).isAir()) {
            level.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
            changed = true;
        }
        return changed;
    }

    /** How many layers of drifted sand lie here (0-7), in long wind ripples. */
    static int dune(int x, int z) {
        double v = 0.5 + 0.3 * Mth.sin((float) (0.11 * x + 0.04 * z + 1.7 * Math.sin(0.031 * z)))
                + 0.2 * Mth.sin((float) (0.083 * z - 0.05 * x + 1.3 * Math.sin(0.027 * x)));
        return Mth.clamp((int) Math.round(v * 7.0), 0, 7);
    }

    /** Where the Lush desert's ground is coarse dirt: irregular patches, about a quarter of it. */
    static boolean coarse(int x, int z) {
        double v = Mth.sin((float) (0.13 * x + 0.9 * Math.sin(0.07 * z))) * Mth.sin((float) (0.11 * z + 0.8 * Math.sin(0.09 * x)))
                + 0.25 * Mth.sin((float) (0.41 * x - 0.37 * z));
        return v > 0.35;
    }

    private static boolean isGround(BlockState s) {
        return s.is(BlockTags.SAND) || s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.GRAVEL)
                || s.is(Blocks.SANDSTONE) || s.is(Blocks.SMOOTH_SANDSTONE) || s.is(Blocks.RED_SANDSTONE) || s.is(Blocks.CLAY)
                || s.is(BlockTags.TERRACOTTA) || s.is(Blocks.CALCITE);
    }
}
