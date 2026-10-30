package net.krisplis.koppenclimate.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The Dry steppe (BSk): its short grass grows straight out of the sand (HotDesertSurfaceFeature), so the grass
// (vanilla's short grass and fern) may stand on ordinary sand and doesn't pop off when something nearby changes.
@Mixin(BushBlock.class)
public abstract class BushBlockSandMixin {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void koppenclimatemod$grassOnSand(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof TallGrassBlock && state.is(Blocks.SAND)) cir.setReturnValue(true);
    }
}
