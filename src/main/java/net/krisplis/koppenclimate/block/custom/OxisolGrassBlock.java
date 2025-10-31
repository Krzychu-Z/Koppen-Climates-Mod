package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public class OxisolGrassBlock extends GrassBlock implements BonemealableBlock {
    private final Supplier<Block> OXISOL;

    public OxisolGrassBlock(BlockBehaviour.Properties props, Supplier<Block> oxisol) {
        super(props);
        this.OXISOL = oxisol;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rng) {
        if (!canBeGrassHere(state, level, pos)) {
            level.setBlockAndUpdate(pos, OXISOL.get().defaultBlockState()); // decay to your soil
            return;
        }

        if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
            BlockState grass = this.defaultBlockState();
            for (int i = 0; i < 4; ++i) {
                BlockPos p = pos.offset(rng.nextInt(3) - 1, rng.nextInt(5) - 3, rng.nextInt(3) - 1);
                if (level.getBlockState(p).is(OXISOL.get()) && canPropagateTo(grass, level, p)) {
                    boolean snowy = level.getBlockState(p.above()).is(Blocks.SNOW);
                    level.setBlockAndUpdate(p, grass.setValue(SnowyDirtBlock.SNOWY, snowy));
                }
            }
        }
    }

    // --- local copies of vanilla logic (names differ; behavior matches) ---

    private static boolean canBeGrassHere(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);

        // one snow layer ok
        if (above.is(BlockTags.SNOW)) return true;

        // water above kills grass
        if (above.getFluidState().is(FluidTags.WATER)) return false;

        // survive unless the block above is a full light-blocking cube
        return !above.isSolidRender(level, abovePos);
    }

    private static boolean canPropagateTo(BlockState grassState, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        // cannot propagate into water
        if (level.getFluidState(abovePos).is(FluidTags.WATER)) return false;
        return canBeGrassHere(grassState, level, pos);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir(); // must have air above
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true; // allow
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos start = pos.above();
        for (int i = 0; i < 128; ++i) {
            BlockPos p = start;

            // vanilla-style random walk
            for (int j = 0; j < i / 16; ++j) {
                p = p.offset(random.nextInt(3) - 1,
                        (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                        random.nextInt(3) - 1);
                if (!level.getBlockState(p).isAir()) continue;

                // allow planting if ground is our grass or our oxisol
                BlockState ground = level.getBlockState(p.below());
                ground.is(this);

                // ---- pick what to place ----
                BlockState plant;
                if (random.nextInt(8) == 0) {
                    Block[] flowers = {
                            Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY
                    };
                    plant = flowers[random.nextInt(flowers.length)].defaultBlockState();
                } else {
                    // <--- the rename you hit:
                    plant = Blocks.SHORT_GRASS.defaultBlockState();
                }

                if (plant.canSurvive(level, p)) {
                    level.setBlock(p, plant, 2);
                }
            }
        }
    }
}