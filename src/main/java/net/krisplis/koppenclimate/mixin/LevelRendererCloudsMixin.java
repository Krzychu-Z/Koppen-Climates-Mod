package net.krisplis.koppenclimate.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.client.AfAmbience;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// No clouds over the hot desert (BWh) or the Mediterranean (Csa): while the player is in one, the clouds aren't drawn.
@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudsMixin {
    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void koppenclimatemod$noDesertClouds(PoseStack poseStack, Matrix4f frustum, Matrix4f projection, float partialTick,
                                                 double camX, double camY, double camZ, CallbackInfo ci) {
        if (AfAmbience.hideClouds()) ci.cancel();
    }
}
