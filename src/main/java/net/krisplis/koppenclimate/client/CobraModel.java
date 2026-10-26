package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CobraEntity;
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

// A cobra about two blocks long. The middle of the body sits on the ground over the hitbox; the tail runs back
// from it as a chain of segments and the neck runs forward as two more, ending in the head. Rearing tips the two
// neck segments up until the front third of the snake stands upright, levels the head to look forward, and
// spreads the two hood flaps out from the upper neck. The head faces -z like every Minecraft mob.
public class CobraModel extends HierarchicalModel<CobraEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "cobra"), "main");

    private static final float GROUND_Y = 24.0F;
    // {width, height, length, texU, texV} for the tail segments, front to tip
    private static final int[][] TAIL = {{3, 2, 5, 0, 6}, {2, 2, 5, 20, 0}, {2, 2, 5, 20, 0}, {2, 1, 5, 34, 0}, {1, 1, 4, 48, 0}};
    private static final int NECK_LENGTH = 5;
    // how far each neck segment tips up when fully reared (together a little over a right angle)
    private static final float NECK1_REAR = -1.0F;
    private static final float NECK2_REAR = -0.62F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] tail = new ModelPart[TAIL.length];
    private final ModelPart neck1;
    private final ModelPart neck2;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart hoodLeft;
    private final ModelPart hoodRight;

    // the black mamba uses this body without the hood
    protected boolean hooded = true;

    // 0 = lying flat, 1 = reared; set per frame in prepareMobModel
    private float rear;

    public CobraModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        ModelPart parent = this.body;
        for (int i = 0; i < TAIL.length; i++) {
            this.tail[i] = parent.getChild("tail" + i);
            parent = this.tail[i];
        }
        this.neck1 = this.body.getChild("neck1");
        this.neck2 = this.neck1.getChild("neck2");
        this.head = this.neck2.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.hoodLeft = this.neck2.getChild("hood_left");
        this.hoodRight = this.neck2.getChild("hood_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // boxes sit on the ground: y runs downward in model space, so each spans -h..0 from its pivot
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-1.5F, -2.0F, -2.0F, 3, 2, 4), PartPose.offset(0.0F, GROUND_Y, 0.0F));

        PartDefinition parent = body;
        float z = 2.0F;
        for (int i = 0; i < TAIL.length; i++) {
            int[] t = TAIL[i];
            parent = parent.addOrReplaceChild("tail" + i, CubeListBuilder.create().texOffs(t[3], t[4])
                    .addBox(-t[0] / 2.0F, -t[1], 0.0F, t[0], t[1], t[2]), PartPose.offset(0.0F, 0.0F, z));
            z = t[2];
        }

        // the neck hinges at the bottom of each joint, so tipping it up lifts the front off the ground
        PartDefinition neck1 = body.addOrReplaceChild("neck1", CubeListBuilder.create().texOffs(0, 6)
                .addBox(-1.5F, -2.0F, -NECK_LENGTH, 3, 2, NECK_LENGTH), PartPose.offset(0.0F, 0.0F, -2.0F));
        PartDefinition neck2 = neck1.addOrReplaceChild("neck2", CubeListBuilder.create().texOffs(0, 6)
                .addBox(-1.5F, -2.0F, -NECK_LENGTH, 3, 2, NECK_LENGTH), PartPose.offset(0.0F, 0.0F, -NECK_LENGTH));
        PartDefinition head = neck2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 13)
                .addBox(-2.0F, -3.0F, -4.0F, 4, 3, 4), PartPose.offset(0.0F, 0.0F, -NECK_LENGTH));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(16, 13)
                .addBox(-1.5F, 0.0F, -4.0F, 3, 1, 4), PartPose.offset(0.0F, -1.0F, 0.0F));
        // the hood: two flat flaps either side of the upper neck, folded away until it rears
        neck2.addOrReplaceChild("hood_left", CubeListBuilder.create().texOffs(0, 20)
                .addBox(0.0F, -0.5F, -NECK_LENGTH, 3, 1, NECK_LENGTH), PartPose.offset(1.5F, -1.0F, 0.0F));
        neck2.addOrReplaceChild("hood_right", CubeListBuilder.create().texOffs(16, 20).mirror()
                .addBox(-3.0F, -0.5F, -NECK_LENGTH, 3, 1, NECK_LENGTH), PartPose.offset(-1.5F, -1.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void prepareMobModel(CobraEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.rear = entity.getRearAmount(partialTick);
    }

    @Override
    public void setupAnim(CobraEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float r = this.rear;
        float flat = 1.0F - r;

        // slithering: a wave runs down the body while it moves, and a slow idle drift when it doesn't
        float moving = Math.min(1.0F, limbSwingAmount * 2.0F);
        float phase = limbSwing * 1.2F;
        for (int i = 0; i < this.tail.length; i++) {
            float wave = Mth.sin(phase - (i + 1) * 0.9F) * 0.45F * moving + Mth.sin(ageInTicks * 0.05F - i) * 0.05F;
            this.tail[i].yRot = wave;
        }
        this.body.yRot = Mth.sin(phase) * 0.2F * moving;
        this.neck1.yRot = Mth.sin(phase + 0.9F) * 0.35F * moving * flat;
        this.neck2.yRot = Mth.sin(phase + 1.8F) * 0.3F * moving * flat;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F * flat;

        // rearing: tip the neck upright, turn it to face the threat and sway a little, keep the head level
        this.neck1.xRot = NECK1_REAR * r;
        this.neck2.xRot = NECK2_REAR * r;
        this.neck1.yRot += (netHeadYaw * Mth.DEG_TO_RAD + Mth.sin(ageInTicks * 0.12F) * 0.12F) * r;
        this.head.xRot = -(NECK1_REAR + NECK2_REAR) * r + headPitch * Mth.DEG_TO_RAD * 0.3F * r;

        // the strike: the raised neck whips forward and the jaws gape
        float strike = Mth.sin(this.attackTime * Mth.PI);
        this.neck1.xRot += strike * 0.9F * r;
        this.neck2.xRot += strike * 0.4F * r;
        this.jaw.xRot = Math.max(strike, 0.35F * r);

        // the hood spreads as it rears
        boolean hood = this.hooded && r > 0.05F;
        this.hoodLeft.visible = hood;
        this.hoodRight.visible = hood;
        this.hoodLeft.xScale = Math.max(r, 0.05F);
        this.hoodRight.xScale = Math.max(r, 0.05F);
    }
}
