package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

// Reshapes the ground of one biome right after the terrain and vanilla surface are built
// (it runs first, in the raw_generation step, once per chunk). Vanilla's surface rules can't be changed
// per biome without replacing the whole overworld noise settings, so this does it per column instead,
// touching only columns whose surface lies in the configured biome:
//   jungle: grass -> oxisol grass, dirt -> oxisol, with patches of podzol
//   river:  a riverbed and banks of dirt, podzol and oxisol, never sand
//   beach:  sand -> white sand
// and in all three, stone in the top LIMESTONE_DEPTH blocks becomes yellow limestone.
public class BiomeSurfaceFeature extends Feature<BiomeSurfaceFeature.Config> {
    private static final int LIMESTONE_DEPTH = 15;
    // how far down soil and sand are rewritten below the surface
    private static final int SOIL_DEPTH = 5;

    public BiomeSurfaceFeature(Codec<Config> codec) {
        super(codec);
    }

    public enum Style implements StringRepresentable {
        JUNGLE("jungle"), RIVER("river"), BEACH("beach");

        public static final Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);
        private final String name;

        Style(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public record Config(ResourceKey<Biome> biome, Style style) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(Config::biome),
                Style.CODEC.fieldOf("style").forGetter(Config::style)
        ).apply(instance, Config::new));
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();
        // with no placement modifiers the origin is the chunk's corner; stay inside this chunk
        int startX = context.origin().getX() & ~15, startZ = context.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean changed = false;

        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                // topmost solid block, under water if there is any
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (top <= level.getMinBuildHeight()) continue;
                pos.set(x, top, z);
                if (!level.getBiome(pos).is(config.biome())) continue;

                boolean underwater = level.getFluidState(pos.above()).is(FluidTags.WATER);
                for (int depth = 0; depth <= LIMESTONE_DEPTH; depth++) {
                    pos.set(x, top - depth, z);
                    BlockState state = level.getBlockState(pos);
                    BlockState replacement = depth <= SOIL_DEPTH ? soil(config.style(), state, depth, underwater, x, z) : null;
                    if (replacement == null && state.is(Blocks.STONE)) {
                        replacement = ModBlocks.YELLOW_LIMESTONE.get().defaultBlockState();
                    }
                    if (replacement != null && replacement != state) {
                        level.setBlock(pos, replacement, Block.UPDATE_CLIENTS);
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

    // What a soil or sand block becomes in this biome, or null to leave it alone
    private static BlockState soil(Style style, BlockState state, int depth, boolean underwater, int x, int z) {
        boolean podzolPatch = patch(x, z, 5);
        return switch (style) {
            case JUNGLE -> {
                if (depth == 0 && state.is(Blocks.GRASS_BLOCK)) {
                    yield podzolPatch ? Blocks.PODZOL.defaultBlockState() : ModBlocks.OXISOL_GRASS.get().defaultBlockState();
                }
                if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)) yield ModBlocks.OXISOL.get().defaultBlockState();
                yield null;
            }
            case RIVER -> {
                boolean loose = state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)
                        || state.is(Blocks.SANDSTONE) || state.is(Blocks.DIRT);
                if (depth == 0 && !underwater && state.is(Blocks.GRASS_BLOCK)) {
                    yield podzolPatch ? Blocks.PODZOL.defaultBlockState() : ModBlocks.OXISOL_GRASS.get().defaultBlockState();
                }
                if (!loose) yield null;
                if (depth == 0) {
                    // the riverbed and bare banks: a mix of dirt, podzol and oxisol
                    yield podzolPatch ? Blocks.PODZOL.defaultBlockState()
                            : patch(x, z, 2) ? Blocks.DIRT.defaultBlockState() : ModBlocks.OXISOL.get().defaultBlockState();
                }
                yield ModBlocks.OXISOL.get().defaultBlockState();
            }
            case BEACH -> {
                if (state.is(Blocks.SAND)) yield ModBlocks.WHITE_SAND.get().defaultBlockState();
                yield null;
            }
        };
    }

    // Blotchy patches about 4 blocks across: true for roughly 1 in `oneIn` of them
    private static boolean patch(int x, int z, int oneIn) {
        return Math.floorMod(Mth.getSeed(x >> 2, 0, z >> 2), oneIn) == 0;
    }
}
