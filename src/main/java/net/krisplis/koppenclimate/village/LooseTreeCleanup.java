package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.custom.BananaBunchBlock;
import net.krisplis.koppenclimate.block.custom.CoconutBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tidies up the trees round the edge of a village's levelled ground. Levelling cuts the trunks of the trees standing
 * in its edge band at their foot and shears off the crowns that reach over the village; this takes away what's left
 * hanging in the air:
 *
 * <ul>
 *   <li>every log (and every branch joined to it) that no longer stands on solid ground;</li>
 *   <li>every tree leaf further from a log than its kind holds on - six leaves for vanilla's, as far as they decay;</li>
 *   <li>vines, cocoa pods, coconuts, glow lichen and the like that have lost what they hung from.</li>
 * </ul>
 *
 * <p>It looks at the chunk being built and the half of each neighbouring chunk next to it (a tree's crown often
 * spans a chunk border), only in the columns the caller's zone takes in. Columns the village clears to the sky
 * (the {@code cleared} test) are taken as empty without being read, since whatever stands in them now will be
 * gone once their own chunk is built. Anything that runs out of the area looked at is left alone: what lies beyond
 * can't be seen, so it's taken to hold it up. Only the leaves trees grow are touched, never ones placed to stay
 * (persistent), nor anything else a builder put there.
 */
public final class LooseTreeCleanup {
    private LooseTreeCleanup() {
    }

    /** A test on a world column (x, z). */
    public interface Columns {
        boolean test(int x, int z);
    }

    private static final byte EMPTY = 0, GROUND = 1, LOG = 2, LEAF = 3, PLANT = 4, OUTSIDE = 5;
    // leaves are counted out this far from the wood; each kind holds on as far as its reach (6 for vanilla's,
    // further for the broad canopies: ReachLeavesBlock)
    private static final int LEAF_REACH = net.krisplis.koppenclimate.block.custom.ReachLeavesBlock.MAX_REACH;
    // how far into each neighbouring chunk to look (15, so every block read stays inside the 3x3 chunks a chunk's
    // decoration may touch)
    private static final int REACH = 15;
    private static final int SIZE = 16 + 2 * REACH;

