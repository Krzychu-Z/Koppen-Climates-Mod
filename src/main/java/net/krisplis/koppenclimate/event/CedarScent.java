package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.effect.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What the Cedar Scent effect (cedar perfume) does: every friendly animal and villager within 16 blocks is drawn to
 * the player and follows them about, and every mob that could harm the player keeps away - monsters, but just as
 * much the predators and anything else that bites, kicks or stings (lions, crocodiles, kangaroos, bears, wolves,
 * bees...): it gives up on the player, backs off, and won't pick them as a target while the scent lasts. Tamed
 * animals and golems are the player's friends, and stay so.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CedarScent {
    private static final double RANGE = 16.0, KEEP_AWAY = 12.0;

    private CedarScent() {
    }

    /** Whether this mob could do the player harm: a monster, or anything wild that can attack. */
    public static boolean isDangerous(Mob mob) {
        if (mob instanceof AbstractGolem || mob instanceof AbstractVillager) return false;
        if (mob instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return false;
        // gentle creatures that only use their teeth on other animals (or never)
        if (mob instanceof net.minecraft.world.entity.animal.Rabbit rabbit) {
            return rabbit.getVariant() == net.minecraft.world.entity.animal.Rabbit.Variant.EVIL;
        }
        if (mob instanceof net.minecraft.world.entity.animal.Fox || mob instanceof net.minecraft.world.entity.animal.Cat
                || mob instanceof net.minecraft.world.entity.animal.Ocelot || mob instanceof net.minecraft.world.entity.animal.axolotl.Axolotl
                || mob instanceof net.minecraft.world.entity.animal.Dolphin) {
            return false;
        }
        return mob instanceof Enemy || mob instanceof NeutralMob || mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE);
    }

    public static boolean isScented(LivingEntity entity) {
        return entity instanceof Player && entity.hasEffect(ModEffects.CEDAR_SCENT.getHolder().get());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 10 != 3) return;
        if (!isScented(player) || !(player.level() instanceof ServerLevel level)) return;
        AABB area = player.getBoundingBox().inflate(RANGE, 6.0, RANGE);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (isDangerous(mob)) {
                if (mob.getTarget() == player) mob.setTarget(null);
                if (mob instanceof PathfinderMob walker && mob.distanceToSqr(player) < KEEP_AWAY * KEEP_AWAY) {
                    Vec3 away = DefaultRandomPos.getPosAway(walker, 16, 7, player.position());
                    if (away != null) walker.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
                }
            } else if ((mob instanceof Animal || mob instanceof AbstractVillager) && !mob.isPassenger() && !mob.isLeashed()) {
                if (mob.distanceToSqr(player) > 9.0) mob.getNavigation().moveTo(player, 1.1);
                mob.getLookControl().setLookAt(player, 10.0F, mob.getMaxHeadXRot());
            }
        }
    }

    // nothing dangerous takes a scented player as its target
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && isDangerous(mob) && event.getNewTarget() != null && isScented(event.getNewTarget())) {
            event.setCanceled(true);
        }
    }
}
