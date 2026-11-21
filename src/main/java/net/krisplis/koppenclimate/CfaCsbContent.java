package net.krisplis.koppenclimate;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * The setup for the Redwood forest (Csb) and the Subtropical plains and swamp (Cfa): their trees in flower pots, on
 * the fire and in the composter, and everything of theirs in the creative tabs. (Called from KoppenClimateMod.)
 */
public final class CfaCsbContent {
    private static final String[] WOODS = {"redwood", "platanus", "cedar", "apple", "apricot", "black_poplar", "cypress", "lemon", "magnolia", "maple", "scots_pine"};
    // the wood families of the trees that first came without one (their logs and planks)
    private static final String[] NEWER_WOODS = {"apple", "apricot", "black_poplar", "cypress", "lemon", "magnolia", "maple", "scots_pine"};

    private CfaCsbContent() {
    }

    /** On the main thread, from the common setup. */
    public static void commonSetup() {
        FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
        pot.addPlant(ModBlocks.REDWOOD_SAPLING.getId(), ModBlocks.POTTED_REDWOOD_SAPLING);
        pot.addPlant(ModBlocks.PLATANUS_SAPLING.getId(), ModBlocks.POTTED_PLATANUS_SAPLING);
        pot.addPlant(ModBlocks.CEDAR_SAPLING.getId(), ModBlocks.POTTED_CEDAR_SAPLING);
        pot.addPlant(ModBlocks.APPLE_SAPLING.getId(), ModBlocks.POTTED_APPLE_SAPLING);
        pot.addPlant(ModBlocks.APRICOT_SAPLING.getId(), ModBlocks.POTTED_APRICOT_SAPLING);
        pot.addPlant(ModBlocks.BLACK_POPLAR_SAPLING.getId(), ModBlocks.POTTED_BLACK_POPLAR_SAPLING);
        pot.addPlant(ModBlocks.MAGNOLIA_SAPLING.getId(), ModBlocks.POTTED_MAGNOLIA_SAPLING);
        pot.addPlant(ModBlocks.MAPLE_SAPLING.getId(), ModBlocks.POTTED_MAPLE_SAPLING);
        pot.addPlant(ModBlocks.SCOTS_PINE_SAPLING.getId(), ModBlocks.POTTED_SCOTS_PINE_SAPLING);

        // same burn odds as vanilla's wood and leaves: (block, encouragement, flammability)
        FireBlock fire = (FireBlock) Blocks.FIRE;
        List<RegistryObject<? extends Block>> logs = List.of(ModBlocks.REDWOOD_LOG, ModBlocks.REDWOOD_WOOD, ModBlocks.STRIPPED_REDWOOD_LOG,
                ModBlocks.STRIPPED_REDWOOD_WOOD, ModBlocks.PLATANUS_LOG, ModBlocks.PLATANUS_WOOD, ModBlocks.STRIPPED_PLATANUS_LOG,
                ModBlocks.STRIPPED_PLATANUS_WOOD, ModBlocks.CEDAR_LOG, ModBlocks.CEDAR_WOOD, ModBlocks.STRIPPED_CEDAR_LOG,
                ModBlocks.STRIPPED_CEDAR_WOOD);
        for (var log : logs) fire.setFlammable(log.get(), 5, 5);
        List<RegistryObject<? extends Block>> wood = List.of(ModBlocks.REDWOOD_PLANKS, ModBlocks.REDWOOD_STAIRS, ModBlocks.REDWOOD_SLAB,
                ModBlocks.REDWOOD_FENCE, ModBlocks.REDWOOD_FENCE_GATE, ModBlocks.PLATANUS_PLANKS, ModBlocks.PLATANUS_STAIRS,
                ModBlocks.PLATANUS_SLAB, ModBlocks.PLATANUS_FENCE, ModBlocks.PLATANUS_FENCE_GATE, ModBlocks.CEDAR_PLANKS,
                ModBlocks.CEDAR_STAIRS, ModBlocks.CEDAR_SLAB, ModBlocks.CEDAR_FENCE, ModBlocks.CEDAR_FENCE_GATE);
        for (var w : wood) fire.setFlammable(w.get(), 5, 20);
        for (String w : NEWER_WOODS) {
            for (String part : new String[]{"%s_log", "%s_wood", "stripped_%s_log", "stripped_%s_wood"}) flammable(fire, String.format(part, w), 5, 5);
            for (String part : new String[]{"%s_planks", "%s_stairs", "%s_slab", "%s_fence", "%s_fence_gate"}) flammable(fire, String.format(part, w), 5, 20);
        }
        List<RegistryObject<? extends Block>> leaves = List.of(ModBlocks.REDWOOD_LEAVES, ModBlocks.PLATANUS_LEAVES, ModBlocks.CEDAR_LEAVES,
                ModBlocks.APPLE_LEAVES, ModBlocks.APRICOT_LEAVES, ModBlocks.BLACK_POPLAR_LEAVES, ModBlocks.MAGNOLIA_LEAVES,
                ModBlocks.SUMMER_CHERRY_LEAVES, ModBlocks.MAPLE_LEAVES, ModBlocks.SCOTS_PINE_LEAVES);
        for (var l : leaves) {
            fire.setFlammable(l.get(), 30, 60);
            ComposterBlock.COMPOSTABLES.put(l.get().asItem(), 0.3F);
        }
        for (var s : List.of(ModBlocks.REDWOOD_SAPLING, ModBlocks.PLATANUS_SAPLING, ModBlocks.CEDAR_SAPLING, ModBlocks.APPLE_SAPLING,
                ModBlocks.APRICOT_SAPLING, ModBlocks.BLACK_POPLAR_SAPLING, ModBlocks.MAGNOLIA_SAPLING,
                ModBlocks.MAPLE_SAPLING, ModBlocks.SCOTS_PINE_SAPLING)) {
            ComposterBlock.COMPOSTABLES.put(s.get().asItem(), 0.3F);
        }
        fire.setFlammable(ModBlocks.WILD_BARLEY.get(), 60, 100);
        ComposterBlock.COMPOSTABLES.put(ModBlocks.CATTAIL.get().asItem(), 0.5F);
        ComposterBlock.COMPOSTABLES.put(ModItems.COTTON_SEEDS.get(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ModItems.CORN.get(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(ModBlocks.WILD_BARLEY.get().asItem(), 0.3F);
        ComposterBlock.COMPOSTABLES.put(ModItems.APRICOT.get(), 0.65F);
        ComposterBlock.COMPOSTABLES.put(ModItems.CHERRIES.get(), 0.3F);
        fire.setFlammable(ModBlocks.SHOJI_SCREEN.get(), 30, 60);
        fire.setFlammable(ModBlocks.SHOJI_PANE.get(), 30, 60);
        fire.setFlammable(ModBlocks.TATAMI.get(), 60, 20);
    }

    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        var tab = event.getTabKey();
        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            for (String w : WOODS) {
                for (String part : new String[]{"%s_log", "%s_wood", "stripped_%s_log", "stripped_%s_wood", "%s_planks", "%s_stairs",
                        "%s_slab", "%s_fence", "%s_fence_gate", "%s_door", "%s_trapdoor", "%s_pressure_plate", "%s_button"}) {
                    accept(event, String.format(part, w));
                }
            }
        }
        if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            for (var b : List.of(ModBlocks.FARMLAND_SOIL_GRASS_BLOCK, ModBlocks.FARMLAND_SOIL, ModBlocks.TILLED_FARMLAND_SOIL, ModBlocks.WILD_BARLEY, ModBlocks.CATTAIL,
                    ModBlocks.REDWOOD_LOG, ModBlocks.REDWOOD_LEAVES, ModBlocks.REDWOOD_SAPLING, ModBlocks.PLATANUS_LOG, ModBlocks.PLATANUS_LEAVES,
                    ModBlocks.PLATANUS_SAPLING, ModBlocks.CEDAR_LOG, ModBlocks.CEDAR_LEAVES, ModBlocks.CEDAR_SAPLING, ModBlocks.APPLE_LEAVES,
                    ModBlocks.APPLE_SAPLING, ModBlocks.APRICOT_LEAVES, ModBlocks.APRICOT_SAPLING, ModBlocks.BLACK_POPLAR_LEAVES,
                    ModBlocks.BLACK_POPLAR_SAPLING, ModBlocks.MAGNOLIA_LOG, ModBlocks.MAGNOLIA_LEAVES, ModBlocks.MAGNOLIA_SAPLING,
                    ModBlocks.SUMMER_CHERRY_LEAVES, ModBlocks.MAPLE_LOG, ModBlocks.MAPLE_LEAVES, ModBlocks.MAPLE_SAPLING,
                    ModBlocks.SCOTS_PINE_LOG, ModBlocks.SCOTS_PINE_LEAVES, ModBlocks.SCOTS_PINE_SAPLING)) {
                event.accept(b);
            }
        }
        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            // building with paper
            event.accept(ModBlocks.SHOJI_SCREEN);
            event.accept(ModBlocks.SHOJI_PANE);
            event.accept(ModItems.SHOJI_DOOR);
            event.accept(ModBlocks.TATAMI);
        }
        if (tab == CreativeModeTabs.COLORED_BLOCKS || tab == CreativeModeTabs.BUILDING_BLOCKS) {
            for (var planks : ModBlocks.PAINTED_PLANKS.values()) event.accept(planks);
        }
        if (tab == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.SHOTGUN);
            event.accept(ModItems.KAR98K);
            event.accept(ModItems.MOSIN);
            event.accept(ModItems.AK47);
        }
        if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.CHINESE_LANTERN);
            for (String w : WOODS) {
                accept(event, w + "_sign");
                accept(event, w + "_hanging_sign");
            }
        }
        if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (String w : WOODS) {
                accept(event, w + "_boat");
                accept(event, w + "_chest_boat");
            }
        }
        if (tab == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(ModItems.APRICOT);
            event.accept(ModItems.APRICOT_JAM);
            event.accept(ModItems.CHERRIES);
            event.accept(ModItems.CORN);
            event.accept(ModItems.COOKED_CORN);
            event.accept(ModItems.CEDAR_PERFUME);
        }
        if (tab == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.DRIED_CEDAR);
            event.accept(ModItems.COTTON_BOLL);
            event.accept(ModItems.COTTON_SEEDS);
        }
        if (tab == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.GRIZZLY_BEAR_SPAWN_EGG);
            event.accept(ModItems.TIGER_SPAWN_EGG);
            event.accept(ModItems.BLACK_BEAR_SPAWN_EGG);
            event.accept(ModItems.BROWN_BEAR_SPAWN_EGG);
        }
    }

    private static void flammable(FireBlock fire, String name, int encouragement, int flammability) {
        Block block = ForgeRegistries.BLOCKS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
        if (block != null && block != Blocks.AIR) fire.setFlammable(block, encouragement, flammability);
    }

    private static void accept(BuildCreativeModeTabContentsEvent event, String name) {
        var item = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, name));
        if (item != null && item != net.minecraft.world.item.Items.AIR) event.accept(item);
    }
}
