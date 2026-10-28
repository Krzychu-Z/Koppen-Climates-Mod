package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Nerium is poisonous, and hostile mobs that brush against it get badly hurt. They don't know it: to their
 * pathfinding it's an ordinary bush (not a danger like cactus or fire), so they blunder right into hedges of it.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NeriumThorns {
    // how much a touch hurts (hearts x2), and how often it can
    private static final float DAMAGE = 6.0F;
    private static final int INTERVAL = 10;

    private NeriumThorns() {
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy) || !(mob.level() instanceof ServerLevel level)) return;
        if ((mob.tickCount + mob.getId()) % INTERVAL != 0 || !mob.isAlive()) return;
        // the blocks it touches: its box, a hair bigger
        AABB touch = mob.getBoundingBox().inflate(0.05);
        int x0 = (int) Math.floor(touch.minX), x1 = (int) Math.floor(touch.maxX);
        int y0 = (int) Math.floor(touch.minY), y1 = (int) Math.floor(touch.maxY);
        int z0 = (int) Math.floor(touch.minZ), z1 = (int) Math.floor(touch.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (level.getBlockState(pos.set(x, y, z)).is(ModBlocks.NERIUM.get())) {
                        mob.hurt(level.damageSources().magic(), DAMAGE);
                        return;
                    }
                }
            }
        }
    }
}
