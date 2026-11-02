package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.KoalaEntity;
import net.krisplis.koppenclimate.mixin.PlayerShoulderAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Koalas riding on players' shoulders (KoalaEntity). Once a second each one gets a second hungrier - a koala eats
 * only eucalyptus leaves, and up there it can't. At three quarters of its time the player is told it's getting
 * hungry; at half a day (KoalaEntity.STARVE_TICKS) it starves and is gone. Crouching sets your koalas down.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KoalaShoulders {
    private static final int STEP = 20;

    private KoalaShoulders() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % STEP != 0) return;
        CompoundTag left = player.getShoulderEntityLeft(), right = player.getShoulderEntityRight();
        boolean leftKoala = KoalaEntity.isKoala(left), rightKoala = KoalaEntity.isKoala(right);
        if (!leftKoala && !rightKoala) return;
        PlayerShoulderAccessor shoulders = (PlayerShoulderAccessor) player;
        // crouch, and they climb down
        if (player.isShiftKeyDown() && player.onGround()) {
            shoulders.koppenclimatemod$removeEntitiesOnShoulder();
            return;
        }
        if (leftKoala) shoulders.koppenclimatemod$setShoulderEntityLeft(hungrier(player, left.copy()));
        if (rightKoala) shoulders.koppenclimatemod$setShoulderEntityRight(hungrier(player, right.copy()));
    }

    /** The koala a second hungrier, or, when it has starved, nothing at all. */
    private static CompoundTag hungrier(ServerPlayer player, CompoundTag koala) {
        int hunger = koala.getInt(KoalaEntity.HUNGER_TAG) + STEP;
        if (hunger >= KoalaEntity.STARVE_TICKS) {
            player.serverLevel().sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.6, player.getZ(), 12, 0.3, 0.2, 0.3, 0.02);
            player.displayClientMessage(Component.translatable("message.koppenclimatemod.koala_starved"), true);
            return new CompoundTag();
        }
        if (hunger >= KoalaEntity.HUNGRY_TICKS && hunger - STEP < KoalaEntity.HUNGRY_TICKS) {
            player.displayClientMessage(Component.translatable("message.koppenclimatemod.koala_hungry"), true);
        }
        koala.putInt(KoalaEntity.HUNGER_TAG, hunger);
        return koala;
    }
}
