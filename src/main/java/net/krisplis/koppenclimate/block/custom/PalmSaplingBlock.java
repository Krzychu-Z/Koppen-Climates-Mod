package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.block.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;

// A sapling that also takes root in sand. Growth, bone meal and random ticks come from SaplingBlock.
// Forge's canSustainPlant asks mayPlaceOn first, so overriding it is enough.
public class PalmSaplingBlock extends SaplingBlock {
    public PalmSaplingBlock(TreeGrower treeGrower, Properties properties) {
        super(treeGrower, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(ModBlockTags.COCONUT_PALM_PLANTABLE_ON);
    }
}
