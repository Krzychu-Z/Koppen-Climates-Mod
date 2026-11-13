package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A Lakota camp on the Prairie (Dfa): a circle of tipis about 50 blocks across, open to the east, with the council
 * lodge in its middle, a horse corral and drying racks. It only goes on fairly level
 * ground wholly inside the Prairie; the structure's terrain adaptation then flattens the yard.
 */
public class LakotaCampStructure extends Structure {
    public static final MapCodec<LakotaCampStructure> CODEC = simpleCodec(LakotaCampStructure::new);

    // how far from the centre the ground is checked
    private static final int CHECK_RADIUS = 22;
    // the most the ground under the clearing may rise and fall
    private static final int MAX_HEIGHT_SPREAD = 8;

    public LakotaCampStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX();
        int cz = chunk.getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heights = context.heightAccessor();
        RandomState randomState = context.randomState();

        // a cheap first test, before any of the costly ground heights: the middle must be in the right biome
        if (!context.validBiome().test(context.biomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(cx),
                net.minecraft.core.QuartPos.fromBlock(context.chunkGenerator().getSeaLevel() + 4),
                net.minecraft.core.QuartPos.fromBlock(cz), context.randomState().sampler()))) {
            return Optional.empty();
        }
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, count = 0;
        for (int dx = -CHECK_RADIUS; dx <= CHECK_RADIUS; dx += CHECK_RADIUS) {
            for (int dz = -CHECK_RADIUS; dz <= CHECK_RADIUS; dz += CHECK_RADIUS) {
                int x = cx + dx, z = cz + dz;
                int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
                int floor = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
                // water on top: a lake, river or the sea
                if (surface != floor || surface < generator.getSeaLevel() - 1) {
                    return Optional.empty();
                }
                if (!context.validBiome().test(context.biomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(x),
                        net.minecraft.core.QuartPos.fromBlock(surface), net.minecraft.core.QuartPos.fromBlock(z), randomState.sampler()))) {
                    return Optional.empty();
                }
                min = Math.min(min, surface);
                max = Math.max(max, surface);
                sum += surface;
                count++;
            }
        }
        if (max - min > MAX_HEIGHT_SPREAD) {
            return Optional.empty();
        }
        int floorY = Math.max(generator.getSeaLevel(), Math.round(sum / (float) count));
        BlockPos centre = new BlockPos(cx, floorY, cz);
        long layoutSeed = context.random().nextLong();
        return Optional.of(new GenerationStub(centre,
                builder -> builder.addPiece(new LakotaCampPiece(centre, layoutSeed))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.LAKOTA_CAMP.get();
    }
}
