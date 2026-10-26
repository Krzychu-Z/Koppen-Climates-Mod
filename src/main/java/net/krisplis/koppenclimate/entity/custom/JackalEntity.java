package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The jackal: a wolf of the savanna, a little bigger, in a sandy coat with a dark back. Jackals live in packs and
 * are never quiet - yipping, whining and howling. By day they keep away from players; at night the pack hunts
 * them, and villagers and every peaceful animal too. They can't be tamed.
 */
public class JackalEntity extends Wolf {
    public JackalEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    private boolean night() {
        return !this.level().isDay();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // by day: keep away from players
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 12.0F, 1.0, 1.3, p -> !this.night()) {
            @Override
            public boolean canUse() {
                return !JackalEntity.this.night() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(JackalEntity.class));
        // by night: the pack goes for players
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p -> this.night()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.npc.AbstractVillager.class, 10, true, false, v -> this.night()));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.animal.Animal.class, 10, true, false,
                a -> this.night() && isPrey(a)));
    }

    @Override
    public void tick() {
        super.tick();
        // come the morning, the pack gives up the hunt
        if (!this.level().isClientSide && !this.night() && this.getTarget() != null && this.getLastHurtByMob() != this.getTarget()) {
            this.setTarget(null);
        }
    }

    // the peaceful animals: not other hunters, and not anyone's pet
    private static boolean isPrey(net.minecraft.world.entity.LivingEntity e) {
        if (e instanceof Wolf || e instanceof LionEntity || e instanceof net.minecraft.world.entity.animal.PolarBear) return false;
        if (e instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame()) return false;
        if (e instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()) return false;
        return !(e instanceof net.minecraft.world.entity.monster.Enemy);
    }

    // they can't be tamed or fed
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    // never quiet: yips and whines by day, growls and howls by night
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.getTarget() != null) return SoundEvents.WOLF_GROWL;
        int r = this.random.nextInt(4);
        if (this.night()) return r == 0 ? SoundEvents.WOLF_HOWL : r == 1 ? SoundEvents.WOLF_GROWL : SoundEvents.WOLF_AMBIENT;
        return r == 0 ? SoundEvents.WOLF_WHINE : SoundEvents.WOLF_AMBIENT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 50;
    }

    @Override
    public float getVoicePitch() {
        return 1.25F + this.random.nextFloat() * 0.2F;
    }
}
