package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.MutantDogEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** The mutant dog: a wolf's model, mangy toxic-green fur, snarling, glowing yellow eyes (its size comes from its scale). */
public class MutantDogRenderer extends MobRenderer<MutantDogEntity, WolfModel<MutantDogEntity>> {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/mutant/mutant_dog.png");
    static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "textures/entity/mutant/mutant_dog_glow.png");

    public MutantDogRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfModel<>(context.bakeLayer(ModelLayers.WOLF)), 0.6F);
        // its eyes and a sickly shimmer in its fur, glowing in the dark
        this.addLayer(new MutantGlowLayer<>(this, GLOW));
    }

    @Override
    protected float getBob(MutantDogEntity entity, float partialTick) {
        return entity.getTailAngle();
    }

    @Override
    public ResourceLocation getTextureLocation(MutantDogEntity entity) {
        return TEXTURE;
    }
}
