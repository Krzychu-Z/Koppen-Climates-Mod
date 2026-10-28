package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A little red crab of the warm beaches, about a scorpion's size. It minds its own business, but pinches back at
 * whatever hurts it. Hold a coconut and it follows you, pincers raised; feed two of them coconuts and they breed.
 */
public class CrabEntity extends Animal {
    private static final EntityDataAccessor<Boolean> CLAWS_UP = SynchedEntityData.defineId(CrabEntity.class, EntityDataSerializers.BOOLEAN);
    private TemptGoal temptGoal;

    public CrabEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 10.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CLAWS_UP, false);
    }

    /** Whether it holds its pincers up: following a coconut, or angry. */
    public boolean clawsUp() {
        return this.entityData.get(CLAWS_UP);
    }

    @Override
    protected void registerGoals() {
        this.temptGoal = new TemptGoal(this, 1.1, CrabEntity::isCoconut, false);
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, false));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, this.temptGoal);
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(CrabEntity.class));
    }

    private static boolean isCoconut(ItemStack stack) {
        return stack.is(ModItems.COCONUT.get());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.getTarget() != null && !this.getTarget().isAlive()) this.setTarget(null);
        this.entityData.set(CLAWS_UP, this.temptGoal.isRunning() || this.getTarget() != null);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return isCoconut(stack);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.CRAB.get().create(level);
    }

    // it lives at the water's edge
    @Override
    public boolean canDrownInFluidType(net.minecraftforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** Like turtles: on the sand (or shingle) of a beach, not far above the sea. */
    public static boolean checkCrabSpawnRules(EntityType<CrabEntity> type, LevelAccessor level, MobSpawnType reason,
                                              BlockPos pos, RandomSource random) {
        BlockState ground = level.getBlockState(pos.below());
        // sand, or the white shingle of the Mediterranean beaches
        boolean beach = ground.is(BlockTags.SAND) || ground.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                || ground.is(net.krisplis.koppenclimate.block.ModBlocks.WHITE_GRAVEL.get());
        return pos.getY() < level.getSeaLevel() + 4 && beach && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.TURTLE_HURT_BABY;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TURTLE_DEATH_BABY;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.TURTLE_SHAMBLE_BABY, 0.15F, 1.3F);
    }
}
