package net.krisplis.koppenclimate;

import com.mojang.logging.LogUtils;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.effect.ModEffects;
import net.krisplis.koppenclimate.effect.ModPotions;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.sound.ModSounds;
import net.krisplis.koppenclimate.worldgen.ModWorldgen;
import net.krisplis.koppenclimate.worldgen.tree.ModTreePlacers;
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
        ModItems.register(modEventBus);
        ModTreePlacers.register(modEventBus);
        ModEntities.register(modEventBus);
        ModWorldgen.register(modEventBus);
        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);
        ModSounds.register(modEventBus);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // These vanilla maps aren't thread safe, so fill them on the main thread
        event.enqueueWork(() -> {
            FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
            pot.addPlant(ModBlocks.COCONUT_PALM_SAPLING.getId(), ModBlocks.POTTED_COCONUT_PALM_SAPLING);
            pot.addPlant(ModBlocks.MAHOGANY_SAPLING.getId(), ModBlocks.POTTED_MAHOGANY_SAPLING);
            pot.addPlant(ModBlocks.BANANA_SAPLING.getId(), ModBlocks.POTTED_BANANA_SAPLING);

            ComposterBlock.COMPOSTABLES.put(ModBlocks.BANANA_SAPLING.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.BANANA_LEAVES.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(ModItems.BANANA.get(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModBlocks.RAFFLESIA.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(ModItems.COCONUT.get(), 0.65F);

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
            fire.setFlammable(ModBlocks.BANANA_LEAVES.get(), 30, 60);
        });
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if(event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.OXISOL);
            event.accept(ModBlocks.OXISOL_GRASS);
            event.accept(ModBlocks.WHITE_SAND);
            event.accept(ModBlocks.YELLOW_LIMESTONE);
            event.accept(ModBlocks.COCONUT_PALM_LOG);
            event.accept(ModBlocks.COCONUT_PALM_PLANKS);
            event.accept(ModBlocks.MAHOGANY_LOG);
            event.accept(ModBlocks.MAHOGANY_WOOD);
            event.accept(ModBlocks.MAHOGANY_PLANKS);
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
            event.accept(ModBlocks.YELLOW_LIMESTONE);
        }
        if(event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.BANANA);
            event.accept(ModItems.COCONUT);
        }
        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RAFFLESIA_PETAL);
        }
        if(event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.ANACONDA_SPAWN_EGG);
            event.accept(ModItems.CROCODILE_SPAWN_EGG);
            event.accept(ModItems.BLACK_PANTHER_SPAWN_EGG);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}
