package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A red farm (RedFarmPiece) out in the American farmland (Cfa): it needs the farmland all round and level ground
 * (the biome's ground is levelled, so it nearly always is).
 */
public class RedFarmStructure extends Structure {
    public static final MapCodec<RedFarmStructure> CODEC = simpleCodec(RedFarmStructure::new);

    private static final int CHECK = 40, MAX_STEP = 3;

    public RedFarmStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        int sea = context.chunkGenerator().getSeaLevel();
        // the farmland, all round
        int[][] spots = {{0, 0}, {CHECK, 0}, {-CHECK, 0}, {0, CHECK}, {0, -CHECK}, {CHECK, CHECK}, {CHECK, -CHECK},
                {-CHECK, CHECK}, {-CHECK, -CHECK}, {CHECK / 2, 0}, {-CHECK / 2, 0}, {0, CHECK / 2}, {0, -CHECK / 2}};
        for (int[] d : spots) {
            if (!context.biomeSource().getNoiseBiome(QuartPos.fromBlock(cx + d[0]), QuartPos.fromBlock(sea + 2),
                    QuartPos.fromBlock(cz + d[1]), context.randomState().sampler()).is(ModBiomes.AMERICAN_FARMLAND)) {
                return Optional.empty();
            }
        }
        // level ground, above the sea
        int y = context.chunkGenerator().getFirstOccupiedHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState()) - 1;
        if (y < sea) return Optional.empty();
        for (int[] d : spots) {
            int h = context.chunkGenerator().getFirstOccupiedHeight(cx + d[0], cz + d[1], Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(), context.randomState()) - 1;
            if (Math.abs(h - y) > MAX_STEP) return Optional.empty();
        }
        BlockPos centre = new BlockPos(cx, y, cz);
        long seed = context.random().nextLong();
        int turn = context.random().nextInt(4);
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new RedFarmPiece(centre, seed, turn))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.RED_FARM.get();
    }
}
