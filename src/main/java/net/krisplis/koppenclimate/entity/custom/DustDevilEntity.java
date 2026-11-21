package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Dust Devil: a small, faceless whirlwind of sand that roams the dry B lands by day and by night - and
 * round the Cursed Oases. It drifts after anyone it sees and, the moment it touches them, flings them high
 * into the air; the fall is what hurts. Arrows and blows scatter it like any other mob; it takes no fall damage.
 */
public class DustDevilEntity extends Monster {
    // straight up, about 20 blocks
    private static final double FLING = 2.1;
    private static final int COOLDOWN = 40;
    private int cooldown;

    public DustDevilEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.swirl();
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.15))) {
            if (player.isCreative() || player.isSpectator()) continue;
            this.fling(player);
            this.cooldown = COOLDOWN;
            break;
        }
    }

    // it doesn't strike: it throws (and the fall hurts)
    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.cooldown == 0 && target instanceof Player player) {
            this.fling(player);
            this.cooldown = COOLDOWN;
            return true;
        }
        return false;
    }

    private void fling(Player player) {
        Vec3 away = player.position().subtract(this.position()).multiply(1, 0, 1);
        Vec3 push = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(0.4);
        player.setDeltaMovement(push.x, FLING, push.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.0F, 0.8F);
        if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    player.getX(), player.getY() + 0.5, player.getZ(), 30, 0.5, 0.8, 0.5, 0.2);
        }
    }

    /** The sand it carries, spiralling up round it. */
    private void swirl() {
        RandomSource random = this.random;
        float h = this.getBbHeight();
        for (int i = 0; i < 2; i++) {
            double y = random.nextDouble() * h;
            double r = 0.15 + 0.5 * y / h;
            double a = this.tickCount * 0.6 + random.nextDouble() * Math.PI * 2;
            this.level().addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                    this.getX() + Math.cos(a) * r, this.getY() + y, this.getZ() + Math.sin(a) * r, 0.0, 0.0, 0.0);
        }
        if (random.nextInt(3) == 0) {
            this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    this.getX() + (random.nextDouble() - 0.5) * 0.8, this.getY() + 0.1, this.getZ() + (random.nextDouble() - 0.5) * 0.8,
                    0.0, 0.1, 0.0);
        }
    }

    /** Natural spawning: by day and by night, under the open sky, on the dry B lands' ground (the biomes see to the rest). */
    public static boolean checkDustDevilSpawnRules(EntityType<DustDevilEntity> type, ServerLevelAccessor level, MobSpawnType reason,
                                                   BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION) {
            return checkMobSpawnRules(type, level, reason, pos, random);
        }
        return level.canSeeSky(pos) && checkMobSpawnRules(type, level, reason, pos, random);
    }

    // sunlight doesn't keep it away (a monster's spawn check otherwise wants the dark)
    @Override
    public float getWalkTargetValue(BlockPos pos, net.minecraft.world.level.LevelReader level) {
        return 0.0F;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BREEZE_IDLE_GROUND;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BREEZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BREEZE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }
}
