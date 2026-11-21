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
 * The Gothic Cathedral of the temperate forest (CathedralPiece). It wants the forest (Cfb) all round, and fairly
 * even, dry ground under its length - some 80 blocks by 50 - which it levels for its close; it turns to one of four
 * ways at random.
 */
public class CathedralStructure extends Structure {
    public static final MapCodec<CathedralStructure> CODEC = simpleCodec(CathedralStructure::new);

    private static final int MAX_SPREAD = 12, MAX_BAD = 1;
    // the middle of the cathedral, from its west front
    private static final int MIDDLE = 26;

    public CathedralStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        int sea = context.chunkGenerator().getSeaLevel();
        int turn = context.random().nextInt(4);
        // the points to check, along the cathedral's length and across it, in its own frame
        int[][] probes = {{-12, 0}, {0, 0}, {MIDDLE, 0}, {52, 0}, {64, 0}, {0, -20}, {0, 20}, {MIDDLE, -22}, {MIDDLE, 22},
                {52, -16}, {52, 16}, {14, -14}, {14, 14}, {40, -14}, {40, 14}};
        for (int[] p : probes) p[0] -= MIDDLE;
        int[] sorted = SiteSurvey.survey(context, turn, probes, 2, MAX_BAD, MAX_SPREAD);
        if (sorted == null) return Optional.empty();
        int floorY = Math.max(sea + 2, sorted[sorted.length / 2] - 1);
        BlockPos origin = new BlockPos(cx + CathedralPiece.rx(turn, -MIDDLE, 0), floorY, cz + CathedralPiece.rz(turn, -MIDDLE, 0));
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(cx, floorY, cz),
                builder -> builder.addPiece(new CathedralPiece(origin, seed, turn))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.GOTHIC_CATHEDRAL.get();
    }
}
