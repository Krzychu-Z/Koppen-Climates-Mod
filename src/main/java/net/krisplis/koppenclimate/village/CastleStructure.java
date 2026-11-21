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

import java.util.Arrays;
import java.util.Optional;

/**
 * The Highland Castle of the Scottish highlands (CastlePiece). It wants the highlands or their sea cliffs (Cfc)
 * all round and dry ground no steeper than a broad hilltop - some 90 blocks across - which it levels (cutting the
 * crest down or building the slope up into its rock); it faces one of four ways.
 */
public class CastleStructure extends Structure {
    public static final MapCodec<CastleStructure> CODEC = simpleCodec(CastleStructure::new);

    private static final int MAX_SPREAD = 18, MAX_BAD = 3;

    public CastleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        int sea = context.chunkGenerator().getSeaLevel();
        int turn = context.random().nextInt(4);
        int[][] probes = new int[25][];
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                int lx = i * 20, lz = j * 18;
                probes[(i + 2) * 5 + j + 2] = new int[]{lx, lz};
            }
        }
        int[] sorted = SiteSurvey.survey(context, turn, probes, 12, MAX_BAD, MAX_SPREAD);
        if (sorted == null) return Optional.empty();
        int n = sorted.length;
        int floorY = Math.max(sea + 3, sorted[n * 3 / 5]);
        BlockPos origin = new BlockPos(cx, floorY, cz);
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new CastlePiece(origin, seed, turn))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.HIGHLAND_CASTLE.get();
    }
}
