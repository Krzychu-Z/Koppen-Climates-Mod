package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.MutantZombieEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

/**
 * The mutant zombie: a zombie's body in sallow, sore-blotched skin and a faded work jacket, and its mutation drawn
 * from the zombie's own model parts - a second head lolling on its left shoulder, or a third leg dragging behind
 * between the other two.
 */
public class MutantZombieRenderer extends ZombieRenderer {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/mutant/mutant_zombie.png");
    static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/mutant/mutant_zombie_glow.png");

    public MutantZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new Mutation(this));
        this.addLayer(new MutantGlowLayer<>(this, GLOW));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return TEXTURE;
    }

    static class Mutation extends RenderLayer<Zombie, ZombieModel<Zombie>> {
        Mutation(RenderLayerParent<Zombie, ZombieModel<Zombie>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, Zombie entity, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(entity instanceof MutantZombieEntity mutant) || entity.isInvisible()) return;
            ZombieModel<Zombie> model = this.getParentModel();
            VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            VertexConsumer glow = buffers.getBuffer(RenderType.eyes(GLOW));
            int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);
            if (entity instanceof net.krisplis.koppenclimate.entity.custom.CrawlingMutantEntity) return;
            pose.pushPose();
            if (mutant.getVariant() == MutantZombieEntity.TWO_HEADED) {
                // the second head: smaller, on the left shoulder, lolling outward, nodding along with a lag
                pose.translate(0.34F, 0.12F, 0.02F);
                pose.mulPose(Axis.ZP.rotationDegrees(28.0F + (float) Math.sin(ageInTicks * 0.09F) * 6.0F));
                pose.mulPose(Axis.YP.rotationDegrees(-20.0F));
                pose.scale(0.85F, 0.85F, 0.85F);
                model.head.render(pose, buffer, light, overlay);
                model.hat.render(pose, buffer, light, overlay);
                model.head.render(pose, glow, 0xF000F0, overlay);
            } else {
                // the third leg, between and behind the other two, splayed a little, swinging out of step with both
                // (a quarter-stride behind), and dragging further than they do
                float x = model.rightLeg.xRot, y = model.rightLeg.yRot, z = model.rightLeg.zRot;
                model.rightLeg.xRot = net.minecraft.util.Mth.cos(limbSwing * 0.6662F + (float) Math.PI * 0.5F) * 1.6F * limbSwingAmount
                        + 0.25F * limbSwingAmount;
                model.rightLeg.yRot = 0.0F;
                model.rightLeg.zRot = 0.12F + net.minecraft.util.Mth.sin(ageInTicks * 0.15F) * 0.05F;
                pose.translate(1.9F / 16.0F, 0.0F, 3.4F / 16.0F);
                model.rightLeg.render(pose, buffer, light, overlay);
                model.rightLeg.render(pose, glow, 0xF000F0, overlay);
                model.rightLeg.xRot = x;
                model.rightLeg.yRot = y;
                model.rightLeg.zRot = z;
            }
            pose.popPose();
        }
    }
}
