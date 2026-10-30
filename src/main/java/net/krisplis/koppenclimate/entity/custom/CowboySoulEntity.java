package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The cowboy soul: the restless dead of the ghost towns (GhostTownPiece). A zombie of a gunslinger in his hat and
 * long coat, grey and gaunt, with great empty eye sockets, that drifts about a block above the ground instead of
 * walking, trailing soul wisps, glowing in the dark. Once it has seen you it whirls round and flies straight at you, screaming like a ghast; its
 * touch takes two hearts and withers you for a moment. Fire can't hurt it. It drops twelve bullets, and
 * sometimes gold or a diamond. It comes out only at night (it's a monster of the ghost town biome) and fades away
 * at sunrise; it never burns, never drowns, never turns into anything, and never calls other zombies for help.
 */
public class CowboySoulEntity extends Zombie {
    // how high it floats over the ground, and how much it bobs
    private static final double HOVER = 0.9, BOB = 0.15;
    // how fast it flies at its prey once it has seen it (about 7.5 blocks a second: it outruns a sprinting player),
    // how much of its old drift it keeps each tick, and how fast it whirls round to face its prey (degrees a tick)
    private static final double CHASE_SPEED = 0.4, CHASE_KEEP = 0.3;
    private static final float TURN_SPEED = 60.0F;
    private boolean chasing;
    // which way it faces while chasing (kept here, so its path-following steering can't turn it away from you)
    private float chaseYaw;
    private int screamIn;

    public CowboySoulEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void randomizeReinforcementsChance() {
        this.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0.0);
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();
        if (this.screamIn > 0) this.screamIn--;
        net.minecraft.world.entity.LivingEntity prey = this.getTarget();
        if (!this.level().isClientSide && prey != null && prey.isAlive() && this.hasLineOfSight(prey)) {
            // it has seen you: it whirls round to face you and flies straight at you, screaming like a ghast
            boolean starting = !this.chasing;
            // (a soul that keeps losing sight of you round corners doesn't scream at every glimpse)
            if (starting ? this.screamIn <= 40 : this.screamIn <= 0) {
                this.playSound(ModSounds.COWBOY_SOUL_SCREAM.get(), 1.6F, 0.85F + this.random.nextFloat() * 0.15F);
                this.screamIn = 60 + this.random.nextInt(60);
            }
            this.chasing = true;
            if (starting) this.chaseYaw = this.yBodyRot;
            this.faceTowards(prey);
            Vec3 to = prey.getEyePosition().subtract(0.0, 0.6, 0.0).subtract(this.position().add(0.0, 1.0, 0.0));
            if (to.lengthSqr() > 1.0E-4) {
                Vec3 v = to.normalize().scale(CHASE_SPEED);
                // at the start of the chase it drops whatever way it was drifting and lunges at you at once
                Vec3 keep = starting ? Vec3.ZERO : this.getDeltaMovement().scale(CHASE_KEEP);
                this.setDeltaMovement(keep.add(v.scale(starting ? 1.0 : 1.0 - CHASE_KEEP)));
            }
        } else {
            this.chasing = false;
            // otherwise it floats above the ground, bobbing gently
            double ground = this.groundBelow();
            double target = ground + HOVER + BOB * Math.sin(this.tickCount * 0.08 + this.getId());
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(v.x, (target - this.getY()) * 0.12, v.z);
        }
        if (this.level().isClientSide) {
            if (this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.SOUL, this.getRandomX(0.5), this.getY() + this.random.nextDouble() * 0.4,
                        this.getRandomZ(0.5), 0.0, -0.02, 0.0);
            }
            return;
        }
        // at sunrise it fades away
        if (this.level().isDay() && this.tickCount % 20 == 0 && !this.hasCustomName()) {
            ((ServerLevel) this.level()).sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            this.playSound(SoundEvents.SOUL_ESCAPE.value(), 1.0F, 0.8F);
            this.discard();
        }
    }

    /** Turns its body, head and gaze towards its prey, quickly (it doesn't fly at you backwards). */
    private void faceTowards(net.minecraft.world.entity.LivingEntity prey) {
        double dx = prey.getX() - this.getX(), dz = prey.getZ() - this.getZ();
        double dy = prey.getEyeY() - this.getEyeY();
        float yaw = (float) (net.minecraft.util.Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(net.minecraft.util.Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI));
        float y = this.chaseYaw = net.minecraft.util.Mth.approachDegrees(this.chaseYaw, yaw, TURN_SPEED);
        this.setYRot(y);
        this.yBodyRot = y;
        this.yHeadRot = y;
        this.setXRot(net.minecraft.util.Mth.approach(this.getXRot(), pitch, TURN_SPEED));
        this.getLookControl().setLookAt(prey, 180.0F, 180.0F);
    }

    // in the air a mob's own steering is weak (vanilla gives it 0.02, a crawl): it drifts about briskly, and when it
    // is after you but can't see you it follows its path at a fast float; once it sees you, tick() does the flying
    @Override
    protected float getFlyingSpeed() {
        if (this.chasing) return 0.0F;
        return this.getSpeed() * (this.getTarget() != null ? 0.5F : 0.2F);
    }

    /** The top of the ground under it (up to 6 blocks down). */
    private double groundBelow() {
        BlockPos.MutableBlockPos pos = this.blockPosition().mutable();
        for (int i = 0; i < 6; i++) {
            BlockState s = this.level().getBlockState(pos);
            if (!s.getCollisionShape(this.level(), pos).isEmpty() || !s.getFluidState().isEmpty()) {
                double top = s.getCollisionShape(this.level(), pos).isEmpty() ? 1.0 : s.getCollisionShape(this.level(), pos).max(net.minecraft.core.Direction.Axis.Y);
                return pos.getY() + top;
            }
            pos.move(0, -1, 0);
        }
        return this.getY() - HOVER;
    }

    // it never touches the ground, so it keeps working out its way as it drifts along
    @Override
    protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        return new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this, level) {
            @Override
            protected boolean canUpdatePath() {
                return true;
            }
        };
    }

    @Override
    protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, net.minecraft.world.DifficultyInstance difficulty) {
        // no weapons: its touch is enough
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    // its touch takes two hearts, and withers you for a moment
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof net.minecraft.world.entity.LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 60, 0), this);
        }
        return hit;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean canBreakDoors() {
        return false;
    }

    @Override
    protected boolean supportsBreakDoorGoal() {
        return false;
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {
    }

    @Override
    protected ItemStack getSkull() {
        return ItemStack.EMPTY;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F + this.random.nextFloat() * 0.15F;
    }

    // its voice is a ghast's: moaning as it drifts, screaming when hurt, and the ghast's death cry as it goes
    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? SoundEvents.SOUL_ESCAPE.value() : ModSounds.COWBOY_SOUL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.COWBOY_SOUL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.COWBOY_SOUL_DEATH.get();
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        // it floats: no footsteps
    }
}
