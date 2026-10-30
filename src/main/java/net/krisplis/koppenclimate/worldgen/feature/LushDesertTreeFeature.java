package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.SaguaroBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * The big plants of the Lush desert (BSh), grown block by block:
 * <ul>
 *   <li>the Joshua tree: a short, shaggy trunk that forks and forks again into crooked arms, each ending in a single
 *   block of stiff, spiky leaves (no two of them touching);</li>
 *   <li>the saguaro: a tall ribbed cactus column with one to three arms, each from a different height and of a
 *   different length, that reach out (an elbow, SaguaroArmBlock) and bend upwards, sometimes crowned with pink
 *   flowers.</li>
 * </ul>
 * The Joshua tree also grows from its sapling (configured_feature/joshua_tree.json). (The dead oak is a plain
 * vanilla tree now: configured_feature/dead_oak.json.)
 */
public class LushDesertTreeFeature extends Feature<LushDesertTreeFeature.Config> {
    public enum Kind implements StringRepresentable {
        JOSHUA("joshua"), DEAD_OAK("dead_oak"), SAGUARO("saguaro");

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

    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public LushDesertTreeFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!free(level, origin)) return false;
        BlockState ground = level.getBlockState(origin.below());
        if (!(ground.is(BlockTags.SAND) || ground.is(BlockTags.DIRT) || ground.is(BlockTags.TERRACOTTA))) return false;
        return switch (context.config().kind()) {
            case JOSHUA -> this.joshua(level, origin, random);
            case DEAD_OAK -> this.deadOak(level, origin, random);
            case SAGUARO -> this.saguaro(level, origin, random);
        };
    }

    private static boolean free(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced() || s.is(ModBlocks.JOSHUA_SAPLING.get());
    }

    private static BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private void put(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (free(level, pos)) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    // ---------------------------------------------------------------- Joshua tree

    private boolean joshua(WorldGenLevel level, BlockPos origin, RandomSource random) {
        Block wood = ModBlocks.JOSHUA_LOG.get();
        int trunk = 2 + random.nextInt(3);
        for (int i = 0; i < trunk + 6; i++) {
            if (!free(level, origin.above(i))) return false;
        }
        for (int i = 0; i < trunk; i++) this.put(level, origin.above(i), log(wood, Direction.Axis.Y));
        BlockPos top = origin.above(trunk - 1);
        // young trees are a single spike on a stick; most fork
        if (random.nextInt(6) == 0) {
            this.put(level, top.above(), log(wood, Direction.Axis.Y));
            this.tuft(level, top.above(), random);
            return true;
        }
        int forks = 2 + random.nextInt(2);
        int start = random.nextInt(4);
        for (int f = 0; f < forks; f++) this.joshuaArm(level, top, SIDES[(start + f * (forks == 2 ? 2 : 1)) % 4], 0, random);
        return true;
    }

    private void joshuaArm(WorldGenLevel level, BlockPos from, Direction side, int depth, RandomSource random) {
        Block wood = ModBlocks.JOSHUA_LOG.get();
        // out one, then up one or two: the crooked, elbowed arms
        BlockPos p = from.relative(side);
        if (!free(level, p)) return;
        this.put(level, p, log(wood, side.getAxis()));
        int up = 1 + random.nextInt(2);
        for (int i = 0; i < up; i++) {
            p = p.above();
            if (!free(level, p)) {
                this.tuft(level, p.below(), random);
                return;
            }
            this.put(level, p, log(wood, Direction.Axis.Y));
        }
        if (depth < 2 && random.nextInt(3) != 0) {
            // fork again, into two arms at right angles to each other
            Direction a = random.nextBoolean() ? side : side.getClockWise();
            this.joshuaArm(level, p, a, depth + 1, random);
            this.joshuaArm(level, p, random.nextBoolean() ? a.getOpposite() : a.getCounterClockWise(), depth + 1, random);
        } else {
            this.tuft(level, p, random);
        }
    }

    /** The leaves on the tip of an arm: one single block of them, never touching any other leaves. */
    private void tuft(WorldGenLevel level, BlockPos tip, RandomSource random) {
        BlockState leaves = ModBlocks.JOSHUA_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
        BlockPos c = tip.above();
        if (!free(level, c)) return;
        for (Direction d : Direction.values()) {
            if (level.getBlockState(c.relative(d)).is(net.minecraft.tags.BlockTags.LEAVES)) return;
        }
        this.put(level, c, leaves);
    }

    // ---------------------------------------------------------------- dead oak

    private boolean deadOak(WorldGenLevel level, BlockPos origin, RandomSource random) {
        Block wood = ModBlocks.DEAD_OAK_LOG.get();
        int height = 4 + random.nextInt(3);
        for (int i = 0; i < height + 3; i++) {
            if (!free(level, origin.above(i))) return false;
        }
        // a trunk that leans over a little as it goes up
        Direction lean = SIDES[random.nextInt(4)];
        BlockPos p = origin;
        List<BlockPos> trunk = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            this.put(level, p, log(wood, Direction.Axis.Y));
            trunk.add(p);
            if (i == height / 2 && random.nextBoolean()) {
                p = p.relative(lean);
                this.put(level, p.below(), ModBlocks.DEAD_OAK_WOOD.get().defaultBlockState());
            }
            p = p.above();
        }
        // a few roots breaking the surface
        for (Direction d : SIDES) {
            BlockPos r = origin.relative(d);
            if (random.nextInt(3) == 0 && free(level, r) && !free(level, r.below())) this.put(level, r, log(wood, d.getAxis()));
        }
        // crooked bare branches from the upper trunk, the top one carrying on upwards
        int branches = 3 + random.nextInt(3);
        int start = random.nextInt(4);
        for (int b = 0; b < branches; b++) {
            BlockPos base = trunk.get(Math.max(1, trunk.size() - 1 - random.nextInt(Math.max(1, trunk.size() / 2))));
            Direction side = SIDES[(start + b) % 4];
            this.deadBranch(level, base, side, 2 + random.nextInt(3), random);
        }
        BlockPos crown = trunk.get(trunk.size() - 1);
        for (int i = 1; i <= 1 + random.nextInt(2); i++) this.put(level, crown.above(i), log(wood, Direction.Axis.Y));
        return true;
    }

    private void deadBranch(WorldGenLevel level, BlockPos from, Direction side, int length, RandomSource random) {
        Block wood = ModBlocks.DEAD_OAK_LOG.get();
        BlockPos p = from;
        for (int i = 0; i < length; i++) {
            // outwards, rising every other block, sometimes twisting aside
            if (i > 0 && random.nextInt(2) == 0) {
                p = p.above();
                if (!free(level, p)) return;
                this.put(level, p, log(wood, Direction.Axis.Y));
            }
            Direction d = i > 0 && random.nextInt(4) == 0 ? (random.nextBoolean() ? side.getClockWise() : side.getCounterClockWise()) : side;
            p = p.relative(d);
            if (!free(level, p)) return;
            this.put(level, p, log(wood, d.getAxis()));
        }
        // a short twig at the end
        if (random.nextBoolean() && free(level, p.above())) this.put(level, p.above(), log(wood, Direction.Axis.Y));
    }

    // ---------------------------------------------------------------- saguaro

    private boolean saguaro(WorldGenLevel level, BlockPos origin, RandomSource random) {
        SaguaroBlock cactus = (SaguaroBlock) ModBlocks.SAGUARO.get();
        int height = 6 + random.nextInt(4);
        for (int i = 0; i < height + 1; i++) {
            if (!free(level, origin.above(i))) return false;
        }
        List<BlockPos> column = new ArrayList<>();
        List<BlockPos> tips = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            this.put(level, origin.above(i), cactus.defaultBlockState());
            column.add(origin.above(i));
        }
        tips.add(origin.above(height - 1));
        // the arms: each from its own height, out one block (the elbow) and then up, each a different length - like
        // the saguaros in the old western comics, never two arms alike
        int arms = random.nextInt(6) == 0 ? 3 : random.nextInt(5) == 0 ? 1 : 2;
        int start = random.nextInt(4);
        List<Integer> heights = new ArrayList<>(), lengths = new ArrayList<>();
        for (int y = 2; y <= height - 3; y++) heights.add(y);
        for (int l = 1; l <= 5; l++) lengths.add(l);
        java.util.Collections.shuffle(heights, new java.util.Random(random.nextLong()));
        java.util.Collections.shuffle(lengths, new java.util.Random(random.nextLong()));
        for (int a = 0; a < arms && a < heights.size(); a++) {
            Direction side = SIDES[(start + a * (arms == 2 ? 2 : 1)) % 4];
            int y = heights.get(a);
            BlockPos elbow = origin.above(y).relative(side);
            if (!free(level, elbow) || !free(level, elbow.above())) continue;
            // (an arm never reaches as high as the top of the trunk)
            int up = Math.min(lengths.get(a), height - 1 - y);
            BlockPos p = elbow;
            List<BlockPos> arm = new ArrayList<>();
            for (int i = 0; i < up; i++) {
                if (!free(level, p.above())) break;
                p = p.above();
                arm.add(p);
            }
            level.setBlock(elbow, ModBlocks.SAGUARO_ARM.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, side.getOpposite())
                    .setValue(net.krisplis.koppenclimate.block.custom.SaguaroArmBlock.UP, !arm.isEmpty()), Block.UPDATE_CLIENTS);
            for (BlockPos q : arm) this.put(level, q, cactus.defaultBlockState());
            column.addAll(arm);
            tips.add(arm.isEmpty() ? elbow : p);
        }
        // join the segments up and down
        for (BlockPos q : column) level.setBlock(q, cactus.connected(level, q), Block.UPDATE_CLIENTS);
        // some flower: pink blooms on the tips of the trunk and arms
        if (random.nextInt(4) == 0) {
            for (BlockPos tip : tips) {
                if (random.nextInt(3) == 0 || !free(level, tip.above()) || !(level.getBlockState(tip).getBlock() instanceof SaguaroBlock)) continue;
                level.setBlock(tip.above(), ModBlocks.SAGUARO_FLOWER.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(tip, cactus.connected(level, tip), Block.UPDATE_CLIENTS);
            }
        }
        return true;
    }
}
