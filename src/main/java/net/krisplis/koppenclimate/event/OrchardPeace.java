package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Only peaceful creatures ever appear in the Cherry orchards (Cwa), even at night. The biome spawns no monsters of its
 * own; this also turns away the hostile mobs vanilla brings in other ways (phantoms over a sleepless player, patrols,
 * spawners): any newly created hostile mob in an orchard simply never arrives. Mobs loaded from a save, and ones that
 * wander in from outside, are left alone.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OrchardPeace {
    private OrchardPeace() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || !(event.getEntity() instanceof Enemy) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level.getBiome(event.getEntity().blockPosition()).is(ModBiomes.CHERRY_ORCHARD)) event.setCanceled(true);
    }
}
