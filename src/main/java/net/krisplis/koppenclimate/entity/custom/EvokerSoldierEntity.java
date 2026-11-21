package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.item.custom.Kar98kItem;
import net.krisplis.koppenclimate.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * An evoker in a field-grey army uniform and steel helmet (no insignia), occupying the French village
 * (FrenchVillagePiece). No spells: he carries a Kar98k and shoots. Seeing a player within 40 blocks he stops, raises
 * the rifle to his shoulder, takes aim for a second and fires - a real bullet, stopped by walls, which can miss
 * (the further off, the likelier), and which armour softens: four hearts on a hit (damage type
 * koppenclimatemod:rifle_shot). Then he works the bolt (two to four seconds) and aims again. Out of sight, he goes
 * after you. His rifle never drops (the village's four are hidden about it); he may drop a few cartridges.
 */
public class EvokerSoldierEntity extends AbstractIllager {
    private static final double RANGE = 40.0;
    private static final float DAMAGE = 8.0F;

    public EvokerSoldierEntity(EntityType<? extends AbstractIllager> type, Level level) {
        super(type, level);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setCanJoinRaid(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RifleGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 15.0F, 1.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, Raider.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KAR98K.get()));
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, DifficultyInstance difficulty,
                                                                     net.minecraft.world.entity.MobSpawnType reason,
                                                                     @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return result;
    }

    @Override
    public IllagerArmPose getArmPose() {
        return this.isAggressive() ? IllagerArmPose.CROSSBOW_HOLD : IllagerArmPose.NEUTRAL;
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean unused) {
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    /** One shot at the target: from the shoulder, straying more the further off it is. */
    void fireAt(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel level)) return;
        Vec3 start = this.getEyePosition();
        Vec3 aim = new Vec3(target.getX(), target.getY(0.6), target.getZ()).subtract(start).normalize();
        float spread = 1.2F + (float) this.distanceTo(target) * 0.07F;
        Vec3 dir = Kar98kItem.stray(aim, spread, this.random);
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(Kar98kItem.RIFLE_SHOT), this);
        Kar98kItem.shoot(level, this, start, dir, RANGE + 8.0, source, DAMAGE);
        level.playSound(null, this.getX(), this.getEyeY(), this.getZ(), ModSounds.SHOTGUN.get(), SoundSource.HOSTILE, 4.0F,
                1.3F + this.random.nextFloat() * 0.15F);
        Vec3 muzzle = start.add(aim.scale(1.4)).add(0.0, -0.2, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.03, 0.03, 0.03, 0.005);
    }

    /** Stop, shoulder the rifle, aim, fire; work the bolt; again. Close the distance when he can't see or reach. */
    static class RifleGoal extends Goal {
        private final EvokerSoldierEntity soldier;
        private int aim, bolt, seen;

        RifleGoal(EvokerSoldierEntity soldier) {
            this.soldier = soldier;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.soldier.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void stop() {
            this.soldier.setAggressive(false);
            this.aim = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = this.soldier.getTarget();
            if (target == null) return;
            if (this.bolt > 0) this.bolt--;
            double d = this.soldier.distanceTo(target);
            boolean sees = this.soldier.getSensing().hasLineOfSight(target);
            this.seen = sees ? Math.min(this.seen + 1, 40) : 0;
            this.soldier.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (sees && d <= RANGE) {
                // in range and in sight: stand (or close in a little from far off) and take aim
                if (d < 26.0 || this.seen < 5) this.soldier.getNavigation().stop();
                else this.soldier.getNavigation().moveTo(target, 0.8);
                this.soldier.setAggressive(true);
                this.aim++;
                if (this.aim >= 20 && this.bolt == 0) {
                    this.soldier.fireAt(target);
                    this.bolt = 40 + this.soldier.getRandom().nextInt(40);
                    this.aim = 0;
                }
            } else {
                this.aim = 0;
                this.soldier.setAggressive(false);
                this.soldier.getNavigation().moveTo(target, 1.0);
            }
        }
    }
}
