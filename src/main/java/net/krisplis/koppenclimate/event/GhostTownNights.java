package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * The ghost towns: a player in one is Haunted (an invisible effect that turns their sky yellow). And their nights:
 * skeleton horses roam the streets after dark (a few around each player there), and at
 * sunrise the ones nobody has tamed are gone again, like the cowboy souls (CowboySoulEntity). By day there's
 * nothing.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GhostTownNights {
    // the horses carry this tag, so the town takes back only its own
    private static final String TAG = KoppenClimateMod.MOD_ID + ".ghost_horse";
    private static final int MAX_HORSES = 3;
    private static final double REACH = 48.0;

    private GhostTownNights() {
    }

    // the Lush desert's rare rain follows the overworld's thunderstorms (RareRain)
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            net.krisplis.koppenclimate.worldgen.biome.RareRain.serverStorm = level.isThundering();
        }
    }

    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.structure.Structure> GHOST_TOWN =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "ghost_town"));

    /** The ghost town this block lies in (its whole area), or an invalid start. */
    private static net.minecraft.world.level.levelgen.structure.StructureStart town(ServerLevel level, BlockPos pos) {
        return level.structureManager().getStructureWithPieceAt(pos, h -> h.is(GHOST_TOWN));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        // in a ghost town, the player is haunted: the sky turns a sickly yellow for them (AfAmbience, ClientLevelSkyMixin)
        if (player.tickCount % 20 == 0 && town(level, player.blockPosition()).isValid()) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.krisplis.koppenclimate.effect.ModEffects.HAUNTED.getHolder().get(),
                    60, 0, true, false, false));
        }
        if (player.tickCount % 80 != 0) return;
        AABB around = player.getBoundingBox().inflate(REACH, 24.0, REACH);
        List<SkeletonHorse> horses = level.getEntitiesOfClass(SkeletonHorse.class, around, h -> h.getTags().contains(TAG));
        if (level.isDay()) {
            // sunrise: the untamed ghost horses fade away
            for (SkeletonHorse horse : horses) {
                if (horse.isTamed() || horse.isVehicle() || horse.isLeashed()) continue;
                level.sendParticles(ParticleTypes.SOUL, horse.getX(), horse.getY() + 1.0, horse.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
                level.playSound(null, horse.blockPosition(), SoundEvents.SKELETON_HORSE_DEATH, SoundSource.HOSTILE, 0.6F, 0.8F);
                horse.discard();
            }
            return;
        }
        var start = town(level, player.blockPosition());
        if (!start.isValid() || horses.size() >= MAX_HORSES) return;
        // a horse comes in somewhere out of sight, in the ghost town
        var random = level.random;
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = 20 + random.nextDouble() * 20;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance), z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!start.getBoundingBox().isInside(pos) || !level.getBlockState(pos.below()).getFluidState().isEmpty()) continue;
            if (!level.noCollision(EntityType.SKELETON_HORSE.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) continue;
            SkeletonHorse horse = EntityType.SKELETON_HORSE.create(level);
            if (horse == null) return;
            horse.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            horse.addTag(TAG);
            level.addFreshEntity(horse);
            return;
        }
    }
}
