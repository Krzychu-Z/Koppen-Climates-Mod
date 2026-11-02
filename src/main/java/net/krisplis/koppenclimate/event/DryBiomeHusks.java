package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * In every dry (B-type) biome - the deserts BWh and BWk, the steppes BSk and BSh - the dead walk as husks, never as
 * zombies: a plain zombie appearing there by any means (night spawns, village sieges, monster spawners) is swapped for a husk on the spot. Zombie villagers are left alone so they can still be cured, and a
 * husk that sinks and turns into a zombie on its way to becoming a drowned is left to do so.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DryBiomeHusks {
    private DryBiomeHusks() {
    }

    /** Whether this is one of the mod's B-type (arid) biomes, known by the Köppen code its name ends with. */
    public static boolean isDryBiome(Holder<Biome> biome) {
        return biome.unwrapKey().map(ResourceKey::location)
                .filter(id -> id.getNamespace().equals(KoppenClimateMod.MOD_ID))
                .map(id -> {
                    String path = id.getPath();
                    return path.endsWith("_bwh") || path.endsWith("_bwk") || path.endsWith("_bsh") || path.endsWith("_bsk");
                })
                .orElse(false);
    }

    // a trial spawner keeps track of the very mobs it spawned, so its zombies are left as they are
    private static final String KEEP_TAG = KoppenClimateMod.MOD_ID + ".trial_zombie";

    @SubscribeEvent
    public static void onFinalizeSpawn(net.minecraftforge.event.entity.living.MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() == MobSpawnType.TRIAL_SPAWNER && event.getEntity().getType() == EntityType.ZOMBIE) {
            event.getEntity().addTag(KEEP_TAG);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Zombie zombie) || zombie.getType() != EntityType.ZOMBIE) return;
        // sinking husks become zombies before they become drowned: let that happen
        if (zombie.isInWater() || zombie.getTags().contains(KEEP_TAG)) return;
        if (!isDryBiome(level.getBiome(zombie.blockPosition()))) return;
        Husk husk = EntityType.HUSK.create(level);
        if (husk == null) return;
        husk.moveTo(zombie.getX(), zombie.getY(), zombie.getZ(), zombie.getYRot(), zombie.getXRot());
        husk.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.NATURAL, null);
        husk.setBaby(zombie.isBaby());
        // keep whatever it was given (a spawner's or a structure's gear)
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!zombie.getItemBySlot(slot).isEmpty()) husk.setItemSlot(slot, zombie.getItemBySlot(slot).copy());
        }
        if (zombie.isPersistenceRequired()) husk.setPersistenceRequired();
        if (zombie.getVehicle() != null) husk.startRiding(zombie.getVehicle(), true);
        event.setCanceled(true);
        level.addFreshEntity(husk);
    }
}
