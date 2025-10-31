package net.krisplis.koppenclimate.client;

import net.minecraft.world.level.GrassColor;
import net.minecraft.client.renderer.BiomeColors;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
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
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item e) {
        e.getItemColors().register((stack, tint) -> tint == 0
                ? GrassColor.get(0.5D, 1.0D) : -1, ModBlocks.OXISOL_GRASS.get());
    }
}
