package net.krisplis.koppenclimate.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * The aurora borealis, over the far north at night: the Subpolar taiga (Dfc), the Tundra (ET) and the Ice cap (EF).
 * Long curtains of light hanging high in the northern sky - green at their foot, fading through teal into violet and
 * nothing at their top - rippling and drifting slowly, brightening and dimming. Drawn just after the sky (so the
 * stars shine through, and the land and clouds pass in front of it), added onto the night's dark; faded in and out over
 * a few seconds as you enter or leave the north, and as night falls or day breaks; hidden by rain or snow.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Aurora {
    // the curtains: how many, how far round the sky each runs (radians, about north), how far off and how high
    private static final int CURTAINS = 3, SEGMENTS = 64;
    private static final float DISTANCE = 100.0F, BASE = 26.0F, HEIGHT = 34.0F;
    private static float fade, fadeO;

    private Aurora() {
    }

    static boolean polar(Holder<Biome> biome) {
        return biome.is(ModBiomes.SUBPOLAR_TAIGA) || biome.is(ModBiomes.TUNDRA) || biome.is(ModBiomes.ICE_CAP);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        fadeO = fade;
        boolean north = mc.player != null && mc.level != null && polar(mc.level.getBiome(mc.player.blockPosition()));
        fade = north ? Math.min(1.0F, fade + 0.01F) : Math.max(0.0F, fade - 0.01F);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        float partial = event.getPartialTick();
        float strength = Mth.lerp(partial, fadeO, fade);
        if (strength <= 0.0F) return;
        // only by night (it shows as the stars come out), and not through rain or snow
        float night = Mth.clamp(level.getStarBrightness(partial) * 2.2F, 0.0F, 1.0F) * (1.0F - level.getRainLevel(partial));
        strength *= night;
        if (strength <= 0.01F) return;
        float time = (level.getGameTime() % 240000L + partial) / 20.0F;
        Matrix4f pose = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int c = 0; c < CURTAINS; c++) {
            // each curtain an arc across the northern sky, at its own distance and angle, drifting slowly
            float span = 1.3F + 0.25F * c, centre = (float) Math.PI + (c - 1) * 0.45F + 0.15F * Mth.sin(time * 0.013F + c * 2.1F);
            float distance = DISTANCE * (0.85F + 0.15F * c);
            float brightness = strength * (0.55F + 0.45F * Mth.sin(time * 0.05F + c * 1.7F) * Mth.sin(time * 0.021F + c));
            brightness = Math.max(brightness, strength * 0.25F);
            for (int i = 0; i < SEGMENTS; i++) {
                float[] a = column(c, i, span, centre, distance, time), b = column(c, i + 1, span, centre, distance, time);
                // fading out at the curtain's two ends
                float ea = edge(i), eb = edge(i + 1);
                vertex(buffer, pose, a[0], a[1], a[2], 70, 255, 150, a[4] * ea * brightness * 0.55F);
                vertex(buffer, pose, a[0], a[3], a[2], 140, 70, 255, 0.0F);
                vertex(buffer, pose, b[0], b[3], b[2], 140, 70, 255, 0.0F);
                vertex(buffer, pose, b[0], b[1], b[2], 70, 255, 150, b[4] * eb * brightness * 0.55F);
                // a brighter teal band low in the curtain
                float ma = (a[1] + a[3]) * 0.5F, mb = (b[1] + b[3]) * 0.5F;
                vertex(buffer, pose, a[0], a[1], a[2], 120, 255, 200, a[4] * ea * brightness * 0.35F);
                vertex(buffer, pose, a[0], ma, a[2], 90, 200, 255, 0.0F);
                vertex(buffer, pose, b[0], mb, b[2], 90, 200, 255, 0.0F);
                vertex(buffer, pose, b[0], b[1], b[2], 120, 255, 200, b[4] * eb * brightness * 0.35F);
            }
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** One point along a curtain: x, its foot's height, z, its top's height, how bright it is there. */
    private static float[] column(int c, int i, float span, float centre, float distance, float time) {
        float t = (float) i / SEGMENTS;
        float angle = centre + (t - 0.5F) * span;
        // the curtain's folds: it ripples in and out
        float fold = 6.0F * Mth.sin(t * 17.0F + time * 0.35F + c * 3.0F) + 3.0F * Mth.sin(t * 41.0F - time * 0.6F + c);
        float r = distance + fold;
        float x = Mth.sin(angle) * r, z = Mth.cos(angle) * r;
        float height = HEIGHT * (0.7F + 0.3F * Mth.sin(t * 9.0F + time * 0.2F + c * 1.3F));
        float glow = 0.6F + 0.4F * Mth.sin(t * 23.0F + time * 0.9F + c * 2.0F);
        return new float[]{x, BASE + c * 4.0F, z, BASE + c * 4.0F + height, glow};
    }

    private static float edge(int i) {
        float t = (float) i / SEGMENTS;
        return Mth.clamp(Math.min(t, 1.0F - t) * 6.0F, 0.0F, 1.0F);
    }

    private static void vertex(BufferBuilder buffer, Matrix4f pose, float x, float y, float z, int r, int g, int b, float alpha) {
        buffer.addVertex(pose, x, y, z).setColor(r, g, b, (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F));
    }
}
