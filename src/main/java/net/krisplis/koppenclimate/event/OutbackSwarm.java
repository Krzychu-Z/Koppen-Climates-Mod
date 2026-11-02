package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * The Australian desert (BSh) crawls with vipers and scorpions, far more than the game's usual cap on animals
 * would ever allow: around every player out there, this keeps the ground stocked with about fifteen of them (out of
 * sight, a dozen blocks away and more). They are only ever the desert's: once nobody is near them any more, they are
 * gone again. At night the air above it fills with bats as well, flying free over the open plain.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OutbackSwarm {
    private static final String TAG = KoppenClimateMod.MOD_ID + ".outback_swarm";
    // how many of them around each player, and how far out they're counted and put
    private static final int TARGET = 15, PER_PASS = 3;
    private static final double COUNT_RANGE = 32.0;
    private static final int MIN_DISTANCE = 12, MAX_DISTANCE = 30;
    // beyond this from every player, the swarm's own go
    private static final double KEEP_RANGE = 96.0;
    // at night, bats: a great many of them flitting about in the open air over the plain
    private static final String BAT_TAG = KoppenClimateMod.MOD_ID + ".outback_bat";
    private static final int BAT_TARGET = 24, BAT_PER_PASS = 3;
    private static final int BAT_MIN_DISTANCE = 6, BAT_MAX_DISTANCE = 28;
    // night runs from just before dusk to just after dawn, in day-time ticks (as in AfAmbience)
    private static final long NIGHT_START = 12600, NIGHT_END = 23400;

    private OutbackSwarm() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 40 != 7) return;
        if (player.isSpectator() || !(player.level() instanceof ServerLevel level)) return;
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        if (!level.getBiome(player.blockPosition()).is(ModBiomes.AUSTRALIAN_DESERT)) return;
        if (isNight(level)) bats(level, player);
        AABB area = player.getBoundingBox().inflate(COUNT_RANGE, 16.0, COUNT_RANGE);
        int present = level.getEntitiesOfClass(Mob.class, area,
                m -> m.getType() == ModEntities.VIPER.get() || m.getType() == ModEntities.SCORPION.get()).size();
        RandomSource random = level.getRandom();
        for (int i = 0; i < PER_PASS && present < TARGET; i++) {
            if (spawnOne(level, player, random)) present++;
        }
    }

    private static boolean spawnOne(ServerLevel level, ServerPlayer player, RandomSource random) {
        float angle = random.nextFloat() * Mth.TWO_PI;
        int distance = MIN_DISTANCE + random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
        int x = Mth.floor(player.getX() + Mth.cos(angle) * distance), z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (!level.getBiome(pos).is(ModBiomes.AUSTRALIAN_DESERT) || !level.getBlockState(pos.below()).is(BlockTags.SAND)) return false;
        if (!level.getBlockState(pos).isAir()) return false;
        EntityType<? extends Mob> type = random.nextBoolean() ? ModEntities.VIPER.get() : ModEntities.SCORPION.get();
        Mob mob = type.create(level);
        if (mob == null) return false;
        mob.moveTo(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(mob)) return false;
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        mob.addTag(TAG);
        return level.addFreshEntity(mob);
    }

    private static boolean isNight(ServerLevel level) {
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return time >= NIGHT_START && time < NIGHT_END;
    }

    // keeps the night sky around the player full of bats, flying free a few blocks over the ground
    private static void bats(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(COUNT_RANGE, 24.0, COUNT_RANGE);
        int present = level.getEntitiesOfClass(net.minecraft.world.entity.ambient.Bat.class, area).size();
        RandomSource random = level.getRandom();
        for (int i = 0; i < BAT_PER_PASS && present < BAT_TARGET; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            int distance = BAT_MIN_DISTANCE + random.nextInt(BAT_MAX_DISTANCE - BAT_MIN_DISTANCE + 1);
            int x = Mth.floor(player.getX() + Mth.cos(angle) * distance), z = Mth.floor(player.getZ() + Mth.sin(angle) * distance);
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2 + random.nextInt(6), z);
            if (!level.getBiome(pos).is(ModBiomes.AUSTRALIAN_DESERT) || !level.getBlockState(pos).isAir()) continue;
            net.minecraft.world.entity.ambient.Bat bat = EntityType.BAT.create(level);
            if (bat == null) continue;
            bat.moveTo(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(bat)) continue;
            bat.setResting(false);
            bat.addTag(BAT_TAG);
            if (level.addFreshEntity(bat)) present++;
        }
    }

    // the swarm's own leave once nobody is near
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.getGameTime() % 100 != 0) return;
        List<Entity> gone = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity.getTags().contains(TAG) && level.getNearestPlayer(entity, KEEP_RANGE) == null) gone.add(entity);
            // the night's bats: gone once nobody is near, and by day once nobody is close enough to see them go
            else if (entity.getTags().contains(BAT_TAG) && (level.getNearestPlayer(entity, KEEP_RANGE) == null
                    || !isNight(level) && level.getNearestPlayer(entity, 24.0) == null)) gone.add(entity);
        }
        gone.forEach(Entity::discard);
    }
}
