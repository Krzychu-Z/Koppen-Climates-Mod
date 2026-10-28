package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.tags.BiomeTags;

import java.util.Optional;

/**
 * A little Dalmatian island town (CroatianVillagePiece), built on a rock in the sea just off a Mediterranean beach.
 * It needs open water some forty blocks across, not too deep, with the Mediterranean beach within reach; its harbour
 * faces that beach.
 */
public class CroatianVillageStructure extends Structure {
    public static final MapCodec<CroatianVillageStructure> CODEC = simpleCodec(CroatianVillageStructure::new);

    private static final int ISLAND_CHECK = 18;
    private static final int MAX_DEPTH = 24;
    private static final int BEACH_SEARCH = 32, BEACH_STEP = 8;
    // how far out from the beach the island's middle may lie
    private static final int[] ISLAND_DISTANCES = {36, 48};

    public CroatianVillageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heights = context.heightAccessor();
        RandomState randomState = context.randomState();
        int sea = generator.getSeaLevel();
        // cheap checks first (biomes), the costly ones (terrain heights) last and few:
        // 1. a bit of Mediterranean beach near this chunk
        BlockPos beach = null;
        search:
        for (int dx = -BEACH_SEARCH; dx <= BEACH_SEARCH; dx += BEACH_STEP) {
            for (int dz = -BEACH_SEARCH; dz <= BEACH_SEARCH; dz += BEACH_STEP) {
                int x = chunk.getMiddleBlockX() + dx, z = chunk.getMiddleBlockZ() + dz;
                if (biomeAt(context, x, sea, z).is(ModBiomes.MEDITERRANEAN_BEACH)) {
                    beach = new BlockPos(x, sea, z);
                    break search;
                }
            }
        }
        if (beach == null) return Optional.empty();
        // 2. which way the open sea lies from it
        for (Direction out : Direction.Plane.HORIZONTAL) {
            if (!biomeAt(context, beach.getX() + out.getStepX() * 40, sea, beach.getZ() + out.getStepZ() * 40).is(BiomeTags.IS_OCEAN)) continue;
            // 3. the island's spot out there: open water, neither too shallow nor too deep
            for (int distance : ISLAND_DISTANCES) {
                int cx = beach.getX() + out.getStepX() * distance, cz = beach.getZ() + out.getStepZ() * distance;
                if (!this.openSea(cx, cz, generator, heights, randomState, sea)) continue;
                BlockPos centre = new BlockPos(cx, sea - 1, cz);
                Direction harbour = out.getOpposite();
                long layoutSeed = context.random().nextLong();
                return Optional.of(new GenerationStub(beach,
                        builder -> builder.addPiece(new CroatianVillagePiece(centre, layoutSeed, harbour))));
            }
        }
        return Optional.empty();
    }

    private static net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeAt(GenerationContext context, int x, int y, int z) {
        return context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                context.randomState().sampler());
    }

    /** Water all over the island's spot (its middle and four points round it), neither too shallow nor too deep. */
    private boolean openSea(int cx, int cz, ChunkGenerator generator, LevelHeightAccessor heights, RandomState randomState, int sea) {
        int[][] spots = {{0, 0}, {ISLAND_CHECK, 0}, {-ISLAND_CHECK, 0}, {0, ISLAND_CHECK}, {0, -ISLAND_CHECK}};
        for (int[] d : spots) {
            int floor = generator.getFirstOccupiedHeight(cx + d[0], cz + d[1], Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
            if (floor > sea - 2 || floor < sea - MAX_DEPTH) return false;
        }
        return true;
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.CROATIAN_VILLAGE.get();
    }
}
