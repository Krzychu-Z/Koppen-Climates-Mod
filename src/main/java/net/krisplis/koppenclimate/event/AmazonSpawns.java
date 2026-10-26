package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cave spiders in the Amazon: they come out in broad daylight too, on the jungle floor as well as in caves.
 * Monsters normally need darkness twice over, once in their spawn placement rule and once more in the mob's own
 * check; ModEntities widens the first and this lifts the second, both only inside the Amazon biomes.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AmazonSpawns {
    /** Every Amazon (Af) biome. */
    public static final TagKey<Biome> AMAZON =
            TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "af"));

    private AmazonSpawns() {
    }

    /** The extra spawn placement rule for cave spiders: any light, as long as it is an Amazon biome. */
    public static boolean caveSpiderInDaylight(EntityType<CaveSpider> type, ServerLevelAccessor level, MobSpawnType spawnType,
                                               BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && (level.getBiome(pos).is(AMAZON) || ModBiomes.isMonsoon(level.getBiome(pos)))
                && level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type);
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof CaveSpider spider) || event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        ServerLevelAccessor level = event.getLevel();
        if (level.getDifficulty() != Difficulty.PEACEFUL && (level.getBiome(spider.blockPosition()).is(AMAZON)
                || ModBiomes.isMonsoon(level.getBiome(spider.blockPosition())))
                && spider.checkSpawnObstruction(level)) {
            event.setResult(Event.Result.ALLOW);
        }
    }
}
