package net.krisplis.koppenclimate.worldgen.tree;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.OliveLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Mediterranean's trees, each drawn by hand rather than from vanilla's trunk and foliage placers:
 * <ul>
 *   <li>{@code italian_pine_umbrella}: the stone pine - a tall bare trunk, bending a little near the top, with a
 *       few short branches holding up one broad, flat-bottomed umbrella of bright needles;</li>
 *   <li>{@code italian_pine_wide}: an older stone pine whose trunk forks into two or three leaning limbs, each
 *       with its own flat crown, together making a wide, ragged umbrella;</li>
 *   <li>{@code cypress}: the Mediterranean cypress - a narrow dark green column, widest a third of the way up
 *       and tapering to a point, on a spruce trunk;</li>
 *   <li>{@code fig}: a low, sprawling fig tree - a short trunk splitting into several limbs reaching out sideways
 *       under one broad, rounded, dark green crown;</li>
 *   <li>{@code lemon}: a small lemon tree - a short trunk and a dense round crown;</li>
 *   <li>{@code olive}: a small, gnarled olive tree - a short twisting trunk (sometimes two) splitting into
 *       crooked branches under lumpy clumps of two-tone leaves, some already carrying olives.</li>
 * </ul>
 * The leaves get their proper distance from the wood, so they decay like vanilla leaves when it's cut down.
 */
public class MediterraneanTreeFeature extends Feature<MediterraneanTreeFeature.Config> {
    public enum Kind implements StringRepresentable {
        ITALIAN_PINE_UMBRELLA("italian_pine_umbrella"), ITALIAN_PINE_WIDE("italian_pine_wide"), CYPRESS("cypress"), OLIVE("olive"),
        FIG("fig"), LEMON("lemon");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }

