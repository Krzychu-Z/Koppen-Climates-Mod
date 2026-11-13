package net.krisplis.koppenclimate.worldgen.feature;

import com.mojang.serialization.Codec;
import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.CornCropBlock;
import net.krisplis.koppenclimate.block.custom.TilledSoilBlock;
import net.krisplis.koppenclimate.worldgen.biome.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * The American farmland (Cfa): the whole biome is one patchwork of fields. The land is cut into irregular parcels
 * (a jittered Voronoi pattern over gently warped coordinates, so no two are alike and none is a square), each
 * between 6400 and 25600 blocks of field (about 7500 to 21500, as CELL, JITTER and WARP are set; only a biome's edge,
 * a river or a farm cuts one smaller), and between them run hedgerows three blocks wide: grass with a broad-leaved tree here and there
 * (oak, plane, black poplar, and now and then an apple tree). Each parcel is one thing from edge to edge:
 * <ul>
 *   <li>most often (three in five) a meadow - short grass, rarely a tuft of tall grass, the odd flower - with a few cows;</li>
 *   <li>now and then a wood of those same broad-leaved trees;</li>
 *   <li>or a field: cotton, corn, wheat or barley, ripe, on tilled farmland soil with every block planted; or
 *       sunflowers, every block.</li>
 * </ul>
 * It works only on the farmland soil grass the biome's surface is made of (so it leaves any structure's own ground,
 * such as the red farms' yards, alone), and it is worked out from world coordinates alone, so the parcels run on
 * seamlessly from chunk to chunk.
 */
public class AmericanFarmlandFeature extends Feature<NoneFeatureConfiguration> {
    // the parcels: one per CELL x CELL square (jittered), and the hedgerows' half width (three blocks wide in all)
    private static final int CELL = 124;
    private static final double JITTER = 0.28, HEDGE_HALF = 1.5, WARP = 6.0, WARP_SCALE = 1.0 / 30.0;
    // on the hedgerow's middle line, one block in this many holds a tree
    private static final int HEDGE_TREE_ONE_IN = 9;
    // in a wood, the trees stand one to a WOOD_CELL square, now and then left out
    private static final int WOOD_CELL = 4;
    // only one meadow chunk in so many gets a little herd of cows
    private static final int COW_CHUNK_ONE_IN = 6;
    private static final double WOOD_FILL = 0.75;

    enum Parcel { MEADOW, WOOD, COTTON, CORN, WHEAT, BARLEY, SUNFLOWERS }

    private static final List<ResourceKey<ConfiguredFeature<?, ?>>> TREES = List.of(
            key("minecraft", "oak"), key("minecraft", "fancy_oak"),
            key(KoppenClimateMod.MOD_ID, "platanus"), key(KoppenClimateMod.MOD_ID, "black_poplar"));
    // the woods' trees: a close broad-leaved wood of oaks and planes - no poplars in them, those only stand in the
    // hedgerows
    private static final List<ResourceKey<ConfiguredFeature<?, ?>>> WOOD_TREES = List.of(
            key("minecraft", "oak"), key("minecraft", "fancy_oak"), key(KoppenClimateMod.MOD_ID, "platanus"));
    // the hedgerows also hold an apple tree now and then: one hedge tree in this many
    private static final int HEDGE_APPLE_ONE_IN = 5;
    private static final ResourceKey<ConfiguredFeature<?, ?>> APPLE = key(KoppenClimateMod.MOD_ID, "apple");
    private static final Block[] FLOWERS = {Blocks.POPPY, Blocks.DANDELION, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY, Blocks.AZURE_BLUET};

