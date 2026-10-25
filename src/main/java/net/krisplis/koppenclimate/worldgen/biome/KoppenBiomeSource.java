package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

// The overworld's biome source: vanilla's own (passed in as "base"), with the Köppen Af biomes swapped in.
// Wherever vanilla would put a jungle it puts the Amazon jungle; rivers and beaches in the same hot, humid
// climate become the Amazon jungle river and the jungle beach. Everything else is left exactly as vanilla
// makes it. Used by data/minecraft/worldgen/world_preset/normal.json, so it applies to new "Default" worlds.
public class KoppenBiomeSource extends BiomeSource {
    public static final MapCodec<KoppenBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("base").forGetter(s -> s.base),
            Biome.CODEC.fieldOf("jungle").forGetter(s -> s.jungle),
            Biome.CODEC.fieldOf("river").forGetter(s -> s.river),
            Biome.CODEC.fieldOf("beach").forGetter(s -> s.beach)
    ).apply(instance, KoppenBiomeSource::new));

    // Vanilla's jungle climate: temperature band 3 (0.2 to 0.55) and humidity bands 3-4 (0.1 and up).
    // Climate values come quantized: multiplied by 10000.
    private static final long HOT_HUMID_MIN_TEMPERATURE = 2000;
    private static final long HOT_HUMID_MAX_TEMPERATURE = 5500;
    private static final long HOT_HUMID_MIN_HUMIDITY = 1000;

    private final BiomeSource base;
    private final Holder<Biome> jungle;
    private final Holder<Biome> river;
    private final Holder<Biome> beach;

    public KoppenBiomeSource(BiomeSource base, Holder<Biome> jungle, Holder<Biome> river, Holder<Biome> beach) {
        this.base = base;
        this.jungle = jungle;
        this.river = river;
        this.beach = beach;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(this.base.possibleBiomes().stream(), Stream.of(this.jungle, this.river, this.beach));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> biome = this.base.getNoiseBiome(x, y, z, sampler);
        if (biome.is(BiomeTags.IS_JUNGLE)) return this.jungle;
        if (biome.is(Biomes.RIVER) || biome.is(Biomes.BEACH)) {
            if (isHotAndHumid(sampler.sample(x, y, z))) {
                return biome.is(Biomes.RIVER) ? this.river : this.beach;
            }
        }
        return biome;
    }

    private static boolean isHotAndHumid(Climate.TargetPoint climate) {
        return climate.temperature() >= HOT_HUMID_MIN_TEMPERATURE && climate.temperature() <= HOT_HUMID_MAX_TEMPERATURE
                && climate.humidity() >= HOT_HUMID_MIN_HUMIDITY;
    }
}
