package net.krisplis.koppenclimate.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A two-block-tall rosemary bush: slows you down like the small one, with the same drops (from the lower half). */
public class TallRosemaryBlock extends DoublePlantBlock {
    public TallRosemaryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.makeStuckInBlock(state, RosemaryBlock.SLOWDOWN);
    }
}
