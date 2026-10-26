package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.block.entity.ModSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/** A sign fixed to a wall, for the mod's woods: vanilla's own block entity type only accepts vanilla signs, so this uses the mod's. */
public class ModWallSignBlock extends WallSignBlock {
    public ModWallSignBlock(WoodType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModSignBlockEntity(pos, state);
    }
}
