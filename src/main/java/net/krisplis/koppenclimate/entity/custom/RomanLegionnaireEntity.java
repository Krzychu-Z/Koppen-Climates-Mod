package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;

/**
 * A Roman Legionnaire: one of Pompeii's dead, still keeping the burning town (PompeiiPiece, the Pompeii biome). A
 * skeleton in the legion's red armour and crested iron helmet, his eyes glowing like embers. He fights with an iron
 * sword (gladius), and bears a Roman shield - a red scutum with its gold boss and rim - which he raises against
 * anyone keeping their distance (or shooting at him), lowering it to strike. Now and then one drops his shield, or
 * his sword (well used), as any mob drops what it holds.
 *
 * <p>The fire doesn't touch him, nor does the sun: he walks Pompeii by day as by night.
 */
public class RomanLegionnaireEntity extends AbstractSkeleton {
    // how long the shield stays up, and how long before he raises it again
    private int shieldTicks, shieldCooldown;

    public RomanLegionnaireEntity(EntityType<? extends AbstractSkeleton> type, Level level) {
        super(type, level);
        // the fire doesn't touch him, so he walks right through it
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.DAMAGE_FIRE, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSkeleton.createAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /** The Roman shield: a shield painted as a red scutum, with a gold rim, spine and boss. */
    public static ItemStack romanShield(HolderGetter<BannerPattern> patterns) {
        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.set(DataComponents.BASE_COLOR, DyeColor.RED);
        shield.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers.Builder()
                .addIfRegistered(patterns, BannerPatterns.STRIPE_CENTER, DyeColor.YELLOW)
                .addIfRegistered(patterns, BannerPatterns.CIRCLE_MIDDLE, DyeColor.ORANGE)
                .addIfRegistered(patterns, BannerPatterns.BORDER, DyeColor.YELLOW)
                .build());
        shield.set(DataComponents.ITEM_NAME, Component.translatable("item.koppenclimatemod.roman_shield"));
        return shield;
    }

    // no fleeing the sun, nor wolves: a legionnaire holds his ground
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setItemSlot(EquipmentSlot.OFFHAND, romanShield(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
    }

    // the sun doesn't burn him
    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    // he comes out by day as by night (and on Pompeii's burning ground: see its spawn placement)
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) return;
        if (this.shieldCooldown > 0) this.shieldCooldown--;
        LivingEntity target = this.getTarget();
        boolean shield = this.getOffhandItem().is(Items.SHIELD);
        if (this.isUsingItem()) {
            // lowered to strike, once close enough; or after a while
            if (!shield || target == null || !target.isAlive() || this.distanceToSqr(target) < 2.8 * 2.8 || --this.shieldTicks <= 0) {
                this.stopUsingItem();
                this.shieldCooldown = 30 + this.random.nextInt(30);
            }
        } else if (shield && target != null && target.isAlive() && this.shieldCooldown == 0) {
            double d = this.distanceToSqr(target);
            boolean shooting = target.getMainHandItem().getItem() instanceof ProjectileWeaponItem
                    || target.getOffhandItem().getItem() instanceof ProjectileWeaponItem;
            if (d > 3.5 * 3.5 && d < 18 * 18 && (shooting || this.random.nextInt(60) == 0)) {
                this.startUsingItem(InteractionHand.OFF_HAND);
                this.shieldTicks = 30 + this.random.nextInt(40);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SKELETON_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.SKELETON_STEP;
    }
}
