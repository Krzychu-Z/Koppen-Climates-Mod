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

// The coconut palm crown: the same fixed shape on every palm. Thin fronds droop from the top log
// and a second layer rises above them, so the frond tips all sit 3.7-4.5 blocks from the crown's
// centre, on a rough sphere like a real palm. Every leaf is at most 6 face steps from the top log,
// so a fresh tree never decays. radius and offset are unused but required by FoliagePlacer.
public class PalmFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<PalmFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            foliagePlacerParts(instance).apply(instance, PalmFoliagePlacer::new));

    // Offsets {x, y, z} from the top log
    private static final int[][] CROWN = {
            // tuft and the four short arms around it
            {0, 1, 0}, {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1},
            // four drooping fronds along the axes, tips 4.5 blocks out
            {1, 0, 0}, {2, 0, 0}, {3, 0, 0}, {3, -1, 0}, {4, -1, 0}, {4, -2, 0},
            {-1, 0, 0}, {-2, 0, 0}, {-3, 0, 0}, {-3, -1, 0}, {-4, -1, 0}, {-4, -2, 0},
            {0, 0, 1}, {0, 0, 2}, {0, 0, 3}, {0, -1, 3}, {0, -1, 4}, {0, -2, 4},
            {0, 0, -1}, {0, 0, -2}, {0, 0, -3}, {0, -1, -3}, {0, -1, -4}, {0, -2, -4},
            // two drooping diagonal fronds on the +x side
            {1, 0, 1}, {1, 0, 2}, {2, 0, 2}, {2, -1, 2}, {2, -1, 3},
            {1, 0, -1}, {2, 0, -1}, {2, 0, -2}, {2, -1, -2}, {3, -1, -2},
            // upright spear leaf in the middle
            {0, 2, 0}, {0, 3, 0}, {0, 4, 0},
            // four fronds rising from the arms and curving outward
            {1, 2, 0}, {2, 2, 0}, {2, 3, 0}, {3, 3, 0},
            {-1, 2, 0}, {-2, 2, 0}, {-2, 3, 0}, {-3, 3, 0},
            {0, 2, 1}, {0, 2, 2}, {0, 3, 2}, {0, 3, 3},
            {0, 2, -1}, {0, 2, -2}, {0, 3, -2}, {0, 3, -3},
            // two rising diagonal fronds on the -x side, where the lower layer has none
            {-1, 1, 1}, {-1, 2, 1}, {-2, 2, 1}, {-2, 3, 1},
            {-1, 1, -1}, {-1, 2, -1}, {-2, 2, -1}, {-2, 3, -1},
    };

    public PalmFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ModTreePlacers.PALM_FOLIAGE_PLACER.get();
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
