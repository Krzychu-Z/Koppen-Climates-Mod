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
 *   <li>{@code apple}: the Subtropical plains' (Cfa) apple tree - vanilla's smallest oak exactly, but with apples in
 *       its leaves;</li>
 *   <li>{@code apricot}: a small, round-crowned apricot on a dark cherry-like trunk;</li>
 *   <li>{@code black_poplar}: a tall dark trunk under a vast, billowing crown;</li>
 *   <li>{@code platanus}: the plane tree - a stout trunk forking into thick limbs under a broad dome;</li>
 *   <li>{@code cedar}: the cedar of Lebanon - long level branches in flat, layered tiers;</li>
 *   <li>{@code olive}: a small, gnarled olive tree - a short twisting trunk (sometimes two) splitting into
 *       crooked branches under lumpy clumps of two-tone leaves, some already carrying olives.</li>
 *   <li>{@code maple}: a short trunk forking into steep limbs under a big round crown (the temperate forest, Cfb);</li>
 *   <li>{@code scots_pine}: a tall, bare, orange-barked trunk under a small, high, flat and ragged crown.</li>
 * </ul>
 * The leaves get their proper distance from the wood, so they decay like vanilla leaves when it's cut down.
 */
public class MediterraneanTreeFeature extends Feature<MediterraneanTreeFeature.Config> {
    public enum Kind implements StringRepresentable {
        ITALIAN_PINE_UMBRELLA("italian_pine_umbrella"), ITALIAN_PINE_WIDE("italian_pine_wide"), CYPRESS("cypress"), OLIVE("olive"),
        FIG("fig"), LEMON("lemon"), APPLE("apple"), APRICOT("apricot"), BLACK_POPLAR("black_poplar"), PLATANUS("platanus"),
        CEDAR("cedar"), MAGNOLIA("magnolia"), MAPLE("maple"), SCOTS_PINE("scots_pine");

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

