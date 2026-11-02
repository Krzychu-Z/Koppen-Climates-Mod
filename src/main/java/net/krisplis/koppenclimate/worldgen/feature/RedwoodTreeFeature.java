package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The coast redwood of the Redwood forest (Csb): a colossal column of red-brown wood, never less than four blocks
 * across (up to seven in the forest), flaring into buttress roots at its foot and tapering slowly as it climbs 55 to
 * 95 blocks. The lower half of the trunk is bare; above it short limbs stick out all round, each with its own clump of
 * dark needles, the clumps narrowing towards the spire at the top into one tall, slim crown.
 *
 * <p>The forest places them from the worldgen (any width from 4 to 7); a 3x3 square of saplings on podzol grows
 * a young one, four blocks across (RedwoodSaplingBlock, through {@link #grow}).
 */
public class RedwoodTreeFeature extends Feature<NoneFeatureConfiguration> {
    public RedwoodTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return grow(context.level(), context.origin(), context.random(), false);
    }

    /**
     * Grows a redwood whose trunk is centred on this spot (the ground being just below it). From saplings it is the
     * smallest kind, four blocks across; from the worldgen four to seven. False, and nothing placed, if there's no
     * room or no soil.
     */
    public static boolean grow(WorldGenLevel level, BlockPos centre, RandomSource random, boolean sapling) {
        if (!level.getBlockState(centre.below()).is(BlockTags.DIRT)) return false;
        int width = sapling ? 4 : 4 + weighted(random);
        int height = sapling ? 42 + random.nextInt(10) : 34 + width * 7 + random.nextInt(14);
        if (centre.getY() + height + 4 >= level.getMaxBuildHeight()) return false;

        Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        Map<BlockPos, Boolean> leaves = new LinkedHashMap<>();
        BlockState log = ModBlocks.REDWOOD_LOG.get().defaultBlockState();
        // the trunk's middle: between blocks for an even width, on one for an odd
        double mid = (width - 1) / 2.0;
        BlockPos corner = centre.offset(-(width - 1) / 2, 0, -(width - 1) / 2);
        float baseRadius = width / 2.0F;
        for (int y = -6; y < height; y++) {
            // a slow taper to a third of the width at the top, and a flare of buttress roots at the foot
            float f = Math.max(0, y) / (float) height;
            float radius = Math.max(0.6F, baseRadius * (1.0F - 0.62F * f));
            if (y <= 2) radius = baseRadius + (y <= 0 ? 1.0F : 0.5F);
            int r = Mth.ceil(radius) + 1;
            for (int dx = -r; dx < width + r; dx++) {
                for (int dz = -r; dz < width + r; dz++) {
                    double ox = dx - mid, oz = dz - mid;
                    double d2 = ox * ox + oz * oz;
                    boolean inside = d2 <= radius * radius + 0.5;
                    // the buttresses: four ridges running out from the foot
                    if (!inside && y <= 1 && (Math.abs(ox) < 0.6 || Math.abs(oz) < 0.6) && d2 <= (radius + 1.5) * (radius + 1.5)) inside = true;
                    if (!inside) continue;
                    BlockPos p = corner.offset(dx, y, dz);
                    // below the ground the trunk only fills down to solid soil, so it never floats on a slope
                    if (y < 0 && !replaceable(level, p)) continue;
                    if (y >= 0 || replaceable(level, p)) logs.put(p, log);
                }
            }
        }
        // the trunk must stand on the ground: at least most of its foot rests on something solid
        int resting = 0, foot = 0;
        for (Map.Entry<BlockPos, BlockState> e : logs.entrySet()) {
            if (e.getKey().getY() != centre.getY()) continue;
            foot++;
            BlockPos below = e.getKey().below();
            if (!logs.containsKey(below) && !level.getBlockState(below).isAir()) resting++;
        }
        if (foot == 0 || resting * 2 < foot) return false;

        // the limbs and their clumps of needles, from a little under half way up to the spire
        int crownStart = (int) (height * (sapling ? 0.42F : 0.48F));
        int y = crownStart;
        while (y < height - 3) {
            float f = (y - crownStart) / (float) Math.max(1, height - crownStart);
            float trunkR = Math.max(0.6F, baseRadius * (1.0F - 0.62F * y / (float) height));
            int limbs = 2 + random.nextInt(2);
            float start = random.nextFloat() * Mth.TWO_PI;
            for (int i = 0; i < limbs; i++) {
                float a = start + i * Mth.TWO_PI / limbs + (random.nextFloat() - 0.5F) * 0.7F;
                // longer limbs low down, short ones near the top
                float len = trunkR + 1.5F + (1.0F - f) * (3.0F + width * 0.5F) + random.nextFloat() * 1.5F;
                BlockPos from = corner.offset(Mth.floor(mid + 0.5), y, Mth.floor(mid + 0.5));
                BlockPos tip = from.offset(Math.round(len * Mth.cos(a)), 1 + random.nextInt(2), Math.round(len * Mth.sin(a)));
                limb(logs, log, from, tip);
                blob(leaves, logs, tip, 2.2F + (1.0F - f) * 1.2F, random);
            }
            // a ring of needles hugging the trunk
            disc(leaves, logs, corner, mid, y + 1, trunkR + 1.5F + (1.0F - f) * 1.5F);
            y += 2 + random.nextInt(2);
        }
        // the spire: a slim cone of needles round the leader
        BlockPos top = corner.offset(Mth.floor(mid + 0.5), height - 1, Mth.floor(mid + 0.5));
        for (int k = 0; k < 4; k++) logs.put(top.above(k), log);
        for (int k = 0; k < 7; k++) {
            float rr = Math.max(0.5F, 2.6F - k * 0.4F);
            disc(leaves, logs, top.offset(0, 0, 0), 0.0, k - 2, rr, top.getX(), top.getZ());
        }
        leaves.put(top.above(5), Boolean.TRUE);

        // room to grow: the wood may only go where there's air, plants or leaves
        for (BlockPos p : logs.keySet()) {
            if (p.getY() >= level.getMaxBuildHeight()) return false;
            if (p.getY() >= centre.getY() && !replaceable(level, p) && !level.getBlockState(p).is(BlockTags.SAPLINGS)) {
                if (sapling || p.getY() > centre.getY() + 2) return false;
            }
        }
        for (Map.Entry<BlockPos, BlockState> e : logs.entrySet()) {
            BlockState state = e.getValue();
            level.setBlock(e.getKey(), state.setValue(RotatedPillarBlock.AXIS, axisOf(e.getKey(), logs)), 19);
        }
        // each needle's distance from the wood, as LeavesBlock counts it (6 at most; anything further is left out)
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos p : logs.keySet()) {
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (leaves.containsKey(n) && !logs.containsKey(n) && !distance.containsKey(n)) {
                    distance.put(n, 1);
                    queue.add(n);
                }
            }
        }
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int dist = distance.get(p);
            if (dist >= LeavesBlock.DECAY_DISTANCE - 1) continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (leaves.containsKey(n) && !logs.containsKey(n) && !distance.containsKey(n)) {
                    distance.put(n, dist + 1);
                    queue.add(n);
                }
            }
        }
        BlockState needles = ModBlocks.REDWOOD_LEAVES.get().defaultBlockState();
        for (Map.Entry<BlockPos, Integer> e : distance.entrySet()) {
            BlockPos p = e.getKey();
            if (p.getY() >= level.getMaxBuildHeight() || !replaceable(level, p)) continue;
            level.setBlock(p, needles.setValue(LeavesBlock.DISTANCE, e.getValue()), 19);
        }
        return true;
    }

    // most redwoods are five or six blocks across; four and seven are rarer
    private static int weighted(RandomSource random) {
        int r = random.nextInt(10);
        return r < 2 ? 0 : r < 6 ? 1 : r < 9 ? 2 : 3;
    }

    private static boolean replaceable(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS);
    }

    /** Which way a log runs: the trunk upright, the limbs along their length. */
    private static Direction.Axis axisOf(BlockPos p, Map<BlockPos, BlockState> logs) {
        if (logs.containsKey(p.above()) || logs.containsKey(p.below())) return Direction.Axis.Y;
        if (logs.containsKey(p.east()) || logs.containsKey(p.west())) return Direction.Axis.X;
        if (logs.containsKey(p.north()) || logs.containsKey(p.south())) return Direction.Axis.Z;
        return Direction.Axis.Y;
    }

    /** A crooked line of logs from a to b, one face-joined block at a time. */
    private static void limb(Map<BlockPos, BlockState> logs, BlockState log, BlockPos a, BlockPos b) {
        BlockPos p = a;
        while (!p.equals(b)) {
            int dx = b.getX() - p.getX(), dy = b.getY() - p.getY(), dz = b.getZ() - p.getZ();
            if (Math.abs(dy) * 3 >= Math.abs(dx) + Math.abs(dz) && dy != 0) p = p.above(Integer.signum(dy));
            else if (Math.abs(dx) >= Math.abs(dz)) p = p.east(Integer.signum(dx));
            else p = p.south(Integer.signum(dz));
            logs.put(p, log);
        }
    }

    /** A lumpy, flattened ball of needles. */
    private static void blob(Map<BlockPos, Boolean> leaves, Map<BlockPos, BlockState> logs, BlockPos centre, float radius, RandomSource random) {
        int r = Mth.ceil(radius);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    float d = Mth.sqrt(dx * dx + (dy * 1.6F) * (dy * 1.6F) + dz * dz);
                    if (d <= radius - 0.3F || (d <= radius + 0.4F && random.nextFloat() < 0.5F)) {
                        BlockPos p = centre.offset(dx, dy, dz);
                        if (!logs.containsKey(p)) leaves.putIfAbsent(p, Boolean.TRUE);
                    }
                }
            }
        }
    }

    /** A flat ring of needles around the trunk at one height. */
    private static void disc(Map<BlockPos, Boolean> leaves, Map<BlockPos, BlockState> logs, BlockPos corner, double mid, int y, float radius) {
        disc(leaves, logs, corner, mid, y, radius, corner.getX() + mid, corner.getZ() + mid);
    }

    private static void disc(Map<BlockPos, Boolean> leaves, Map<BlockPos, BlockState> logs, BlockPos base, double mid, int y, float radius,
                             double cx, double cz) {
        int r = Mth.ceil(radius) + 1;
        int bx = Mth.floor(cx), bz = Mth.floor(cz);
        for (int x = bx - r; x <= bx + r + 1; x++) {
            for (int z = bz - r; z <= bz + r + 1; z++) {
                double ox = x - cx, oz = z - cz;
                if (ox * ox + oz * oz > radius * radius) continue;
                BlockPos p = new BlockPos(x, base.getY() + y, z);
                if (!logs.containsKey(p)) leaves.putIfAbsent(p, Boolean.TRUE);
            }
        }
    }
}
