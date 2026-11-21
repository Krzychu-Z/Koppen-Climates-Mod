package net.krisplis.koppenclimate.item.custom;

import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The shotgun, found hidden under the hay in the red farms' barns (RedFarmPiece). Each shot uses up one of the
 * cowboy souls' copper bullets (ModItems.BULLET: the same ammunition feeds every gun); it hits the first creature in
 * its line of fire, up to 32 blocks away (shattering glass and suchlike on the way), for eight hearts, and throws it back - about three blocks for a creature of a man's size, less the heavier
 * it is (by the size of its body, and its own resistance to being knocked back), a little more for small game. It
 * goes off with a loud bang (sounds/item/shotgun.ogg) and one small puff of smoke at the muzzle - where the muzzle is
 * as each one sees it: the shooter in first person sees it out in front of his view, everyone else (and the shooter in
 * third person) at the end of the barrel held at his hip. Skeletons of every kind go down in one shot. Then it must be
 * reloaded (a second and a half).
 */
public class ShotgunItem extends Item {
    private static final double RANGE = 32.0;
    private static final float DAMAGE = 16.0F;
    // the throw: about three blocks back for a body of a man's size, with a little lift
    private static final double PUSH = 0.95, LIFT = 0.35;
    // a man's "mass" (the volume of the hitbox: 0.6 x 0.6 x 1.8), and how far lighter game may be thrown beyond his
    private static final double MAN = 0.648, MAX_FACTOR = 1.25, MIN_FACTOR = 0.05;
    private static final int RELOAD = 30;

    public ShotgunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        ItemStack ammo = this.findAmmo(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.6F);
            return InteractionResultHolder.fail(gun);
        }
        net.minecraft.world.entity.HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        if (level.isClientSide) {
            // the shooter's own puff of smoke, where his camera shows the muzzle
            net.krisplis.koppenclimate.client.ShotgunClient.muzzleSmoke(player, arm);
        } else {
            this.fire((ServerLevel) level, player, arm);
            if (!player.getAbilities().instabuild) {
                ammo.shrink(1);
                gun.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
        }
        player.getCooldowns().addCooldown(this, RELOAD);
        // (consumed, not a success: the gun isn't swung like a tool when it fires, it stays levelled at the crosshair)
        return InteractionResultHolder.consume(gun);
    }

    /**
     * How hard a hit throws this creature, against a man's 1: by its mass (taken as the volume of its body, so twice
     * as heavy is thrown about 0.7 as far, an elephant hardly at all, a chicken a little further than a man), and less
     * again by its own knockback resistance.
     */
    static double throwFactor(Entity target) {
        double mass = Math.max(0.01, target.getBbWidth() * target.getBbWidth() * target.getBbHeight());
        double k = Math.sqrt(MAN / mass);
        if (target instanceof LivingEntity living) {
            k *= 1.0 - Math.min(1.0, living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE));
        }
        return Math.max(MIN_FACTOR, Math.min(MAX_FACTOR, k));
    }

    private ItemStack findAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.BULLET.get())) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** Where the muzzle is in first person: out in front of the eyes, low and to the side of the gun hand. */
    public static Vec3 firstPersonMuzzle(Player player, net.minecraft.world.entity.HumanoidArm arm) {
        Vec3 look = player.getViewVector(1.0F);
        Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize().scale(arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.35 : -0.35);
        return player.getEyePosition().add(look.scale(1.4)).add(side).add(0.0, -0.25, 0.0);
    }

    /** Where the muzzle is seen from outside: the end of the barrel, held level at the hip, out ahead of the body. */
    public static Vec3 thirdPersonMuzzle(Player player, net.minecraft.world.entity.HumanoidArm arm) {
        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 forward = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        double s = arm == net.minecraft.world.entity.HumanoidArm.RIGHT ? 0.35 : -0.35;
        return player.position().add(forward.scale(1.4)).add(right.scale(s)).add(0.0, player.isCrouching() ? 0.55 : 0.75, 0.0);
    }

    private void fire(ServerLevel level, Player player, net.minecraft.world.entity.HumanoidArm arm) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(RANGE));
        // stopped by the first solid block - shattering any glass, pumpkins, pots, lanterns and the like on its way
        // (Kar98kItem.breaks)
        BlockHitResult block;
        Vec3 from = start;
        for (int pierced = 0; ; pierced++) {
            block = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (block.getType() == HitResult.Type.MISS || pierced >= 16
                    || !Kar98kItem.breaks(level.getBlockState(block.getBlockPos()))) break;
            level.destroyBlock(block.getBlockPos(), true, player);
            from = block.getLocation().add(look.scale(0.05));
        }
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, start, end,
                new AABB(start, end).inflate(1.0), e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player, 0.3F);
        if (hit != null) end = hit.getLocation();

        // the bang: loud, and heard a long way off (a volume of 4 carries it 64 blocks)
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.SHOTGUN.get(), SoundSource.PLAYERS,
                4.0F, 0.95F + level.random.nextFloat() * 0.1F);
        // one small puff of smoke at the muzzle, for everyone else watching (the shooter makes his own: see use)
        Vec3 muzzle = thirdPersonMuzzle(player, arm);
        for (net.minecraft.server.level.ServerPlayer other : level.players()) {
            if (other != player && other.distanceToSqr(muzzle) < 64.0 * 64.0) {
                level.sendParticles(other, ParticleTypes.SMOKE, false, muzzle.x, muzzle.y, muzzle.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        if (hit != null) {
            Entity target = hit.getEntity();
            // skeletons of every kind are blown apart by a single shot
            float damage = target.getType().is(net.minecraft.tags.EntityTypeTags.SKELETONS) && target instanceof LivingEntity living
                    ? Math.max(DAMAGE, living.getMaxHealth() * 4.0F) : DAMAGE;
            if (target.hurt(level.damageSources().playerAttack(player), damage)) {
                double k = throwFactor(target);
                Vec3 push = new Vec3(look.x, 0.0, look.z).normalize().scale(PUSH * k);
                target.push(push.x, LIFT * Math.min(1.0, k), push.z);
                target.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, end.x, end.y, end.z, 6, 0.2, 0.2, 0.2, 0.1);
        } else if (block.getType() != HitResult.Type.MISS) {
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 4, 0.1, 0.1, 0.1, 0.01);
        }
    }
}
