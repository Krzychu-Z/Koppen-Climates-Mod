package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.BlackPantherEntity;
import net.minecraft.client.model.OcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// The ocelot's body, half again as big, in a black coat (textures/entity/black_panther.png)
public class BlackPantherRenderer extends MobRenderer<BlackPantherEntity, OcelotModel<BlackPantherEntity>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/black_panther.png");
    private static final float SIZE = 1.5F;

    public BlackPantherRenderer(EntityRendererProvider.Context context) {
        super(context, new OcelotModel<>(context.bakeLayer(ModelLayers.OCELOT)), 0.4F * SIZE);
    }

    @Override
    protected void scale(BlackPantherEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SIZE, SIZE, SIZE);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackPantherEntity entity) {
        return TEXTURE;
    }
}
