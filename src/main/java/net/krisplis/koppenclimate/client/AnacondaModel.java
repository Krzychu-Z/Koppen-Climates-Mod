package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.AnacondaEntity;
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

// A long snake built from a chain of body segments: each segment is a child of the one in front of it,
// so turning one swings everything behind it. setupAnim sends a sine wave down the chain to slither.
// The head faces -z like every Minecraft mob; segment0 sits right behind it and the tail trails along +z.
public class AnacondaModel extends HierarchicalModel<AnacondaEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "anaconda"), "main");

    private static final int SEGMENT_LENGTH = 8;
    // {width, height, texU, texV} per segment, front to tail: six full-width ones, then tapering to the tip
    private static final int[][] SEGMENTS = {
            {6, 5, 0, 0}, {6, 5, 0, 0}, {6, 5, 0, 0}, {6, 5, 0, 0}, {6, 5, 0, 0}, {6, 5, 0, 0},
            {5, 4, 0, 14}, {5, 4, 0, 14}, {4, 3, 0, 27}, {3, 2, 0, 39}};
    // segment0's front edge: puts the snake's middle roughly over the hitbox
    private static final float FRONT_Z = -40.0F;
    private static final float GROUND_Y = 24.0F;

    // Resting coil: joint i turns COIL_BASE + COIL_STEP * i radians, a spiral that tightens toward the tail
    // (about 1.1 turns, 1.5 blocks across) without the body crossing itself
    private static final float COIL_BASE = 0.46F;
    private static final float COIL_STEP = 0.06F;
    // the head turns in toward the coil's middle and lies on top of it
    private static final float COIL_HEAD_YAW = -1.3F;
    private static final float COIL_HEAD_LIFT = 3.0F;
    private static final float BREATH_DEPTH = 0.04F;

    // 0 = stretched out, 1 = fully coiled; set per frame in prepareMobModel
    private float coil;

    private final ModelPart root;
    private final ModelPart[] segments = new ModelPart[SEGMENTS.length];
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tongue;

    public AnacondaModel(ModelPart root) {
        this.root = root;
        this.segments[0] = root.getChild("segment0");
        for (int i = 1; i < SEGMENTS.length; i++) {
            this.segments[i] = this.segments[i - 1].getChild("segment" + i);
        }
        this.head = this.segments[0].getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tongue = this.head.getChild("tongue");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot();
        PartDefinition first = null;
        for (int i = 0; i < SEGMENTS.length; i++) {
            int w = SEGMENTS[i][0], h = SEGMENTS[i][1];
            // boxes sit on the ground: y runs downward in model space, so the box spans -h..0 from its pivot
            CubeListBuilder cube = CubeListBuilder.create().texOffs(SEGMENTS[i][2], SEGMENTS[i][3])
                    .addBox(-w / 2.0F, -h, 0.0F, w, h, SEGMENT_LENGTH);
            PartPose pose = i == 0 ? PartPose.offset(0.0F, GROUND_Y, FRONT_Z) : PartPose.offset(0.0F, 0.0F, SEGMENT_LENGTH);
            parent = parent.addOrReplaceChild("segment" + i, cube, pose);
            if (i == 0) first = parent;
        }

        PartDefinition head = first.addOrReplaceChild("head", CubeListBuilder.create().texOffs(30, 0)
                .addBox(-3.0F, -4.0F, -7.0F, 6, 3, 7), PartPose.ZERO);
        // lower jaw hinges at the back of the head and drops open on a strike
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(30, 11)
                .addBox(-2.5F, 0.0F, -6.0F, 5, 1, 6), PartPose.offset(0.0F, -1.0F, 0.0F));
        // a flat forked tongue poking out of the snout
        head.addOrReplaceChild("tongue", CubeListBuilder.create().texOffs(30, 19)
                .addBox(-0.5F, 0.0F, -3.0F, 1, 0, 3), PartPose.offset(0.0F, -1.5F, -7.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void prepareMobModel(AnacondaEntity entity, float limbSwing, float limbSwingAmount, float partialTick) {
        this.coil = entity.getCoilAmount(partialTick);
    }

    @Override
    public void setupAnim(AnacondaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        // the wave travels down the body as it moves; a gentle sway when it lies still.
        // While curling up or stretching out, blend between that and the coil.
        float moving = Math.min(limbSwingAmount, 1.0F);
        float phase = limbSwing * 0.8F + ageInTicks * 0.05F;
        float amplitude = 0.08F + moving * 0.22F;
        for (int i = 1; i < this.segments.length; i++) {
            this.segments[i].yRot = Mth.lerp(this.coil, amplitude * Mth.sin(phase - i * 0.9F), COIL_BASE + COIL_STEP * i);
        }

        this.head.yRot = Mth.lerp(this.coil, netHeadYaw * Mth.DEG_TO_RAD * 0.5F, COIL_HEAD_YAW);
        this.head.xRot = Mth.lerp(this.coil, headPitch * Mth.DEG_TO_RAD * 0.5F, 0.0F);
        this.head.y = -COIL_HEAD_LIFT * this.coil;
        // slow breathing while it rests: the whole body rises and falls a little
        this.segments[0].yScale = 1.0F + this.coil * BREATH_DEPTH * Mth.sin(ageInTicks * 0.08F);

        // the jaw opens and snaps shut during a strike (attackTime runs 0 -> 1)
        this.jaw.xRot = this.attackTime > 0.0F ? Mth.sin(this.attackTime * Mth.PI) * 0.7F : 0.0F;
        // the tongue flicks out for a moment every few seconds, but not while it sleeps
        this.tongue.visible = this.coil < 0.5F && (ageInTicks % 60.0F) < 6.0F;
    }
}
