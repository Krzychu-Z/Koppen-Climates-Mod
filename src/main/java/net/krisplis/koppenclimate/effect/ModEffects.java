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

    // Haunted: while in a ghost town (GhostTownNights); the client turns the sky yellow for it. Nothing else.
    public static final RegistryObject<MobEffect> HAUNTED = MOB_EFFECTS.register("haunted",
            () -> new PsychedeliaEffect(MobEffectCategory.NEUTRAL, 0xE3C04A));

    // Cedar Scent: from cedar perfume; friendly animals and villagers come to the player, hostile mobs keep away (CedarScent)
    public static final RegistryObject<MobEffect> CEDAR_SCENT = MOB_EFFECTS.register("cedar_scent",
            () -> new PsychedeliaEffect(MobEffectCategory.BENEFICIAL, 0xB0703C));

    // Oleander poisoning: from falling into a nerium bush; poison that doesn't stop at half a heart, and kills slowly
    public static final RegistryObject<MobEffect> OLEANDER_POISONING = MOB_EFFECTS.register("oleander_poisoning",
            () -> new OleanderPoisoningEffect(MobEffectCategory.HARMFUL, 0x6E8F2A));

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
