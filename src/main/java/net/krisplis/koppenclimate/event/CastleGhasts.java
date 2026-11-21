package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.TinyGhastEntity;
import net.krisplis.koppenclimate.village.CastlePiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Highland Castle's swarms of tiny ghasts. Ghasts never spawn in the overworld by themselves, so while a player
 * is in or about a castle, every couple of seconds a few more tiny ghasts float in - over the courtyard, round the
 * towers, in the halls - until some {@value #SWARM} of them are about. They keep coming, day and night, as long as
 * anyone's there; they wander off and are forgotten (despawn) once no one is near.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CastleGhasts {
    private static final ResourceKey<Structure> CASTLE = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "highland_castle"));
    private static final int SWARM = 16, RANGE = 56, PERIOD = 40;

    private CastleGhasts() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.isSpectator()) return;
        if ((player.tickCount + player.getId()) % PERIOD != 0) return;
        ServerLevel level = player.serverLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), h -> h.is(CASTLE));
        if (!start.isValid()) return;
        CastlePiece castle = null;
        for (StructurePiece piece : start.getPieces()) if (piece instanceof CastlePiece c) castle = c;
        if (castle == null) return;
        BlockPos o = castle.origin();
        int count = level.getEntitiesOfClass(TinyGhastEntity.class, new AABB(o).inflate(RANGE, RANGE, RANGE)).size();
        RandomSource random = level.getRandom();
        for (int i = 0, n = Math.min(SWARM - count, 1 + random.nextInt(3)); i < n; i++) {
            for (int tries = 0; tries < 8; tries++) {
                BlockPos pos = o.offset(random.nextInt(61) - 30, 4 + random.nextInt(30), random.nextInt(61) - 30);
                if (!level.isLoaded(pos) || !free(level, pos)) continue;
                // not right on top of anyone
                if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 6.0, false) != null) continue;
                TinyGhastEntity ghast = ModEntities.TINY_GHAST.get().create(level);
                if (ghast == null) return;
                ghast.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                ghast.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                level.addFreshEntity(ghast);
                break;
            }
        }
    }

    // room for a tiny ghast (a block across)
    private static boolean free(ServerLevel level, BlockPos pos) {
        for (int y = 0; y <= 1; y++) {
            BlockPos p = pos.above(y);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() || !level.getFluidState(p).isEmpty()) return false;
        }
        return true;
    }
}
