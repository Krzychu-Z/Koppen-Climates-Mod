package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A karst tower of the monsoon subtropics (Cwa), as along the Li river at Guilin: a sheer-sided, round-topped pillar of
 * white limestone rising twenty to forty-odd blocks straight up from the plain. Its sides bulge and pinch as they climb
 * (with a ledge or an overhang here and there) and are streaked with moss; every ledge and the domed top carry a little
 * earth under grass and shrubs, and curtains of vines hang down its faces.
 */
public class KarstTowerFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_HEIGHT = 20, MAX_HEIGHT = 44;
    // the widest radius it may reach: it must stay within the chunks a feature may write to
    private static final float MAX_RADIUS = 9.5F;

    public KarstTowerFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        int cx = origin.getX(), cz = origin.getZ();
        int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx, cz);
        if (!level.getFluidState(new BlockPos(cx, ground, cz)).isEmpty()) return false;
        if (!level.getBlockState(new BlockPos(cx, ground - 1, cz)).is(BlockTags.DIRT)
                && !level.getBlockState(new BlockPos(cx, ground - 1, cz)).is(ModBlocks.WHITE_LIMESTONE.get())) return false;

        int height = MIN_HEIGHT + random.nextInt(MAX_HEIGHT - MIN_HEIGHT + 1);
        float base = 4.5F + random.nextFloat() * 3.0F;
        // slender towers are the taller ones
        if (height > 34) base = Math.min(base, 6.0F);
        float phase = random.nextFloat() * Mth.TWO_PI, phase2 = random.nextFloat() * Mth.TWO_PI;
        float lean = (random.nextFloat() - 0.5F) * 0.12F, leanAngle = random.nextFloat() * Mth.TWO_PI;
        float bulgeFreq = 0.18F + random.nextFloat() * 0.15F;
        // the lowest ground under its footprint: the tower goes down to it
        int r0 = (int) Math.ceil(MAX_RADIUS);
        int bottom = ground;
        for (int dx = -r0; dx <= r0; dx += 2) {
            for (int dz = -r0; dz <= r0; dz += 2) {
                if (dx * dx + dz * dz > base * base) continue;
                bottom = Math.min(bottom, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, cx + dx, cz + dz));
            }
        }
        bottom -= 3;
        int top = ground + height;
        BlockState stone = ModBlocks.WHITE_LIMESTONE.get().defaultBlockState();
        BlockState mossy = ModBlocks.MOSSY_COBBLED_WHITE_LIMESTONE.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int domeStart = top - Math.max(4, Math.round(base * 0.9F));

        for (int y = bottom; y <= top; y++) {
            float t = (float) (y - ground) / height;
            // the sides: a little flare at the foot, bulges and pinches up the shaft, a rounded dome on top
            float r = base * (1.0F + 0.16F * Mth.sin(y * bulgeFreq + phase) + 0.07F * Mth.sin(y * 0.53F + phase2));
            if (t < 0.12F) r += (0.12F - t) * 10.0F;
            r *= 1.0F - 0.25F * t * t;
            if (y > domeStart) {
                float d = (float) (y - domeStart) / (top - domeStart + 1);
                r *= Mth.sqrt(Math.max(0.0F, 1.0F - d * d));
            }
            r = Math.min(r, MAX_RADIUS);
            if (r < 0.8F) continue;
            float ox = Mth.cos(leanAngle) * lean * (y - ground), oz = Mth.sin(leanAngle) * lean * (y - ground);
            int ri = (int) Math.ceil(r + 1.0F);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    float fx = dx - ox, fz = dz - oz;
                    float angle = (float) Mth.atan2(fz, fx);
                    float edge = r * (1.0F + 0.13F * Mth.sin(3.0F * angle + phase + y * 0.07F) + 0.07F * Mth.sin(5.0F * angle + phase2));
                    if (fx * fx + fz * fz > edge * edge) continue;
                    if (Math.abs(dx) > 15 || Math.abs(dz) > 15) continue;
                    pos.set(cx + dx, y, cz + dz);
                    BlockState here = level.getBlockState(pos);
                    if (here.is(Blocks.BEDROCK)) continue;
                    boolean shell = fx * fx + fz * fz > (edge - 1.2F) * (edge - 1.2F);
                    BlockState put = shell && random.nextFloat() < 0.18F ? mossy : stone;
                    level.setBlock(pos, put, Block.UPDATE_CLIENTS);
                }
            }
        }

        // earth and grass on every upward face above the plain, shrubs here and there; vines down the sides
        int reach = (int) Math.ceil(MAX_RADIUS + 3);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (Math.abs(dx) > 15 || Math.abs(dz) > 15) continue;
                for (int y = top; y > ground + 1; y--) {
                    pos.set(cx + dx, y, cz + dz);
                    BlockState state = level.getBlockState(pos);
                    if (!isTowerRock(state)) continue;
                    if (!level.getBlockState(pos.above()).isAir()) continue;
                    // a cap of earth under grass on the ledge or the top
                    level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
                    BlockPos below = pos.below();
                    if (isTowerRock(level.getBlockState(below)) && random.nextFloat() < 0.7F) {
                        level.setBlock(below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    float roll = random.nextFloat();
                    BlockPos above = pos.above();
                    if (roll < 0.18F) {
                        level.setBlock(above, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
                        if (random.nextFloat() < 0.4F && level.getBlockState(above.above()).isAir()) {
                            level.setBlock(above.above(), Blocks.AZALEA_LEAVES.defaultBlockState()
                                    .setValue(LeavesBlock.PERSISTENT, true), Block.UPDATE_CLIENTS);
                        }
                    } else if (roll < 0.55F) {
                        level.setBlock(above, Blocks.SHORT_GRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
                    } else if (roll < 0.6F) {
                        level.setBlock(above, Blocks.FERN.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (Math.abs(dx) > 15 || Math.abs(dz) > 15) continue;
                for (int y = top; y > ground + 2; y--) {
                    pos.set(cx + dx, y, cz + dz);
                    if (!level.getBlockState(pos).isAir()) continue;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        if (!isTowerSide(level.getBlockState(pos.relative(d)))) continue;
                        if (random.nextFloat() >= 0.07F) continue;
                        // a curtain of vines from here down
                        int length = 3 + random.nextInt(10);
                        BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(d), true);
                        BlockPos.MutableBlockPos v = pos.mutable();
                        for (int n = 0; n < length && v.getY() > ground; n++) {
                            if (!level.getBlockState(v).isAir() || !isTowerSide(level.getBlockState(v.relative(d)))) break;
                            level.setBlock(v, vine, Block.UPDATE_CLIENTS);
                            v.move(Direction.DOWN);
                        }
                        break;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isTowerRock(BlockState state) {
        return state.is(ModBlocks.WHITE_LIMESTONE.get()) || state.is(ModBlocks.MOSSY_COBBLED_WHITE_LIMESTONE.get());
    }

    private static boolean isTowerSide(BlockState state) {
        return isTowerRock(state) || state.is(Blocks.DIRT);
    }
}
