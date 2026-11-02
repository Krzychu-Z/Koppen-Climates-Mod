package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * The grizzly bear of the Redwood forest (Csb): a big brown bear, built like the polar bear (and standing up on its
 * hind legs to fight the same way), but it minds its own business. It attacks a player only if they hit it - or if
 * they stare it in the eyes, as an enderman does: look straight at its head from within a dozen blocks and it comes
 * for you. It leaves foxes alone.
 */
public class GrizzlyBearEntity extends PolarBear {
    private static final double STARE_RANGE = 12.0;

    public GrizzlyBearEntity(EntityType<? extends PolarBear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createGrizzlyAttributes() {
        return PolarBear.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.25));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // a player who stares it in the eyes
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, true, false,
                target -> target instanceof Player player && this.isStaredAtBy(player)));
        this.targetSelector.addGoal(3, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    /** Whether the player is looking it straight in the eyes, as the enderman checks it. */
    boolean isStaredAtBy(Player player) {
        if (player.isCreative() || player.isSpectator() || this.isBaby()) return false;
        if (this.distanceToSqr(player) > STARE_RANGE * STARE_RANGE) return false;
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 toEyes = new Vec3(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
        double distance = toEyes.length();
        if (distance < 1.0E-4) return false;
        return view.dot(toEyes.normalize()) > 1.0 - 0.025 / distance && player.hasLineOfSight(this);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // it rears up on its hind legs when it's about to strike
        LivingEntity target = this.getTarget();
        this.setStanding(target != null && this.distanceToSqr(target) < 9.0);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.GRIZZLY_BEAR.get().create(level);
    }
}
