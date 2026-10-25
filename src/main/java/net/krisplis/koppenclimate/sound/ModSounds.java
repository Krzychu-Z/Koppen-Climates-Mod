package net.krisplis.koppenclimate.sound;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// Biome ambience, put together from Minecraft's own sound files in assets/koppenclimatemod/sounds.json.
// The biomes use them in their "ambient_sound" (a quiet loop) and "additions_sound" (calls now and then).
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, KoppenClimateMod.MOD_ID);

    // rainforest: a constant insect hum, with birds and frogs calling
    public static final RegistryObject<SoundEvent> RAINFOREST_LOOP = register("ambient.rainforest.loop");
    public static final RegistryObject<SoundEvent> RAINFOREST_ADDITIONS = register("ambient.rainforest.additions");
    // jungle beach: wind off the sea, with birds further away
    public static final RegistryObject<SoundEvent> BEACH_LOOP = register("ambient.jungle_beach.loop");
    public static final RegistryObject<SoundEvent> BEACH_ADDITIONS = register("ambient.jungle_beach.additions");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
