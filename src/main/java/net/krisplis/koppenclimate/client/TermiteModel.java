package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.TermiteEntity;
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

// A termite: a dark head with mandibles, a small thorax and a fat pale abdomen, on six thin legs that scuttle.
public class TermiteModel extends HierarchicalModel<TermiteEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "termite"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart abdomen;
    private final ModelPart[] legs = new ModelPart[6];

    public TermiteModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.abdomen = body.getChild("abdomen");
        for (int i = 0; i < 6; i++) this.legs[i] = body.getChild("leg" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0F, -1.5F, -1.0F, 2, 1.5F, 2), PartPose.offset(0.0F, 22.8F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 4)
                .addBox(-1.0F, -1.5F, -2.0F, 2, 1.5F, 2)
                .texOffs(8, 0).addBox(-1.0F, -0.6F, -3.0F, 0.5F, 0.5F, 1)
                .texOffs(8, 0).addBox(0.5F, -0.6F, -3.0F, 0.5F, 0.5F, 1), PartPose.offset(0.0F, 0.0F, -1.0F));
        body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 8)
                .addBox(-1.5F, -2.0F, 0.0F, 3, 2, 4), PartPose.offset(0.0F, 0.2F, 1.0F));
        for (int i = 0; i < 6; i++) {
            boolean left = i % 2 == 0;
            float z = -0.7F + (i / 2) * 0.8F;
            body.addOrReplaceChild("leg" + i, CubeListBuilder.create().texOffs(12, 4)
                    .addBox(left ? 0.0F : -2.5F, -0.25F, -0.25F, 2.5F, 0.5F, 0.5F),
                    PartPose.offsetAndRotation(left ? 1.0F : -1.0F, -0.4F, z, 0.0F, 0.0F, left ? 0.5F : -0.5F));
        }
        return LayerDefinition.create(mesh, 32, 16);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(TermiteEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        float speed = limbSwing * 3.0F;
        for (int i = 0; i < 6; i++) {
            float phase = (i % 2 == 0) == ((i / 2) % 2 == 0) ? 0.0F : Mth.PI;
            this.legs[i].yRot = Mth.cos(speed + phase) * 0.6F * limbSwingAmount;
            this.legs[i].zRot += Mth.sin(speed + phase) * 0.3F * limbSwingAmount * (i % 2 == 0 ? 1 : -1);
        }
        this.abdomen.yRot = Mth.sin(ageInTicks * 0.2F) * 0.05F;
    }
}
