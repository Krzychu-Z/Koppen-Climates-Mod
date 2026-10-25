package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// A short afternoon shower every day, like the rainforest's daily convection rains.
// Minecraft has one weather for the whole world, so this only starts the shower when a player is in an
// Af biome at shower time; while it lasts it rains everywhere it can rain.
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AfWeather {
    // early afternoon (6000 is noon)
    private static final long SHOWER_TIME = 8000;
    // one to two in-game hours: 1-2 minutes of real time
    private static final int MIN_SHOWER_TICKS = 1200;
    private static final int MAX_SHOWER_TICKS = 2400;
    private static final float THUNDER_CHANCE = 0.2F;

    private AfWeather() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || level.getDayTime() % 24000L != SHOWER_TIME || level.isRaining()) return;

        boolean someoneInAf = level.players().stream().anyMatch(p -> level.getBiome(p.blockPosition()).is(ModBiomes.AF));
        if (!someoneInAf) return;

        RandomSource random = level.getRandom();
        int duration = MIN_SHOWER_TICKS + random.nextInt(MAX_SHOWER_TICKS - MIN_SHOWER_TICKS + 1);
        level.setWeatherParameters(0, duration, true, random.nextFloat() < THUNDER_CHANCE);
    }
}
