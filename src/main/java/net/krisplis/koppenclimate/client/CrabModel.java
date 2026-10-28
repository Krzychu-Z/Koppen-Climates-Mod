package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CrabEntity;
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

// A little crab: a wide flat shell with two eyes on stalks, eight splayed legs, and two arms ending in big pincers.
// When it follows a coconut (or is angry) it holds both pincers high and snaps them.
public class CrabModel extends HierarchicalModel<CrabEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "crab"), "main");

    private final ModelPart root, leftArm, rightArm, leftClaw, rightClaw;
    private final ModelPart[] legs = new ModelPart[8];

    public CrabModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftClaw = this.leftArm.getChild("claw");
        this.rightClaw = this.rightArm.getChild("claw");
        for (int i = 0; i < 8; i++) this.legs[i] = body.getChild("leg" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.5F, -2.0F, -2.5F, 7, 2, 5)
                // the eyes, up on their stalks
                .texOffs(0, 7).addBox(-1.8F, -3.2F, -2.6F, 0.8F, 1.4F, 0.8F)
                .texOffs(0, 7).addBox(1.0F, -3.2F, -2.6F, 0.8F, 1.4F, 0.8F),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition arm = body.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", CubeListBuilder.create()
                    .texOffs(4, 7).addBox(-0.4F, -0.4F, -2.5F, 0.8F, 0.8F, 2.5F),
                    PartPose.offsetAndRotation(side * 2.5F, -1.0F, -2.2F, 0.0F, -side * 0.5F, 0.0F));
            arm.addOrReplaceChild("claw", CubeListBuilder.create()
                    .texOffs(10, 7).addBox(-0.9F, -0.8F, -2.2F, 1.8F, 1.6F, 2.2F)
                    .texOffs(18, 7).addBox(-0.6F, -0.5F, -3.4F, 0.5F, 0.6F, 1.2F)
                    .texOffs(18, 7).addBox(0.1F, -0.1F, -3.2F, 0.5F, 0.5F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.0F, side * 0.7F, 0.0F));
        }
        for (int i = 0; i < 8; i++) {
            boolean left = i % 2 == 0;
            float z = -1.6F + (i / 2) * 1.05F;
            body.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(24, 0)
                    .addBox(left ? 0.0F : -3.0F, -0.25F, -0.25F, 3.0F, 0.5F, 0.5F),
                    PartPose.offsetAndRotation(left ? 3.2F : -3.2F, -0.8F, z, 0.0F, (left ? -1 : 1) * (0.35F - (i / 2) * 0.25F), left ? 0.6F : -0.6F));
        }
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(CrabEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float speed = limbSwing * 3.0F;
        for (int i = 0; i < 8; i++) {
            float phase = (i % 2 == 0) == ((i / 2) % 2 == 0) ? 0.0F : Mth.PI;
            this.legs[i].yRot += Mth.cos(speed + phase) * 0.45F * limbSwingAmount;
            this.legs[i].zRot += Mth.sin(speed + phase) * 0.3F * limbSwingAmount * (i % 2 == 0 ? 1 : -1);
        }
        if (entity.clawsUp()) {
            // pincers raised high, snapping
            this.leftArm.xRot = -1.1F;
            this.rightArm.xRot = -1.1F;
            this.leftArm.zRot = -0.3F;
            this.rightArm.zRot = 0.3F;
            float snap = Mth.sin(ageInTicks * 0.9F) * 0.3F;
            this.leftClaw.yRot += snap;
            this.rightClaw.yRot -= snap;
            this.leftClaw.xRot = -0.4F;
            this.rightClaw.xRot = -0.4F;
        } else {
            float idle = Mth.sin(ageInTicks * 0.06F) * 0.06F;
            this.leftArm.xRot = idle;
            this.rightArm.xRot = -idle;
        }
    }
}
