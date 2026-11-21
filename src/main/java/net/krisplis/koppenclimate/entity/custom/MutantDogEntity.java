package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The mutant dog that guards the Graphite ring in the basement vault of the abandoned Soviet estate (ChernobylPiece):
 * a big, mangy, toxic-green dog with yellow eyes, forever snarling. It attacks anything that lives on sight - players,
 * villagers, animals - fast and hard; it can't be tamed or fed; its bite poisons, sickens and starves; and it's
 * contagious: whatever it kills (but a player) gets up again as a mutant zombie (event/ChernobylLife).
 */
public class MutantDogEntity extends Wolf {
    public MutantDogEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes().add(Attributes.MAX_HEALTH, 44.0).add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 4.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.SCALE, 1.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.45F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.35, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Animal.class, 10, false, false,
                e -> !(e instanceof MutantDogEntity)));
    }

    // always snarling
    @Override
    public boolean isAngry() {
        return true;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Nullable
    @Override
    public Wolf getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1), this);
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1), this);
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0), this);
        }
        return hit;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + (this.random.nextFloat() - 0.5F) * 0.1F;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
