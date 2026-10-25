package net.krisplis.koppenclimate.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

// A straight rainforest trunk, 1x1 or 2x2 ("giant"), with buttress roots flaring out at the base
// and branches high up that climb as they reach out. Each branch end and the trunk top get a crown.
//   buttress_length: how far the root fins reach (0 = none); a fin is tallest next to the trunk
//   branch_count / branch_length: branches go in distinct directions, diagonals included
//   branch_start: lowest branch height as a fraction of the trunk height
//   branch_crown_offset: how much smaller a branch's crown is than the top one (0 = same size)
public class MahoganyTrunkPlacer extends TrunkPlacer {
    // written out in full (same limits as TrunkPlacer.trunkPlacerParts) because there are too many fields to append
    public static final MapCodec<MahoganyTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 32).fieldOf("base_height").forGetter(p -> p.baseHeight),
            Codec.intRange(0, 24).fieldOf("height_rand_a").forGetter(p -> p.heightRandA),
            Codec.intRange(0, 24).fieldOf("height_rand_b").forGetter(p -> p.heightRandB),
            Codec.BOOL.fieldOf("giant").forGetter(p -> p.giant),
            IntProvider.codec(0, 4).fieldOf("buttress_length").forGetter(p -> p.buttressLength),
            IntProvider.codec(0, 8).fieldOf("branch_count").forGetter(p -> p.branchCount),
            IntProvider.codec(1, 6).fieldOf("branch_length").forGetter(p -> p.branchLength),
            Codec.floatRange(0.0F, 1.0F).fieldOf("branch_start").forGetter(p -> p.branchStart),
            Codec.intRange(-3, 0).optionalFieldOf("branch_crown_offset", -1).forGetter(p -> p.branchCrownOffset)
    ).apply(instance, MahoganyTrunkPlacer::new));

    private static final int[][] DIRECTIONS = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final int MAX_FIN_HEIGHT = 3;
    private static final float SHORT_FIN_CHANCE = 0.3F;

    private final boolean giant;
    private final IntProvider buttressLength;
    private final IntProvider branchCount;
    private final IntProvider branchLength;
    private final float branchStart;
    private final int branchCrownOffset;

    public MahoganyTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, boolean giant, IntProvider buttressLength,
                               IntProvider branchCount, IntProvider branchLength, float branchStart, int branchCrownOffset) {
        super(baseHeight, heightRandA, heightRandB);
        this.giant = giant;
        this.buttressLength = buttressLength;
        this.branchCount = branchCount;
        this.branchLength = branchLength;
        this.branchStart = branchStart;
        this.branchCrownOffset = branchCrownOffset;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModTreePlacers.MAHOGANY_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random, int height, BlockPos origin, TreeConfiguration config) {
        int width = this.giant ? 2 : 1;
        for (int dx = 0; dx < width; dx++) {
            for (int dz = 0; dz < width; dz++) {
                setDirtAt(level, blockSetter, random, origin.offset(dx, -1, dz), config);
                for (int y = 0; y < height; y++) {
                    placeLog(level, blockSetter, random, origin.offset(dx, y, dz), config);
                }
            }
        }

        placeButtressRoots(level, blockSetter, random, origin, config, width);

        List<FoliagePlacer.FoliageAttachment> attachments = new ArrayList<>();
        // a 2x2 trunk hands its corner to the crown with doubleTrunk set, like vanilla giant trees
        attachments.add(new FoliagePlacer.FoliageAttachment(origin.above(height - 1), 0, this.giant));
        placeBranches(level, blockSetter, random, origin, config, width, height, attachments);
        return attachments;
    }

    // One fin per side, growing from a random trunk column on that side, tallest next to the trunk.
    private void placeButtressRoots(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random,
                                    BlockPos origin, TreeConfiguration config, int width) {
        int length = this.buttressLength.sample(random);
        if (length <= 0) return;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int ux = dir.getStepX(), uz = dir.getStepZ();
            int column = random.nextInt(width);
            int startX = ux > 0 ? width : ux < 0 ? -1 : column;
            int startZ = uz > 0 ? width : uz < 0 ? -1 : column;
            int fin = length - (random.nextFloat() < SHORT_FIN_CHANCE ? 1 : 0);
            for (int k = 0; k < fin; k++) {
                for (int y = 0; y < Math.min(MAX_FIN_HEIGHT, fin - k); y++) {
                    placeLog(level, blockSetter, random, origin.offset(startX + ux * k, y, startZ + uz * k), config);
                }
            }
        }
    }

    private void placeBranches(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random,
                               BlockPos origin, TreeConfiguration config, int width, int height,
                               List<FoliagePlacer.FoliageAttachment> attachments) {
        // shuffle the eight directions so each branch gets its own
        int[][] dirs = DIRECTIONS.clone();
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int[] t = dirs[i]; dirs[i] = dirs[j]; dirs[j] = t;
        }

        int count = Math.min(this.branchCount.sample(random), dirs.length);
        int low = Math.round(height * this.branchStart);
        int high = Math.max(low, height - 3);
        for (int i = 0; i < count; i++) {
            int ux = dirs[i][0], uz = dirs[i][1];
            int y = low + random.nextInt(high - low + 1);
            int length = this.branchLength.sample(random);
            // start from the trunk column on that side
            int x = ux > 0 ? width - 1 : 0;
            int z = uz > 0 ? width - 1 : 0;
            Direction.Axis axis = ux != 0 ? Direction.Axis.X : Direction.Axis.Z;
            for (int k = 1; k <= length; k++) {
                if (ux != 0 && uz != 0) {
                    // diagonal: step along x first so the branch stays joined face to face
                    x += ux;
                    placeBranchLog(level, blockSetter, random, origin.offset(x, y, z), config, Direction.Axis.X);
                    z += uz;
                } else {
                    x += ux;
                    z += uz;
                }
                if (k % 2 == 0) y++;   // branches climb as they reach out
                placeBranchLog(level, blockSetter, random, origin.offset(x, y, z), config, axis);
            }
            attachments.add(new FoliagePlacer.FoliageAttachment(origin.offset(x, y, z), this.branchCrownOffset, false));
        }
    }

    private void placeBranchLog(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter, RandomSource random,
                                BlockPos pos, TreeConfiguration config, Direction.Axis axis) {
        placeLog(level, blockSetter, random, pos, config, state -> state.trySetValue(RotatedPillarBlock.AXIS, axis));
    }
}
