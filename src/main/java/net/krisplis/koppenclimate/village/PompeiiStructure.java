package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.worldgen.biome.KoppenBiomeSource;
import net.krisplis.koppenclimate.worldgen.biome.PompeiiSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Pompeii (PompeiiPiece). There is one at every Pompeii site (PompeiiSites): on the Mediterranean's (Csa) level
 * land, in the middle of its patch of red-skied Pompeii biome. Its structure set tries every chunk; only the chunk
 * holding a site's centre starts the town, which levels itself at the middle height of the ground under it.
 */
public class PompeiiStructure extends Structure {
    public static final MapCodec<PompeiiStructure> CODEC = simpleCodec(PompeiiStructure::new);

    public PompeiiStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!(context.biomeSource() instanceof KoppenBiomeSource source)) return Optional.empty();
        ChunkPos chunk = context.chunkPos();
        Optional<PompeiiSites.Site> found = source.pompeiiSite(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                context.randomState().sampler());
        if (found.isEmpty()) return Optional.empty();
        PompeiiSites.Site site = found.get();
        if (SectionPos.blockToSectionCoord(site.x()) != chunk.x || SectionPos.blockToSectionCoord(site.z()) != chunk.z) {
            return Optional.empty();
        }
        // the ground under the town: the middle of its heights at the centre and on three rings round it
        int sea = context.chunkGenerator().getSeaLevel();
        int[] h = new int[1 + 3 * 12];
        int k = 0;
        h[k++] = this.ground(context, site.x(), site.z());
        for (int ring = 1; ring <= 3; ring++) {
            for (int i = 0; i < 12; i++) {
                float a = i * Mth.TWO_PI / 12 + ring * 0.4F;
                h[k++] = this.ground(context, site.x() + Math.round(Mth.cos(a) * ring * 13), site.z() + Math.round(Mth.sin(a) * ring * 13));
            }
        }
        Arrays.sort(h);
        int floor = Math.max(sea + 2, h[h.length / 2]);
        BlockPos centre = new BlockPos(site.x(), floor, site.z());
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new PompeiiPiece(centre, seed))));
    }

    private int ground(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                context.randomState()) - 1;
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.POMPEII.get();
    }
}
