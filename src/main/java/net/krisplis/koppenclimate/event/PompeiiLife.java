package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.RomanLegionnaireEntity;
import net.krisplis.koppenclimate.entity.custom.ScorpionEntity;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pompeii's dead and its creatures, at any hour. Monsters only come out of the dark by themselves (in daylight a
 * zombie, a skeleton or a blaze won't spawn), and Pompeii burns bright day and night: so about every player in
 * Pompeii's biome, every couple of seconds, more are brought out on the burning ground some way off, up to a
 * number - swarms of scorpions (fire-proof here: see ScorpionEntity), Roman Legionnaires and a few blazes.
 * (By night the biome's own monster spawns add to them.) They go again, as monsters do, once nobody is near.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PompeiiLife {
    private static final int EVERY = 40;
    private static final double RANGE = 40.0;
    private static final int MAX_SCORPIONS = 20, MAX_LEGIONNAIRES = 6, MAX_BLAZES = 3;

    private PompeiiLife() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level) || player.isSpectator()
                || player.tickCount % EVERY != 23 || level.getDifficulty() == Difficulty.PEACEFUL) return;
        if (!level.getBiome(player.blockPosition()).is(ModBiomes.POMPEII)) return;
        AABB area = player.getBoundingBox().inflate(RANGE, 16.0, RANGE);
        if (level.getEntitiesOfClass(ScorpionEntity.class, area).size() < MAX_SCORPIONS) {
            spawn(level, player, ModEntities.SCORPION.get(), 2 + level.random.nextInt(3), 0);
        }
        if (level.random.nextInt(2) == 0 && level.getEntitiesOfClass(RomanLegionnaireEntity.class, area).size() < MAX_LEGIONNAIRES) {
            spawn(level, player, ModEntities.ROMAN_LEGIONNAIRE.get(), 1 + level.random.nextInt(2), 0);
        }
        if (level.random.nextInt(4) == 0 && level.getEntitiesOfClass(Blaze.class, area).size() < MAX_BLAZES) {
            spawn(level, player, EntityType.BLAZE, 1, 2 + level.random.nextInt(4));
        }
    }

    /** A group of mobs together somewhere on Pompeii's ground, 12 to 30 blocks from the player (the blazes above it). */
    private static void spawn(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> type, int count, int lift) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = level.random.nextDouble() * Math.PI * 2, r = 12 + level.random.nextDouble() * 18;
            int x = (int) Math.floor(player.getX() + Math.cos(a) * r), z = (int) Math.floor(player.getZ() + Math.sin(a) * r);
            BlockPos probe = new BlockPos(x, player.getBlockY(), z);
            if (!level.hasChunkAt(probe)) continue;
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
            if (Math.abs(at.getY() - player.getY()) > 12 || !level.getBiome(at).is(ModBiomes.POMPEII)) continue;
            if (!level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP) || !level.getFluidState(at).isEmpty()
                    || !level.getFluidState(at.below()).isEmpty()) continue;
            // scorpions not right into a fire (they would be fine, but it looks better)
            if (type == ModEntities.SCORPION.get() && level.getBlockState(at).is(Blocks.FIRE)) continue;
            if (!level.noCollision(type.getSpawnAABB(at.getX() + 0.5, at.getY() + lift, at.getZ() + 0.5))) continue;
            for (int i = 0; i < count; i++) {
                Mob mob = type.create(level);
                if (mob == null) return;
                double ox = i == 0 ? 0 : level.random.nextDouble() * 2 - 1, oz = i == 0 ? 0 : level.random.nextDouble() * 2 - 1;
                mob.moveTo(at.getX() + 0.5 + ox, at.getY() + lift, at.getZ() + 0.5 + oz, level.random.nextFloat() * 360.0F, 0.0F);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
                level.addFreshEntityWithPassengers(mob);
            }
            return;
        }
    }
}
