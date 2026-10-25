package net.krisplis.koppenclimate.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

// The giant rafflesia: five thick petals spread across the whole block around a hollow bowl,
// instead of standing up like other flowers. Everything else (soil, suspicious stew, bees) comes from FlowerBlock.
public class RafflesiaBlock extends FlowerBlock {
    // as tall as the bowl's rim
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 5, 15);

    public RafflesiaBlock(Holder<MobEffect> stewEffect, float stewSeconds, Properties properties) {
        super(stewEffect, stewSeconds, properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
