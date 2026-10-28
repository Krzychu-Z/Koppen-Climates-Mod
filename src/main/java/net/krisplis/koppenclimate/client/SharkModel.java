package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.SharkEntity;
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

// A shark, about three blocks long: a thick body with a tall dorsal fin and two pectoral fins, a pointed head with a
// jaw that gapes when it hunts and snaps when it bites, and a two-part tail ending in the tall crescent tail fin.
public class SharkModel extends HierarchicalModel<SharkEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "shark"), "main");

    private final ModelPart root, body, jaw, tail0, tail1, leftFin, rightFin;

    public SharkModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("head").getChild("jaw");
        this.tail0 = this.body.getChild("tail0");
        this.tail1 = this.tail0.getChild("tail1");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -5.0F, -10.0F, 10, 10, 20), PartPose.offset(0.0F, 18.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 30).addBox(-4.0F, -4.5F, -9.0F, 8, 7, 9), PartPose.offset(0.0F, 0.0F, -10.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                .texOffs(36, 30).addBox(-3.5F, 0.0F, -8.0F, 7, 2, 8), PartPose.offset(0.0F, 2.5F, -0.5F));
        body.addOrReplaceChild("dorsal_fin", CubeListBuilder.create()
                .texOffs(100, 0).addBox(-0.5F, -8.0F, 0.0F, 1, 8, 6), PartPose.offsetAndRotation(0.0F, -4.5F, -4.0F, -0.6F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_fin", CubeListBuilder.create()
                .texOffs(66, 30).addBox(0.0F, -0.5F, 0.0F, 8, 1, 5), PartPose.offsetAndRotation(4.5F, 3.0F, -6.0F, 0.0F, -0.5F, 0.5F));
        body.addOrReplaceChild("right_fin", CubeListBuilder.create()
                .texOffs(66, 30).mirror().addBox(-8.0F, -0.5F, 0.0F, 8, 1, 5), PartPose.offsetAndRotation(-4.5F, 3.0F, -6.0F, 0.0F, 0.5F, -0.5F));
        PartDefinition tail0 = body.addOrReplaceChild("tail0", CubeListBuilder.create()
                .texOffs(64, 0).addBox(-3.5F, -3.5F, 0.0F, 7, 7, 10), PartPose.offset(0.0F, -0.5F, 10.0F));
        PartDefinition tail1 = tail0.addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(64, 17).addBox(-2.0F, -2.0F, 0.0F, 4, 4, 8), PartPose.offset(0.0F, 0.0F, 9.5F));
        tail1.addOrReplaceChild("tail_fin_top", CubeListBuilder.create()
                .texOffs(100, 14).addBox(-0.5F, -10.0F, 0.0F, 1, 10, 4), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, -0.75F, 0.0F, 0.0F));
        tail1.addOrReplaceChild("tail_fin_bottom", CubeListBuilder.create()
                .texOffs(112, 14).addBox(-0.5F, 0.0F, 0.0F, 1, 6, 3), PartPose.offsetAndRotation(0.0F, 0.0F, 6.0F, 0.6F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(SharkEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        // it pitches up and down with its swimming
        this.body.xRot = headPitch * Mth.DEG_TO_RAD;
        float speed = entity.isHunting() ? 0.55F : 0.25F;
        float sweep = entity.isHunting() ? 0.3F : 0.18F;
        this.tail0.yRot = Mth.sin(ageInTicks * speed) * sweep;
        this.tail1.yRot = Mth.sin(ageInTicks * speed - 0.8F) * sweep * 1.4F;
        this.leftFin.zRot += Mth.sin(ageInTicks * 0.12F) * 0.08F;
        this.rightFin.zRot -= Mth.sin(ageInTicks * 0.12F) * 0.08F;
        // jaws gape while it hunts, and snap shut on a bite
        float gape = entity.isHunting() ? 0.35F + Mth.sin(ageInTicks * 0.3F) * 0.1F : 0.05F;
        if (this.attackTime > 0.0F) gape = 0.7F * Mth.sin(this.attackTime * Mth.PI) + gape * (1.0F - this.attackTime);
        this.jaw.xRot = gape;
    }
}