    /**
     * @param zone    the columns to look at (the ring of ground round the village where trees may have been cut)
     * @param cleared the columns the village clears all the way up (looked at as empty)
     */
    public static void clean(WorldGenLevel level, ChunkPos chunk, int yMin, int yMax, Columns zone, Columns cleared) {
        int x0 = chunk.getMinBlockX() - REACH, z0 = chunk.getMinBlockZ() - REACH;
        int h = yMax - yMin + 1;
        if (h <= 2) return;
        byte[] grid = new byte[SIZE * SIZE * h];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean trees = false;
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                int wx = x0 + dx, wz = z0 + dz;
                byte fill = cleared.test(wx, wz) ? EMPTY : zone.test(wx, wz) ? (byte) -1 : OUTSIDE;
                if (fill != -1) {
                    if (fill != EMPTY) {
                        for (int y = 0; y < h; y++) grid[index(dx, y, dz, h)] = fill;
                    }
                    continue;
                }
                for (int y = 0; y < h; y++) {
                    BlockState state = level.getBlockState(pos.set(wx, yMin + y, wz));
                    byte kind = classify(state);
                    if (kind == LOG || kind == LEAF) trees = true;
                    grid[index(dx, y, dz, h)] = kind;
                }
            }
        }
        if (!trees) return;
        removeLooseLogs(level, grid, x0, yMin, z0, h, pos);
        removeLooseLeaves(level, grid, x0, yMin, z0, h, pos);
        removeHangingPlants(level, grid, x0, yMin, z0, h, pos);
    }

    private static byte classify(BlockState state) {
        if (state.isAir()) return EMPTY;
        if (state.is(BlockTags.LOGS)) return LOG;
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) {
            // leaves placed to stay (by a builder, or the karst towers' bushes) are left be, and hold nothing up
            return state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT) ? GROUND : LEAF;
        }
        if (block instanceof VineBlock || block instanceof CocoaBlock || block instanceof BambooStalkBlock
                || block instanceof BambooSaplingBlock || block instanceof MangrovePropaguleBlock || block instanceof MultifaceBlock
                || block instanceof HangingRootsBlock || block instanceof CoconutBlock || block instanceof BananaBunchBlock) {
            return PLANT;
        }
        if (!state.getFluidState().isEmpty()) return EMPTY;
        return state.isSolid() ? GROUND : EMPTY;
    }

    private static int index(int dx, int y, int dz, int h) {
        return (dx * SIZE + dz) * h + y;
    }

    private static void clear(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    /** Logs: every group of touching logs that neither stands on ground nor runs out of sight goes. */
    private static void removeLooseLogs(WorldGenLevel level, byte[] grid, int x0, int yMin, int z0, int h, BlockPos.MutableBlockPos pos) {
        boolean[] seen = new boolean[grid.length];
        int[] queue = new int[grid.length];
        int[] group = new int[grid.length];
        for (int start = 0; start < grid.length; start++) {
            if (grid[start] != LOG || seen[start]) continue;
            int head = 0, tail = 0, size = 0;
            boolean held = false;
            queue[tail++] = start;
            seen[start] = true;
            while (head < tail) {
                int i = queue[head++];
                group[size++] = i;
                int y = i % h, dz = (i / h) % SIZE, dx = i / h / SIZE;
                // standing on the ground, or on what lies beyond the area looked at
                if (y == 0) {
                    held = true;
                } else {
                    byte below = grid[i - 1];
                    if (below == GROUND || below == OUTSIDE) held = true;
                }
                for (int ox = -1; ox <= 1; ox++) {
                    for (int oy = -1; oy <= 1; oy++) {
                        for (int oz = -1; oz <= 1; oz++) {
                            if (ox == 0 && oy == 0 && oz == 0) continue;
                            int nx = dx + ox, ny = y + oy, nz = dz + oz;
                            if (nx < 0 || nz < 0 || nx >= SIZE || nz >= SIZE || ny < 0 || ny >= h) {
                                held = true;
                                continue;
                            }
                            int n = index(nx, ny, nz, h);
                            if (grid[n] == OUTSIDE) {
                                held = true;
                            } else if (grid[n] == LOG && !seen[n]) {
                                seen[n] = true;
                                queue[tail++] = n;
                            }
                        }
                    }
                }
            }
            if (held) continue;
            for (int k = 0; k < size; k++) {
                int i = group[k];
                grid[i] = EMPTY;
                clear(level, pos, x0 + i / h / SIZE, yMin + i % h, z0 + (i / h) % SIZE);
            }
        }
    }

    /** Leaves: those more than six leaves from any log left (or from the edge of what's looked at) go. */
    private static void removeLooseLeaves(WorldGenLevel level, byte[] grid, int x0, int yMin, int z0, int h, BlockPos.MutableBlockPos pos) {
        byte[] distance = new byte[grid.length];
        java.util.Arrays.fill(distance, Byte.MAX_VALUE);
        int[] queue = new int[grid.length];
        int head = 0, tail = 0;
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] == LOG) {
                distance[i] = 0;
                queue[tail++] = i;
            } else if (grid[i] == LEAF && atEdge(grid, i, h)) {
                // it may hang from a tree out of sight
                distance[i] = 0;
                queue[tail++] = i;
            }
        }
        int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (head < tail) {
            int i = queue[head++];
            if (distance[i] >= LEAF_REACH) continue;
            int y = i % h, dz = (i / h) % SIZE, dx = i / h / SIZE;
            for (int[] s : steps) {
                int nx = dx + s[0], ny = y + s[1], nz = dz + s[2];
                if (nx < 0 || nz < 0 || nx >= SIZE || nz >= SIZE || ny < 0 || ny >= h) continue;
                int n = index(nx, ny, nz, h);
                if (grid[n] == LEAF && distance[n] > distance[i] + 1) {
                    distance[n] = (byte) (distance[i] + 1);
                    queue[tail++] = n;
                }
            }
        }
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != LEAF || distance[i] <= net.minecraft.world.level.block.LeavesBlock.DECAY_DISTANCE - 1) continue;
            int x = x0 + i / h / SIZE, y = yMin + i % h, z = z0 + (i / h) % SIZE;
            if (distance[i] <= net.krisplis.koppenclimate.block.custom.ReachLeavesBlock.reachOf(level.getBlockState(pos.set(x, y, z)))) continue;
            grid[i] = EMPTY;
            clear(level, pos, x, y, z);
        }
    }

    private static boolean atEdge(byte[] grid, int i, int h) {
        int y = i % h, dz = (i / h) % SIZE, dx = i / h / SIZE;
        if (dx == 0 || dz == 0 || dx == SIZE - 1 || dz == SIZE - 1 || y == 0 || y == h - 1) return true;
        return grid[index(dx + 1, y, dz, h)] == OUTSIDE || grid[index(dx - 1, y, dz, h)] == OUTSIDE
                || grid[index(dx, y, dz + 1, h)] == OUTSIDE || grid[index(dx, y, dz - 1, h)] == OUTSIDE;
    }

    /** Vines, cocoa, coconuts and the like that can no longer hang where they are; from the top down, so a vine
     *  hanging from a vine that has gone goes too. */
    private static void removeHangingPlants(WorldGenLevel level, byte[] grid, int x0, int yMin, int z0, int h, BlockPos.MutableBlockPos pos) {
        for (int dx = 0; dx < SIZE; dx++) {
            for (int dz = 0; dz < SIZE; dz++) {
                for (int y = h - 1; y >= 0; y--) {
                    int i = index(dx, y, dz, h);
                    if (grid[i] != PLANT) continue;
                    pos.set(x0 + dx, yMin + y, z0 + dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.canSurvive(level, pos)) {
                        grid[i] = EMPTY;
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }
}
