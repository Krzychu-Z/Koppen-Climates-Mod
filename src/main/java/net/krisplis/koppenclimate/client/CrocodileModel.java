package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CrocodileEntity;
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

// A low, heavy crocodile, about 4 blocks long: body with two rows of back scutes, a head with a long snout
// and a hinged lower jaw, four splayed legs and a tail of four segments, each a child of the one before.
// The head faces -z like every Minecraft mob.
public class CrocodileModel extends HierarchicalModel<CrocodileEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "crocodile"), "main");

    // legs splay out sideways, as a crocodile's do
    private static final float LEG_SPLAY = 0.35F;
    // legs fold back along the body to swim
    private static final float SWIM_LEG_TUCK = 1.3F;
    private static final float BASK_JAW_OPEN = 0.8F;
    private static final float BITE_JAW_OPEN = 0.9F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart upperJaw;
    private final ModelPart lowerJaw;
    private final ModelPart frontLeftLeg, frontRightLeg, backLeftLeg, backRightLeg;
    private final ModelPart[] tail = new ModelPart[4];

    // set per frame in prepareMobModel
    private float bask;
    private float swim;

    public CrocodileModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.upperJaw = this.head.getChild("upper_jaw");
        this.lowerJaw = this.head.getChild("lower_jaw");
        this.frontLeftLeg = this.body.getChild("front_left_leg");
        this.frontRightLeg = this.body.getChild("front_right_leg");
        this.backLeftLeg = this.body.getChild("back_left_leg");
        this.backRightLeg = this.body.getChild("back_right_leg");
        this.tail[0] = this.body.getChild("tail0");
        for (int i = 1; i < this.tail.length; i++) {
            this.tail[i] = this.tail[i - 1].getChild("tail" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // body 10 x 5 x 18 with two scute ridges on top; sits 4 px off the ground on its legs
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -2.5F, -9.0F, 10, 5, 18)
                .texOffs(0, 24).addBox(-3.0F, -3.5F, -8.0F, 1, 1, 16)
                .texOffs(0, 24).addBox(2.0F, -3.5F, -8.0F, 1, 1, 16),
                PartPose.offset(0.0F, 17.5F, -8.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(60, 0).addBox(-4.0F, -2.0F, -6.0F, 8, 4, 6)
                .texOffs(92, 0).addBox(-3.5F, -3.0F, -5.0F, 2, 1, 2)     // eyes on top of the head
                .texOffs(92, 0).addBox(1.5F, -3.0F, -5.0F, 2, 1, 2),
                PartPose.offset(0.0F, -0.5F, -9.0F));
        head.addOrReplaceChild("upper_jaw", CubeListBuilder.create()
                .texOffs(60, 11).addBox(-2.5F, -1.0F, -10.0F, 5, 2, 10), PartPose.offset(0.0F, -1.0F, -6.0F));
        // hinged at the back of the head; it reaches all the way to the tip of the snout
        head.addOrReplaceChild("lower_jaw", CubeListBuilder.create()
                .texOffs(60, 24).addBox(-2.5F, 0.0F, -15.0F, 5, 2, 15), PartPose.offset(0.0F, 1.0F, -1.0F));

        CubeListBuilder leg = CubeListBuilder.create().texOffs(92, 4).addBox(-1.5F, 0.0F, -1.5F, 3, 4, 3);
        body.addOrReplaceChild("front_left_leg", leg, PartPose.offsetAndRotation(4.5F, 2.5F, -6.0F, 0.0F, 0.0F, -LEG_SPLAY));
        body.addOrReplaceChild("front_right_leg", leg, PartPose.offsetAndRotation(-4.5F, 2.5F, -6.0F, 0.0F, 0.0F, LEG_SPLAY));
        body.addOrReplaceChild("back_left_leg", leg, PartPose.offsetAndRotation(4.5F, 2.5F, 6.0F, 0.0F, 0.0F, -LEG_SPLAY));
        body.addOrReplaceChild("back_right_leg", leg, PartPose.offsetAndRotation(-4.5F, 2.5F, 6.0F, 0.0F, 0.0F, LEG_SPLAY));

        // tail, tapering, each segment with a ridge of scutes; {width, height, texU, texV, pivot y}
        int[][] tail = {{7, 4, 0, 42}, {5, 3, 32, 42}, {4, 3, 60, 42}, {2, 2, 86, 42}};
        float[] pivotY = {-1.0F, 0.5F, 0.0F, 0.5F};
        PartDefinition parent = body;
        for (int i = 0; i < tail.length; i++) {
            float w = tail[i][0], h = tail[i][1];
            CubeListBuilder cube = CubeListBuilder.create()
                    .texOffs(tail[i][2], tail[i][3]).addBox(-w / 2.0F, -h / 2.0F, 0.0F, w, h, 8)
                    .texOffs(106, 42).addBox(-0.5F, -h / 2.0F - 1.0F, 1.0F, 1, 1, 6);
            PartPose pose = PartPose.offset(0.0F, pivotY[i], i == 0 ? 9.0F : 8.0F);
            parent = parent.addOrReplaceChild("tail" + i, cube, pose);
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void prepareMobModel(CrocodileEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.bask = entity.getBaskAmount(partialTick);
        this.swim = entity.isInWater() ? 1.0F : 0.0F;
    }

    @Override
    public void setupAnim(CrocodileEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float moving = Math.min(limbSwingAmount, 1.0F);

        // walking: diagonal pairs of legs step together, the body swaying a little with them
        float step = Mth.cos(limbSwing * 0.9F) * moving;
        float walk = 1.0F - this.swim;
        this.frontLeftLeg.xRot = Mth.lerp(this.swim, step, SWIM_LEG_TUCK);
        this.backRightLeg.xRot = Mth.lerp(this.swim, step, SWIM_LEG_TUCK);
        this.frontRightLeg.xRot = Mth.lerp(this.swim, -step, SWIM_LEG_TUCK);
        this.backLeftLeg.xRot = Mth.lerp(this.swim, -step, SWIM_LEG_TUCK);
        this.body.yRot = Mth.sin(limbSwing * 0.9F) * 0.08F * moving * walk;

        // the tail sweeps side to side, harder when it swims; a wave runs down it
        float amplitude = 0.08F + moving * 0.2F + this.swim * 0.25F;
        for (int i = 0; i < this.tail.length; i++) {
            this.tail[i].yRot = amplitude * Mth.sin(ageInTicks * 0.1F + limbSwing * 0.6F - i * 0.8F);
        }

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F - this.bask * 0.15F;
        // jaws: open wide while basking, snap during a bite (attackTime runs 0 -> 1)
        float bite = this.attackTime > 0.0F ? Mth.sin(this.attackTime * Mth.PI) * BITE_JAW_OPEN : 0.0F;
        float open = Math.max(this.bask * BASK_JAW_OPEN, bite);
        this.lowerJaw.xRot = open;
        this.upperJaw.xRot = -open * 0.25F;
    }
}
