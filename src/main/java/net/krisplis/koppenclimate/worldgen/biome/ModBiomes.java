package net.krisplis.koppenclimate.worldgen.biome;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

// The Köppen Af and Aw biomes. Their definitions are data: worldgen/biome/*.json
public class ModBiomes {
    public static final ResourceKey<Biome> AMAZON_JUNGLE = key("amazon_jungle_af");
    public static final ResourceKey<Biome> AMAZON_JUNGLE_RIVER = key("amazon_jungle_river_af");
    public static final ResourceKey<Biome> TROPICAL_BEACH = key("tropical_beach_af");
    // Aw: the drier edge of the tropics, a monsoon forest of big trees and bamboo
    public static final ResourceKey<Biome> TROPICAL_MONSOON = key("tropical_monsoon_aw");
    // Aw on the hills: stepped rice paddies down the slopes
    public static final ResourceKey<Biome> RICE_TERRACES = key("rice_terraces_aw");
    // As: the dry tropical savanna of Africa, in place of every vanilla savanna
    public static final ResourceKey<Biome> AFRICAN_SAVANNA = key("african_savanna_as");
    // Csa: the Mediterranean - hills of white limestone and terra rossa, olives, stone pines and cypresses - and
    // its shore of white gravel
    // the Mediterranean shrubland, its commonest biome; then its forest, olive groves, river, farmland and lavender fields
    public static final ResourceKey<Biome> MEDITERRANEAN = key("mediterranean_shrubland_csa");
    public static final ResourceKey<Biome> MEDITERRANEAN_FOREST = key("mediterranean_forest_csa");
    public static final ResourceKey<Biome> OLIVE_GROVE = key("olive_grove_csa");
    public static final ResourceKey<Biome> MEDITERRANEAN_RIVER = key("mediterranean_river_csa");
    public static final ResourceKey<Biome> MEDITERRANEAN_FARMLAND = key("mediterranean_farmland_csa");
    public static final ResourceKey<Biome> LAVENDER_FIELD = key("lavender_field_csa");
    // every Csa biome, its beach included (tags/worldgen/biome/is_mediterranean.json)
    public static final TagKey<Biome> IS_MEDITERRANEAN = TagKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "is_mediterranean"));
    public static final ResourceKey<Biome> MEDITERRANEAN_BEACH = key("mediterranean_beach_csa");
    // the hot desert (BWh), in place of vanilla's desert
    public static final ResourceKey<Biome> HOT_DESERT = key("hot_desert_bwh");
    public static final ResourceKey<Biome> CONTINENTAL_DESERT = key("continental_desert_bwk");
    public static final ResourceKey<Biome> DESERT_MOUNTAINS = key("desert_mountains_bwk");
    // the Dry steppe (BSk): the continental desert's wetter side, its ground all grass
    public static final ResourceKey<Biome> DRY_STEPPE = key("dry_steppe_bsk");
    // the Australian desert (BSh): a dead-flat plain of red sand, with its red rocks, eucalyptus, koalas and kangaroos
    public static final ResourceKey<Biome> AUSTRALIAN_DESERT = key("australian_desert_bsh");
    // the Lush desert (BSh), in place of vanilla's badlands
    public static final ResourceKey<Biome> LUSH_DESERT = key("lush_desert_bsh");
    // the Mediterranean highland (Csb), on the mountains in and around the Csa
    public static final ResourceKey<Biome> MEDITERRANEAN_HIGHLAND = key("mediterranean_highland_csb");
    // the Redwood forest (Csb): the cooler side of the Mediterranean climate, nothing but giant redwoods
    public static final ResourceKey<Biome> REDWOOD_FOREST = key("redwood_forest_csb");
    // the humid subtropics (Cfa): the Subtropical plains on the flat coastal land, and the Subtropical swamp (a
    // floodplain) along their lukewarm shore
    public static final ResourceKey<Biome> SUBTROPICAL_PLAINS = key("subtropical_plains_cfa");
    public static final ResourceKey<Biome> SUBTROPICAL_SWAMP = key("subtropical_swamp_cfa");
    // the American farmland (Cfa): the humid subtropics further inland, one great patchwork of fields
    public static final ResourceKey<Biome> AMERICAN_FARMLAND = key("american_farmland_cfa");
    // the monsoon subtropics (Cwa), southern China: long rolling green hills and deep valleys (the Monsoon subtropical
    // hills), and on the gentler ground a plain of white limestone karst towers (the Subtropical karst peaks)
    public static final ResourceKey<Biome> MONSOON_SUBTROPICAL = key("monsoon_subtropical_cwa");
    public static final ResourceKey<Biome> SUBTROPICAL_KARST_PEAKS = key("subtropical_karst_peaks_cwa");
    // the wettest of the hills: a sparse orchard of cherries, apples, magnolias and little oaks, where only peaceful
    // animals ever spawn (the Japanese villages stand in it)
    public static final ResourceKey<Biome> CHERRY_ORCHARD = key("cherry_orchard_cwa");
    // the hot-summer continental climate (Dfa): the Prairie
    public static final ResourceKey<Biome> PRAIRIE = key("prairie_dfa");
    // the temperate oceanic band (Cfb): its forest and sunflower fields, the English countryside behind the White
    // cliffs, and its broad, brown Flooded rivers
    public static final ResourceKey<Biome> TEMPERATE_FOREST = key("temperate_forest_cfb");
    public static final ResourceKey<Biome> SUNFLOWER_FIELD = key("sunflower_field_cfb");
    public static final ResourceKey<Biome> ENGLISH_COUNTRYSIDE = key("english_countryside_cfb");
    public static final ResourceKey<Biome> WHITE_CLIFFS = key("white_cliffs_cfb");
    public static final ResourceKey<Biome> FLOODED_RIVER = key("flooded_river_cfb");
    // the subpolar oceanic band (Cfc) and the humid continental (Dfb)
    public static final ResourceKey<Biome> SCOTTISH_HIGHLAND = key("scottish_highland_cfc");
    public static final ResourceKey<Biome> SCOTTISH_CLIFFS = key("scottish_cliffs_cfc");
    public static final ResourceKey<Biome> CONTINENTAL_FOREST = key("continental_forest_dfb");
    public static final ResourceKey<Biome> PINE_FOREST = key("pine_forest_dfb");
    public static final ResourceKey<Biome> ALPINE_VALLEY = key("alpine_valley_dfb");
    public static final ResourceKey<Biome> ALPINE_MID = key("alpine_mid_altitude_dfc");
    public static final ResourceKey<Biome> ALPINE_HIGH = key("alpine_high_altitude_et");
    // inside them, great sweeps of rice terraces
    public static final ResourceKey<Biome> SUBTROPICAL_RICE_TERRACES = key("subtropical_rice_terraces_cwa");

    // all three Af biomes (tags/worldgen/biome/af.json)
    public static final TagKey<Biome> AF = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "af"));

    /** The Amazon jungle and its river, and the monsoon forest: the overcast, rainy tropical forests. */
    public static boolean isRainforest(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(AMAZON_JUNGLE) || biome.is(AMAZON_JUNGLE_RIVER) || isMonsoon(biome);
    }

    /** The two Aw biomes: the monsoon forest and the rice terraces. */
    public static boolean isMonsoon(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(TROPICAL_MONSOON) || biome.is(RICE_TERRACES);
    }

    /** Every Csa biome, its beach included (by key, so it never depends on biome tags reaching the client). */
    public static boolean isMediterranean(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(MEDITERRANEAN) || biome.is(MEDITERRANEAN_FOREST) || biome.is(OLIVE_GROVE) || biome.is(MEDITERRANEAN_RIVER)
                || biome.is(MEDITERRANEAN_FARMLAND) || biome.is(LAVENDER_FIELD) || biome.is(MEDITERRANEAN_BEACH)
                || biome.is(MEDITERRANEAN_HIGHLAND);
    }

    /** The deserts: hot (BWh), the Sahel (BWk), the desert mountains (BWk), the Lush desert (BSh), the Dry steppe (BSk) and the Australian desert (BSh). */
    public static boolean isDesert(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(HOT_DESERT) || biome.is(CONTINENTAL_DESERT) || biome.is(DESERT_MOUNTAINS) || isLushDesert(biome)
                || biome.is(DRY_STEPPE) || biome.is(AUSTRALIAN_DESERT);
    }

    /** The Subtropical plains and swamp (Cfa). */
    public static boolean isSubtropical(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(SUBTROPICAL_PLAINS) || biome.is(SUBTROPICAL_SWAMP) || biome.is(AMERICAN_FARMLAND) || isCwa(biome);
    }

    /** The monsoon subtropics (Cwa) and their rice terraces. */
    public static boolean isCwa(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(MONSOON_SUBTROPICAL) || biome.is(SUBTROPICAL_KARST_PEAKS) || biome.is(SUBTROPICAL_RICE_TERRACES)
                || biome.is(CHERRY_ORCHARD);
    }

    /** The Lush desert (BSh). */
    public static boolean isLushDesert(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(LUSH_DESERT);
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
    }
}
