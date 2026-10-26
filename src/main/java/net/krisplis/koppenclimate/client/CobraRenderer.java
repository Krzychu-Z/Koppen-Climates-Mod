package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CobraEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CobraRenderer extends MobRenderer<CobraEntity, CobraModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/cobra.png");

    public CobraRenderer(EntityRendererProvider.Context context) {
        super(context, new CobraModel(context.bakeLayer(CobraModel.LAYER)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(CobraEntity entity) {
        return TEXTURE;
    }
}
