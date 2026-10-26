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

/** Draws the tribe's villagers on the ordinary villager model, with one of the mod's two skins. */
public class AmazonVillagerRenderer extends MobRenderer<AmazonVillagerEntity, VillagerModel<AmazonVillagerEntity>> {
    private static final ResourceLocation TRIBESMAN =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/amazon_villager_tribesman.png");
    private static final ResourceLocation LEADER =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/amazon_villager_leader.png");

    public AmazonVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(AmazonVillagerEntity entity) {
        return entity.isLeader() ? LEADER : TRIBESMAN;
    }

    @Override
    protected void scale(AmazonVillagerEntity entity, PoseStack poseStack, float partialTick) {
        // the same size as a villager
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
