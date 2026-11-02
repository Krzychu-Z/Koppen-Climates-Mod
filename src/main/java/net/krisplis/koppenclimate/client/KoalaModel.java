package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.KoalaEntity;
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
import net.minecraft.world.entity.Entity;

/**
 * The koala: a round grey body sitting upright, a broad head with big fluffy white-edged ears and a big black nose,
 * and its arms held out in front of it - around its tree trunk when it clings to one, around nothing (or your
 * shoulder) otherwise. The boxes match textures/entity/koala.png. Also drawn on players' shoulders (KoalaOnShoulderLayer).
 */
public class KoalaModel<T extends Entity> extends HierarchicalModel<T> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "koala"), "main");

    private final ModelPart root, body, head, armL, armR, legL, legR;

    public KoalaModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.armL = root.getChild("arm_l");
        this.armR = root.getChild("arm_r");
        this.legL = root.getChild("leg_l");
        this.legR = root.getChild("leg_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -8.0F, -2.5F, 7, 8, 5),
                PartPose.offset(0.0F, 22.0F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-3.0F, -5.0F, -3.0F, 6, 5, 5),
                PartPose.offset(0.0F, 14.0F, -0.5F));
        head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(46, 0).addBox(-1.5F, -1.5F, -0.5F, 3, 3, 1),
                PartPose.offset(3.5F, -4.5F, -0.5F));
        head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(54, 0).addBox(-1.5F, -1.5F, -0.5F, 3, 3, 1),
                PartPose.offset(-3.5F, -4.5F, -0.5F));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(24, 10).addBox(-1.0F, -1.0F, -1.0F, 2, 2, 1),
                PartPose.offset(0.0F, -1.5F, -3.0F));
        root.addOrReplaceChild("arm_l", CubeListBuilder.create().texOffs(0, 13).addBox(-1.0F, 0.0F, -1.0F, 2, 6, 2),
                PartPose.offsetAndRotation(3.5F, 15.0F, -1.5F, -1.2F, 0.0F, 0.0F));
        root.addOrReplaceChild("arm_r", CubeListBuilder.create().texOffs(8, 13).addBox(-1.0F, 0.0F, -1.0F, 2, 6, 2),
                PartPose.offsetAndRotation(-3.5F, 15.0F, -1.5F, -1.2F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(16, 13).addBox(-1.0F, 0.0F, -4.0F, 2, 2, 4),
                PartPose.offset(2.0F, 22.0F, -1.0F));
        root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(28, 13).addBox(-1.0F, 0.0F, -4.0F, 2, 2, 4),
                PartPose.offset(-2.0F, 22.0F, -1.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        boolean clinging = entity instanceof KoalaEntity koala && koala.isClinging();
        if (clinging) {
            // asleep on its trunk: head bowed, arms wrapped round, breathing slowly
            this.head.xRot = 0.35F;
            this.armL.zRot = -0.25F;
            this.armR.zRot = 0.25F;
            this.body.y += Mth.sin(ageInTicks * 0.05F) * 0.15F;
        } else {
            this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
            this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;
            float a = Mth.cos(limbSwing * 0.6662F) * 0.8F * limbSwingAmount;
            this.legL.xRot = a;
            this.legR.xRot = -a;
            this.armL.xRot += -a * 0.5F;
            this.armR.xRot += a * 0.5F;
        }
    }

    /** Sitting on a player's shoulder: half asleep, holding on. */
    public void setupShoulderPose(float ageInTicks) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.xRot = 0.2F + Mth.sin(ageInTicks * 0.04F) * 0.05F;
        this.armL.zRot = -0.2F;
        this.armR.zRot = 0.2F;
    }
}
