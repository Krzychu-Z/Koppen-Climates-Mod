package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.effect.ModEffects;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.MutantDogEntity;
import net.krisplis.koppenclimate.entity.custom.MutantZombieEntity;
import net.krisplis.koppenclimate.village.ChernobylPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Life, of a sort, in the abandoned Soviet estate ("Chernobyl", ChernobylPiece):
 * <ul>
 *   <li>anyone in it is marked by Radiation, which turns their sky and fog green (client/AfAmbience);</li>
 *   <li>mutant zombies (one in four a crawler) keep coming, day and night - in the flats, on the stairs, in the courtyard - until some
 *       {@value #CROWD} are about the player (they don't burn in the sun, so they needn't wait for dark);</li>
 *   <li>the mutant dog is contagious: whatever it kills (but a player) gets up again as a mutant zombie.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChernobylLife {
    private static final ResourceKey<Structure> ESTATE = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chernobyl"));
    private static final int CROWD = 22, RANGE = 48, PERIOD = 40;

    private ChernobylLife() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if ((player.tickCount + player.getId()) % 20 != 0) return;
        ServerLevel level = player.serverLevel();
        StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), h -> h.is(ESTATE));
        if (!start.isValid()) return;
        ChernobylPiece estate = null;
        for (StructurePiece piece : start.getPieces()) if (piece instanceof ChernobylPiece c) estate = c;
        if (estate == null || !estate.inEstate(player.blockPosition())) return;
        player.addEffect(new MobEffectInstance(ModEffects.RADIATION.getHolder().orElseThrow(), 60, 0, true, false, true));
        if ((player.tickCount + player.getId()) % PERIOD != 0 || player.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL) return;
        int count = level.getEntitiesOfClass(MutantZombieEntity.class, player.getBoundingBox().inflate(RANGE, 24, RANGE)).size();
        RandomSource random = level.getRandom();
        for (int i = 0, n = Math.min(CROWD - count, 1 + random.nextInt(3)); i < n; i++) {
            for (int tries = 0; tries < 10; tries++) {
                BlockPos pos = spot(level, estate, player, random);
                if (pos == null) continue;
                // one in four a crawler
                MutantZombieEntity mutant = (random.nextInt(4) == 0 ? ModEntities.CRAWLING_MUTANT.get() : ModEntities.MUTANT_ZOMBIE.get()).create(level);
                if (mutant == null) return;
                mutant.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                mutant.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                level.addFreshEntity(mutant);
                break;
            }
        }
    }

    // somewhere a mutant could stand: in the estate, 12 to 40 blocks from the player, out of his sight line or not
    private static BlockPos spot(ServerLevel level, ChernobylPiece estate, Player player, RandomSource random) {
        double a = random.nextDouble() * Math.PI * 2, d = 12 + random.nextDouble() * 28;
        int x = player.getBlockX() + (int) (Math.cos(a) * d), z = player.getBlockZ() + (int) (Math.sin(a) * d);
        int y = estate.origin().getY() + random.nextInt(31);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        if (!level.isLoaded(pos) || !estate.inEstate(pos)) return null;
        for (int k = 0; k < 8; k++, pos.move(0, -1, 0)) {
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                    && level.getFluidState(pos).isEmpty() && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
                return pos.immutable();
            }
        }
        return null;
    }

    // the dog's contagion
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof MutantDogEntity) || victim instanceof Player || victim instanceof MutantZombieEntity
                || !(victim.level() instanceof ServerLevel level)) return;
        MutantZombieEntity mutant = ModEntities.MUTANT_ZOMBIE.get().create(level);
        if (mutant == null) return;
        mutant.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
        mutant.finalizeSpawn(level, level.getCurrentDifficultyAt(victim.blockPosition()), MobSpawnType.CONVERSION, null);
        level.addFreshEntity(mutant);
    }
}
