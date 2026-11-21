package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * The reindeer of the Subpolar taiga (Dfc) and the Tundra (ET): shy herd animals. Put one on a lead, sneak and
 * click a boat with it and it is harnessed to the boat - and a boat with reindeer harnessed to it flies (see
 * ReindeerSleigh): they gallop through the air ahead of it, up to four, two by two. Click a harnessed reindeer to
 * take it back on your lead.
 */
public class ReindeerEntity extends Animal {
    public static final int MAX_TEAM = 4;
    @Nullable
    private Boat harnessedTo;

    public ReindeerEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.8));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.2, Ingredient.of(Items.WHEAT, Items.MOSS_BLOCK), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** The boat this reindeer is harnessed to (on its lead), if any. */
    @Nullable
    public Boat sleigh() {
        return this.getLeashHolder() instanceof Boat boat && boat.isAlive() ? boat : null;
    }

    /** Galloping through the air before a flying boat. */
    public boolean isFlying() {
        return this.isNoGravity() && !this.onGround() && this.sleigh() != null;
    }

    /** The reindeer harnessed to this boat, in their places (two by two, the first pair nearest the boat). */
    public static List<ReindeerEntity> team(Boat boat) {
        return boat.level().getEntitiesOfClass(ReindeerEntity.class, boat.getBoundingBox().inflate(12.0), r -> r.sleigh() == boat)
                .stream().sorted(Comparator.comparingInt(Entity::getId)).limit(MAX_TEAM).toList();
    }

    /** Puts the team in their places ahead of the boat, facing its way. */
    public static void harness(Boat boat, List<ReindeerEntity> team) {
        float yaw = boat.getYRot();
        double fx = -Math.sin(Math.toRadians(yaw)), fz = Math.cos(Math.toRadians(yaw));
        for (int i = 0; i < team.size(); i++) {
            ReindeerEntity deer = team.get(i);
            double ahead = 2.8 + 1.9 * (i / 2), side = team.size() == 1 ? 0.0 : (i % 2 == 0 ? 0.65 : -0.65);
            double x = boat.getX() + fx * ahead - fz * side, z = boat.getZ() + fz * ahead + fx * side;
            deer.setPos(x, boat.getY() + 0.15, z);
            deer.setYRot(yaw);
            deer.setYHeadRot(yaw);
            deer.yBodyRot = yaw;
            deer.setDeltaMovement(boat.getDeltaMovement());
            deer.resetFallDistance();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        Boat boat = this.sleigh();
        if (boat != null) {
            this.harnessedTo = boat;
            this.setNoGravity(true);
            this.getNavigation().stop();
            boat.setNoGravity(true);
            boat.resetFallDistance();
            List<ReindeerEntity> team = team(boat);
            if (!team.isEmpty() && team.get(0) == this) harness(boat, team);
        } else if (this.harnessedTo != null) {
            // unharnessed: back to earth, and the boat with it once the last of the team is off it
            this.setNoGravity(false);
            Boat old = this.harnessedTo;
            this.harnessedTo = null;
            if (old.isAlive() && team(old).isEmpty()) old.setNoGravity(false);
        }
    }

    // a harnessed reindeer comes back to your lead when you click it
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && this.sleigh() != null) {
            if (!this.level().isClientSide) this.setLeashedTo(player, true);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return this.sleigh() == null && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, this.getEyeHeight() * 0.6, this.getBbWidth() * 0.4);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.MOSS_BLOCK);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.REINDEER.get().create(level);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GOAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GOAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GOAT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75F;
    }
}
