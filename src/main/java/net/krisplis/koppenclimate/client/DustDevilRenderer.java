package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.DustDevilEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The Dust Devil: its funnel of half-see-through, spinning rings of sand. */
public class DustDevilRenderer extends MobRenderer<DustDevilEntity, DustDevilModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/dust_devil.png");

    public DustDevilRenderer(EntityRendererProvider.Context context) {
        super(context, new DustDevilModel(context.bakeLayer(DustDevilModel.LAYER)), 0.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(DustDevilEntity entity) {
        return TEXTURE;
    }

    @Override
    protected RenderType getRenderType(DustDevilEntity entity, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible || translucent ? RenderType.entityTranslucent(TEXTURE) : null;
    }
}
