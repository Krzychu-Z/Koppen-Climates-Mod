package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.worldgen.biome.KoppenBiomeSource;
import net.krisplis.koppenclimate.worldgen.biome.RiceTerraceSites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A Thai floating village. There is one at every rice terraces site (RiceTerraceSites): on the Amazon jungle
 * river where it runs past the monsoon forest, right beside the terraces, so it always borders all three.
 * Its structure set tries every chunk; only the chunk holding a site's spot on the river starts a village.
 */
public class ThaiVillageStructure extends Structure {
    public static final MapCodec<ThaiVillageStructure> CODEC = simpleCodec(ThaiVillageStructure::new);

    public ThaiVillageStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (!(context.biomeSource() instanceof KoppenBiomeSource source)) return Optional.empty();
        ChunkPos chunk = context.chunkPos();
        Optional<RiceTerraceSites.Site> found = source.riceTerraceSite(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ(),
                context.randomState().sampler());
        if (found.isEmpty()) return Optional.empty();
        RiceTerraceSites.Site site = found.get();
        if (SectionPos.blockToSectionCoord(site.riverX()) != chunk.x || SectionPos.blockToSectionCoord(site.riverZ()) != chunk.z) {
            return Optional.empty();
        }
        // the water's surface is the top block below sea level
        BlockPos centre = new BlockPos(site.riverX(), context.chunkGenerator().getSeaLevel() - 1, site.riverZ());
        long layoutSeed = context.random().nextLong();
        // the boardwalk nearest in direction to the rice terraces (which lie on the land) runs on ashore
        net.minecraft.core.Direction landing = net.minecraft.core.Direction.getNearest(
                site.centreX() - site.riverX(), 0, site.centreZ() - site.riverZ());
        return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new ThaiVillagePiece(centre, layoutSeed, landing))));
    }

    @Override
    public StructureType<?> type() {
        return VillageRegistry.THAI_VILLAGE.get();
    }
}
