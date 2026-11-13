package net.krisplis.koppenclimate.client;

import net.krisplis.koppenclimate.item.custom.ShotgunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The shotgun's muzzle smoke as the shooter himself sees it: in first person out in front of his view, where the
 * gun's muzzle is drawn; in third person at the end of the barrel at his hip (as everyone else sees it).
 */
public final class ShotgunClient {
    private ShotgunClient() {
    }

    public static void muzzleSmoke(Player player, HumanoidArm arm) {
        Minecraft mc = Minecraft.getInstance();
        if (player != mc.player || mc.level == null) return;
        Vec3 at = mc.options.getCameraType().isFirstPerson() ? ShotgunItem.firstPersonMuzzle(player, arm)
                : ShotgunItem.thirdPersonMuzzle(player, arm);
        mc.level.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0.0, 0.01, 0.0);
    }
}
