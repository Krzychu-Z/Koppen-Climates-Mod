package net.krisplis.koppenclimate.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import java.util.ArrayList;
import java.util.List;

// Hangs fruit that grows like cocoa (banana bunches, coconuts) on the sides of the trunk just under
// the crown, the way vanilla's CocoaDecorator puts cocoa on jungle logs.
//   block: a CocoaBlock-style fruit (FACING + AGE)
//   probability: chance a tree gets any fruit at all
//   min_depth / levels: which logs, counted down from the top log (0 = the top log itself)
//   count: how many fruit, each on its own free side at a random growth stage
public class HangingFruitDecorator extends TreeDecorator {
    public static final MapCodec<HangingFruitDecorator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(d -> d.block),
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(d -> d.probability),
            Codec.intRange(0, 8).fieldOf("min_depth").forGetter(d -> d.minDepth),
            Codec.intRange(1, 8).fieldOf("levels").forGetter(d -> d.levels),
            IntProvider.codec(1, 8).fieldOf("count").forGetter(d -> d.count)
    ).apply(instance, HangingFruitDecorator::new));

    private final Block block;
    private final float probability;
    private final int minDepth;
    private final int levels;
    private final IntProvider count;

    public HangingFruitDecorator(Block block, float probability, int minDepth, int levels, IntProvider count) {
        this.block = block;
        this.probability = probability;
        this.minDepth = minDepth;
        this.levels = levels;
        this.count = count;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModTreePlacers.HANGING_FRUIT_DECORATOR.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        if (context.logs().isEmpty() || !(this.block instanceof CocoaBlock) || random.nextFloat() >= this.probability) return;

        int topY = context.logs().stream().mapToInt(BlockPos::getY).max().getAsInt();
        int highest = topY - this.minDepth, lowest = highest - this.levels + 1;
        List<BlockPos> logs = context.logs().stream().filter(p -> p.getY() <= highest && p.getY() >= lowest).toList();

        // every free side of those logs, as (spot, direction from the log)
        List<BlockPos> spots = new ArrayList<>();
        List<Direction> sides = new ArrayList<>();
        for (BlockPos log : logs) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos spot = log.relative(dir);
                if (context.isAir(spot)) {
                    spots.add(spot);
                    sides.add(dir);
                }
            }
        }

        int fruit = this.count.sample(random);
        for (int i = 0; i < fruit && !spots.isEmpty(); i++) {
            int pick = random.nextInt(spots.size());
            // FACING points from the fruit back to the log it hangs on
            context.setBlock(spots.remove(pick), this.block.defaultBlockState()
                    .setValue(CocoaBlock.FACING, sides.remove(pick).getOpposite())
                    .setValue(CocoaBlock.AGE, random.nextInt(CocoaBlock.MAX_AGE + 1)));
        }
    }
}
