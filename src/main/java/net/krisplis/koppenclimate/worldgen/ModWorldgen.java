package net.krisplis.koppenclimate.worldgen;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.worldgen.biome.KoppenBiomeSource;
import net.krisplis.koppenclimate.worldgen.feature.BiomeSurfaceFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, KoppenClimateMod.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, KoppenClimateMod.MOD_ID);

    public static final RegistryObject<Feature<BiomeSurfaceFeature.Config>> BIOME_SURFACE =
            FEATURES.register("biome_surface", () -> new BiomeSurfaceFeature(BiomeSurfaceFeature.Config.CODEC));

    public static final RegistryObject<MapCodec<KoppenBiomeSource>> KOPPEN_BIOME_SOURCE =
            BIOME_SOURCES.register("koppen", () -> KoppenBiomeSource.CODEC);

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
        BIOME_SOURCES.register(eventBus);
    }
}
