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
                    Map.entry(ModBlocks.FIG_WOOD.get(), ModBlocks.STRIPPED_FIG_WOOD.get()),
                    Map.entry(ModBlocks.JOSHUA_LOG.get(), ModBlocks.STRIPPED_JOSHUA_LOG.get()),
                    Map.entry(ModBlocks.JOSHUA_WOOD.get(), ModBlocks.STRIPPED_JOSHUA_WOOD.get()),
                    Map.entry(ModBlocks.DEAD_OAK_LOG.get(), ModBlocks.STRIPPED_DEAD_OAK_LOG.get()),
                    Map.entry(ModBlocks.DEAD_OAK_WOOD.get(), ModBlocks.STRIPPED_DEAD_OAK_WOOD.get()),
                    Map.entry(ModBlocks.EUCALYPTUS_LOG.get(), ModBlocks.STRIPPED_EUCALYPTUS_LOG.get()),
                    Map.entry(ModBlocks.EUCALYPTUS_WOOD.get(), ModBlocks.STRIPPED_EUCALYPTUS_WOOD.get()),
                    Map.entry(ModBlocks.REDWOOD_LOG.get(), ModBlocks.STRIPPED_REDWOOD_LOG.get()),
                    Map.entry(ModBlocks.REDWOOD_WOOD.get(), ModBlocks.STRIPPED_REDWOOD_WOOD.get()),
                    Map.entry(ModBlocks.PLATANUS_LOG.get(), ModBlocks.STRIPPED_PLATANUS_LOG.get()),
                    Map.entry(ModBlocks.PLATANUS_WOOD.get(), ModBlocks.STRIPPED_PLATANUS_WOOD.get()),
                    Map.entry(ModBlocks.CEDAR_LOG.get(), ModBlocks.STRIPPED_CEDAR_LOG.get()),
                    Map.entry(ModBlocks.CEDAR_WOOD.get(), ModBlocks.STRIPPED_CEDAR_WOOD.get()),
                    Map.entry(ModBlocks.APPLE_LOG.get(), ModBlocks.STRIPPED_APPLE_LOG.get()),
                    Map.entry(ModBlocks.APPLE_WOOD.get(), ModBlocks.STRIPPED_APPLE_WOOD.get()),
                    Map.entry(ModBlocks.APRICOT_LOG.get(), ModBlocks.STRIPPED_APRICOT_LOG.get()),
                    Map.entry(ModBlocks.APRICOT_WOOD.get(), ModBlocks.STRIPPED_APRICOT_WOOD.get()),
                    Map.entry(ModBlocks.BLACK_POPLAR_LOG.get(), ModBlocks.STRIPPED_BLACK_POPLAR_LOG.get()),
                    Map.entry(ModBlocks.BLACK_POPLAR_WOOD.get(), ModBlocks.STRIPPED_BLACK_POPLAR_WOOD.get()),
                    Map.entry(ModBlocks.CYPRESS_LOG.get(), ModBlocks.STRIPPED_CYPRESS_LOG.get()),
                    Map.entry(ModBlocks.CYPRESS_WOOD.get(), ModBlocks.STRIPPED_CYPRESS_WOOD.get()),
                    Map.entry(ModBlocks.LEMON_LOG.get(), ModBlocks.STRIPPED_LEMON_LOG.get()),
                    Map.entry(ModBlocks.LEMON_WOOD.get(), ModBlocks.STRIPPED_LEMON_WOOD.get()),
                    Map.entry(ModBlocks.MAGNOLIA_LOG.get(), ModBlocks.STRIPPED_MAGNOLIA_LOG.get()),
                    Map.entry(ModBlocks.MAGNOLIA_WOOD.get(), ModBlocks.STRIPPED_MAGNOLIA_WOOD.get()),
                    Map.entry(ModBlocks.MAPLE_LOG.get(), ModBlocks.STRIPPED_MAPLE_LOG.get()),
                    Map.entry(ModBlocks.MAPLE_WOOD.get(), ModBlocks.STRIPPED_MAPLE_WOOD.get()),
                    Map.entry(ModBlocks.SCOTS_PINE_LOG.get(), ModBlocks.STRIPPED_SCOTS_PINE_LOG.get()),
                    Map.entry(ModBlocks.SCOTS_PINE_WOOD.get(), ModBlocks.STRIPPED_SCOTS_PINE_WOOD.get()));
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
