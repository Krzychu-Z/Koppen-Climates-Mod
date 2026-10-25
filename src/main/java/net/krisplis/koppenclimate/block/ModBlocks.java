package net.krisplis.koppenclimate.block;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.custom.BananaBunchBlock;
import net.krisplis.koppenclimate.block.custom.CoconutBlock;
import net.krisplis.koppenclimate.block.custom.OxisolGrassBlock;
import net.krisplis.koppenclimate.block.custom.PalmSaplingBlock;
import net.krisplis.koppenclimate.block.custom.RafflesiaBlock;
import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, KoppenClimateMod.MOD_ID);

    public static final RegistryObject<Block> OXISOL = registerBlock("oxisol",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE)
                    .strength(0.5F)
                    .sound(SoundType.GRAVEL)
                    .instrument(NoteBlockInstrument.BASS)
            )
    );

    public static final RegistryObject<Block> OXISOL_GRASS = registerBlock("oxisol_grass",
            () -> new OxisolGrassBlock(BlockBehaviour.Properties
                    .of().mapColor(MapColor.GRASS)   // like grass
                    .strength(0.6F)
                    .sound(SoundType.GRASS)
                    .randomTicks(),                  // required for spread/decay
                    OXISOL                            // your base oxisol block
            )
    );

    // Falls like sand; the dust colour is what drips from it when it hangs over air
    public static final RegistryObject<ColoredFallingBlock> WHITE_SAND = registerBlock("white_sand",
            () -> new ColoredFallingBlock(new ColorRGBA(0xEDE8D8),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.QUARTZ))
    );

    // Coconut palm wood set: properties copied from jungle wood
    public static final RegistryObject<RotatedPillarBlock> COCONUT_PALM_LOG = registerBlock("coconut_palm_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG))
    );

    public static final RegistryObject<Block> COCONUT_PALM_PLANKS = registerBlock("coconut_palm_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_PLANKS))
    );

    public static final RegistryObject<LeavesBlock> COCONUT_PALM_LEAVES = registerBlock("coconut_palm_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LEAVES))
    );

    public static final RegistryObject<PalmSaplingBlock> COCONUT_PALM_SAPLING = registerBlock("coconut_palm_sapling",
            () -> new PalmSaplingBlock(ModTreeGrowers.COCONUT_PALM, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_SAPLING))
    );

    // No item: placing the sapling in a flower pot gives this block (hooked up in KoppenClimateMod.commonSetup)
    public static final RegistryObject<FlowerPotBlock> POTTED_COCONUT_PALM_SAPLING = BLOCKS.register("potted_coconut_palm_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, COCONUT_PALM_SAPLING,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_JUNGLE_SAPLING))
    );

    // Mahogany wood set
    public static final RegistryObject<RotatedPillarBlock> MAHOGANY_LOG = registerBlock("mahogany_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG).mapColor(MapColor.TERRACOTTA_BROWN))
    );

    // bark on all six sides
    public static final RegistryObject<RotatedPillarBlock> MAHOGANY_WOOD = registerBlock("mahogany_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_WOOD).mapColor(MapColor.TERRACOTTA_BROWN))
    );

    public static final RegistryObject<Block> MAHOGANY_PLANKS = registerBlock("mahogany_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_PLANKS).mapColor(MapColor.TERRACOTTA_RED))
    );

    public static final RegistryObject<LeavesBlock> MAHOGANY_LEAVES = registerBlock("mahogany_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LEAVES))
    );

    public static final RegistryObject<SaplingBlock> MAHOGANY_SAPLING = registerBlock("mahogany_sapling",
            () -> new SaplingBlock(ModTreeGrowers.MAHOGANY, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_SAPLING))
    );

    public static final RegistryObject<FlowerPotBlock> POTTED_MAHOGANY_SAPLING = BLOCKS.register("potted_mahogany_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, MAHOGANY_SAPLING,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_JUNGLE_SAPLING))
    );

    // Banana plant: a soft green pseudostem rather than wood, so no planks
    public static final RegistryObject<RotatedPillarBlock> BANANA_STEM = registerBlock("banana_stem",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LOG)
                    .mapColor(MapColor.PLANT).strength(1.0F).sound(SoundType.STEM))
    );

    public static final RegistryObject<LeavesBlock> BANANA_LEAVES = registerBlock("banana_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_LEAVES))
    );

    public static final RegistryObject<SaplingBlock> BANANA_SAPLING = registerBlock("banana_sapling",
            () -> new SaplingBlock(ModTreeGrowers.BANANA, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_SAPLING))
    );

    public static final RegistryObject<FlowerPotBlock> POTTED_BANANA_SAPLING = BLOCKS.register("potted_banana_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BANANA_SAPLING,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_JUNGLE_SAPLING))
    );

    // No block item of its own: the banana (ModItems.BANANA) places it, like cocoa beans place cocoa
    public static final RegistryObject<BananaBunchBlock> BANANA_BUNCH = BLOCKS.register("banana_bunch",
            () -> new BananaBunchBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COCOA).mapColor(MapColor.PLANT))
    );

    // Lies flat over the whole block, so no random offset like other flowers. Its smell of rotting
    // meat makes suspicious stew give Nausea.
    public static final RegistryObject<RafflesiaBlock> RAFFLESIA = registerBlock("rafflesia",
            () -> new RafflesiaBlock(MobEffects.CONFUSION, 7.0F, BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)
                    .mapColor(MapColor.COLOR_RED).offsetType(BlockBehaviour.OffsetType.NONE))
    );

    // Replaces stone in the top 15 blocks under the Af biomes; mines like stone but keeps its own drop
    public static final RegistryObject<Block> YELLOW_LIMESTONE = registerBlock("yellow_limestone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.SAND).strength(1.2F, 4.0F))
    );

    // No block item of its own: the coconut (ModItems.COCONUT) places it on a palm log, like cocoa beans
    public static final RegistryObject<CoconutBlock> COCONUT = BLOCKS.register("coconut",
            () -> new CoconutBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COCOA).mapColor(MapColor.COLOR_BROWN))
    );

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, RegistryObject<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
