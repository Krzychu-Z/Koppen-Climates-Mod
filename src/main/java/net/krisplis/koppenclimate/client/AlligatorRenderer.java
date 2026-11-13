package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CrocodileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The alligator: the crocodile's model, smaller, in its own dark, near-black hide. */
public class AlligatorRenderer extends MobRenderer<CrocodileEntity, CrocodileModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/alligator.png");
    private static final float SCALE = 0.7F;

    public AlligatorRenderer(EntityRendererProvider.Context context) {
        super(context, new CrocodileModel(context.bakeLayer(CrocodileModel.LAYER)), 0.8F * SCALE);
    }

    @Override
    protected void scale(CrocodileEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(CrocodileEntity entity) {
        return TEXTURE;
    }
}
