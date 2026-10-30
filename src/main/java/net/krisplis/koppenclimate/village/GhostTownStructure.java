package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.worldgen.biome.GhostTownSites;
import net.krisplis.koppenclimate.worldgen.biome.KoppenBiomeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * A ghost town (GhostTownPiece). There is one at every ghost town site (GhostTownSites): out in the Lush desert
 * (BSh), in the middle of its patch of yellow-skied ghost town biome. Its structure set tries every chunk; only the
 * chunk holding a site's centre starts a town.
 */
public class GhostTownStructure extends Structure {
    public static final MapCodec<GhostTownStructure> CODEC = simpleCodec(GhostTownStructure::new);

    public GhostTownStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!(context.biomeSource() instanceof KoppenBiomeSource source)) return Optional.empty();
        ChunkPos chunk = context.chunkPos();
        Optional<GhostTownSites.Site> found = source.ghostTownSite(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                context.randomState().sampler());
        if (found.isEmpty()) return Optional.empty();
        GhostTownSites.Site site = found.get();
        if (SectionPos.blockToSectionCoord(site.x()) != chunk.x || SectionPos.blockToSectionCoord(site.z()) != chunk.z) {
            return Optional.empty();
        }
        // the town isn't levelled: each of its parts stands at the middle height of the ground under it
        int sea = context.chunkGenerator().getSeaLevel() + 1;
        int[][] feet = GhostTownPiece.footprints();
        int[] floors = new int[feet.length];
        for (int i = 0; i < feet.length; i++) {
            int[] f = feet[i];
            int[] h = new int[9];
            int k = 0;
            for (int a = 0; a <= 2; a++) {
                for (int b = 0; b <= 2; b++) {
                    int x = site.x() + f[0] + (f[2] - f[0]) * a / 2, z = site.z() + f[1] + (f[3] - f[1]) * b / 2;
                    h[k++] = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                            context.heightAccessor(), context.randomState()) - 1;
                }
            }
            Arrays.sort(h);
            floors[i] = Math.max(sea, h[4]);
        }
        int[] all = floors.clone();
        Arrays.sort(all);
        BlockPos centre = new BlockPos(site.x(), all[all.length / 2], site.z());
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new GhostTownPiece(centre, seed, floors))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.GHOST_TOWN.get();
    }
}
