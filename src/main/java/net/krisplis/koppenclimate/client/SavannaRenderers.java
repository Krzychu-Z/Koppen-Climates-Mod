package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CobraEntity;
import net.krisplis.koppenclimate.entity.custom.ElephantEntity;
import net.krisplis.koppenclimate.entity.custom.GiraffeEntity;
import net.krisplis.koppenclimate.entity.custom.JackalEntity;
import net.krisplis.koppenclimate.entity.custom.LionEntity;
import net.krisplis.koppenclimate.entity.custom.RhinoEntity;
import net.krisplis.koppenclimate.entity.custom.ZebraEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ChestedHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Renderers for the African savanna's animals. */
public final class SavannaRenderers {
    private SavannaRenderers() {
    }

    static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/" + name + ".png");
    }

    /** One of the generated models, at a size, half that for young ones. */
    static class Scaled<T extends Mob, M extends net.minecraft.client.model.EntityModel<T>> extends MobRenderer<T, M> {
        private final ResourceLocation texture;
        private final float size;

        Scaled(EntityRendererProvider.Context context, M model, float shadow, float size, String texture) {
            super(context, model, shadow * size);
            this.texture = tex(texture);
            this.size = size;
        }

        @Override
        protected void scale(T entity, PoseStack pose, float partialTick) {
            float s = entity.isBaby() ? this.size * 0.5F : this.size;
            pose.scale(s, s, s);
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return this.texture;
        }
    }

    public static MobRenderer<LionEntity, LionModel> lion(EntityRendererProvider.Context c) {
        return new Scaled<>(c, new LionModel(c.bakeLayer(LionModel.LAYER)), 0.7F, 1.0F, "lion");
    }

    public static MobRenderer<ElephantEntity, ElephantModel> elephant(EntityRendererProvider.Context c) {
        return new Scaled<>(c, new ElephantModel(c.bakeLayer(ElephantModel.LAYER)), 1.4F, 2.0F, "elephant");
    }

    public static MobRenderer<RhinoEntity, RhinoModel> rhino(EntityRendererProvider.Context c) {
        return new Scaled<>(c, new RhinoModel(c.bakeLayer(RhinoModel.LAYER)), 1.0F, 2.0F, "rhino");
    }

    public static MobRenderer<GiraffeEntity, GiraffeModel> giraffe(EntityRendererProvider.Context c) {
        return new Scaled<>(c, new GiraffeModel(c.bakeLayer(GiraffeModel.LAYER)), 0.9F, 2.0F, "giraffe");
    }

    /** The black mamba: the cobra's body, longer and hoodless, in olive grey. */
    public static MobRenderer<CobraEntity, CobraModel> mamba(EntityRendererProvider.Context c) {
        CobraModel model = new CobraModel(c.bakeLayer(CobraModel.LAYER));
        model.hooded = false;
        return new MobRenderer<>(c, model, 0.35F) {
            @Override
            protected void scale(CobraEntity entity, PoseStack pose, float partialTick) {
                pose.scale(1.0F, 1.0F, 1.35F);
            }

            @Override
            public ResourceLocation getTextureLocation(CobraEntity entity) {
                return tex("black_mamba");
            }
        };
    }

    /** The jackal: a wolf, a fifth bigger, in its own coat. */
    public static MobRenderer<JackalEntity, WolfModel<JackalEntity>> jackal(EntityRendererProvider.Context c) {
        return new Scaled<>(c, new WolfModel<>(c.bakeLayer(ModelLayers.WOLF)), 0.5F, 1.2F, "jackal") {
            @Override
            protected float getBob(JackalEntity entity, float partialTick) {
                return entity.getTailAngle();
            }
        };
    }

    /** The zebra: a donkey's body in stripes. */
    public static class Zebra extends ChestedHorseRenderer<ZebraEntity> {
        private static final ResourceLocation TEXTURE = tex("zebra");

        public Zebra(EntityRendererProvider.Context context) {
            super(context, 0.92F, ModelLayers.DONKEY);
        }

        @Override
        public ResourceLocation getTextureLocation(ZebraEntity entity) {
            return TEXTURE;
        }
    }
}
