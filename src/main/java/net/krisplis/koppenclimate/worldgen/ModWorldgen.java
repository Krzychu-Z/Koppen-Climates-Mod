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
    // the American farmland's (Cfa) patchwork of fields, meadows, woods and hedgerows
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> AMERICAN_FARMLAND =
            FEATURES.register("american_farmland", () -> new net.krisplis.koppenclimate.worldgen.feature.AmericanFarmlandFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    // the Prairie's (Dfa) sward: short grass on every grass block, ferns and the odd dandelion
    public static final RegistryObject<Feature<net.krisplis.koppenclimate.worldgen.feature.PrairieGrassFeature.Config>> PRAIRIE_GRASS =
            FEATURES.register("prairie_grass", () -> new net.krisplis.koppenclimate.worldgen.feature.PrairieGrassFeature(
                    net.krisplis.koppenclimate.worldgen.feature.PrairieGrassFeature.Config.CODEC));
    // the Alpine valley's stream and ponds: water over the white gravel bed, a few cod in it
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> ALPINE_STREAM =
            FEATURES.register("alpine_stream", () -> new net.krisplis.koppenclimate.worldgen.feature.AlpineStreamFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    // placement only between two heights (the Alps' spruce foothills, the dwarf pines at the bottom of the mid-altitude)
    public static final DeferredRegister<net.minecraft.world.level.levelgen.placement.PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, KoppenClimateMod.MOD_ID);
    public static final RegistryObject<net.minecraft.world.level.levelgen.placement.PlacementModifierType<net.krisplis.koppenclimate.worldgen.feature.HeightBandFilter>> HEIGHT_BAND =
            PLACEMENT_MODIFIERS.register("height_band", () -> () -> net.krisplis.koppenclimate.worldgen.feature.HeightBandFilter.CODEC);
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

    // the Lush desert's (BSh) Joshua trees, dead oaks and saguaros
    public static final RegistryObject<Feature<net.krisplis.koppenclimate.worldgen.feature.LushDesertTreeFeature.Config>> LUSH_DESERT_TREE =
            FEATURES.register("lush_desert_tree", () -> new net.krisplis.koppenclimate.worldgen.feature.LushDesertTreeFeature(
                    net.krisplis.koppenclimate.worldgen.feature.LushDesertTreeFeature.Config.CODEC));

    // the Australian desert (BSh): its rainbow eucalyptus (with koalas) and its lone red rocks
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> EUCALYPTUS_TREE =
            FEATURES.register("eucalyptus_tree", () -> new net.krisplis.koppenclimate.worldgen.feature.EucalyptusTreeFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    // the monsoon subtropics' (Cwa) karst towers of white limestone
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> KARST_TOWER =
            FEATURES.register("karst_tower", () -> new net.krisplis.koppenclimate.worldgen.feature.KarstTowerFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    // the temperate oceanic band (Cfb): the forest's mud ponds, the countryside's hedgerows and its limestone tors
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> MUD_POND =
            FEATURES.register("mud_pond", () -> new net.krisplis.koppenclimate.worldgen.feature.MudPondFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<net.krisplis.koppenclimate.worldgen.feature.HedgerowFeature.Config>> HEDGEROWS =
            FEATURES.register("hedgerows", () -> new net.krisplis.koppenclimate.worldgen.feature.HedgerowFeature(
                    net.krisplis.koppenclimate.worldgen.feature.HedgerowFeature.Config.CODEC));
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> LIMESTONE_TOR =
            FEATURES.register("limestone_tor", () -> new net.krisplis.koppenclimate.worldgen.feature.LimestoneTorFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> RED_ROCK =
            FEATURES.register("red_rock", () -> new net.krisplis.koppenclimate.worldgen.feature.RedRockFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

    // the Redwood forest's (Csb) giant redwoods
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>> REDWOOD_TREE =
            FEATURES.register("redwood_tree", () -> new net.krisplis.koppenclimate.worldgen.feature.RedwoodTreeFeature(
                    net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));

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
    // the Australian desert's dead-flat plain (its ceiling and its floor)
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.AustralianPlain>> AUSTRALIAN_PLAIN =
            DENSITY_FUNCTION_TYPES.register("australian_plain", () -> net.krisplis.koppenclimate.worldgen.biome.AustralianPlain.MAP_CODEC);
    // the humid subtropics' (Cfa) dead-flat coastal plain (its ceiling and its floor)
    // smooths the terrain functions below between the corners of vanilla's 4 x 4 climate cells
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.Smooth2D>> SMOOTH_2D =
            DENSITY_FUNCTION_TYPES.register("smooth_2d", () -> net.krisplis.koppenclimate.worldgen.biome.Smooth2D.MAP_CODEC);
    // the monsoon subtropics' (Cwa) karst plain and rolling hills
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.CwaTerrain>> CWA_TERRAIN =
            DENSITY_FUNCTION_TYPES.register("cwa_terrain", () -> net.krisplis.koppenclimate.worldgen.biome.CwaTerrain.MAP_CODEC);
    // the Prairie's (Dfa) broad, gently rolling plain, which fills in its rivers (a floor only)
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.PrairiePlain>> PRAIRIE_PLAIN =
            DENSITY_FUNCTION_TYPES.register("prairie_plain", () -> net.krisplis.koppenclimate.worldgen.biome.PrairiePlain.MAP_CODEC);
    // the temperate oceanic band (Cfb): the countryside's upland, the white cliffs and the flooded rivers (ceiling and floor)
    // the Cfb's Flooded rivers: vanilla's lowland rivers, broadened (a ceiling)
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.CfbRiver>> CFB_RIVER =
            DENSITY_FUNCTION_TYPES.register("cfb_river", () -> net.krisplis.koppenclimate.worldgen.biome.CfbRiver.MAP_CODEC);
    // the Alps: long flat valleys high up between huge, sharp mountains (ceiling and floor)
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.AlpineTerrain>> ALPINE_TERRAIN =
            DENSITY_FUNCTION_TYPES.register("alpine", () -> net.krisplis.koppenclimate.worldgen.biome.AlpineTerrain.MAP_CODEC);
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.CfbTerrain>> CFB_TERRAIN =
            DENSITY_FUNCTION_TYPES.register("cfb_terrain", () -> net.krisplis.koppenclimate.worldgen.biome.CfbTerrain.MAP_CODEC);
    public static final RegistryObject<MapCodec<net.krisplis.koppenclimate.worldgen.biome.CfaPlain>> CFA_PLAIN =
            DENSITY_FUNCTION_TYPES.register("cfa_plain", () -> net.krisplis.koppenclimate.worldgen.biome.CfaPlain.MAP_CODEC);
    public static final RegistryObject<StructureType<VolcanoStructure>> VOLCANO =
            STRUCTURE_TYPES.register("volcanic_island", () -> () -> VolcanoStructure.CODEC);
    public static final RegistryObject<StructurePieceType> VOLCANO_PIECE =
            STRUCTURE_PIECES.register("volcano_crater", () -> (StructurePieceType.ContextlessType) VolcanoPiece::new);

    public static void register(IEventBus eventBus) {
        DENSITY_FUNCTION_TYPES.register(eventBus);
        STRUCTURE_PIECES.register(eventBus);
        STRUCTURE_TYPES.register(eventBus);
        FEATURES.register(eventBus);
        PLACEMENT_MODIFIERS.register(eventBus);
        BIOME_SOURCES.register(eventBus);
    }
}
