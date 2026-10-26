package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.PiranhaEntity;
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

// A deep-bodied, flat-sided fish a little bigger than a cod: a blunt head with a jutting lower jaw full of
// teeth, a tall dorsal fin, an anal fin, two pectoral fins and a forked tail. The head faces -z.
public class PiranhaModel extends HierarchicalModel<PiranhaEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "piranha"), "main");

    private final ModelPart root;
    private final ModelPart tail;
    private final ModelPart jaw;

    public PiranhaModel(ModelPart root) {
        this.root = root;
        this.tail = root.getChild("tail_fin");
        this.jaw = root.getChild("head").getChild("jaw");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // body 2 wide, 5 tall, 6 long, its belly just off the ground
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.0F, -2.5F, -3.0F, 2, 5, 6), PartPose.offset(0.0F, 21.5F, 0.0F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 0)
                .addBox(-1.0F, -2.0F, -3.0F, 2, 4, 3), PartPose.offset(0.0F, 21.0F, -3.0F));
        // the lower jaw sticks out past the snout, teeth showing
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(16, 7)
                .addBox(-1.0F, 0.0F, -4.0F, 2, 1, 4), PartPose.offset(0.0F, 1.5F, 0.0F));
        root.addOrReplaceChild("tail_fin", CubeListBuilder.create().texOffs(0, 11)
                .addBox(0.0F, -3.0F, 0.0F, 0, 6, 4), PartPose.offset(0.0F, 21.5F, 3.0F));
        root.addOrReplaceChild("dorsal_fin", CubeListBuilder.create().texOffs(8, 11)
                .addBox(0.0F, -2.0F, -1.0F, 0, 2, 4), PartPose.offset(0.0F, 19.0F, 0.0F));
        root.addOrReplaceChild("anal_fin", CubeListBuilder.create().texOffs(8, 17)
                .addBox(0.0F, 0.0F, 0.0F, 0, 1, 3), PartPose.offset(0.0F, 24.0F, 0.0F));
        root.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(22, 12)
                .addBox(-2.0F, 0.0F, -1.0F, 2, 0, 2), PartPose.offsetAndRotation(-1.0F, 22.5F, -1.5F, 0.0F, 0.0F, -Mth.PI / 4));
        root.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(22, 15)
                .addBox(0.0F, 0.0F, -1.0F, 2, 0, 2), PartPose.offsetAndRotation(1.0F, 22.5F, -1.5F, 0.0F, 0.0F, Mth.PI / 4));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(PiranhaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // a quick tail beat, faster still when it flaps about on land
        float beat = entity.isInWater() ? 1.0F : 1.5F;
        this.tail.yRot = -beat * 0.5F * Mth.sin(0.9F * ageInTicks);
        // the jaw snaps during a bite and works a little all the time
        float bite = this.attackTime > 0.0F ? Mth.sin(this.attackTime * Mth.PI) * 0.6F : 0.0F;
        this.jaw.xRot = 0.08F + 0.05F * Mth.sin(ageInTicks * 0.3F) + bite;
    }
}
