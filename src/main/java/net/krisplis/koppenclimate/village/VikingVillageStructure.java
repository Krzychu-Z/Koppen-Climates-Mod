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
 * A Viking village of the Subpolar taiga (Dfc): see VikingVillagePiece. It wants fairly even ground some 60 blocks
 * across, mostly dry (a bit of a fjord's edge is fine: the levelling banks it up), and levels a round platform for
 * itself; it turns to one of four ways at random.
 */
public class VikingVillageStructure extends Structure {
    public static final MapCodec<VikingVillageStructure> CODEC = simpleCodec(VikingVillageStructure::new);

    private static final int CHECK = 26, MAX_SPREAD = 14, MAX_WET = 3, MAX_BAD = 2;

    public VikingVillageStructure(StructureSettings settings) {
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
        // the biomes first (cheap): the middle in the taiga, and nearly all round it
        int bad = 0;
        for (int dx = -CHECK; dx <= CHECK; dx += CHECK) {
            for (int dz = -CHECK; dz <= CHECK; dz += CHECK) {
                var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx + dx), QuartPos.fromBlock(sea + 8),
                        QuartPos.fromBlock(cz + dz), randomState.sampler());
                if (!context.validBiome().test(biome) && (dx == 0 && dz == 0 || ++bad > MAX_BAD)) return Optional.empty();
            }
        }
        // then the ground at nine spots: fairly even, the middle dry and above the sea
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, count = 0, wet = 0;
        for (int dx = -CHECK; dx <= CHECK; dx += CHECK) {
            for (int dz = -CHECK; dz <= CHECK; dz += CHECK) {
                int x = cx + dx, z = cz + dz;
                int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, heights, randomState);
                int floor = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, heights, randomState);
                if (surface != floor || surface <= sea) {
                    if (dx == 0 && dz == 0 || ++wet > MAX_WET) return Optional.empty();
                    continue;
                }
                min = Math.min(min, surface);
                max = Math.max(max, surface);
                sum += surface;
                count++;
                if (max - min > MAX_SPREAD) return Optional.empty();
            }
        }
        if (count == 0) return Optional.empty();
        int floorY = Math.max(sea + 2, Math.round(sum / (float) count) - 1);
        BlockPos centre = new BlockPos(cx, floorY, cz);
        long layoutSeed = context.random().nextLong();
        int turn = context.random().nextInt(4);
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new VikingVillagePiece(centre, layoutSeed, turn))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.VIKING_VILLAGE.get();
    }
}
