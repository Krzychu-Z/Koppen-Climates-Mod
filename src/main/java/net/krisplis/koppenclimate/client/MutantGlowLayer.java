package net.krisplis.koppenclimate.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** A mutant's glow: its sores and eyes drawn again at full brightness, so they shine in the dark. */
public class MutantGlowLayer<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {
    private final RenderType type;

    public MutantGlowLayer(RenderLayerParent<T, M> parent, ResourceLocation texture) {
        super(parent);
        this.type = RenderType.eyes(texture);
    }

    @Override
    public RenderType renderType() {
        return this.type;
    }
}
