package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.village.VillageRegistry;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Mediterranean's towns (the Tuscan towns and the Dalmatian island towns) are never raided. Bad Omen does
 * nothing anywhere in the Csa or in one of its towns (BadOmenMixin) - the player just keeps it - a Raid Omen picked
 * up for one of them anyway is lifted, and should a raid begin at one, it's called off at once.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CsaVillagePeace {
    // how far from a spot to look for the Venetian villagers that make it one of these towns
    private static final double TOWNSFOLK_RANGE = 48.0;

    private CsaVillagePeace() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || !(event.player instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) return;
        BlockPos pos = player.blockPosition();
        if (player.hasEffect(MobEffects.RAID_OMEN)) {
            BlockPos omen = player.getRaidOmenPosition();
            if (isCsaTown(level, omen != null ? omen : pos)) {
                player.removeEffect(MobEffects.RAID_OMEN);
                player.clearRaidOmenPosition();
            }
        }
        if (player.tickCount % 20 == 0) {
            Raid raid = level.getRaidAt(pos);
            if (raid != null && !raid.isStopped() && isCsaTown(level, raid.getCenter())) raid.stop();
        }
    }

    /** A town in the Mediterranean (Csa), or one whose villagers are Venetian (the island towns stand in the sea). */
    public static boolean isCsaTown(ServerLevel level, BlockPos pos) {
        if (ModBiomes.isMediterranean(level.getBiome(pos))) return true;
        return !level.getEntitiesOfClass(Villager.class, new AABB(pos).inflate(TOWNSFOLK_RANGE),
                v -> v.getVillagerData().getType() == VillageRegistry.VENETIAN.get()).isEmpty();
    }
}
