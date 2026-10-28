package net.krisplis.koppenclimate.worldgen;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.worldgen.biome.KoppenBiomeSource;
import net.krisplis.koppenclimate.worldgen.feature.BiomeSurfaceFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.krisplis.koppenclimate.worldgen.island.VolcanicIslands;
import net.krisplis.koppenclimate.worldgen.island.VolcanoPiece;
import net.krisplis.koppenclimate.worldgen.island.VolcanoStructure;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, KoppenClimateMod.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, KoppenClimateMod.MOD_ID);

    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> HOT_DESERT_SURFACE =
            FEATURES.register("hot_desert_surface", () -> new net.krisplis.koppenclimate.worldgen.feature.HotDesertSurfaceFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<BiomeSurfaceFeature.Config>> BIOME_SURFACE =
            FEATURES.register("biome_surface", () -> new BiomeSurfaceFeature(BiomeSurfaceFeature.Config.CODEC));

    // one paddy of the Rice Terraces (Aw)
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> RICE_TERRACES =
            FEATURES.register("rice_terraces", () -> new net.krisplis.koppenclimate.worldgen.feature.RiceTerracesFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    // the Mediterranean's olive, Italian pine and cypress trees
    public static final RegistryObject<Feature<net.krisplis.koppenclimate.worldgen.tree.MediterraneanTreeFeature.Config>> MEDITERRANEAN_TREE =
            FEATURES.register("mediterranean_tree", () -> new net.krisplis.koppenclimate.worldgen.tree.MediterraneanTreeFeature(
                    net.krisplis.koppenclimate.worldgen.tree.MediterraneanTreeFeature.Config.CODEC));

    // a lone one-block cactus in the Mediterranean
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> LONE_CACTUS =
            FEATURES.register("lone_cactus", () -> new net.krisplis.koppenclimate.worldgen.feature.LoneCactusFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    // a field of wheat or barley in the Mediterranean farmland, and the vineyard of the wine terraces
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> CROP_FIELD =
            FEATURES.register("crop_field", () -> new net.krisplis.koppenclimate.worldgen.feature.CropFieldFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> WINE_TERRACE_VINES =
            FEATURES.register("wine_terrace_vines", () -> new net.krisplis.koppenclimate.worldgen.feature.WineTerraceFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    // the lavender of the lavender fields
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> LAVENDER_FIELD =
            FEATURES.register("lavender_field", () -> new net.krisplis.koppenclimate.worldgen.feature.LavenderFieldFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    public static final RegistryObject<MapCodec<KoppenBiomeSource>> KOPPEN_BIOME_SOURCE =
            BIOME_SOURCES.register("koppen", () -> KoppenBiomeSource.CODEC);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, KoppenClimateMod.MOD_ID);

    // the jungle temple, as the Amazon jungle gets it (worldgen/structure/amazon_jungle_temple.json)
    public static final RegistryObject<StructureType<AmazonTempleStructure>> AMAZON_TEMPLE =
            STRUCTURE_TYPES.register("amazon_jungle_temple", () -> () -> AmazonTempleStructure.CODEC);

    // the tropical volcanic islands: their terrain (used by the mod's noise settings), and their craters
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, KoppenClimateMod.MOD_ID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, KoppenClimateMod.MOD_ID);

    public static final RegistryObject<MapCodec<VolcanicIslands>> VOLCANIC_ISLANDS =
            DENSITY_FUNCTION_TYPES.register("volcanic_islands", () -> VolcanicIslands.MAP_CODEC);
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.SavannaCeiling>> SAVANNA_CEILING =
            DENSITY_FUNCTION_TYPES.register("savanna_ceiling", () -> net.krisplis.koppenclimate.worldgen.biome.SavannaCeiling.MAP_CODEC);
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.CsaHills>> CSA_HILLS =
            DENSITY_FUNCTION_TYPES.register("csa_hills", () -> net.krisplis.koppenclimate.worldgen.biome.CsaHills.MAP_CODEC);
    public static final RegistryObject<StructureType<VolcanoStructure>> VOLCANO =
            STRUCTURE_TYPES.register("volcanic_island", () -> () -> VolcanoStructure.CODEC);
    public static final RegistryObject<StructurePieceType> VOLCANO_PIECE =
            STRUCTURE_PIECES.register("volcano_crater", () -> (StructurePieceType.ContextlessType) VolcanoPiece::new);

    public static void register(IEventBus eventBus) {
        DENSITY_FUNCTION_TYPES.register(eventBus);
        STRUCTURE_PIECES.register(eventBus);
        STRUCTURE_TYPES.register(eventBus);
        FEATURES.register(eventBus);
        BIOME_SOURCES.register(eventBus);
    }
}
