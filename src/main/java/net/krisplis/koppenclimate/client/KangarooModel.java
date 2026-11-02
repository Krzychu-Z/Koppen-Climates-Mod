package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.KangarooEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The kangaroo: a body leaning forward and up, small arms held in front, a long head with tall ears, huge hind legs
 * on long feet, and a thick tail resting behind. It leaps - both hind legs together - and kicks with both at once,
 * swinging them right up in front of it. The boxes match textures/entity/kangaroo.png.
 */
public class KangarooModel extends HierarchicalModel<KangarooEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "kangaroo"), "main");
    private static final float THIGH_REST = 0.35F, TAIL_REST = -0.45F;
    // how far down the thigh the foot hangs, so that (with the thigh's lean) its 1.5-high sole ends exactly on the ground
    private static final float FOOT_DROP = (24.0F - 16.0F - 1.5F) / 0.9394F;

    private final ModelPart root, body, head, legL, legR, tail;

    public KangarooModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.legL = root.getChild("leg_l");
        this.legR = root.getChild("leg_r");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -4.0F, -7.0F, 7, 8, 10),
                PartPose.offsetAndRotation(0.0F, 12.0F, 1.0F, -0.6F, 0.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(34, 0).addBox(-2.5F, -4.0F, -5.0F, 5, 5, 7),
                PartPose.offset(0.0F, 5.0F, -5.0F));
        head.addOrReplaceChild("snout", CubeListBuilder.create().texOffs(34, 12).addBox(-1.5F, -1.0F, -3.0F, 3, 3, 3),
                PartPose.offset(0.0F, -1.0F, -5.0F));
        head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(46, 12).addBox(-1.0F, -4.0F, -0.5F, 2, 4, 1),
                PartPose.offsetAndRotation(1.5F, -4.0F, 0.5F, 0.0F, 0.0F, 0.2F));
        head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(52, 12).addBox(-1.0F, -4.0F, -0.5F, 2, 4, 1),
                PartPose.offsetAndRotation(-1.5F, -4.0F, 0.5F, 0.0F, 0.0F, -0.2F));
        root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(20, 30).addBox(-1.0F, 0.0F, -1.0F, 2, 5, 2),
                PartPose.offsetAndRotation(2.5F, 10.0F, -5.0F, -0.5F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(28, 30).addBox(-1.0F, 0.0F, -1.0F, 2, 5, 2),
                PartPose.offsetAndRotation(-2.5F, 10.0F, -5.0F, -0.5F, 0.0F, 0.0F));
        // slender hind legs: a narrow thigh, then the long flat foot resting right on the ground (its sole at y 24)
        PartDefinition legL = root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, -1.0F, -2.0F, 2, 6, 4),
                PartPose.offsetAndRotation(2.5F, 16.0F, 1.0F, THIGH_REST, 0.0F, 0.0F));
        legL.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(36, 18).addBox(-0.75F, 0.0F, -6.0F, 1.5F, 1.5F, 8.0F),
                PartPose.offsetAndRotation(0.0F, FOOT_DROP, 0.0F, -THIGH_REST, 0.0F, 0.0F));
        PartDefinition legR = root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(18, 18).addBox(-1.0F, -1.0F, -2.0F, 2, 6, 4),
                PartPose.offsetAndRotation(-2.5F, 16.0F, 1.0F, THIGH_REST, 0.0F, 0.0F));
        legR.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(0, 30).addBox(-0.75F, 0.0F, -6.0F, 1.5F, 1.5F, 8.0F),
                PartPose.offsetAndRotation(0.0F, FOOT_DROP, 0.0F, -THIGH_REST, 0.0F, 0.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 40).addBox(-1.5F, 0.0F, 0.0F, 3, 3, 12),
                PartPose.offsetAndRotation(0.0F, 14.0F, 3.0F, TAIL_REST, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(KangarooEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F;
        if (entity.kickTicks > 0) {
            // the kick: both legs flung forward together, the body thrown back onto the tail
            float k = entity.kickTicks / 10.0F;
            this.legL.xRot = THIGH_REST - 1.6F * k;
            this.legR.xRot = THIGH_REST - 1.6F * k;
            this.body.xRot -= 0.3F * k;
            this.tail.xRot = TAIL_REST + 0.25F * k;
            return;
        }
        // it moves only by leaping (KangarooEntity.HopMoveControl): in the air, both hind legs drive back off the
        // ground, then swing forward together to land, the body stretching out and the tail lifting for balance;
        // on landing it sinks onto its legs for a moment, then stands
        float frac = ageInTicks - entity.tickCount;
        if (entity.airTicks > 0) {
            float a = entity.airTicks + frac;
            float push = Mth.clamp(1.0F - a / 5.0F, 0.0F, 1.0F);
            float reach = Mth.clamp((a - 5.0F) / 7.0F, 0.0F, 1.0F);
            float legs = THIGH_REST + 1.1F * push - 0.55F * reach;
            this.legL.xRot = legs;
            this.legR.xRot = legs;
            this.body.xRot += 0.35F * (1.0F - reach * 0.5F);
            this.tail.xRot = TAIL_REST - 0.35F;
        } else if (entity.groundTicks < 5) {
            float g = 1.0F - (entity.groundTicks + frac) / 5.0F;
            if (g > 0.0F) {
                this.body.y += 1.2F * g;
                this.head.y += 1.2F * g;
                this.tail.xRot = TAIL_REST + 0.15F * g;
            }
        }
        this.tail.yRot = Mth.sin(ageInTicks * 0.06F) * 0.08F;
    }
}
