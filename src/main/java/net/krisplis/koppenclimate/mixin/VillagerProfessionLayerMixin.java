package net.krisplis.koppenclimate.mixin;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.village.VillageRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

import java.util.Set;

// Thai and Venetian villagers wear their own work clothes: when the profession outfit of a (living, not zombie) Thai
// or Venetian villager is drawn, the texture comes from textures/entity/villager/thai_profession/ (or
// venetian_profession/) instead of vanilla's. Professions
// from other mods keep their own outfits.
@Mixin(VillagerProfessionLayer.class)
public abstract class VillagerProfessionLayerMixin {
    private static final Set<String> KOPPENCLIMATEMOD$THAI_OUTFITS = Set.of("armorer", "butcher", "cartographer", "cleric",
            "farmer", "fisherman", "fletcher", "leatherworker", "librarian", "mason", "nitwit", "shepherd", "toolsmith", "weaponsmith");

    @Shadow
    @Final
    private String path;

    @ModifyArg(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "INVOKE", ordinal = 1,
                    target = "Lnet/minecraft/client/renderer/entity/layers/VillagerProfessionLayer;renderColoredCutoutModel(Lnet/minecraft/client/model/EntityModel;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;I)V"),
            index = 1)
    private ResourceLocation koppenclimatemod$thaiOutfit(EntityModel<?> model, ResourceLocation texture, PoseStack pose,
                                                         MultiBufferSource buffers, int light, LivingEntity entity, int color) {
        if (!"villager".equals(this.path) || !(entity instanceof VillagerDataHolder holder)) return texture;
        var type = holder.getVillagerData().getType();
        String folder = type == VillageRegistry.THAI.get() ? "thai_profession"
                : type == VillageRegistry.VENETIAN.get() ? "venetian_profession" : null;
        if (folder == null) return texture;
        VillagerProfession profession = holder.getVillagerData().getProfession();
        ResourceLocation key = BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession);
        if (!"minecraft".equals(key.getNamespace()) || !KOPPENCLIMATEMOD$THAI_OUTFITS.contains(key.getPath())) return texture;
        return ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID,
                "textures/entity/villager/" + folder + "/" + key.getPath() + ".png");
    }
}
