package net.krisplis.koppenclimate.sound;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

// Biome ambience: recorded day and night loops in assets/koppenclimatemod/sounds/ambience, listed in sounds.json.
// client/AfAmbience plays the right one for the biome and the time of day.
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, KoppenClimateMod.MOD_ID);

    // the Amazon jungle and its rivers, by day and by night (played by client/AfAmbience)
    public static final RegistryObject<SoundEvent> AMAZON_DAY = register("ambient.amazon.day");
    public static final RegistryObject<SoundEvent> AMAZON_NIGHT = register("ambient.amazon.night");
    // the jungle beach: the same, with the sea quietly underneath
    public static final RegistryObject<SoundEvent> BEACH_DAY = register("ambient.jungle_beach.day");
    public static final RegistryObject<SoundEvent> BEACH_NIGHT = register("ambient.jungle_beach.night");

    // the African savanna, by day and by night
    public static final RegistryObject<SoundEvent> SAVANNA_DAY = register("ambient.african_savanna.day");
    public static final RegistryObject<SoundEvent> SAVANNA_NIGHT = register("ambient.african_savanna.night");

    // the Mediterranean, by day and by night (the night is the day with crickets over it), and its owls
    public static final RegistryObject<SoundEvent> MEDITERRANEAN_DAY = register("ambient.mediterranean.day");
    public static final RegistryObject<SoundEvent> MEDITERRANEAN_NIGHT = register("ambient.mediterranean.night");
    public static final RegistryObject<SoundEvent> MEDITERRANEAN_BEACH_DAY = register("ambient.mediterranean_beach.day");
    public static final RegistryObject<SoundEvent> MEDITERRANEAN_BEACH_NIGHT = register("ambient.mediterranean_beach.night");
    public static final RegistryObject<SoundEvent> OWL = register("ambient.mediterranean.owl");
    // the hot desert (BWh): its wind by day, and the cry of its eagles
    public static final RegistryObject<SoundEvent> HOT_DESERT_WIND = register("ambient.hot_desert.wind");
    public static final RegistryObject<SoundEvent> EAGLE = register("entity.eagle.cry");
    public static final RegistryObject<SoundEvent> RATTLESNAKE_RATTLE = register("entity.rattlesnake.rattle");

    // the savanna's elephants (trumpeting) and lions (roaring)
    public static final RegistryObject<SoundEvent> ELEPHANT = register("entity.elephant.trumpet");
    public static final RegistryObject<SoundEvent> LION = register("entity.lion.roar");
    public static final RegistryObject<SoundEvent> LION_HURT = register("entity.lion.hurt");
    public static final RegistryObject<SoundEvent> LION_DEATH = register("entity.lion.death");

    // the cowboy souls of the ghost towns: their voices are the ghast's (its screams, moans and death cry, in sounds.json)
    public static final RegistryObject<SoundEvent> COWBOY_SOUL_SCREAM = register("entity.cowboy_soul.scream");
    public static final RegistryObject<SoundEvent> COWBOY_SOUL_AMBIENT = register("entity.cowboy_soul.ambient");
    public static final RegistryObject<SoundEvent> COWBOY_SOUL_HURT = register("entity.cowboy_soul.hurt");
    public static final RegistryObject<SoundEvent> COWBOY_SOUL_DEATH = register("entity.cowboy_soul.death");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
