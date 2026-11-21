package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A mutant zombie, of the abandoned Soviet estate (ChernobylPiece): sallow, blotched with sores, in the rags of a
 * work jacket - and either two-headed (a second head lolling on its shoulder) or three-legged (a third leg dragging
 * between the other two): see client/MutantZombieRenderer. The sun doesn't burn it; water doesn't drown it into
 * anything else; it's tougher than a zombie, and its blows poison. Never a baby, never a jockey.
 */
public class MutantZombieEntity extends Zombie {
    public static final int TWO_HEADED = 0, THREE_LEGGED = 1;
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(MutantZombieEntity.class, EntityDataSerializers.INT);

    public MutantZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes().add(Attributes.MAX_HEALTH, 28.0).add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, TWO_HEADED);
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, variant);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        this.setBaby(false);
        if (this.getVehicle() != null) {
            Entity mount = this.getVehicle();
            this.stopRiding();
            mount.discard();
        }
        this.setVariant(this.random.nextInt(2));
        return result;
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
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.POISON, 140, 1), this);
        return hit;
    }

    @Override
    public float getVoicePitch() {
        return 0.65F + (this.random.nextFloat() - 0.5F) * 0.15F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Mutation", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Mutation"));
    }
}
