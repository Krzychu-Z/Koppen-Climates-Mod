package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.RomanLegionnaireEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The Roman Legionnaire: a skeleton in the legion's red armour and crested galea (its own texture, painted over the
 * skeleton's), his eyes glowing orange in the dark like embers. He raises his shield as a player does.
 */
public class RomanLegionnaireRenderer extends SkeletonRenderer<RomanLegionnaireEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID,
            "textures/entity/roman_legionnaire/roman_legionnaire.png");
    private static final RenderType EYES = RenderType.eyes(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID,
            "textures/entity/roman_legionnaire/roman_legionnaire_eyes.png"));

    public RomanLegionnaireRenderer(EntityRendererProvider.Context context) {
        super(context, ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR, new Model(context.bakeLayer(ModelLayers.SKELETON)));
        this.addLayer(new Eyes(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RomanLegionnaireEntity entity) {
        return TEXTURE;
    }

    /** The skeleton's model, raising the shield arm while he blocks. */
    static class Model extends SkeletonModel<RomanLegionnaireEntity> {
        Model(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(RomanLegionnaireEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
            super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
            if (entity.isBlocking()) {
                if (entity.getMainArm() == HumanoidArm.RIGHT) this.leftArmPose = HumanoidModel.ArmPose.BLOCK;
                else this.rightArmPose = HumanoidModel.ArmPose.BLOCK;
            }
        }

        // a skeleton about to strike holds both arms out; a legionnaire keeps his shield arm before him, or up
        @Override
        public void setupAnim(RomanLegionnaireEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                              float netHeadYaw, float headPitch) {
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            ModelPart shieldArm = entity.getMainArm() == HumanoidArm.RIGHT ? this.leftArm : this.rightArm;
            float side = entity.getMainArm() == HumanoidArm.RIGHT ? 1.0F : -1.0F;
            if (entity.isBlocking()) {
                shieldArm.xRot = -0.9424779F;
                shieldArm.yRot = 0.5235988F * side;
                shieldArm.zRot = 0.0F;
            } else if (entity.isAggressive()) {
                shieldArm.xRot = -0.45F;
                shieldArm.yRot = 0.25F * side;
                shieldArm.zRot = 0.0F;
            }
        }
    }

    static class Eyes extends EyesLayer<RomanLegionnaireEntity, SkeletonModel<RomanLegionnaireEntity>> {
        Eyes(RenderLayerParent<RomanLegionnaireEntity, SkeletonModel<RomanLegionnaireEntity>> parent) {
            super(parent);
        }

        @Override
        public RenderType renderType() {
            return EYES;
        }
    }
}
