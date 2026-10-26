package net.krisplis.koppenclimate.village;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws the tribe's villagers on the ordinary villager model, with the Maasai villager's or the elder's skin. */
public class AfricanVillagerRenderer extends MobRenderer<AfricanVillagerEntity, VillagerModel<AfricanVillagerEntity>> {
    private static final ResourceLocation VILLAGER =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/african_villager.png");
    private static final ResourceLocation ELDER =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/african_villager_elder.png");

    public AfricanVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(AfricanVillagerEntity entity) {
        return entity.isLeader() ? ELDER : VILLAGER;
    }

    @Override
    protected void scale(AfricanVillagerEntity entity, PoseStack poseStack, float partialTick) {
        // the same size as a villager
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
