package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A hoe tills oxisol and oxisol grass into farmland, the same as dirt and grass: only from above or the side,
 * and only with air on top. (Vanilla's list of tillable blocks is fixed, so the mod adds its soils here.)
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OxisolTilling {
    private OxisolTilling() {
    }

    @SubscribeEvent
    public static void onToolUse(BlockEvent.BlockToolModificationEvent event) {
        if (event.getToolAction() != ToolActions.HOE_TILL) return;
        BlockState state = event.getState();
        if (!state.is(ModBlocks.OXISOL.get()) && !state.is(ModBlocks.OXISOL_GRASS.get())) return;
        UseOnContext context = event.getContext();
        BlockPos pos = context.getClickedPos();
        if (context.getClickedFace() == Direction.DOWN || !context.getLevel().getBlockState(pos.above()).isAir()) return;
        event.setFinalState(Blocks.FARMLAND.defaultBlockState());
    }
}
