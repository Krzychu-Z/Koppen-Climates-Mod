package net.krisplis.koppenclimate.worldgen.tree;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
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

/**
 * The umbrella thorn of the African savanna: a tall bare trunk, sometimes leaning a little, that splits near the
 * top into a few long limbs reaching out and up at different angles. Each limb ends in a wide, flat pad of leaves
 * (an acacia foliage placer), and together the pads make the broad flat-topped crown.
 */
public class UmbrellaAcaciaTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<UmbrellaAcaciaTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            trunkPlacerParts(instance).and(instance.group(
                    IntProvider.codec(1, 6).fieldOf("branch_count").forGetter(p -> p.branchCount),
                    IntProvider.codec(1, 10).fieldOf("branch_length").forGetter(p -> p.branchLength)
            )).apply(instance, UmbrellaAcaciaTrunkPlacer::new));

    private final IntProvider branchCount;
    private final IntProvider branchLength;

    public UmbrellaAcaciaTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider branchCount, IntProvider branchLength) {
        super(baseHeight, heightRandA, heightRandB);
        this.branchCount = branchCount;
        this.branchLength = branchLength;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModTreePlacers.UMBRELLA_ACACIA_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random, int height, BlockPos origin, TreeConfiguration config) {
        setDirtAt(level, blockSetter, random, origin.below(), config);
        // the trunk, with an occasional kink a block sideways two thirds of the way up
        Direction kink = random.nextInt(3) == 0 ? Direction.Plane.HORIZONTAL.getRandomDirection(random) : null;
        int kinkAt = height * 2 / 3;
        BlockPos pos = origin;
        for (int i = 0; i < height; i++) {
            pos = origin.above(i);
            if (kink != null && i >= kinkAt) pos = pos.relative(kink);
            if (kink != null && i == kinkAt) {
                placeLog(level, blockSetter, random, origin.above(i), config,
                        s -> s.trySetValue(RotatedPillarBlock.AXIS, kink.getAxis()));
            }
            placeLog(level, blockSetter, random, pos, config);
        }
        BlockPos top = pos;

        // the limbs, spread evenly round the compass with a little jitter
        int count = this.branchCount.sample(random);
        float start = random.nextFloat() * Mth.TWO_PI;
        List<FoliagePlacer.FoliageAttachment> crowns = new ArrayList<>();
        for (int b = 0; b < count; b++) {
            float angle = start + b * Mth.TWO_PI / count + (random.nextFloat() - 0.5F) * 0.6F;
            float dx = Mth.cos(angle), dz = Mth.sin(angle);
            int length = this.branchLength.sample(random);
            int rise = 1 + random.nextInt(3);
            BlockPos end = top;
            double fx = top.getX() + 0.5, fz = top.getZ() + 0.5;
            for (int s = 1; s <= length; s++) {
                fx += dx;
                fz += dz;
                int y = top.getY() + 1 + Math.round((float) rise * s / length);
                BlockPos next = new BlockPos(Mth.floor(fx), y, Mth.floor(fz));
                Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
                if (next.getY() > end.getY() && (next.getX() != end.getX() || next.getZ() != end.getZ())) {
                    // step up and out: a log at the corner keeps the limb joined
                    placeLog(level, blockSetter, random, new BlockPos(next.getX(), end.getY(), next.getZ()), config,
                            st -> st.trySetValue(RotatedPillarBlock.AXIS, axis));
                }
                placeLog(level, blockSetter, random, next, config, st -> st.trySetValue(RotatedPillarBlock.AXIS, axis));
                end = next;
            }
            crowns.add(new FoliagePlacer.FoliageAttachment(end.above(), 0, false));
        }
        // a pad over the trunk itself, a little higher, ties the crown together
        crowns.add(new FoliagePlacer.FoliageAttachment(top.above(3), -1, false));
        placeLog(level, blockSetter, random, top.above(), config);
        placeLog(level, blockSetter, random, top.above(2), config);
        return crowns;
    }
}
