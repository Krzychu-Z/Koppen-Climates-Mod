package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Sky and fog colour in the Af biomes. The Amazon jungle and its river are always overcast, so rain doesn't
// change their grey sky at all; over the jungle beach the blue sky turns that same grey as rain comes in.
// Everywhere else this returns null / leaves the colour alone, and vanilla's own colours are used.
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AfSky {
    // the Amazon jungle's sky colour, as in worldgen/biome/amazon_jungle.json
    private static final Vec3 OVERCAST = Vec3.fromRGB24(0xA7AEB3);
    private static final float TAU = (float) (Math.PI * 2);

    private AfSky() {
    }

    // Called from ClientLevelSkyMixin before vanilla's ClientLevel.getSkyColor. Mirrors it step for step
    // (blended biome sky, daylight, rain, thunder, lightning flash), except for how Af biomes take the weather.
    // Each step is linear in the colour, so applying the weather per biome sample before blending gives
    // exactly vanilla's result for every other biome, and smooth edges at Af borders.
    public static Vec3 skyColor(ClientLevel level, Vec3 pos, float partialTick) {
        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        float daylight = Mth.clamp(Mth.cos(level.getTimeOfDay(partialTick) * TAU) * 2.0F + 0.5F, 0.0F, 1.0F);
        BiomeManager biomes = level.getBiomeManager();
        boolean[] nearAf = {false};

        Vec3 sky = CubicSampler.gaussianSampleVec3(pos.subtract(2.0, 2.0, 2.0).scale(0.25), (x, y, z) -> {
            Holder<Biome> biome = biomes.getNoiseBiomeAtQuart(x, y, z);
            Vec3 colour = Vec3.fromRGB24(biome.value().getSkyColor()).scale(daylight);
            if (biome.is(ModBiomes.AMAZON_JUNGLE) || biome.is(ModBiomes.AMAZON_JUNGLE_RIVER)) {
                nearAf[0] = true;
                return colour;
            }
            if (biome.is(ModBiomes.JUNGLE_BEACH)) {
                nearAf[0] = true;
                return colour.lerp(OVERCAST.scale(daylight), rain);
            }
            return vanillaWeather(colour, rain, thunder);
        });
        if (!nearAf[0]) return null;

        int flash = level.getSkyFlashTime();
        if (flash > 0) {
            float f = Math.min(flash - partialTick, 1.0F) * 0.45F;
            sky = new Vec3(sky.x * (1.0F - f) + 0.8F * f, sky.y * (1.0F - f) + 0.8F * f, sky.z * (1.0F - f) + f);
        }
        return sky;
    }

    // vanilla's rain and thunder greying of the sky
    private static Vec3 vanillaWeather(Vec3 c, float rain, float thunder) {
        if (rain > 0.0F) {
            double grey = (c.x * 0.3 + c.y * 0.59 + c.z * 0.11) * 0.6;
            double f = 1.0 - rain * 0.75;
            c = new Vec3(c.x * f + grey * (1.0 - f), c.y * f + grey * (1.0 - f), c.z * f + grey * (1.0 - f));
        }
        if (thunder > 0.0F) {
            double grey = (c.x * 0.3 + c.y * 0.59 + c.z * 0.11) * 0.2;
            double f = 1.0 - thunder * 0.75;
            c = new Vec3(c.x * f + grey * (1.0 - f), c.y * f + grey * (1.0 - f), c.z * f + grey * (1.0 - f));
        }
        return c;
    }

    // Vanilla darkens the fog by (1 - rain/2) and (1 - thunder/2). Undo that in the Amazon and on its river;
    // on the jungle beach, fade the fog to grey as the rain comes in instead.
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Camera camera = event.getCamera();
        if (camera.getFluidInCamera() != FogType.NONE || camera.getEntity() == null) return;
        Level level = camera.getEntity().level();
        Holder<Biome> biome = level.getBiome(camera.getBlockPosition());
        boolean overcast = biome.is(ModBiomes.AMAZON_JUNGLE) || biome.is(ModBiomes.AMAZON_JUNGLE_RIVER);
        boolean beach = biome.is(ModBiomes.JUNGLE_BEACH);
        if (!overcast && !beach) return;

        float partialTick = (float) event.getPartialTick();
        float rain = level.getRainLevel(partialTick);
        float darkening = (1.0F - rain * 0.5F) * (1.0F - level.getThunderLevel(partialTick) * 0.5F);
        float r = Math.min(1.0F, event.getRed() / darkening);
        float g = Math.min(1.0F, event.getGreen() / darkening);
        float b = Math.min(1.0F, event.getBlue() / darkening);
        if (beach) {
            float grey = r * 0.3F + g * 0.59F + b * 0.11F;
            r = Mth.lerp(rain, r, grey);
            g = Mth.lerp(rain, g, grey);
            b = Mth.lerp(rain, b, grey);
        }
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }
}
