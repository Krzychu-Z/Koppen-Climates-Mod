package net.krisplis.koppenclimate.worldgen.tree;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

// The banana plant's crown: the same fixed shape on every plant. A rolled young leaf stands up in
// the middle and eight broad leaves arch out and droop, leaving the sides of the top two stem blocks
// free for banana bunches (HangingFruitDecorator). Every leaf is at most 6 face steps from the stem.
// radius and offset are unused but required by FoliagePlacer.
public class BananaFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<BananaFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            foliagePlacerParts(instance).apply(instance, BananaFoliagePlacer::new));

    // Offsets {x, y, z} from the top stem block
    private static final int[][] CROWN = {
            // rolled young leaf in the middle
            {0, 1, 0}, {0, 2, 0},
            // four leaves along the axes: up from the middle, out, then drooping
            {1, 1, 0}, {1, 2, 0}, {2, 1, 0}, {2, 0, 0}, {3, 0, 0}, {3, -1, 0},
            {-1, 1, 0}, {-1, 2, 0}, {-2, 1, 0}, {-2, 0, 0}, {-3, 0, 0}, {-3, -1, 0},
            {0, 1, 1}, {0, 2, 1}, {0, 1, 2}, {0, 0, 2}, {0, 0, 3}, {0, -1, 3},
            {0, 1, -1}, {0, 2, -1}, {0, 1, -2}, {0, 0, -2}, {0, 0, -3}, {0, -1, -3},
            // four shorter diagonal leaves, alternating which way they step
            {1, 1, 1}, {2, 1, 1}, {2, 1, 2}, {2, 0, 2},
            {1, 1, -1}, {1, 1, -2}, {2, 1, -2}, {2, 0, -2},
            {-1, 1, 1}, {-2, 1, 1}, {-2, 1, 2}, {-2, 0, 2},
            {-1, 1, -1}, {-1, 1, -2}, {-2, 1, -2}, {-2, 0, -2},
    };

    public BananaFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ModTreePlacers.BANANA_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random, TreeConfiguration config,
                                 int maxFreeTreeHeight, FoliageAttachment attachment, int foliageHeight, int foliageRadius, int offset) {
        BlockPos top = attachment.pos();
        for (int[] c : CROWN) {
            tryPlaceLeaf(level, setter, random, config, top.offset(c[0], c[1], c[2]));
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int height, TreeConfiguration config) {
        return 0;
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return false;
    }
}
