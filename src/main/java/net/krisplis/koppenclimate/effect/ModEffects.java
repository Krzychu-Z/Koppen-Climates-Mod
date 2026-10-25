package net.krisplis.koppenclimate.effect;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, KoppenClimateMod.MOD_ID);

    // Visions: while it lasts, the client runs a colour-cycling, rippling screen shader (PsychedeliaClient)
    public static final RegistryObject<MobEffect> PSYCHEDELIA = MOB_EFFECTS.register("psychedelia",
            () -> new PsychedeliaEffect(MobEffectCategory.HARMFUL, 0xC23BD9));

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    // MobEffect's constructor is protected
    static class PsychedeliaEffect extends MobEffect {
        PsychedeliaEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }
}
