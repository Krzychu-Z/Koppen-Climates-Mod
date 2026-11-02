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
//  - On the Mediterranean's cooler side the Redwood forest (Csb); on its wetter side, but only on the low, flat
//    coastal land, the humid subtropics (Cfa): the Subtropical plains, and along the shore the Subtropical swamp,
//    with a lukewarm sea off them.
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
            Biome.CODEC.optionalFieldOf("mediterranean_beach").forGetter(s -> s.mediterraneanBeach),
            CsaBiomes.CODEC.optionalFieldOf("mediterranean_zones").forGetter(s -> s.csa),
            DesertBiomes.CODEC.optionalFieldOf("deserts").forGetter(s -> s.deserts)
    ).apply(instance, KoppenBiomeSource::new));

    /**
     * The rest of the Mediterranean (Csa) biomes, beside "mediterranean" (the shrubland, the commonest) and its beach:
     * which patch of the Csa takes which is up to {@link CsaZones}; the river takes vanilla's rivers in the Csa.
     */
    public record CsaBiomes(Holder<Biome> forest, Holder<Biome> oliveGrove, Holder<Biome> river, Holder<Biome> farmland,
                            Holder<Biome> lavenderField, Optional<Holder<Biome>> highland, Optional<Holder<Biome>> redwood,
                            Optional<Holder<Biome>> subtropicalPlains, Optional<Holder<Biome>> subtropicalSwamp) {
        public static final com.mojang.serialization.Codec<CsaBiomes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("forest").forGetter(CsaBiomes::forest),
                Biome.CODEC.fieldOf("olive_grove").forGetter(CsaBiomes::oliveGrove),
                Biome.CODEC.fieldOf("river").forGetter(CsaBiomes::river),
                Biome.CODEC.fieldOf("farmland").forGetter(CsaBiomes::farmland),
                Biome.CODEC.fieldOf("lavender_field").forGetter(CsaBiomes::lavenderField),
                Biome.CODEC.optionalFieldOf("highland").forGetter(CsaBiomes::highland),
                // the Mediterranean's cooler neighbour, the Redwood forest (Csb), and the humid subtropics beyond it:
                // the humid subtropics (Cfa): the Subtropical plains, and their swamp along the lukewarm shore
                Biome.CODEC.optionalFieldOf("redwood_forest").forGetter(CsaBiomes::redwood),
                Biome.CODEC.optionalFieldOf("subtropical_plains").forGetter(CsaBiomes::subtropicalPlains),
                Biome.CODEC.optionalFieldOf("subtropical_swamp").forGetter(CsaBiomes::subtropicalSwamp)
        ).apply(i, CsaBiomes::new));

        Stream<Holder<Biome>> all() {
            return Stream.of(Stream.of(this.forest, this.oliveGrove, this.river, this.farmland, this.lavenderField),
                    this.highland.stream(), this.redwood.stream(), this.subtropicalPlains.stream(), this.subtropicalSwamp.stream())
                    .flatMap(s -> s);
        }
    }

    /**
     * The deserts, which take the whole hot band beyond the jungle's (and the hottest, driest edge of the savanna's).
     * Sized by area, largest first (measured by sampling the climate noises over many seeds, roughly 31/27/20/12/7/3%):
     * the Australian desert (BSh) on the drier half of the inland hot band, the hot desert (BWh) on the rest of it,
     * the continental desert (BWk) on the coasts and the cooler edge, the Dry steppe (BSk) on the wetter inland
     * side, the Lush desert (BSh) on vanilla's wettest badlands (with a ghost town here and there: GhostTownSites,
     * GhostTownStructure), and the desert mountains (BWk) on the most mountainous ground. The steppe and the
     * Australian desert keep away from every water.
     */
    public record DesertBiomes(Holder<Biome> hot, Holder<Biome> continental, Holder<Biome> mountains,
                               Optional<Holder<Biome>> lush, Optional<Holder<Biome>> steppe, Optional<Holder<Biome>> australian) {
        public static final com.mojang.serialization.Codec<DesertBiomes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("hot").forGetter(DesertBiomes::hot),
                Biome.CODEC.fieldOf("continental").forGetter(DesertBiomes::continental),
                Biome.CODEC.fieldOf("mountains").forGetter(DesertBiomes::mountains),
                Biome.CODEC.optionalFieldOf("lush").forGetter(DesertBiomes::lush),
                Biome.CODEC.optionalFieldOf("steppe").forGetter(DesertBiomes::steppe),
                Biome.CODEC.optionalFieldOf("australian").forGetter(DesertBiomes::australian)
        ).apply(i, DesertBiomes::new));

        /**
         * The deserts' low ground: the Dry steppe (BSk) where it may be (see isSteppeClimate; the caller checks it is
         * clear of the Lush desert's rivers); otherwise the hot desert (BWh) deep inland, and the continental desert
         * (BWk) nearer the sea.
         */
        Holder<Biome> lowland(Climate.TargetPoint climate, boolean steppeAllowed) {
            if (this.steppe.isPresent() && steppeAllowed) return this.steppe.get();
            return climate.continentalness() >= DESERT_INLAND_CONTINENTS && climate.temperature() > HOT_MAX_TEMPERATURE
                    ? this.hot : this.continental;
        }

        Stream<Holder<Biome>> all() {
            return Stream.concat(Stream.of(this.hot, this.continental, this.mountains),
                    Stream.concat(this.lush.stream(), Stream.concat(this.steppe.stream(), this.australian.stream())));
        }
    }

    // below this block y the Csa's ground is all shrubland: its patches of other biomes are only on the surface
    private static final int CSA_PATCH_MIN_Y = 50;

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
            SAVANNA_MIN_CONTINENTALNESS = -0.11;
    private static final long SAVANNA_MIN_TEMPERATURE = 0;
    // The deserts' climate: the hot band beyond the jungle's, and the hottest, driest edge of the savanna's.
    // Unquantized for SavannaCeiling, quantized for the biome choice.
    static final double DESERT_EDGE_TEMP = 0.40, DESERT_EDGE_HUMIDITY = -0.35;
    private static final long DESERT_EDGE_TEMPERATURE = 4000, DESERT_EDGE_HUMIDITY_Q = -3500;
    // inland enough for the hot desert (in the hot band itself; its cooler edge is the continental desert's)
    private static final long DESERT_INLAND_CONTINENTS = -300;
    // the desert mountains: only the most mountainous ground (vanilla's mountains start at erosion -0.375), not in a valley
    private static final long DESERT_MOUNTAIN_EROSION = -4500;
    private static final float DESERT_MOUNTAIN_MIN_PV = -0.2F;
    // The Dry steppe (BSk) and the Australian desert (BSh) never meet any water, so they keep well away from it: far
    // inland (no sea, no beach), deep in the hot band (the jungles, with their rivers, and the savanna, with its
    // ponds, lie on its cooler side), and away from the mountains (their creeks) and the badlands (the Lush desert's
    // rivers). Inside them, the rivers have run dry: HotDesertSurfaceFeature fills them, and the Australian desert's
    // flattened ground (AustralianPlain) buries them.
    // the steppe: the wetter side of the inland hot band (humidity 0.05 and up), off the rugged hills (vanilla's
    // rugged hills are erosion -0.2225 and down)
    private static final long STEPPE_MIN_HUMIDITY = 500, STEPPE_MIN_EROSION = -2225, STEPPE_MIN_CONTINENTS = 1500;
    // both: the whole hot band (which starts at 0.55)
    static final double DRY_MIN_TEMP = 0.55;
    private static final long DRY_MIN_TEMPERATURE = 5500;
    // the Australian desert: the largest of them all - the drier half of the inland hot band (humidity under 0.02),
    // everywhere but its mountains (vanilla's mountains are erosion -0.375 and down; the plain flattens the rest),
    // and inland of the coast (which ends at continentalness 0.03)
    static final double AUSTRALIAN_MIN_EROSION = -0.375, AUSTRALIAN_MIN_CONTINENTS = 0.03, AUSTRALIAN_MAX_HUMIDITY = 0.02;
    private static final long AUSTRALIAN_MIN_EROSION_Q = -3750, AUSTRALIAN_MIN_CONTINENTS_Q = 300, AUSTRALIAN_MAX_HUMIDITY_Q = 200;
    // the Lush desert: only vanilla's wettest badlands (the rest of them are the other deserts)
    private static final long LUSH_MIN_HUMIDITY = 2500;
    // how far (in quarter-chunks) the steppe and the Australian desert keep from the Lush desert
    private static final int BADLANDS_CLEARANCE = 10;

    static boolean isSteppeClimate(Climate.TargetPoint c) {
        return c.humidity() >= STEPPE_MIN_HUMIDITY && c.continentalness() >= STEPPE_MIN_CONTINENTS
                && c.temperature() >= DRY_MIN_TEMPERATURE && c.erosion() >= STEPPE_MIN_EROSION;
    }

    static boolean isAustralianClimate(Climate.TargetPoint c) {
        return c.humidity() < AUSTRALIAN_MAX_HUMIDITY_Q && c.continentalness() >= AUSTRALIAN_MIN_CONTINENTS_Q
                && c.temperature() >= DRY_MIN_TEMPERATURE && c.erosion() >= AUSTRALIAN_MIN_EROSION_Q;
    }

    static boolean isDesertClimate(Climate.TargetPoint c) {
        long t = c.temperature();
        return t > HOT_MAX_TEMPERATURE || t > DESERT_EDGE_TEMPERATURE && c.humidity() < DESERT_EDGE_HUMIDITY_Q;
    }

    private static float peaksAndValleys(long weirdness) {
        float w = Climate.unquantizeCoord(weirdness);
        return -(Math.abs(Math.abs(w) - 0.6666667F) - 0.33333334F) * 3.0F;
    }

    // what keeps its own biome inside the hot desert's climate
    private static final Set<ResourceKey<Biome>> DESERT_KEEPS = Set.of(Biomes.BEACH, Biomes.STONY_SHORE, Biomes.SNOWY_BEACH,
            Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN,
            Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.MUSHROOM_FIELDS);
    // vanilla's badlands: in the deserts' climate, the Lush desert (BSh) takes their wettest part
    private static final Set<ResourceKey<Biome>> BADLANDS = Set.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS);
    // only the most mountainous badlands are desert mountains
    private static final long LUSH_MOUNTAIN_EROSION = -7800;
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
    // the mountain biomes, which in and around the Csa are the Mediterranean highland (Csb) above HIGHLAND_MIN_Y
    private static final Set<ResourceKey<Biome>> CSA_MOUNTAINS = Set.of(Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS,
            Biomes.SNOWY_SLOPES, Biomes.GROVE, Biomes.MEADOW, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
            Biomes.WINDSWEPT_FOREST, Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.ICE_SPIKES);
    public static final int HIGHLAND_MIN_Y = 130;
    private static final Set<ResourceKey<Biome>> CSA_SHORES = Set.of(Biomes.BEACH, Biomes.STONY_SHORE, Biomes.SNOWY_BEACH);
    // Vanilla puts its cave biomes at depth 0.2 and below.
    private static final long UNDERGROUND_MIN_DEPTH = 2000;

    // The Redwood forest (Csb): the cooler side of the Mediterranean's climate (vanilla's temperature band below it),
    // where it's dry to middling. Its forests, plains and taiga become the redwoods.
    private static final long REDWOOD_MIN_TEMPERATURE = -4000, REDWOOD_MAX_HUMIDITY = 1000;
    private static final Set<ResourceKey<Biome>> REDWOOD_LAND = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST,
            Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.MEADOW, Biomes.WINDSWEPT_FOREST);
    // The humid subtropics (Cfa): as warm as the Mediterranean (its temperature band) but with ordinary rainfall
    // (the wetter side of it, humidity CFA_MIN_HUMIDITY and up), and only on the low, flat land along the coast:
    // from the shore to a little inland, never where vanilla's erosion makes hills or mountains. The sea off it is
    // lukewarm. All of its land is the Subtropical plains, but for a strip along the shore, which is the
    // Subtropical swamp. CfaPlain levels the very same ground dead flat, just above the sea.
    // (Unquantized, for CfaPlain, and quantized for the biome choice.)
    static final double CFA_MIN_HUMIDITY = 0.03, CFA_MAX_HUMIDITY = 0.35, CFA_MIN_CONTINENTALNESS = -0.19,
            CFA_MAX_CONTINENTALNESS = 0.55, CFA_MIN_EROSION = 0.05, CFA_SWAMP_MAX_CONTINENTALNESS = -0.06;
    private static final long CFA_MIN_HUMIDITY_Q = 300, CFA_MAX_HUMIDITY_Q = 3500, CFA_MIN_CONTINENTS = -1900,
            CFA_MAX_CONTINENTS = 5500, CFA_MIN_EROSION_Q = 500, CFA_SWAMP_MAX_CONTINENTS = -600;
    private static final Set<ResourceKey<Biome>> ALL_OCEANS = Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.LUKEWARM_OCEAN,
            Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN,
            Biomes.DEEP_FROZEN_OCEAN);
    // what keeps its own biome in the Cfa: the sea (made lukewarm), the rivers and mushroom islands
    private static final Set<ResourceKey<Biome>> CFA_KEEPS = Set.of(Biomes.RIVER, Biomes.FROZEN_RIVER, Biomes.MUSHROOM_FIELDS);

    static boolean isCfaClimate(Climate.TargetPoint c) {
        long t = c.temperature(), h = c.humidity(), k = c.continentalness();
        return t >= CSA_MIN_TEMPERATURE && t < CSA_MAX_TEMPERATURE && h >= CFA_MIN_HUMIDITY_Q && h < CFA_MAX_HUMIDITY_Q
                && k >= CFA_MIN_CONTINENTS && k < CFA_MAX_CONTINENTS && c.erosion() >= CFA_MIN_EROSION_Q;
    }

    // the Cfa's temperature and rainfall widened by a margin, for the seas off its coasts
    static boolean isNearCfaClimate(Climate.TargetPoint c) {
        long t = c.temperature(), h = c.humidity();
        return t >= CSA_MIN_TEMPERATURE - 1000 && t < CSA_MAX_TEMPERATURE + 1000 && h >= CFA_MIN_HUMIDITY_Q - 1000
                && h < CFA_MAX_HUMIDITY_Q + 1000;
    }

    static boolean isRedwoodClimate(Climate.TargetPoint c) {
        return c.temperature() >= REDWOOD_MIN_TEMPERATURE && c.temperature() < CSA_MIN_TEMPERATURE && c.humidity() < REDWOOD_MAX_HUMIDITY;
    }

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
    // the deserts (BWh and BWk), which take the place of every vanilla desert and the whole hot band, when given
    private final Optional<DesertBiomes> deserts;
    // the Mediterranean (Csa) and its white gravel shore
    private final Optional<Holder<Biome>> mediterranean, mediterraneanBeach;
    private final Optional<CsaBiomes> csa;
    private record CsaSalt(Climate.Sampler sampler, long salt) {
    }
    private volatile CsaSalt csaSalt;
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
                             Optional<Holder<Biome>> mediterranean, Optional<Holder<Biome>> mediterraneanBeach,
                             Optional<CsaBiomes> csa, Optional<DesertBiomes> deserts) {
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
        this.csa = csa;
        this.deserts = deserts;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        Stream<Holder<Biome>> stream = Stream.concat(this.base.possibleBiomes().stream(),
                Stream.concat(Stream.of(this.jungle, this.river, this.beach, this.underground, this.warmLand, this.warmSwamp, this.warmOcean),
                        Stream.concat(Stream.concat(Stream.concat(this.islandSummit.stream(), this.savanna.stream()),
                                        Stream.concat(Stream.concat(this.mediterranean.stream(), this.mediterraneanBeach.stream()),
                                                this.csa.stream().flatMap(CsaBiomes::all))),
                                Stream.concat(this.monsoon.stream(), this.riceTerraces.stream()))))
                // vanilla's savannas never generate once the African savanna replaces them
                .filter(b -> this.savanna.isEmpty() || !b.unwrapKey().map(VANILLA_SAVANNAS::contains).orElse(false))
                .filter(b -> this.monsoon.isEmpty() || !b.unwrapKey().map(VANILLA_JUNGLES::contains).orElse(false))
                .filter(b -> this.deserts.isEmpty() || !b.is(Biomes.DESERT))
                .filter(b -> this.deserts.isEmpty() || this.deserts.get().lush().isEmpty() || !b.unwrapKey().map(BADLANDS::contains).orElse(false));
        stream = Stream.concat(stream, this.deserts.stream().flatMap(DesertBiomes::all));
        return stream;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> biome = this.mappedBiome(x, y, z, sampler);
        // any desert left (outside the desert climate) is the continental desert (BWk)
        if (this.deserts.isPresent() && biome.is(Biomes.DESERT)) return this.deserts.get().continental();
        if (this.deserts.isPresent() && this.deserts.get().lush().isPresent()) {
            Holder<Biome> lush = this.deserts.get().lush().get();
            // any badlands left are the Lush desert (BSh)
            if (biome.unwrapKey().map(BADLANDS::contains).orElse(false)) biome = lush;
        }
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

    // ---------------------------------------------------------------- ghost towns

    private record GhostCache(Climate.Sampler sampler, GhostTownSites sites) {
    }
    private volatile GhostCache ghostCache;

    private GhostTownSites ghostTownSites(Climate.Sampler sampler) {
        GhostCache cache = this.ghostCache;
        if (cache == null || cache.sampler() != sampler) {
            double t = sampler.temperature().compute(new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            cache = new GhostCache(sampler, new GhostTownSites(IslandMap.layoutSeed(t) * 53L + 11L));
            this.ghostCache = cache;
        }
        return cache.sites();
    }

    private GhostTownSites.LushSurface lushSurface(Climate.Sampler sampler) {
        return (x, z) -> {
            if (this.deserts.isEmpty() || this.deserts.get().lush().isEmpty()) return false;
            Holder<Biome> b = this.surfaceBiome(x, z, sampler);
            return b == this.deserts.get().lush().get() || b.unwrapKey().map(BADLANDS::contains).orElse(false);
        };
    }

    /** The ghost town of the cell this block lies in, if it has one (used to place the ghost town structure). */
    public java.util.Optional<GhostTownSites.Site> ghostTownSite(int blockX, int blockZ, Climate.Sampler sampler) {
        if (this.deserts.isEmpty() || this.deserts.get().lush().isEmpty()) return java.util.Optional.empty();
        return this.ghostTownSites(sampler).site(Math.floorDiv(blockX, GhostTownSites.CELL), Math.floorDiv(blockZ, GhostTownSites.CELL),
                this.lushSurface(sampler));
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
            boolean land = biome.unwrapKey().map(CSA_LAND::contains).orElse(false);
            boolean mountain = biome.unwrapKey().map(CSA_MOUNTAINS::contains).orElse(false);
            // high up, the Mediterranean highland (Csb)
            Holder<Biome> highland = this.csa.flatMap(CsaBiomes::highland).orElse(null);
            if (highland != null && (land || mountain) && QuartPos.toBlock(y) >= HIGHLAND_MIN_Y) return highland;
            if (land || mountain) return this.csaLand(x, y, z, sampler, mountain, climate);
            if (this.csa.isPresent() && biome.is(Biomes.RIVER)) return this.csa.get().river();
            if (this.mediterraneanBeach.isPresent() && biome.unwrapKey().map(CSA_SHORES::contains).orElse(false)) {
                return this.mediterraneanBeach.get();
            }
        }
        // the mountains just around it are highland too
        if (this.mediterranean.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && isNearMediterraneanClimate(climate)
                && biome.unwrapKey().map(CSA_MOUNTAINS::contains).orElse(false) && QuartPos.toBlock(y) >= HIGHLAND_MIN_Y) {
            Holder<Biome> highland = this.csa.flatMap(CsaBiomes::highland).orElse(null);
            if (highland != null) return highland;
        }
        // the Redwood forest (Csb): on the Mediterranean's cooler side
        Holder<Biome> redwood = this.csa.flatMap(CsaBiomes::redwood).orElse(null);
        if (redwood != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isRedwoodClimate(climate)
                && biome.unwrapKey().map(REDWOOD_LAND::contains).orElse(false)) {
            return redwood;
        }
        // the humid subtropics (Cfa): all their land the Subtropical plains, the strip along the shore their swamp
        // (which takes the beach as well, so it runs right down to the water)
        Holder<Biome> subtropicalPlains = this.csa.flatMap(CsaBiomes::subtropicalPlains).orElse(null);
        if (subtropicalPlains != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isCfaClimate(climate)
                && !biome.unwrapKey().map(k -> ALL_OCEANS.contains(k) || CFA_KEEPS.contains(k)).orElse(false)) {
            Holder<Biome> swamp = this.csa.flatMap(CsaBiomes::subtropicalSwamp).orElse(null);
            return swamp != null && climate.continentalness() < CFA_SWAMP_MAX_CONTINENTS ? swamp : subtropicalPlains;
        }
        // the sea off the humid subtropics is lukewarm
        if (subtropicalPlains != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isNearCfaClimate(climate)
                && biome.unwrapKey().map(CSA_SEAS::contains).orElse(false)) {
            ResourceKey<Biome> lukewarm = biome.is(Biomes.DEEP_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                    ? Biomes.DEEP_LUKEWARM_OCEAN : Biomes.LUKEWARM_OCEAN;
            Holder<Biome> sea = this.base.possibleBiomes().stream().filter(b -> b.is(lukewarm)).findFirst().orElse(null);
            if (sea != null) return sea;
        }
        // the sea off the Mediterranean is lukewarm, a little beyond its coast too, so its beaches only ever meet lukewarm water
        if (this.mediterranean.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && isNearMediterraneanClimate(climate)) {
            ResourceKey<Biome> lukewarm = biome.is(Biomes.DEEP_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                    ? Biomes.DEEP_LUKEWARM_OCEAN : Biomes.LUKEWARM_OCEAN;
            if (biome.unwrapKey().map(CSA_SEAS::contains).orElse(false)) {
                Holder<Biome> sea = this.base.possibleBiomes().stream().filter(b -> b.is(lukewarm)).findFirst().orElse(null);
                if (sea != null) return sea;
            }
        }
        // the deserts take the whole hot band beyond the jungle's - every bit of land in it (deserts, badlands,
        // savannas, hills); only the sea, its shores and mushroom islands keep their own biomes
        if (this.deserts.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && isDesertClimate(climate)
                && !biome.unwrapKey().map(DESERT_KEEPS::contains).orElse(false)) {
            DesertBiomes d = this.deserts.get();
            boolean mountains = climate.erosion() < DESERT_MOUNTAIN_EROSION
                    && peaksAndValleys(climate.weirdness()) > DESERT_MOUNTAIN_MIN_PV;
            // the Lush desert (BSh): vanilla's wettest badlands, but for their most mountainous parts; its rivers keep some water
            if (d.lush().isPresent() && climate.humidity() >= LUSH_MIN_HUMIDITY) {
                if (biome.unwrapKey().map(BADLANDS::contains).orElse(false)
                        && !(mountains && climate.erosion() < LUSH_MOUNTAIN_EROSION)) {
                    return d.lush().get();
                }
                if (biome.is(Biomes.RIVER) && !mountains && this.byBadlands(x, y, z, sampler)) return d.lush().get();
            }
            // the Australian desert (BSh): flat, dry and far from any water - its rivers included, buried under its plain
            if (d.australian().isPresent() && isAustralianClimate(climate) && !this.nearLush(x, y, z, sampler)) {
                return d.australian().get();
            }
            boolean steppe = d.steppe().isPresent() && isSteppeClimate(climate) && !this.nearLush(x, y, z, sampler);
            if (biome.is(Biomes.RIVER)) {
                // creeks run on through the mountains and the continental desert; in the hot desert and the steppe
                // the rivers have run dry (HotDesertSurfaceFeature fills their beds with sand), so they never meet water
                if (mountains || d.lowland(climate, false) != d.hot()) return biome;
                return d.lowland(climate, steppe);
            }
            if (mountains) return d.mountains();
            // the hot desert lies deep inland, nearer the sea it's the continental desert; where wetter, the Dry steppe
            return d.lowland(climate, steppe);
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

    private static final Set<ResourceKey<Biome>> CSA_SEAS = Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.COLD_OCEAN,
            Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN, Biomes.DEEP_FROZEN_OCEAN, Biomes.WARM_OCEAN);

    // the Mediterranean climate widened by a margin, for the seas off its coasts
    static boolean isNearMediterraneanClimate(Climate.TargetPoint c) {
        long t = c.temperature(), h = c.humidity();
        return t >= CSA_MIN_TEMPERATURE - 1000 && t < CSA_MAX_TEMPERATURE + 1000 && h < CSA_MAX_HUMIDITY_Q + 1000;
    }

    /** Which of the Mediterranean's land biomes this spot falls in (see CsaZones). */
    // whether vanilla's badlands lie just beside this spot (12 blocks or so)
    /** Whether vanilla would have badlands (the Lush desert, with its rivers) anywhere within reach of this spot. */
    // the Lush desert's ground: vanilla's badlands, where wet enough
    private boolean lushAt(int x, int y, int z, Climate.Sampler sampler) {
        return this.base.getNoiseBiome(x, y, z, sampler).unwrapKey().map(BADLANDS::contains).orElse(false)
                && sampler.sample(x, y, z).humidity() >= LUSH_MIN_HUMIDITY;
    }

    private boolean nearLush(int x, int y, int z, Climate.Sampler sampler) {
        int r = BADLANDS_CLEARANCE, h = BADLANDS_CLEARANCE / 2;
        for (int[] o : new int[][]{{r, 0}, {-r, 0}, {0, r}, {0, -r}, {h, h}, {h, -h}, {-h, h}, {-h, -h}}) {
            if (this.lushAt(x + o[0], y, z + o[1], sampler)) return true;
        }
        return this.lushAt(x, y, z, sampler);
    }

    private boolean byBadlands(int x, int y, int z, Climate.Sampler sampler) {
        for (int[] o : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
            if (this.base.getNoiseBiome(x + o[0], y, z + o[1], sampler).unwrapKey().map(BADLANDS::contains).orElse(false)) return true;
        }
        return false;
    }

    private Holder<Biome> csaLand(int x, int y, int z, Climate.Sampler sampler, boolean mountain, Climate.TargetPoint climate) {
        if (this.csa.isEmpty()) return this.mediterranean.get();
        CsaSalt cached = this.csaSalt;
        if (cached == null || cached.sampler() != sampler) {
            cached = new CsaSalt(sampler, CsaZones.salt(sampler.temperature()));
            this.csaSalt = cached;
        }
        CsaBiomes b = this.csa.get();
        int bx = QuartPos.toBlock(x) + 2, by = QuartPos.toBlock(y), bz = QuartPos.toBlock(z) + 2;
        if (by < CSA_PATCH_MIN_Y) return this.mediterranean.get();
        // the farmland, olive groves and lavender fields lie on level ground (CsaHills levels it): never on the
        // mountains, which keep their shape and are shrubland or woods
        boolean rugged = mountain || Climate.unquantizeCoord(climate.erosion()) < CsaHills.FLAT_EROSION_FULL;
        return switch (CsaZones.at(bx, bz, cached.salt(), sampler.weirdness()).zone()) {
            case FOREST -> b.forest();
            case OLIVE_GROVE -> rugged ? this.mediterranean.get() : b.oliveGrove();
            case FARMLAND -> rugged ? this.mediterranean.get() : b.farmland();
            case LAVENDER_FIELD -> rugged ? this.mediterranean.get() : b.lavenderField();
            case SHRUBLAND -> this.mediterranean.get();
        };
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
                && c.temperature() <= HOT_MAX_TEMPERATURE
                && !(c.temperature() > DESERT_EDGE_TEMPERATURE && c.humidity() < DESERT_EDGE_HUMIDITY_Q);
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