    public AmericanFarmlandFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> key(String ns, String path) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(ns, path));
    }

    // ================================================================ the pattern of parcels

    private static long hash(long seed, int a, int b, int salt) {
        long h = seed ^ (a * 0x9E3779B97F4A7C15L) ^ (b * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Which parcel (cell x, cell z) a spot belongs to, and how far it is from that parcel's edge. */
    record Spot(int cellX, int cellZ, double edge) {
    }

    static Spot spot(long seed, int x, int z) {
        // gently warped, so the parcels' edges wander rather than run dead straight
        double wx = x + WARP * Math.sin(z * WARP_SCALE + unit(hash(seed, 1, 2, 3)) * 6.28)
                + WARP * 0.5 * Math.sin((x + z) * WARP_SCALE * 1.7);
        double wz = z + WARP * Math.sin(x * WARP_SCALE + unit(hash(seed, 4, 5, 6)) * 6.28)
                + WARP * 0.5 * Math.sin((x - z) * WARP_SCALE * 1.3);
        int gx = Mth.floor(wx / CELL), gz = Mth.floor(wz / CELL);
        double best = Double.MAX_VALUE;
        double bx = 0, bz = 0;
        int bcx = 0, bcz = 0;
        double[][] points = new double[9][];
        int n = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int cx = gx + dx, cz = gz + dz;
                double px = (cx + 0.5 + (unit(hash(seed, cx, cz, 11)) - 0.5) * 2 * JITTER) * CELL;
                double pz = (cz + 0.5 + (unit(hash(seed, cx, cz, 12)) - 0.5) * 2 * JITTER) * CELL;
                points[n++] = new double[]{px, pz, cx, cz};
                double d = (wx - px) * (wx - px) + (wz - pz) * (wz - pz);
                if (d < best) {
                    best = d;
                    bx = px;
                    bz = pz;
                    bcx = cx;
                    bcz = cz;
                }
            }
        }
        // distance to the nearest edge: to the closest perpendicular bisector with a neighbouring parcel
        double edge = Double.MAX_VALUE;
        for (double[] p : points) {
            if ((int) p[2] == bcx && (int) p[3] == bcz) continue;
            double ex = p[0] - bx, ez = p[1] - bz;
            double len = Math.sqrt(ex * ex + ez * ez);
            double mx = (p[0] + bx) / 2, mz = (p[1] + bz) / 2;
            double d = ((mx - wx) * ex + (mz - wz) * ez) / len;
            edge = Math.min(edge, d);
        }
        return new Spot(bcx, bcz, edge);
    }

    static Parcel parcel(long seed, int cellX, int cellZ) {
        int r = (int) Math.floorMod(hash(seed, cellX, cellZ, 21), 100L);
        if (r < 60) return Parcel.MEADOW;
        if (r < 68) return Parcel.WOOD;
        if (r < 75) return Parcel.COTTON;
        if (r < 82) return Parcel.CORN;
        if (r < 88) return Parcel.WHEAT;
        if (r < 94) return Parcel.BARLEY;
        return Parcel.SUNFLOWERS;
    }

    // ================================================================ placing it

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        long seed = level.getSeed();
        int startX = context.origin().getX() & ~15, startZ = context.origin().getZ() & ~15;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        List<BlockPos> trees = new ArrayList<>();
        List<BlockPos> hedgeTrees = new ArrayList<>();
        List<BlockPos> pasture = new ArrayList<>();
        boolean changed = false;

        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                pos.set(x, top, z);
                // only the biome's own farmland soil grass, out in the open
                if (!level.getBlockState(pos).is(ModBlocks.FARMLAND_SOIL_GRASS_BLOCK.get())) continue;
                if (!level.getBiome(pos).is(ModBiomes.AMERICAN_FARMLAND)) continue;
                BlockPos ground = pos.immutable();
                BlockPos above = ground.above();
                Spot spot = spot(seed, x, z);
                long h = hash(seed, x, z, 31);

                if (spot.edge() < HEDGE_HALF) {
                    // the hedgerow: grass, and a tree now and then along its middle
                    if (spot.edge() < 0.75 && Math.floorMod(h, HEDGE_TREE_ONE_IN) == 0) hedgeTrees.add(above);
                    else if (unit(h) < 0.55) changed |= this.grass(level, above, unit(hash(seed, x, z, 32)) < 0.3);
                    continue;
                }
                switch (parcel(seed, spot.cellX(), spot.cellZ())) {
                    case MEADOW -> {
                        double r = unit(h);
                        if (r < 0.015) changed |= this.set(level, above, FLOWERS[(int) Math.floorMod(h >>> 8, FLOWERS.length)].defaultBlockState());
                        else if (r < 0.04) changed |= this.grass(level, above, true);   // tall grass only now and then
                        else if (r < 0.75) changed |= this.grass(level, above, false);
                        pasture.add(above);
                    }
                    case WOOD -> {
                        // one tree to each little square, at a spot of its own, now and then left out
                        int sx = Math.floorDiv(x, WOOD_CELL), sz = Math.floorDiv(z, WOOD_CELL);
                        long sh = hash(seed, sx, sz, 41);
                        int tx = sx * WOOD_CELL + (int) Math.floorMod(sh, WOOD_CELL);
                        int tz = sz * WOOD_CELL + (int) Math.floorMod(sh >>> 16, WOOD_CELL);
                        if (x == tx && z == tz && unit(hash(seed, sx, sz, 42)) < WOOD_FILL) trees.add(above);
                        else if (unit(h) < 0.35) changed |= this.grass(level, above, false);
                    }
                    case COTTON -> changed |= this.crop(level, ground, ModBlocks.COTTON.get());
                    case CORN -> changed |= this.crop(level, ground, ModBlocks.CORN.get());
                    case WHEAT -> changed |= this.crop(level, ground, Blocks.WHEAT);
                    case BARLEY -> changed |= this.crop(level, ground, ModBlocks.BARLEY.get());
                    case SUNFLOWERS -> {
                        if (level.isEmptyBlock(above) && level.isEmptyBlock(above.above())) {
                            DoublePlantBlock.placeAt(level, Blocks.SUNFLOWER.defaultBlockState(), above, 2);
                            changed = true;
                        }
                    }
                }
            }
        }

        // the trees, after the ground cover, so nothing is planted over their trunks
        var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        for (BlockPos at : trees) {
            if (!level.isEmptyBlock(at)) continue;
            var tree = registry.getHolder(WOOD_TREES.get(random.nextInt(WOOD_TREES.size())));
            if (tree.isPresent()) changed |= tree.get().value().place(level, context.chunkGenerator(), random, at);
        }
        // the hedgerows' trees: the same broad-leaved ones, with an apple tree among them now and then
        for (BlockPos at : hedgeTrees) {
            if (!level.isEmptyBlock(at)) continue;
            var key = random.nextInt(HEDGE_APPLE_ONE_IN) == 0 ? APPLE : TREES.get(random.nextInt(TREES.size()));
            var tree = registry.getHolder(key);
            if (tree.isPresent()) changed |= tree.get().value().place(level, context.chunkGenerator(), random, at);
        }
        // the meadows' cows: a little herd in a chunk that is mostly meadow
        if (pasture.size() > 128 && random.nextInt(COW_CHUNK_ONE_IN) == 0) {
            int cows = 1 + random.nextInt(3);
            for (int i = 0; i < cows; i++) {
                BlockPos at = pasture.get(random.nextInt(pasture.size()));
                if (!level.getBlockState(at).canBeReplaced() || !level.getBlockState(at.above()).canBeReplaced()) continue;
                Cow cow = EntityType.COW.create(level.getLevel());
                if (cow == null) break;
                cow.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                cow.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.CHUNK_GENERATION, null);
                cow.setPersistenceRequired();
                level.addFreshEntityWithPassengers(cow);
            }
        }
        return changed;
    }

    private boolean set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.isEmptyBlock(pos)) return false;
        level.setBlock(pos, state, 2);
        return true;
    }

    private boolean grass(WorldGenLevel level, BlockPos pos, boolean tall) {
        if (tall) {
            if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) return false;
            DoublePlantBlock.placeAt(level, Blocks.TALL_GRASS.defaultBlockState(), pos, 2);
            return true;
        }
        return this.set(level, pos, Blocks.SHORT_GRASS.defaultBlockState());
    }

    // the ground tilled (always moist: TilledSoilBlock) and the ripe crop on it
    private boolean crop(WorldGenLevel level, BlockPos ground, Block crop) {
        if (!level.isEmptyBlock(ground.above())) return false;
        // corn stands three blocks tall
        if (crop instanceof CornCropBlock corn) {
            if (!level.isEmptyBlock(ground.above(2)) || !level.isEmptyBlock(ground.above(3))) return false;
            level.setBlock(ground, ModBlocks.TILLED_FARMLAND_SOIL.get().defaultBlockState().setValue(TilledSoilBlock.MOISTURE, 7), 2);
            CornCropBlock.placeColumn(level, ground.above(), corn.getMaxAge(), 2);
            return true;
        }
        level.setBlock(ground, ModBlocks.TILLED_FARMLAND_SOIL.get().defaultBlockState().setValue(TilledSoilBlock.MOISTURE, 7), 2);
        BlockState state = crop instanceof CropBlock c ? c.getStateForAge(c.getMaxAge()) : crop.defaultBlockState();
        level.setBlock(ground.above(), state, 2);
        return true;
    }
}
