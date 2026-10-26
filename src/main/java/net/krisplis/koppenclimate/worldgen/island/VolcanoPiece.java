package net.krisplis.koppenclimate.worldgen.island;

import net.krisplis.koppenclimate.worldgen.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Dresses a volcano's crater once the island's terrain is in place.
 * <ul>
 *   <li>Active: a lava lake in the bottom of the crater, its floor and walls magma, basalt and blackstone,
 *       and scorched basalt and tuff round the rim.</li>
 *   <li>Extinct: the crater has long grassed over; only patches of tuff, basalt and coarse dirt, a few old
 *       boulders and sometimes a small rainwater pool are left of the eruption.</li>
 * </ul>
 */
public class VolcanoPiece extends StructurePiece {
    // the scorched or rocky ring reaches this far past the crater's edge
    private static final int RIM = 6;

    private final int cx, cz;
    private final int craterRadius;
    private final int floorY;
    private final boolean active;
    private final long seed;

    public VolcanoPiece(IslandMap.Island island) {
        super(ModWorldgen.VOLCANO_PIECE.get(), 0, box(island));
        this.cx = island.x();
        this.cz = island.z();
        this.craterRadius = (int) Math.ceil(island.craterRadius());
        this.floorY = IslandMap.SEA_LEVEL + (int) Math.floor(island.craterFloor());
        this.active = island.active();
        this.seed = island.seed();
    }

    public VolcanoPiece(CompoundTag tag) {
        super(ModWorldgen.VOLCANO_PIECE.get(), tag);
        this.cx = tag.getInt("CX");
        this.cz = tag.getInt("CZ");
        this.craterRadius = tag.getInt("Crater");
        this.floorY = tag.getInt("Floor");
        this.active = tag.getBoolean("Active");
        this.seed = tag.getLong("Seed");
    }

    private static BoundingBox box(IslandMap.Island island) {
        int r = (int) Math.ceil(island.craterRadius()) + RIM + 1;
        int floor = IslandMap.SEA_LEVEL + (int) Math.floor(island.craterFloor());
        int rim = IslandMap.SEA_LEVEL + (int) Math.ceil(island.rimHeight());
        return new BoundingBox(island.x() - r, floor - 8, island.z() - r, island.x() + r, rim + 16, island.z() + r);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.cx);
        tag.putInt("CZ", this.cz);
        tag.putInt("Crater", this.craterRadius);
        tag.putInt("Floor", this.floorY);
        tag.putBoolean("Active", this.active);
        tag.putLong("Seed", this.seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int reach = this.craterRadius + RIM;
        // extinct craters sometimes hold a little rainwater pool
        boolean pool = !this.active && Math.floorMod(this.seed, 3) == 0;
        int lavaY = this.floorY + 3;
        int poolY = this.floorY + 1;

        for (int x = Math.max(box.minX(), this.cx - reach); x <= Math.min(box.maxX(), this.cx + reach); x++) {
            for (int z = Math.max(box.minZ(), this.cz - reach); z <= Math.min(box.maxZ(), this.cz + reach); z++) {
                double d = Math.sqrt((double) (x - this.cx) * (x - this.cx) + (double) (z - this.cz) * (z - this.cz));
                if (d > reach) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                double roll = noise(x, z);
                boolean inside = d < this.craterRadius;
                // nothing grows on an active crater; plants are cleared there
                if (this.active) {
                    clearPlants(level, pos, x, top, z);
                }

                if (this.active) {
                    if (inside) {
                        // floor and walls: magma, basalt and blackstone, two blocks deep
                        for (int y = top - 2; y <= top; y++) {
                            set(level, box, pos.set(x, y, z), roll < 0.4 ? Blocks.MAGMA_BLOCK.defaultBlockState()
                                    : roll < 0.7 ? Blocks.BASALT.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
                        }
                        // the lava lake fills the bottom of the bowl
                        for (int y = top + 1; y <= lavaY; y++) {
                            set(level, box, pos.set(x, y, z), Blocks.LAVA.defaultBlockState());
                        }
                    } else {
                        // the scorched rim, thinning out away from the crater
                        double chance = 1.0 - (d - this.craterRadius) / RIM;
                        if (roll < chance * 0.85) {
                            set(level, box, pos.set(x, top, z), roll < chance * 0.3 ? Blocks.TUFF.defaultBlockState()
                                    : roll < chance * 0.45 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BASALT.defaultBlockState());
                        }
                    }
                } else {
                    // long extinct: mostly grass, with patches of the old rock showing through
                    double chance = inside ? 0.28 : 0.28 * (1.0 - (d - this.craterRadius) / RIM);
                    if (roll < chance) {
                        set(level, box, pos.set(x, top, z), roll < chance * 0.45 ? Blocks.TUFF.defaultBlockState()
                                : roll < chance * 0.75 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.BASALT.defaultBlockState());
                    }
                    if (pool && inside && top < poolY) {
                        for (int y = top + 1; y <= poolY; y++) {
                            set(level, box, pos.set(x, y, z), Blocks.WATER.defaultBlockState());
                        }
                    }
                }
            }
        }
        if (!this.active) {
            this.placeBoulders(level, box);
        }
    }

    /** A few weathered lumps of tuff and basalt on the crater floor and rim. */
    private void placeBoulders(WorldGenLevel level, BoundingBox box) {
        RandomSource random = RandomSource.create(this.seed);
        int count = 4 + random.nextInt(5);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double dist = random.nextDouble() * (this.craterRadius + RIM - 2);
            int bx = this.cx + (int) (Math.cos(angle) * dist), bz = this.cz + (int) (Math.sin(angle) * dist);
            if (!box.isInside(bx, box.minY(), bz)) {
                continue;
            }
            int by = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, bx, bz);
            int r = 1 + random.nextInt(2);
            BlockState rock = random.nextBoolean() ? Blocks.TUFF.defaultBlockState() : Blocks.BASALT.defaultBlockState();
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -1; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dy * dy + dz * dz <= r * r + 1) {
                            set(level, box, pos.set(bx + dx, by + dy, bz + dz), rock);
                        }
                    }
                }
            }
        }
    }

    private static void clearPlants(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int top, int z) {
        for (int y = top + 1; y <= top + 2; y++) {
            BlockState state = level.getBlockState(pos.set(x, y, z));
            if (!state.isAir() && (state.canBeReplaced() && state.getFluidState().isEmpty() || state.is(BlockTags.FLOWERS))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }

    private static void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    // a steady 0..1 value per column, so neighbouring chunks agree
    private double noise(int x, int z) {
        long h = this.seed ^ (x * 3129871L) ^ (z * 116129781L);
        h = h * h * 42317861L + h * 11L;
        return ((h >>> 16) & 0xFFFF) / 65536.0;
    }
}
