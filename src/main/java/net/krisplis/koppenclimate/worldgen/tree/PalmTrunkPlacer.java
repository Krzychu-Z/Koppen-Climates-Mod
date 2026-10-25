package net.krisplis.koppenclimate.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

import java.util.List;
import java.util.function.BiConsumer;

// A single-stem palm trunk that can lean in one random direction.
// "lean" is how many blocks sideways the top ends up; "curve" shapes the bend:
//   curve > 1 keeps the trunk straight and bends it near the crown,
//   curve < 1 leans hard at the base and straightens up toward the crown.
public class PalmTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<PalmTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            trunkPlacerParts(instance).and(instance.group(
                    IntProvider.codec(0, 8).fieldOf("lean").forGetter(p -> p.lean),
                    Codec.floatRange(0.1F, 5.0F).fieldOf("curve").forGetter(p -> p.curve)
            )).apply(instance, PalmTrunkPlacer::new));

    private final IntProvider lean;
    private final float curve;

    public PalmTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider lean, float curve) {
        super(baseHeight, heightRandA, heightRandB);
        this.lean = lean;
        this.curve = curve;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModTreePlacers.PALM_TRUNK_PLACER.get();
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random, int height, BlockPos origin, TreeConfiguration config) {
        // Palms grow on sand: keep the sand, only turn other ground into dirt like vanilla trees do.
        BlockPos below = origin.below();
        if (!level.isStateAtPosition(below, state -> state.is(BlockTags.SAND))) {
            setDirtAt(level, blockSetter, random, below, config);
        }

        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int totalLean = this.lean.sample(random);
        // The top log sits straight above the one below it, so the crown always has a log underneath.
        int span = Math.max(1, height - 2);
        int offset = 0;
        BlockPos top = origin;

        for (int i = 0; i < height; i++) {
            int target = Math.round(totalLean * (float) Math.pow(Math.min(i, span) / (float) span, this.curve));
            // Every sideways step gets a horizontal log so the trunk stays joined face to face.
            while (offset < target) {
                placeLog(level, blockSetter, random, origin.above(i).relative(direction, offset), config,
                        state -> state.trySetValue(RotatedPillarBlock.AXIS, direction.getAxis()));
                offset++;
            }
            top = origin.above(i).relative(direction, offset);
            placeLog(level, blockSetter, random, top, config);
        }

        return List.of(new FoliagePlacer.FoliageAttachment(top, 0, false));
    }
}
