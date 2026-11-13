package net.krisplis.koppenclimate.worldgen.tree;

import net.krisplis.koppenclimate.block.custom.ReachLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * For tree features that place their leaves one by one: once a tree's wood and leaves are down, every leaf is given
 * its true distance from the wood (counted through the leaves, as LeavesBlock counts it), so it decays exactly when
 * vanilla's would once the wood is gone; and a leaf too far from the wood for its kind - one cut off from it by a gap
 * - is taken away there and then, rather than left to hang until something next to it changes.
 */
public final class LeafDistances {
    private LeafDistances() {
    }

    public static void settle(WorldGenLevel level, Collection<BlockPos> leaves) {
        if (leaves.isEmpty()) return;
        Map<BlockPos, Integer> distance = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos p : leaves) {
            for (Direction d : Direction.values()) {
                if (level.getBlockState(p.relative(d)).is(BlockTags.LOGS)) {
                    distance.put(p, 1);
                    queue.add(p);
                    break;
                }
            }
        }
        java.util.Set<BlockPos> set = new java.util.HashSet<>(leaves);
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            int dist = distance.get(p);
            BlockState here = level.getBlockState(p);
            if (dist >= ReachLeavesBlock.reachOf(here)) continue;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (set.contains(n) && !distance.containsKey(n)) {
                    distance.put(n, dist + 1);
                    queue.add(n);
                }
            }
        }
        for (BlockPos p : leaves) {
            BlockState state = level.getBlockState(p);
            if (!(state.getBlock() instanceof LeavesBlock)) continue;
            Integer dist = distance.get(p);
            if (dist == null) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(p, ReachLeavesBlock.withDistance(state, dist), Block.UPDATE_CLIENTS);
            }
        }
    }
}
