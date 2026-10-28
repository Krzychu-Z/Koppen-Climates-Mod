package net.krisplis.koppenclimate;

import com.mojang.logging.LogUtils;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.entity.ModBlockEntities;
import net.krisplis.koppenclimate.effect.ModEffects;
import net.krisplis.koppenclimate.effect.ModPotions;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.sound.ModSounds;
import net.krisplis.koppenclimate.village.VillageRegistry;
import net.krisplis.koppenclimate.worldgen.ModWorldgen;
import net.krisplis.koppenclimate.worldgen.tree.ModTreePlacers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(KoppenClimateMod.MOD_ID)
public class KoppenClimateMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "koppenclimatemod";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public KoppenClimateMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModTreePlacers.register(modEventBus);
        ModEntities.register(modEventBus);
        ModWorldgen.register(modEventBus);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModSounds.register(modEventBus);
        net.krisplis.koppenclimate.particle.ModParticles.register(modEventBus);
        VillageRegistry.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // the African savanna's baobab wood, ground, termite mound and termites
    private static void addSavannaCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            for (var b : java.util.List.of(ModBlocks.BAOBAB_LOG, ModBlocks.BAOBAB_WOOD, ModBlocks.STRIPPED_BAOBAB_LOG,
                    ModBlocks.STRIPPED_BAOBAB_WOOD, ModBlocks.BAOBAB_PLANKS, ModBlocks.BAOBAB_STAIRS, ModBlocks.BAOBAB_SLAB,
                    ModBlocks.BAOBAB_FENCE, ModBlocks.BAOBAB_FENCE_GATE)) event.accept(b);
            event.accept(ModItems.BAOBAB_DOOR);
            event.accept(ModBlocks.BAOBAB_TRAPDOOR);
            event.accept(ModBlocks.BAOBAB_PRESSURE_PLATE);
            event.accept(ModBlocks.BAOBAB_BUTTON);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.SAVANNA_OXISOL_GRASS);
            event.accept(ModBlocks.TERMITE_MOUND);
            event.accept(ModBlocks.BAOBAB_LOG);
            event.accept(ModBlocks.BAOBAB_LEAVES);
            event.accept(ModBlocks.BAOBAB_SAPLING);
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.BAOBAB_SIGN);
            event.accept(ModItems.BAOBAB_HANGING_SIGN);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.BAOBAB_BOAT);
            event.accept(ModItems.BAOBAB_CHEST_BOAT);
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            for (var egg : java.util.List.of(ModItems.TERMITE_SPAWN_EGG, ModItems.BLACK_MAMBA_SPAWN_EGG, ModItems.GIRAFFE_SPAWN_EGG,
                    ModItems.ELEPHANT_SPAWN_EGG, ModItems.RHINO_SPAWN_EGG, ModItems.LION_SPAWN_EGG, ModItems.ZEBRA_SPAWN_EGG,
                    ModItems.JACKAL_SPAWN_EGG, ModItems.WARM_PIG_SPAWN_EGG)) event.accept(egg);
        }
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.DRY_GRASS);
            event.accept(ModBlocks.TALL_DRY_GRASS);
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.IVORY_TUSK);
            event.accept(ModItems.FUR);
            event.accept(ModItems.RHINO_PLATE);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.RHINO_PLATE_HELMET);
            event.accept(ModItems.RHINO_PLATE_CHESTPLATE);
            event.accept(ModItems.RHINO_PLATE_LEGGINGS);
            event.accept(ModItems.RHINO_PLATE_BOOTS);
        }
    }

    // the Mediterranean (Csa): white limestone, terra rossa, white gravel, rosemary, olive, Italian pine and cypress
    private static void addMediterraneanCreative(BuildCreativeModeTabContentsEvent event) {
        var tab = event.getTabKey();
        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            for (String wood : new String[]{"olive", "italian_pine", "fig"}) {
                for (String part : new String[]{"%s_log", "%s_wood", "stripped_%s_log", "stripped_%s_wood", "%s_planks", "%s_stairs",
                        "%s_slab", "%s_fence", "%s_fence_gate", "%s_door", "%s_trapdoor", "%s_pressure_plate", "%s_button"}) {
                    acceptById(event, String.format(part, wood));
                }
            }
            for (String stone : new String[]{"white_limestone", "white_limestone_stairs", "white_limestone_slab",
                    "white_limestone_pressure_plate", "white_limestone_button", "cobbled_white_limestone", "cobbled_white_limestone_stairs",
                    "cobbled_white_limestone_slab", "cobbled_white_limestone_wall", "mossy_cobbled_white_limestone",
                    "mossy_cobbled_white_limestone_stairs", "mossy_cobbled_white_limestone_slab", "mossy_cobbled_white_limestone_wall",
                    "smooth_white_limestone", "smooth_white_limestone_slab", "cut_white_limestone", "cut_white_limestone_slab",
                    "white_limestone_bricks", "white_limestone_brick_stairs", "white_limestone_brick_slab", "white_limestone_brick_wall",
                    "cracked_white_limestone_bricks", "chiseled_white_limestone_bricks", "mossy_white_limestone_bricks",
                    "mossy_white_limestone_brick_stairs", "mossy_white_limestone_brick_slab", "mossy_white_limestone_brick_wall"}) {
                acceptById(event, stone);
            }
            for (String marble : new String[]{"marble", "marble_stairs", "marble_slab", "marble_wall", "polished_marble",
                    "polished_marble_stairs", "polished_marble_slab", "polished_marble_wall", "marble_bricks", "marble_brick_stairs",
                    "marble_brick_slab", "marble_brick_wall", "cracked_marble_bricks", "mossy_marble_bricks", "mossy_marble_brick_stairs",
                    "mossy_marble_brick_slab", "mossy_marble_brick_wall", "marble_tiles", "marble_tile_stairs", "marble_tile_slab",
                    "marble_tile_wall", "chiseled_marble", "marble_pillar", "fluted_marble_column", "marble_column_base",
                    "marble_column_capital"}) {
                acceptById(event, marble);
            }
        }
        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.TERRACOTTA_ROOF_TILES);
        }
        if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.ORANGE_SAND);
            event.accept(ModBlocks.ORANGE_SAND_DUNE);
            event.accept(ModBlocks.ORANGE_SANDSTONE);
            event.accept(ModBlocks.SILVER_GRASS);
            for (var b : java.util.List.of(ModBlocks.WHITE_LIMESTONE, ModBlocks.CINNAMON_GRASS_BLOCK, ModBlocks.CINNAMON_DIRT,
                    ModBlocks.WHITE_GRAVEL, ModBlocks.ROSEMARY, ModBlocks.TALL_ROSEMARY, ModBlocks.OLIVE_LOG, ModBlocks.OLIVE_LEAVES,
                    ModBlocks.OLIVE_SAPLING, ModBlocks.ITALIAN_PINE_LOG, ModBlocks.ITALIAN_PINE_LEAVES, ModBlocks.ITALIAN_PINE_SAPLING,
                    ModBlocks.CYPRESS_LEAVES, ModBlocks.CYPRESS_SAPLING, ModBlocks.FIG_LOG, ModBlocks.FIG_LEAVES, ModBlocks.FIG_SAPLING,
                    ModBlocks.LEMON_LEAVES, ModBlocks.LEMON_SAPLING, ModBlocks.LAVENDER, ModBlocks.AGAVE, ModBlocks.NERIUM,
                    ModBlocks.MARBLE)) event.accept(b);
            event.accept(ModBlocks.WINE_POLE);
            event.accept(ModItems.BARLEY_SEEDS);
        }
        if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            for (var i : java.util.List.of(ModItems.OLIVE_SIGN, ModItems.OLIVE_HANGING_SIGN, ModItems.ITALIAN_PINE_SIGN,
                    ModItems.ITALIAN_PINE_HANGING_SIGN, ModItems.FIG_SIGN, ModItems.FIG_HANGING_SIGN)) event.accept(i);
        }
        if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (var i : java.util.List.of(ModItems.OLIVE_BOAT, ModItems.OLIVE_CHEST_BOAT, ModItems.ITALIAN_PINE_BOAT,
                    ModItems.ITALIAN_PINE_CHEST_BOAT, ModItems.FIG_BOAT, ModItems.FIG_CHEST_BOAT)) event.accept(i);
        }
        if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.OLIVE);
            event.accept(ModItems.OLIVE_OIL);
            event.accept(ModItems.FIG);
            event.accept(ModItems.LEMON);
            event.accept(ModItems.RED_WINE_GRAPES);
            event.accept(ModItems.GREEN_WINE_GRAPES);
            event.accept(ModItems.TOMATO);
        }
        if (tab == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.ROSEMARY_SPRIG);
            event.accept(ModItems.ROSEMARY_OIL);
            event.accept(ModItems.BARLEY);
            event.accept(ModItems.AGAVE_LEAVES);
            event.accept(ModItems.AGAVE_MASH);
            event.accept(ModItems.NERIUM_TWIGS);
        }
        if (tab == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.SCORPION_SPAWN_EGG);
        }
    }

    private static void acceptById(BuildCreativeModeTabContentsEvent event, String name) {
        var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, name));
        if (item != null && item != net.minecraft.world.item.Items.AIR) event.accept(item);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // These vanilla maps aren't thread safe, so fill them on the main thread
        event.enqueueWork(() -> {
            FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
            pot.addPlant(ModBlocks.COCONUT_PALM_SAPLING.getId(), ModBlocks.POTTED_COCONUT_PALM_SAPLING);
            pot.addPlant(ModBlocks.MAHOGANY_SAPLING.getId(), ModBlocks.POTTED_MAHOGANY_SAPLING);
            pot.addPlant(ModBlocks.BANANA_SAPLING.getId(), ModBlocks.POTTED_BANANA_SAPLING);
            pot.addPlant(ModBlocks.BAOBAB_SAPLING.getId(), ModBlocks.POTTED_BAOBAB_SAPLING);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.BAOBAB_SAPLING.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.BAOBAB_LEAVES.get().asItem(), 0.3F);

            ComposterBlock.COMPOSTABLES.put(ModBlocks.BANANA_SAPLING.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.BANANA_LEAVES.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModItems.BANANA.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.RAFFLESIA.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModItems.COCONUT.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModItems.RICE_SEEDS.get(), 0.3F);
            net.krisplis.koppenclimate.village.ThaiVillagers.registerBiomeTypes();
            ComposterBlock.COMPOSTABLES.put(ModItems.RICE.get(), 0.65F);

            ComposterBlock.COMPOSTABLES.put(ModBlocks.COCONUT_PALM_SAPLING.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.COCONUT_PALM_LEAVES.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.MAHOGANY_SAPLING.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.MAHOGANY_LEAVES.get().asItem(), 0.3F);

            // Same burn odds as vanilla wood and leaves: (block, encouragement, flammability)
            FireBlock fire = (FireBlock) Blocks.FIRE;
            fire.setFlammable(ModBlocks.COCONUT_PALM_LOG.get(), 5, 5);
            fire.setFlammable(ModBlocks.COCONUT_PALM_PLANKS.get(), 5, 20);
            fire.setFlammable(ModBlocks.COCONUT_PALM_LEAVES.get(), 30, 60);
            fire.setFlammable(ModBlocks.MAHOGANY_LOG.get(), 5, 5);
            fire.setFlammable(ModBlocks.MAHOGANY_WOOD.get(), 5, 5);
            fire.setFlammable(ModBlocks.MAHOGANY_PLANKS.get(), 5, 20);
            fire.setFlammable(ModBlocks.MAHOGANY_LEAVES.get(), 30, 60);
            fire.setFlammable(ModBlocks.BANANA_STEM.get(), 5, 5);
            for (var log : java.util.List.of(ModBlocks.STRIPPED_MAHOGANY_LOG, ModBlocks.STRIPPED_MAHOGANY_WOOD,
                    ModBlocks.COCONUT_PALM_WOOD, ModBlocks.STRIPPED_COCONUT_PALM_LOG, ModBlocks.STRIPPED_COCONUT_PALM_WOOD)) {
                fire.setFlammable(log.get(), 5, 5);
            }
            for (var wood : java.util.List.of(ModBlocks.MAHOGANY_STAIRS, ModBlocks.MAHOGANY_SLAB, ModBlocks.MAHOGANY_FENCE,
                    ModBlocks.MAHOGANY_FENCE_GATE, ModBlocks.COCONUT_PALM_STAIRS, ModBlocks.COCONUT_PALM_SLAB,
                    ModBlocks.COCONUT_PALM_FENCE, ModBlocks.COCONUT_PALM_FENCE_GATE)) {
                fire.setFlammable(wood.get(), 5, 20);
            }
            fire.setFlammable(ModBlocks.BANANA_LEAVES.get(), 30, 60);
            for (var log : java.util.List.of(ModBlocks.BAOBAB_LOG, ModBlocks.BAOBAB_WOOD, ModBlocks.STRIPPED_BAOBAB_LOG,
                    ModBlocks.STRIPPED_BAOBAB_WOOD)) {
                fire.setFlammable(log.get(), 5, 5);
            }
            for (var wood : java.util.List.of(ModBlocks.BAOBAB_PLANKS, ModBlocks.BAOBAB_STAIRS, ModBlocks.BAOBAB_SLAB,
                    ModBlocks.BAOBAB_FENCE, ModBlocks.BAOBAB_FENCE_GATE)) {
                fire.setFlammable(wood.get(), 5, 20);
            }
            fire.setFlammable(ModBlocks.BAOBAB_LEAVES.get(), 30, 60);
            fire.setFlammable(ModBlocks.DRY_GRASS.get(), 60, 100);
            fire.setFlammable(ModBlocks.TALL_DRY_GRASS.get(), 60, 100);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.DRY_GRASS.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.TALL_DRY_GRASS.get().asItem(), 0.5F);

            // the Mediterranean's trees and plants
            pot.addPlant(ModBlocks.OLIVE_SAPLING.getId(), ModBlocks.POTTED_OLIVE_SAPLING);
            pot.addPlant(ModBlocks.ITALIAN_PINE_SAPLING.getId(), ModBlocks.POTTED_ITALIAN_PINE_SAPLING);
            pot.addPlant(ModBlocks.CYPRESS_SAPLING.getId(), ModBlocks.POTTED_CYPRESS_SAPLING);
            for (var b : java.util.List.of(ModBlocks.OLIVE_SAPLING, ModBlocks.OLIVE_LEAVES, ModBlocks.ITALIAN_PINE_SAPLING,
                    ModBlocks.ITALIAN_PINE_LEAVES, ModBlocks.CYPRESS_SAPLING, ModBlocks.CYPRESS_LEAVES, ModBlocks.ROSEMARY)) {
                ComposterBlock.COMPOSTABLES.put(b.get().asItem(), 0.3F);
            }
            ComposterBlock.COMPOSTABLES.put(ModBlocks.TALL_ROSEMARY.get().asItem(), 0.5F);
            ComposterBlock.COMPOSTABLES.put(ModItems.OLIVE.get(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModItems.ROSEMARY_SPRIG.get(), 0.3F);
            // the resinous Mediterranean trees and scrub catch fire extremely easily
            for (var log : java.util.List.of(ModBlocks.OLIVE_LOG, ModBlocks.OLIVE_WOOD, ModBlocks.STRIPPED_OLIVE_LOG, ModBlocks.STRIPPED_OLIVE_WOOD,
                    ModBlocks.ITALIAN_PINE_LOG, ModBlocks.ITALIAN_PINE_WOOD, ModBlocks.STRIPPED_ITALIAN_PINE_LOG, ModBlocks.STRIPPED_ITALIAN_PINE_WOOD)) {
                fire.setFlammable(log.get(), 30, 60);
            }
            for (var wood : java.util.List.of(ModBlocks.OLIVE_PLANKS, ModBlocks.OLIVE_STAIRS, ModBlocks.OLIVE_SLAB, ModBlocks.OLIVE_FENCE,
                    ModBlocks.OLIVE_FENCE_GATE, ModBlocks.ITALIAN_PINE_PLANKS, ModBlocks.ITALIAN_PINE_STAIRS, ModBlocks.ITALIAN_PINE_SLAB,
                    ModBlocks.ITALIAN_PINE_FENCE, ModBlocks.ITALIAN_PINE_FENCE_GATE)) {
                fire.setFlammable(wood.get(), 5, 20);
            }
            for (var leaves : java.util.List.of(ModBlocks.OLIVE_LEAVES, ModBlocks.CYPRESS_LEAVES)) {
                fire.setFlammable(leaves.get(), 60, 100);
            }
            // the resin-soaked stone pine goes up like a torch: a single flame can take a whole pine forest
            fire.setFlammable(ModBlocks.ITALIAN_PINE_LEAVES.get(), 150, 300);
            for (var log : java.util.List.of(ModBlocks.ITALIAN_PINE_LOG, ModBlocks.ITALIAN_PINE_WOOD,
                    ModBlocks.STRIPPED_ITALIAN_PINE_LOG, ModBlocks.STRIPPED_ITALIAN_PINE_WOOD)) {
                fire.setFlammable(log.get(), 100, 150);
            }
            fire.setFlammable(ModBlocks.FIG_LEAVES.get(), 30, 60);
            for (var log : java.util.List.of(ModBlocks.FIG_LOG, ModBlocks.FIG_WOOD, ModBlocks.STRIPPED_FIG_LOG, ModBlocks.STRIPPED_FIG_WOOD)) {
                fire.setFlammable(log.get(), 5, 5);
            }
            for (var wood : java.util.List.of(ModBlocks.FIG_PLANKS, ModBlocks.FIG_STAIRS, ModBlocks.FIG_SLAB, ModBlocks.FIG_FENCE,
                    ModBlocks.FIG_FENCE_GATE)) {
                fire.setFlammable(wood.get(), 5, 20);
            }
            fire.setFlammable(ModBlocks.LEMON_LEAVES.get(), 30, 60);
            fire.setFlammable(ModBlocks.ROSEMARY.get(), 100, 100);
            fire.setFlammable(ModBlocks.TALL_ROSEMARY.get(), 100, 100);
            fire.setFlammable(ModBlocks.LAVENDER.get(), 60, 100);
            pot.addPlant(ModBlocks.FIG_SAPLING.getId(), ModBlocks.POTTED_FIG_SAPLING);
            pot.addPlant(ModBlocks.LEMON_SAPLING.getId(), ModBlocks.POTTED_LEMON_SAPLING);
            pot.addPlant(ModBlocks.LAVENDER.getId(), ModBlocks.POTTED_LAVENDER);
            pot.addPlant(ModBlocks.NERIUM.getId(), ModBlocks.POTTED_NERIUM);
            for (var b : java.util.List.of(ModBlocks.FIG_SAPLING, ModBlocks.FIG_LEAVES, ModBlocks.LEMON_SAPLING, ModBlocks.LEMON_LEAVES)) {
                ComposterBlock.COMPOSTABLES.put(b.get().asItem(), 0.3F);
            }
            ComposterBlock.COMPOSTABLES.put(ModBlocks.LAVENDER.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModItems.FIG.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModItems.LEMON.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModItems.RED_WINE_GRAPES.get(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModItems.GREEN_WINE_GRAPES.get(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModItems.AGAVE_LEAVES.get(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModItems.BARLEY_SEEDS.get(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModItems.BARLEY.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.AGAVE.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.NERIUM.get().asItem(), 0.65F);
            fire.setFlammable(ModBlocks.NERIUM.get(), 30, 60);
            fire.setFlammable(ModBlocks.AGAVE_STALK.get(), 5, 20);
            fire.setFlammable(ModBlocks.WINE_POLE.get(), 5, 20);
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        addSavannaCreative(event);
        addMediterraneanCreative(event);
        if(event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.OXISOL);
            event.accept(ModBlocks.OXISOL_GRASS);
            event.accept(ModBlocks.WHITE_SAND);
            // mahogany, then coconut palm, in vanilla's order for a wood
            event.accept(ModBlocks.MAHOGANY_LOG);
            event.accept(ModBlocks.MAHOGANY_WOOD);
            event.accept(ModBlocks.STRIPPED_MAHOGANY_LOG);
            event.accept(ModBlocks.STRIPPED_MAHOGANY_WOOD);
            event.accept(ModBlocks.MAHOGANY_PLANKS);
            event.accept(ModBlocks.MAHOGANY_STAIRS);
            event.accept(ModBlocks.MAHOGANY_SLAB);
            event.accept(ModBlocks.MAHOGANY_FENCE);
            event.accept(ModBlocks.MAHOGANY_FENCE_GATE);
            event.accept(ModItems.MAHOGANY_DOOR);
            event.accept(ModBlocks.MAHOGANY_TRAPDOOR);
            event.accept(ModBlocks.MAHOGANY_PRESSURE_PLATE);
            event.accept(ModBlocks.MAHOGANY_BUTTON);
            event.accept(ModBlocks.COCONUT_PALM_LOG);
            event.accept(ModBlocks.COCONUT_PALM_WOOD);
            event.accept(ModBlocks.STRIPPED_COCONUT_PALM_LOG);
            event.accept(ModBlocks.STRIPPED_COCONUT_PALM_WOOD);
            event.accept(ModBlocks.COCONUT_PALM_PLANKS);
            event.accept(ModBlocks.COCONUT_PALM_STAIRS);
            event.accept(ModBlocks.COCONUT_PALM_SLAB);
            event.accept(ModBlocks.COCONUT_PALM_FENCE);
            event.accept(ModBlocks.COCONUT_PALM_FENCE_GATE);
            event.accept(ModItems.COCONUT_PALM_DOOR);
            event.accept(ModBlocks.COCONUT_PALM_TRAPDOOR);
            event.accept(ModBlocks.COCONUT_PALM_PRESSURE_PLATE);
            event.accept(ModBlocks.COCONUT_PALM_BUTTON);
            // yellow limestone, in vanilla's order for stone
            event.accept(ModBlocks.YELLOW_LIMESTONE);
            event.accept(ModBlocks.YELLOW_LIMESTONE_STAIRS);
            event.accept(ModBlocks.YELLOW_LIMESTONE_SLAB);
            event.accept(ModBlocks.YELLOW_LIMESTONE_PRESSURE_PLATE);
            event.accept(ModBlocks.YELLOW_LIMESTONE_BUTTON);
            event.accept(ModBlocks.COBBLED_YELLOW_LIMESTONE);
            event.accept(ModBlocks.COBBLED_YELLOW_LIMESTONE_STAIRS);
            event.accept(ModBlocks.COBBLED_YELLOW_LIMESTONE_SLAB);
            event.accept(ModBlocks.COBBLED_YELLOW_LIMESTONE_WALL);
            event.accept(ModBlocks.MOSSY_COBBLED_YELLOW_LIMESTONE);
            event.accept(ModBlocks.MOSSY_COBBLED_YELLOW_LIMESTONE_STAIRS);
            event.accept(ModBlocks.MOSSY_COBBLED_YELLOW_LIMESTONE_SLAB);
            event.accept(ModBlocks.MOSSY_COBBLED_YELLOW_LIMESTONE_WALL);
            event.accept(ModBlocks.SMOOTH_YELLOW_LIMESTONE);
            event.accept(ModBlocks.SMOOTH_YELLOW_LIMESTONE_SLAB);
            event.accept(ModBlocks.YELLOW_LIMESTONE_BRICKS);
            event.accept(ModBlocks.CRACKED_YELLOW_LIMESTONE_BRICKS);
            event.accept(ModBlocks.YELLOW_LIMESTONE_BRICK_STAIRS);
            event.accept(ModBlocks.YELLOW_LIMESTONE_BRICK_SLAB);
            event.accept(ModBlocks.YELLOW_LIMESTONE_BRICK_WALL);
            event.accept(ModBlocks.CHISELED_YELLOW_LIMESTONE_BRICKS);
            event.accept(ModBlocks.MOSSY_YELLOW_LIMESTONE_BRICKS);
            event.accept(ModBlocks.MOSSY_YELLOW_LIMESTONE_BRICK_STAIRS);
            event.accept(ModBlocks.MOSSY_YELLOW_LIMESTONE_BRICK_SLAB);
            event.accept(ModBlocks.MOSSY_YELLOW_LIMESTONE_BRICK_WALL);
            event.accept(ModBlocks.CUT_YELLOW_LIMESTONE);
            event.accept(ModBlocks.CUT_YELLOW_LIMESTONE_SLAB);
        }
        if(event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModBlocks.WHITE_SAND);
            event.accept(ModBlocks.COCONUT_PALM_LOG);
            event.accept(ModBlocks.COCONUT_PALM_LEAVES);
            event.accept(ModBlocks.COCONUT_PALM_SAPLING);
            event.accept(ModBlocks.MAHOGANY_LOG);
            event.accept(ModBlocks.MAHOGANY_LEAVES);
            event.accept(ModBlocks.MAHOGANY_SAPLING);
            event.accept(ModBlocks.BANANA_STEM);
            event.accept(ModBlocks.BANANA_LEAVES);
            event.accept(ModBlocks.BANANA_SAPLING);
            event.accept(ModBlocks.RAFFLESIA);
            event.accept(ModItems.RICE_SEEDS);
            event.accept(ModBlocks.YELLOW_LIMESTONE);
            event.accept(ModBlocks.JADE_ORE);
            event.accept(ModBlocks.DEEPSLATE_JADE_ORE);
        }
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.JADE);
        }
        if(event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.JADE_RING);
        }
        if(event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.MAHOGANY_SIGN);
            event.accept(ModItems.MAHOGANY_HANGING_SIGN);
            event.accept(ModItems.COCONUT_PALM_SIGN);
            event.accept(ModItems.COCONUT_PALM_HANGING_SIGN);
        }
        if(event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(ModBlocks.MAHOGANY_BUTTON);
            event.accept(ModBlocks.MAHOGANY_PRESSURE_PLATE);
            event.accept(ModItems.MAHOGANY_DOOR);
            event.accept(ModBlocks.MAHOGANY_TRAPDOOR);
            event.accept(ModBlocks.MAHOGANY_FENCE_GATE);
            event.accept(ModBlocks.COCONUT_PALM_BUTTON);
            event.accept(ModBlocks.COCONUT_PALM_PRESSURE_PLATE);
            event.accept(ModItems.COCONUT_PALM_DOOR);
            event.accept(ModBlocks.COCONUT_PALM_TRAPDOOR);
            event.accept(ModBlocks.COCONUT_PALM_FENCE_GATE);
            event.accept(ModBlocks.YELLOW_LIMESTONE_BUTTON);
            event.accept(ModBlocks.YELLOW_LIMESTONE_PRESSURE_PLATE);
        }
        if(event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.BANANA);
            event.accept(ModItems.COCONUT);
            event.accept(ModItems.BOWL_OF_RICE);
        }
        if(event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.PIRANHA_BUCKET);
            event.accept(ModItems.SLEEPING_MAT);
            event.accept(ModItems.MAHOGANY_BOAT);
            event.accept(ModItems.MAHOGANY_CHEST_BOAT);
            event.accept(ModItems.COCONUT_PALM_BOAT);
            event.accept(ModItems.COCONUT_PALM_CHEST_BOAT);
        }
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RAFFLESIA_PETAL);
            event.accept(ModItems.RICE_SEEDS);
            event.accept(ModItems.RICE);
        }
        if(event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.ANACONDA_SPAWN_EGG);
            event.accept(ModItems.CROCODILE_SPAWN_EGG);
            event.accept(ModItems.BLACK_PANTHER_SPAWN_EGG);
            event.accept(VillageRegistry.AMAZON_VILLAGER_SPAWN_EGG);
            event.accept(VillageRegistry.AFRICAN_VILLAGER_SPAWN_EGG);
            event.accept(ModItems.PIRANHA_SPAWN_EGG);
            event.accept(ModItems.COBRA_SPAWN_EGG);
            event.accept(ModItems.VIPER_SPAWN_EGG);
            event.accept(ModItems.SHARK_SPAWN_EGG);
            event.accept(ModItems.CRAB_SPAWN_EGG);
            event.accept(ModItems.EAGLE_SPAWN_EGG);
            event.accept(ModItems.RATTLESNAKE_SPAWN_EGG);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // lists this mod's structures in the log, so it's easy to check they loaded (run/logs/latest.log)
        var structures = event.getServer().registryAccess().registryOrThrow(Registries.STRUCTURE).keySet().stream()
                .filter(id -> id.getNamespace().equals(MOD_ID)).map(Object::toString).sorted().toList();
        LOGGER.info("Koppen Climate structures loaded: {}", structures);
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
