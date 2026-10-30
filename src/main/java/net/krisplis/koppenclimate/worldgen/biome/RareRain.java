package net.krisplis.koppenclimate.worldgen.biome;

import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.thread.EffectiveSide;

/**
 * The Lush desert (BSh) gets rain, but rarely: only when the weather is a thunderstorm, not every time it rains
 * elsewhere (BiomeRareRainMixin). Its biomes are told apart by their climate: temperature 1.6 and downfall 0.1,
 * which no other biome has. Each side keeps its own idea of whether there's a storm on.
 */
public final class RareRain {
    public static final float TEMPERATURE = 1.6F, DOWNFALL = 0.1F;
    public static volatile boolean serverStorm, clientStorm;

    private RareRain() {
    }

    public static boolean isRareRainBiome(Biome biome) {
        Biome.ClimateSettings c = biome.getModifiedClimateSettings();
        return c.hasPrecipitation() && Math.abs(c.temperature() - TEMPERATURE) < 1.0E-4F && Math.abs(c.downfall() - DOWNFALL) < 1.0E-4F;
    }

    /** Whether it may rain in the Lush desert right now, on this thread's side. */
    public static boolean stormNow() {
        return EffectiveSide.get() == LogicalSide.CLIENT ? clientStorm : serverStorm;
    }
}
