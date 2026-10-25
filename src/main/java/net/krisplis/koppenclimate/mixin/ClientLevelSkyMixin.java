package net.krisplis.koppenclimate.mixin;

import net.krisplis.koppenclimate.client.AfSky;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Forge has no hook for the sky colour, so this asks AfSky first: near an Af biome it supplies the colour,
// anywhere else it returns null and vanilla's ClientLevel.getSkyColor runs as normal.
@Mixin(ClientLevel.class)
public abstract class ClientLevelSkyMixin {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void koppenclimatemod$afSkyColor(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Vec3 sky = AfSky.skyColor((ClientLevel) (Object) this, pos, partialTick);
        if (sky != null) {
            cir.setReturnValue(sky);
        }
    }
}
