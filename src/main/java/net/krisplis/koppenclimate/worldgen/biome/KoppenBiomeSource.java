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
                            Optional<Holder<Biome>> subtropicalPlains, Optional<Holder<Biome>> subtropicalSwamp,
                            Optional<Holder<Biome>> americanFarmland, Optional<Holder<Biome>> monsoonSubtropical,
                            Optional<Holder<Biome>> subtropicalRiceTerraces, Optional<Holder<Biome>> subtropicalKarstPeaks,
                            Optional<Holder<Biome>> cherryOrchard, Optional<Holder<Biome>> prairie,
                            Optional<CfbBiomes> temperate) {
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
                Biome.CODEC.optionalFieldOf("subtropical_swamp").forGetter(CsaBiomes::subtropicalSwamp),
                Biome.CODEC.optionalFieldOf("american_farmland").forGetter(CsaBiomes::americanFarmland),
                // the monsoon subtropics (Cwa), southern China's climate, and their great rice terraces
                Biome.CODEC.optionalFieldOf("monsoon_subtropical").forGetter(CsaBiomes::monsoonSubtropical),
                Biome.CODEC.optionalFieldOf("subtropical_rice_terraces").forGetter(CsaBiomes::subtropicalRiceTerraces),
                Biome.CODEC.optionalFieldOf("subtropical_karst_peaks").forGetter(CsaBiomes::subtropicalKarstPeaks),
                Biome.CODEC.optionalFieldOf("cherry_orchard").forGetter(CsaBiomes::cherryOrchard),
                // the hot-summer continental climate (Dfa): the Prairie
                Biome.CODEC.optionalFieldOf("prairie").forGetter(CsaBiomes::prairie),
                // the temperate oceanic band (Cfb): kept here, as the biome source's own fields are full (16 at most)
                CfbBiomes.CODEC.optionalFieldOf("temperate_zones").forGetter(CsaBiomes::temperate)
        ).apply(i, CsaBiomes::new));

        Stream<Holder<Biome>> all() {
            return Stream.of(Stream.of(this.forest, this.oliveGrove, this.river, this.farmland, this.lavenderField),
                    this.highland.stream(), this.redwood.stream(), this.subtropicalPlains.stream(), this.subtropicalSwamp.stream(),
                    this.americanFarmland.stream(), this.monsoonSubtropical.stream(), this.subtropicalRiceTerraces.stream(),
                    this.subtropicalKarstPeaks.stream(), this.cherryOrchard.stream(), this.prairie.stream(),
                    this.temperate.stream().flatMap(CfbBiomes::all))
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
                               Optional<Holder<Biome>> lush, Optional<Holder<Biome>> steppe, Optional<Holder<Biome>> australian,
                               Optional<Holder<Biome>> nile,
                               // Pompeii (Csa), the ever-burning Roman town on the Mediterranean's level land (PompeiiSites):
                               // kept here, as the Mediterranean's own fields are full (16 at most)
                               Optional<Holder<Biome>> pompeii,
                               // the savanna's own rivers (As): the savanna's water, crocodiles (kept here for the same reason)
                               Optional<Holder<Biome>> savannaRiver) {
        public static final com.mojang.serialization.Codec<DesertBiomes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("hot").forGetter(DesertBiomes::hot),
                Biome.CODEC.fieldOf("continental").forGetter(DesertBiomes::continental),
                Biome.CODEC.fieldOf("mountains").forGetter(DesertBiomes::mountains),
                Biome.CODEC.optionalFieldOf("lush").forGetter(DesertBiomes::lush),
                Biome.CODEC.optionalFieldOf("steppe").forGetter(DesertBiomes::steppe),
                Biome.CODEC.optionalFieldOf("australian").forGetter(DesertBiomes::australian),
                Biome.CODEC.optionalFieldOf("nile").forGetter(DesertBiomes::nile),
                Biome.CODEC.optionalFieldOf("pompeii").forGetter(DesertBiomes::pompeii),
                Biome.CODEC.optionalFieldOf("savanna_river").forGetter(DesertBiomes::savannaRiver)
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
                    Stream.concat(this.lush.stream(), Stream.concat(this.steppe.stream(),
                            Stream.concat(this.australian.stream(), Stream.concat(this.nile.stream(),
                                    Stream.concat(this.pompeii.stream(), this.savannaRiver.stream()))))));
        }
    }

    /**
     * The temperate oceanic band (Cfb): the cool, wet side of the temperate band, between the Prairie (drier) and the
     * humid subtropics (warmer). Its coast is the White cliffs, the land behind them the English countryside (both on
     * CfbTerrain's flat upland), further inland the Temperate forest, with Sunflower fields on its flattest ground, and
     * its rivers are the broad, brown Flooded rivers. The sea off it is plain ocean.
     */
    public record CfbBiomes(Holder<Biome> forest, Holder<Biome> sunflowerField, Holder<Biome> countryside,
                            Holder<Biome> whiteCliffs, Holder<Biome> floodedRiver,
                            // the subpolar oceanic band (Cfc) beyond it, and the humid continental (Dfb) inland
                            Optional<Holder<Biome>> highland, Optional<Holder<Biome>> scottishCliffs,
                            Optional<Holder<Biome>> continentalForest, Optional<Holder<Biome>> pineForest,
                            // the Alps (AlpineTerrain): the valleys, and the mountains' two altitude bands
                            Optional<Holder<Biome>> alpineValley, Optional<Holder<Biome>> alpineMid, Optional<Holder<Biome>> alpineHigh,
                            // and the Continental mountains (Dfc) ringing the Alps round
                            Optional<Holder<Biome>> continentalMountains,
                            // the Continental clearings (Dfb) in the Continental forest, and the Cold lakeland (Dfc)
                            // on the Dfb's cold side
                            Optional<Holder<Biome>> continentalClearing, Optional<Holder<Biome>> coldLakeland,
                            // the far north: the Tundra (ET) and the Ice cap (EF)
                            Optional<PolarBiomes> polar) {
        public static final com.mojang.serialization.Codec<CfbBiomes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("forest").forGetter(CfbBiomes::forest),
                Biome.CODEC.fieldOf("sunflower_field").forGetter(CfbBiomes::sunflowerField),
                Biome.CODEC.fieldOf("countryside").forGetter(CfbBiomes::countryside),
                Biome.CODEC.fieldOf("white_cliffs").forGetter(CfbBiomes::whiteCliffs),
                Biome.CODEC.fieldOf("flooded_river").forGetter(CfbBiomes::floodedRiver),
                Biome.CODEC.optionalFieldOf("scottish_highland").forGetter(CfbBiomes::highland),
                Biome.CODEC.optionalFieldOf("scottish_cliffs").forGetter(CfbBiomes::scottishCliffs),
                Biome.CODEC.optionalFieldOf("continental_forest").forGetter(CfbBiomes::continentalForest),
                Biome.CODEC.optionalFieldOf("pine_forest").forGetter(CfbBiomes::pineForest),
                Biome.CODEC.optionalFieldOf("alpine_valley").forGetter(CfbBiomes::alpineValley),
                Biome.CODEC.optionalFieldOf("alpine_mid_altitude").forGetter(CfbBiomes::alpineMid),
                Biome.CODEC.optionalFieldOf("alpine_high_altitude").forGetter(CfbBiomes::alpineHigh),
                Biome.CODEC.optionalFieldOf("continental_mountains").forGetter(CfbBiomes::continentalMountains),
                Biome.CODEC.optionalFieldOf("continental_clearing").forGetter(CfbBiomes::continentalClearing),
                Biome.CODEC.optionalFieldOf("cold_lakeland").forGetter(CfbBiomes::coldLakeland),
                PolarBiomes.CODEC.optionalFieldOf("polar_zones").forGetter(CfbBiomes::polar)
        ).apply(i, CfbBiomes::new));

        Stream<Holder<Biome>> all() {
            return Stream.of(Stream.of(this.forest, this.sunflowerField, this.countryside, this.whiteCliffs, this.floodedRiver),
                    this.highland.stream(), this.scottishCliffs.stream(), this.continentalForest.stream(),
                    this.pineForest.stream(), this.alpineValley.stream(), this.alpineMid.stream(),
                    this.alpineHigh.stream(), this.continentalMountains.stream(), this.continentalClearing.stream(),
                    this.coldLakeland.stream(), this.polar.stream().flatMap(PolarBiomes::all)).flatMap(x -> x);
        }
    }

    /**
     * The far north. Coldest of all (temperature below -0.78) the Tundra (ET), right down to the sea - whose
     * coldest stretches beyond it (below -0.8) are the Ice cap (EF), AlpineTerrain's flat sheet of ice; the rest of
     * the sea up there is vanilla's frozen ocean (EF). Between the Tundra and the temperate bands (-0.78 to -0.62)
     * the Subpolar taiga (Dfc), with its fjords on the coast - but for the
     * wet coasts there, the Scottish highland's (Cfc).
     */
    public record PolarBiomes(Holder<Biome> tundra, Holder<Biome> iceCap, Optional<Holder<Biome>> subpolarTaiga) {
        public static final com.mojang.serialization.Codec<PolarBiomes> CODEC = RecordCodecBuilder.create(i -> i.group(
                Biome.CODEC.fieldOf("tundra").forGetter(PolarBiomes::tundra),
                Biome.CODEC.fieldOf("ice_cap").forGetter(PolarBiomes::iceCap),
                Biome.CODEC.optionalFieldOf("subpolar_taiga").forGetter(PolarBiomes::subpolarTaiga)
        ).apply(i, PolarBiomes::new));

        Stream<Holder<Biome>> all() {
            return Stream.concat(Stream.of(this.tundra, this.iceCap), this.subpolarTaiga.stream());
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
    // ... and only on the ground itself: never in the open air high over lower land (depth below -0.12 is more than
    // about 15 blocks above vanilla's ground), where the sky, fog and sounds would otherwise turn to the highland's
    private static final long HIGHLAND_MIN_DEPTH = -1200;
    private static final Set<ResourceKey<Biome>> CSA_SHORES = Set.of(Biomes.BEACH, Biomes.STONY_SHORE, Biomes.SNOWY_BEACH);
    // Vanilla puts its cave biomes at depth 0.2 and below.
    private static final long UNDERGROUND_MIN_DEPTH = 2000;

    // The Redwood forest (Csb): the cooler side of the Mediterranean's climate (vanilla's temperature band below it),
    // where it's dry to middling. Its forests, plains and taiga become the redwoods.
    // Extremely rare: only a narrow, dry band on the Mediterranean's cool side (in all about one part in two
    // hundred of the world, in stands a few hundred blocks across, before REDWOOD_LAND narrows it further)
    private static final long REDWOOD_MIN_TEMPERATURE = -2200, REDWOOD_MAX_TEMPERATURE = -1600, REDWOOD_MAX_HUMIDITY = -2500;
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
    // The American farmland: one of the vastest biomes of all, and an inland one. It takes the inland part of the Cfa
    // (continentalness 0.2 and up, well back from the sea; the Subtropical grove lies between it and the swamp along
    // the shore), and reaches on beyond it,
    // inland without limit, onto the gentler hills (erosion FARMLAND_MIN_EROSION and up, still far from vanilla's
    // mountains at -0.375) and into the wetter and cooler land next to it (vanilla's forests: humidity
    // FARMLAND_MIN_HUMIDITY and up). It never comes near the taiga: it keeps to the warm side of FARMLAND_MIN_TEMP,
    // a little above the Cfa's own coolest edge (-0.15, below which vanilla's taiga lies), so a band of the
    // Subtropical grove or of vanilla's broad-leaved forests always lies between. CfaPlain levels all of it, and fills in
    // the river valleys across it too (CfaPlain.farmlandFill), so no river cuts it up: the farmland takes their beds.
    // (Unquantized, for CfaPlain, and quantized for the biome choice.)
    static final double FARMLAND_MIN_CONTINENTALNESS = 0.2, FARMLAND_MIN_EROSION = -0.15, FARMLAND_MIN_HUMIDITY = 0.1,
            FARMLAND_MIN_TEMP = -0.1, FARMLAND_MAX_TEMP = 0.05;
    private static final long CFA_FARMLAND_MIN_CONTINENTS = 2000, FARMLAND_MIN_EROSION_Q = -1500,
            FARMLAND_MIN_HUMIDITY_Q = 1000, FARMLAND_MIN_TEMPERATURE = -1000,
            FARMLAND_MAX_TEMPERATURE = 500;
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

    /** The American farmland's climate: the Cfa's inland part, and the land beyond it (see FARMLAND_MIN_*). */
    static boolean isFarmlandClimate(Climate.TargetPoint c) {
        if (c.continentalness() < CFA_FARMLAND_MIN_CONTINENTS) return false;
        long t = c.temperature();
        // nor near the tropics: it keeps well short of the warm fringe (0.1), where the monsoon forests and jungles begin
        if (t < FARMLAND_MIN_TEMPERATURE || t >= FARMLAND_MAX_TEMPERATURE) return false;
        if (isCfaClimate(c)) return true;
        return t < CSA_MAX_TEMPERATURE && c.humidity() >= FARMLAND_MIN_HUMIDITY_Q
                && c.erosion() >= FARMLAND_MIN_EROSION_Q;
    }

    // The monsoon subtropics (Cwa): southern China's climate. The warm, wet strip between the Cfa's American farmland
    // (which ends at 0.05) and the tropics' fringe, reaching a little into the fringe too (to 0.15), wherever it's
    // humid (0.1 and up), on land. About one part in thirty of the land. It takes all of vanilla's ordinary land
    // there, its mountains included (CwaTerrain lays all of it out anew); the sea, the rivers and the beaches keep
    // their own biomes. Where the erosion is high (CWA_KARST_MIN_EROSION and up: about half of it) it is the
    // Subtropical karst peaks, on a broad plain; elsewhere the Monsoon subtropical hills, but where those are wettest
    // (humidity CWA_ORCHARD_MIN_HUMIDITY and up: about a quarter of the monsoon subtropics) the Cherry orchards, on
    // gentler ground.
    // (Unquantized, for CwaTerrain and CfaPlain, and quantized for the biome choice.)
    static final double CWA_MIN_TEMP = 0.05, CWA_MAX_TEMP = 0.15, CWA_MIN_HUMIDITY = 0.1, CWA_MIN_CONTINENTALNESS = -0.11,
            CWA_KARST_MIN_EROSION = 0.0, CWA_ORCHARD_MIN_HUMIDITY = 0.25;
    private static final long CWA_MIN_TEMPERATURE = 500, CWA_MAX_TEMPERATURE = 1500, CWA_MIN_HUMIDITY_Q = 1000,
            CWA_MIN_CONTINENTALNESS_Q = -1100, CWA_KARST_MIN_EROSION_Q = 0, CWA_ORCHARD_MIN_HUMIDITY_Q = 2500;
    private static final Set<ResourceKey<Biome>> CWA_LAND = Set.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST,
            Biomes.FLOWER_FOREST, Biomes.BIRCH_FOREST, Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.DARK_FOREST, Biomes.MEADOW,
            Biomes.CHERRY_GROVE, Biomes.SWAMP, Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_GRAVELLY_HILLS,
            Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SAVANNA, Biomes.TAIGA, Biomes.GROVE,
            Biomes.SNOWY_SLOPES, Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS);

    static boolean isCwaClimate(Climate.TargetPoint c) {
        long t = c.temperature();
        return t >= CWA_MIN_TEMPERATURE && t < CWA_MAX_TEMPERATURE && c.humidity() >= CWA_MIN_HUMIDITY_Q
                && c.continentalness() >= CWA_MIN_CONTINENTALNESS_Q;
    }

    // the Cfa's temperature and rainfall widened by a margin, for the seas off its coasts
    static boolean isNearCfaClimate(Climate.TargetPoint c) {
        long t = c.temperature(), h = c.humidity();
        return t >= CSA_MIN_TEMPERATURE - 1000 && t < CSA_MAX_TEMPERATURE + 1000 && h >= CFA_MIN_HUMIDITY_Q - 1000
                && h < CFA_MAX_HUMIDITY_Q + 1000;
    }

    static boolean isRedwoodClimate(Climate.TargetPoint c) {
        return c.temperature() >= REDWOOD_MIN_TEMPERATURE && c.temperature() < REDWOOD_MAX_TEMPERATURE && c.humidity() < REDWOOD_MAX_HUMIDITY;
    }

    // The Prairie (Dfa): the hot-summer continental climate of the North American plains. It takes the cool temperate
    // band on its drier side, from where vanilla's plains begin down to the edge of the snowy lands (temperature -0.6
    // to -0.25, humidity under -0.1), well inland only (continentalness 0.15 and up: never near the sea). Sampled over whole
    // worlds that is about 2-3% of the world (a third of what it was), in stretches 2-3 km across. Its warm edge stays 0.1 short of the Mediterranean's
    // (-0.15), so the two never meet: there's always a hundred blocks and more of other land between them. Every biome
    // there but the mountains' (and, should any reach in, the sea's and its shores') is the Prairie, its rivers, ponds
    // and swamps included - PrairiePlain fills every one of them in, so no water is left on it.
    // (Unquantized, for PrairiePlain, and quantized for the biome choice.) Erosion doesn't limit it: on rugged ground
    // vanilla's hills stand up out of the grass, and its peaks keep their own biomes.
    // (Made rarer, not smaller: only the driest part of that humidity range, and only well inland.)
    static final double PRAIRIE_MIN_TEMP = -0.6, PRAIRIE_MAX_TEMP = -0.25, PRAIRIE_MAX_HUMIDITY = -0.1,
            PRAIRIE_MIN_CONTINENTALNESS = 0.15, PRAIRIE_MIN_EROSION = -2.0;
    private static final long PRAIRIE_MIN_TEMPERATURE = -6000, PRAIRIE_MAX_TEMPERATURE = -2500, PRAIRIE_MAX_HUMIDITY_Q = -1000,
            PRAIRIE_MIN_CONTINENTS = 1500, PRAIRIE_MIN_EROSION_Q = -20000;
    // what keeps its own biome in the Prairie's climate: the mountains, and the sea and its shores
    private static final Set<ResourceKey<Biome>> PRAIRIE_KEEPS = Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.LUKEWARM_OCEAN,
            Biomes.DEEP_LUKEWARM_OCEAN, Biomes.WARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN,
            Biomes.DEEP_FROZEN_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE, Biomes.MUSHROOM_FIELDS,
            Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES, Biomes.GROVE);

    static boolean isPrairieClimate(Climate.TargetPoint c) {
        long t = c.temperature();
        return t >= PRAIRIE_MIN_TEMPERATURE && t < PRAIRIE_MAX_TEMPERATURE && c.humidity() < PRAIRIE_MAX_HUMIDITY_Q
                && c.continentalness() >= PRAIRIE_MIN_CONTINENTS && c.erosion() >= PRAIRIE_MIN_EROSION_Q;
    }

    // The temperate oceanic band (Cfb): the cool, wet side of the temperate band - temperature -0.6 to -0.15,
    // humidity 0.1 and up (where the drier Prairie doesn't take it first) - except, colder than -0.25, far inland
    // (continentalness 0.25 and up), which is the humid continental (Dfb). Its ground is vanilla's own (save that its
    // lowland rivers are broadened, CfbRiver). Behind the coast (continentalness under 0.15) lies the English
    // countryside, further inland the Temperate forest; small Sunflower fields lie here and there on the flattest
    // inland ground; vanilla's rocky shores are the White cliffs, its rivers the Flooded rivers.
    // The subpolar oceanic (Cfc) lies beyond it towards the cold: temperature -0.9 to -0.6, humidity -0.1 and up,
    // not far inland: the Scottish highland, with its rocky shores the Scottish cliffs.
    // The humid continental (Dfb) is the cool interior: temperature -0.9 to -0.25, humidity 0.1 and up,
    // continentalness 0.25 and up: the Continental forest, with small Pine forest stands on the flat here and there. (Quantized.)
    private static final long CFB_MIN_TEMPERATURE = -6000, CFB_MAX_TEMPERATURE = -1500, CFB_MIN_HUMIDITY_Q = 1000,
            CFB_COUNTRYSIDE_MAX_CONTINENTS = 1500, CFB_SUNFLOWER_MIN_EROSION = 6500, CFB_SUNFLOWER_MIN_CONTINENTS = 500,
            CFB_SUNFLOWER_MAX_WEIRDNESS = 3000;
    private static final long CFC_MIN_TEMPERATURE = -9000, CFC_MIN_HUMIDITY = -1000;
    private static final long DFB_MIN_TEMPERATURE = -9000, DFB_MAX_TEMPERATURE = -2500, DFB_MIN_CONTINENTS = 2500,
            DFB_MIN_HUMIDITY = 1000;
    // what keeps its own biome in the Cfb: the mountains and the mushroom islands (the seas and shores are seen to apart)
    private static final Set<ResourceKey<Biome>> CFB_KEEPS = Set.of(Biomes.MUSHROOM_FIELDS, Biomes.JAGGED_PEAKS,
            Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES, Biomes.GROVE);
    private static final Set<ResourceKey<Biome>> DEEP_OCEANS = Set.of(Biomes.DEEP_OCEAN, Biomes.DEEP_COLD_OCEAN,
            Biomes.DEEP_FROZEN_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN);

    static boolean isDfbClimate(Climate.TargetPoint c) {
        long t = c.temperature();
        return t >= DFB_MIN_TEMPERATURE && t < DFB_MAX_TEMPERATURE && c.humidity() >= DFB_MIN_HUMIDITY
                && c.continentalness() >= DFB_MIN_CONTINENTS;
    }

    static boolean isCfcClimate(Climate.TargetPoint c) {
        long t = c.temperature();
        return t >= CFC_MIN_TEMPERATURE && t < CFB_MIN_TEMPERATURE && c.humidity() >= CFC_MIN_HUMIDITY
                && c.continentalness() < DFB_MIN_CONTINENTS;
    }

    static boolean isCfbClimate(Climate.TargetPoint c) {
        long t = c.temperature();
        return t >= CFB_MIN_TEMPERATURE && t < CFB_MAX_TEMPERATURE && c.humidity() >= CFB_MIN_HUMIDITY_Q && !isDfbClimate(c);
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
    // the temperate oceanic band (Cfb)
    private final Optional<CfbBiomes> cfb;
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
        this.cfb = csa.flatMap(CsaBiomes::temperate);
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
                .filter(b -> this.deserts.isEmpty() || this.deserts.get().lush().isEmpty() || !b.unwrapKey().map(BADLANDS::contains).orElse(false))
                // nor does any of vanilla's land (see mappedBiome)
                .filter(b -> this.cfb.isEmpty() || !this.isVanillaLand(b));
        stream = Stream.concat(stream, this.deserts.stream().flatMap(DesertBiomes::all));
        return stream;
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> biome = this.mappedBiome(x, y, z, sampler);
        // the lush caves only deep down; above that the caves are the ground's own biome's
        if (y > LUSH_CAVES_MAX_QY && biome.is(Biomes.LUSH_CAVES)) {
            Holder<Biome> above = this.mappedBiome(x, QuartPos.fromBlock(SURFACE_Y), z, sampler);
            if (!above.is(Biomes.LUSH_CAVES)) biome = above;
        }
        // any desert left (outside the desert climate) is the continental desert (BWk)
        if (this.deserts.isPresent() && biome.is(Biomes.DESERT)) return this.deserts.get().continental();
        if (this.deserts.isPresent() && this.deserts.get().lush().isPresent()) {
            Holder<Biome> lush = this.deserts.get().lush().get();
            // any badlands left are the Lush desert (BSh)
            if (biome.unwrapKey().map(BADLANDS::contains).orElse(false)) biome = lush;
        }
        // Pompeii (Csa): its round patch of burnt land, wherever it is the Mediterranean's land
        Holder<Biome> pompeii = this.deserts.flatMap(DesertBiomes::pompeii).orElse(null);
        if (pompeii != null && y >= POMPEII_MIN_QY && this.isCsaLand(biome)
                && this.columnFlag(IN_POMPEII, x, z, sampler, () -> this.pompeiiSites(sampler)
                        .inPompeii(QuartPos.toBlock(x) + 2, QuartPos.toBlock(z) + 2, this.csaSurface(sampler)))) {
            return pompeii;
        }
        if (this.riceTerraces.isPresent() && this.monsoon.isPresent() && biome == this.monsoon.get()
                && this.columnFlag(IN_TERRACES_AW, x, z, sampler, () -> this.terraceSites(sampler)
                        .inTerraces(QuartPos.toBlock(x) + 2, QuartPos.toBlock(z) + 2, this.surfaceBiomes(sampler)))) {
            return this.riceTerraces.get();
        }
        // the Subtropical rice terraces (Cwa): great patches deep inside the monsoon subtropics
        Holder<Biome> cwa = this.csa.flatMap(CsaBiomes::monsoonSubtropical).orElse(null);
        Holder<Biome> cwaTerraces = this.csa.flatMap(CsaBiomes::subtropicalRiceTerraces).orElse(null);
        if (cwa != null && cwaTerraces != null && this.isCwaBiome(biome)
                && this.columnFlag(IN_TERRACES_CWA, x, z, sampler, () -> this.cwaTerraceSites(sampler)
                        .inTerraces(QuartPos.toBlock(x) + 2, QuartPos.toBlock(z) + 2, this.cwaSurface(sampler)))) {
            return cwaTerraces;
        }
        return biome;
    }

    // ---------------------------------------------------------------- the Nile

    // the Nile only on land
    private static final long NILE_MIN_CONTINENTS = -1100;

    private record NileCache(Climate.Sampler sampler, NileMap map) {
    }
    private volatile NileCache nileCache;

    private NileMap nileMap(Climate.Sampler sampler) {
        NileCache cache = this.nileCache;
        if (cache == null || cache.sampler() != sampler) {
            double t = sampler.temperature().compute(new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            cache = new NileCache(sampler, NileMap.forSeed(IslandMap.layoutSeed(t)));
            this.nileCache = cache;
        }
        return cache.map();
    }

    // ---------------------------------------------------------------- rice terraces and Thai villages

    // The sites already found, kept per world (by layout seed) for good. Each chunk's biomes are worked out with a
    // climate sampler of its own, so keeping them per sampler (as before) meant searching for every site afresh,
    // hundreds of climate samples at a time, chunk after chunk.
    private static final java.util.Map<Long, RiceTerraceSites> RICE_SITES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Long, CwaTerraceSites> CWA_SITES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<Long, GhostTownSites> GHOST_SITES = new java.util.concurrent.ConcurrentHashMap<>();

    private record TerraceCache(Climate.Sampler sampler, RiceTerraceSites sites) {
    }
    private volatile TerraceCache terraceCache;
    // the surface, for working out where rivers and monsoon forest are
    private static final int SURFACE_Y = 64;
    private static final int LUSH_CAVES_MAX_QY = QuartPos.fromBlock(-24);

    private RiceTerraceSites terraceSites(Climate.Sampler sampler) {
        TerraceCache cache = this.terraceCache;
        if (cache == null || cache.sampler() != sampler) {
            double t = sampler.temperature().compute(new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            cache = new TerraceCache(sampler, RICE_SITES.computeIfAbsent(IslandMap.layoutSeed(t) * 31L + 7L, RiceTerraceSites::new));
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

    // ---------------------------------------------------------------- the Subtropical rice terraces (Cwa)

    private record CwaCache(Climate.Sampler sampler, CwaTerraceSites sites) {
    }
    private volatile CwaCache cwaCache;

    private CwaTerraceSites cwaTerraceSites(Climate.Sampler sampler) {
        CwaCache cache = this.cwaCache;
        if (cache == null || cache.sampler() != sampler) {
            double t = sampler.temperature().compute(new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            cache = new CwaCache(sampler, CWA_SITES.computeIfAbsent(IslandMap.layoutSeed(t) * 71L + 13L, CwaTerraceSites::new));
            this.cwaCache = cache;
        }
        return cache.sites();
    }

    private CwaTerraceSites.CwaSurface cwaSurface(Climate.Sampler sampler) {
        return (x, z) -> {
            Holder<Biome> cwa = this.csa.flatMap(CsaBiomes::monsoonSubtropical).orElse(null);
            return cwa != null && this.isCwaBiome(this.surfaceBiome(x, z, sampler));
        };
    }

    /** The Monsoon subtropical hills or the Subtropical karst peaks. */
    private boolean isCwaBiome(Holder<Biome> biome) {
        return this.csa.flatMap(CsaBiomes::monsoonSubtropical).map(b -> b == biome).orElse(false)
                || this.csa.flatMap(CsaBiomes::subtropicalKarstPeaks).map(b -> b == biome).orElse(false)
                || this.csa.flatMap(CsaBiomes::cherryOrchard).map(b -> b == biome).orElse(false);
    }

    // ---------------------------------------------------------------- ghost towns

    private record GhostCache(Climate.Sampler sampler, GhostTownSites sites) {
    }
    private volatile GhostCache ghostCache;

    private GhostTownSites ghostTownSites(Climate.Sampler sampler) {
        GhostCache cache = this.ghostCache;
        if (cache == null || cache.sampler() != sampler) {
            double t = sampler.temperature().compute(new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            cache = new GhostCache(sampler, GHOST_SITES.computeIfAbsent(IslandMap.layoutSeed(t) * 53L + 11L, GhostTownSites::new));
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

    // ---------------------------------------------------------------- Pompeii

    // Pompeii's burnt land reaches down to here (below it, the caves are the Mediterranean's as before)
    private static final int POMPEII_MIN_QY = QuartPos.fromBlock(40);
    private static final java.util.Map<Long, PompeiiSites> POMPEII_SITES = new java.util.concurrent.ConcurrentHashMap<>();

    private record PompeiiCache(Climate.Sampler sampler, PompeiiSites sites) {
    }
    private volatile PompeiiCache pompeiiCache;

    private PompeiiSites pompeiiSites(Climate.Sampler sampler) {
        PompeiiCache cache = this.pompeiiCache;
        if (cache == null || cache.sampler() != sampler) {
            double t = sampler.temperature().compute(new DensityFunction.SinglePointContext(IslandMap.SEED_PROBE_X, 0, IslandMap.SEED_PROBE_Z));
            cache = new PompeiiCache(sampler, POMPEII_SITES.computeIfAbsent(IslandMap.layoutSeed(t) * 97L + 29L, PompeiiSites::new));
            this.pompeiiCache = cache;
        }
        return cache.sites();
    }

    /** The Mediterranean's land (Csa): its shrubland, woods, olive groves, farmland and lavender fields. */
    private boolean isCsaLand(Holder<Biome> biome) {
        if (this.mediterranean.isPresent() && biome == this.mediterranean.get()) return true;
        if (this.csa.isEmpty()) return false;
        CsaBiomes b = this.csa.get();
        return biome == b.forest() || biome == b.oliveGrove() || biome == b.farmland() || biome == b.lavenderField();
    }

    // Pompeii's sites want the Mediterranean's land on level ground (where CsaHills levels it), not its mountains
    private PompeiiSites.CsaSurface csaSurface(Climate.Sampler sampler) {
        return (x, z) -> {
            if (!this.isCsaLand(this.surfaceBiome(x, z, sampler))) return false;
            Climate.TargetPoint c = sampler.sample(QuartPos.fromBlock(x), QuartPos.fromBlock(SURFACE_Y), QuartPos.fromBlock(z));
            return Climate.unquantizeCoord(c.erosion()) >= CsaHills.FLAT_EROSION_FULL;
        };
    }

    /** Pompeii's site in the cell this block lies in, if it has one (used to place the town). */
    public java.util.Optional<PompeiiSites.Site> pompeiiSite(int blockX, int blockZ, Climate.Sampler sampler) {
        if (this.deserts.flatMap(DesertBiomes::pompeii).isEmpty() || this.mediterranean.isEmpty()) return java.util.Optional.empty();
        return this.pompeiiSites(sampler).site(Math.floorDiv(blockX, PompeiiSites.CELL), Math.floorDiv(blockZ, PompeiiSites.CELL),
                this.csaSurface(sampler));
    }

    /** The rice terraces site of the cell this block lies in, if it has one (used to place the Thai villages). */
    public java.util.Optional<RiceTerraceSites.Site> riceTerraceSite(int blockX, int blockZ, Climate.Sampler sampler) {
        if (this.riceTerraces.isEmpty() || this.monsoon.isEmpty()) return java.util.Optional.empty();
        return this.terraceSites(sampler).site(Math.floorDiv(blockX, RiceTerraceSites.CELL), Math.floorDiv(blockZ, RiceTerraceSites.CELL),
                this.surfaceBiomes(sampler));
    }

    // every biome swap but the rice terraces
    private Holder<Biome> mappedBiome(int x, int y, int z, Climate.Sampler sampler) {
        Holder<Biome> biome = this.climateBiome(x, y, z, sampler);
        // no vanilla land is left anywhere: whatever vanilla land the climates above leave is their nearest one's
        if (this.isVanillaLand(biome)) {
            Holder<Biome> nearest = this.nearestClimateBiome(y, sampler.sample(x, y, z), biome);
            if (nearest != null) return nearest;
        }
        return biome;
    }

    // ---------------------------------------------------------------- no vanilla land

    // what vanilla keeps: its seas and their shores, its rivers, its cave biomes and its mushroom islands
    private static final Set<ResourceKey<Biome>> VANILLA_KEEPS = Set.of(Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.WARM_OCEAN,
            Biomes.LUKEWARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.COLD_OCEAN, Biomes.DEEP_COLD_OCEAN, Biomes.FROZEN_OCEAN,
            Biomes.DEEP_FROZEN_OCEAN, Biomes.BEACH, Biomes.SNOWY_BEACH, Biomes.STONY_SHORE, Biomes.RIVER, Biomes.FROZEN_RIVER,
            Biomes.LUSH_CAVES, Biomes.DRIPSTONE_CAVES, Biomes.DEEP_DARK, Biomes.MUSHROOM_FIELDS);
    // vanilla's mountain land: high up, it's the Alps' altitude bands (or the highlands, or the desert mountains)
    private static final Set<ResourceKey<Biome>> VANILLA_MOUNTAINS = Set.of(Biomes.JAGGED_PEAKS, Biomes.FROZEN_PEAKS,
            Biomes.STONY_PEAKS, Biomes.SNOWY_SLOPES, Biomes.GROVE, Biomes.MEADOW, Biomes.CHERRY_GROVE, Biomes.WINDSWEPT_HILLS,
            Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST, Biomes.WINDSWEPT_SAVANNA, Biomes.SAVANNA_PLATEAU,
            Biomes.WOODED_BADLANDS);

    private boolean isVanillaLand(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> "minecraft".equals(k.location().getNamespace()) && !VANILLA_KEEPS.contains(k)).orElse(false);
    }

    /**
     * The mod's biome for the climate here, in place of vanilla land: the deserts in the desert climate; the Amazon
     * jungle, the monsoon forest or the savanna in the tropics (by rainfall); the monsoon subtropics, the Mediterranean
     * or the humid subtropics in the warm temperate band (mountains there the Mediterranean highland); in the cool
     * band the Dfb's forest inland, the Prairie on its dry inland side, the Scottish highland on the wet coasts, the
     * Cfb's forest elsewhere; colder, the pine forest, the Subpolar taiga, the Tundra. High mountains anywhere cool or
     * cold are the Alps' altitude bands. Null only if the needed biomes aren't configured.
     */
    @javax.annotation.Nullable
    private Holder<Biome> nearestClimateBiome(int qy, Climate.TargetPoint c, Holder<Biome> vanilla) {
        boolean mountain = vanilla.unwrapKey().map(VANILLA_MOUNTAINS::contains).orElse(false);
        int by = QuartPos.toBlock(qy);
        long t = c.temperature(), h = c.humidity();
        CsaBiomes csa = this.csa.orElse(null);
        CfbBiomes cfb = this.cfb.orElse(null);
        if (this.deserts.isPresent() && isDesertClimate(c)) {
            DesertBiomes d = this.deserts.get();
            return mountain ? d.mountains() : d.lowland(c, false);
        }
        if (t >= HOT_MIN_TEMPERATURE) {
            if (h >= HUMID_MIN_HUMIDITY) return this.jungle;
            if (h >= FRINGE_MIN_HUMIDITY) return this.warmLand(c);
            return this.savanna.orElseGet(() -> this.warmLand(c));
        }
        if (csa != null && isCwaClimate(c)) {
            Holder<Biome> karst = csa.subtropicalKarstPeaks().orElse(null);
            if (mountain && karst != null) return karst;
            if (csa.monsoonSubtropical().isPresent()) return csa.monsoonSubtropical().get();
        }
        if (isWarmFringe(c)) return this.warmLand(c);
        if (t >= CSA_MIN_TEMPERATURE && csa != null) {
            if (mountain && csa.highland().isPresent()) return csa.highland().get();
            if (h < CSA_MAX_HUMIDITY_Q && this.mediterranean.isPresent()) return this.mediterranean.get();
            if (csa.subtropicalPlains().isPresent()) return csa.subtropicalPlains().get();
            if (this.mediterranean.isPresent()) return this.mediterranean.get();
        }
        if (cfb == null) return null;
        if ((mountain || by >= AlpineTerrain.MID_Y) && cfb.alpineMid().isPresent() && cfb.alpineHigh().isPresent()) {
            return by >= AlpineTerrain.HIGH_Y ? cfb.alpineHigh().get() : cfb.alpineMid().get();
        }
        Optional<PolarBiomes> polar = cfb.polar();
        if (t < TUNDRA_MAX_T && polar.isPresent()) return polar.get().tundra();
        if (t >= -4500) {
            if (isDfbClimate(c) && cfb.continentalForest().isPresent()) return cfb.continentalForest().get();
            if (c.continentalness() >= DFB_MIN_CONTINENTS && csa != null && csa.prairie().isPresent()) return csa.prairie().get();
            if (isCfcClimate(c) && cfb.highland().isPresent()) return cfb.highland().get();
            return cfb.forest();
        }
        if (c.continentalness() < DFB_MIN_CONTINENTS && cfb.highland().isPresent()) return cfb.highland().get();
        if (isDfbClimate(c) && cfb.pineForest().isPresent()) return cfb.pineForest().get();
        if (polar.isPresent() && polar.get().subpolarTaiga().isPresent()) return polar.get().subpolarTaiga().get();
        return cfb.pineForest().orElse(cfb.forest());
    }

    private Holder<Biome> climateBiome(int x, int y, int z, Climate.Sampler sampler) {
        // the tropical volcanic islands: jungle beach on the shore, Amazon jungle up the slopes, plains on top
        if (this.islandSummit.isPresent()) {
            Holder<Biome> island = this.islandBiome(x, y, z, sampler);
            if (island != null) return island;
        }
        // the climate is sampled once and vanilla's biome looked up from it (vanilla's own lookup would sample it again)
        Climate.TargetPoint climate = sampler.sample(x, y, z);
        // the Alps: above 230 and 280 the mountains' altitude bands, wherever the ground reaches so high; below them,
        // all through the Alps' own country, the Alpine valley
        Holder<Biome> alpine = this.alpineBiome(y, climate);
        if (alpine != null) return alpine;
        // the Cold lakeland: its own flat ground all through, whatever vanilla's ground would have been (so never
        // vanilla's cave biomes - dripstone and the like - showing at its surface)
        if (this.cfb.isPresent() && this.cfb.get().coldLakeland().isPresent() && QuartPos.toBlock(y) >= LAKELAND_MIN_Y
                && isDfbClimate(climate) && lakeland(climate)) {
            return this.cfb.get().coldLakeland().get();
        }
        Holder<Biome> biome = this.baseBiome(x, y, z, climate, sampler);
        // the far north: the Ice cap, the Tundra and the Subpolar taiga
        Holder<Biome> polar = this.polarBiome(y, climate, biome);
        if (polar != null) return polar;
        // the savanna's rivers, and those just around it, are the savanna's own (its water, its crocodiles - and no
        // Thai village, as there was on the Amazon's river here); without it, the Amazon jungle's
        if (this.savanna.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && biome.is(Biomes.RIVER) && isNearSavannaClimate(climate)) {
            return this.deserts.flatMap(DesertBiomes::savannaRiver).orElse(this.river);
        }
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
            if (highland != null && (land || mountain) && QuartPos.toBlock(y) >= HIGHLAND_MIN_Y
                    && climate.depth() > HIGHLAND_MIN_DEPTH) return highland;
            if (land || mountain) return this.csaLand(x, y, z, sampler, mountain, climate);
            if (this.csa.isPresent() && biome.is(Biomes.RIVER)) return this.csa.get().river();
            if (this.mediterraneanBeach.isPresent() && biome.unwrapKey().map(CSA_SHORES::contains).orElse(false)) {
                return this.mediterraneanBeach.get();
            }
        }
        // the mountains just around it are highland too
        if (this.mediterranean.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && isNearMediterraneanClimate(climate)
                && biome.unwrapKey().map(CSA_MOUNTAINS::contains).orElse(false) && QuartPos.toBlock(y) >= HIGHLAND_MIN_Y
                && climate.depth() > HIGHLAND_MIN_DEPTH) {
            Holder<Biome> highland = this.csa.flatMap(CsaBiomes::highland).orElse(null);
            if (highland != null) return highland;
        }
        // the Redwood forest (Csb): on the Mediterranean's cooler side
        Holder<Biome> redwood = this.csa.flatMap(CsaBiomes::redwood).orElse(null);
        if (redwood != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isRedwoodClimate(climate)
                && biome.unwrapKey().map(REDWOOD_LAND::contains).orElse(false)) {
            return redwood;
        }
        // the Redwood forest's rivers are the Mediterranean's
        if (this.csa.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && biome.is(Biomes.RIVER) && isRedwoodClimate(climate)) {
            return this.csa.get().river();
        }
        // the Prairie (Dfa): the vast, dry, treeless grassland of the cool temperate band's inland
        Holder<Biome> prairie = this.csa.flatMap(CsaBiomes::prairie).orElse(null);
        if (prairie != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isPrairieClimate(climate)
                && !biome.unwrapKey().map(PRAIRIE_KEEPS::contains).orElse(false)) {
            return prairie;
        }
        // the temperate oceanic band (Cfb)
        if (this.cfb.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH) {
            Holder<Biome> cfbBiome = isCfbClimate(climate) ? this.cfbBiome(x, y, z, sampler, climate, biome)
                    : isDfbClimate(climate) ? this.dfbBiome(x, z, climate, biome)
                    : isCfcClimate(climate) ? this.cfcBiome(climate, biome) : null;
            if (cfbBiome != null) return cfbBiome;
        }
        // the humid subtropics' rivers (Cfa, Cwa, and the American farmland's where it keeps any) are the Mediterranean's
        if (this.csa.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && biome.is(Biomes.RIVER)
                && (isCfaClimate(climate) || isCwaClimate(climate) || isFarmlandClimate(climate))) {
            boolean filled = isFarmlandClimate(climate) && CfaPlain.farmlandFill(climate.continentalness() / 10000.0,
                    climate.erosion() / 10000.0, climate.temperature() / 10000.0, climate.humidity() / 10000.0) >= 0.999;
            Holder<Biome> farmland = this.csa.flatMap(CsaBiomes::americanFarmland).orElse(null);
            if (filled && farmland != null) return farmland;
            return this.csa.get().river();
        }
        // the monsoon subtropics (Cwa): the karst peaks on the gentle ground, the hills elsewhere
        Holder<Biome> cwa = this.csa.flatMap(CsaBiomes::monsoonSubtropical).orElse(null);
        if (cwa != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isCwaClimate(climate)
                && biome.unwrapKey().map(CWA_LAND::contains).orElse(false)) {
            Holder<Biome> karst = this.csa.flatMap(CsaBiomes::subtropicalKarstPeaks).orElse(null);
            if (karst != null && climate.erosion() >= CWA_KARST_MIN_EROSION_Q) return karst;
            // the Cherry orchards: the wettest of the hills
            Holder<Biome> orchard = this.csa.flatMap(CsaBiomes::cherryOrchard).orElse(null);
            return orchard != null && climate.humidity() >= CWA_ORCHARD_MIN_HUMIDITY_Q ? orchard : cwa;
        }
        // the humid subtropics (Cfa): all their land the Subtropical plains, the strip along the shore their swamp
        // (which takes the beach as well, so it runs right down to the water)
        Holder<Biome> subtropicalPlains = this.csa.flatMap(CsaBiomes::subtropicalPlains).orElse(null);
        // further inland, and far beyond the Cfa, the American farmland
        Holder<Biome> farmland = this.csa.flatMap(CsaBiomes::americanFarmland).orElse(null);
        if (subtropicalPlains != null && farmland != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isFarmlandClimate(climate)
                && !biome.unwrapKey().map(ALL_OCEANS::contains).orElse(false)) {
            // a river runs on only near the farmland's edge; further in, its bed is filled and farmed
            boolean river = biome.unwrapKey().map(CFA_KEEPS::contains).orElse(false);
            if (!river || CfaPlain.farmlandFill(climate.continentalness() / 10000.0, climate.erosion() / 10000.0,
                    climate.temperature() / 10000.0, climate.humidity() / 10000.0) >= 0.999) {
                return farmland;
            }
        }
        if (subtropicalPlains != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isCfaClimate(climate)
                && !biome.unwrapKey().map(k -> ALL_OCEANS.contains(k) || CFA_KEEPS.contains(k)).orElse(false)) {
            Holder<Biome> swamp = this.csa.flatMap(CsaBiomes::subtropicalSwamp).orElse(null);
            if (swamp != null && climate.continentalness() < CFA_SWAMP_MAX_CONTINENTS) return swamp;
            return subtropicalPlains;
        }
        // the sea off the humid subtropics is lukewarm
        if (subtropicalPlains != null && climate.depth() < UNDERGROUND_MIN_DEPTH && isNearCfaClimate(climate)
                && biome.unwrapKey().map(CSA_SEAS::contains).orElse(false)) {
            ResourceKey<Biome> lukewarm = biome.is(Biomes.DEEP_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                    ? Biomes.DEEP_LUKEWARM_OCEAN : Biomes.LUKEWARM_OCEAN;
            Holder<Biome> sea = this.vanilla(lukewarm);
            if (sea != null) return sea;
        }
        // the sea off the Mediterranean is lukewarm, a little beyond its coast too, so its beaches only ever meet lukewarm water
        if (this.mediterranean.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && isNearMediterraneanClimate(climate)) {
            ResourceKey<Biome> lukewarm = biome.is(Biomes.DEEP_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN) || biome.is(Biomes.DEEP_FROZEN_OCEAN)
                    ? Biomes.DEEP_LUKEWARM_OCEAN : Biomes.LUKEWARM_OCEAN;
            if (biome.unwrapKey().map(CSA_SEAS::contains).orElse(false)) {
                Holder<Biome> sea = this.vanilla(lukewarm);
                if (sea != null) return sea;
            }
        }
        // the deserts take the whole hot band beyond the jungle's - every bit of land in it (deserts, badlands,
        // savannas, hills); only the sea, its shores and mushroom islands keep their own biomes
        if (this.deserts.isPresent() && climate.depth() < UNDERGROUND_MIN_DEPTH && isDesertClimate(climate)
                && !biome.unwrapKey().map(DESERT_KEEPS::contains).orElse(false)) {
            DesertBiomes d = this.deserts.get();
            // the Nile: one great, rare river of its own, right across whichever deserts it meets (NileMap; NileRiver
            // cuts its valley)
            if (d.nile().isPresent() && climate.continentalness() >= NILE_MIN_CONTINENTS
                    && this.nileMap(sampler).reach(QuartPos.toBlock(x) + 2, QuartPos.toBlock(z) + 2).inBiome()) {
                return d.nile().get();
            }
            boolean mountains = climate.erosion() < DESERT_MOUNTAIN_EROSION
                    && peaksAndValleys(climate.weirdness()) > DESERT_MOUNTAIN_MIN_PV;
            // the Lush desert (BSh): vanilla's wettest badlands, but for their most mountainous parts; its rivers keep some water
            if (d.lush().isPresent() && climate.humidity() >= LUSH_MIN_HUMIDITY) {
                if (biome.unwrapKey().map(BADLANDS::contains).orElse(false)
                        && !(mountains && climate.erosion() < LUSH_MOUNTAIN_EROSION)) {
                    return d.lush().get();
                }
                // (its rivers too: they've run dry, HotDesertSurfaceFeature fills their beds)
                if (biome.is(Biomes.RIVER) && !mountains && this.byBadlands(x, y, z, sampler)) return d.lush().get();
            }
            // the Australian desert (BSh): flat, dry and far from any water - its rivers included, buried under its plain
            if (d.australian().isPresent() && isAustralianClimate(climate) && !this.nearLush(x, y, z, sampler)) {
                return d.australian().get();
            }
            boolean steppe = d.steppe().isPresent() && isSteppeClimate(climate) && !this.nearLush(x, y, z, sampler);
            if (biome.is(Biomes.RIVER)) {
                // no ordinary river runs in the dry climates (B): every one has run dry, its bed filled with sand
                // (terracotta in the mountains) by HotDesertSurfaceFeature - the Nile is the only water there
                return mountains ? d.mountains() : d.lowland(climate, steppe);
            }
            if (mountains) return d.mountains();
            // the hot desert lies deep inland, nearer the sea it's the continental desert; where wetter, the Dry steppe
            return d.lowland(climate, steppe);
        }
        // vanilla savannas outside that climate (the hottest, driest ones) are desert
        if (this.savanna.isPresent() && biome.unwrapKey().map(VANILLA_SAVANNAS::contains).orElse(false)) {
            if (climate.depth() >= UNDERGROUND_MIN_DEPTH) return biome;
            Holder<Biome> desert = this.vanilla(Biomes.DESERT);
            return desert != null ? desert : this.savanna.get();
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
            // the monsoon forest's rivers, out to its drier edges, are the Amazon jungle's river too
            if (biome.is(Biomes.RIVER)) return this.river;
            if (biome.unwrapKey().map(TEMPERATE_LAND::contains).orElse(false)) return this.warmLand(climate);
            if (biome.is(Biomes.SWAMP)) return this.warmSwamp;
        }
        if (climate.temperature() >= HOT_MIN_TEMPERATURE && climate.humidity() >= FRINGE_MIN_HUMIDITY
                && biome.unwrapKey().map(COOLER_OCEANS::contains).orElse(false)) {
            return this.warmOcean;
        }
        return biome;
    }

    /** The Cfb's biome for vanilla's {@code biome} here, or null to keep vanilla's. The ground is never altered. */
    @javax.annotation.Nullable
    private Holder<Biome> cfbBiome(int x, int y, int z, Climate.Sampler sampler, Climate.TargetPoint climate, Holder<Biome> biome) {
        CfbBiomes cfb = this.cfb.get();
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key == null || CFB_KEEPS.contains(key)) return null;
        // the sea off it is plain ocean: never cold or frozen, nor lukewarm
        if (ALL_OCEANS.contains(key)) return this.vanilla(DEEP_OCEANS.contains(key) ? Biomes.DEEP_OCEAN : Biomes.OCEAN);
        // its rivers are vanilla's own (broadened in the lowlands, CfbRiver); where vanilla carries one out over the
        // shallows off the coast, that's sea
        if (key == Biomes.RIVER || key == Biomes.FROZEN_RIVER) {
            return climate.continentalness() < CFB_RIVER_MIN_CONTINENTS ? this.vanilla(Biomes.OCEAN) : cfb.floodedRiver();
        }
        // vanilla's rocky shore - the steep coast that rises straight out of the sea - is the White cliffs
        if (key == Biomes.STONY_SHORE) return cfb.whiteCliffs();
        // its sandy shores stay beaches (never snowy)
        if (key == Biomes.BEACH || key == Biomes.SNOWY_BEACH) return this.vanilla(Biomes.BEACH);
        long c = climate.continentalness();
        // the sunflower fields: small patches in the forest and the countryside, on the flattest ground and only
        // along the middle of a band of weirdness (which doesn't grow with Large Biomes, so they stay small)
        if (climate.erosion() >= CFB_SUNFLOWER_MIN_EROSION && c >= CFB_SUNFLOWER_MIN_CONTINENTS
                && Math.abs(climate.weirdness()) < CFB_SUNFLOWER_MAX_WEIRDNESS) {
            return cfb.sunflowerField();
        }
        return c < CFB_COUNTRYSIDE_MAX_CONTINENTS ? cfb.countryside() : cfb.forest();
    }

    private static final long CFB_RIVER_MIN_CONTINENTS = -1100;
    // what keeps its own biome in the Cfc and the Dfb: the high peaks, and the mushroom islands
    private static final Set<ResourceKey<Biome>> COLD_KEEPS = Set.of(Biomes.MUSHROOM_FIELDS, Biomes.JAGGED_PEAKS,
            Biomes.FROZEN_PEAKS, Biomes.STONY_PEAKS);

    private static final long TUNDRA_MAX_T = Math.round(AlpineTerrain.TUNDRA * 10000), SUBPOLAR_MAX_T = Math.round(AlpineTerrain.SUBPOLAR * 10000),
            SUBPOLAR_WET_COAST_Q = Math.round(AlpineTerrain.SUBPOLAR_WET_COAST * 10000);

    /** The far north's biomes (PolarBiomes), or null. */
    @javax.annotation.Nullable
    private Holder<Biome> polarBiome(int qy, Climate.TargetPoint climate, Holder<Biome> biome) {
        if (this.cfb.isEmpty()) return null;
        Optional<PolarBiomes> polar = this.cfb.get().polar();
        long t = climate.temperature();
        if (t >= SUBPOLAR_MAX_T) return null;
        // the Ice cap: its own solid ground right down, so all of it, at every depth
        if (polar.isPresent() && AlpineTerrain.iceCapMask(t / 10000.0, climate.continentalness() / 10000.0) >= 0.5) {
            return polar.get().iceCap();
        }
        if (climate.depth() >= UNDERGROUND_MIN_DEPTH) return null;
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key == null || COLD_KEEPS.contains(key) || ALL_OCEANS.contains(key)) return null;
        if (key == Biomes.RIVER || key == Biomes.FROZEN_RIVER) return null;
        if (t < TUNDRA_MAX_T) return polar.map(PolarBiomes::tundra).orElse(null);
        // the Subpolar taiga, but on the wet coasts (the Scottish highland's)
        if (climate.continentalness() < DFB_MIN_CONTINENTS && climate.humidity() >= SUBPOLAR_WET_COAST_Q) return null;
        if (key == Biomes.BEACH || key == Biomes.SNOWY_BEACH || key == Biomes.STONY_SHORE) return this.vanilla(Biomes.STONY_SHORE);
        return polar.flatMap(PolarBiomes::subpolarTaiga).orElseGet(() -> this.vanilla(Biomes.OLD_GROWTH_SPRUCE_TAIGA));
    }

    @javax.annotation.Nullable
    private Holder<Biome> alpineBiome(int qy, Climate.TargetPoint climate) {
        if (this.cfb.isEmpty()) return null;
        CfbBiomes cfb = this.cfb.get();
        if (cfb.alpineValley().isEmpty() || cfb.alpineMid().isEmpty() || cfb.alpineHigh().isEmpty()) return null;
        int by = QuartPos.toBlock(qy);
        double t = climate.temperature() / 10000.0, h = climate.humidity() / 10000.0;
        double c = climate.continentalness() / 10000.0, e = climate.erosion() / 10000.0;
        double m = AlpineTerrain.mask(t, h, c, e);
        // the altitude bands: all through the Alps, and on any high ground in the cool and cold climates beyond the
        // Continental mountains (whose rolling hills keep their own biome however high they roll)
        double b = AlpineTerrain.beltMask(t, h, c, e);
        boolean belt = b >= AlpineTerrain.BELT && m < AlpineTerrain.ALPS_BIOME;
        if (m >= AlpineTerrain.ALPS_BIOME || (b <= 0.0 && climate.temperature() <= ALPINE_BANDS_MAX_TEMPERATURE)) {
            if (by >= AlpineTerrain.HIGH_Y) return cfb.alpineHigh().get();
            if (by >= AlpineTerrain.MID_Y) return cfb.alpineMid().get();
        }
        // the whole of the Alps below them, right down to the bottom of the world, is the valley's (so no caves or
        // ravines are carved anywhere under them: the carvers go by the biome deep down)
        if (m >= AlpineTerrain.ALPS_BIOME) return cfb.alpineValley().get();
        // round them, the Continental mountains (Dfc); under them vanilla's caves keep their own biomes
        if (belt && by >= CONTINENTAL_MOUNTAINS_MIN_Y && cfb.continentalMountains().isPresent()) {
            return cfb.continentalMountains().get();
        }
        return null;
    }

    // the altitude bands reach high ground only where it is cool or cold (temperature -0.05 and below)
    private static final long ALPINE_BANDS_MAX_TEMPERATURE = -500;
    private static final int CONTINENTAL_MOUNTAINS_MIN_Y = 40;

    /** The Cfc's biome: the Scottish highland on its hills and mountains, temperate forest on its flat land; its rocky shores the cliffs. */
    @javax.annotation.Nullable
    private Holder<Biome> cfcBiome(Climate.TargetPoint climate, Holder<Biome> biome) {
        CfbBiomes cfb = this.cfb.get();
        if (cfb.highland().isEmpty()) return null;
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        if (key == null || COLD_KEEPS.contains(key) || ALL_OCEANS.contains(key)) return null;
        // its rivers are vanilla's own, frozen where vanilla freezes them
        if (key == Biomes.RIVER || key == Biomes.FROZEN_RIVER || key == Biomes.BEACH) return null;
        if (key == Biomes.SNOWY_BEACH) return this.vanilla(Biomes.BEACH);
        if (key == Biomes.STONY_SHORE) return cfb.scottishCliffs().orElse(null);
        // the highland only on hilly ground (vanilla's hills and mountains); the flat land is temperate forest
        if (climate.erosion() < CFC_HILL_MAX_EROSION || HILL_KEYS.contains(key)) return cfb.highland().get();
        return cfb.forest();
    }

    // rolling and hilly ground: erosion below 0.05 (vanilla's hills and mountains, and its rolling land, everything
    // but the flattest plains), and vanilla's own upland biomes
    private static final long CFC_HILL_MAX_EROSION = 500;
    private static final Set<ResourceKey<Biome>> HILL_KEYS = Set.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS,
            Biomes.WINDSWEPT_FOREST, Biomes.MEADOW, Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.CHERRY_GROVE);

    /** The Dfb's biome: the Continental forest, with small Pine forest stands on the flat here and there. */
    @javax.annotation.Nullable
    private Holder<Biome> dfbBiome(int x, int z, Climate.TargetPoint climate, Holder<Biome> biome) {
        CfbBiomes cfb = this.cfb.get();
        if (cfb.continentalForest().isEmpty() || cfb.pineForest().isEmpty()) return null;
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        // the Cold lakeland (Dfc): its own flat ground and lakes (AlpineTerrain.lakeland) over the whole of it,
        // rivers, hills and peaks and all
        if (key != null && key != Biomes.MUSHROOM_FIELDS && !ALL_OCEANS.contains(key) && cfb.coldLakeland().isPresent()
                && lakeland(climate)) return cfb.coldLakeland().get();
        if (key == null || COLD_KEEPS.contains(key) || ALL_OCEANS.contains(key) || key == Biomes.STONY_SHORE) return null;
        // its rivers are vanilla's own, frozen where vanilla freezes them
        if (key == Biomes.RIVER || key == Biomes.FROZEN_RIVER || key == Biomes.BEACH) return null;
        if (key == Biomes.SNOWY_BEACH) return this.vanilla(Biomes.BEACH);
        boolean flat = climate.erosion() >= PINE_MIN_EROSION;
        // the pine woods: big stands here and there on the flat, never on hills
        if (flat && pineStand(x, z)) return cfb.pineForest().get();
        // the clearings: open, grassy glades in the forest, a lone tree here and there
        if (flat && clearing(x, z) && cfb.continentalClearing().isPresent()) return cfb.continentalClearing().get();
        return cfb.continentalForest().get();
    }

    // the pine woods: big, roundish forests of 9-14 chunks across - at most one to every PINE_CELL x PINE_CELL
    // quarts, in PINE_CHANCE of them, a disc of 18-28 quarts' radius (72-112 blocks) with a slightly wavy edge,
    // somewhere inside it - and only on flat ground
    private static final int PINE_CELL = 80;
    private static final int LAKELAND_MIN_Y = 30;

    static boolean lakeland(Climate.TargetPoint c) {
        double t = c.temperature() / 10000.0, h = c.humidity() / 10000.0, k = c.continentalness() / 10000.0,
                e = c.erosion() / 10000.0;
        return AlpineTerrain.lakelandWeight(t, h, k, e) >= AlpineTerrain.LAKELAND;
    }
    // the clearings: at most one to every CLEARING_CELL x CLEARING_CELL quarts, in CLEARING_CHANCE of them, a disc of
    // 7-13 quarts' radius (28-52 blocks)
    private static final int CLEARING_CELL = 48;
    private static final double CLEARING_CHANCE = 0.45;

    static boolean clearing(int qx, int qz) {
        int ci = Math.floorDiv(qx, CLEARING_CELL), cj = Math.floorDiv(qz, CLEARING_CELL);
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                int a = i + 31337, b = j - 52711;
                if (cellValue(a, b) >= CLEARING_CHANCE) continue;
                double r = 7.0 + 6.0 * cellValue(a + 7919, b);
                double cx = i * CLEARING_CELL + r + (CLEARING_CELL - 2 * r) * cellValue(a, b + 7919);
                double cz = j * CLEARING_CELL + r + (CLEARING_CELL - 2 * r) * cellValue(a + 104729, b + 7919);
                double dx = qx - cx, dz = qz - cz;
                double wobble = 1.0 + 0.22 * Math.sin(Math.atan2(dz, dx) * 4.0 + cellValue(a, b - 7919) * 6.28);
                if (dx * dx + dz * dz <= r * r * wobble * wobble) return true;
            }
        }
        return false;
    }
    private static final double PINE_CHANCE = 0.5;
    private static final long PINE_MIN_EROSION = -1000;

    static boolean pineStand(int qx, int qz) {
        int ci = Math.floorDiv(qx, PINE_CELL), cj = Math.floorDiv(qz, PINE_CELL);
        for (int i = ci - 1; i <= ci + 1; i++) {
            for (int j = cj - 1; j <= cj + 1; j++) {
                if (cellValue(i, j) >= PINE_CHANCE) continue;
                double r = 18.0 + 10.0 * cellValue(i + 7919, j);
                double cx = i * PINE_CELL + r + (PINE_CELL - 2 * r) * cellValue(i, j + 7919);
                double cz = j * PINE_CELL + r + (PINE_CELL - 2 * r) * cellValue(i + 104729, j + 7919);
                double dx = qx - cx, dz = qz - cz;
                double wobble = 1.0 + 0.18 * Math.sin(Math.atan2(dz, dx) * 3.0 + cellValue(i, j - 7919) * 6.28);
                if (dx * dx + dz * dz <= r * r * wobble * wobble) return true;
            }
        }
        return false;
    }

    private static double cellValue(int i, int j) {
        long h = i * 0x9E3779B97F4A7C15L ^ j * 0xC2B2AE3D27D4EB4FL ^ 0x5851F42D4C957F2DL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
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
        Climate.TargetPoint c = sampler.sample(x, y, z);
        return this.baseBiome(x, y, z, c, sampler).unwrapKey().map(BADLANDS::contains).orElse(false)
                && c.humidity() >= LUSH_MIN_HUMIDITY;
    }

    // These two look around a column at the surface (they only matter for the surface biomes). Each takes several
    // climate samples, so the answer is kept per column: a chunk's biomes ask about each column 96 times over.
    private boolean nearLush(int x, int y, int z, Climate.Sampler sampler) {
        return this.columnFlag(NEAR_LUSH, x, z, sampler, () -> {
            int sy = QuartPos.fromBlock(SURFACE_Y);
            int r = BADLANDS_CLEARANCE, h = BADLANDS_CLEARANCE / 2;
            for (int[] o : new int[][]{{r, 0}, {-r, 0}, {0, r}, {0, -r}, {h, h}, {h, -h}, {-h, h}, {-h, -h}}) {
                if (this.lushAt(x + o[0], sy, z + o[1], sampler)) return true;
            }
            return this.lushAt(x, sy, z, sampler);
        });
    }

    private boolean byBadlands(int x, int y, int z, Climate.Sampler sampler) {
        return this.columnFlag(BY_BADLANDS, x, z, sampler, () -> {
            int sy = QuartPos.fromBlock(SURFACE_Y);
            for (int[] o : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
                if (this.base.getNoiseBiome(x + o[0], sy, z + o[1], sampler).unwrapKey().map(BADLANDS::contains).orElse(false)) return true;
            }
            return false;
        });
    }

    // ---------------------------------------------------------------- caches

    /** Vanilla's biome for a climate already sampled. */
    private Holder<Biome> baseBiome(int x, int y, int z, Climate.TargetPoint climate, Climate.Sampler sampler) {
        if (this.base instanceof net.minecraft.world.level.biome.MultiNoiseBiomeSource multiNoise) {
            return multiNoise.getNoiseBiome(climate);
        }
        return this.base.getNoiseBiome(x, y, z, sampler);
    }

    // vanilla's biomes by key, looked up once (they used to be searched for on every call)
    private final java.util.Map<ResourceKey<Biome>, Optional<Holder<Biome>>> vanillaBiomes = new java.util.concurrent.ConcurrentHashMap<>();

    private Holder<Biome> vanilla(ResourceKey<Biome> key) {
        return this.vanillaBiomes.computeIfAbsent(key,
                k -> this.base.possibleBiomes().stream().filter(b -> b.is(k)).findFirst()).orElse(null);
    }

    // (whether a column lies in rice terraces is also kept: it means looking up several cells' sites)
    private static final int NEAR_LUSH = 0, BY_BADLANDS = 1, IN_TERRACES_AW = 2, IN_TERRACES_CWA = 3, IN_POMPEII = 4, COLUMN_FLAGS = 5;
    private static final int COLUMN_CACHE = 1024;

    // per thread, per column (direct-mapped by quart column): 0 not yet known, 1 false, 2 true
    private static final class ColumnFlags {
        Climate.Sampler sampler;
        final long[][] keys = new long[COLUMN_FLAGS][COLUMN_CACHE];
        final byte[][] values = new byte[COLUMN_FLAGS][COLUMN_CACHE];
    }

    private final ThreadLocal<ColumnFlags> columnFlags = ThreadLocal.withInitial(ColumnFlags::new);

    private boolean columnFlag(int which, int qx, int qz, Climate.Sampler sampler, java.util.function.BooleanSupplier work) {
        ColumnFlags flags = this.columnFlags.get();
        if (flags.sampler != sampler) {
            flags.sampler = sampler;
            for (byte[] v : flags.values) java.util.Arrays.fill(v, (byte) 0);
        }
        long key = ((long) qx << 32) ^ (qz & 0xFFFFFFFFL);
        int slot = ((qx & 31) << 5) | (qz & 31);
        if (flags.values[which][slot] != 0 && flags.keys[which][slot] == key) return flags.values[which][slot] == 2;
        boolean result = work.getAsBoolean();
        flags.keys[which][slot] = key;
        flags.values[which][slot] = (byte) (result ? 2 : 1);
        return result;
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

    /** The savanna's climate widened by a little, for its rivers (which are the Amazon jungle's right round it). */
    static boolean isNearSavannaClimate(Climate.TargetPoint c) {
        return c.continentalness() >= SAVANNA_MIN_CONTINENTS - 300 && c.humidity() < FRINGE_MIN_HUMIDITY + 300
                && c.temperature() >= SAVANNA_MIN_TEMPERATURE - 300
                && c.temperature() <= HOT_MAX_TEMPERATURE
                && !(c.temperature() > DESERT_EDGE_TEMPERATURE && c.humidity() < DESERT_EDGE_HUMIDITY_Q);
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
