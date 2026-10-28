package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.EagleEntity;
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

// A brown eagle in flight: a stout body, a golden-brown head with a hooked yellow beak, broad wings in two parts
// spread wide (about two blocks across), a fanned tail and the yellow talons tucked under. It glides with its wings
// held a little up, flaps when it climbs, and folds them back when it dives.
public class EagleModel extends HierarchicalModel<EagleEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "eagle"), "main");

    private final ModelPart root, leftWing, rightWing, leftTip, rightTip, tail;

    public EagleModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.leftTip = this.leftWing.getChild("tip");
        this.rightTip = this.rightWing.getChild("tip");
        this.tail = body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.0F, -2.0F, -4.0F, 4, 3, 8), PartPose.offset(0.0F, 20.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.5F, -2.0F, -3.0F, 3, 3, 3)
                .texOffs(36, 0).addBox(-0.5F, -1.0F, -4.5F, 1, 1.5F, 1.5F)
                .texOffs(36, 3).addBox(-0.5F, 0.5F, -4.5F, 1, 0.8F, 0.6F), PartPose.offset(0.0F, -1.0F, -4.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(0, 11).addBox(-2.5F, -0.5F, 0.0F, 5, 1, 5), PartPose.offset(0.0F, -1.0F, 4.0F));
        for (int side = -1; side <= 1; side += 2) {
            String name = side < 0 ? "right_wing" : "left_wing";
            CubeListBuilder inner = CubeListBuilder.create().texOffs(0, 17);
            CubeListBuilder outer = CubeListBuilder.create().texOffs(0, 25);
            if (side < 0) {
                inner.mirror().addBox(-8.0F, -0.5F, -3.0F, 8, 1, 6);
                outer.mirror().addBox(-8.0F, -0.5F, -2.5F, 8, 1, 5);
            } else {
                inner.addBox(0.0F, -0.5F, -3.0F, 8, 1, 6);
                outer.addBox(0.0F, -0.5F, -2.5F, 8, 1, 5);
            }
            PartDefinition wing = body.addOrReplaceChild(name, inner, PartPose.offset(side * 2.0F, -1.5F, -0.5F));
            wing.addOrReplaceChild("tip", outer, PartPose.offset(side * 8.0F, 0.0F, 0.0F));
        }
        for (int side = -1; side <= 1; side += 2) {
            body.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create()
                    .texOffs(36, 6).addBox(-0.5F, 0.0F, -0.5F, 1, 2, 1),
                    PartPose.offsetAndRotation(side * 1.0F, 1.0F, 2.0F, 1.1F, 0.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(EagleEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float pitch = entity.getXRot();
        float lift, tip;
        if (pitch > 25.0F) {
            // diving: wings swept back and half folded
            lift = 0.25F;
            tip = 0.9F;
            this.leftWing.yRot = 0.7F;
            this.rightWing.yRot = -0.7F;
        } else if (pitch < -8.0F) {
            // climbing: deep wingbeats
            lift = Mth.sin(ageInTicks * 0.5F) * 0.7F;
            tip = Mth.sin(ageInTicks * 0.5F - 0.6F) * 0.4F;
        } else {
            // soaring: wings held a little up, the tips flexing in the air
            lift = 0.12F + Mth.sin(ageInTicks * 0.08F) * 0.04F;
            tip = 0.08F + Mth.sin(ageInTicks * 0.08F + 0.5F) * 0.05F;
        }
        this.leftWing.zRot = -lift;
        this.rightWing.zRot = lift;
        this.leftTip.zRot = -tip;
        this.rightTip.zRot = tip;
        this.tail.xRot = Mth.sin(ageInTicks * 0.05F) * 0.05F;
    }
}
