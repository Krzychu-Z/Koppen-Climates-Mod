package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CrabEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CrabRenderer extends MobRenderer<CrabEntity, CrabModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/crab.png");

    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER)), 0.25F);
    }

    // about a scorpion's size; the young are half that
    @Override
    protected void scale(CrabEntity entity, PoseStack poseStack, float partialTick) {
        float s = entity.isBaby() ? 0.6F : 1.2F;
        poseStack.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(CrabEntity entity) {
        return TEXTURE;
    }
}
