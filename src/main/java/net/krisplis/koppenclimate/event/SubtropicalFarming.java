package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.TilledSoilBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Subtropical plains' (Cfa) farmland soil: a hoe tills it (or its grass) into tilled farmland soil, and on that
 * every crop grows twice as fast as on the best vanilla farmland - which, a crop in the middle of a fully watered
 * field, grows a stage in about 1 of 3.5 of its random ticks; here 2 in 3.5 (dry, twice the dry rate).
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SubtropicalFarming {
    private static final float MOIST_CHANCE = 2.0F / 3.5F, DRY_CHANCE = 2.0F / 7.25F;

    private SubtropicalFarming() {
    }

    @SubscribeEvent
    public static void onToolUse(BlockEvent.BlockToolModificationEvent event) {
        if (event.getToolAction() != ToolActions.HOE_TILL) return;
        BlockState state = event.getState();
        if (!state.is(ModBlocks.FARMLAND_SOIL.get()) && !state.is(ModBlocks.FARMLAND_SOIL_GRASS_BLOCK.get())) return;
        UseOnContext context = event.getContext();
        BlockPos pos = context.getClickedPos();
        if (context.getClickedFace() == Direction.DOWN || !context.getLevel().getBlockState(pos.above()).isAir()) return;
        event.setFinalState(ModBlocks.TILLED_FARMLAND_SOIL.get().defaultBlockState());
    }

    @SubscribeEvent
    public static void onCropGrow(BlockEvent.CropGrowEvent.Pre event) {
        BlockState soil = event.getLevel().getBlockState(event.getPos().below());
        if (!soil.is(ModBlocks.TILLED_FARMLAND_SOIL.get())) return;
        float chance = soil.getValue(TilledSoilBlock.MOISTURE) > 0 ? MOIST_CHANCE : DRY_CHANCE;
        event.setResult(event.getLevel().getRandom().nextFloat() < chance ? Event.Result.ALLOW : Event.Result.DENY);
    }
}
