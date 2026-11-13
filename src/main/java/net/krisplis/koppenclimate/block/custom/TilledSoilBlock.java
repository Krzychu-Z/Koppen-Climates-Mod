package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

import javax.annotation.Nullable;

/**
 * Farmland soil tilled with a hoe (SubtropicalFarming): farmland like vanilla's, but the rich, black soil holds
 * its moisture by itself - it never needs water nearby and never dries out - and crops on it grow twice as fast
 * (SubtropicalFarming). Trampled, it goes back to farmland soil, not dirt. Crops and stems may stand on it (CropFarmlandMixin).
 */
public class TilledSoilBlock extends FarmBlock {
    public static final MapCodec<TilledSoilBlock> CODEC = simpleCodec(TilledSoilBlock::new);

    public TilledSoilBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MOISTURE, 7));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public MapCodec<FarmBlock> codec() {
        return (MapCodec) CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return !this.defaultBlockState().canSurvive(context.getLevel(), context.getClickedPos())
                ? ModBlocks.FARMLAND_SOIL.get().defaultBlockState() : this.defaultBlockState();
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) toSoil(null, state, level, pos);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // always moist, water or no water
        if (state.getValue(MOISTURE) < 7) level.setBlock(pos, state.setValue(MOISTURE, 7), 2);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (!level.isClientSide && level.random.nextFloat() < fallDistance - 0.5F && entity instanceof LivingEntity
                && (entity instanceof Player || level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING))
                && entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight() > 0.512F) {
            toSoil(entity, state, level, pos);
        }
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    /** Back to plain farmland soil, lifting anything standing on it out of the way. */
    public static void toSoil(@Nullable Entity entity, BlockState state, Level level, BlockPos pos) {
        BlockState soil = Block.pushEntitiesUp(state, ModBlocks.FARMLAND_SOIL.get().defaultBlockState(), level, pos);
        level.setBlockAndUpdate(pos, soil);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(entity, soil));
    }

}
