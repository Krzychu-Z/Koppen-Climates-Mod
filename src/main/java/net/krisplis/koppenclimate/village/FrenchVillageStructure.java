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
 * The French village of the temperate forest (FrenchVillagePiece). A town of its size wants the forest (Cfb) all
 * round and fairly even, dry ground - some 75 blocks by 80 - which it levels; it turns one of four ways at random.
 */
public class FrenchVillageStructure extends Structure {
    public static final MapCodec<FrenchVillageStructure> CODEC = simpleCodec(FrenchVillageStructure::new);

    private static final int MAX_SPREAD = 14, MAX_BAD = 2;

    public FrenchVillageStructure(StructureSettings settings) {
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
                int lx = i * 17, lz = j * 17 - 10;
                probes[(i + 2) * 5 + j + 2] = new int[]{lx, lz};
            }
        }
        int[] sorted = SiteSurvey.survey(context, turn, probes, 12, MAX_BAD, MAX_SPREAD);
        if (sorted == null) return Optional.empty();
        int n = sorted.length;
        int floorY = Math.max(sea + 2, sorted[n / 2] - 1);
        BlockPos origin = new BlockPos(cx, floorY, cz);
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new FrenchVillagePiece(origin, seed, turn))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.FRENCH_VILLAGE.get();
    }
}
