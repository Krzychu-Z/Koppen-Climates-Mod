package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A Tuscan hill town of the Mediterranean (Csa): see TuscanVillagePiece. It needs dry, fairly even ground about
 * a hundred blocks across, all of it in the Csa land biomes it may stand in (not the beach, river or lavender
 * fields), so that the town and its fields fit.
 */
public class TuscanVillageStructure extends Structure {
    public static final MapCodec<TuscanVillageStructure> CODEC = simpleCodec(TuscanVillageStructure::new);

    private static final int CHECK_RADIUS = 44;
    private static final int CHECK_STEP = 22;
    private static final int MAX_HEIGHT_SPREAD = 18;
    // how many of the 25 spots checked may be wet or in another biome (the town's earthworks cover them)
    private static final int MAX_BAD = 5;

    public TuscanVillageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor heights = context.heightAccessor();
        RandomState randomState = context.randomState();

        int sea = generator.getSeaLevel();
        // cheap first: the biomes over the town's spot (taken at about ground level); the middle must be right
        int bad = 0;
        for (int dx = -CHECK_RADIUS; dx <= CHECK_RADIUS; dx += CHECK_STEP) {
            for (int dz = -CHECK_RADIUS; dz <= CHECK_RADIUS; dz += CHECK_STEP) {
                var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx + dx), QuartPos.fromBlock(sea + 8),
                        QuartPos.fromBlock(cz + dz), randomState.sampler());
                if (!context.validBiome().test(biome) && (dx == 0 && dz == 0 || ++bad > MAX_BAD)) return Optional.empty();
            }
        }
        // then the ground, at nine spots only (finding the ground is costly): dry, and fairly even
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, count = 0, wet = 0;
        for (int dx = -CHECK_RADIUS; dx <= CHECK_RADIUS; dx += CHECK_RADIUS) {
            for (int dz = -CHECK_RADIUS; dz <= CHECK_RADIUS; dz += CHECK_RADIUS) {
                int x = cx + dx, z = cz + dz;
                int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
                int floor = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
                if (surface != floor || surface < sea) {
                    if (dx == 0 && dz == 0 || ++wet > 2) return Optional.empty();
                    continue;
                }
                min = Math.min(min, surface);
                max = Math.max(max, surface);
                sum += surface;
                count++;
                if (max - min > MAX_HEIGHT_SPREAD) return Optional.empty();
            }
        }
        if (max - min > MAX_HEIGHT_SPREAD) return Optional.empty();
        int floorY = Math.round(sum / (float) count) - 1;
        BlockPos centre = new BlockPos(cx, floorY, cz);
        long layoutSeed = context.random().nextLong();
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new TuscanVillagePiece(centre, layoutSeed))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.TUSCAN_VILLAGE.get();
    }
}
