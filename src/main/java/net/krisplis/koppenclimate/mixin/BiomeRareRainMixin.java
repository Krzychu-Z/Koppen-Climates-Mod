package net.krisplis.koppenclimate.mixin;

import net.krisplis.koppenclimate.worldgen.biome.RareRain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The Lush desert (BSh) only gets rain in thunderstorms (RareRain).
@Mixin(Biome.class)
public abstract class BiomeRareRainMixin {
    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void koppenclimatemod$rareRain(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (!RareRain.stormNow() && RareRain.isRareRainBiome((Biome) (Object) this)) cir.setReturnValue(Biome.Precipitation.NONE);
    }
}
