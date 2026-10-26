package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.sound.ModSounds;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

/**
 * The Amazon's background ambience, with a day and a night version. Vanilla biomes can only have one looping
 * ambient sound, so instead of the biome files this plays the loop itself: the Amazon jungle and its rivers get
 * the jungle loop, the monsoon forest too, the tropical beach the same with the sea under it, each cross-fading between day and night at
 * dusk and dawn and fading out when the player leaves.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AfAmbience {
    // ticks to fade a loop fully in or out
    private static final int FADE_TICKS = 60;
    // night runs from just before dusk to just after dawn, in day-time ticks
    private static final long NIGHT_START = 12600;
    private static final long NIGHT_END = 23400;

    @Nullable
    private static Loop current;

    private AfAmbience() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        mosquitoes(mc, player);
        owls(mc, player);
        SoundEvent wanted = null;
        if (player != null && mc.level != null && !player.isRemoved()) {
            wanted = pick(mc.level.getBiome(player.blockPosition()), mc.level.getDayTime());
        }
        if (current != null && (current.isStopped() || !mc.getSoundManager().isActive(current))) {
            current = null;
        }
        if (current != null && current.event != wanted) {
            current.fadeOut();
            current = null;
        }
        if (current == null && wanted != null) {
            current = new Loop(wanted);
            mc.getSoundManager().play(current);
        }
    }

    // every couple of seconds, a little cloud of mosquitoes somewhere around the player, over the Amazon's rivers
    // and the rice terraces (the biomes' own ambient mosquitoes are spread over far too big a space to notice)
    private static void mosquitoes(Minecraft mc, @Nullable LocalPlayer player) {
        if (player == null || mc.level == null || mc.isPaused() || player.tickCount % 40 != 0) return;
        Holder<Biome> biome = mc.level.getBiome(player.blockPosition());
        if (!biome.is(ModBiomes.AMAZON_JUNGLE) && !biome.is(ModBiomes.AMAZON_JUNGLE_RIVER) && !biome.is(ModBiomes.RICE_TERRACES)) return;
        var random = mc.level.random;
        // somewhere in the open air around the player, at about head height - never in water, never up in the canopy
        double cx = 0, cy = 0, cz = 0;
        boolean found = false;
        for (int attempt = 0; attempt < 6 && !found; attempt++) {
            cx = player.getX() + (random.nextDouble() - 0.5) * 10.0;
            cz = player.getZ() + (random.nextDouble() - 0.5) * 10.0;
            cy = player.getY() + random.nextDouble() * 2.5;
            net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(cx, cy, cz);
            found = mc.level.getBlockState(at).isAir() && mc.level.getFluidState(at).isEmpty();
        }
        if (!found) return;
        int count = 3 + random.nextInt(5);
        for (int i = 0; i < count; i++) {
            mc.level.addParticle(net.krisplis.koppenclimate.particle.ModParticles.MOSQUITO.get(),
                    cx + random.nextGaussian() * 0.4, cy + random.nextGaussian() * 0.3, cz + random.nextGaussian() * 0.4, 0, 0, 0);
        }
    }

    // now and then at night in the Mediterranean, an owl hoots somewhere out in the trees
    private static int owlCooldown = 200;

    private static void owls(Minecraft mc, @Nullable LocalPlayer player) {
        if (player == null || mc.level == null || mc.isPaused()) return;
        if (--owlCooldown > 0) return;
        var random = mc.level.random;
        owlCooldown = 500 + random.nextInt(1100);
        long time = Math.floorMod(mc.level.getDayTime(), 24000L);
        if (time < NIGHT_START || time >= NIGHT_END) return;
        Holder<Biome> biome = mc.level.getBiome(player.blockPosition());
        if (!biome.is(ModBiomes.MEDITERRANEAN) && !biome.is(ModBiomes.MEDITERRANEAN_BEACH)) return;
        double angle = random.nextDouble() * Math.PI * 2, distance = 12 + random.nextDouble() * 14;
        mc.getSoundManager().play(new net.minecraft.client.resources.sounds.SimpleSoundInstance(ModSounds.OWL.get(), SoundSource.AMBIENT,
                0.7F + random.nextFloat() * 0.3F, 0.9F + random.nextFloat() * 0.2F, SoundInstance.createUnseededRandom(),
                player.getX() + Math.cos(angle) * distance, player.getY() + 4 + random.nextDouble() * 6, player.getZ() + Math.sin(angle) * distance));
    }

    @Nullable
    private static SoundEvent pick(Holder<Biome> biome, long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        boolean night = time >= NIGHT_START && time < NIGHT_END;
        if (ModBiomes.isRainforest(biome)) {
            return (night ? ModSounds.AMAZON_NIGHT : ModSounds.AMAZON_DAY).get();
        }
        if (biome.is(ModBiomes.AFRICAN_SAVANNA)) {
            return (night ? ModSounds.SAVANNA_NIGHT : ModSounds.SAVANNA_DAY).get();
        }
        if (biome.is(ModBiomes.MEDITERRANEAN) || biome.is(ModBiomes.MEDITERRANEAN_BEACH)) {
            return (night ? ModSounds.MEDITERRANEAN_NIGHT : ModSounds.MEDITERRANEAN_DAY).get();
        }
        if (biome.is(ModBiomes.TROPICAL_BEACH)) {
            return (night ? ModSounds.BEACH_NIGHT : ModSounds.BEACH_DAY).get();
        }
        return null;
    }

    /** One playing loop: fades in when it starts, and out (then stops) when it's no longer wanted. */
    private static final class Loop extends AbstractTickableSoundInstance {
        final SoundEvent event;
        private int fade;
        private int direction = 1;

        Loop(SoundEvent event) {
            super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.event = event;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
        }

        void fadeOut() {
            this.direction = -1;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            this.fade = Mth.clamp(this.fade + this.direction, 0, FADE_TICKS);
            this.volume = Math.max(0.01F, (float) this.fade / FADE_TICKS);
            if (this.direction < 0 && this.fade == 0) {
                this.stop();
            }
        }
    }
}
