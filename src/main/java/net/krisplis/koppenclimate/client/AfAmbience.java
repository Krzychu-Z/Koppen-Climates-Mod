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
    // the wind over the dry lands (B: the deserts, the steppe, the Australian desert, the Nile): a second loop,
    // under whatever else plays there, day and night
    private static Loop wind;
    // how far the clouds have faded away (0 = full clouds, 1 = none): they thin out gradually over the hot desert, the
    // Sahel and the Mediterranean (see LevelRendererCloudsMixin) and come back gradually when the player leaves
    private static volatile float cloudFade;
    private static float cloudFadeO;
    // ticks for the clouds to fade fully out or in
    private static final float CLOUD_FADE_TICKS = 120.0F;
    // whether the player is by one of the Dalmatian island towns (out in the sea, so known by its Venetian villagers)
    private static boolean nearIslandTown;
    // the Mediterranean river also runs through the humid subtropics (Cfa, Cwa): there it sounds (and looks, under
    // its sky) like the subtropics about it, not the Mediterranean
    private static boolean subtropicalRiver;

    /** The Mediterranean's own ground and rivers - but not its river where it runs through the subtropics. */
    private static boolean mediterranean(Holder<Biome> biome) {
        return ModBiomes.isMediterranean(biome) && !(biome.is(ModBiomes.MEDITERRANEAN_RIVER) && subtropicalRiver);
    }

    /** Whether the land around this river is the subtropics' (Cfa, Cwa) rather than the Mediterranean's. */
    private static boolean amidSubtropics(Minecraft mc, LocalPlayer player) {
        int subtropical = 0, mediterranean = 0;
        for (int ring = 1; ring <= 4; ring++) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4;
                net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(player.getX() + Math.cos(a) * 24 * ring,
                        player.getY(), player.getZ() + Math.sin(a) * 24 * ring);
                Holder<Biome> b = mc.level.getBiome(at);
                if (ModBiomes.isSubtropical(b)) subtropical++;
                else if (ModBiomes.isMediterranean(b) && !b.is(ModBiomes.MEDITERRANEAN_RIVER)) mediterranean++;
            }
        }
        // (ties - rivers wide enough that most of the ring is water - go to the subtropics when there's any of them about)
        return subtropical > 0 && subtropical >= mediterranean;
    }

    public static boolean hideClouds() {
        return cloudFade >= 1.0F && cloudFadeO >= 1.0F;
    }

    // how visible the clouds are this frame, between 0 and 1
    public static float cloudAlpha(float partialTick) {
        return 1.0F - net.minecraft.util.Mth.lerp(partialTick, cloudFadeO, cloudFade);
    }

    // how far the sky has turned to the ghost towns' yellow (0-1), fading in and out over a few seconds
    private static float haunt, hauntO;

    public static float haunt(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, hauntO, haunt);
    }

    // how far the sky and the fog have turned to Pompeii's fiery red (0-1), fading in on entering it
    private static float pompeii, pompeiiO;
    // Pompeii's red: the sky by day, a dark glowing ember at night; its fog, thick with smoke and ash
    private static final net.minecraft.world.phys.Vec3 POMPEII_SKY = new net.minecraft.world.phys.Vec3(0.77, 0.20, 0.10);
    private static final net.minecraft.world.phys.Vec3 POMPEII_FOG = new net.minecraft.world.phys.Vec3(0.60, 0.17, 0.08);

    public static float pompeii(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, pompeiiO, pompeii);
    }

    /** The red Pompeii's fire casts over the sky - never quite dark, even at night. */
    public static net.minecraft.world.phys.Vec3 pompeiiSky(net.minecraft.world.phys.Vec3 sky, float daylight, float partialTick) {
        float red = pompeii(partialTick);
        if (red <= 0.0F) return sky;
        return sky.lerp(POMPEII_SKY.scale(Math.max(daylight, 0.45F)), red * 0.9F);
    }

    // its fog too (vanilla's fog takes the biome's fog colour by day, but goes black at night)
    @SubscribeEvent
    public static void onFogColor(net.minecraftforge.client.event.ViewportEvent.ComputeFogColor event) {
        float green = radiation((float) event.getPartialTick());
        if (green > 0.0F && event.getCamera().getFluidInCamera() == net.minecraft.world.level.material.FogType.NONE) {
            // the estate's fog: a murky green, by day and (dimmer) by night
            Minecraft m = Minecraft.getInstance();
            float day = m.level == null ? 1.0F : Mth.clamp(Mth.cos(m.level.getTimeOfDay((float) event.getPartialTick())
                    * ((float) Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
            net.minecraft.world.phys.Vec3 fog = new net.minecraft.world.phys.Vec3(event.getRed(), event.getGreen(), event.getBlue())
                    .lerp(new net.minecraft.world.phys.Vec3(0.42, 0.62, 0.22).scale(Math.max(day, 0.35F)), green * 0.85F);
            event.setRed((float) fog.x);
            event.setGreen((float) fog.y);
            event.setBlue((float) fog.z);
        }
        float red = pompeii((float) event.getPartialTick());
        if (red <= 0.0F || event.getCamera().getFluidInCamera() != net.minecraft.world.level.material.FogType.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        float daylight = mc.level == null ? 1.0F : Mth.clamp(Mth.cos(mc.level.getTimeOfDay((float) event.getPartialTick())
                * ((float) Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
        net.minecraft.world.phys.Vec3 fog = new net.minecraft.world.phys.Vec3(event.getRed(), event.getGreen(), event.getBlue())
                .lerp(POMPEII_FOG.scale(Math.max(daylight, 0.5F)), red * 0.85F);
        event.setRed((float) fog.x);
        event.setGreen((float) fog.y);
        event.setBlue((float) fog.z);
    }

    private static void fadePompeii(boolean in) {
        pompeiiO = pompeii;
        pompeii = in ? Math.min(1.0F, pompeii + 0.025F) : Math.max(0.0F, pompeii - 0.025F);
    }

    // how far the sky and fog have turned the estate's sickly green (0-1)
    private static float glow, glowO;

    public static float radiation(float partialTick) {
        return Mth.lerp(partialTick, glowO, glow);
    }

    private static void fadeRadiation(boolean in) {
        glowO = glow;
        glow = in ? Math.min(1.0F, glow + 0.02F) : Math.max(0.0F, glow - 0.02F);
    }

    private static void fadeHaunt(boolean haunted) {
        hauntO = haunt;
        haunt = haunted ? Math.min(1.0F, haunt + 0.02F) : Math.max(0.0F, haunt - 0.02F);
    }

    private static void fadeClouds(boolean hide) {
        cloudFadeO = cloudFade;
        float step = 1.0F / CLOUD_FADE_TICKS;
        cloudFade = hide ? Math.min(1.0F, cloudFade + step) : Math.max(0.0F, cloudFade - step);
    }

    private static boolean nearVenetians(Minecraft mc, LocalPlayer player) {
        return !mc.level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, player.getBoundingBox().inflate(56.0),
                v -> v.getVillagerData().getType() == net.krisplis.koppenclimate.village.VillageRegistry.VENETIAN.get()).isEmpty();
    }

    private AfAmbience() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        net.krisplis.koppenclimate.worldgen.biome.RareRain.clientStorm = mc.level != null && mc.level.isThundering();
        mosquitoes(mc, player);
        glowworms(mc, player);
        owls(mc, player);
        woodBirds(mc, player);
        SoundEvent wanted = null, windWanted = null;
        if (player != null && mc.level != null && !player.isRemoved()) {
            Holder<Biome> here = mc.level.getBiome(player.blockPosition());
            // no clouds over the hot desert, the Sahel or the Mediterranean
            // (and over the Nile, which runs through the deserts)
            if (player.tickCount % 40 == 0) subtropicalRiver = here.is(ModBiomes.MEDITERRANEAN_RIVER) && amidSubtropics(mc, player);
            fadeClouds(here.is(ModBiomes.HOT_DESERT) || here.is(ModBiomes.CONTINENTAL_DESERT) || here.is(ModBiomes.DRY_STEPPE) || here.is(ModBiomes.AUSTRALIAN_DESERT)
                    || here.is(ModBiomes.NILE) || mediterranean(here));
            fadePompeii(here.is(ModBiomes.POMPEII));
            fadeHaunt(player.hasEffect(net.krisplis.koppenclimate.effect.ModEffects.HAUNTED.getHolder().get()));
            fadeRadiation(player.hasEffect(net.krisplis.koppenclimate.effect.ModEffects.RADIATION.getHolder().get()));
            wanted = pick(here, mc.level.getDayTime());
            if (ModBiomes.isDesert(here) || here.is(ModBiomes.NILE)) windWanted = ModSounds.HOT_DESERT_WIND.get();
            // the Dalmatian island towns stand out in the sea: around one, the sea sounds like the Mediterranean beach
            if (player.tickCount % 40 == 0) nearIslandTown = nearVenetians(mc, player);
            if (wanted == null && nearIslandTown && here.is(net.minecraft.tags.BiomeTags.IS_OCEAN)) {
                long time = Math.floorMod(mc.level.getDayTime(), 24000L);
                boolean night = time >= NIGHT_START && time < NIGHT_END;
                wanted = (night ? ModSounds.MEDITERRANEAN_BEACH_NIGHT : ModSounds.MEDITERRANEAN_BEACH_DAY).get();
            }
        } else {
            cloudFade = cloudFadeO = 0.0F;
            pompeii = pompeiiO = 0.0F;
        }
        seaUnderCliffs(mc, player != null && mc.level != null && !player.isRemoved() ? mc.level.getBiome(player.blockPosition()) : null);
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
        if (wind != null && (wind.isStopped() || !mc.getSoundManager().isActive(wind))) wind = null;
        if (wind != null && wind.event != windWanted) {
            wind.fadeOut();
            wind = null;
        }
        if (wind == null && windWanted != null) {
            wind = new Loop(windWanted);
            mc.getSoundManager().play(wind);
        }
    }

    // every couple of seconds, a little cloud of mosquitoes somewhere around the player, over the Amazon's rivers
    // and the rice terraces (the biomes' own ambient mosquitoes are spread over far too big a space to notice)
    private static void mosquitoes(Minecraft mc, @Nullable LocalPlayer player) {
        if (player == null || mc.level == null || mc.isPaused() || player.tickCount % 40 != 0) return;
        Holder<Biome> biome = mc.level.getBiome(player.blockPosition());
        // every tropical rain forest (A climates): the Amazon and its river, the monsoon forest and the rice terraces
        if (!ModBiomes.isRainforest(biome) && !biome.is(ModBiomes.RICE_TERRACES) && !biome.is(ModBiomes.SUBTROPICAL_SWAMP)) return;
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

    // on Mediterranean and Australian desert nights, glowworms: just like the mosquitoes - every second, a little cloud of them hovering
    // somewhere in the open air around the player at about head height - but glowing, and only at night
    private static void glowworms(Minecraft mc, @Nullable LocalPlayer player) {
        if (player == null || mc.level == null || mc.isPaused() || player.tickCount % 20 != 0) return;
        long time = Math.floorMod(mc.level.getDayTime(), 24000L);
        if (time < NIGHT_START || time >= NIGHT_END) return;
        Holder<Biome> here = mc.level.getBiome(player.blockPosition());
        if (!mediterranean(here) && !here.is(ModBiomes.AUSTRALIAN_DESERT)) return;
        var random = mc.level.random;
        double cx = 0, cy = 0, cz = 0;
        boolean found = false;
        for (int attempt = 0; attempt < 6 && !found; attempt++) {
            cx = player.getX() + (random.nextDouble() - 0.5) * 12.0;
            cz = player.getZ() + (random.nextDouble() - 0.5) * 12.0;
            cy = player.getY() + random.nextDouble() * 2.5;
            net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(cx, cy, cz);
            found = mc.level.getBlockState(at).isAir() && mc.level.getFluidState(at).isEmpty();
        }
        if (!found) return;
        int count = 2 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            mc.level.addParticle(net.krisplis.koppenclimate.particle.ModParticles.GLOWWORM.get(),
                    cx + random.nextGaussian() * 0.8, cy + random.nextGaussian() * 0.4, cz + random.nextGaussian() * 0.8, 0, 0, 0);
        }
    }

    // now and then at night in the Mediterranean, an owl hoots somewhere out in the trees
    private static int owlCooldown = 200;

    private static void owls(Minecraft mc, @Nullable LocalPlayer player) {
        if (player == null || mc.level == null || mc.isPaused()) return;
        if (--owlCooldown > 0) return;
        var random = mc.level.random;
        long time = Math.floorMod(mc.level.getDayTime(), 24000L);
        Holder<Biome> biome = mc.level.getBiome(player.blockPosition());
        // outside the night or the biome, check again in a few seconds rather than waiting out a whole pause
        if (time < NIGHT_START || time >= NIGHT_END
                || !(mediterranean(biome) || ModBiomes.isDesert(biome) || biome.is(ModBiomes.REDWOOD_FOREST)
                || isTemperateWood(biome))) {
            owlCooldown = 100;
            return;
        }
        // roughly every 15-40 seconds, close enough to hear clearly over the crickets
        owlCooldown = 300 + random.nextInt(500);
        double angle = random.nextDouble() * Math.PI * 2, distance = 6 + random.nextDouble() * 6;
        mc.getSoundManager().play(new net.minecraft.client.resources.sounds.SimpleSoundInstance(ModSounds.OWL.get(), SoundSource.AMBIENT,
                0.9F + random.nextFloat() * 0.1F, 0.9F + random.nextFloat() * 0.2F, SoundInstance.createUnseededRandom(),
                player.getX() + Math.cos(angle) * distance, earLevel(player, random), player.getZ() + Math.sin(angle) * distance));
    }

    /**
     * The height to play a one-off ambient sound at: about the player's ears. A sound placed high above them is in
     * front of them whenever they look up, and the game plays sounds in front louder than sounds behind or above
     * (most of all with directional audio), so a call from above would grow loud just by looking at the sky.
     */
    private static double earLevel(LocalPlayer player, net.minecraft.util.RandomSource random) {
        return player.getEyeY() - 0.5 + random.nextDouble();
    }

    static boolean isTemperateWood(Holder<Biome> biome) {
        return biome.is(ModBiomes.TEMPERATE_FOREST) || biome.is(ModBiomes.SUNFLOWER_FIELD)
                || biome.is(ModBiomes.CONTINENTAL_FOREST) || biome.is(ModBiomes.PINE_FOREST) || biome.is(ModBiomes.ALPINE_VALLEY)
                || biome.is(ModBiomes.CONTINENTAL_MOUNTAINS) || biome.is(ModBiomes.CONTINENTAL_CLEARING)
                || biome.is(ModBiomes.COLD_LAKELAND) || biome.is(ModBiomes.SUBPOLAR_TAIGA);
    }

    // by day, now and then, a crow somewhere in the Continental forest (Dfb) - and less often in the Temperate forest
    // (Cfb) - and a woodpecker drumming in the Pine forest; never at night
    private static int birdCooldown = 200;

    private static void woodBirds(Minecraft mc, @Nullable LocalPlayer player) {
        if (player == null || mc.level == null || mc.isPaused()) return;
        if (--birdCooldown > 0) return;
        var random = mc.level.random;
        long time = Math.floorMod(mc.level.getDayTime(), 24000L);
        Holder<Biome> biome = mc.level.getBiome(player.blockPosition());
        SoundEvent call = null;
        if (time < NIGHT_START || time >= NIGHT_END) {
            if (biome.is(ModBiomes.PINE_FOREST) || biome.is(ModBiomes.ALPINE_VALLEY) || biome.is(ModBiomes.COLD_LAKELAND)) {
                call = ModSounds.DFB_WOODPECKER.get();
                birdCooldown = 200 + random.nextInt(300);       // every 10-25 seconds
            } else if (biome.is(ModBiomes.CONTINENTAL_FOREST) || biome.is(ModBiomes.CONTINENTAL_MOUNTAINS)
                    || biome.is(ModBiomes.CONTINENTAL_CLEARING)) {
                call = ModSounds.DFB_CROW.get();
                birdCooldown = 240 + random.nextInt(300);       // every 12-27 seconds
            }
        }
        if (call == null) {
            birdCooldown = 100;
            return;
        }
        // close by, in the trees round the player (a sound at volume 1 fades out over 16 blocks)
        double angle = random.nextDouble() * Math.PI * 2, distance = 3 + random.nextDouble() * 5;
        mc.getSoundManager().play(new net.minecraft.client.resources.sounds.SimpleSoundInstance(call, SoundSource.AMBIENT,
                1.0F, 0.95F + random.nextFloat() * 0.1F, SoundInstance.createUnseededRandom(),
                player.getX() + Math.cos(angle) * distance, earLevel(player, random), player.getZ() + Math.sin(angle) * distance));
    }

    // the sea under the cliffs (White cliffs, Scottish cliffs): a second loop, under the wind
    private static Loop sea;

    private static void seaUnderCliffs(Minecraft mc, @Nullable Holder<Biome> here) {
        SoundEvent wanted = here != null && (here.is(ModBiomes.WHITE_CLIFFS) || here.is(ModBiomes.SCOTTISH_CLIFFS))
                ? ModSounds.CLIFF_WAVES.get() : null;
        if (sea != null && (sea.isStopped() || !mc.getSoundManager().isActive(sea))) sea = null;
        if (sea != null && sea.event != wanted) {
            sea.fadeOut();
            sea = null;
        }
        if (sea == null && wanted != null) {
            sea = new Loop(wanted);
            mc.getSoundManager().play(sea);
        }
    }

    @Nullable
    private static SoundEvent pick(Holder<Biome> biome, long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        boolean night = time >= NIGHT_START && time < NIGHT_END;
        if (ModBiomes.isRainforest(biome)) {
            return (night ? ModSounds.AMAZON_NIGHT : ModSounds.AMAZON_DAY).get();
        }
        if (biome.is(ModBiomes.AFRICAN_SAVANNA) || biome.is(ModBiomes.SAVANNA_RIVER)) {
            return (night ? ModSounds.SAVANNA_NIGHT : ModSounds.SAVANNA_DAY).get();
        }
        if (biome.is(ModBiomes.MEDITERRANEAN_BEACH)) {
            return (night ? ModSounds.MEDITERRANEAN_BEACH_NIGHT : ModSounds.MEDITERRANEAN_BEACH_DAY).get();
        }
        if (biome.is(ModBiomes.REDWOOD_FOREST)) {
            // the forest by day; at night it falls quiet but for the owls
            return night ? null : ModSounds.REDWOOD_FOREST_DAY.get();
        }
        if (ModBiomes.isSubtropical(biome) || biome.is(ModBiomes.MEDITERRANEAN_RIVER) && subtropicalRiver) {
            // the humid subtropics (Cfa), plains and swamp alike
            return (night ? ModSounds.CFA_NIGHT : ModSounds.CFA_DAY).get();
        }
        if (biome.is(ModBiomes.AUSTRALIAN_DESERT)) {
            // a faint, quiet outback by day; at night only the owls
            return night ? null : ModSounds.AUSTRALIAN_DESERT_DAY.get();
        }
        if (biome.is(ModBiomes.PRAIRIE)) {
            // nothing but a soft wind through the grass, day and night
            return ModSounds.PRAIRIE_WIND.get();
        }
        if (isTemperateWood(biome)) {
            // the temperate and continental forests (Cfb, Dfb) by day; at night they fall silent but for the owls
            return night ? null : ModSounds.CFB_FOREST.get();
        }
        if (biome.is(ModBiomes.ALPINE_MID) || biome.is(ModBiomes.ALPINE_HIGH)) {
            // only the wind up on the mountains, day and night
            return ModSounds.ALPINE_WIND.get();
        }
        if (biome.is(ModBiomes.ENGLISH_COUNTRYSIDE) || biome.is(ModBiomes.WHITE_CLIFFS)
                || biome.is(ModBiomes.SCOTTISH_HIGHLAND) || biome.is(ModBiomes.SCOTTISH_CLIFFS)) {
            // a faint wind over the fields
            return ModSounds.CFB_WIND.get();
        }
        if (ModBiomes.isDesert(biome) || biome.is(ModBiomes.NILE)) {
            // (the dry lands' wind plays on its own loop, day and night: see wind)
            return null;
        }
        if (mediterranean(biome)) {
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
        // how loud it gets at most: the desert wind is kept low, under everything else
        private final float max;

        Loop(SoundEvent event) {
            super(event, SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
            this.event = event;
            this.looping = true;
            this.delay = 0;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.volume = 0.01F;
            this.max = event == ModSounds.HOT_DESERT_WIND.get() ? 0.3F
                    : event == ModSounds.PRAIRIE_WIND.get() ? 0.22F
                    : event == ModSounds.CFB_WIND.get() ? 0.15F
                    : event == ModSounds.ALPINE_WIND.get() ? 0.18F
                    : event == ModSounds.CFB_FOREST.get() ? 0.8F
                    : event == ModSounds.CLIFF_WAVES.get() ? 0.3F
                    : event == ModSounds.AUSTRALIAN_DESERT_DAY.get() ? 0.5F
                    : event == ModSounds.REDWOOD_FOREST_DAY.get() ? 0.8F : 1.0F;
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
            this.volume = Math.max(0.01F, this.max * this.fade / FADE_TICKS);
            if (this.direction < 0 && this.fade == 0) {
                this.stop();
            }
        }
    }
}
