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
    public static final ResourceKey<Biome> MEDITERRANEAN = key("mediterranean_csa");
    public static final ResourceKey<Biome> MEDITERRANEAN_BEACH = key("mediterranean_beach_csa");

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

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
    }
}
