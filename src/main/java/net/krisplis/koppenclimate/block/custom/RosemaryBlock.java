package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wild rosemary, the Mediterranean's commonest shrub: a dense, woody little bush. Walking through it is hard work -
 * almost like a cobweb, but twice as fast. Broken, it always gives a stick or two and sometimes a sprig of rosemary
 * (loot_table/blocks/rosemary.json).
 */
public class RosemaryBlock extends BushBlock {
    public static final MapCodec<RosemaryBlock> CODEC = simpleCodec(RosemaryBlock::new);
    // cobweb holds you to a quarter of your speed sideways and 5% vertically; rosemary to twice that
    static final Vec3 SLOWDOWN = new Vec3(0.5, 0.1, 0.5);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public RosemaryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        entity.makeStuckInBlock(state, SLOWDOWN);
    }
}
