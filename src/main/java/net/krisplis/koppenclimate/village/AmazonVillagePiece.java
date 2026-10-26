package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.common.world.PieceBeardifierModifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole Amazon village is one piece, built column by column inside whichever chunk is being decorated.
 * The layout (where the huts, the farm, the well and the fire go) comes from a seed saved with the piece, so
 * every chunk works out the same village.
 *
 * <ul>
 *   <li>the clearing: a disc of dirt path about 50 blocks across, cleared of trees and plants;</li>
 *   <li>a ring of 5x5 huts of cobbled yellow limestone with flat bamboo roofs, doorways facing the middle,
 *       open windows, a sleeping mat inside, and no wood, glass or doors;</li>
 *   <li>in the middle a small well and a 2x2 fire pit of lit campfires;</li>
 *   <li>a few torches, and sometimes one small potato, carrot or beetroot field;</li>
 *   <li>one villager per hut, and one tribe leader.</li>
 * </ul>
 */
public class AmazonVillagePiece extends StructurePiece implements PieceBeardifierModifier {
    // the clearing's radius wobbles between about 22 and 25 blocks
    private static final float CLEARING_RADIUS = 23.5F;
    private static final int BOX_RADIUS = 25;
    // the huts' (and the farm's) centres stand on this ring
    private static final int RING_RADIUS = 15;
    // trees and plants are cleared this high above the clearing
    private static final int CLEAR_HEIGHT = 48;
    // and holes under it are filled this deep
    private static final int FILL_DEPTH = 14;

    private final BlockPos centre;
    private final long seed;

