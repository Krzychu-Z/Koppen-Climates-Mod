package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

/** Cotton: a crop of eight stages, like wheat, on farmland; ripe, it bursts into white bolls - four cotton bolls a plant - and its seeds are the cotton seeds. */
public class CottonCropBlock extends CropBlock {
    public static final MapCodec<CottonCropBlock> CODEC = simpleCodec(CottonCropBlock::new);

    public CottonCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CottonCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.COTTON_SEEDS.get();
    }
}
