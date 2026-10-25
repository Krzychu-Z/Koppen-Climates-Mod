package net.krisplis.koppenclimate.worldgen.biome;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

// The Köppen Af biomes. Their definitions are data: worldgen/biome/*.json
public class ModBiomes {
    public static final ResourceKey<Biome> AMAZON_JUNGLE = key("amazon_jungle");
    public static final ResourceKey<Biome> AMAZON_JUNGLE_RIVER = key("amazon_jungle_river");
    public static final ResourceKey<Biome> JUNGLE_BEACH = key("jungle_beach");

    // all three Af biomes (tags/worldgen/biome/af.json)
    public static final TagKey<Biome> AF = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "af"));

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
    }
}