    public record Config(Kind kind) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = Kind.CODEC.fieldOf("kind").xmap(Config::new, Config::kind).codec();
    }

    public MediterraneanTreeFeature(Codec<Config> codec) {
        super(codec);
    }

    /** The tree being drawn: its wood and its leaves, before anything is placed. */
    private static final class Plan {
        final Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        final Map<BlockPos, Boolean> leaves = new LinkedHashMap<>();
        final BlockState log;

        Plan(BlockState log) {
            this.log = log;
        }

        void log(BlockPos pos, Direction.Axis axis) {
            this.logs.put(pos.immutable(), this.log.setValue(RotatedPillarBlock.AXIS, axis));
            this.leaves.remove(pos);
        }

        void leaf(BlockPos pos) {
            if (!this.logs.containsKey(pos)) this.leaves.putIfAbsent(pos.immutable(), Boolean.TRUE);
        }

        /** A crooked line of logs from a to b, one face-joined block at a time, each turned the way it runs. */
        void limb(BlockPos a, BlockPos b) {
            BlockPos p = a;
            while (!p.equals(b)) {
                int dx = b.getX() - p.getX(), dy = b.getY() - p.getY(), dz = b.getZ() - p.getZ();
                Direction.Axis axis;
                if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) >= Math.abs(dz)) {
                    p = p.above(Integer.signum(dy));
                    axis = Direction.Axis.Y;
                } else if (Math.abs(dx) >= Math.abs(dz)) {
                    p = p.east(Integer.signum(dx));
                    axis = Direction.Axis.X;
                } else {
                    p = p.south(Integer.signum(dz));
                    axis = Direction.Axis.Z;
                }
                this.log(p, axis);
            }
        }

        /** A flat, round layer of leaves, with a ragged edge. */
        void disc(BlockPos centre, float radius, RandomSource random, float ragged) {
            int r = Mth.ceil(radius);
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    float d = Mth.sqrt(dx * dx + dz * dz);
                    if (d <= radius - 0.5F || (d <= radius + 0.3F && random.nextFloat() > ragged)) {
                        this.leaf(centre.offset(dx, 0, dz));
                    }
                }
            }
        }

        /** A lumpy ball of leaves, squashed to the given height. */
        void blob(BlockPos centre, float radius, float squash, RandomSource random) {
            int r = Mth.ceil(radius);
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        float d = Mth.sqrt(dx * dx + (dy / squash) * (dy / squash) + dz * dz);
                        if (d <= radius - 0.4F || (d <= radius + 0.4F && random.nextFloat() < 0.45F)) {
                            this.leaf(centre.offset(dx, dy, dz));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!level.getBlockState(origin.below()).is(BlockTags.DIRT)) return false;

        Plan plan = switch (context.config().kind()) {
            case ITALIAN_PINE_UMBRELLA -> umbrellaPine(origin, random);
            case ITALIAN_PINE_WIDE -> widePine(origin, random);
            case CYPRESS -> cypress(origin, random);
            case OLIVE -> olive(origin, random);
            case FIG -> fig(origin, random);
            case LEMON -> lemon(origin, random);
        };

        // room to grow: the wood may only go where there's air, plants or leaves
        for (BlockPos pos : plan.logs.keySet()) {
            if (pos.getY() >= level.getMaxBuildHeight() || !canReplace(level, pos)) return false;
        }
        for (Map.Entry<BlockPos, BlockState> e : plan.logs.entrySet()) {
            level.setBlock(e.getKey(), e.getValue(), 19);
        }
        // the soil under the trunk turns to dirt, as under vanilla trees
        BlockPos ground = origin.below();
        if (level.getBlockState(ground).is(ModBlocks.CINNAMON_GRASS_BLOCK.get())) {
            level.setBlock(ground, ModBlocks.CINNAMON_DIRT.get().defaultBlockState(), 2);
        }

        // each leaf's distance from the wood, as LeavesBlock counts it (6 at most; anything further is left out)
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos log : plan.logs.keySet()) {
            for (Direction d : Direction.values()) {
                BlockPos n = log.relative(d);
                if (plan.leaves.containsKey(n) && !distance.containsKey(n)) {
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
                if (plan.leaves.containsKey(n) && !distance.containsKey(n)) {
                    distance.put(n, dist + 1);
                    queue.add(n);
                }
            }
        }
        BlockState leaves = switch (context.config().kind()) {
            case ITALIAN_PINE_UMBRELLA, ITALIAN_PINE_WIDE -> ModBlocks.ITALIAN_PINE_LEAVES.get().defaultBlockState();
            case CYPRESS -> ModBlocks.CYPRESS_LEAVES.get().defaultBlockState();
            case OLIVE -> ModBlocks.OLIVE_LEAVES.get().defaultBlockState();
            case FIG -> ModBlocks.FIG_LEAVES.get().defaultBlockState();
            case LEMON -> ModBlocks.LEMON_LEAVES.get().defaultBlockState();
        };
        for (Map.Entry<BlockPos, Integer> e : distance.entrySet()) {
            BlockPos pos = e.getKey();
            if (pos.getY() >= level.getMaxBuildHeight() || !canReplace(level, pos)) continue;
            BlockState state = leaves.setValue(LeavesBlock.DISTANCE, e.getValue());
            if (state.hasProperty(OliveLeavesBlock.OLIVES)) {
                state = state.setValue(OliveLeavesBlock.OLIVES, random.nextFloat() < 0.3F);
            }
            if (state.hasProperty(net.krisplis.koppenclimate.block.custom.FruitLeavesBlock.FRUIT)) {
                float f = random.nextFloat();
                state = state.setValue(net.krisplis.koppenclimate.block.custom.FruitLeavesBlock.FRUIT, f < 0.6F ? 0 : f < 0.8F ? 1 : 2);
            }
            level.setBlock(pos, state, 19);
        }
        // now and then wild bees have made their nest on a fig's trunk
        if (context.config().kind() == Kind.FIG && random.nextInt(5) == 0) {
            beeNest(level, origin, random);
        }
        return true;
    }

    private static void beeNest(WorldGenLevel level, BlockPos origin, RandomSource random) {
        for (int tries = 0; tries < 6; tries++) {
            Direction side = randomSide(random);
            BlockPos log = origin.above(1 + random.nextInt(2));
            if (!level.getBlockState(log).is(BlockTags.LOGS)) continue;
            BlockPos nest = log.relative(side);
            if (!level.getBlockState(nest).isAir() || !level.getBlockState(nest.relative(side)).isAir()) continue;
            level.setBlock(nest, Blocks.BEE_NEST.defaultBlockState().setValue(net.minecraft.world.level.block.BeehiveBlock.FACING, side), 2);
            if (level.getBlockEntity(nest) instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity hive) {
                int bees = 2 + random.nextInt(2);
                for (int i = 0; i < bees; i++) {
                    hive.storeBee(net.minecraft.world.level.block.entity.BeehiveBlockEntity.Occupant.create(random.nextInt(599)));
                }
            }
            return;
        }
    }

    private static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(BlockTags.SAPLINGS);
    }

    private static Direction randomSide(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    // ---------------------------------------------------------------- the stone pine, tall umbrella
    private static Plan umbrellaPine(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.ITALIAN_PINE_LOG.get().defaultBlockState());
        int height = 9 + random.nextInt(5);
        Direction lean = randomSide(random);
        int bendAt = height - 3 - random.nextInt(2);
        BlockPos top = null;
        for (int y = 0; y < height; y++) {
            // above the bend the trunk drifts a block or two to one side
            int drift = y >= bendAt ? Math.min(2, 1 + (y - bendAt) / 2) : 0;
            BlockPos next = origin.above(y).relative(lean, drift);
            if (top != null && (next.getX() != top.getX() || next.getZ() != top.getZ())) {
                plan.log(top.relative(lean), lean.getAxis());   // the elbow
            }
            plan.log(next, Direction.Axis.Y);
            top = next;
        }
        // short branches spreading out under the crown
        int branches = 3 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < branches; i++) {
            float a = start + i * Mth.TWO_PI / branches;
            int len = 2 + random.nextInt(2);
            BlockPos tip = top.offset(Math.round(len * Mth.cos(a)), 1, Math.round(len * Mth.sin(a)));
            plan.limb(top, tip);
        }
        // the leader runs on up into the crown
        plan.log(top.above(1), Direction.Axis.Y);
        plan.log(top.above(2), Direction.Axis.Y);
        // the umbrella: narrow underneath, spreading out layer by layer to a broad, flat top, 4-5 layers deep
        float radius = 4.5F + random.nextFloat() * 1.5F;
        float[] shrink = random.nextBoolean() ? new float[]{1.8F, 0.9F, 0.3F, 0.0F} : new float[]{2.8F, 1.3F, 0.6F, 0.2F, 0.0F};
        for (int i = 0; i < shrink.length; i++) {
            plan.disc(top.above(1 + i), radius - shrink[i], random, i == 0 ? 0.5F : 0.35F);
        }
        return plan;
    }

    // ---------------------------------------------------------------- the stone pine, broad and forked
    private static Plan widePine(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.ITALIAN_PINE_LOG.get().defaultBlockState());
        int trunk = 5 + random.nextInt(3);
        for (int y = 0; y < trunk; y++) plan.log(origin.above(y), Direction.Axis.Y);
        BlockPos fork = origin.above(trunk - 1);
        int limbs = 2 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < limbs; i++) {
            float a = start + i * Mth.TWO_PI / limbs + (random.nextFloat() - 0.5F) * 0.5F;
            int out = 3 + random.nextInt(2), up = 3 + random.nextInt(3);
            BlockPos tip = fork.offset(Math.round(out * Mth.cos(a)), up, Math.round(out * Mth.sin(a)));
            plan.limb(fork, tip);
            plan.log(tip.above(), Direction.Axis.Y);
            // each limb carries its own thick crown, 4 layers deep, narrow below and broad and flat on top
            float radius = 3.2F + random.nextFloat();
            float[] shrink = {2.2F, 1.0F, 0.3F, 0.0F};
            for (int l = 0; l < shrink.length; l++) {
                plan.disc(tip.above(1 + l), radius - shrink[l], random, l == 0 ? 0.5F : 0.35F);
            }
        }
        return plan;
    }

    // ---------------------------------------------------------------- the cypress column
    private static Plan cypress(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(Blocks.SPRUCE_LOG.defaultBlockState());
        int height = 9 + random.nextInt(7);
        for (int y = 0; y < height; y++) plan.log(origin.above(y), Direction.Axis.Y);
        float widest = height > 12 ? 2.2F : 1.6F;
        for (int y = 1; y < height; y++) {
            float t = y / (float) height;
            // swelling to its widest a third of the way up, then narrowing towards the top - never thinner than
            // the four leaves around the trunk
            float r = Math.max(1.0F, t < 0.33F ? Mth.lerp(t / 0.33F, 1.0F, widest) : Mth.lerp((t - 0.33F) / 0.67F, widest, 1.0F));
            int ri = Mth.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dz * dz <= r * r + 0.3F) plan.leaf(origin.offset(dx, y, dz));
                }
            }
        }
        // the point: a leaf around the top log, and two above it
        for (Direction d : Direction.Plane.HORIZONTAL) plan.leaf(origin.above(height - 1).relative(d));
        plan.leaf(origin.above(height));
        plan.leaf(origin.above(height + 1));
        return plan;
    }

    // ---------------------------------------------------------------- the olive tree
    private static Plan olive(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.OLIVE_LOG.get().defaultBlockState());
        int stems = random.nextFloat() < 0.3F ? 2 : 1;
        for (int s = 0; s < stems; s++) {
            BlockPos pos = s == 0 ? origin : origin.relative(randomSide(random));
            if (s > 0) plan.log(pos, randomSide(random).getAxis());
            int height = 2 + random.nextInt(2);
            // a short, twisting trunk: it wanders a block sideways now and then on its way up
            for (int y = 0; y < height; y++) {
                plan.log(pos, Direction.Axis.Y);
                if (y > 0 && random.nextFloat() < 0.45F) {
                    Direction d = randomSide(random);
                    pos = pos.relative(d);
                    plan.log(pos, d.getAxis());
                }
                pos = pos.above();
            }
            pos = pos.below();
            // crooked branches out to the clumps of leaves
            int branches = 2 + random.nextInt(3);
            float start = random.nextFloat() * Mth.TWO_PI;
            for (int i = 0; i < branches; i++) {
                float a = start + i * Mth.TWO_PI / branches + (random.nextFloat() - 0.5F) * 0.6F;
                int out = 2 + random.nextInt(2), up = 1;
                BlockPos tip = pos.offset(Math.round(out * Mth.cos(a)), up, Math.round(out * Mth.sin(a)));
                plan.limb(pos, tip);
                plan.blob(tip.above(), 2.0F + random.nextFloat() * 0.8F, 0.6F, random);
            }
            plan.blob(pos.above(2), 1.8F, 0.6F, random);
        }
        // an olive is a low tree: nothing of it above the 6th block
        plan.logs.keySet().removeIf(p -> p.getY() > origin.getY() + 5);
        plan.leaves.keySet().removeIf(p -> p.getY() > origin.getY() + 5);
        return plan;
    }

    // ---------------------------------------------------------------- the fig tree
    private static Plan fig(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.FIG_LOG.get().defaultBlockState());
        int trunk = 2 + random.nextInt(2);
        for (int y = 0; y < trunk; y++) plan.log(origin.above(y), Direction.Axis.Y);
        BlockPos fork = origin.above(trunk - 1);
        int limbs = 3 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < limbs; i++) {
            float a = start + i * Mth.TWO_PI / limbs + (random.nextFloat() - 0.5F) * 0.4F;
            int out = 3 + random.nextInt(2);
            BlockPos tip = fork.offset(Math.round(out * Mth.cos(a)), 1 + random.nextInt(2), Math.round(out * Mth.sin(a)));
            plan.limb(fork, tip);
            plan.blob(tip.above(), 2.4F + random.nextFloat() * 0.6F, 0.6F, random);
        }
        plan.blob(fork.above(3), 2.6F, 0.6F, random);
        return plan;
    }

    // ---------------------------------------------------------------- the lemon tree
    private static Plan lemon(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(Blocks.OAK_LOG.defaultBlockState());
        int trunk = 2 + random.nextInt(2);
        for (int y = 0; y < trunk + 1; y++) plan.log(origin.above(y), Direction.Axis.Y);
        plan.blob(origin.above(trunk + 1), 2.2F + random.nextFloat() * 0.5F, 0.9F, random);
        return plan;
    }
}
