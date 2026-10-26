package net.krisplis.koppenclimate.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.HashMap;
import java.util.Map;

// The master fisherman's "boat for emeralds" trade lists one boat per villager type and its constructor refuses
// to run if any registered type is missing - which includes our Thai villagers. Just before that check, any type
// vanilla doesn't list (Thai, or another mod's) is given bamboo rafts.
@Mixin(VillagerTrades.EmeraldsForVillagerTypeItem.class)
public abstract class VillagerTradesMixin {
    @ModifyVariable(method = "<init>", argsOnly = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/DefaultedRegistry;stream()Ljava/util/stream/Stream;"))
    private Map<VillagerType, Item> koppenclimatemod$boatsForEveryType(Map<VillagerType, Item> trades) {
        Map<VillagerType, Item> all = new HashMap<>(trades);
        for (VillagerType type : BuiltInRegistries.VILLAGER_TYPE) {
            all.putIfAbsent(type, Items.BAMBOO_RAFT);
        }
        return all;
    }
}
