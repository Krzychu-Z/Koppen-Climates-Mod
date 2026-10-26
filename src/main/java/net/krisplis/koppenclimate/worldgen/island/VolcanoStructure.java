package net.krisplis.koppenclimate.worldgen.island;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.worldgen.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.List;
import java.util.Optional;

/**
 * The crater of a tropical volcanic island, as a structure, so islands can be found with
 * {@code /locate structure koppenclimatemod:tropical_island}. The island itself is terrain (VolcanicIslands);
 * this marks its top and dresses the crater: lava and magma for an active volcano, grass with old volcanic rock
 * for an extinct one.
 *
 * <p>Its structure set tries one chunk in every 4x4; this takes that chance if an island's crater centre lies
 * in the same 4x4 chunks, so each island is found exactly once.
 */
public class VolcanoStructure extends Structure {
    public static final MapCodec<VolcanoStructure> CODEC = simpleCodec(VolcanoStructure::new);
    private static final int REGION_CHUNKS = 4;

    public VolcanoStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        Climate.Sampler sampler = context.randomState().sampler();
        IslandMap map = IslandMap.forSeed(IslandMap.layoutSeed(sampler.temperature().compute(
                new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z))));
        ChunkPos chunk = context.chunkPos();
        int regionX = Math.floorDiv(chunk.x, REGION_CHUNKS) * REGION_CHUNKS * 16;
        int regionZ = Math.floorDiv(chunk.z, REGION_CHUNKS) * REGION_CHUNKS * 16;
        List<IslandMap.Island> islands = map.islandsCentredIn(regionX, regionZ,
                regionX + REGION_CHUNKS * 16 - 1, regionZ + REGION_CHUNKS * 16 - 1, probe(sampler));
        if (islands.isEmpty()) {
            return Optional.empty();
        }
        IslandMap.Island island = islands.get(0);
        BlockPos top = new BlockPos(island.x(), IslandMap.SEA_LEVEL + (int) Math.round(island.rimHeight()), island.z());
        return Optional.of(new GenerationStub(top, builder -> builder.addPiece(new VolcanoPiece(island))));
    }

    /** The overworld's climate, read straight from the world's noise. */
    public static IslandMap.ClimateProbe probe(Climate.Sampler sampler) {
        return new IslandMap.ClimateProbe() {
            @Override
            public double continentalness(int x, int z) {
                return sampler.continentalness().compute(new DensityFunction.SinglePointContext(x, 0, z));
            }

            @Override
            public double temperature(int x, int z) {
                return sampler.temperature().compute(new DensityFunction.SinglePointContext(x, 0, z));
            }

            @Override
            public double humidity(int x, int z) {
                return sampler.humidity().compute(new DensityFunction.SinglePointContext(x, 0, z));
            }
        };
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.VOLCANO.get();
    }
}
