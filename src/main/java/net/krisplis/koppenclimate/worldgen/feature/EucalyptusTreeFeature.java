package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.KoalaEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * The rainbow eucalyptus of the Australian desert (BSh), a lone tree out on the plain: a tall, smooth trunk with a
 * lean to it, forking near the top into two or three long limbs that reach up and out, each carrying a thin, ragged
 * crown of leaves (the light falls straight through a eucalyptus). Out in the wild (not from a sapling), one to three
 * koalas are found asleep clinging to its trunk. Also grows from its sapling (configured_feature/eucalyptus_tree.json).
 */
public class EucalyptusTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public EucalyptusTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();
        if (!free(level, origin)) return false;
        BlockState ground = level.getBlockState(origin.below());
        if (!(ground.is(BlockTags.SAND) || ground.is(BlockTags.DIRT) || ground.is(BlockTags.TERRACOTTA))) return false;
        int height = 9 + random.nextInt(5);
        for (int i = 1; i <= height + 4; i++) {
            if (!free(level, origin.above(i))) return false;
        }

        // the trunk: straight up, then leaning one block over partway, now and then once more
        Direction lean = SIDES[random.nextInt(4)];
        int leanAt = height / 2 + random.nextInt(2);
        List<BlockPos> trunk = new ArrayList<>();
        BlockPos p = origin;
        for (int i = 0; i < height; i++) {
            if (i == leanAt || (i == leanAt + 3 && random.nextInt(2) == 0)) {
                // the bend: a sideways log joins the two straight runs
                BlockPos side = p.relative(lean);
                this.put(level, side, log(lean.getAxis()));
                p = side.above();
            } else if (i > 0) {
                p = p.above();
            }
            this.put(level, p, log(Direction.Axis.Y));
            trunk.add(p);
        }
        BlockPos top = p;

        // the limbs: two or three, up and out from the top of the trunk, each with its ragged crown
        int limbs = 2 + random.nextInt(2);
        float start = random.nextFloat() * Mth.TWO_PI;
        for (int b = 0; b < limbs; b++) {
            float angle = start + b * Mth.TWO_PI / limbs + (random.nextFloat() - 0.5F) * 0.6F;
            float dx = Mth.cos(angle), dz = Mth.sin(angle);
            Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.X : Direction.Axis.Z;
            double fx = top.getX() + 0.5, fz = top.getZ() + 0.5;
            int y = top.getY();
            BlockPos end = top;
            int length = 2 + random.nextInt(3);
            for (int s = 0; s < length; s++) {
                fx += dx;
                fz += dz;
                y += random.nextInt(3) == 0 ? 0 : 1;
                end = new BlockPos(Mth.floor(fx), y, Mth.floor(fz));
                this.put(level, end, log(axis));
            }
            this.crown(level, end, random);
        }
        this.crown(level, top.above(), random);

        // koalas, asleep on the trunk (only out in the wild: a tree grown from a sapling is still bare of them)
        if (level instanceof WorldGenRegion) {
            int koalas = 1 + random.nextInt(3);
            for (int k = 0; k < koalas; k++) {
                BlockPos at = trunk.get(2 + random.nextInt(Math.max(1, trunk.size() - 4)));
                Direction side = SIDES[random.nextInt(4)];
                BlockPos spot = at.relative(side);
                if (!level.getBlockState(spot).isAir()) continue;
                KoalaEntity koala = ModEntities.KOALA.get().create(level.getLevel());
                if (koala == null) continue;
                koala.moveTo(spot.getX() + 0.5 - side.getStepX() * 0.2, spot.getY(), spot.getZ() + 0.5 - side.getStepZ() * 0.2,
                        side.getOpposite().toYRot(), 0.0F);
                koala.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.STRUCTURE, null);
                koala.clingTo(at, side.getOpposite());
                koala.setPersistenceRequired();
                level.addFreshEntityWithPassengers(koala);
            }
        }
        return true;
    }

    /** A thin, ragged clump of leaves around a limb's end: a rough ball with gaps in it. */
    private void crown(WorldGenLevel level, BlockPos centre, RandomSource random) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int d = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                    if (d == 0 || d > 3) continue;
                    if (random.nextFloat() < (d == 1 ? 0.1F : 0.35F)) continue;   // the gaps
                    BlockPos q = centre.offset(dx, dy, dz);
                    if (!level.getBlockState(q).isAir()) continue;
                    level.setBlock(q, ModBlocks.EUCALYPTUS_LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, Math.min(6, d)),
                            Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    private static BlockState log(Direction.Axis axis) {
        return ModBlocks.EUCALYPTUS_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    private void put(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (free(level, pos)) level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static boolean free(WorldGenLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.isAir() || s.canBeReplaced() || s.is(BlockTags.LEAVES) || s.is(ModBlocks.EUCALYPTUS_SAPLING.get());
    }
}
