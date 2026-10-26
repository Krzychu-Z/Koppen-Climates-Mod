package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.ScorpionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ScorpionRenderer extends MobRenderer<ScorpionEntity, ScorpionModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/scorpion.png");

    public ScorpionRenderer(EntityRendererProvider.Context context) {
        super(context, new ScorpionModel(context.bakeLayer(ScorpionModel.LAYER)), 0.25F);
    }

    // a little bigger than the model's pixels, about half again a termite
    @Override
    protected void scale(ScorpionEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.3F, 1.3F, 1.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(ScorpionEntity entity) {
        return TEXTURE;
    }
}
