package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.SaunaStoveBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The warmth of a dry sauna and of the Tundra's hot springs: a player sitting in a sauna with its stove burning
 * (within 4 blocks of it), or bathing in a hot spring (in water, within 4 blocks of a geothermal vent), is warmed
 * through - slowly healing (Regeneration) and losing any chill (freezing) - for as long as they stay.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SaunaWarmth {
    private static final int SAUNA = 4, SPRING = 4;

    private SaunaWarmth() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide || player.tickCount % 20 != 0) return;
        boolean bathing = player.isInWater();
        if (warm(level, player.blockPosition(), bathing)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
            player.setTicksFrozen(0);
        }
    }

    private static boolean warm(Level level, BlockPos at, boolean bathing) {
        int r = bathing ? SPRING : SAUNA;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-r, -r, -r), at.offset(r, r, r))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.SAUNA_STOVE.get()) && state.getValue(SaunaStoveBlock.LIT)) return true;
            if (bathing && state.is(ModBlocks.GEOTHERMAL_VENT.get())) return true;
        }
        return false;
    }
}
