package net.krisplis.koppenclimate.village;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Vanilla's desert villages, rebuilt in the colours of the mod's deserts: yellow sand becomes orange sand, and the
 * yellow sandstone family (plain, smooth, cut, chiseled; stairs, slabs and walls) becomes red sandstone, every
 * block keeping its facing, half, shape and so on. Added to every piece of the desert village's template pools.
 */
public class DesertRecolorProcessor extends StructureProcessor {
    public static final DesertRecolorProcessor INSTANCE = new DesertRecolorProcessor();
    public static final MapCodec<DesertRecolorProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    private static Map<Block, Block> swaps;

    private static Map<Block, Block> swaps() {
        if (swaps == null) {
            swaps = Map.ofEntries(
                    Map.entry(Blocks.SAND, ModBlocks.ORANGE_SAND.get()),
                    Map.entry(Blocks.SANDSTONE, Blocks.RED_SANDSTONE),
                    Map.entry(Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE),
                    Map.entry(Blocks.CUT_SANDSTONE, Blocks.CUT_RED_SANDSTONE),
                    Map.entry(Blocks.CHISELED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE),
                    Map.entry(Blocks.SANDSTONE_STAIRS, Blocks.RED_SANDSTONE_STAIRS),
                    Map.entry(Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE_STAIRS),
                    Map.entry(Blocks.SANDSTONE_SLAB, Blocks.RED_SANDSTONE_SLAB),
                    Map.entry(Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE_SLAB),
                    Map.entry(Blocks.CUT_SANDSTONE_SLAB, Blocks.CUT_RED_SANDSTONE_SLAB),
                    Map.entry(Blocks.SANDSTONE_WALL, Blocks.RED_SANDSTONE_WALL));
        }
        return swaps;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                            StructureTemplate.StructureBlockInfo original,
                                                            StructureTemplate.StructureBlockInfo current,
                                                            StructurePlaceSettings settings) {
        if (current == null) return null;
        Block to = swaps().get(current.state().getBlock());
        if (to == null) return current;
        return new StructureTemplate.StructureBlockInfo(current.pos(), copyProperties(current.state(), to.defaultBlockState()), current.nbt());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperties(BlockState from, BlockState to) {
        for (Property property : from.getProperties()) {
            if (to.hasProperty(property)) to = to.setValue(property, from.getValue(property));
        }
        return to;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return VillageRegistry.DESERT_RECOLOR.get();
    }
}
