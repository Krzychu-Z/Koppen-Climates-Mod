package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.TemplarEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The Templar: a skeleton in full iron armour (his real armour, drawn as any mob's is) and over it the white
 * surcoat with the red cross - a layer of its own, a little larger than the armour, covering the body and hanging
 * down over the thighs - his eyes glowing green through the helmet's slits.
 */
public class TemplarRenderer extends SkeletonRenderer<TemplarEntity> {
    public static final ModelLayerLocation SURCOAT = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "templar"), "surcoat");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID,
            "textures/entity/templar/templar.png");
    private static final ResourceLocation SURCOAT_TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID,
            "textures/entity/templar/templar_surcoat.png");
    private static final RenderType EYES = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID,
            "textures/entity/templar/templar_eyes.png"));

    public TemplarRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON, ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR);
        this.addLayer(new Surcoat(this, new HumanoidModel<>(context.bakeLayer(SURCOAT))));
        this.addLayer(new Eyes(this));
    }

    public static LayerDefinition createSurcoatLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.15F), 0.0F), 64, 32);
    }

    @Override
    public ResourceLocation getTextureLocation(TemplarEntity entity) {
        return TEXTURE;
    }

    static class Surcoat extends RenderLayer<TemplarEntity, SkeletonModel<TemplarEntity>> {
        private final HumanoidModel<TemplarEntity> model;

        Surcoat(RenderLayerParent<TemplarEntity, SkeletonModel<TemplarEntity>> parent, HumanoidModel<TemplarEntity> model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, TemplarEntity entity, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.isInvisible()) return;
            this.getParentModel().copyPropertiesTo(this.model);
            this.model.setAllVisible(false);
            this.model.body.visible = true;
            this.model.leftLeg.visible = true;
            this.model.rightLeg.visible = true;
            renderColoredCutoutModel(this.model, SURCOAT_TEXTURE, poseStack, buffer, packedLight, entity, -1);
        }
    }

    static class Eyes extends EyesLayer<TemplarEntity, SkeletonModel<TemplarEntity>> {
        Eyes(RenderLayerParent<TemplarEntity, SkeletonModel<TemplarEntity>> parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
