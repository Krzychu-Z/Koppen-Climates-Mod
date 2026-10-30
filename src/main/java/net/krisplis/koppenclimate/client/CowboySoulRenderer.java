package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.CowboySoulEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The cowboy soul, floating over the ground (its shadow stays small and faint), glowing, with burning eyes. */
public class CowboySoulRenderer extends HumanoidMobRenderer<CowboySoulEntity, CowboySoulModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/cowboy_soul.png");

    private static final net.minecraft.client.renderer.RenderType EYES = net.minecraft.client.renderer.RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/cowboy_soul_eyes.png"));

    public CowboySoulRenderer(EntityRendererProvider.Context context) {
        super(context, new CowboySoulModel(context.bakeLayer(CowboySoulModel.LAYER)), 0.35F);
        this.shadowStrength = 0.5F;
        // its burning eyes, bright even in the dark
        this.addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
            @Override
            public net.minecraft.client.renderer.RenderType renderType() {
                return EYES;
            }
        });
    }

    // it glows: always drawn fully lit, whatever the light around it
    @Override
    protected int getBlockLightLevel(CowboySoulEntity entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(CowboySoulEntity entity) {
        return TEXTURE;
    }
}
