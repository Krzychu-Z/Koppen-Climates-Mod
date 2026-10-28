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
    // the Mediterranean highland (Csb), on the mountains in and around the Csa
    public static final ResourceKey<Biome> MEDITERRANEAN_HIGHLAND = key("mediterranean_highland_csb");

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

    /** The three deserts: hot (BWh), continental (BWk) and the desert mountains (BWk). */
    public static boolean isDesert(net.minecraft.core.Holder<Biome> biome) {
        return biome.is(HOT_DESERT) || biome.is(CONTINENTAL_DESERT) || biome.is(DESERT_MOUNTAINS);
    }

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
    }
}
