package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * The Cursed Oasis of the Hot desert (BWh) and the Sahel (BWk): see CursedOasisPiece. It wants fairly even, dry
 * ground some 20 blocks across in the desert's own middle.
 */
public class CursedOasisStructure extends Structure {
    public static final MapCodec<CursedOasisStructure> CODEC = simpleCodec(CursedOasisStructure::new);
    private static final int CHECK = 9, MAX_SPREAD = 7;

    public CursedOasisStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        int sea = generator.getSeaLevel();
        if (!context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx), QuartPos.fromBlock(sea + 8),
                QuartPos.fromBlock(cz), context.randomState().sampler()))) {
            return Optional.empty();
        }
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE, sum = 0, n = 0;
        for (int dx = -CHECK; dx <= CHECK; dx += CHECK) {
            for (int dz = -CHECK; dz <= CHECK; dz += CHECK) {
                int x = cx + dx, z = cz + dz;
                int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                int floor = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                if (surface != floor || surface <= sea) return Optional.empty();
                min = Math.min(min, surface);
                max = Math.max(max, surface);
                sum += surface;
                n++;
            }
        }
        if (max - min > MAX_SPREAD) return Optional.empty();
        BlockPos centre = new BlockPos(cx, Math.round(sum / (float) n) - 1, cz);
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new CursedOasisPiece(centre, seed))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.CURSED_OASIS.get();
    }
}
