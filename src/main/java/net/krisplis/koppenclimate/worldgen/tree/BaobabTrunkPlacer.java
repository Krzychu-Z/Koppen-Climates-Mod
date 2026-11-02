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
 * The baobab: an enormous bottle-shaped trunk, round in section, swelling out a third of the way up and
 * narrowing toward the top, crowned by a ring of short, thick, crooked limbs that reach out sideways like roots
 * in the air. Each limb ends in a small round clump of leaves. Like an old baobab, the trunk is hollow: a wall of
 * wood one block thick around an empty room, closed over at the top under the crown, with a doorway at its foot.
 */
public class BaobabTrunkPlacer extends TrunkPlacer {
    public static final MapCodec<BaobabTrunkPlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            trunkPlacerParts(instance).and(instance.group(
                    IntProvider.codec(2, 10).fieldOf("branch_count").forGetter(p -> p.branchCount),
                    IntProvider.codec(1, 8).fieldOf("branch_length").forGetter(p -> p.branchLength)
            )).apply(instance, BaobabTrunkPlacer::new));

    // trunk radius at the foot, the bulge and the top
    private static final float FOOT = 2.0F, BULGE = 2.6F, TOP = 1.3F;

    private final IntProvider branchCount;
    private final IntProvider branchLength;

    public BaobabTrunkPlacer(int baseHeight, int heightRandA, int heightRandB, IntProvider branchCount, IntProvider branchLength) {
        super(baseHeight, heightRandA, heightRandB);
        this.branchCount = branchCount;
        this.branchLength = branchLength;
    }

    @Override
    protected TrunkPlacerType<?> type() {
        return ModTreePlacers.BAOBAB_TRUNK_PLACER.get();
    }

    private static float radius(int y, int height) {
        float t = (float) y / Math.max(1, height - 1);
        if (t < 0.35F) return Mth.lerp(t / 0.35F, FOOT, BULGE);
        return Mth.lerp((t - 0.35F) / 0.65F, BULGE, TOP);
    }

    private static boolean inDisc(int dx, int dz, float r) {
        return dx * dx + dz * dz <= r * r + 0.5F;
    }

    /** Whether a hollow block here would be shut in: wood (or more hollow) on all four sides, and roofed over above. */
    private static boolean walledIn(int dx, int dz, float r, float above) {
        return inDisc(dx + 1, dz, r) && inDisc(dx - 1, dz, r) && inDisc(dx, dz + 1, r) && inDisc(dx, dz - 1, r)
                && inDisc(dx, dz, above);
    }

    /** Whether this column of the wall is in the doorway: straight out from the middle on the door's side. */
    private static boolean isDoorway(int dx, int dz, Direction door) {
        int along = dx * door.getStepX() + dz * door.getStepZ();
        int across = dx * door.getStepZ() - dz * door.getStepX();
        return along > 0 && across == 0;
    }

    @Override
    public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> blockSetter,
                                                            RandomSource random, int height, BlockPos origin, TreeConfiguration config) {
        int reach = Mth.ceil(BULGE);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz <= FOOT * FOOT + 0.5F) setDirtAt(level, blockSetter, random, origin.offset(dx, -1, dz), config);
            }
        }
        // the doorway: two blocks high, out through the wall on one side
        Direction door = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int y = 0; y < height; y++) {
            float r = radius(y, height);
            // the inside: everything more than a block in from the bark, all the way up but for the top layer (the roof)
            float inner = y < height - 1 ? r - 1.0F : -1.0F;
            float above = radius(y + 1, height);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    int d2 = dx * dx + dz * dz;
                    if (d2 > r * r + 0.5F) continue;
                    if (inner > 0.0F && d2 <= inner * inner + 0.5F && walledIn(dx, dz, r, above)) continue;   // hollow
                    if (y < 2 && isDoorway(dx, dz, door)) continue;
                    placeLog(level, blockSetter, random, origin.offset(dx, y, dz), config);
                }
            }
        }

        // the limbs: thick and crooked, out sideways from the top of the trunk
        BlockPos top = origin.above(height - 1);
        int count = this.branchCount.sample(random);
        float start = random.nextFloat() * Mth.TWO_PI;
        List<FoliagePlacer.FoliageAttachment> crowns = new ArrayList<>();
        for (int b = 0; b < count; b++) {
            float angle = start + b * Mth.TWO_PI / count + (random.nextFloat() - 0.5F) * 0.5F;
            float dx = Mth.cos(angle), dz = Mth.sin(angle);
            Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
            int length = this.branchLength.sample(random) + 1;
            double fx = top.getX() + 0.5 + dx, fz = top.getZ() + 0.5 + dz;
            int y = top.getY() - random.nextInt(2);
            BlockPos end = top;
            for (int s = 0; s < length; s++) {
                fx += dx;
                fz += dz;
                if (s > 0 && random.nextInt(2) == 0) {
                    // a kink upward: a vertical log joins the two levels
                    placeLog(level, blockSetter, random, new BlockPos(Mth.floor(fx), y, Mth.floor(fz)), config);
                    y++;
                }
                end = new BlockPos(Mth.floor(fx), y, Mth.floor(fz));
                placeLog(level, blockSetter, random, end, config, st -> st.trySetValue(RotatedPillarBlock.AXIS, axis));
            }
            crowns.add(new FoliagePlacer.FoliageAttachment(end.above(), 0, false));
        }
        // and a short stub straight up in the middle with its own clump
        placeLog(level, blockSetter, random, top.above(), config);
        crowns.add(new FoliagePlacer.FoliageAttachment(top.above(2), 0, false));
        return crowns;
    }
}
