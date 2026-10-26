package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.InteractGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

import javax.annotation.Nullable;

/**
 * A villager of the Amazon tribal villages. There are two looks: an ordinary tribe member, who only trades fish,
 * and the tribe leader (the village's priest), who trades what a cleric does. A village gets exactly one leader.
 * They have no professions, workstations or beds, never breed, and keep to the village they were born in.
 */
public class AmazonVillagerEntity extends AbstractVillager {
    private static final EntityDataAccessor<Boolean> DATA_LEADER =
            SynchedEntityData.defineId(AmazonVillagerEntity.class, EntityDataSerializers.BOOLEAN);
    // how far from the middle of their village they wander
    public static final int VILLAGE_RADIUS = 22;
    private static final int FISH_OFFERS = 4;

    @Nullable
    private BlockPos village;

    public AmazonVillagerEntity(EntityType<? extends AmazonVillagerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LEADER, false);
    }

    public boolean isLeader() {
        return this.entityData.get(DATA_LEADER);
    }

    public void setLeader(boolean leader) {
        this.entityData.set(DATA_LEADER, leader);
    }

    /** Ties this villager to its village, so it keeps to the clearing. */
    public void setVillage(BlockPos centre) {
        this.village = centre.immutable();
        this.restrictTo(this.village, VILLAGE_RADIUS);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(2, new SleepOnMatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Zombie.class, 8.0F, 0.5D, 0.6D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Pillager.class, 15.0F, 0.5D, 0.6D));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Vindicator.class, 8.0F, 0.5D, 0.6D));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.6D));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.4D));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35D));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    // wake up when hurt, like a villager
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.isSleeping()) {
            this.stopSleeping();
        }
        return super.hurt(source, amount);
    }

    /**
     * At night a villager walks to a free sleeping mat in its village and lies down on it until morning
     * (or until something wakes it).
     */
    static final class SleepOnMatGoal extends Goal {
        private static final int SEARCH = 24;
        private final AmazonVillagerEntity villager;
        @Nullable
        private BlockPos mat;
        private int cooldown;

        SleepOnMatGoal(AmazonVillagerEntity villager) {
            this.villager = villager;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!this.villager.level().isNight() || this.villager.isTrading() || this.villager.getTarget() != null) {
                return false;
            }
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            this.cooldown = 40;
            this.mat = this.findMat();
            return this.mat != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.mat != null && this.villager.level().isNight() && !this.villager.isTrading()
                    && isFreeMat(this.villager.level().getBlockState(this.mat), this.villager.isSleeping());
        }

        @Override
        public void tick() {
            if (this.mat == null || this.villager.isSleeping()) {
                return;
            }
            if (this.villager.distanceToSqr(Vec3.atBottomCenterOf(this.mat)) < 2.5D) {
                this.villager.getNavigation().stop();
                this.villager.startSleeping(this.mat);
            } else if (this.villager.getNavigation().isDone()) {
                this.villager.getNavigation().moveTo(this.mat.getX() + 0.5D, this.mat.getY(), this.mat.getZ() + 0.5D, 0.5D);
            }
        }

        @Override
        public void stop() {
            if (this.villager.isSleeping()) {
                this.villager.stopSleeping();
            }
            this.mat = null;
        }

        @Nullable
        private BlockPos findMat() {
            BlockPos centre = this.villager.hasRestriction() ? this.villager.getRestrictCenter() : this.villager.blockPosition();
            BlockPos best = null;
            double bestDistance = Double.MAX_VALUE;
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-SEARCH, -4, -SEARCH), centre.offset(SEARCH, 4, SEARCH))) {
                BlockState state = this.villager.level().getBlockState(pos);
                if (isFreeMat(state, false)) {
                    double d = pos.distSqr(this.villager.blockPosition());
                    if (d < bestDistance) {
                        bestDistance = d;
                        best = pos.immutable();
                    }
                }
            }
            return best;
        }

        // the head half of a mat nobody else is lying on (or the one this villager is lying on)
        private static boolean isFreeMat(BlockState state, boolean mine) {
            return state.is(ModBlocks.SLEEPING_MAT.get()) && state.getValue(BedBlock.PART) == BedPart.HEAD
                    && (mine || !state.getValue(BedBlock.OCCUPIED));
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof net.minecraft.world.item.SpawnEggItem) && this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                player.awardStat(Stats.TALKED_TO_VILLAGER);
            }
            if (!this.level().isClientSide) {
                if (this.getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        if (this.isLeader()) {
            // the leader is the village's priest: every trade a cleric can offer, from novice to master
            var cleric = VillagerTrades.TRADES.get(VillagerProfession.CLERIC);
            if (cleric != null) {
                for (int level = 1; level <= 5; level++) {
                    VillagerTrades.ItemListing[] listings = cleric.get(level);
                    if (listings != null) {
                        this.addOffersFromItemListings(offers, listings, listings.length);
                    }
                }
            }
        } else {
            this.addOffersFromItemListings(offers, FISH_TRADES, FISH_OFFERS);
        }
    }

    // ordinary tribe members trade fish and nothing else
    private static final VillagerTrades.ItemListing[] FISH_TRADES = {
            buy(Items.COD, 6, 16, 2),
            buy(Items.SALMON, 5, 16, 2),
            buy(Items.TROPICAL_FISH, 3, 12, 3),
            buy(Items.PUFFERFISH, 2, 12, 3),
            sell(Items.COOKED_COD, 6, 16, 2),
            sell(Items.COOKED_SALMON, 5, 16, 2),
            sell(Items.TROPICAL_FISH, 2, 12, 3),
            sell(Items.PUFFERFISH, 1, 12, 3),
    };

    /** The villager buys {@code count} fish for an emerald. */
    private static VillagerTrades.ItemListing buy(ItemLike fish, int count, int maxUses, int xp) {
        return (Entity trader, RandomSource random) ->
                new MerchantOffer(new ItemCost(fish, count), new ItemStack(Items.EMERALD), maxUses, xp, 0.05F);
    }

    /** The villager sells {@code count} fish for an emerald. */
    private static VillagerTrades.ItemListing sell(ItemLike fish, int count, int maxUses, int xp) {
        return (Entity trader, RandomSource random) ->
                new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(fish, count), maxUses, xp, 0.05F);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            int xp = 3 + this.random.nextInt(4);
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), xp));
        }
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected Component getTypeName() {
        return this.isLeader()
                ? Component.translatable(this.getType().getDescriptionId() + ".leader")
                : super.getTypeName();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Leader", this.isLeader());
        if (this.village != null) {
            tag.put("Village", NbtUtils.writeBlockPos(this.village));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setLeader(tag.getBoolean("Leader"));
        NbtUtils.readBlockPos(tag, "Village").ifPresent(this::setVillage);
        this.setAge(Math.max(0, this.getAge()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean yes) {
        return yes ? SoundEvents.VILLAGER_YES : SoundEvents.VILLAGER_NO;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }
}
