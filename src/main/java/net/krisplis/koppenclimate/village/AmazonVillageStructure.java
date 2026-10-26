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
 * A small Amazon tribal village: a round clearing of trampled earth about 50 blocks across, limestone huts with
 * bamboo roofs standing in a ring, and a well and a fire pit in the middle. It only goes on fairly level dry
 * ground; the structure's terrain adaptation then flattens the clearing.
 */
public class AmazonVillageStructure extends Structure {
    public static final MapCodec<AmazonVillageStructure> CODEC = simpleCodec(AmazonVillageStructure::new);

    // how far from the centre the ground is checked
    private static final int CHECK_RADIUS = 18;
    // the most the ground under the clearing may rise and fall
    private static final int MAX_HEIGHT_SPREAD = 10;

    public AmazonVillageStructure(StructureSettings settings) {
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
                builder -> builder.addPiece(new AmazonVillagePiece(centre, layoutSeed))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.AMAZON_VILLAGE.get();
    }
}
