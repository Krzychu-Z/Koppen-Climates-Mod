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
 * The English Mansion of the English countryside (MansionPiece). It wants the countryside (Cfb) all round and
 * fairly even, dry ground - some 95 blocks by 130 for the house and its lawns and the long walk running out from its front
 * - which it levels; it faces one of four ways.
 */
public class MansionStructure extends Structure {
    public static final MapCodec<MansionStructure> CODEC = simpleCodec(MansionStructure::new);

    private static final int MAX_SPREAD = 18, MAX_BAD = 3;

    public MansionStructure(StructureSettings settings) {
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
                int lz = j * 28 + 38, lx = lz > 44 ? i * 11 : i * 20;   // the house and its lawns, then down the long walk
                probes[(i + 2) * 5 + j + 2] = new int[]{lx, lz};
            }
        }
        int[] sorted = SiteSurvey.survey(context, turn, probes, 11, MAX_BAD, MAX_SPREAD);
        if (sorted == null) return Optional.empty();
        int n = sorted.length;
        int floorY = Math.max(sea + 2, sorted[n / 2] - 1);
        BlockPos origin = new BlockPos(cx, floorY, cz);
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new MansionPiece(origin, seed, turn))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.ENGLISH_MANSION.get();
    }
}
