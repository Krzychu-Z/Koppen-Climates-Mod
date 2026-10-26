package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.Optional;
import java.util.Set;
import net.krisplis.koppenclimate.worldgen.island.IslandMap;
import net.krisplis.koppenclimate.worldgen.island.VolcanoStructure;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.DensityFunction;
import java.util.stream.Stream;

// The overworld's biome source: vanilla's own (passed in as "base"), with the Köppen Af biomes swapped in.
//  - Vanilla jungle on flat ground becomes the Amazon jungle; on hilly ground it stays vanilla's sparse jungle,
//    so the Amazon covers the flattest land. Underground, the Amazon has lush caves.
//  - Everywhere vanilla would make sparse jungle (hilly jungle, the jungle's edges, and the temperate land swapped
//    out around it below) becomes the Tropical Monsoon (Aw) forest instead, when one is given.
//  - Small, rare patches of that monsoon forest beside its rivers become the Rice Terraces (Aw), if those are
//    given; a Thai village stands on the river next to each (see RiceTerraceSites).
//  - Rivers and beaches in the same hot, humid climate become the Amazon jungle river and the jungle beach.
//  - Around it everything is kept warm: temperate land in that climate becomes sparse jungle, swamps become
//    mangrove swamps and the sea becomes warm ocean, so the Amazon only borders warm biomes and the beach
//    only meets the Amazon and a warm, coral sea.
//  - Every vanilla savanna (plain, plateau and windswept) becomes the African savanna (As), when one is given,
//    and so does the temperate land on the warm, dry side of the jungles, making it as large as they are.
// Everything else is exactly as vanilla makes it. Used by data/minecraft/worldgen/world_preset/normal.json.
public class KoppenBiomeSource extends BiomeSource {
    public static final MapCodec<KoppenBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("base").forGetter(s -> s.base),
            Biome.CODEC.fieldOf("jungle").forGetter(s -> s.jungle),
            Biome.CODEC.optionalFieldOf("monsoon").forGetter(s -> s.monsoon),
            Biome.CODEC.optionalFieldOf("rice_terraces").forGetter(s -> s.riceTerraces),
            Biome.CODEC.fieldOf("river").forGetter(s -> s.river),
            Biome.CODEC.fieldOf("beach").forGetter(s -> s.beach),
            Biome.CODEC.fieldOf("underground").forGetter(s -> s.underground),
            Biome.CODEC.fieldOf("warm_land").forGetter(s -> s.warmLand),
            Biome.CODEC.fieldOf("warm_swamp").forGetter(s -> s.warmSwamp),
            Biome.CODEC.fieldOf("warm_ocean").forGetter(s -> s.warmOcean),
            Biome.CODEC.optionalFieldOf("island_summit").forGetter(s -> s.islandSummit),
            Biome.CODEC.optionalFieldOf("savanna").forGetter(s -> s.savanna),
            Biome.CODEC.optionalFieldOf("mediterranean").forGetter(s -> s.mediterranean),
            Biome.CODEC.optionalFieldOf("mediterranean_beach").forGetter(s -> s.mediterraneanBeach)
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
    // The savanna reaches a little cooler than the jungle fringe, and deserts only keep their driest part.
    // (the same bounds unquantized, which SavannaCeiling flattens the terrain by)
    static final double SAVANNA_MIN_TEMP = 0.0, SAVANNA_MAX_TEMP = 0.55, SAVANNA_MAX_HUMIDITY = -0.1,
            SAVANNA_DESERT_MIN_HUMIDITY = -0.35, SAVANNA_MIN_CONTINENTALNESS = -0.11;
    private static final long SAVANNA_MIN_TEMPERATURE = 0;
    private static final long DESERT_MAX_HUMIDITY = -3500;
    private static final long SAVANNA_MIN_CONTINENTS = -1100;
    // what keeps its own biome inside the savanna's climate: water and its shores
    private static final Set<ResourceKey<Biome>> SAVANNA_KEEPS = Set.of(Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.BEACH,
            Biomes.STONY_SHORE, Biomes.SNOWY_BEACH, Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.LUKEWARM_OCEAN,
            Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN,
            Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.MUSHROOM_FIELDS);
    // The Mediterranean (Csa): the dry side of the temperate band, between the savanna (warmer and drier) and
    // vanilla's temperate forests (wetter). Unquantized, for CsaHills, and quantized for the biome choice.
    static final double CSA_MIN_TEMP = -0.15, CSA_MAX_TEMP = 0.1, CSA_MAX_HUMIDITY = 0.0, CSA_MIN_CONTINENTALNESS = -0.11;
    private static final long CSA_MIN_TEMPERATURE = -1500, CSA_MAX_TEMPERATURE = 1000, CSA_MAX_HUMIDITY_Q = 0;
    // in the Mediterranean's climate these become the Mediterranean, and its beaches the Mediterranean beach
    private static final Set<ResourceKey<Biome>> CSA_LAND = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST,
            Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.MEADOW,
            Biomes.CHERRY_GROVE, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.SWAMP,
            Biomes.DESERT, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA);
    private static final Set<ResourceKey<Biome>> CSA_SHORES = Set.of(Biomes.BEACH, Biomes.STONY_SHORE, Biomes.SNOWY_BEACH);
    // Vanilla puts its cave biomes at depth 0.2 and below.
    private static final long UNDERGROUND_MIN_DEPTH = 2000;

    private static final Set<ResourceKey<Biome>> TEMPERATE_LAND = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST,
            Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.MEADOW, Biomes.CHERRY_GROVE);
    private static final Set<ResourceKey<Biome>> COOLER_OCEANS = Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN,
            Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);

    private final BiomeSource base;
    private final Holder<Biome> jungle, river, beach, underground, warmLand, warmSwamp, warmOcean;
    // the Aw monsoon forest, which takes the place of every sparse jungle; without it they stay sparse jungle
    private final Optional<Holder<Biome>> monsoon;
    // small patches of the monsoon forest by its rivers: rice terraces
    private final Optional<Holder<Biome>> riceTerraces;
    // the grassy top of the volcanic islands (plains); the islands are skipped if it isn't given
    private final Optional<Holder<Biome>> islandSummit;
    // the African savanna (As), which takes the place of every vanilla savanna when given
    private final Optional<Holder<Biome>> savanna;
    // the Mediterranean (Csa) and its white gravel shore
    private final Optional<Holder<Biome>> mediterranean, mediterraneanBeach;
    private static final Set<ResourceKey<Biome>> VANILLA_SAVANNAS = Set.of(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA);
    // vanilla's jungles: with the Amazon and the monsoon forest in place, none of these ever generates
    private static final Set<ResourceKey<Biome>> VANILLA_JUNGLES = Set.of(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE);
    // the island layout for the world being generated, worked out once from its noise
    private record IslandCache(Climate.Sampler sampler, IslandMap islands) {
    }
    private volatile IslandCache islandCache;
    // below this depth inside an island the biome is left as the sea's
    private static final int ISLAND_BIOME_DEPTH = 48;

    public KoppenBiomeSource(BiomeSource base, Holder<Biome> jungle, Optional<Holder<Biome>> monsoon,
                             Optional<Holder<Biome>> riceTerraces, Holder<Biome> river, Holder<Biome> beach,
                             Holder<Biome> underground, Holder<Biome> warmLand, Holder<Biome> warmSwamp, Holder<Biome> warmOcean,
                             Optional<Holder<Biome>> islandSummit, Optional<Holder<Biome>> savanna,
                             Optional<Holder<Biome>> mediterranean, Optional<Holder<Biome>> mediterraneanBeach) {
        this.base = base;
        this.jungle = jungle;
        this.monsoon = monsoon;
        this.riceTerraces = riceTerraces;
        this.river = river;
        this.beach = beach;
        this.underground = underground;
        this.warmLand = warmLand;
        this.warmSwamp = warmSwamp;
        this.warmOcean = warmOcean;
        this.islandSummit = islandSummit;
        this.savanna = savanna;
        this.mediterranean = mediterranean;
        this.mediterraneanBeach = mediterraneanBeach;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(this.base.possibleBiomes().stream(),
                Stream.concat(Stream.of(this.jungle, this.river, this.beach, this.underground, this.warmLand, this.warmSwamp, this.warmOcean),
                        Stream.concat(Stream.concat(Stream.concat(this.islandSummit.stream(), this.savanna.stream()),
                                        Stream.concat(this.mediterranean.stream(), this.mediterraneanBeach.stream())),
                                Stream.concat(this.monsoon.stream(), this.riceTerraces.stream()))))
                // vanilla's savannas never generate once the African savanna replaces them
                .filter(b -> this.savanna.isEmpty() || !b.unwrapKey().map(VANILLA_SAVANNAS::contains).orElse(false))
                .filter(b -> this.monsoon.isEmpty() || !b.unwrapKey().map(VANILLA_JUNGLES::contains).orElse(false));
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> biome = this.mappedBiome(x, y, z, sampler);
        if (this.riceTerraces.isPresent() && this.monsoon.isPresent() && biome == this.monsoon.get()
                && this.terraceSites(sampler).inTerraces(QuartPos.toBlock(x) + 2, QuartPos.toBlock(z) + 2, this.surfaceBiomes(sampler))) {
            return this.riceTerraces.get();
        }
        return biome;
    }

    // ---------------------------------------------------------------- rice terraces and Thai villages

    private record TerraceCache(Climate.Sampler sampler, RiceTerraceSites sites) {
    }
    private volatile TerraceCache terraceCache;
    // the surface, for working out where rivers and monsoon forest are
    private static final int SURFACE_Y = 64;

    private RiceTerraceSites terraceSites(Climate.Sampler sampler) {
        TerraceCache cache = this.terraceCache;
        if (cache == null || cache.sampler() != sampler) {
            double t = sampler.temperature().compute(new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            cache = new TerraceCache(sampler, new RiceTerraceSites(IslandMap.layoutSeed(t) * 31L + 7L));
            this.terraceCache = cache;
        }
        return cache.sites();
    }

    private RiceTerraceSites.SurfaceBiomes surfaceBiomes(Climate.Sampler sampler) {
        return new RiceTerraceSites.SurfaceBiomes() {
            @Override
            public boolean isRiver(int x, int z) {
                return KoppenBiomeSource.this.surfaceBiome(x, z, sampler) == KoppenBiomeSource.this.river;
            }

            @Override
            public boolean isMonsoon(int x, int z) {
                return KoppenBiomeSource.this.monsoon.isPresent()
                        && KoppenBiomeSource.this.surfaceBiome(x, z, sampler) == KoppenBiomeSource.this.monsoon.get();
            }
        };
    }

    private Holder<Biome> surfaceBiome(int x, int z, Climate.Sampler sampler) {
        return this.mappedBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(SURFACE_Y), QuartPos.fromBlock(z), sampler);
    }

    /** The rice terraces site of the cell this block lies in, if it has one (used to place the Thai villages). */
    public java.util.Optional<RiceTerraceSites.Site> riceTerraceSite(int blockX, int blockZ, Climate.Sampler sampler) {
        if (this.riceTerraces.isEmpty() || this.monsoon.isEmpty()) return java.util.Optional.empty();
        return this.terraceSites(sampler).site(Math.floorDiv(blockX, RiceTerraceSites.CELL), Math.floorDiv(blockZ, RiceTerraceSites.CELL),
                this.surfaceBiomes(sampler));
    }

    // every biome swap but the rice terraces
    private Holder<Biome> mappedBiome(int x, int y, int z, Climate.Sampler sampler) {
        // the tropical volcanic islands: jungle beach on the shore, Amazon jungle up the slopes, plains on top
        if (this.islandSummit.isPresent()) {
            Holder<Biome> island = this.islandBiome(x, y, z, sampler);
            if (island != null) return island;
        }
        Holder<Biome> biome = this.base.getNoiseBiome(x, y, z, sampler);
        Climate.TargetPoint climate = sampler.sample(x, y, z);
        // the savanna takes the whole dry side of the warm belt the jungles fill on the wet side, and the less
        // parched edges of the deserts beyond it; SavannaCeiling flattens the very same ground
        if (this.savanna.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && isSavannaClimate(climate)
                && !biome.unwrapKey().map(SAVANNA_KEEPS::contains).orElse(false)) {
            return this.savanna.get();
        }
        // the Mediterranean (Csa) takes the dry side of the temperate band; CsaHills raises the very same ground
        if (this.mediterranean.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && isMediterraneanClimate(climate)) {
            if (biome.unwrapKey().map(CSA_LAND::contains).orElse(false)) return this.mediterranean.get();
            if (this.mediterraneanBeach.isPresent() && biome.unwrapKey().map(CSA_SHORES::contains).orElse(false)) {
                return this.mediterraneanBeach.get();
            }
        }
        // vanilla savannas outside that climate (the hottest, driest ones) are desert
        if (this.savanna.isPresent() && biome.unwrapKey().map(VANILLA_SAVANNAS::contains).orElse(false)) {
            return climate.depth() >= UNDERGROUND_MIN_DEPTH ? biome : this.base.possibleBiomes().stream()
                    .filter(b -> b.is(Biomes.DESERT)).findFirst().orElse(this.savanna.get());
        }

        if (biome.is(Biomes.JUNGLE) || biome.is(Biomes.BAMBOO_JUNGLE)) {
            if (climate.depth() >= UNDERGROUND_MIN_DEPTH) return this.underground;
            return climate.erosion() >= FLAT_MIN_EROSION ? this.jungle : this.warmLand(climate);
        }
        if (biome.is(Biomes.SPARSE_JUNGLE)) return this.warmLand(climate);
        if (isHotAndHumid(climate)) {
            if (biome.is(Biomes.RIVER)) return this.river;
            if (biome.is(Biomes.BEACH)) return this.beach;
        }
        if (isWarmFringe(climate)) {
            if (biome.unwrapKey().map(TEMPERATE_LAND::contains).orElse(false)) return this.warmLand(climate);
            if (biome.is(Biomes.SWAMP)) return this.warmSwamp;
        }
        if (climate.temperature() >= HOT_MIN_TEMPERATURE && climate.humidity() >= FRINGE_MIN_HUMIDITY
                && biome.unwrapKey().map(COOLER_OCEANS::contains).orElse(false)) {
            return this.warmOcean;
        }
        return biome;
    }

    static boolean isMediterraneanClimate(Climate.TargetPoint c) {
        long t = c.temperature(), h = c.humidity();
        if (t < CSA_MIN_TEMPERATURE || t >= CSA_MAX_TEMPERATURE || h >= CSA_MAX_HUMIDITY_Q) return false;
        // the warm, dry corner is the savanna's
        return !(t >= SAVANNA_MIN_TEMPERATURE && h < FRINGE_MIN_HUMIDITY);
    }

    static boolean isSavannaClimate(Climate.TargetPoint c) {
        return c.continentalness() >= SAVANNA_MIN_CONTINENTS && c.humidity() < FRINGE_MIN_HUMIDITY
                && c.temperature() >= SAVANNA_MIN_TEMPERATURE
                && (c.temperature() <= HOT_MAX_TEMPERATURE || c.humidity() >= DESERT_MAX_HUMIDITY);
    }

    private Holder<Biome> islandBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        IslandCache cache = this.islandCache;
        if (cache == null || cache.sampler() != sampler) {
            double t = sampler.temperature().compute(new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            cache = new IslandCache(sampler, IslandMap.forSeed(IslandMap.layoutSeed(t)));
            this.islandCache = cache;
        }
        IslandMap islands = cache.islands();
        int bx = QuartPos.toBlock(qx) + 2, bz = QuartPos.toBlock(qz) + 2;
        double h = islands.height(bx, bz, VolcanoStructure.probe(sampler));
        // the sea around the islands stays sea
        if (Double.isNaN(h) || h < 0.0) return null;
        if (QuartPos.toBlock(qy) < IslandMap.SEA_LEVEL + h - ISLAND_BIOME_DEPTH) return null;
        if (h <= IslandMap.BEACH_TOP) return this.beach;
        if (h <= IslandMap.JUNGLE_TOP) return this.jungle;
        return this.islandSummit.get();
    }

    // the warm land around the Amazon: the monsoon forest if there is one, else sparse jungle
    private Holder<Biome> warmLand(Climate.TargetPoint climate) {
        return this.monsoon.orElse(this.warmLand);
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
