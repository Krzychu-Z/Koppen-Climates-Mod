package net.krisplis.koppenclimate.block.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.TermiteEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The hard red earth of a termite mound. A few termites always live about it: now and then (a random tick) the
 * mound sends one out if there are fewer than {@link #COLONY} nearby. Breaking into it rouses the colony: a few
 * more termites burst out, and every termite near it goes for whoever did it.
 */
public class TermiteMoundBlock extends Block {
    private static final int COLONY = 4;
    private static final double RANGE = 10.0;

    public TermiteMoundBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos out = pos.above();
        if (!level.getBlockState(out).isAir()) return;
        if (level.getEntitiesOfClass(TermiteEntity.class, new AABB(pos).inflate(RANGE)).size() >= COLONY) return;
        spawn(level, pos, out);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !player.isCreative()) {
            for (TermiteEntity termite : server.getEntitiesOfClass(TermiteEntity.class, new AABB(pos).inflate(RANGE))) {
                termite.rouse(player);
            }
            int burst = 2 + server.getRandom().nextInt(3);
            for (int i = 0; i < burst; i++) {
                TermiteEntity termite = spawn(server, pos, pos);
                if (termite != null) termite.rouse(player);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    private static TermiteEntity spawn(ServerLevel level, BlockPos mound, BlockPos at) {
        TermiteEntity termite = ModEntities.TERMITE.get().create(level);
        if (termite == null) return null;
        termite.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        termite.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.SPAWNER, null);
        termite.setHome(mound);
        level.addFreshEntity(termite);
        return termite;
    }
}
