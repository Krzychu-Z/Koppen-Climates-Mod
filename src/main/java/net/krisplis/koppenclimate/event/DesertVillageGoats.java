package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Arab villages of the deserts (vanilla's desert villages, in the mod's desert colours) keep no pigs: every pig
 * that turns up in one (in the animal pens and butcher's yards the village is built with, bred there, or brought in)
 * becomes a goat, of the same age, standing where the pig stood.
 * <p>
 * The pens are vanilla's own pieces, sharing their animal pools with every other village, so the swap is made on the
 * animals themselves rather than in the pools. It waits a tick after the pig appears, so that nothing is looked up
 * or replaced while its chunk is still being loaded.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DesertVillageGoats {
    private DesertVillageGoats() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Pig pig) || !(event.getLevel() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        server.tell(new TickTask(server.getTickCount(), () -> swap(level, pig)));
    }

    private static void swap(ServerLevel level, Pig pig) {
        if (pig.isRemoved() || pig.level() != level || pig.isVehicle() || pig.isPassenger() || pig.hasCustomName()) return;
        if (!level.structureManager().getStructureWithPieceAt(pig.blockPosition(), h -> h.is(BuiltinStructures.VILLAGE_DESERT)).isValid()) return;
        Goat goat = EntityType.GOAT.create(level);
        if (goat == null) return;
        goat.moveTo(pig.getX(), pig.getY(), pig.getZ(), pig.getYRot(), pig.getXRot());
        goat.setYHeadRot(pig.getYHeadRot());
        goat.setAge(pig.getAge());
        if (pig.isPersistenceRequired()) goat.setPersistenceRequired();
        pig.discard();
        level.addFreshEntity(goat);
    }
}
