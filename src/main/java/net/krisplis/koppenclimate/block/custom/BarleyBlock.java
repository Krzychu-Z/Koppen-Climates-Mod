package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Barley: grows like wheat, in eight stages, on farmland; its seeds are the barley seeds. */
public class BarleyBlock extends CropBlock {
    public static final MapCodec<BarleyBlock> CODEC = simpleCodec(BarleyBlock::new);

    public BarleyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BarleyBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.BARLEY_SEEDS.get();
    }
}
