package net.krisplis.koppenclimate.client;

import net.minecraft.world.level.GrassColor;
import net.minecraft.client.renderer.BiomeColors;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.ModWoodTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.krisplis.koppenclimate.block.entity.ModBlockEntities;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.village.AmazonVillagerRenderer;
import net.krisplis.koppenclimate.village.VillageRegistry;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    @SubscribeEvent
    public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block e) {
        e.getBlockColors().register((state, level, pos, tint) -> {
            if (tint == 0) {
                return (level != null && pos != null)
                        ? BiomeColors.getAverageGrassColor(level, pos)
                        : GrassColor.get(0.5D, 1.0D);
            }
            return -1; // don't tint sides or bottom
        }, ModBlocks.OXISOL_GRASS.get());
        // the Lush desert's dry bush: all of it in the grass's colour
        e.getBlockColors().register((state, level, pos, tint) -> level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.get(0.8D, 0.2D), ModBlocks.DRY_BUSH.get());
    }



    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent e) {
        // the sign renderers look up each wood type's sign textures here
        e.enqueueWork(() -> {
            Sheets.addWoodType(ModWoodTypes.MAHOGANY);
            Sheets.addWoodType(ModWoodTypes.COCONUT_PALM);
            Sheets.addWoodType(ModWoodTypes.BAOBAB);
            Sheets.addWoodType(ModWoodTypes.OLIVE);
            Sheets.addWoodType(ModWoodTypes.ITALIAN_PINE);
            Sheets.addWoodType(ModWoodTypes.FIG);
            Sheets.addWoodType(ModWoodTypes.JOSHUA);
            Sheets.addWoodType(ModWoodTypes.DEAD_OAK);
        });
    }

    // mosquitoes over the Amazon rivers and the rice terraces
    @SubscribeEvent
    public static void onRegisterParticles(net.minecraftforge.client.event.RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(net.krisplis.koppenclimate.particle.ModParticles.MOSQUITO.get(), MosquitoParticle.Provider::new);
        e.registerSpriteSet(net.krisplis.koppenclimate.particle.ModParticles.GLOWWORM.get(), GlowwormParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(AnacondaModel.LAYER, AnacondaModel::createBodyLayer);
        e.registerLayerDefinition(CrocodileModel.LAYER, CrocodileModel::createBodyLayer);
        e.registerLayerDefinition(PiranhaModel.LAYER, PiranhaModel::createBodyLayer);
        e.registerLayerDefinition(CobraModel.LAYER, CobraModel::createBodyLayer);
        e.registerLayerDefinition(TermiteModel.LAYER, TermiteModel::createBodyLayer);
        e.registerLayerDefinition(ScorpionModel.LAYER, ScorpionModel::createBodyLayer);
        e.registerLayerDefinition(SharkModel.LAYER, SharkModel::createBodyLayer);
        e.registerLayerDefinition(CrabModel.LAYER, CrabModel::createBodyLayer);
        e.registerLayerDefinition(EagleModel.LAYER, EagleModel::createBodyLayer);
        e.registerLayerDefinition(CowboySoulModel.LAYER, CowboySoulModel::createBodyLayer);
        e.registerLayerDefinition(LionModel.LAYER, LionModel::createBodyLayer);
        e.registerLayerDefinition(ElephantModel.LAYER, ElephantModel::createBodyLayer);
        e.registerLayerDefinition(RhinoModel.LAYER, RhinoModel::createBodyLayer);
        e.registerLayerDefinition(GiraffeModel.LAYER, GiraffeModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(ModEntities.ANACONDA.get(), AnacondaRenderer::new);
        e.registerEntityRenderer(ModEntities.CROCODILE.get(), CrocodileRenderer::new);
        e.registerEntityRenderer(ModEntities.BLACK_PANTHER.get(), BlackPantherRenderer::new);
        e.registerEntityRenderer(ModEntities.PIRANHA.get(), PiranhaRenderer::new);
        e.registerEntityRenderer(ModEntities.COBRA.get(), CobraRenderer::new);
        e.registerEntityRenderer(ModEntities.VIPER.get(), SavannaRenderers::viper);
        e.registerEntityRenderer(ModEntities.TERMITE.get(), TermiteRenderer::new);
        e.registerEntityRenderer(ModEntities.SCORPION.get(), ScorpionRenderer::new);
        e.registerEntityRenderer(ModEntities.SHARK.get(), SharkRenderer::new);
        e.registerEntityRenderer(ModEntities.CRAB.get(), CrabRenderer::new);
        e.registerEntityRenderer(ModEntities.EAGLE.get(), EagleRenderer::new);
        e.registerEntityRenderer(ModEntities.COWBOY_SOUL.get(), CowboySoulRenderer::new);
        e.registerEntityRenderer(ModEntities.RATTLESNAKE.get(), SavannaRenderers::rattlesnake);
        e.registerEntityRenderer(ModEntities.BLACK_MAMBA.get(), SavannaRenderers::mamba);
        e.registerEntityRenderer(ModEntities.LION.get(), SavannaRenderers::lion);
        e.registerEntityRenderer(ModEntities.ELEPHANT.get(), SavannaRenderers::elephant);
        e.registerEntityRenderer(ModEntities.RHINO.get(), SavannaRenderers::rhino);
        e.registerEntityRenderer(ModEntities.GIRAFFE.get(), SavannaRenderers::giraffe);
        e.registerEntityRenderer(ModEntities.JACKAL.get(), SavannaRenderers::jackal);
        e.registerEntityRenderer(ModEntities.ZEBRA.get(), SavannaRenderers.Zebra::new);
        e.registerEntityRenderer(ModEntities.WARM_PIG.get(), ctx -> new net.minecraft.client.renderer.entity.PigRenderer(ctx) {
            private static final net.minecraft.resources.ResourceLocation TEXTURE =
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "textures/entity/warm_pig.png");

            @Override
            public net.minecraft.resources.ResourceLocation getTextureLocation(net.minecraft.world.entity.animal.Pig pig) {
                return TEXTURE;
            }
        });
        e.registerEntityRenderer(VillageRegistry.AMAZON_VILLAGER.get(), AmazonVillagerRenderer::new);
        e.registerEntityRenderer(VillageRegistry.AFRICAN_VILLAGER.get(), net.krisplis.koppenclimate.village.AfricanVillagerRenderer::new);
        e.registerEntityRenderer(ModEntities.BOAT.get(), context -> new ModBoatRenderer(context, false));
        e.registerEntityRenderer(ModEntities.CHEST_BOAT.get(), context -> new ModBoatRenderer(context, true));
        e.registerBlockEntityRenderer(ModBlockEntities.SIGN.get(), SignRenderer::new);
        e.registerBlockEntityRenderer(ModBlockEntities.HANGING_SIGN.get(), HangingSignRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item e) {
        e.getItemColors().register((stack, tint) -> tint == 0
                ? GrassColor.get(0.5D, 1.0D) : -1, ModBlocks.OXISOL_GRASS.get());
        e.getItemColors().register((stack, tint) -> tint == 0 ? 0xA9A86A : -1, ModBlocks.DRY_BUSH.get());

    }
}
