package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.TigerEntity;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// The ocelot's body at 1.8x in an orange coat with black stripes (textures/entity/tiger.png)
public class TigerRenderer extends MobRenderer<TigerEntity, OcelotModel<TigerEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/tiger.png");
    private static final float SIZE = 1.8F;

    public TigerRenderer(EntityRendererProvider.Context context) {
        super(context, new OcelotModel<>(context.bakeLayer(ModelLayers.OCELOT)), 0.4F * SIZE);
    }

    @Override
    protected void scale(TigerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SIZE, SIZE, SIZE);
    }

    @Override
    public ResourceLocation getTextureLocation(TigerEntity entity) {
        return TEXTURE;
    }
}
