package net.krisplis.koppenclimate.item.custom;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The banana: food, and a planting. Used on the side of a banana stem it starts a new bunch (like cocoa beans);
 * used on the top of soil with room above it plants a young banana plant there.
 */
public class BananaItem extends ItemNameBlockItem {
    public BananaItem(Block bunch, Properties properties) {
        super(bunch, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos above = context.getClickedPos().above();
        if (context.getClickedFace() == Direction.UP && level.getBlockState(above).canBeReplaced()) {
            BlockState sapling = ModBlocks.BANANA_SAPLING.get().defaultBlockState();
            if (sapling.canSurvive(level, above)) {
                if (!level.isClientSide) {
                    level.setBlock(above, sapling, 3);
                    level.playSound(null, above, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, above);
                    if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                        context.getItemInHand().shrink(1);
                    }
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return super.useOn(context);
    }
}