    public AmazonVillagePiece(BlockPos centre, long seed) {
        super(VillageRegistry.AMAZON_VILLAGE_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX_RADIUS, centre.getY() - FILL_DEPTH, centre.getZ() - BOX_RADIUS,
                centre.getX() + BOX_RADIUS, centre.getY() + CLEAR_HEIGHT, centre.getZ() + BOX_RADIUS));
        this.centre = centre;
        this.seed = seed;
    }

    public AmazonVillagePiece(CompoundTag tag) {
        super(VillageRegistry.AMAZON_VILLAGE_PIECE.get(), tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.seed = tag.getLong("Layout");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.centre.getX());
        tag.putInt("CY", this.centre.getY());
        tag.putInt("CZ", this.centre.getZ());
        tag.putLong("Layout", this.seed);
    }

    // ---------------------------------------------------------------- terrain adaptation

    // the ground under the clearing is levelled to the village floor, as it is under vanilla villages
    @Override
    public BoundingBox getBeardifierBox() {
        return new BoundingBox(this.centre.getX() - 22, this.centre.getY(), this.centre.getZ() - 22,
                this.centre.getX() + 22, this.centre.getY() + 12, this.centre.getZ() + 22);
    }

    @Override
    public TerrainAdjustment getTerrainAdjustment() {
        return TerrainAdjustment.BEARD_THIN;
    }

    @Override
    public int getGroundLevelDelta() {
        return 1;
    }

    // ---------------------------------------------------------------- layout

    record Hut(int x, int z, Direction door, boolean outsideTorch) {
    }

    record Farm(int x, int z, Direction along, Block crop) {
    }

    /** Where everything goes; worked out the same way from the seed for every chunk. */
    static final class Layout {
        final List<Hut> huts = new ArrayList<>();
        Farm farm;
        int wellX, wellZ, fireX, fireZ;
        final List<BlockPos> torchPosts = new ArrayList<>();
        BlockPos leaderSpawn;
        final float edgePhase1, edgePhase2;

        Layout(BlockPos centre, long seed) {
            RandomSource random = RandomSource.create(seed);
            int cx = centre.getX(), cz = centre.getZ(), y = centre.getY();
            int hutCount = 6 + random.nextInt(3);
            boolean hasFarm = random.nextBoolean();
            int slots = hutCount + (hasFarm ? 1 : 0);
            int farmSlot = hasFarm ? random.nextInt(slots) : -1;
            float start = random.nextFloat() * Mth.TWO_PI;
            for (int i = 0; i < slots; i++) {
                float angle = start + i * Mth.TWO_PI / slots + (random.nextFloat() - 0.5F) * 0.1F;
                int x = cx + Math.round(RING_RADIUS * Mth.cos(angle));
                int z = cz + Math.round(RING_RADIUS * Mth.sin(angle));
                Direction towardCentre = towards(cx - x, cz - z);
                if (i == farmSlot) {
                    Block[] crops = {Blocks.POTATOES, Blocks.CARROTS, Blocks.BEETROOTS};
                    this.farm = new Farm(x, z, towardCentre, crops[random.nextInt(crops.length)]);
                } else {
                    this.huts.add(new Hut(x, z, towardCentre, random.nextFloat() < 0.4F));
                }
            }
            // the well and the fire face each other across the middle, turned any of four ways
            int turn = random.nextInt(4);
            int[] well = rotate(-4, 0, turn), fire = rotate(4, 0, turn);
            this.wellX = cx + well[0];
            this.wellZ = cz + well[1];
            this.fireX = cx + fire[0];
            this.fireZ = cz + fire[1];
            for (int side = -1; side <= 1; side += 2) {
                int[] post = rotate(0, 5 * side, turn);
                this.torchPosts.add(new BlockPos(cx + post[0], y + 1, cz + post[1]));
            }
            int[] leader = rotate(0, 2, turn);
            this.leaderSpawn = new BlockPos(cx + leader[0], y + 1, cz + leader[1]);
            this.edgePhase1 = random.nextFloat() * Mth.TWO_PI;
            this.edgePhase2 = random.nextFloat() * Mth.TWO_PI;
        }

        float edgeRadius(float angle) {
            return CLEARING_RADIUS + Mth.sin(3 * angle + this.edgePhase1) + 0.6F * Mth.sin(5 * angle + this.edgePhase2);
        }

        private static Direction towards(int dx, int dz) {
            if (Math.abs(dx) > Math.abs(dz)) {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            }
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        private static int[] rotate(int x, int z, int quarterTurns) {
            for (int i = 0; i < quarterTurns; i++) {
                int t = x;
                x = -z;
                z = t;
            }
            return new int[]{x, z};
        }
    }

    // ---------------------------------------------------------------- building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Layout layout = new Layout(this.centre, this.seed);
        int y0 = this.centre.getY();

        this.buildClearing(level, box, layout, y0);
        for (Hut hut : layout.huts) {
            this.buildHut(level, box, hut, y0);
        }
        if (layout.farm != null) {
            this.buildFarm(level, box, layout.farm, y0, random);
        }
        this.buildWell(level, box, layout.wellX, layout.wellZ, y0);
        this.buildFirePit(level, box, layout.fireX, layout.fireZ, y0);
        for (BlockPos post : layout.torchPosts) {
            this.set(level, box, post, ModBlocks.COBBLED_YELLOW_LIMESTONE_WALL.get().defaultBlockState());
            this.set(level, box, post.above(), Blocks.TORCH.defaultBlockState());
        }

        // villagers: one per hut, and the village's only leader by the fire
        for (Hut hut : layout.huts) {
            this.spawnVillager(level, box, new BlockPos(hut.x(), y0 + 1, hut.z()), false);
        }
        this.spawnVillager(level, box, layout.leaderSpawn, true);
    }

    /** Clears the disc of trees and plants, fills any holes and lays dirt path over it. */
    private void buildClearing(WorldGenLevel level, BoundingBox box, Layout layout, int y0) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cx = this.centre.getX(), cz = this.centre.getZ();
        for (int x = Math.max(box.minX(), cx - BOX_RADIUS); x <= Math.min(box.maxX(), cx + BOX_RADIUS); x++) {
            for (int z = Math.max(box.minZ(), cz - BOX_RADIUS); z <= Math.min(box.maxZ(), cz + BOX_RADIUS); z++) {
                int dx = x - cx, dz = z - cz;
                float dist = Mth.sqrt(dx * dx + dz * dz);
                if (dist > layout.edgeRadius((float) Mth.atan2(dz, dx))) {
                    continue;
                }
                for (int y = y0 + 1; y <= y0 + CLEAR_HEIGHT; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                pos.set(x, y0, z);
                level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), 2);
                for (int y = y0 - 1; y >= y0 - FILL_DEPTH; y--) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (isGround(state)) {
                        break;
                    }
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }
    }

    private static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && !state.is(Blocks.BAMBOO);
    }

    /**
     * A 5x5 limestone hut, three blocks high inside, with a flat roof of bamboo blocks and a sleeping mat. The doorway is in the middle
     * of the wall that faces the centre of the village; the two side walls have open windows.
     */
    private void buildHut(WorldGenLevel level, BoundingBox box, Hut hut, int y0) {
        BlockState limestone = ModBlocks.COBBLED_YELLOW_LIMESTONE.get().defaultBlockState();
        int hx = hut.x(), hz = hut.z();
        Direction door = hut.door();
        Direction side = door.getClockWise();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x = hx + dx, z = hz + dz;
                // floor, on a footing down to solid ground
                this.set(level, box, x, y0, z, limestone);
                this.fillDown(level, box, x, y0 - 1, z, limestone);
                boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                for (int y = y0 + 1; y <= y0 + 3; y++) {
                    this.set(level, box, x, y, z, wall ? limestone : Blocks.AIR.defaultBlockState());
                }
            }
        }
        // doorway, two blocks high
        for (int y = y0 + 1; y <= y0 + 2; y++) {
            this.set(level, box, hx + door.getStepX() * 2, y, hz + door.getStepZ() * 2, Blocks.AIR.defaultBlockState());
        }
        // a window in each side wall
        for (Direction d : new Direction[]{side, side.getOpposite()}) {
            this.set(level, box, hx + d.getStepX() * 2, y0 + 2, hz + d.getStepZ() * 2, Blocks.AIR.defaultBlockState());
        }
        // torches: inside on the back wall, and sometimes outside beside the doorway
        Direction back = door.getOpposite();
        this.set(level, box, hx + back.getStepX(), y0 + 2, hz + back.getStepZ(),
                Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, door));
        if (hut.outsideTorch()) {
            this.set(level, box, hx + door.getStepX() * 3 + side.getStepX(), y0 + 2, hz + door.getStepZ() * 3 + side.getStepZ(),
                    Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, door));
        }
        // a sleeping mat along the back wall, its pillow end towards one side
        BlockPos foot = new BlockPos(hx + back.getStepX() + side.getOpposite().getStepX(), y0 + 1, hz + back.getStepZ() + side.getOpposite().getStepZ());
        BlockPos head = foot.relative(side);
        BlockState mat = ModBlocks.SLEEPING_MAT.get().defaultBlockState().setValue(BedBlock.FACING, side);
        this.set(level, box, foot, mat.setValue(BedBlock.PART, BedPart.FOOT));
        this.set(level, box, head, mat.setValue(BedBlock.PART, BedPart.HEAD));
        this.buildRoof(level, box, hx, y0 + 4, hz);
    }

    /** A flat roof of bamboo blocks, one block thick, reaching a block past the walls all round. */
    private void buildRoof(WorldGenLevel level, BoundingBox box, int hx, int y, int hz) {
        BlockState bamboo = Blocks.BAMBOO_BLOCK.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                this.set(level, box, hx + dx, y, hz + dz, bamboo);
            }
        }
    }

    /** A 5x5 field inside a limestone edge, with a channel of water down the middle. No composter, no farmer. */
    private void buildFarm(WorldGenLevel level, BoundingBox box, Farm farm, int y0, RandomSource random) {
        BlockState limestone = ModBlocks.COBBLED_YELLOW_LIMESTONE.get().defaultBlockState();
        BlockState farmland = Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE);
        boolean channelAlongX = farm.along().getAxis() == Direction.Axis.X;
        CropBlock crop = (CropBlock) farm.crop();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = farm.x() + dx, z = farm.z() + dz;
                this.fillDown(level, box, x, y0 - 1, z, Blocks.DIRT.defaultBlockState());
                if (Math.abs(dx) == 3 || Math.abs(dz) == 3) {
                    this.set(level, box, x, y0, z, limestone);
                } else if ((channelAlongX ? dz : dx) == 0) {
                    this.set(level, box, x, y0, z, Blocks.WATER.defaultBlockState());
                } else {
                    this.set(level, box, x, y0, z, farmland);
                    int age = random.nextInt(crop.getMaxAge() + 1);
                    this.set(level, box, x, y0 + 1, z, crop.getStateForAge(age));
                }
            }
        }
    }

    /** A small 4x4 well: a limestone rim round 2x2 water four deep, wall posts and a bamboo slab roof. */
    private void buildWell(WorldGenLevel level, BoundingBox box, int wx, int wz, int y0) {
        BlockState limestone = ModBlocks.COBBLED_YELLOW_LIMESTONE.get().defaultBlockState();
        for (int dx = -2; dx <= 1; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                int x = wx + dx, z = wz + dz;
                boolean rim = dx == -2 || dx == 1 || dz == -2 || dz == 1;
                boolean corner = (dx == -2 || dx == 1) && (dz == -2 || dz == 1);
                for (int y = y0 - 4; y <= y0; y++) {
                    this.set(level, box, x, y, z, rim || y == y0 - 4 ? limestone : Blocks.WATER.defaultBlockState());
                }
                if (rim) {
                    this.set(level, box, x, y0 + 1, z, limestone);
                }
                if (corner) {
                    this.set(level, box, x, y0 + 2, z, ModBlocks.COBBLED_YELLOW_LIMESTONE_WALL.get().defaultBlockState());
                    this.set(level, box, x, y0 + 3, z, ModBlocks.COBBLED_YELLOW_LIMESTONE_WALL.get().defaultBlockState());
                }
                this.set(level, box, x, y0 + 4, z, Blocks.BAMBOO_MOSAIC_SLAB.defaultBlockState());
            }
        }
    }

    /** A 2x2 fire of lit campfires inside a ring of cobbled limestone. */
    private void buildFirePit(WorldGenLevel level, BoundingBox box, int fx, int fz, int y0) {
        for (int dx = -2; dx <= 1; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                boolean ring = dx == -2 || dx == 1 || dz == -2 || dz == 1;
                this.set(level, box, fx + dx, y0 - 1, fz + dz, Blocks.DIRT.defaultBlockState());
                this.set(level, box, fx + dx, y0, fz + dz,
                        ring ? ModBlocks.COBBLED_YELLOW_LIMESTONE.get().defaultBlockState() : Blocks.CAMPFIRE.defaultBlockState());
            }
        }
    }

    private void spawnVillager(WorldGenLevel level, BoundingBox box, BlockPos pos, boolean leader) {
        if (!box.isInside(pos)) {
            return;
        }
        AmazonVillagerEntity villager = VillageRegistry.AMAZON_VILLAGER.get().create(level.getLevel());
        if (villager == null) {
            return;
        }
        villager.setLeader(leader);
        villager.setVillage(this.centre.above());
        villager.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }

    // ---------------------------------------------------------------- helpers

    private void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    private void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        this.set(level, box, new BlockPos(x, y, z), state);
    }

    /** Fills from {@code y} down until it meets solid ground, so nothing floats. */
    private void fillDown(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        for (int i = 0; i < FILL_DEPTH && !isGround(level.getBlockState(pos)); i++) {
            level.setBlock(pos, state, 2);
            pos.move(Direction.DOWN);
        }
    }
}
