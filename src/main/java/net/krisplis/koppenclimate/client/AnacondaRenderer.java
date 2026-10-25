package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.AnacondaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AnacondaRenderer extends MobRenderer<AnacondaEntity, AnacondaModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/anaconda.png");

    public AnacondaRenderer(EntityRendererProvider.Context context) {
        super(context, new AnacondaModel(context.bakeLayer(AnacondaModel.LAYER)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(AnacondaEntity entity) {
        return TEXTURE;
    }
}
