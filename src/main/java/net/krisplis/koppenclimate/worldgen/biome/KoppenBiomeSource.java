package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.Set;
import java.util.stream.Stream;

// The overworld's biome source: vanilla's own (passed in as "base"), with the Köppen Af biomes swapped in.
//  - Vanilla jungle on flat ground becomes the Amazon jungle; on hilly ground it stays vanilla's sparse jungle,
//    so the Amazon covers the flattest land. Underground, the Amazon has lush caves.
//  - Rivers and beaches in the same hot, humid climate become the Amazon jungle river and the jungle beach.
//  - Around it everything is kept warm: temperate land in that climate becomes sparse jungle, swamps become
//    mangrove swamps and the sea becomes warm ocean, so the Amazon only borders warm biomes and the beach
//    only meets the Amazon and a warm, coral sea.
// Everything else is exactly as vanilla makes it. Used by data/minecraft/worldgen/world_preset/normal.json.
public class KoppenBiomeSource extends BiomeSource {
    public static final MapCodec<KoppenBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("base").forGetter(s -> s.base),
            Biome.CODEC.fieldOf("jungle").forGetter(s -> s.jungle),
            Biome.CODEC.fieldOf("river").forGetter(s -> s.river),
            Biome.CODEC.fieldOf("beach").forGetter(s -> s.beach),
            Biome.CODEC.fieldOf("underground").forGetter(s -> s.underground),
            Biome.CODEC.fieldOf("warm_land").forGetter(s -> s.warmLand),
            Biome.CODEC.fieldOf("warm_swamp").forGetter(s -> s.warmSwamp),
            Biome.CODEC.fieldOf("warm_ocean").forGetter(s -> s.warmOcean)
    ).apply(instance, KoppenBiomeSource::new));

    // Climate values come quantized: multiplied by 10000.
    // The Amazon's climate is vanilla's jungle climate: temperature band 3 (0.2 to 0.55), humidity 0.1 and up.
    private static final long HOT_MIN_TEMPERATURE = 2000;
    private static final long HOT_MAX_TEMPERATURE = 5500;
    private static final long HUMID_MIN_HUMIDITY = 1000;
    // The warm fringe around it: a little cooler and a little drier still counts.
    private static final long FRINGE_MIN_TEMPERATURE = 1000;
    private static final long FRINGE_MIN_HUMIDITY = -1000;
    // Erosion this high or more is vanilla's gentler terrain; below it are the steep hills.
    private static final long FLAT_MIN_EROSION = -2225;
    // Vanilla puts its cave biomes at depth 0.2 and below.
    private static final long UNDERGROUND_MIN_DEPTH = 2000;

    private static final Set<ResourceKey<Biome>> TEMPERATE_LAND = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST,
            Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.MEADOW, Biomes.CHERRY_GROVE);
    private static final Set<ResourceKey<Biome>> COOLER_OCEANS = Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN,
            Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);

    private final BiomeSource base;
    private final Holder<Biome> jungle, river, beach, underground, warmLand, warmSwamp, warmOcean;

    public KoppenBiomeSource(BiomeSource base, Holder<Biome> jungle, Holder<Biome> river, Holder<Biome> beach,
                             Holder<Biome> underground, Holder<Biome> warmLand, Holder<Biome> warmSwamp, Holder<Biome> warmOcean) {
        this.base = base;
        this.jungle = jungle;
        this.river = river;
        this.beach = beach;
        this.underground = underground;
        this.warmLand = warmLand;
        this.warmSwamp = warmSwamp;
        this.warmOcean = warmOcean;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(this.base.possibleBiomes().stream(),
                Stream.of(this.jungle, this.river, this.beach, this.underground, this.warmLand, this.warmSwamp, this.warmOcean));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> biome = this.base.getNoiseBiome(x, y, z, sampler);
        Climate.TargetPoint climate = sampler.sample(x, y, z);

        if (biome.is(Biomes.JUNGLE) || biome.is(Biomes.BAMBOO_JUNGLE)) {
            if (climate.depth() >= UNDERGROUND_MIN_DEPTH) return this.underground;
            return climate.erosion() >= FLAT_MIN_EROSION ? this.jungle : this.warmLand;
        }
        if (isHotAndHumid(climate)) {
            if (biome.is(Biomes.RIVER)) return this.river;
            if (biome.is(Biomes.BEACH)) return this.beach;
        }
        if (isWarmFringe(climate)) {
            if (biome.unwrapKey().map(TEMPERATE_LAND::contains).orElse(false)) return this.warmLand;
            if (biome.is(Biomes.SWAMP)) return this.warmSwamp;
        }
        if (climate.temperature() >= HOT_MIN_TEMPERATURE && climate.humidity() >= FRINGE_MIN_HUMIDITY
                && biome.unwrapKey().map(COOLER_OCEANS::contains).orElse(false)) {
            return this.warmOcean;
        }
        return biome;
    }

    private static boolean isHotAndHumid(Climate.TargetPoint climate) {
        return climate.temperature() >= HOT_MIN_TEMPERATURE && climate.temperature() <= HOT_MAX_TEMPERATURE
                && climate.humidity() >= HUMID_MIN_HUMIDITY;
    }

    private static boolean isWarmFringe(Climate.TargetPoint climate) {
        return climate.temperature() >= FRINGE_MIN_TEMPERATURE && climate.temperature() <= HOT_MAX_TEMPERATURE
                && climate.humidity() >= FRINGE_MIN_HUMIDITY;
    }
}
