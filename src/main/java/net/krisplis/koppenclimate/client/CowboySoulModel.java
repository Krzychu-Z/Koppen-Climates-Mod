package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CowboySoulEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** The cowboy soul: a zombie's body (arms out in front), with a wide-brimmed cowboy hat on its head. */
public class CowboySoulModel extends ZombieModel<CowboySoulEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "cowboy_soul"), "main");

    public CowboySoulModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition head = mesh.getRoot().getChild("head");
        // the brim, just above the brow, and the crown over the top of the head
        head.addOrReplaceChild("hat_brim", CubeListBuilder.create().texOffs(0, 64)
                .addBox(-6.0F, -8.5F, -6.0F, 12.0F, 1.0F, 12.0F), PartPose.ZERO);
        head.addOrReplaceChild("hat_crown", CubeListBuilder.create().texOffs(0, 77)
                .addBox(-4.5F, -12.0F, -4.5F, 9.0F, 4.0F, 9.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 96);
    }
}
