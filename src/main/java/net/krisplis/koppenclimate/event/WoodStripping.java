package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/** An axe strips the mod's logs and wood, like vanilla's, keeping which way they lie. */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WoodStripping {
    private static Map<Block, Block> strippables;

    private WoodStripping() {
    }

    private static Map<Block, Block> strippables() {
        if (strippables == null) {
            strippables = Map.ofEntries(
                    Map.entry(ModBlocks.BAOBAB_LOG.get(), ModBlocks.STRIPPED_BAOBAB_LOG.get()),
                    Map.entry(ModBlocks.BAOBAB_WOOD.get(), ModBlocks.STRIPPED_BAOBAB_WOOD.get()),
                    Map.entry(ModBlocks.MAHOGANY_LOG.get(), ModBlocks.STRIPPED_MAHOGANY_LOG.get()),
                    Map.entry(ModBlocks.MAHOGANY_WOOD.get(), ModBlocks.STRIPPED_MAHOGANY_WOOD.get()),
                    Map.entry(ModBlocks.COCONUT_PALM_LOG.get(), ModBlocks.STRIPPED_COCONUT_PALM_LOG.get()),
                    Map.entry(ModBlocks.COCONUT_PALM_WOOD.get(), ModBlocks.STRIPPED_COCONUT_PALM_WOOD.get()),
                    Map.entry(ModBlocks.OLIVE_LOG.get(), ModBlocks.STRIPPED_OLIVE_LOG.get()),
                    Map.entry(ModBlocks.OLIVE_WOOD.get(), ModBlocks.STRIPPED_OLIVE_WOOD.get()),
                    Map.entry(ModBlocks.ITALIAN_PINE_LOG.get(), ModBlocks.STRIPPED_ITALIAN_PINE_LOG.get()),
                    Map.entry(ModBlocks.ITALIAN_PINE_WOOD.get(), ModBlocks.STRIPPED_ITALIAN_PINE_WOOD.get()),
                    Map.entry(ModBlocks.FIG_LOG.get(), ModBlocks.STRIPPED_FIG_LOG.get()),
                    Map.entry(ModBlocks.FIG_WOOD.get(), ModBlocks.STRIPPED_FIG_WOOD.get()));
        }
        return strippables;
    }

    @SubscribeEvent
    public static void onToolUse(BlockEvent.BlockToolModificationEvent event) {
        if (event.getToolAction() != ToolActions.AXE_STRIP) return;
        BlockState state = event.getState();
        Block stripped = strippables().get(state.getBlock());
        if (stripped != null) {
            event.setFinalState(stripped.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)));
        }
    }
}
