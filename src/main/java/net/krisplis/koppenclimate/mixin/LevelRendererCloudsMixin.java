package net.krisplis.koppenclimate.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.client.AfAmbience;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// No clouds over the hot desert (BWh), the Sahel (BWk) or the Mediterranean (Csa/Csb): walking in, the clouds thin out
// gradually until they're gone, and walking out they come back the same way.
@Mixin(LevelRenderer.class)
public abstract class LevelRendererCloudsMixin {
    @Unique
    private boolean koppenclimatemod$faded;

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void koppenclimatemod$fadeCloudsIn(PoseStack poseStack, Matrix4f frustum, Matrix4f projection, float partialTick,
                                               double camX, double camY, double camZ, CallbackInfo ci) {
        koppenclimatemod$faded = false;
        if (AfAmbience.hideClouds()) {
            ci.cancel();
            return;
        }
        float alpha = AfAmbience.cloudAlpha(partialTick);
        if (alpha < 0.999F) {
            // the cloud shader multiplies its colour by the shader colour, so this fades them
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, alpha));
            koppenclimatemod$faded = true;
        }
    }

    @Inject(method = "renderClouds", at = @At("RETURN"))
    private void koppenclimatemod$fadeCloudsOut(PoseStack poseStack, Matrix4f frustum, Matrix4f projection, float partialTick,
                                                double camX, double camY, double camZ, CallbackInfo ci) {
        if (koppenclimatemod$faded) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            koppenclimatemod$faded = false;
        }
    }
}
