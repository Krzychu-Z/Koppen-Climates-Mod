package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.EagleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class EagleRenderer extends MobRenderer<EagleEntity, EagleModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/eagle.png");

    public EagleRenderer(EntityRendererProvider.Context context) {
        super(context, new EagleModel(context.bakeLayer(EagleModel.LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(EagleEntity entity) {
        return TEXTURE;
    }

    // a little bigger than its model's pixels; tilted nose-down in a dive and nose-up when climbing
    @Override
    protected void setupRotations(EagleEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        poseStack.translate(0.0F, 0.3F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-entity.getViewXRot(partialTick)));
        poseStack.translate(0.0F, -0.3F, 0.0F);
    }

    @Override
    protected void scale(EagleEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.3F, 1.3F, 1.3F);
    }
}
