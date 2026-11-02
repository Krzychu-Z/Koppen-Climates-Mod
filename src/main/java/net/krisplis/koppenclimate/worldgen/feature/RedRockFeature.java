package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A lone red rock of the Australian desert (BSh), like Uluru: a great whale-backed monolith of red terracotta rising
 * sheer out of the flat plain - long and rounded, steep-sided, flat on top, its flanks scored with vertical gullies
 * and banded with orange and brown. It is sunk a few blocks into the ground so it never stands on stilts.
 */
public class RedRockFeature extends Feature<NoneFeatureConfiguration> {
    // how deep the rock goes below the plain
    private static final int ROOT = 4;

    public RedRockFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        // long and narrow, on a random heading
        float length = 9.0F + random.nextFloat() * 5.0F, width = 5.0F + random.nextFloat() * 3.5F;
        int height = 8 + random.nextInt(9);
        float heading = random.nextFloat() * Mth.PI;
        float ca = Mth.cos(heading), sa = Mth.sin(heading);
        int reach = Mth.ceil(length) + 1;
        int gullySeed = random.nextInt(1000);
        int base = origin.getY() - ROOT;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                // in the rock's own frame: u along it, v across it
                float u = (dx * ca + dz * sa) / length, v = (-dx * sa + dz * ca) / width;
                float r = u * u + v * v;
                if (r > 1.0F) continue;
                // a squarish dome: steep sides, then a broad flat top (a superellipse of the distance from the middle)
                float shoulder = 1.0F - (float) Math.pow(r, 3.0);
                // gullies: narrow vertical grooves down the flanks
                float angle = (float) Mth.atan2(v, u);
                float groove = Mth.sin(angle * 9.0F + gullySeed) > 0.85F && r > 0.55F ? 0.75F : 1.0F;
                int top = origin.getY() + Mth.floor(height * shoulder * groove);
                for (int y = base; y <= top; y++) {
                    pos.set(origin.getX() + dx, y, origin.getZ() + dz);
                    level.setBlock(pos, stone(y, origin.getY(), random), Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** The rock's colour: red terracotta, banded here and there with orange, and brown low down. */
    private static BlockState stone(int y, int ground, RandomSource random) {
        int band = Math.floorMod(y - ground, 7);
        if (y <= ground + 1 && random.nextInt(3) == 0) return Blocks.BROWN_TERRACOTTA.defaultBlockState();
        if (band == 3 && random.nextInt(4) != 0) return Blocks.ORANGE_TERRACOTTA.defaultBlockState();
        if (random.nextInt(25) == 0) return Blocks.TERRACOTTA.defaultBlockState();
        return Blocks.RED_TERRACOTTA.defaultBlockState();
    }
}
