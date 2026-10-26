package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/**
 * A villager of the Maasai settlements of the African savanna. Like the Amazon tribe there are two looks: an
 * ordinary villager in a red shuka, who trades what the herds give (beef, milk, leather, fur and red cloth), and
 * the settlement's elder, who trades what a cleric does. They sleep on mats in their huts and keep to the
 * settlement.
 */
public class AfricanVillagerEntity extends AmazonVillagerEntity {
    private static final int HERD_OFFERS = 4;

    public AfricanVillagerEntity(EntityType<? extends AfricanVillagerEntity> type, Level level) {
        super(type, level);
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
            this.addOffersFromItemListings(offers, HERD_TRADES, HERD_OFFERS);
        }
    }

    private static final VillagerTrades.ItemListing[] HERD_TRADES = {
            buy(Items.LEATHER, 6, 16, 2),
            buy(Items.BEEF, 10, 16, 2),
            sell(Items.COOKED_BEEF, 1, 6, 16, 2),
            sell(Items.MILK_BUCKET, 1, 1, 8, 2),
            sell(Items.RED_WOOL, 1, 4, 16, 2),
            sell(ModItems.FUR.get(), 3, 1, 6, 4),
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
}
