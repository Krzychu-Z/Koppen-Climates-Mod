package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.krisplis.koppenclimate.entity.custom.HyenaEntity;
import net.krisplis.koppenclimate.entity.custom.KangarooEntity;
import net.krisplis.koppenclimate.entity.custom.KoalaEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Renderers for the Australian desert's animals, and the koala riding on a player's shoulder. */
public final class AustralianRenderers {
    static final ResourceLocation KOALA_TEXTURE = SavannaRenderers.tex("koala");

    private AustralianRenderers() {
    }

    public static MobRenderer<KoalaEntity, KoalaModel<KoalaEntity>> koala(EntityRendererProvider.Context c) {
        return new SavannaRenderers.Scaled<>(c, new KoalaModel<>(c.bakeLayer(KoalaModel.LAYER)), 0.35F, 0.85F, "koala");
    }

    public static MobRenderer<KangarooEntity, KangarooModel> kangaroo(EntityRendererProvider.Context c) {
        return new SavannaRenderers.Scaled<>(c, new KangarooModel(c.bakeLayer(KangarooModel.LAYER)), 0.6F, 1.15F, "kangaroo");
    }

    /** The hyena: a wolf's body, a third bigger, in its spotted coat. */
    public static MobRenderer<HyenaEntity, WolfModel<HyenaEntity>> hyena(EntityRendererProvider.Context c) {
        return new SavannaRenderers.Scaled<>(c, new WolfModel<>(c.bakeLayer(ModelLayers.WOLF)), 0.55F, 1.3F, "hyena") {
            @Override
            protected float getBob(HyenaEntity entity, float partialTick) {
                return entity.getTailAngle();
            }
        };
    }

    /** A koala riding on a player's shoulder, the way vanilla draws a parrot there. */
    public static class KoalaOnShoulderLayer<T extends Player> extends RenderLayer<T, PlayerModel<T>> {
        private static final float SIZE = 0.6F;
        private final KoalaModel<Player> model;

        public KoalaOnShoulderLayer(RenderLayerParent<T, PlayerModel<T>> parent, EntityModelSet models) {
            super(parent);
            this.model = new KoalaModel<>(models.bakeLayer(KoalaModel.LAYER));
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T player, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            this.renderOne(pose, buffers, light, player, ageInTicks, true);
            this.renderOne(pose, buffers, light, player, ageInTicks, false);
        }

        private void renderOne(PoseStack pose, MultiBufferSource buffers, int light, T player, float ageInTicks, boolean left) {
            CompoundTag tag = left ? player.getShoulderEntityLeft() : player.getShoulderEntityRight();
            if (!KoalaEntity.isKoala(tag)) return;
            pose.pushPose();
            // its feet (the model's y 24) on the top of the shoulder
            pose.translate(left ? 0.4F : -0.4F, (player.isCrouching() ? -0.7F : -0.9F), 0.0F);
            pose.scale(SIZE, SIZE, SIZE);
            this.model.setupShoulderPose(ageInTicks);
            VertexConsumer vc = buffers.getBuffer(this.model.renderType(KOALA_TEXTURE));
            this.model.renderToBuffer(pose, vc, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
