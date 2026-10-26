package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.AnacondaEntity;
import net.krisplis.koppenclimate.entity.custom.CrocodileEntity;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Crocodiles and anacondas living in the Amazon's rivers. Vanilla only spawns land animals on solid ground and
 * almost only when a chunk is first made, so a river would never get any; this tops them up instead, in the
 * water of the Amazon Jungle River, out of the player's sight and up to a small number around each player.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RiverPredators {
    // how often (in ticks) each player's surroundings are checked
    private static final int INTERVAL = 200;
    // spawn this far from the player: out of sight, but close enough to meet
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 64;
    // how many of each may live within this many blocks of a player
    private static final int COUNT_RADIUS = 80;
    private static final int MAX_CROCODILES = 4;
    private static final int MAX_ANACONDAS = 3;

    private RiverPredators() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD || level.getGameTime() % INTERVAL != 0) {
            return;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return;
        }
        RandomSource random = level.getRandom();
        for (Player player : level.players()) {
            if (player.isSpectator()) continue;
            // the monsoon (Aw) country has cobras on land instead of anacondas, so its rivers only get crocodiles
            boolean crocodile = random.nextFloat() < 0.55F || ModBiomes.isMonsoon(level.getBiome(player.blockPosition()));
            EntityType<? extends Mob> type = crocodile ? ModEntities.CROCODILE.get() : ModEntities.ANACONDA.get();
            int max = crocodile ? MAX_CROCODILES : MAX_ANACONDAS;
            Class<? extends Mob> kind = crocodile ? CrocodileEntity.class : AnacondaEntity.class;
            if (level.getEntitiesOfClass(kind, new AABB(player.blockPosition()).inflate(COUNT_RADIUS)).size() >= max) {
                continue;
            }
            BlockPos pos = findRiverWater(level, player, random);
            if (pos != null) {
                spawn(level, type, pos);
            }
        }
    }

    // a spot at the surface of Amazon river water, the right distance from the player
    private static BlockPos findRiverWater(ServerLevel level, Player player, RandomSource random) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int distance = Mth.nextInt(random, MIN_DISTANCE, MAX_DISTANCE);
            int x = player.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = player.getBlockZ() + (int) (Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos surface = new BlockPos(x, top, z);
            if (!level.getFluidState(surface).is(FluidTags.WATER)) continue;
            if (!level.getBiome(surface).is(ModBiomes.AMAZON_JUNGLE_RIVER)) continue;
            if (!level.getBlockState(surface.above()).isAir()) continue;
            return surface;
        }
        return null;
    }

    private static void spawn(ServerLevel level, EntityType<? extends Mob> type, BlockPos pos) {
        Mob mob = type.create(level);
        if (mob == null) return;
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(mob)) return;
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        level.addFreshEntity(mob);
    }
}
