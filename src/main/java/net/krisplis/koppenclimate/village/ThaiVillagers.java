package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.custom.RiceBlock;
import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * The Thai villagers' ways:
 * <ul>
 *   <li>Villagers born in the monsoon country (Aw) are Thai.</li>
 *   <li>Thai farmers tend the rice terraces instead of fields: by day they wade out to ripe rice, harvest it and
 *       replant it. The harvest feeds the village (it goes into the farmer's food, which villagers share, eat
 *       and breed on).</li>
 *   <li>The Thai cleric - the village priest - deals in jade.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThaiVillagers {
    private static final int SEARCH_RADIUS = 28;
    private static final int SEARCH_TRIES = 48;
    private static final double HARVEST_REACH = 2.5;
    private static final String TARGET = "koppenclimatemod:rice_target";
    private static final String TARGET_TIME = "koppenclimatemod:rice_target_time";
    // a farmer gives up on rice it hasn't reached in this long
    private static final int GIVE_UP_TICKS = 600;

    private ThaiVillagers() {
    }

    /** Villagers born in the monsoon country are Thai. Vanilla keeps the biome-to-type map private, so fill it in. */
    @SuppressWarnings("unchecked")
    public static void registerBiomeTypes() {
        try {
            Field field = VillagerType.class.getDeclaredField("BY_BIOME");
            field.setAccessible(true);
            Map<ResourceKey<Biome>, VillagerType> map = (Map<ResourceKey<Biome>, VillagerType>) field.get(null);
            map.put(ModBiomes.TROPICAL_MONSOON, VillageRegistry.THAI.get());
            map.put(ModBiomes.RICE_TERRACES, VillageRegistry.THAI.get());
        } catch (ReflectiveOperationException | RuntimeException e) {
            KoppenClimateMod.LOGGER.warn("Couldn't make villagers born in the monsoon country Thai", e);
        }
    }

    static boolean isThai(Entity entity) {
        return entity instanceof Villager villager && villager.getVillagerData().getType() == VillageRegistry.THAI.get();
    }

    // ---------------------------------------------------------------- farmers in the rice terraces

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || !(villager.level() instanceof ServerLevel level)) return;
        if ((villager.tickCount + villager.getId()) % 20 != 0) return;
        if (!isThai(villager) || villager.getVillagerData().getProfession() != VillagerProfession.FARMER || villager.isBaby()) return;
        long time = level.getDayTime() % 24000L;
        if (time < 2000 || time > 11000 || villager.isSleeping() || villager.isTrading()) return;

        var data = villager.getPersistentData();
        BlockPos target = data.contains(TARGET) ? BlockPos.of(data.getLong(TARGET)) : null;
        if (target != null && (!ripe(level.getBlockState(target)) || level.getGameTime() - data.getLong(TARGET_TIME) > GIVE_UP_TICKS)) {
            data.remove(TARGET);
            target = null;
        }
        if (target == null) {
            target = findRipeRice(level, villager.blockPosition(), villager.getRandom());
            if (target == null) return;
            data.putLong(TARGET, target.asLong());
            data.putLong(TARGET_TIME, level.getGameTime());
        }
        if (villager.position().distanceTo(target.getCenter()) <= HARVEST_REACH) {
            harvest(level, villager, target);
            data.remove(TARGET);
            return;
        }
        villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(target, 0.5F, 1));
        villager.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target));
    }

    private static boolean ripe(BlockState state) {
        return state.getBlock() instanceof RiceBlock rice && rice.isMaxAge(state);
    }

    private static BlockPos findRipeRice(ServerLevel level, BlockPos from, RandomSource random) {
        for (int i = 0; i < SEARCH_TRIES; i++) {
            int x = from.getX() + random.nextInt(2 * SEARCH_RADIUS + 1) - SEARCH_RADIUS;
            int z = from.getZ() + random.nextInt(2 * SEARCH_RADIUS + 1) - SEARCH_RADIUS;
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            if (Math.abs(top.getY() - from.getY()) <= 12 && ripe(level.getBlockState(top))) return top;
        }
        return null;
    }

    // cut the ripe rice, replant it at once, and take the harvest home as food
    private static void harvest(ServerLevel level, Villager villager, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RiceBlock rice)) return;
        level.setBlock(pos, rice.getStateForAge(0), 2);
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        villager.swing(villager.getUsedItemHand());
        // villagers only eat bread, carrots, potatoes and beetroot, so the rice is stored as a loaf's worth of food
        ItemStack food = new ItemStack(Items.BREAD);
        if (villager.getInventory().canAddItem(food)) villager.getInventory().addItem(food);
    }

    // ---------------------------------------------------------------- the priest's jade

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.CLERIC) return;
        // a journeyman priest buys jade; an expert one sells it
        event.getTrades().get(3).add(thaiOnly((trader, random) ->
                new MerchantOffer(new ItemCost(ModItems.JADE.get(), 1), new ItemStack(Items.EMERALD, 12), 8, 20, 0.05F)));
        event.getTrades().get(4).add(thaiOnly((trader, random) ->
                new MerchantOffer(new ItemCost(Items.EMERALD, 28), new ItemStack(ModItems.JADE.get()), 4, 30, 0.05F)));
    }

    private static VillagerTrades.ItemListing thaiOnly(VillagerTrades.ItemListing listing) {
        return (trader, random) -> isThai(trader) ? listing.getOffer(trader, random) : null;
    }
}
