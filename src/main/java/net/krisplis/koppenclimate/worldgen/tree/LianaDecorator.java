package net.krisplis.koppenclimate.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

// Long lianas: vines hanging from the sides of leaves all the way down, far longer than vanilla's
// leave_vine (which stops after 4 blocks). Each leaf side that faces air gets one with "probability";
// it hangs "length" blocks, or until it reaches something.
public class LianaDecorator extends TreeDecorator {
    public static final MapCodec<LianaDecorator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0.0F, 1.0F).fieldOf("probability").forGetter(d -> d.probability),
            IntProvider.codec(1, 32).fieldOf("length").forGetter(d -> d.length)
    ).apply(instance, LianaDecorator::new));

    private final float probability;
    private final IntProvider length;

    public LianaDecorator(float probability, IntProvider length) {
        this.probability = probability;
        this.length = length;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModTreePlacers.LIANA_DECORATOR.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        context.leaves().forEach(leaf -> {
            // the vine sits beside the leaf, attached to the side that faces it
            hang(context, random, leaf.west(), VineBlock.EAST);
            hang(context, random, leaf.east(), VineBlock.WEST);
            hang(context, random, leaf.north(), VineBlock.SOUTH);
            hang(context, random, leaf.south(), VineBlock.NORTH);
        });
    }

    private void hang(Context context, RandomSource random, BlockPos pos, BooleanProperty attachedSide) {
        if (random.nextFloat() >= this.probability || !context.isAir(pos)) return;
        context.placeVine(pos, attachedSide);
        int remaining = this.length.sample(random) - 1;
        BlockPos below = pos.below();
        while (remaining-- > 0 && context.isAir(below)) {
            context.placeVine(below, attachedSide);
            below = below.below();
        }
    }
}
