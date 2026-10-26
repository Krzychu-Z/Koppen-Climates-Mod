package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.ScorpionEntity;
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

// A scorpion: a flat body, two pincer arms reaching forward, eight legs, and a five-segment tail curling up and
// forward over the back to the sting. The claws snap and the tail sways; the legs scuttle.
public class ScorpionModel extends HierarchicalModel<ScorpionEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "scorpion"), "main");

    private final ModelPart root, tail, leftArm, rightArm;
    private final ModelPart[] legs = new ModelPart[8];

    public ScorpionModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.tail = body.getChild("tail0");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        for (int i = 0; i < 8; i++) this.legs[i] = body.getChild("leg" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -1.2F, -2.5F, 3, 1.2F, 5)             // the back plates
                .texOffs(0, 7).addBox(-1.0F, -1.0F, -3.5F, 2, 1.0F, 1),            // the head
                PartPose.offset(0.0F, 23.0F, 0.0F));
        // pincers: an arm out to the side and forward, ending in a claw
        for (int side = -1; side <= 1; side += 2) {
            PartDefinition arm = body.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", CubeListBuilder.create()
                    .texOffs(16, 0).addBox(side < 0 ? -2.0F : 0.0F, -0.3F, -0.3F, 2, 0.6F, 0.6F),
                    PartPose.offsetAndRotation(side * 1.0F, -0.5F, -3.0F, 0.0F, side * 0.7F, 0.0F));
            arm.addOrReplaceChild("claw", CubeListBuilder.create()
                    .texOffs(16, 2).addBox(-0.6F, -0.5F, -2.0F, 1.2F, 1.0F, 2.0F)
                    .texOffs(16, 6).addBox(-0.6F, -0.3F, -3.0F, 0.5F, 0.6F, 1.0F)
                    .texOffs(16, 6).addBox(0.1F, -0.3F, -3.0F, 0.5F, 0.6F, 1.0F),
                    PartPose.offsetAndRotation(side * 2.0F, 0.0F, 0.0F, 0.0F, -side * 0.9F, 0.0F));
        }
        for (int i = 0; i < 8; i++) {
            boolean left = i % 2 == 0;
            float z = -1.8F + (i / 2) * 1.1F;
            body.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(24, 0)
                    .addBox(left ? 0.0F : -2.5F, -0.2F, -0.2F, 2.5F, 0.4F, 0.4F),
                    PartPose.offsetAndRotation(left ? 1.4F : -1.4F, -0.5F, z, 0.0F, (left ? -1 : 1) * (0.4F - (i / 2) * 0.25F), left ? 0.55F : -0.55F));
        }
        // the tail: five segments, each tipped up a little more, and the sting
        PartDefinition seg = body.addOrReplaceChild("tail0", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-0.6F, -0.6F, 0.0F, 1.2F, 1.2F, 1.4F), PartPose.offsetAndRotation(0.0F, -0.7F, 2.4F, 0.5F, 0.0F, 0.0F));
        for (int i = 1; i < 5; i++) {
            seg = seg.addOrReplaceChild("tail" + i, CubeListBuilder.create().texOffs(0, 10)
                    .addBox(-0.55F, -0.55F, 0.0F, 1.1F, 1.1F, 1.3F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.3F, 0.55F, 0.0F, 0.0F));
        }
        seg.addOrReplaceChild("sting", CubeListBuilder.create().texOffs(8, 10)
                .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F)
                .texOffs(8, 13).addBox(-0.15F, -0.9F, 0.6F, 0.3F, 0.6F, 0.3F), PartPose.offsetAndRotation(0.0F, 0.0F, 1.2F, 0.6F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(ScorpionEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float speed = limbSwing * 2.6F;
        for (int i = 0; i < 8; i++) {
            float phase = (i % 2 == 0) == ((i / 2) % 2 == 0) ? 0.0F : Mth.PI;
            this.legs[i].yRot += Mth.cos(speed + phase) * 0.5F * limbSwingAmount;
            this.legs[i].zRot += Mth.sin(speed + phase) * 0.25F * limbSwingAmount * (i % 2 == 0 ? 1 : -1);
        }
        this.tail.xRot += Mth.sin(ageInTicks * 0.1F) * 0.08F;
        this.tail.yRot = Mth.sin(ageInTicks * 0.07F) * 0.12F;
        float snap = entity.getTarget() != null ? Mth.sin(ageInTicks * 0.8F) * 0.25F : Mth.sin(ageInTicks * 0.05F) * 0.05F;
        this.leftArm.yRot += snap;
        this.rightArm.yRot -= snap;
    }
}
