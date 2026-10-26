package net.krisplis.koppenclimate.particle;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, KoppenClimateMod.MOD_ID);

    // a lone mosquito buzzing about: the ambient particle of the Amazon jungle river and the rice terraces
    public static final RegistryObject<SimpleParticleType> MOSQUITO = PARTICLES.register("mosquito", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLES.register(eventBus);
    }
}
