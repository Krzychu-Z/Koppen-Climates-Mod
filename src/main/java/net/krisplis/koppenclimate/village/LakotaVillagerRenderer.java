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

/** Draws the camp's villagers on the ordinary villager model: a man's or a woman's dress, or the elder's. */
public class LakotaVillagerRenderer extends MobRenderer<LakotaVillagerEntity, VillagerModel<LakotaVillagerEntity>> {
    private static final ResourceLocation MAN =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/lakota_villager.png");
    private static final ResourceLocation WOMAN =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/lakota_villager_woman.png");
    private static final ResourceLocation ELDER =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/lakota_villager_elder.png");

    public LakotaVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(LakotaVillagerEntity entity) {
        if (entity.isLeader()) return ELDER;
        return entity.isWoman() ? WOMAN : MAN;
    }

    @Override
    protected void scale(LakotaVillagerEntity entity, PoseStack poseStack, float partialTick) {
        // the same size as a villager
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }
}
