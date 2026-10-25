package net.krisplis.koppenclimate.effect;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.brewing.BrewingRecipeRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// Potion of Delirium, brewed from a rafflesia petal: visions (Psychedelia), the world swimming (Nausea)
// and the poison of the flower's rotting-meat sap. All three variants share the name "delirium",
// like vanilla's long_ and strong_ potions, so they read "Potion of Delirium".
//   Delirium         awkward potion + rafflesia petal   Psychedelia 0:45, Nausea 0:45, Poison 0:15
//   Long Delirium    delirium + redstone                Psychedelia 1:30, Nausea 1:30, Poison 0:20
//   Strong Delirium  delirium + glowstone dust          Psychedelia II 0:22, Nausea 0:22, Poison II 0:10
// Splash and lingering versions come from vanilla's gunpowder and dragon's breath mixes.
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, KoppenClimateMod.MOD_ID);
    private static final String NAME = "delirium";

    public static final RegistryObject<Potion> DELIRIUM = POTIONS.register("delirium", () -> new Potion(NAME,
            psychedelia(900, 0), new MobEffectInstance(MobEffects.CONFUSION, 900), new MobEffectInstance(MobEffects.POISON, 300)));
    public static final RegistryObject<Potion> LONG_DELIRIUM = POTIONS.register("long_delirium", () -> new Potion(NAME,
            psychedelia(1800, 0), new MobEffectInstance(MobEffects.CONFUSION, 1800), new MobEffectInstance(MobEffects.POISON, 400)));
    public static final RegistryObject<Potion> STRONG_DELIRIUM = POTIONS.register("strong_delirium", () -> new Potion(NAME,
            psychedelia(440, 1), new MobEffectInstance(MobEffects.CONFUSION, 440), new MobEffectInstance(MobEffects.POISON, 200, 1)));

    private static MobEffectInstance psychedelia(int ticks, int amplifier) {
        return new MobEffectInstance(ModEffects.PSYCHEDELIA.getHolder().orElseThrow(), ticks, amplifier);
    }

    public static void register(IEventBus eventBus) {
        POTIONS.register(eventBus);
    }

    @SubscribeEvent
    public static void onRegisterBrewing(BrewingRecipeRegisterEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();
        builder.addMix(Potions.AWKWARD, ModItems.RAFFLESIA_PETAL.get(), DELIRIUM.getHolder().orElseThrow());
        builder.addMix(DELIRIUM.getHolder().orElseThrow(), Items.REDSTONE, LONG_DELIRIUM.getHolder().orElseThrow());
        builder.addMix(DELIRIUM.getHolder().orElseThrow(), Items.GLOWSTONE_DUST, STRONG_DELIRIUM.getHolder().orElseThrow());
    }
}
