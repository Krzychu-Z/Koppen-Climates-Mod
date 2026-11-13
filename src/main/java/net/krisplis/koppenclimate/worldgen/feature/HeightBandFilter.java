package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.krisplis.koppenclimate.worldgen.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** Lets a feature through only between two heights (inclusive), e.g. after a heightmap placement. */
public class HeightBandFilter extends PlacementFilter {
    public static final MapCodec<HeightBandFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("min_y").forGetter(f -> f.minY),
            Codec.INT.fieldOf("max_y").forGetter(f -> f.maxY)
    ).apply(i, HeightBandFilter::new));

    private final int minY, maxY;

    public HeightBandFilter(int minY, int maxY) {
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return pos.getY() >= this.minY && pos.getY() <= this.maxY;
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModWorldgen.HEIGHT_BAND.get();
    }
}
