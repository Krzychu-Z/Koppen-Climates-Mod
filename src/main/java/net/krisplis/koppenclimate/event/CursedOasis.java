package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.DustDevilEntity;
import net.krisplis.koppenclimate.village.CursedOasisPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Cursed Oasis's curse (see CursedOasisPiece): its pond is a mirage - step into the water and the whole pond
 * is gone in a hiss of steam, leaving a dry hollow (and the sugar cane round it withering). And by day, Dust Devils
 * whirl about it: up to three at a time round whoever is near it.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CursedOasis {
    public static final ResourceKey<Structure> OASIS = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "cursed_oasis"));
    private static final int MAX_DEVILS = 3;

    private CursedOasis() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level) || player.isSpectator()) return;
        boolean wet = player.tickCount % 5 == 0 && player.isInWater();
        boolean daySpawn = player.tickCount % 100 == 37 && level.isDay();
        if (!wet && !daySpawn) return;
        Structure oasis = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(OASIS);
        if (oasis == null) return;
        StructureStart start = level.structureManager().getStructureAt(player.blockPosition(), oasis);
        if (!start.isValid()) return;
        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof CursedOasisPiece pond)) continue;
            if (wet) vanish(level, player, pond);
            if (daySpawn) whirl(level, pond);
        }
    }

    /** Into the water? Then the whole pond goes, if it's this pond the water is. */
    private static void vanish(ServerLevel level, ServerPlayer player, CursedOasisPiece pond) {
        BlockPos c = pond.centre();
        double d = Mth.length(player.getX() - (c.getX() + 0.5), player.getZ() - (c.getZ() + 0.5));
        if (d > CursedOasisPiece.POND + 2.0 || Math.abs(player.getY() - c.getY()) > 3.0) return;
        int r = (int) Math.ceil(CursedOasisPiece.POND * 1.2) + 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int y = c.getY() - 3; y <= c.getY() + 1; y++) {
                    if (level.getBlockState(pos.set(c.getX() + dx, y, c.getZ() + dz)).is(Blocks.WATER)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        if (level.random.nextInt(4) == 0) {
                            level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 2, 0.3, 0.2, 0.3, 0.02);
                        }
                        any = true;
                    }
                }
            }
        }
        if (!any) return;
        level.playSound(null, c, SoundEvents.FIRE_EXTINGUISH, SoundSource.AMBIENT, 2.0F, 0.6F);
        player.displayClientMessage(Component.translatable("message.koppenclimatemod.cursed_oasis"), true);
    }

    /** By day, Dust Devils about the oasis: one more now and then, up to three. */
    private static void whirl(ServerLevel level, CursedOasisPiece pond) {
        BlockPos c = pond.centre();
        var area = new net.minecraft.world.phys.AABB(c).inflate(40.0, 20.0, 40.0);
        if (level.getEntitiesOfClass(DustDevilEntity.class, area).size() >= MAX_DEVILS) return;
        for (int attempt = 0; attempt < 6; attempt++) {
            double a = level.random.nextDouble() * Math.PI * 2, r = 10 + level.random.nextDouble() * 16;
            int x = c.getX() + (int) Math.round(Math.cos(a) * r), z = c.getZ() + (int) Math.round(Math.sin(a) * r);
            if (!level.hasChunkAt(new BlockPos(x, c.getY(), z))) continue;
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)
                    || !level.getFluidState(at).isEmpty()) continue;
            DustDevilEntity devil = ModEntities.DUST_DEVIL.get().create(level);
            if (devil == null) return;
            devil.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
            devil.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            level.addFreshEntity(devil);
            return;
        }
    }
}
