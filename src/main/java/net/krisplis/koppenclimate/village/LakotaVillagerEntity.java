package net.krisplis.koppenclimate.village;

import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * A villager of the Lakota camps of the Prairie (Dfa). Like the Amazon tribe there are two looks: an ordinary
 * villager (a man in a fringed, beaded buckskin shirt, leggings and moccasins with an eagle feather in his braids,
 * or a woman in a long buckskin dress with a beaded yoke - which one is fixed for each villager), and the camp's
 * elder in fringed white buckskin and a war bonnet, who trades what a cleric does. The others trade what the camp
 * lives by: hides, meat, bows and arrows, and horse gear.
 *
 * <p>They keep the camp's horses: now and then one walks up to two grown, tame horses with golden carrots and sets
 * them to breed, as long as the herd round the camp is still small.
 */
public class LakotaVillagerEntity extends AmazonVillagerEntity {
    private static final int CAMP_OFFERS = 4;
    // the herd is left to breed up to this many horses round the camp
    static final int MAX_HERD = 12;

    public LakotaVillagerEntity(EntityType<? extends LakotaVillagerEntity> type, Level level) {
        super(type, level);
    }

    /** Whether this villager wears a woman's dress (fixed for each villager). */
    public boolean isWoman() {
        return (this.getUUID().getLeastSignificantBits() & 1L) == 0L;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(5, new TendHorsesGoal(this));
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = this.getOffers();
        if (this.isLeader()) {
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
            this.addOffersFromItemListings(offers, CAMP_TRADES, CAMP_OFFERS);
        }
    }

    private static final VillagerTrades.ItemListing[] CAMP_TRADES = {
            buy(Items.WHEAT, 20, 16, 2),
            buy(Items.IRON_INGOT, 4, 12, 3),
            sell(Items.LEATHER, 1, 3, 16, 2),
            sell(Items.COOKED_BEEF, 1, 5, 16, 2),
            sell(Items.RABBIT_HIDE, 1, 4, 16, 2),
            sell(Items.ARROW, 1, 12, 16, 2),
            sell(Items.BOW, 2, 1, 6, 4),
            sell(Items.LEAD, 1, 2, 8, 3),
            sell(Items.SADDLE, 6, 1, 4, 6),
    };

    /** The villager buys {@code count} of an item for an emerald. */
    private static VillagerTrades.ItemListing buy(ItemLike item, int count, int maxUses, int xp) {
        return (Entity trader, RandomSource random) ->
                new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD), maxUses, xp, 0.05F);
    }

    /** The villager sells {@code count} of an item for {@code price} emeralds. */
    private static VillagerTrades.ItemListing sell(ItemLike item, int price, int count, int maxUses, int xp) {
        return (Entity trader, RandomSource random) ->
                new MerchantOffer(new ItemCost(Items.EMERALD, price), new ItemStack(item, count), maxUses, xp, 0.05F);
    }

    /**
     * Breeding the herd: every half minute or so (at random) a villager looks for two grown, tame horses near the camp
     * that are ready to breed, walks up to them with golden carrots in hand and feeds them, so they fall in love and
     * breed the vanilla way. It stops once the camp's herd is MAX_HERD strong, and never at night or while trading.
     */
    static final class TendHorsesGoal extends Goal {
        private static final double RANGE = 26.0D;
        private static final int GIVE_UP = 400;
        private final LakotaVillagerEntity villager;
        @Nullable
        private Horse first, second;
        private int cooldown;
        private int ticks;

        TendHorsesGoal(LakotaVillagerEntity villager) {
            this.villager = villager;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            this.cooldown = 400 + this.villager.getRandom().nextInt(400);
            if (this.villager.level().isNight() || this.villager.isTrading() || this.villager.isSleeping() || this.villager.isBaby()) {
                return false;
            }
            AABB around = this.villager.hasRestriction()
                    ? new AABB(this.villager.getRestrictCenter()).inflate(RANGE)
                    : this.villager.getBoundingBox().inflate(RANGE);
            List<Horse> herd = this.villager.level().getEntitiesOfClass(Horse.class, around, Horse::isAlive);
            if (herd.size() >= MAX_HERD) {
                return false;
            }
            this.first = null;
            this.second = null;
            for (Horse horse : herd) {
                // only tame horses breed, and not one with a rider
                if (horse.getAge() != 0 || horse.isInLove() || !horse.isTamed() || horse.isVehicle()) continue;
                if (this.first == null) {
                    this.first = horse;
                } else if (horse.distanceToSqr(this.first) < 100.0D) {
                    this.second = horse;
                    break;
                }
            }
            return this.first != null && this.second != null;
        }

        @Override
        public void start() {
            this.ticks = 0;
            this.villager.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_CARROT));
        }

        @Override
        public boolean canContinueToUse() {
            return this.first != null && this.second != null && this.first.isAlive() && this.second.isAlive()
                    && !this.villager.isTrading() && this.ticks < GIVE_UP;
        }

        @Override
        public void tick() {
            this.ticks++;
            Horse target = this.first.isInLove() ? this.second : this.first;
            this.villager.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (this.villager.distanceToSqr(target) > 7.0D) {
                if (this.villager.getNavigation().isDone() || this.ticks % 20 == 0) {
                    this.villager.getNavigation().moveTo(target, 0.45D);
                }
                return;
            }
            this.villager.getNavigation().stop();
            this.villager.swing(InteractionHand.MAIN_HAND);
            target.setInLove(null);
            if (this.first.isInLove() && this.second.isInLove()) {
                this.first = null;
                this.second = null;
            }
        }

        @Override
        public void stop() {
            this.villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            this.first = null;
            this.second = null;
        }
    }
}
