package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * The Prairie's sward (Dfa), once per chunk, last of its plants: every grass block of the biome in the chunk gets a
 * one-block-tall plant on top - short grass nearly everywhere, a fern here and there (in loose clumps), and now and
 * then a dandelion. Any water still lying on the surface (a spring, a dent the plain didn't quite fill) is filled in
 * with earth and grassed over, so the Prairie never meets any.
 */
public class PrairieGrassFeature extends Feature<PrairieGrassFeature.Config> {
    // the deepest surface water that gets filled in
    private static final int FILL_DEPTH = 8;

    public record Config(ResourceKey<Biome> biome, float fernChance, float flowerChance) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(Config::biome),
                Codec.floatRange(0.0F, 1.0F).fieldOf("fern_chance").forGetter(Config::fernChance),
                Codec.floatRange(0.0F, 1.0F).fieldOf("flower_chance").forGetter(Config::flowerChance)
        ).apply(instance, Config::new));
    }

    public PrairieGrassFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        Config config = context.config();
        int startX = context.origin().getX() & ~15, startZ = context.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        long seed = level.getSeed();
        boolean changed = false;
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (surface <= level.getMinBuildHeight()) continue;
                pos.set(x, floor, z);
                if (!level.getBiome(pos).is(config.biome())) continue;
                if (surface > floor && !level.getFluidState(pos.set(x, floor + 1, z)).isEmpty()) {
                    // water on top: filled in up to its surface, if it's shallow
                    int top = floor + 1;
                    while (top < surface && !level.getFluidState(pos.set(x, top + 1, z)).isEmpty()) top++;
                    if (top - floor > FILL_DEPTH) continue;
                    for (int y = floor + 1; y <= top; y++) {
                        level.setBlock(pos.set(x, y, z), (y == top ? Blocks.GRASS_BLOCK : Blocks.DIRT).defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                    floor = top;
                    changed = true;
                }
                pos.set(x, floor, z);
                if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) continue;
                pos.set(x, floor + 1, z);
                if (!level.getBlockState(pos).isAir()) continue;
                level.setBlock(pos, this.plant(config, seed, x, z), Block.UPDATE_CLIENTS);
                changed = true;
            }
        }
        return changed;
    }

    private BlockState plant(Config config, long seed, int x, int z) {
        // ferns in loose clumps: more likely where a coarse 8-block cell says so
        long cell = hash(seed, x >> 3, z >> 3);
        long here = hash(seed ^ 0x5DEECE66DL, x, z);
        float roll = (here & 0xFFFFFF) / (float) 0x1000000;
        boolean fernCell = (cell & 7) == 0;
        if (roll < config.flowerChance()) return Blocks.DANDELION.defaultBlockState();
        if (roll < config.flowerChance() + config.fernChance() * (fernCell ? 6.0F : 0.4F)) return Blocks.FERN.defaultBlockState();
        return Blocks.SHORT_GRASS.defaultBlockState();
    }

    private static long hash(long seed, int x, int z) {
        long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return h;
    }
}
