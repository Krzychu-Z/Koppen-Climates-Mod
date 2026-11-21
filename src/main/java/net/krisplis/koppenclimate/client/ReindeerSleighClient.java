package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.custom.ReindeerEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Flying a boat with reindeer harnessed to it (the boat is steered by the one sitting in it, so its flight is worked
 * out here, on their side). Forward flies the way you look - look up to climb, down to come down - at a good
 * gallop; jump climbs straight up; back slows and backs; let go and it hovers where it is. Left and right turn
 * the boat as on water. The team gallops ahead of the boat, in their places.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ReindeerSleighClient {
    private static final double SPEED = 0.75, CLIMB = 0.35, EASE = 0.12;
    // a boat in the air loses this much of its speed each tick (the flight makes up for it)
    private static final double AIR_FRICTION = 0.9;

    private ReindeerSleighClient() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.isPaused()) return;
        if (!(player.getVehicle() instanceof Boat boat) || boat.getControllingPassenger() != player) return;
        List<ReindeerEntity> team = ReindeerEntity.team(boat);
        if (team.isEmpty()) return;
        if (event.phase == TickEvent.Phase.END) {
            // the team in their places, after everything has moved this tick
            ReindeerEntity.harness(boat, team);
            return;
        }
        if (!boat.isNoGravity()) return;
        float yaw = boat.getYRot() * Mth.DEG_TO_RAD, pitch = player.getXRot() * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        Vec3 want = Vec3.ZERO;
        if (mc.options.keyUp.isDown()) want = look.scale(SPEED * team.size() / (team.size() + 0.5));
        else if (mc.options.keyDown.isDown()) want = look.scale(-0.2);
        if (mc.options.keyJump.isDown()) want = want.add(0.0, CLIMB, 0.0);
        Vec3 now = boat.getDeltaMovement();
        Vec3 next = now.add(want.subtract(now).scale(EASE));
        boat.setDeltaMovement(next.scale(1.0 / AIR_FRICTION));
        boat.resetFallDistance();
    }
}
