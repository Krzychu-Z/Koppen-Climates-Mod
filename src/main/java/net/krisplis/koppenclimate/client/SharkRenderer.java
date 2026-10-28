package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.SharkEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SharkRenderer extends MobRenderer<SharkEntity, SharkModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/shark.png");

    public SharkRenderer(EntityRendererProvider.Context context) {
        super(context, new SharkModel(context.bakeLayer(SharkModel.LAYER)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(SharkEntity entity) {
        return TEXTURE;
    }

    // a bit bigger than its model's pixels; lies on its side when stranded
    @Override
    protected void scale(SharkEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.25F, 1.25F, 1.25F);
    }

    @Override
    protected void setupRotations(SharkEntity entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        if (!entity.isInWater()) {
            poseStack.translate(0.3F, 0.3F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
