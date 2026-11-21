package net.krisplis.koppenclimate.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A geothermal vent (the Tundra's hot springs): dark volcanic rock glowing a little in its cracks, hot water
 * welling up from it - bubbles rising through the pool above and steam drifting off the pool's surface. Bathing
 * near one warms you through (SaunaWarmth).
 */
public class GeothermalVentBlock extends Block {
    public GeothermalVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockPos.MutableBlockPos at = pos.mutable().move(0, 1, 0);
        int depth = 0;
        while (depth < 6 && level.getFluidState(at).isSource() && level.getBlockState(at).is(Blocks.WATER)) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.BUBBLE, at.getX() + random.nextDouble(), at.getY() + random.nextDouble(),
                        at.getZ() + random.nextDouble(), 0.0, 0.08, 0.0);
            }
            at.move(0, 1, 0);
            depth++;
        }
        // the steam off the hot water (or straight off the rock, if it lies dry)
        if (level.getBlockState(at).isAir() && random.nextInt(2) == 0) {
            for (int i = 0; i < 1 + random.nextInt(2); i++) {
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                        at.getX() + 0.2 + random.nextDouble() * 0.6, at.getY() + 0.05, at.getZ() + 0.2 + random.nextDouble() * 0.6,
                        (random.nextDouble() - 0.5) * 0.01, 0.02 + random.nextDouble() * 0.02, (random.nextDouble() - 0.5) * 0.01);
            }
        }
        if (depth > 0 && random.nextInt(40) == 0) {
            level.playLocalSound(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT,
                    SoundSource.BLOCKS, 0.25F, 0.8F + random.nextFloat() * 0.2F, false);
        }
    }
}
