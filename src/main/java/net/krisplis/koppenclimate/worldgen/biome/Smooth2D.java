package net.krisplis.koppenclimate.worldgen.biome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Smooths a 2D terrain function (a ground height per column, such as CfaPlain's or CwaTerrain's) between the corners
 * of the 4 x 4 block cells vanilla's climate noises are worked out on.
 *
 * <p>Vanilla's continentalness, erosion and ridges are cached once per 4 x 4 cell (flat_cache), so any height worked
 * out from them steps up or down at every cell's edge: hillsides come out as stairs of 4 x 4 block slabs. Vanilla
 * hides this by interpolating its terrain; this does the same for ours, working the height out at the cell's four
 * corners and blending between them, so slopes climb block by block like vanilla's.
 */
public final class Smooth2D implements DensityFunction {
    public static final MapCodec<Smooth2D> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("argument").forGetter(Smooth2D::argument)
    ).apply(instance, Smooth2D::new));
    public static final KeyDispatchDataCodec<Smooth2D> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static final int CELL = 4;

    private final DensityFunction argument;
    // the cell asked about last: terrain is generated cell by cell, every block of a cell's column in turn, so it is
    // nearly always the same one (an immutable snapshot, so threads can share it safely)
    private volatile Corners last;
    // the corners worked out lately: each is shared by four cells, and the Alps' terrain looks at points round
    // about (the slope of the weirdness), so the same corners come up again and again. A small fixed table, one
    // corner per slot, each entry immutable (threads may share it without locks).
    private final Corner[] corners = new Corner[CORNERS];
    private static final int CORNERS = 256;

    private record Corners(int x0, int z0, double v00, double v10, double v01, double v11) {
    }

    private record Corner(int x, int z, double value) {
    }

    private double corner(int x, int z, int y) {
        int slot = (int) (((x * 0x9E3779B1L) ^ (z * 0x85EBCA77L)) >>> 7) & (CORNERS - 1);
        Corner c = this.corners[slot];
        if (c == null || c.x() != x || c.z() != z) {
            c = new Corner(x, z, this.argument.compute(new SinglePointContext(x, y, z)));
            this.corners[slot] = c;
        }
        return c.value();
    }

    public Smooth2D(DensityFunction argument) {
        this.argument = argument;
    }

    public DensityFunction argument() {
        return this.argument;
    }

    @Override
    public double compute(FunctionContext context) {
        int x = context.blockX(), z = context.blockZ();
        int x0 = Math.floorDiv(x, CELL) * CELL, z0 = Math.floorDiv(z, CELL) * CELL;
        Corners c = this.last;
        if (c == null || c.x0() != x0 || c.z0() != z0) {
            int y = context.blockY();
            c = new Corners(x0, z0, this.corner(x0, z0, y), this.corner(x0 + CELL, z0, y),
                    this.corner(x0, z0 + CELL, y), this.corner(x0 + CELL, z0 + CELL, y));
            this.last = c;
        }
        double fx = (x - x0) / (double) CELL, fz = (z - z0) / (double) CELL;
        return Mth.lerp2(fx, fz, c.v00(), c.v10(), c.v01(), c.v11());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Smooth2D o && this.argument.equals(o.argument);
    }

    @Override
    public int hashCode() {
        return this.argument.hashCode() * 31 + 7;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new Smooth2D(this.argument.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return this.argument.minValue();
    }

    @Override
    public double maxValue() {
        return this.argument.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
