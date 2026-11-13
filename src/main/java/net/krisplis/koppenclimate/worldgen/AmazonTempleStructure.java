package net.krisplis.koppenclimate.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;

import java.util.Optional;

/**
 * The vanilla jungle temple, for the Amazon jungle. Vanilla's own refuses any spot whose lowest corner is below
 * sea level, and the Amazon is flat lowland that mostly sits right at sea level, so it almost never got one.
 * This builds the very same temple (same piece, same loot, same traps), and only asks for dry ground.
 */
public class AmazonTempleStructure extends Structure {
    public static final MapCodec<AmazonTempleStructure> CODEC = simpleCodec(AmazonTempleStructure::new);

    // the temple's footprint
    private static final int WIDTH = 12;
    private static final int DEPTH = 15;

    public AmazonTempleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        ChunkGenerator generator = context.chunkGenerator();
        // a cheap first test, before any of the costly ground heights: the middle must be in the right biome
        if (!context.validBiome().test(context.biomeSource().getNoiseBiome(net.minecraft.core.QuartPos.fromBlock(chunk.getMiddleBlockX()),
                net.minecraft.core.QuartPos.fromBlock(context.chunkGenerator().getSeaLevel() + 4),
                net.minecraft.core.QuartPos.fromBlock(chunk.getMiddleBlockZ()), context.randomState().sampler()))) {
            return Optional.empty();
        }
        int[][] corners = {{0, 0}, {WIDTH, 0}, {0, DEPTH}, {WIDTH, DEPTH}};
        for (int[] corner : corners) {
            int x = chunk.getMinBlockX() + corner[0], z = chunk.getMinBlockZ() + corner[1];
            int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            int floor = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            // standing in a lake, a river or the sea
            if (surface != floor) {
                return Optional.empty();
            }
        }
        return onTopOfChunkCenter(context, Heightmap.Types.WORLD_SURFACE_WG, builder -> builder.addPiece(
                new JungleTemplePiece(context.random(), chunk.getMinBlockX(), chunk.getMinBlockZ())));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.AMAZON_TEMPLE.get();
    }
}