    /** Which tree, and whether it's hung with vines (as in the Subtropical swamp, Cfa). */
    public record Config(Kind kind, boolean vines) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                Kind.CODEC.fieldOf("kind").forGetter(Config::kind),
                Codec.BOOL.optionalFieldOf("vines", false).forGetter(Config::vines)
        ).apply(instance, Config::new));
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
            case APPLE -> apple(origin, random);
            case APRICOT -> apricot(origin, random);
            case BLACK_POPLAR -> blackPoplar(origin, random);
            case PLATANUS -> platanus(origin, random);
            case CEDAR -> cedar(origin, random);
            case MAGNOLIA -> magnolia(origin, random);
            case MAPLE -> maple(origin, random);
            case SCOTS_PINE -> scotsPine(origin, random);
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

        BlockState leaves = switch (context.config().kind()) {
            case ITALIAN_PINE_UMBRELLA, ITALIAN_PINE_WIDE -> ModBlocks.ITALIAN_PINE_LEAVES.get().defaultBlockState();
            case CYPRESS -> ModBlocks.CYPRESS_LEAVES.get().defaultBlockState();
            case OLIVE -> ModBlocks.OLIVE_LEAVES.get().defaultBlockState();
            case FIG -> ModBlocks.FIG_LEAVES.get().defaultBlockState();
            case LEMON -> ModBlocks.LEMON_LEAVES.get().defaultBlockState();
            case APPLE -> ModBlocks.APPLE_LEAVES.get().defaultBlockState();
            case APRICOT -> ModBlocks.APRICOT_LEAVES.get().defaultBlockState();
            case BLACK_POPLAR -> ModBlocks.BLACK_POPLAR_LEAVES.get().defaultBlockState();
            case PLATANUS -> ModBlocks.PLATANUS_LEAVES.get().defaultBlockState();
            case CEDAR -> ModBlocks.CEDAR_LEAVES.get().defaultBlockState();
            case MAGNOLIA -> ModBlocks.MAGNOLIA_LEAVES.get().defaultBlockState();
            case MAPLE -> ModBlocks.MAPLE_LEAVES.get().defaultBlockState();
            case SCOTS_PINE -> ModBlocks.SCOTS_PINE_LEAVES.get().defaultBlockState();
        };
        int reach = net.krisplis.koppenclimate.block.custom.ReachLeavesBlock.reachOf(leaves);
        // each leaf's distance from the wood, as LeavesBlock counts it, out to as far as this kind's leaves hold on (6
        // as vanilla's, further for the broad canopies: ReachLeavesBlock); anything further is left out
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
            if (dist >= reach) continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (plan.leaves.containsKey(n) && !distance.containsKey(n)) {
                    distance.put(n, dist + 1);
                    queue.add(n);
                }
            }
        }
        for (Map.Entry<BlockPos, Integer> e : distance.entrySet()) {
            BlockPos pos = e.getKey();
            if (pos.getY() >= level.getMaxBuildHeight() || !canReplace(level, pos)) continue;
            BlockState state = net.krisplis.koppenclimate.block.custom.ReachLeavesBlock.withDistance(leaves, e.getValue());
            if (state.hasProperty(OliveLeavesBlock.OLIVES)) {
                state = state.setValue(OliveLeavesBlock.OLIVES, random.nextFloat() < 0.3F);
            }
            if (state.hasProperty(net.krisplis.koppenclimate.block.custom.FruitLeavesBlock.FRUIT)) {
                float f = random.nextFloat();
                state = state.setValue(net.krisplis.koppenclimate.block.custom.FruitLeavesBlock.FRUIT, f < 0.6F ? 0 : f < 0.8F ? 1 : 2);
            }
            level.setBlock(pos, state, 19);
        }
        if (context.config().vines()) vines(level, plan, random);
        // now and then wild bees have made their nest on a fig's trunk
        if (context.config().kind() == Kind.FIG && random.nextInt(5) == 0) {
            beeNest(level, origin, random);
        }
        return true;
    }

    // vines all over it, as on vanilla's swamp oaks: hanging in curtains from the crown's sides and climbing the wood
    private static void vines(WorldGenLevel level, Plan plan, RandomSource random) {
        for (BlockPos leaf : plan.leaves.keySet()) {
            if (!(level.getBlockState(leaf).getBlock() instanceof LeavesBlock)) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (random.nextInt(4) != 0) continue;
                BlockPos pos = leaf.relative(d);
                int length = 1 + random.nextInt(4);
                for (int i = 0; i < length && level.getBlockState(pos).isAir(); i++, pos = pos.below()) {
                    level.setBlock(pos, Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(d.getOpposite()), true), 2);
                }
            }
        }
        for (BlockPos log : plan.logs.keySet()) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos pos = log.relative(d);
                if (random.nextInt(3) == 0 && level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.VINE.defaultBlockState().setValue(net.minecraft.world.level.block.VineBlock.getPropertyForFace(d.getOpposite()), true), 2);
                }
            }
        }
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
        Plan plan = new Plan(ModBlocks.CYPRESS_LOG.get().defaultBlockState());
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
        Plan plan = new Plan(ModBlocks.LEMON_LOG.get().defaultBlockState());
        int trunk = 2 + random.nextInt(2);
        for (int y = 0; y < trunk + 1; y++) plan.log(origin.above(y), Direction.Axis.Y);
        plan.blob(origin.above(trunk + 1), 2.2F + random.nextFloat() * 0.5F, 0.9F, random);
        return plan;
    }

    // ---------------------------------------------------------------- the apple tree: vanilla's small oak, leaf for leaf
    private static Plan apple(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.APPLE_LOG.get().defaultBlockState());
        int height = 4 + random.nextInt(3);
        for (int y = 0; y < height; y++) plan.log(origin.above(y), Direction.Axis.Y);
        // as vanilla's blob foliage: two wide layers (5x5, corners now and then) under two narrow ones (3x3, the top
        // one a plus), the crown's top one block above the trunk
        for (int layer = 0; layer < 4; layer++) {
            int y = height - 3 + layer;
            int r = layer < 2 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    boolean corner = Math.abs(dx) == r && Math.abs(dz) == r;
                    if (corner && (layer == 3 || random.nextInt(2) == 0)) continue;
                    plan.leaf(origin.offset(dx, y, dz));
                }
            }
        }
        return plan;
    }

    // ---------------------------------------------------------------- the apricot tree
    private static Plan apricot(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.APRICOT_LOG.get().defaultBlockState());
        int trunk = 2 + random.nextInt(2);
        for (int y = 0; y < trunk; y++) plan.log(origin.above(y), Direction.Axis.Y);
        BlockPos fork = origin.above(trunk - 1);
        int limbs = 2 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < limbs; i++) {
            float a = start + i * Mth.TWO_PI / limbs;
            BlockPos tip = fork.offset(Math.round(1.6F * Mth.cos(a)), 2, Math.round(1.6F * Mth.sin(a)));
            plan.limb(fork, tip);
        }
        plan.blob(fork.above(3), 2.8F + random.nextFloat() * 0.4F, 0.75F, random);
        return plan;
    }

    // ---------------------------------------------------------------- the black poplar: the Lombardy poplar of the
    // Po valley - a tall, slim column of leaves, like a much larger cypress but broad-leaved and loose: short limbs
    // climb steeply up the trunk inside it, the crown fullest a little below the middle, tapering to a narrow top
    private static Plan blackPoplar(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.BLACK_POPLAR_LOG.get().defaultBlockState());
        int height = 15 + random.nextInt(8);
        for (int y = 0; y < height; y++) plan.log(origin.above(y), Direction.Axis.Y);
        // limbs sweeping steeply upwards, all the way up the crown
        for (int y = 4; y < height - 3; y += 2 + random.nextInt(2)) {
            float a = random.nextFloat() * Mth.TWO_PI;
            BlockPos tip = origin.offset(Math.round(1.4F * Mth.cos(a)), y + 3, Math.round(1.4F * Mth.sin(a)));
            plan.limb(origin.above(y), tip);
        }
        int crownBase = 2 + random.nextInt(2);
        float widest = 2.4F + random.nextFloat() * 0.7F;
        int top = height + 2;
        for (int y = crownBase; y <= top; y++) {
            float t = (y - crownBase) / (float) (top - crownBase);
            float r = t < 0.35F ? Mth.lerp(t / 0.35F, 1.3F, widest) : Mth.lerp((t - 0.35F) / 0.65F, widest, 0.8F);
            r += (random.nextFloat() - 0.5F) * 0.5F;
            int ri = Mth.ceil(r);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    float d2 = dx * dx + dz * dz;
                    if (d2 > r * r + 0.4F) continue;
                    // the outermost leaves thin out, so the column looks leafy rather than solid
                    if (d2 > (r - 1.0F) * (r - 1.0F) && random.nextInt(4) == 0) continue;
                    plan.leaf(origin.offset(dx, y, dz));
                }
            }
        }
        plan.leaf(origin.above(top + 1));
        return plan;
    }

    // ---------------------------------------------------------------- the plane tree
    private static Plan platanus(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.PLATANUS_LOG.get().defaultBlockState());
        int trunk = 5 + random.nextInt(3);
        for (int y = 0; y < trunk; y++) plan.log(origin.above(y), Direction.Axis.Y);
        BlockPos fork = origin.above(trunk - 1);
        int limbs = 3 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < limbs; i++) {
            float a = start + i * Mth.TWO_PI / limbs + (random.nextFloat() - 0.5F) * 0.4F;
            int out = 4 + random.nextInt(2), up = 3 + random.nextInt(3);
            BlockPos tip = fork.offset(Math.round(out * Mth.cos(a)), up, Math.round(out * Mth.sin(a)));
            plan.limb(fork, tip);
            plan.blob(tip.above(), 3.2F + random.nextFloat() * 0.6F, 0.7F, random);
        }
        plan.blob(fork.above(5), 3.4F, 0.7F, random);
        return plan;
    }

    // ---------------------------------------------------------------- the southern magnolia
    // a straight trunk to the top, short branches all the way up, and a dense, broad egg of a crown that comes down
    // almost to the ground: the evergreen magnolia of the south, in leaf all year round
    private static Plan magnolia(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.MAGNOLIA_LOG.get().defaultBlockState());
        int height = 9 + random.nextInt(4);
        for (int y = 0; y < height; y++) plan.log(origin.above(y), Direction.Axis.Y);
        float widest = 3.2F + random.nextFloat() * 0.8F;
        // short branches, angled up and out, from low on the trunk to near its top
        for (int y = 3; y < height - 2; y += 2) {
            int branches = 2 + random.nextInt(2);
            float start = random.nextFloat() * Mth.TWO_PI;
            for (int i = 0; i < branches; i++) {
                float a = start + i * Mth.TWO_PI / branches + (random.nextFloat() - 0.5F) * 0.6F;
                int len = y < height / 2 ? 3 : 2;
                BlockPos tip = origin.offset(Math.round(len * Mth.cos(a)), y + 1, Math.round(len * Mth.sin(a)));
                plan.limb(origin.above(y), tip);
                plan.blob(tip, 1.6F, 0.8F, random);
            }
        }
        // the crown: an egg, broadest a little below its middle, from two blocks up to just over the top
        int bottom = 2, top = height + 1;
        float middle = bottom + (top - bottom) * 0.42F;
        for (int y = bottom; y <= top; y++) {
            float t = y <= middle ? (middle - y) / (middle - bottom) : (y - middle) / (top - middle);
            float r = widest * Mth.sqrt(Math.max(0.0F, 1.0F - t * t));
            if (r < 0.8F) r = 0.8F;
            plan.disc(origin.above(y), r, random, 0.35F);
        }
        plan.leaf(origin.above(top + 1));
        return plan;
    }

    // ---------------------------------------------------------------- the cedar of Lebanon
    private static Plan cedar(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.CEDAR_LOG.get().defaultBlockState());
        int height = 11 + random.nextInt(5);
        for (int y = 0; y < height; y++) plan.log(origin.above(y), Direction.Axis.Y);
        // the tiers: long level branches, shorter towards the top, each holding a flat pad of needles one or two deep
        for (int y = 4 + random.nextInt(2); y < height; y += 3 + random.nextInt(2)) {
            float f = y / (float) height;
            int branches = 3 + random.nextInt(2);
            float start = random.nextFloat() * Mth.TWO_PI;
            for (int i = 0; i < branches; i++) {
                float a = start + i * Mth.TWO_PI / branches + (random.nextFloat() - 0.5F) * 0.5F;
                int len = Math.max(2, Math.round((1.0F - f) * 7.0F + 1.0F + random.nextFloat() * 1.5F));
                BlockPos tip = origin.offset(Math.round(len * Mth.cos(a)), y, Math.round(len * Mth.sin(a)));
                plan.limb(origin.above(y), tip);
                float pad = 2.2F + (1.0F - f) * 1.2F;
                plan.disc(tip.above(), pad, random, 0.4F);
                if (random.nextBoolean()) plan.disc(tip.above(2), pad - 1.0F, random, 0.5F);
            }
            plan.disc(origin.above(y + 1), 1.6F, random, 0.3F);
        }
        // the flat top
        plan.disc(origin.above(height), 2.6F, random, 0.4F);
        plan.disc(origin.above(height + 1), 1.6F, random, 0.5F);
        return plan;
    }

    // ---------------------------------------------------------------- the maple
    // the sycamore and Norway maple of the temperate woods: a short stout trunk forking into a few thick limbs that
    // climb steeply into a big, full, round crown
    private static Plan maple(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.MAPLE_LOG.get().defaultBlockState());
        int trunk = 4 + random.nextInt(2);
        int leader = trunk + 4 + random.nextInt(2);
        for (int y = 0; y < leader; y++) plan.log(origin.above(y), Direction.Axis.Y);
        BlockPos fork = origin.above(trunk - 1);
        int limbs = 3 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < limbs; i++) {
            float a = start + i * Mth.TWO_PI / limbs + (random.nextFloat() - 0.5F) * 0.5F;
            int out = 2 + random.nextInt(2), up = 3 + random.nextInt(2);
            BlockPos tip = fork.offset(Math.round(out * Mth.cos(a)), up, Math.round(out * Mth.sin(a)));
            plan.limb(fork, tip);
            plan.blob(tip.above(), 2.4F + random.nextFloat() * 0.4F, 0.8F, random);
        }
        float radius = 3.8F + random.nextFloat() * 0.6F;
        plan.blob(origin.above(trunk + 3), radius, 0.85F, random);
        return plan;
    }

    // ---------------------------------------------------------------- the Scots pine
    // a straight trunk with a slight lean or kink near the top, bare and orange-barked below, and an open, irregular
    // crown over its upper third or so: a few branches of uneven length at uneven heights, each holding a ragged
    // clump of blue-green needles, and sometimes the leader forking at the top
    private static Plan scotsPine(BlockPos origin, RandomSource random) {
        Plan plan = new Plan(ModBlocks.SCOTS_PINE_LOG.get().defaultBlockState());
        int height = 10 + random.nextInt(5);
        Direction lean = randomSide(random);
        int kinkAt = height - 4 - random.nextInt(3);
        BlockPos top = null;
        for (int y = 0; y < height; y++) {
            // above the kink the trunk steps a block aside
            BlockPos next = origin.above(y).relative(lean, y >= kinkAt ? 1 : 0);
            if (top != null && (next.getX() != top.getX() || next.getZ() != top.getZ())) {
                plan.log(top.relative(lean), lean.getAxis());
            }
            plan.log(next, Direction.Axis.Y);
            top = next;
        }
        int crownFrom = height - 4 - random.nextInt(3);
        float start = random.nextFloat() * Mth.TWO_PI;
        int n = 0;
        for (int y = crownFrom; y < height; y += 1 + random.nextInt(2)) {
            int branches = 1 + random.nextInt(2);
            for (int i = 0; i < branches; i++, n++) {
                // spread the branches round the trunk, not quite evenly
                float a = start + n * 2.4F + (random.nextFloat() - 0.5F) * 0.9F;
                int len = 2 + random.nextInt(2) + (y < height - 2 ? 1 : 0);
                BlockPos from = origin.above(y).relative(lean, y >= kinkAt ? 1 : 0);
                BlockPos tip = from.offset(Math.round(len * Mth.cos(a)), random.nextInt(2), Math.round(len * Mth.sin(a)));
                plan.limb(from, tip);
                plan.blob(tip.above(), 1.3F + random.nextFloat() * 0.9F, 0.55F + random.nextFloat() * 0.2F, random);
            }
        }
        // the top: a clump, sometimes over a forked leader
        if (random.nextInt(3) == 0) {
            Direction d = randomSide(random);
            plan.limb(top, top.relative(d, 2).above());
            plan.blob(top.relative(d, 2).above(2), 1.4F, 0.6F, random);
        }
        plan.blob(top.above(), 1.6F + random.nextFloat() * 0.6F, 0.6F, random);
        // now and then a dead stub of a lower branch on the bare trunk
        for (int i = random.nextInt(3); i > 0; i--) {
            Direction d = randomSide(random);
            plan.log(origin.above(3 + random.nextInt(Math.max(1, crownFrom - 4))).relative(d), d.getAxis());
        }
        return plan;
    }
}
