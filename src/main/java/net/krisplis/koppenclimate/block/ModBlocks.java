package net.krisplis.koppenclimate.block;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.custom.BananaBunchBlock;
import net.krisplis.koppenclimate.block.custom.CoconutBlock;
import net.krisplis.koppenclimate.block.custom.OxisolGrassBlock;
import net.krisplis.koppenclimate.block.custom.PalmSaplingBlock;
import net.krisplis.koppenclimate.block.custom.RafflesiaBlock;
import net.krisplis.koppenclimate.block.custom.RiceBlock;
import net.krisplis.koppenclimate.block.custom.SleepingMatBlock;
import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.krisplis.koppenclimate.block.custom.ModHangingSignBlock;
import net.krisplis.koppenclimate.block.custom.ModStandingSignBlock;
import net.krisplis.koppenclimate.block.custom.ModWallHangingSignBlock;
import net.krisplis.koppenclimate.block.custom.ModWallSignBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;
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

    // ------------------------------------------------------------------ the hot desert (BWh)
    // its yellow-orange sand, the sand drifted into dunes on top of it, and the sandstone under it
    public static final RegistryObject<ColoredFallingBlock> ORANGE_SAND = registerBlock("orange_sand",
            () -> new ColoredFallingBlock(new ColorRGBA(0xE0A04C),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_ORANGE))
    );
    public static final RegistryObject<Block> ORANGE_SAND_DUNE = registerBlock("orange_sand_dune",
            () -> new net.krisplis.koppenclimate.block.custom.SandDuneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SAND)
                    .mapColor(MapColor.COLOR_ORANGE).forceSolidOff().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );
    // the continental desert's (BWk) silvery-green grass tufts
    public static final RegistryObject<Block> SILVER_GRASS = registerBlock("silver_grass",
            () -> new net.krisplis.koppenclimate.block.custom.SilverGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS)
                    .mapColor(MapColor.COLOR_LIGHT_GRAY))
    );
    // ------------------------------------------------------------------ Lush desert (BSh) plants
    // the dry bush: a woody shrub like wild rosemary, in the grass's colour
    public static final RegistryObject<Block> DRY_BUSH = registerBlock("dry_bush",
            () -> new net.krisplis.koppenclimate.block.custom.DryBushBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                    .noCollission().strength(0.4F).sound(SoundType.AZALEA).ignitedByLava()
                    .noOcclusion().isViewBlocking((st, l, p) -> false).isSuffocating((st, l, p) -> false)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );
    // the giant forked saguaro cactus, and its flowers
    public static final RegistryObject<Block> SAGUARO = registerBlock("saguaro",
            () -> new net.krisplis.koppenclimate.block.custom.SaguaroBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CACTUS)
                    .strength(0.6F).noOcclusion().mapColor(MapColor.PLANT))
    );
    // the elbow where a saguaro's arm leaves the trunk (grown only: it drops a saguaro)
    public static final RegistryObject<Block> SAGUARO_ARM = BLOCKS.register("saguaro_arm",
            () -> new net.krisplis.koppenclimate.block.custom.SaguaroArmBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CACTUS)
                    .strength(0.6F).noOcclusion().mapColor(MapColor.PLANT)));
    public static final RegistryObject<Block> SAGUARO_FLOWER = registerBlock("saguaro_flower",
            () -> new net.krisplis.koppenclimate.block.custom.SaguaroFlowerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DANDELION)
                    .mapColor(MapColor.SNOW))
    );
    public static final RegistryObject<Block> ORANGE_SANDSTONE = registerBlock("orange_sandstone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE).mapColor(MapColor.COLOR_ORANGE))
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

    // Dressed limestone for building. It looks like yellow limestone but isn't natural stone, so ores never grow into it
    public static final RegistryObject<Block> CUT_YELLOW_LIMESTONE = registerBlock("cut_yellow_limestone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.SAND).strength(1.2F, 4.0F))
    );

    // A woven reed-and-bamboo mat to sleep on, two blocks long like a bed. Its item is ModItems.SLEEPING_MAT
    public static final RegistryObject<SleepingMatBlock> SLEEPING_MAT = BLOCKS.register("sleeping_mat",
            () -> new SleepingMatBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).sound(SoundType.GRASS)
                    .strength(0.2F).noOcclusion().ignitedByLava().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );

    // Jade: as rare as ancient debris is in the Nether. Mined, it gives a jade gem (more with Fortune); with
    // Silk Touch the ore itself, which smelts into a gem
    public static final RegistryObject<DropExperienceBlock> JADE_ORE = registerBlock("jade_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).mapColor(MapColor.COLOR_GREEN))
    );
    public static final RegistryObject<DropExperienceBlock> DEEPSLATE_JADE_ORE = registerBlock("deepslate_jade_ore",
            () -> new DropExperienceBlock(UniformInt.of(3, 7), BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_DIAMOND_ORE))
    );

    // Rice, growing in a block of water. No block item of its own: rice seeds (ModItems.RICE_SEEDS) plant it
    // barley, the Mediterranean farmland's second grain (item: ModItems.BARLEY_SEEDS)
    public static final RegistryObject<net.krisplis.koppenclimate.block.custom.BarleyBlock> BARLEY = BLOCKS.register("barley",
            () -> new net.krisplis.koppenclimate.block.custom.BarleyBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT))
    );
    public static final RegistryObject<RiceBlock> RICE = BLOCKS.register("rice",
            () -> new RiceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).sound(SoundType.WET_GRASS))
    );

    // No block item of its own: the coconut (ModItems.COCONUT) places it on a palm log, like cocoa beans
    public static final RegistryObject<CoconutBlock> COCONUT = BLOCKS.register("coconut",
            () -> new CoconutBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COCOA).mapColor(MapColor.COLOR_BROWN))
    );

    // ------------------------------------------------------------------ mahogany family
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_MAHOGANY_LOG = registerBlock("stripped_mahogany_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_JUNGLE_LOG).mapColor(MapColor.TERRACOTTA_RED))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_MAHOGANY_WOOD = registerBlock("stripped_mahogany_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_JUNGLE_WOOD).mapColor(MapColor.TERRACOTTA_RED))
    );
    public static final RegistryObject<StairBlock> MAHOGANY_STAIRS = registerBlock("mahogany_stairs",
            () -> new StairBlock(MAHOGANY_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(MAHOGANY_PLANKS.get()))
    );
    public static final RegistryObject<SlabBlock> MAHOGANY_SLAB = registerBlock("mahogany_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(MAHOGANY_PLANKS.get()))
    );
    public static final RegistryObject<FenceBlock> MAHOGANY_FENCE = registerBlock("mahogany_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_FENCE).mapColor(MapColor.TERRACOTTA_RED))
    );
    public static final RegistryObject<FenceGateBlock> MAHOGANY_FENCE_GATE = registerBlock("mahogany_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.MAHOGANY, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_FENCE_GATE).mapColor(MapColor.TERRACOTTA_RED))
    );
    // item: ModItems.MAHOGANY_DOOR, a two-block-high item like vanilla doors
    public static final RegistryObject<DoorBlock> MAHOGANY_DOOR = BLOCKS.register("mahogany_door",
            () -> new DoorBlock(ModWoodTypes.MAHOGANY.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_DOOR).mapColor(MapColor.TERRACOTTA_RED))
    );
    public static final RegistryObject<TrapDoorBlock> MAHOGANY_TRAPDOOR = registerBlock("mahogany_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.MAHOGANY.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_TRAPDOOR).mapColor(MapColor.TERRACOTTA_RED))
    );
    public static final RegistryObject<PressurePlateBlock> MAHOGANY_PRESSURE_PLATE = registerBlock("mahogany_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.MAHOGANY.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_PRESSURE_PLATE).mapColor(MapColor.TERRACOTTA_RED))
    );
    public static final RegistryObject<ButtonBlock> MAHOGANY_BUTTON = registerBlock("mahogany_button",
            () -> new ButtonBlock(ModWoodTypes.MAHOGANY.setType(), 30, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_BUTTON))
    );
    // signs have no block items of their own: the sign items (ModItems) place the standing or the wall version
    public static final RegistryObject<ModStandingSignBlock> MAHOGANY_SIGN = BLOCKS.register("mahogany_sign",
            () -> new ModStandingSignBlock(ModWoodTypes.MAHOGANY, signProperties(MapColor.TERRACOTTA_BROWN))
    );
    public static final RegistryObject<ModWallSignBlock> MAHOGANY_WALL_SIGN = BLOCKS.register("mahogany_wall_sign",
            () -> new ModWallSignBlock(ModWoodTypes.MAHOGANY, signProperties(MapColor.TERRACOTTA_BROWN).lootFrom(MAHOGANY_SIGN))
    );
    public static final RegistryObject<ModHangingSignBlock> MAHOGANY_HANGING_SIGN = BLOCKS.register("mahogany_hanging_sign",
            () -> new ModHangingSignBlock(ModWoodTypes.MAHOGANY, signProperties(MapColor.TERRACOTTA_BROWN))
    );
    public static final RegistryObject<ModWallHangingSignBlock> MAHOGANY_WALL_HANGING_SIGN = BLOCKS.register("mahogany_wall_hanging_sign",
            () -> new ModWallHangingSignBlock(ModWoodTypes.MAHOGANY, signProperties(MapColor.TERRACOTTA_BROWN).lootFrom(MAHOGANY_HANGING_SIGN))
    );

    // ------------------------------------------------------------------ coconut palm family
    // bark on all six sides
    public static final RegistryObject<RotatedPillarBlock> COCONUT_PALM_WOOD = registerBlock("coconut_palm_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_WOOD))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_COCONUT_PALM_LOG = registerBlock("stripped_coconut_palm_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_JUNGLE_LOG).mapColor(MapColor.TERRACOTTA_ORANGE))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_COCONUT_PALM_WOOD = registerBlock("stripped_coconut_palm_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_JUNGLE_WOOD).mapColor(MapColor.TERRACOTTA_ORANGE))
    );
    public static final RegistryObject<StairBlock> COCONUT_PALM_STAIRS = registerBlock("coconut_palm_stairs",
            () -> new StairBlock(COCONUT_PALM_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(COCONUT_PALM_PLANKS.get()))
    );
    public static final RegistryObject<SlabBlock> COCONUT_PALM_SLAB = registerBlock("coconut_palm_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(COCONUT_PALM_PLANKS.get()))
    );
    public static final RegistryObject<FenceBlock> COCONUT_PALM_FENCE = registerBlock("coconut_palm_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_FENCE))
    );
    public static final RegistryObject<FenceGateBlock> COCONUT_PALM_FENCE_GATE = registerBlock("coconut_palm_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.COCONUT_PALM, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_FENCE_GATE))
    );
    // item: ModItems.COCONUT_PALM_DOOR, a two-block-high item like vanilla doors
    public static final RegistryObject<DoorBlock> COCONUT_PALM_DOOR = BLOCKS.register("coconut_palm_door",
            () -> new DoorBlock(ModWoodTypes.COCONUT_PALM.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_DOOR))
    );
    public static final RegistryObject<TrapDoorBlock> COCONUT_PALM_TRAPDOOR = registerBlock("coconut_palm_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.COCONUT_PALM.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_TRAPDOOR))
    );
    public static final RegistryObject<PressurePlateBlock> COCONUT_PALM_PRESSURE_PLATE = registerBlock("coconut_palm_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.COCONUT_PALM.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> COCONUT_PALM_BUTTON = registerBlock("coconut_palm_button",
            () -> new ButtonBlock(ModWoodTypes.COCONUT_PALM.setType(), 30, BlockBehaviour.Properties.ofFullCopy(Blocks.JUNGLE_BUTTON))
    );
    public static final RegistryObject<ModStandingSignBlock> COCONUT_PALM_SIGN = BLOCKS.register("coconut_palm_sign",
            () -> new ModStandingSignBlock(ModWoodTypes.COCONUT_PALM, signProperties(MapColor.WOOD))
    );
    public static final RegistryObject<ModWallSignBlock> COCONUT_PALM_WALL_SIGN = BLOCKS.register("coconut_palm_wall_sign",
            () -> new ModWallSignBlock(ModWoodTypes.COCONUT_PALM, signProperties(MapColor.WOOD).lootFrom(COCONUT_PALM_SIGN))
    );
    public static final RegistryObject<ModHangingSignBlock> COCONUT_PALM_HANGING_SIGN = BLOCKS.register("coconut_palm_hanging_sign",
            () -> new ModHangingSignBlock(ModWoodTypes.COCONUT_PALM, signProperties(MapColor.WOOD))
    );
    public static final RegistryObject<ModWallHangingSignBlock> COCONUT_PALM_WALL_HANGING_SIGN = BLOCKS.register("coconut_palm_wall_hanging_sign",
            () -> new ModWallHangingSignBlock(ModWoodTypes.COCONUT_PALM, signProperties(MapColor.WOOD).lootFrom(COCONUT_PALM_HANGING_SIGN))
    );

    // ------------------------------------------------------------------ baobab family (African savanna)
    // pale, smooth, grey-pink bark; planks a light warm tan. Sounds and opens like acacia
    public static final RegistryObject<RotatedPillarBlock> BAOBAB_LOG = registerBlock("baobab_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> BAOBAB_WOOD = registerBlock("baobab_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_WOOD).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_BAOBAB_LOG = registerBlock("stripped_baobab_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_ACACIA_LOG).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_BAOBAB_WOOD = registerBlock("stripped_baobab_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_ACACIA_WOOD).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<Block> BAOBAB_PLANKS = registerBlock("baobab_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_PLANKS).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<LeavesBlock> BAOBAB_LEAVES = registerBlock("baobab_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LEAVES))
    );
    public static final RegistryObject<SaplingBlock> BAOBAB_SAPLING = registerBlock("baobab_sapling",
            () -> new SaplingBlock(net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers.BAOBAB, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_SAPLING))
    );
    public static final RegistryObject<FlowerPotBlock> POTTED_BAOBAB_SAPLING = BLOCKS.register("potted_baobab_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, BAOBAB_SAPLING,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ACACIA_SAPLING))
    );
    public static final RegistryObject<StairBlock> BAOBAB_STAIRS = registerBlock("baobab_stairs",
            () -> new StairBlock(BAOBAB_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(BAOBAB_PLANKS.get()))
    );
    public static final RegistryObject<SlabBlock> BAOBAB_SLAB = registerBlock("baobab_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(BAOBAB_PLANKS.get()))
    );
    public static final RegistryObject<FenceBlock> BAOBAB_FENCE = registerBlock("baobab_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<FenceGateBlock> BAOBAB_FENCE_GATE = registerBlock("baobab_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.BAOBAB, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE_GATE).mapColor(MapColor.SAND))
    );
    // item: ModItems.BAOBAB_DOOR
    public static final RegistryObject<DoorBlock> BAOBAB_DOOR = BLOCKS.register("baobab_door",
            () -> new DoorBlock(ModWoodTypes.BAOBAB.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_DOOR))
    );
    public static final RegistryObject<TrapDoorBlock> BAOBAB_TRAPDOOR = registerBlock("baobab_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.BAOBAB.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_TRAPDOOR))
    );
    public static final RegistryObject<PressurePlateBlock> BAOBAB_PRESSURE_PLATE = registerBlock("baobab_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.BAOBAB.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> BAOBAB_BUTTON = registerBlock("baobab_button",
            () -> new ButtonBlock(ModWoodTypes.BAOBAB.setType(), 30, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_BUTTON))
    );
    public static final RegistryObject<ModStandingSignBlock> BAOBAB_SIGN = BLOCKS.register("baobab_sign",
            () -> new ModStandingSignBlock(ModWoodTypes.BAOBAB, signProperties(MapColor.SAND))
    );
    public static final RegistryObject<ModWallSignBlock> BAOBAB_WALL_SIGN = BLOCKS.register("baobab_wall_sign",
            () -> new ModWallSignBlock(ModWoodTypes.BAOBAB, signProperties(MapColor.SAND).lootFrom(BAOBAB_SIGN))
    );
    public static final RegistryObject<ModHangingSignBlock> BAOBAB_HANGING_SIGN = BLOCKS.register("baobab_hanging_sign",
            () -> new ModHangingSignBlock(ModWoodTypes.BAOBAB, signProperties(MapColor.SAND))
    );
    public static final RegistryObject<ModWallHangingSignBlock> BAOBAB_WALL_HANGING_SIGN = BLOCKS.register("baobab_wall_hanging_sign",
            () -> new ModWallHangingSignBlock(ModWoodTypes.BAOBAB, signProperties(MapColor.SAND).lootFrom(BAOBAB_HANGING_SIGN))
    );

    // ------------------------------------------------------------------ olive family (Mediterranean)
    // knotty grey bark; pale cream wood with honey streaks. Sounds and opens like acacia
    public static final RegistryObject<RotatedPillarBlock> OLIVE_LOG = registerBlock("olive_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> OLIVE_WOOD = registerBlock("olive_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_WOOD).mapColor(MapColor.TERRACOTTA_LIGHT_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_OLIVE_LOG = registerBlock("stripped_olive_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_ACACIA_LOG).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_OLIVE_WOOD = registerBlock("stripped_olive_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_ACACIA_WOOD).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<Block> OLIVE_PLANKS = registerBlock("olive_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_PLANKS).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<LeavesBlock> OLIVE_LEAVES = registerBlock("olive_leaves",
            () -> new net.krisplis.koppenclimate.block.custom.OliveLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.AZALEA_LEAVES).mapColor(MapColor.GRASS))
    );
    public static final RegistryObject<SaplingBlock> OLIVE_SAPLING = registerBlock("olive_sapling",
            () -> new SaplingBlock(net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers.OLIVE, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_SAPLING))
    );
    public static final RegistryObject<FlowerPotBlock> POTTED_OLIVE_SAPLING = BLOCKS.register("potted_olive_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, OLIVE_SAPLING,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ACACIA_SAPLING))
    );
    public static final RegistryObject<StairBlock> OLIVE_STAIRS = registerBlock("olive_stairs",
            () -> new StairBlock(OLIVE_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(OLIVE_PLANKS.get()))
    );
    public static final RegistryObject<SlabBlock> OLIVE_SLAB = registerBlock("olive_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(OLIVE_PLANKS.get()))
    );
    public static final RegistryObject<FenceBlock> OLIVE_FENCE = registerBlock("olive_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<FenceGateBlock> OLIVE_FENCE_GATE = registerBlock("olive_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.OLIVE, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE_GATE).mapColor(MapColor.SAND))
    );
    // item: ModItems.OLIVE_DOOR
    public static final RegistryObject<DoorBlock> OLIVE_DOOR = BLOCKS.register("olive_door",
            () -> new DoorBlock(ModWoodTypes.OLIVE.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_DOOR))
    );
    public static final RegistryObject<TrapDoorBlock> OLIVE_TRAPDOOR = registerBlock("olive_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.OLIVE.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_TRAPDOOR))
    );
    public static final RegistryObject<PressurePlateBlock> OLIVE_PRESSURE_PLATE = registerBlock("olive_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.OLIVE.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> OLIVE_BUTTON = registerBlock("olive_button",
            () -> new ButtonBlock(ModWoodTypes.OLIVE.setType(), 30, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_BUTTON))
    );
    public static final RegistryObject<ModStandingSignBlock> OLIVE_SIGN = BLOCKS.register("olive_sign",
            () -> new ModStandingSignBlock(ModWoodTypes.OLIVE, signProperties(MapColor.SAND))
    );
    public static final RegistryObject<ModWallSignBlock> OLIVE_WALL_SIGN = BLOCKS.register("olive_wall_sign",
            () -> new ModWallSignBlock(ModWoodTypes.OLIVE, signProperties(MapColor.SAND).lootFrom(OLIVE_SIGN))
    );
    public static final RegistryObject<ModHangingSignBlock> OLIVE_HANGING_SIGN = BLOCKS.register("olive_hanging_sign",
            () -> new ModHangingSignBlock(ModWoodTypes.OLIVE, signProperties(MapColor.SAND))
    );
    public static final RegistryObject<ModWallHangingSignBlock> OLIVE_WALL_HANGING_SIGN = BLOCKS.register("olive_wall_hanging_sign",
            () -> new ModWallHangingSignBlock(ModWoodTypes.OLIVE, signProperties(MapColor.SAND).lootFrom(OLIVE_HANGING_SIGN))
    );

    // ------------------------------------------------------------------ Italian (stone) pine family (Mediterranean)
    // red-brown plated bark; warm orange-brown wood
    public static final RegistryObject<RotatedPillarBlock> ITALIAN_PINE_LOG = registerBlock("italian_pine_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_LOG).mapColor(MapColor.TERRACOTTA_BROWN))
    );
    public static final RegistryObject<RotatedPillarBlock> ITALIAN_PINE_WOOD = registerBlock("italian_pine_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_WOOD).mapColor(MapColor.TERRACOTTA_BROWN))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_ITALIAN_PINE_LOG = registerBlock("stripped_italian_pine_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_ACACIA_LOG).mapColor(MapColor.TERRACOTTA_ORANGE))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_ITALIAN_PINE_WOOD = registerBlock("stripped_italian_pine_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_ACACIA_WOOD).mapColor(MapColor.TERRACOTTA_ORANGE))
    );
    public static final RegistryObject<Block> ITALIAN_PINE_PLANKS = registerBlock("italian_pine_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_PLANKS).mapColor(MapColor.TERRACOTTA_ORANGE))
    );
    public static final RegistryObject<LeavesBlock> ITALIAN_PINE_LEAVES = registerBlock("italian_pine_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES).mapColor(MapColor.GRASS))
    );
    public static final RegistryObject<SaplingBlock> ITALIAN_PINE_SAPLING = registerBlock("italian_pine_sapling",
            () -> new SaplingBlock(net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers.ITALIAN_PINE, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_SAPLING))
    );
    public static final RegistryObject<FlowerPotBlock> POTTED_ITALIAN_PINE_SAPLING = BLOCKS.register("potted_italian_pine_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, ITALIAN_PINE_SAPLING,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ACACIA_SAPLING))
    );
    public static final RegistryObject<StairBlock> ITALIAN_PINE_STAIRS = registerBlock("italian_pine_stairs",
            () -> new StairBlock(ITALIAN_PINE_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(ITALIAN_PINE_PLANKS.get()))
    );
    public static final RegistryObject<SlabBlock> ITALIAN_PINE_SLAB = registerBlock("italian_pine_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(ITALIAN_PINE_PLANKS.get()))
    );
    public static final RegistryObject<FenceBlock> ITALIAN_PINE_FENCE = registerBlock("italian_pine_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE).mapColor(MapColor.TERRACOTTA_ORANGE))
    );
    public static final RegistryObject<FenceGateBlock> ITALIAN_PINE_FENCE_GATE = registerBlock("italian_pine_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.ITALIAN_PINE, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_FENCE_GATE).mapColor(MapColor.TERRACOTTA_ORANGE))
    );
    // item: ModItems.ITALIAN_PINE_DOOR
    public static final RegistryObject<DoorBlock> ITALIAN_PINE_DOOR = BLOCKS.register("italian_pine_door",
            () -> new DoorBlock(ModWoodTypes.ITALIAN_PINE.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_DOOR))
    );
    public static final RegistryObject<TrapDoorBlock> ITALIAN_PINE_TRAPDOOR = registerBlock("italian_pine_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.ITALIAN_PINE.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_TRAPDOOR))
    );
    public static final RegistryObject<PressurePlateBlock> ITALIAN_PINE_PRESSURE_PLATE = registerBlock("italian_pine_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.ITALIAN_PINE.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> ITALIAN_PINE_BUTTON = registerBlock("italian_pine_button",
            () -> new ButtonBlock(ModWoodTypes.ITALIAN_PINE.setType(), 30, BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_BUTTON))
    );
    public static final RegistryObject<ModStandingSignBlock> ITALIAN_PINE_SIGN = BLOCKS.register("italian_pine_sign",
            () -> new ModStandingSignBlock(ModWoodTypes.ITALIAN_PINE, signProperties(MapColor.TERRACOTTA_ORANGE))
    );
    public static final RegistryObject<ModWallSignBlock> ITALIAN_PINE_WALL_SIGN = BLOCKS.register("italian_pine_wall_sign",
            () -> new ModWallSignBlock(ModWoodTypes.ITALIAN_PINE, signProperties(MapColor.TERRACOTTA_ORANGE).lootFrom(ITALIAN_PINE_SIGN))
    );
    public static final RegistryObject<ModHangingSignBlock> ITALIAN_PINE_HANGING_SIGN = BLOCKS.register("italian_pine_hanging_sign",
            () -> new ModHangingSignBlock(ModWoodTypes.ITALIAN_PINE, signProperties(MapColor.TERRACOTTA_ORANGE))
    );
    public static final RegistryObject<ModWallHangingSignBlock> ITALIAN_PINE_WALL_HANGING_SIGN = BLOCKS.register("italian_pine_wall_hanging_sign",
            () -> new ModWallHangingSignBlock(ModWoodTypes.ITALIAN_PINE, signProperties(MapColor.TERRACOTTA_ORANGE).lootFrom(ITALIAN_PINE_HANGING_SIGN))
    );

    // ------------------------------------------------------------------ fig wood family (Mediterranean)
    // smooth pale-grey bark; soft creamy wood
    public static final RegistryObject<RotatedPillarBlock> FIG_LOG = registerBlock("fig_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_LIGHT_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> FIG_WOOD = registerBlock("fig_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(MapColor.COLOR_LIGHT_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_FIG_LOG = registerBlock("stripped_fig_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_FIG_WOOD = registerBlock("stripped_fig_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<Block> FIG_PLANKS = registerBlock("fig_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<StairBlock> FIG_STAIRS = registerBlock("fig_stairs",
            () -> new StairBlock(FIG_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(FIG_PLANKS.get()))
    );
    public static final RegistryObject<SlabBlock> FIG_SLAB = registerBlock("fig_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(FIG_PLANKS.get()))
    );
    public static final RegistryObject<FenceBlock> FIG_FENCE = registerBlock("fig_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(MapColor.SAND))
    );
    public static final RegistryObject<FenceGateBlock> FIG_FENCE_GATE = registerBlock("fig_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.FIG, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.SAND))
    );
    // item: ModItems.FIG_DOOR
    public static final RegistryObject<DoorBlock> FIG_DOOR = BLOCKS.register("fig_door",
            () -> new DoorBlock(ModWoodTypes.FIG.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR))
    );
    public static final RegistryObject<TrapDoorBlock> FIG_TRAPDOOR = registerBlock("fig_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.FIG.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR))
    );
    public static final RegistryObject<PressurePlateBlock> FIG_PRESSURE_PLATE = registerBlock("fig_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.FIG.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> FIG_BUTTON = registerBlock("fig_button",
            () -> new ButtonBlock(ModWoodTypes.FIG.setType(), 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON))
    );
    public static final RegistryObject<ModStandingSignBlock> FIG_SIGN = BLOCKS.register("fig_sign",
            () -> new ModStandingSignBlock(ModWoodTypes.FIG, signProperties(MapColor.SAND))
    );
    public static final RegistryObject<ModWallSignBlock> FIG_WALL_SIGN = BLOCKS.register("fig_wall_sign",
            () -> new ModWallSignBlock(ModWoodTypes.FIG, signProperties(MapColor.SAND).lootFrom(FIG_SIGN))
    );
    public static final RegistryObject<ModHangingSignBlock> FIG_HANGING_SIGN = BLOCKS.register("fig_hanging_sign",
            () -> new ModHangingSignBlock(ModWoodTypes.FIG, signProperties(MapColor.SAND))
    );
    public static final RegistryObject<ModWallHangingSignBlock> FIG_WALL_HANGING_SIGN = BLOCKS.register("fig_wall_hanging_sign",
            () -> new ModWallHangingSignBlock(ModWoodTypes.FIG, signProperties(MapColor.SAND).lootFrom(FIG_HANGING_SIGN))
    );


    // ------------------------------------------------------------------ Joshua tree wood family (Lush desert, BSh)
    // shaggy grey-brown bark; pale, fibrous tan wood
    public static final RegistryObject<RotatedPillarBlock> JOSHUA_LOG = registerBlock("joshua_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.TERRACOTTA_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> JOSHUA_WOOD = registerBlock("joshua_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(MapColor.TERRACOTTA_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_JOSHUA_LOG = registerBlock("stripped_joshua_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.TERRACOTTA_WHITE))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_JOSHUA_WOOD = registerBlock("stripped_joshua_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.TERRACOTTA_WHITE))
    );
    public static final RegistryObject<Block> JOSHUA_PLANKS = registerBlock("joshua_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.TERRACOTTA_WHITE))
    );
    public static final RegistryObject<StairBlock> JOSHUA_STAIRS = registerBlock("joshua_stairs",
            () -> new StairBlock(JOSHUA_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(JOSHUA_PLANKS.get()))
    );
    public static final RegistryObject<SlabBlock> JOSHUA_SLAB = registerBlock("joshua_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(JOSHUA_PLANKS.get()))
    );
    public static final RegistryObject<FenceBlock> JOSHUA_FENCE = registerBlock("joshua_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(MapColor.TERRACOTTA_WHITE))
    );
    public static final RegistryObject<FenceGateBlock> JOSHUA_FENCE_GATE = registerBlock("joshua_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.JOSHUA, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.TERRACOTTA_WHITE))
    );
    // item: ModItems.JOSHUA_DOOR
    public static final RegistryObject<DoorBlock> JOSHUA_DOOR = BLOCKS.register("joshua_door",
            () -> new DoorBlock(ModWoodTypes.JOSHUA.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR))
    );
    public static final RegistryObject<TrapDoorBlock> JOSHUA_TRAPDOOR = registerBlock("joshua_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.JOSHUA.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR))
    );
    public static final RegistryObject<PressurePlateBlock> JOSHUA_PRESSURE_PLATE = registerBlock("joshua_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.JOSHUA.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> JOSHUA_BUTTON = registerBlock("joshua_button",
            () -> new ButtonBlock(ModWoodTypes.JOSHUA.setType(), 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON))
    );
    public static final RegistryObject<ModStandingSignBlock> JOSHUA_SIGN = BLOCKS.register("joshua_sign",
            () -> new ModStandingSignBlock(ModWoodTypes.JOSHUA, signProperties(MapColor.TERRACOTTA_WHITE))
    );
    public static final RegistryObject<ModWallSignBlock> JOSHUA_WALL_SIGN = BLOCKS.register("joshua_wall_sign",
            () -> new ModWallSignBlock(ModWoodTypes.JOSHUA, signProperties(MapColor.TERRACOTTA_WHITE).lootFrom(JOSHUA_SIGN))
    );
    public static final RegistryObject<ModHangingSignBlock> JOSHUA_HANGING_SIGN = BLOCKS.register("joshua_hanging_sign",
            () -> new ModHangingSignBlock(ModWoodTypes.JOSHUA, signProperties(MapColor.TERRACOTTA_WHITE))
    );
    public static final RegistryObject<ModWallHangingSignBlock> JOSHUA_WALL_HANGING_SIGN = BLOCKS.register("joshua_wall_hanging_sign",
            () -> new ModWallHangingSignBlock(ModWoodTypes.JOSHUA, signProperties(MapColor.TERRACOTTA_WHITE).lootFrom(JOSHUA_HANGING_SIGN))
    );


    // the dead oak's bare grey twigs, in place of leaves
    public static final RegistryObject<LeavesBlock> DEAD_OAK_LEAVES = registerBlock("dead_oak_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.COLOR_GRAY)
                    .sound(net.minecraft.world.level.block.SoundType.AZALEA))
    );
    public static final RegistryObject<LeavesBlock> JOSHUA_LEAVES = registerBlock("joshua_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.TERRACOTTA_GREEN))
    );
    // takes root in sand as well as dirt
    public static final RegistryObject<net.krisplis.koppenclimate.block.custom.PalmSaplingBlock> JOSHUA_SAPLING = registerBlock("joshua_sapling",
            () -> new net.krisplis.koppenclimate.block.custom.PalmSaplingBlock(net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers.JOSHUA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
    );
    public static final RegistryObject<FlowerPotBlock> POTTED_JOSHUA_SAPLING = BLOCKS.register("potted_joshua_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, JOSHUA_SAPLING, BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING))
    );

    // ------------------------------------------------------------------ dead oak wood family (Lush desert, BSh)
    // bare, sun-bleached grey bark; weathered grey wood. A dead tree: no leaves, no sapling
    public static final RegistryObject<RotatedPillarBlock> DEAD_OAK_LOG = registerBlock("dead_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> DEAD_OAK_WOOD = registerBlock("dead_oak_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(MapColor.COLOR_GRAY))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_DEAD_OAK_LOG = registerBlock("stripped_dead_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.STONE))
    );
    public static final RegistryObject<RotatedPillarBlock> STRIPPED_DEAD_OAK_WOOD = registerBlock("stripped_dead_oak_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.STONE))
    );
    public static final RegistryObject<Block> DEAD_OAK_PLANKS = registerBlock("dead_oak_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.STONE))
    );
    public static final RegistryObject<StairBlock> DEAD_OAK_STAIRS = registerBlock("dead_oak_stairs",
            () -> new StairBlock(DEAD_OAK_PLANKS.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(DEAD_OAK_PLANKS.get()))
    );
    public static final RegistryObject<SlabBlock> DEAD_OAK_SLAB = registerBlock("dead_oak_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(DEAD_OAK_PLANKS.get()))
    );
    public static final RegistryObject<FenceBlock> DEAD_OAK_FENCE = registerBlock("dead_oak_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(MapColor.STONE))
    );
    public static final RegistryObject<FenceGateBlock> DEAD_OAK_FENCE_GATE = registerBlock("dead_oak_fence_gate",
            () -> new FenceGateBlock(ModWoodTypes.DEAD_OAK, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.STONE))
    );
    // item: ModItems.DEAD_OAK_DOOR
    public static final RegistryObject<DoorBlock> DEAD_OAK_DOOR = BLOCKS.register("dead_oak_door",
            () -> new DoorBlock(ModWoodTypes.DEAD_OAK.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR))
    );
    public static final RegistryObject<TrapDoorBlock> DEAD_OAK_TRAPDOOR = registerBlock("dead_oak_trapdoor",
            () -> new TrapDoorBlock(ModWoodTypes.DEAD_OAK.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR))
    );
    public static final RegistryObject<PressurePlateBlock> DEAD_OAK_PRESSURE_PLATE = registerBlock("dead_oak_pressure_plate",
            () -> new PressurePlateBlock(ModWoodTypes.DEAD_OAK.setType(), BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> DEAD_OAK_BUTTON = registerBlock("dead_oak_button",
            () -> new ButtonBlock(ModWoodTypes.DEAD_OAK.setType(), 30, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON))
    );
    public static final RegistryObject<ModStandingSignBlock> DEAD_OAK_SIGN = BLOCKS.register("dead_oak_sign",
            () -> new ModStandingSignBlock(ModWoodTypes.DEAD_OAK, signProperties(MapColor.STONE))
    );
    public static final RegistryObject<ModWallSignBlock> DEAD_OAK_WALL_SIGN = BLOCKS.register("dead_oak_wall_sign",
            () -> new ModWallSignBlock(ModWoodTypes.DEAD_OAK, signProperties(MapColor.STONE).lootFrom(DEAD_OAK_SIGN))
    );
    public static final RegistryObject<ModHangingSignBlock> DEAD_OAK_HANGING_SIGN = BLOCKS.register("dead_oak_hanging_sign",
            () -> new ModHangingSignBlock(ModWoodTypes.DEAD_OAK, signProperties(MapColor.STONE))
    );
    public static final RegistryObject<ModWallHangingSignBlock> DEAD_OAK_WALL_HANGING_SIGN = BLOCKS.register("dead_oak_wall_hanging_sign",
            () -> new ModWallHangingSignBlock(ModWoodTypes.DEAD_OAK, signProperties(MapColor.STONE).lootFrom(DEAD_OAK_HANGING_SIGN))
    );



    // ------------------------------------------------------------------ cypress (Mediterranean): leaves on a spruce trunk, no wood of its own
    public static final RegistryObject<LeavesBlock> CYPRESS_LEAVES = registerBlock("cypress_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES).mapColor(MapColor.COLOR_GREEN))
    );
    public static final RegistryObject<SaplingBlock> CYPRESS_SAPLING = registerBlock("cypress_sapling",
            () -> new SaplingBlock(net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers.CYPRESS, BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING))
    );
    public static final RegistryObject<FlowerPotBlock> POTTED_CYPRESS_SAPLING = BLOCKS.register("potted_cypress_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, CYPRESS_SAPLING,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_SPRUCE_SAPLING))
    );

    // ------------------------------------------------------------------ fig and lemon trees (Mediterranean, rare): fruit-bearing leaves
    public static final RegistryObject<LeavesBlock> FIG_LEAVES = registerBlock("fig_leaves",
            () -> new net.krisplis.koppenclimate.block.custom.FruitLeavesBlock(() -> net.krisplis.koppenclimate.item.ModItems.FIG.get(), 20, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.COLOR_GREEN))
    );
    public static final RegistryObject<SaplingBlock> FIG_SAPLING = registerBlock("fig_sapling",
            () -> new SaplingBlock(net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers.FIG, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
    );
    public static final RegistryObject<FlowerPotBlock> POTTED_FIG_SAPLING = BLOCKS.register("potted_fig_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, FIG_SAPLING, BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING))
    );
    public static final RegistryObject<LeavesBlock> LEMON_LEAVES = registerBlock("lemon_leaves",
            () -> new net.krisplis.koppenclimate.block.custom.FruitLeavesBlock(() -> net.krisplis.koppenclimate.item.ModItems.LEMON.get(), 25, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.PLANT))
    );
    public static final RegistryObject<SaplingBlock> LEMON_SAPLING = registerBlock("lemon_sapling",
            () -> new SaplingBlock(net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers.LEMON, BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING))
    );
    public static final RegistryObject<FlowerPotBlock> POTTED_LEMON_SAPLING = BLOCKS.register("potted_lemon_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, LEMON_SAPLING, BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING))
    );
    // lavender: the Mediterranean's only flower, and a rare one
    public static final RegistryObject<net.minecraft.world.level.block.FlowerBlock> LAVENDER = registerBlock("lavender",
            () -> new net.minecraft.world.level.block.FlowerBlock(MobEffects.REGENERATION, 5.0F,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.ALLIUM).mapColor(MapColor.COLOR_PURPLE))
    );
    public static final RegistryObject<FlowerPotBlock> POTTED_LAVENDER = BLOCKS.register("potted_lavender",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, LAVENDER, BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ALLIUM))
    );

    // ------------------------------------------------------------------ agave, nerium and the grapevine (Mediterranean)
    // the agave rosette (AGE 0-3) and the woody flower stalk a grown one sends up
    public static final RegistryObject<Block> AGAVE = registerBlock("agave",
            () -> new net.krisplis.koppenclimate.block.custom.AgaveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .noCollission().instabreak().sound(SoundType.AZALEA).randomTicks()
                    .ignitedByLava().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );
    public static final RegistryObject<Block> AGAVE_STALK = BLOCKS.register("agave_stalk",
            () -> new net.krisplis.koppenclimate.block.custom.AgaveStalkBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(0.6F).sound(SoundType.BAMBOO).noOcclusion().ignitedByLava()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );
    // nerium (oleander): a poisonous flowering bush, built of these blocks
    public static final RegistryObject<Block> NERIUM = registerBlock("nerium",
            () -> new net.krisplis.koppenclimate.block.custom.NeriumBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK)
                    .strength(0.3F).sound(SoundType.AZALEA_LEAVES).randomTicks().noOcclusion()
                    .isValidSpawn((st, l, p, e) -> false).isSuffocating((st, l, p) -> false).isViewBlocking((st, l, p) -> false)
                    .ignitedByLava().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );
    // a wine pole, for a grapevine to climb
    public static final RegistryObject<Block> WINE_POLE = registerBlock("wine_pole",
            () -> new net.krisplis.koppenclimate.block.custom.WinePoleBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .strength(0.8F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );

    // terracotta roof tiles (coppi): a sheet of tiles tilted at 45 degrees, for the red roofs of Tuscany
    public static final RegistryObject<Block> TERRACOTTA_ROOF_TILES = registerBlock("terracotta_roof_tiles",
            () -> new net.krisplis.koppenclimate.block.custom.RoofTileBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_RED)
                    .strength(1.25F, 4.2F).sound(SoundType.DECORATED_POT).requiresCorrectToolForDrops().noOcclusion())
    );
    // a nerium kept in a flower pot
    public static final RegistryObject<FlowerPotBlock> POTTED_NERIUM = BLOCKS.register("potted_nerium",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, NERIUM, BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_AZALEA))
    );

    // ------------------------------------------------------------------ marble (Mediterranean): Carrara-white, for classical building
    private static BlockBehaviour.Properties marble() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.QUARTZ).strength(1.6F, 6.0F);
    }
    public static final RegistryObject<Block> MARBLE = registerBlock("marble", () -> new Block(marble()));
    public static final RegistryObject<Block> POLISHED_MARBLE = registerBlock("polished_marble", () -> new Block(marble()));
    public static final RegistryObject<Block> MARBLE_BRICKS = registerBlock("marble_bricks", () -> new Block(marble()));
    public static final RegistryObject<Block> CRACKED_MARBLE_BRICKS = registerBlock("cracked_marble_bricks", () -> new Block(marble()));
    public static final RegistryObject<Block> MOSSY_MARBLE_BRICKS = registerBlock("mossy_marble_bricks", () -> new Block(marble()));
    public static final RegistryObject<Block> MARBLE_TILES = registerBlock("marble_tiles", () -> new Block(marble()));
    public static final RegistryObject<Block> CHISELED_MARBLE = registerBlock("chiseled_marble", () -> new Block(marble()));
    public static final RegistryObject<Block> MARBLE_COLUMN_CAPITAL = registerBlock("marble_column_capital", () -> new Block(marble()));
    public static final RegistryObject<Block> MARBLE_COLUMN_BASE = registerBlock("marble_column_base", () -> new Block(marble()));
    public static final RegistryObject<RotatedPillarBlock> MARBLE_PILLAR = registerBlock("marble_pillar", () -> new RotatedPillarBlock(marble()));
    public static final RegistryObject<RotatedPillarBlock> FLUTED_MARBLE_COLUMN = registerBlock("fluted_marble_column", () -> new RotatedPillarBlock(marble()));
    public static final RegistryObject<StairBlock> MARBLE_STAIRS = registerBlock("marble_stairs", () -> new StairBlock(MARBLE.get().defaultBlockState(), marble()));
    public static final RegistryObject<SlabBlock> MARBLE_SLAB = registerBlock("marble_slab", () -> new SlabBlock(marble()));
    public static final RegistryObject<WallBlock> MARBLE_WALL = registerBlock("marble_wall", () -> new WallBlock(marble().forceSolidOn()));
    public static final RegistryObject<StairBlock> POLISHED_MARBLE_STAIRS = registerBlock("polished_marble_stairs", () -> new StairBlock(POLISHED_MARBLE.get().defaultBlockState(), marble()));
    public static final RegistryObject<SlabBlock> POLISHED_MARBLE_SLAB = registerBlock("polished_marble_slab", () -> new SlabBlock(marble()));
    public static final RegistryObject<WallBlock> POLISHED_MARBLE_WALL = registerBlock("polished_marble_wall", () -> new WallBlock(marble().forceSolidOn()));
    public static final RegistryObject<StairBlock> MARBLE_BRICK_STAIRS = registerBlock("marble_brick_stairs", () -> new StairBlock(MARBLE_BRICKS.get().defaultBlockState(), marble()));
    public static final RegistryObject<SlabBlock> MARBLE_BRICK_SLAB = registerBlock("marble_brick_slab", () -> new SlabBlock(marble()));
    public static final RegistryObject<WallBlock> MARBLE_BRICK_WALL = registerBlock("marble_brick_wall", () -> new WallBlock(marble().forceSolidOn()));
    public static final RegistryObject<StairBlock> MOSSY_MARBLE_BRICK_STAIRS = registerBlock("mossy_marble_brick_stairs", () -> new StairBlock(MOSSY_MARBLE_BRICKS.get().defaultBlockState(), marble()));
    public static final RegistryObject<SlabBlock> MOSSY_MARBLE_BRICK_SLAB = registerBlock("mossy_marble_brick_slab", () -> new SlabBlock(marble()));
    public static final RegistryObject<WallBlock> MOSSY_MARBLE_BRICK_WALL = registerBlock("mossy_marble_brick_wall", () -> new WallBlock(marble().forceSolidOn()));
    public static final RegistryObject<StairBlock> MARBLE_TILE_STAIRS = registerBlock("marble_tile_stairs", () -> new StairBlock(MARBLE_TILES.get().defaultBlockState(), marble()));
    public static final RegistryObject<SlabBlock> MARBLE_TILE_SLAB = registerBlock("marble_tile_slab", () -> new SlabBlock(marble()));
    public static final RegistryObject<WallBlock> MARBLE_TILE_WALL = registerBlock("marble_tile_wall", () -> new WallBlock(marble().forceSolidOn()));

    // ------------------------------------------------------------------ Mediterranean ground and plants
    // terra rossa: the red-brown soil of the limestone hills
    public static final RegistryObject<Block> CINNAMON_DIRT = registerBlock("cinnamon_dirt",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.TERRACOTTA_RED))
    );
    // grass on cinnamon dirt, drawn in the dry silvery-to-straw green of the Mediterranean; spreads over cinnamon dirt
    public static final RegistryObject<Block> CINNAMON_GRASS_BLOCK = registerBlock("cinnamon_grass_block",
            () -> new OxisolGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK), CINNAMON_DIRT)
    );
    // the pale pebbles of the Mediterranean shore; falls like gravel
    public static final RegistryObject<Block> WHITE_GRAVEL = registerBlock("white_gravel",
            () -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0xFFD8D6CE),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GRAVEL).mapColor(MapColor.QUARTZ))
    );
    // wild rosemary, one or two blocks tall: slows you down, drops sticks and sometimes rosemary
    public static final RegistryObject<Block> ROSEMARY = registerBlock("rosemary",
            () -> new net.krisplis.koppenclimate.block.custom.RosemaryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                    .noCollission().strength(0.4F).sound(SoundType.AZALEA).ignitedByLava()
                    .noOcclusion().isViewBlocking((st, l, p) -> false).isSuffocating((st, l, p) -> false)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );
    public static final RegistryObject<Block> TALL_ROSEMARY = registerBlock("tall_rosemary",
            () -> new net.krisplis.koppenclimate.block.custom.TallRosemaryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT)
                    .noCollission().strength(0.4F).sound(SoundType.AZALEA).ignitedByLava()
                    .noOcclusion().isViewBlocking((st, l, p) -> false).isSuffocating((st, l, p) -> false)
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY))
    );

    // ------------------------------------------------------------------ African savanna ground
    // oxisol grass whose grass is always the straw yellow of the dry savanna (drawn yellow, not tinted)
    public static final RegistryObject<Block> SAVANNA_OXISOL_GRASS = registerBlock("savanna_oxisol_grass",
            () -> new OxisolGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.6F).sound(SoundType.GRASS).randomTicks(), OXISOL)
    );
    // the savanna's dry, straw-yellow grass, short and tall (untinted)
    public static final RegistryObject<net.krisplis.koppenclimate.block.custom.DryGrassBlock> DRY_GRASS = registerBlock("dry_grass",
            () -> new net.krisplis.koppenclimate.block.custom.DryGrassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_YELLOW))
    );
    public static final RegistryObject<net.minecraft.world.level.block.DoublePlantBlock> TALL_DRY_GRASS = registerBlock("tall_dry_grass",
            () -> new net.minecraft.world.level.block.DoublePlantBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS).mapColor(MapColor.COLOR_YELLOW))
    );
    // the earth of a termite colony's mound; termites live in it
    public static final RegistryObject<Block> TERMITE_MOUND = registerBlock("termite_mound",
            () -> new net.krisplis.koppenclimate.block.custom.TermiteMoundBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.5F).sound(SoundType.MUD_BRICKS).randomTicks())
    );

    // ------------------------------------------------------------------ yellow limestone family
    // Mined without Silk Touch (or blown up), yellow limestone drops this, like stone drops cobblestone. It's used
    // like cobblestone too (furnaces, stone tools, smelts back into limestone). Not natural stone, so ores never grow
    // into it - the Amazon villages are built of it.
    public static final RegistryObject<Block> COBBLED_YELLOW_LIMESTONE = registerBlock("cobbled_yellow_limestone",
            () -> new Block(limestone(Blocks.COBBLESTONE))
    );
    public static final RegistryObject<StairBlock> YELLOW_LIMESTONE_STAIRS = registerBlock("yellow_limestone_stairs",
            () -> new StairBlock(YELLOW_LIMESTONE.get().defaultBlockState(), limestone(Blocks.STONE_STAIRS))
    );
    public static final RegistryObject<SlabBlock> YELLOW_LIMESTONE_SLAB = registerBlock("yellow_limestone_slab",
            () -> new SlabBlock(limestone(Blocks.STONE_SLAB))
    );
    public static final RegistryObject<PressurePlateBlock> YELLOW_LIMESTONE_PRESSURE_PLATE = registerBlock("yellow_limestone_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.STONE, limestone(Blocks.STONE_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> YELLOW_LIMESTONE_BUTTON = registerBlock("yellow_limestone_button",
            () -> new ButtonBlock(BlockSetType.STONE, 20, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON))
    );
    public static final RegistryObject<StairBlock> COBBLED_YELLOW_LIMESTONE_STAIRS = registerBlock("cobbled_yellow_limestone_stairs",
            () -> new StairBlock(COBBLED_YELLOW_LIMESTONE.get().defaultBlockState(), limestone(Blocks.COBBLESTONE_STAIRS))
    );
    public static final RegistryObject<SlabBlock> COBBLED_YELLOW_LIMESTONE_SLAB = registerBlock("cobbled_yellow_limestone_slab",
            () -> new SlabBlock(limestone(Blocks.COBBLESTONE_SLAB))
    );
    public static final RegistryObject<WallBlock> COBBLED_YELLOW_LIMESTONE_WALL = registerBlock("cobbled_yellow_limestone_wall",
            () -> new WallBlock(limestone(Blocks.COBBLESTONE_WALL))
    );
    public static final RegistryObject<Block> MOSSY_COBBLED_YELLOW_LIMESTONE = registerBlock("mossy_cobbled_yellow_limestone",
            () -> new Block(limestone(Blocks.MOSSY_COBBLESTONE))
    );
    public static final RegistryObject<StairBlock> MOSSY_COBBLED_YELLOW_LIMESTONE_STAIRS = registerBlock("mossy_cobbled_yellow_limestone_stairs",
            () -> new StairBlock(MOSSY_COBBLED_YELLOW_LIMESTONE.get().defaultBlockState(), limestone(Blocks.MOSSY_COBBLESTONE_STAIRS))
    );
    public static final RegistryObject<SlabBlock> MOSSY_COBBLED_YELLOW_LIMESTONE_SLAB = registerBlock("mossy_cobbled_yellow_limestone_slab",
            () -> new SlabBlock(limestone(Blocks.MOSSY_COBBLESTONE_SLAB))
    );
    public static final RegistryObject<WallBlock> MOSSY_COBBLED_YELLOW_LIMESTONE_WALL = registerBlock("mossy_cobbled_yellow_limestone_wall",
            () -> new WallBlock(limestone(Blocks.MOSSY_COBBLESTONE_WALL))
    );
    public static final RegistryObject<Block> SMOOTH_YELLOW_LIMESTONE = registerBlock("smooth_yellow_limestone",
            () -> new Block(limestone(Blocks.SMOOTH_STONE))
    );
    public static final RegistryObject<SlabBlock> SMOOTH_YELLOW_LIMESTONE_SLAB = registerBlock("smooth_yellow_limestone_slab",
            () -> new SlabBlock(limestone(Blocks.SMOOTH_STONE_SLAB))
    );
    public static final RegistryObject<Block> YELLOW_LIMESTONE_BRICKS = registerBlock("yellow_limestone_bricks",
            () -> new Block(limestone(Blocks.STONE_BRICKS))
    );
    public static final RegistryObject<StairBlock> YELLOW_LIMESTONE_BRICK_STAIRS = registerBlock("yellow_limestone_brick_stairs",
            () -> new StairBlock(YELLOW_LIMESTONE_BRICKS.get().defaultBlockState(), limestone(Blocks.STONE_BRICK_STAIRS))
    );
    public static final RegistryObject<SlabBlock> YELLOW_LIMESTONE_BRICK_SLAB = registerBlock("yellow_limestone_brick_slab",
            () -> new SlabBlock(limestone(Blocks.STONE_BRICK_SLAB))
    );
    public static final RegistryObject<WallBlock> YELLOW_LIMESTONE_BRICK_WALL = registerBlock("yellow_limestone_brick_wall",
            () -> new WallBlock(limestone(Blocks.STONE_BRICK_WALL))
    );
    public static final RegistryObject<Block> CRACKED_YELLOW_LIMESTONE_BRICKS = registerBlock("cracked_yellow_limestone_bricks",
            () -> new Block(limestone(Blocks.CRACKED_STONE_BRICKS))
    );
    public static final RegistryObject<Block> CHISELED_YELLOW_LIMESTONE_BRICKS = registerBlock("chiseled_yellow_limestone_bricks",
            () -> new Block(limestone(Blocks.CHISELED_STONE_BRICKS))
    );
    public static final RegistryObject<Block> MOSSY_YELLOW_LIMESTONE_BRICKS = registerBlock("mossy_yellow_limestone_bricks",
            () -> new Block(limestone(Blocks.MOSSY_STONE_BRICKS))
    );
    public static final RegistryObject<StairBlock> MOSSY_YELLOW_LIMESTONE_BRICK_STAIRS = registerBlock("mossy_yellow_limestone_brick_stairs",
            () -> new StairBlock(MOSSY_YELLOW_LIMESTONE_BRICKS.get().defaultBlockState(), limestone(Blocks.MOSSY_STONE_BRICK_STAIRS))
    );
    public static final RegistryObject<SlabBlock> MOSSY_YELLOW_LIMESTONE_BRICK_SLAB = registerBlock("mossy_yellow_limestone_brick_slab",
            () -> new SlabBlock(limestone(Blocks.MOSSY_STONE_BRICK_SLAB))
    );
    public static final RegistryObject<WallBlock> MOSSY_YELLOW_LIMESTONE_BRICK_WALL = registerBlock("mossy_yellow_limestone_brick_wall",
            () -> new WallBlock(limestone(Blocks.MOSSY_STONE_BRICK_WALL))
    );
    public static final RegistryObject<SlabBlock> CUT_YELLOW_LIMESTONE_SLAB = registerBlock("cut_yellow_limestone_slab",
            () -> new SlabBlock(limestone(Blocks.STONE_SLAB))
    );

    // ------------------------------------------------------------------ white limestone family (Mediterranean)
    // White limestone: the bright karst rock of the Mediterranean hills (the Dalmatian coast); replaces stone under the Csa biomes
    public static final RegistryObject<Block> WHITE_LIMESTONE = registerBlock("white_limestone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.QUARTZ).strength(1.2F, 4.0F))
    );

    // Dressed limestone for building. It looks like white limestone but isn't natural stone, so ores never grow into it
    public static final RegistryObject<Block> CUT_WHITE_LIMESTONE = registerBlock("cut_white_limestone",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.QUARTZ).strength(1.2F, 4.0F))
    );

    // ------------------------------------------------------------------ white limestone family
    // Mined without Silk Touch (or blown up), white limestone drops this, like stone drops cobblestone. It's used
    // like cobblestone too (furnaces, stone tools, smelts back into limestone). Not natural stone, so ores never grow
    // into it - 
    public static final RegistryObject<Block> COBBLED_WHITE_LIMESTONE = registerBlock("cobbled_white_limestone",
            () -> new Block(whiteLimestone(Blocks.COBBLESTONE))
    );
    public static final RegistryObject<StairBlock> WHITE_LIMESTONE_STAIRS = registerBlock("white_limestone_stairs",
            () -> new StairBlock(WHITE_LIMESTONE.get().defaultBlockState(), whiteLimestone(Blocks.STONE_STAIRS))
    );
    public static final RegistryObject<SlabBlock> WHITE_LIMESTONE_SLAB = registerBlock("white_limestone_slab",
            () -> new SlabBlock(whiteLimestone(Blocks.STONE_SLAB))
    );
    public static final RegistryObject<PressurePlateBlock> WHITE_LIMESTONE_PRESSURE_PLATE = registerBlock("white_limestone_pressure_plate",
            () -> new PressurePlateBlock(BlockSetType.STONE, whiteLimestone(Blocks.STONE_PRESSURE_PLATE))
    );
    public static final RegistryObject<ButtonBlock> WHITE_LIMESTONE_BUTTON = registerBlock("white_limestone_button",
            () -> new ButtonBlock(BlockSetType.STONE, 20, BlockBehaviour.Properties.ofFullCopy(Blocks.STONE_BUTTON))
    );
    public static final RegistryObject<StairBlock> COBBLED_WHITE_LIMESTONE_STAIRS = registerBlock("cobbled_white_limestone_stairs",
            () -> new StairBlock(COBBLED_WHITE_LIMESTONE.get().defaultBlockState(), whiteLimestone(Blocks.COBBLESTONE_STAIRS))
    );
    public static final RegistryObject<SlabBlock> COBBLED_WHITE_LIMESTONE_SLAB = registerBlock("cobbled_white_limestone_slab",
            () -> new SlabBlock(whiteLimestone(Blocks.COBBLESTONE_SLAB))
    );
    public static final RegistryObject<WallBlock> COBBLED_WHITE_LIMESTONE_WALL = registerBlock("cobbled_white_limestone_wall",
            () -> new WallBlock(whiteLimestone(Blocks.COBBLESTONE_WALL))
    );
    public static final RegistryObject<Block> MOSSY_COBBLED_WHITE_LIMESTONE = registerBlock("mossy_cobbled_white_limestone",
            () -> new Block(whiteLimestone(Blocks.MOSSY_COBBLESTONE))
    );
    public static final RegistryObject<StairBlock> MOSSY_COBBLED_WHITE_LIMESTONE_STAIRS = registerBlock("mossy_cobbled_white_limestone_stairs",
            () -> new StairBlock(MOSSY_COBBLED_WHITE_LIMESTONE.get().defaultBlockState(), whiteLimestone(Blocks.MOSSY_COBBLESTONE_STAIRS))
    );
    public static final RegistryObject<SlabBlock> MOSSY_COBBLED_WHITE_LIMESTONE_SLAB = registerBlock("mossy_cobbled_white_limestone_slab",
            () -> new SlabBlock(whiteLimestone(Blocks.MOSSY_COBBLESTONE_SLAB))
    );
    public static final RegistryObject<WallBlock> MOSSY_COBBLED_WHITE_LIMESTONE_WALL = registerBlock("mossy_cobbled_white_limestone_wall",
            () -> new WallBlock(whiteLimestone(Blocks.MOSSY_COBBLESTONE_WALL))
    );
    public static final RegistryObject<Block> SMOOTH_WHITE_LIMESTONE = registerBlock("smooth_white_limestone",
            () -> new Block(whiteLimestone(Blocks.SMOOTH_STONE))
    );
    public static final RegistryObject<SlabBlock> SMOOTH_WHITE_LIMESTONE_SLAB = registerBlock("smooth_white_limestone_slab",
            () -> new SlabBlock(whiteLimestone(Blocks.SMOOTH_STONE_SLAB))
    );
    public static final RegistryObject<Block> WHITE_LIMESTONE_BRICKS = registerBlock("white_limestone_bricks",
            () -> new Block(whiteLimestone(Blocks.STONE_BRICKS))
    );
    public static final RegistryObject<StairBlock> WHITE_LIMESTONE_BRICK_STAIRS = registerBlock("white_limestone_brick_stairs",
            () -> new StairBlock(WHITE_LIMESTONE_BRICKS.get().defaultBlockState(), whiteLimestone(Blocks.STONE_BRICK_STAIRS))
    );
    public static final RegistryObject<SlabBlock> WHITE_LIMESTONE_BRICK_SLAB = registerBlock("white_limestone_brick_slab",
            () -> new SlabBlock(whiteLimestone(Blocks.STONE_BRICK_SLAB))
    );
    public static final RegistryObject<WallBlock> WHITE_LIMESTONE_BRICK_WALL = registerBlock("white_limestone_brick_wall",
            () -> new WallBlock(whiteLimestone(Blocks.STONE_BRICK_WALL))
    );
    public static final RegistryObject<Block> CRACKED_WHITE_LIMESTONE_BRICKS = registerBlock("cracked_white_limestone_bricks",
            () -> new Block(whiteLimestone(Blocks.CRACKED_STONE_BRICKS))
    );
    public static final RegistryObject<Block> CHISELED_WHITE_LIMESTONE_BRICKS = registerBlock("chiseled_white_limestone_bricks",
            () -> new Block(whiteLimestone(Blocks.CHISELED_STONE_BRICKS))
    );
    public static final RegistryObject<Block> MOSSY_WHITE_LIMESTONE_BRICKS = registerBlock("mossy_white_limestone_bricks",
            () -> new Block(whiteLimestone(Blocks.MOSSY_STONE_BRICKS))
    );
    public static final RegistryObject<StairBlock> MOSSY_WHITE_LIMESTONE_BRICK_STAIRS = registerBlock("mossy_white_limestone_brick_stairs",
            () -> new StairBlock(MOSSY_WHITE_LIMESTONE_BRICKS.get().defaultBlockState(), whiteLimestone(Blocks.MOSSY_STONE_BRICK_STAIRS))
    );
    public static final RegistryObject<SlabBlock> MOSSY_WHITE_LIMESTONE_BRICK_SLAB = registerBlock("mossy_white_limestone_brick_slab",
            () -> new SlabBlock(whiteLimestone(Blocks.MOSSY_STONE_BRICK_SLAB))
    );
    public static final RegistryObject<WallBlock> MOSSY_WHITE_LIMESTONE_BRICK_WALL = registerBlock("mossy_white_limestone_brick_wall",
            () -> new WallBlock(whiteLimestone(Blocks.MOSSY_STONE_BRICK_WALL))
    );
    public static final RegistryObject<SlabBlock> CUT_WHITE_LIMESTONE_SLAB = registerBlock("cut_white_limestone_slab",
            () -> new SlabBlock(whiteLimestone(Blocks.STONE_SLAB))
    );

    // the vanilla stone block's behaviour, in white limestone's pale colour
    private static BlockBehaviour.Properties whiteLimestone(Block vanilla) {
        return BlockBehaviour.Properties.ofFullCopy(vanilla).mapColor(MapColor.QUARTZ);
    }

    // the vanilla stone block's behaviour, in limestone's sandy colour
    private static BlockBehaviour.Properties limestone(Block vanilla) {
        return BlockBehaviour.Properties.ofFullCopy(vanilla).mapColor(MapColor.SAND);
    }

    // as vanilla's signs: no collision, burns, breaks in one hit with an axe
    private static BlockBehaviour.Properties signProperties(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).forceSolidOn().instrument(NoteBlockInstrument.BASS)
                .noCollission().strength(1.0F).ignitedByLava();
    }

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
