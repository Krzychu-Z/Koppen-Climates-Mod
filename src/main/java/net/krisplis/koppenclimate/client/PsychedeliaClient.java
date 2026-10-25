package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.effect.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// While the player has Psychedelia, show the psychedelic screen shader (shaders/post/psychedelia.json):
// colours cycle through the rainbow and the world ripples. It's switched off when the effect ends.
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PsychedeliaClient {
    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "shaders/post/psychedelia.json");

    // if the shader can't load (Minecraft logs why), don't retry every tick until the effect wears off
    private static boolean failed;

    private PsychedeliaClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameRenderer == null) return;

        boolean tripping = ModEffects.PSYCHEDELIA.getHolder().map(mc.player::hasEffect).orElse(false);
        PostChain current = mc.gameRenderer.currentEffect();
        boolean showing = current != null && SHADER.toString().equals(current.getName());
        // checked every tick, since switching camera view resets the screen effect
        if (tripping && !showing && !failed) {
            mc.gameRenderer.loadEffect(SHADER);
            PostChain loaded = mc.gameRenderer.currentEffect();
            failed = loaded == null || !SHADER.toString().equals(loaded.getName());
        } else if (!tripping) {
            failed = false;
            if (showing) mc.gameRenderer.shutdownEffect();
        }
    }
}
