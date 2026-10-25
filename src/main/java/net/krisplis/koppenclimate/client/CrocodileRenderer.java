package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CrocodileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CrocodileRenderer extends MobRenderer<CrocodileEntity, CrocodileModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/crocodile.png");

    public CrocodileRenderer(EntityRendererProvider.Context context) {
        super(context, new CrocodileModel(context.bakeLayer(CrocodileModel.LAYER)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(CrocodileEntity entity) {
        return TEXTURE;
    }
}
