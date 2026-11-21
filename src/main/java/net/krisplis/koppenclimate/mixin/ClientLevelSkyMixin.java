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

    // in Pompeii the sky turns a fiery red, glowing even at night
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void koppenclimatemod$pompeiiSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if (net.krisplis.koppenclimate.client.AfAmbience.pompeii(partialTick) <= 0.0F) return;
        ClientLevel level = (ClientLevel) (Object) this;
        float daylight = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.cos(level.getTimeOfDay(partialTick) * ((float) Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
        cir.setReturnValue(net.krisplis.koppenclimate.client.AfAmbience.pompeiiSky(cir.getReturnValue(), daylight, partialTick));
    }

    // in a ghost town, by day the sky turns a sickly yellow
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void koppenclimatemod$hauntedSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        float haunt = net.krisplis.koppenclimate.client.AfAmbience.haunt(partialTick);
        if (haunt <= 0.0F) return;
        ClientLevel level = (ClientLevel) (Object) this;
        float daylight = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.cos(level.getTimeOfDay(partialTick) * ((float) Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
        Vec3 yellow = new Vec3(0.89, 0.75, 0.29).scale(daylight);
        cir.setReturnValue(cir.getReturnValue().lerp(yellow, haunt * 0.85));
    }

    // over the abandoned Soviet estate the sky turns a poisonous green - still faintly green at night
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void koppenclimatemod$radiationSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        float green = net.krisplis.koppenclimate.client.AfAmbience.radiation(partialTick);
        if (green <= 0.0F) return;
        ClientLevel level = (ClientLevel) (Object) this;
        float daylight = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.cos(level.getTimeOfDay(partialTick) * ((float) Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
        Vec3 sick = new Vec3(0.45, 0.85, 0.2).scale(Math.max(daylight, 0.22F));
        cir.setReturnValue(cir.getReturnValue().lerp(sick, green * 0.9));
    }
}
