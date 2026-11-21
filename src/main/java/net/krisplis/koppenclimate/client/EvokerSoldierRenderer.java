package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.EvokerSoldierEntity;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * The evoker soldier: the evoker's model in a field-grey uniform and greatcoat, a steel helmet on his head (the
 * model's hat layer, which illagers don't otherwise show), his rifle always in hand - at the shoulder when he aims,
 * pointing straight where he looks: at you.
 */
public class EvokerSoldierRenderer extends MobRenderer<EvokerSoldierEntity, IllagerModel<EvokerSoldierEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID,
            "textures/entity/evoker_soldier/evoker_soldier.png");

    public EvokerSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.EVOKER)), 0.5F);
        this.model.getHat().visible = true;
        this.addLayer(new Rifle(this, context.getItemInHandRenderer()));
    }

    @Override
    protected void scale(EvokerSoldierEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public ResourceLocation getTextureLocation(EvokerSoldierEntity entity) {
        return TEXTURE;
    }

    /**
     * The rifle in his hand. Held down, it's drawn as any held item is (levelled forward from the hip). Aiming, his
     * arms come up to the shoulder - which would tip the rifle up to the sky along with them, so it is turned back
     * at the hand: the arm's own turn undone and the head's put in its place, so the barrel points exactly where his
     * eyes look.
     */
    static class Rifle extends ItemInHandLayer<EvokerSoldierEntity, IllagerModel<EvokerSoldierEntity>> {
        private final net.minecraft.client.renderer.ItemInHandRenderer items;

        Rifle(net.minecraft.client.renderer.entity.RenderLayerParent<EvokerSoldierEntity, IllagerModel<EvokerSoldierEntity>> parent,
              net.minecraft.client.renderer.ItemInHandRenderer items) {
            super(parent, items);
            this.items = items;
        }

        @Override
        protected void renderArmWithItem(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.item.ItemStack stack,
                                         net.minecraft.world.item.ItemDisplayContext context, net.minecraft.world.entity.HumanoidArm arm,
                                         PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int light) {
            if (stack.isEmpty() || !(entity instanceof net.minecraft.world.entity.Mob mob && mob.isAggressive()) || arm != entity.getMainArm()) {
                super.renderArmWithItem(entity, stack, context, arm, poseStack, buffer, light);
                return;
            }
            IllagerModel<EvokerSoldierEntity> model = this.getParentModel();
            net.minecraft.client.model.geom.ModelPart armPart = model.root().getChild(arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? "right_arm" : "left_arm");
            net.minecraft.client.model.geom.ModelPart head = model.getHead();
            poseStack.pushPose();
            model.translateToHand(arm, poseStack);
            org.joml.Quaternionf hold = new org.joml.Quaternionf().rotateX((float) -Math.PI / 2).rotateY((float) Math.PI);
            poseStack.mulPose(hold);
            boolean left = arm == net.minecraft.world.entity.HumanoidArm.LEFT;
            poseStack.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            // undo the arm's turn, put the head's in its place: K = hold^-1 * arm^-1 * head * hold
            org.joml.Quaternionf armTurn = new org.joml.Quaternionf().rotationZYX(armPart.zRot, armPart.yRot, armPart.xRot);
            org.joml.Quaternionf headTurn = new org.joml.Quaternionf().rotationZYX(0.0F, head.yRot, head.xRot);
            org.joml.Quaternionf k = new org.joml.Quaternionf(hold).invert().mul(armTurn.invert()).mul(headTurn).mul(hold);
            poseStack.mulPose(k);
            this.items.renderItem(entity, stack, context, left, poseStack, buffer, light);
            poseStack.popPose();
        }
    }
}
