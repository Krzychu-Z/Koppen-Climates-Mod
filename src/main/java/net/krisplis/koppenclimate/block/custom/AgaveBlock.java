package net.krisplis.koppenclimate.block.custom;

import com.mojang.serialization.MapCodec;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The agave: a rosette of thick, spiny blue-grey leaves that grows through four stages (AGE 0-3). Fully grown, it
 * now and then sends up its famous woody flower stalk (AgaveStalkBlock), several blocks tall. Broken, it gives the
 * agave back and agave leaves, more the bigger it was (loot_table/blocks/agave.json). Its leaves end in hard spines:
 * pushing through one (once it's past a seedling) slows you and pricks you, like a berry bush.
 */
public class AgaveBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<AgaveBlock> CODEC = simpleCodec(AgaveBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {Block.box(5, 0, 5, 11, 5, 11), Block.box(3, 0, 3, 13, 8, 13),
            Block.box(1, 0, 1, 15, 14, 15), Block.box(0, 0, 0, 16, 16, 16)};
    // 1 in this many random ticks a young agave grows a stage; and a grown one sends up its stalk
    private static final int GROW_CHANCE = 10, STALK_CHANCE = 80;

    public AgaveBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age < 3) {
            if (random.nextInt(GROW_CHANCE) == 0) level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        } else if (random.nextInt(STALK_CHANCE) == 0) {
            sproutStalk(level, pos, random);
        }
    }

    /** Sends up the flower stalk from a grown agave, if there's room; returns whether it did. */
    public static boolean sproutStalk(Level level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) return false;
        int height = 3 + random.nextInt(3);
        int room = 0;
        while (room < height && level.getBlockState(pos.above(room + 1)).isAir()) room++;
        if (room < 2) return false;
        for (int y = 1; y <= room; y++) {
            level.setBlock(pos.above(y), ModBlocks.AGAVE_STALK.get().defaultBlockState().setValue(AgaveStalkBlock.TOP, y == room), 3);
        }
        return true;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) < 3 || level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (state.getValue(AGE) < 3) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 2);
        } else if (random.nextInt(4) == 0) {
            sproutStalk(level, pos, random);
        }
    }

    // the spines: walking through a grown agave slows you and pricks you whenever you move in it (not the small
    // seedling, nor anything sitting still in it)
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (state.getValue(AGE) == 0 || !(entity instanceof net.minecraft.world.entity.LivingEntity)) return;
        entity.makeStuckInBlock(state, new net.minecraft.world.phys.Vec3(0.8, 0.75, 0.8));
        if (level.isClientSide) return;
        if (entity.xOld != entity.getX() || entity.zOld != entity.getZ()) {
            double dx = Math.abs(entity.getX() - entity.xOld), dz = Math.abs(entity.getZ() - entity.zOld);
            if (dx >= 0.003 || dz >= 0.003) entity.hurt(level.damageSources().cactus(), 1.0F + state.getValue(AGE) * 0.5F);
        }
    }
}
