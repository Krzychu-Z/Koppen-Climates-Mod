package net.krisplis.koppenclimate.mixin;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The Subtropical plains' tilled farmland soil is farmland too: every crop and stem (wheat, barley, carrots,
// melons...) may be planted on it, just as on vanilla's farmland (only vanilla's is listed in their code).
@Mixin({CropBlock.class, StemBlock.class})
public abstract class CropFarmlandMixin {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void koppenclimatemod$tilledSoil(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(ModBlocks.TILLED_FARMLAND_SOIL.get())) cir.setReturnValue(true);
    }
}
