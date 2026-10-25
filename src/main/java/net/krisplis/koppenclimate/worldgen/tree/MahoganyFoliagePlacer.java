package net.krisplis.koppenclimate.worldgen.tree;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// A dense, slightly flattened round crown with a ragged edge, four layers tall, centred on the
// attachment log (a 2x2 trunk top when doubleTrunk is set). Each branch passes radiusOffset -1,
// so branch crowns are a size smaller than the top one.
// Only leaves within 6 face steps of the centre log(s), walking through the crown itself, are placed,
// so the ragged edge can never cut a leaf off from the tree and leave it to decay.
public class MahoganyFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<MahoganyFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            foliagePlacerParts(instance).apply(instance, MahoganyFoliagePlacer::new));

    private static final int[] LAYERS = {-1, 0, 1, 2};
    private static final float RAGGED_EDGE_CHANCE = 0.25F;
    private static final int MAX_LEAF_STEPS = 6;

    public MahoganyFoliagePlacer(IntProvider radius, IntProvider offset) {
        super(radius, offset);
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ModTreePlacers.MAHOGANY_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random, TreeConfiguration config,
                                 int maxFreeTreeHeight, FoliageAttachment attachment, int foliageHeight, int foliageRadius, int offset) {
        int radius = foliageRadius + attachment.radiusOffset();
        boolean wide = attachment.doubleTrunk();
        int width = wide ? 2 : 1;
        double shift = wide ? 0.5 : 0.0;   // a 2x2 trunk's centre sits between its four columns
        int lo = -radius - 1;
        int hi = radius + 1 + (wide ? 1 : 0);

        // 1. the crown's shape, relative to the attachment
        Set<BlockPos> candidates = new HashSet<>();
        for (int dy : LAYERS) {
            int r = radius - (dy == LAYERS[0] || dy == LAYERS[LAYERS.length - 1] ? 1 : 0);
            double edge = r * 0.8;
            for (int dx = lo; dx <= hi; dx++) {
                for (int dz = lo; dz <= hi; dz++) {
                    double fx = dx - shift, fz = dz - shift;
                    double d2 = fx * fx + fz * fz;
                    if (d2 > r * r + edge) continue;
                    if (d2 > (r - 1) * (r - 1) + edge && random.nextFloat() < RAGGED_EDGE_CHANCE) continue;
                    candidates.add(new BlockPos(dx, dy, dz));
                }
            }
        }

        // 2. keep what the trunk top reaches within MAX_LEAF_STEPS, walking through the crown
        Map<BlockPos, Integer> depth = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < width; dz++) {
                BlockPos start = new BlockPos(dx, 0, dz);
                depth.put(start, 0);
                queue.add(start);
            }
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int d = depth.get(p);
            if (d == MAX_LEAF_STEPS) continue;
            for (Direction dir : Direction.values()) {
                BlockPos n = p.relative(dir);
                if (candidates.contains(n) && !depth.containsKey(n)) {
                    depth.put(n, d + 1);
                    queue.add(n);
                }
            }
        }

        BlockPos centre = attachment.pos();
        for (Map.Entry<BlockPos, Integer> e : depth.entrySet()) {
            if (e.getValue() > 0) {
                tryPlaceLeaf(level, setter, random, config, centre.offset(e.getKey()));
            }
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
