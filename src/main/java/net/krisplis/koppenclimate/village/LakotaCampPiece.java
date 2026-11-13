package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.common.world.PieceBeardifierModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The whole Lakota camp is one piece, built column by column inside whichever chunk is being decorated; the layout
 * comes from a seed saved with the piece, so every chunk works out the same camp.
 *
 * <ul>
 *   <li>the camp circle: tipis standing in a ring about 35 blocks across, the circle left open to the east, and
 *       every tipi's door facing east too, toward the rising sun, as the Lakota pitched them;</li>
 *   <li>each tipi a cone of hide (white wool) over a frame of poles crossing above the smoke hole, painted with a
 *       band of colour round its foot and a dark one round its top, a symbol or two on its sides; inside, a fire in
 *       the middle, a bed of robes at the back, a rawhide box (a barrel);</li>
 *   <li>in the middle of the circle the council lodge, a great tipi with robes laid round its fire, and before it,
 *       toward the opening, the camp's fire in a ring of stones;</li>
 *   <li>a corral of rails on one side of the ring with the camp's horses in it - tame, so they breed;</li>
 *   <li>drying racks for meat and hides between the tipis;</li>
 *   <li>one or two villagers to a tipi, and the camp's elder at the council lodge door.</li>
 * </ul>
 */
public class LakotaCampPiece extends StructurePiece implements PieceBeardifierModifier {
    private static final float YARD_RADIUS = 24.0F;
    private static final int BOX_RADIUS = 26;
    // the tipis' (and the corral's) centres stand on this ring
    private static final int RING = 16;
    private static final int TIPIS = 9;
    // the camp circle's opening: nothing stands within this angle (degrees) of due east
    private static final float OPENING = 32.0F;
    private static final float TIPI_RADIUS = 3.6F, LODGE_RADIUS = 5.2F;
    private static final int TIPI_HEIGHT = 8, LODGE_HEIGHT = 10;
    private static final float CORRAL_RADIUS = 4.6F;
    // the council lodge stands a little west of the middle, the camp fire to the east of it
    private static final int LODGE_X = -3, FIRE_X = 6;
    private static final int CLEAR_HEIGHT = 30;
    private static final int FILL_DEPTH = 14;
    // the colours a tipi's foot may be painted
    private static final Block[] BANDS = {Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.BLACK_WOOL, Blocks.RED_WOOL};
    private static final Block[] SYMBOLS = {Blocks.RED_WOOL, Blocks.BLUE_WOOL, Blocks.YELLOW_WOOL, Blocks.BLACK_WOOL};

    private final BlockPos centre;
    private final long seed;

    public LakotaCampPiece(BlockPos centre, long seed) {
        super(VillageRegistry.LAKOTA_CAMP_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX_RADIUS, centre.getY() - FILL_DEPTH, centre.getZ() - BOX_RADIUS,
                centre.getX() + BOX_RADIUS, centre.getY() + CLEAR_HEIGHT, centre.getZ() + BOX_RADIUS));
        this.centre = centre;
        this.seed = seed;
    }

    public LakotaCampPiece(CompoundTag tag) {
        super(VillageRegistry.LAKOTA_CAMP_PIECE.get(), tag);
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

    @Override
    public BoundingBox getBeardifierBox() {
        return new BoundingBox(this.centre.getX() - 25, this.centre.getY(), this.centre.getZ() - 25,
                this.centre.getX() + 25, this.centre.getY() + 12, this.centre.getZ() + 25);
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

    /** A tipi on the ring: its centre, the paint of its foot and of its symbols, and where its symbols go. */
    record Tipi(int x, int z, Block band, Block symbol, int symbolAt, boolean rack) {
    }

    static final class Layout {
        final List<Tipi> tipis = new ArrayList<>();
        int corralX, corralZ;
        Direction corralIn;
        final Set<Long> corral = new HashSet<>();
        long corralGate;
        final List<BlockPos> horses = new ArrayList<>();

        Layout(BlockPos centre, long seed) {
            RandomSource random = RandomSource.create(seed);
            int cx = centre.getX(), cz = centre.getZ(), y = centre.getY();
            // the places round the ring, from just past the opening's north side round to its south side
            int slots = TIPIS + 1;
            int corralAt = 2 + random.nextInt(slots - 4);
            for (int i = 0; i < slots; i++) {
                float deg = OPENING + i * (360.0F - 2 * OPENING) / (slots - 1);
                float a = deg * Mth.DEG_TO_RAD;
                int x = cx + Math.round(RING * Mth.cos(a)), z = cz + Math.round(RING * Mth.sin(a));
                if (i == corralAt) {
                    this.corralX = x;
                    this.corralZ = z;
                    this.corralIn = towards(cx - x, cz - z);
                    ring(x, z, CORRAL_RADIUS, this.corral);
                    this.corralGate = nearest(this.corral, x + this.corralIn.getStepX() * CORRAL_RADIUS,
                            z + this.corralIn.getStepZ() * CORRAL_RADIUS);
                    for (int h = 0; h < 6; h++) {
                        float ha = random.nextFloat() * Mth.TWO_PI, hr = random.nextFloat() * 2.4F;
                        this.horses.add(new BlockPos(x + Math.round(hr * Mth.cos(ha)), y + 1, z + Math.round(hr * Mth.sin(ha))));
                    }
                    continue;
                }
                this.tipis.add(new Tipi(x, z, BANDS[random.nextInt(BANDS.length)], SYMBOLS[random.nextInt(SYMBOLS.length)],
                        random.nextInt(4), random.nextFloat() < 0.45F));
            }
        }

        static Direction towards(int dx, int dz) {
            if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        /** Every column inside the circle that touches one outside it. */
        private static void ring(int cx, int cz, float radius, Set<Long> out) {
            int r = Mth.ceil(radius) + 1;
            float r2 = radius * radius;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r2) continue;
                    boolean edge = (dx + 1) * (dx + 1) + dz * dz > r2 || (dx - 1) * (dx - 1) + dz * dz > r2
                            || dx * dx + (dz + 1) * (dz + 1) > r2 || dx * dx + (dz - 1) * (dz - 1) > r2;
                    if (edge) out.add(BlockPos.asLong(cx + dx, 0, cz + dz));
                }
            }
        }

        private static long nearest(Set<Long> ring, float x, float z) {
            long best = 0;
            float bestD = Float.MAX_VALUE;
            for (long p : ring) {
                float dx = BlockPos.getX(p) - x, dz = BlockPos.getZ(p) - z;
                float d = dx * dx + dz * dz;
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
            return best;
        }
    }

    // ---------------------------------------------------------------- building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Layout layout = new Layout(this.centre, this.seed);
        int y0 = this.centre.getY();
        int cx = this.centre.getX(), cz = this.centre.getZ();
        this.buildYard(level, box, y0);
        for (Tipi t : layout.tipis) {
            this.buildTipi(level, box, t.x(), t.z(), y0, TIPI_RADIUS, TIPI_HEIGHT, t.band(), t.symbol(), t.symbolAt(), false);
            if (t.rack()) this.buildRack(level, box, t, y0);
        }
        this.buildTipi(level, box, cx + LODGE_X, cz, y0, LODGE_RADIUS, LODGE_HEIGHT, Blocks.RED_WOOL, Blocks.BLACK_WOOL, 0, true);
        this.buildCampFire(level, box, cx + FIRE_X, cz, y0);
        this.buildCorral(level, box, layout, y0);

        // the people: one or two to a tipi, just outside its door; the elder at the council lodge door
        int n = 0;
        for (Tipi t : layout.tipis) {
            BlockPos door = new BlockPos(t.x() + 4, y0 + 1, t.z());
            this.spawnVillager(level, box, door, false);
            if (n++ % 2 == 0) this.spawnVillager(level, box, door.south(), false);
        }
        this.spawnVillager(level, box, new BlockPos(cx + LODGE_X + 6, y0 + 1, cz + 1), true);
        for (BlockPos h : layout.horses) this.spawnHorse(level, box, h);
    }

    /** Clears the camp ground and grasses it over, with paths worn round the circle, in from the east and to the fire. */
    private void buildYard(WorldGenLevel level, BoundingBox box, int y0) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cx = this.centre.getX(), cz = this.centre.getZ();
        float r2 = (YARD_RADIUS + 0.5F) * (YARD_RADIUS + 0.5F);
        for (int x = Math.max(box.minX(), cx - BOX_RADIUS); x <= Math.min(box.maxX(), cx + BOX_RADIUS); x++) {
            for (int z = Math.max(box.minZ(), cz - BOX_RADIUS); z <= Math.min(box.maxZ(), cz + BOX_RADIUS); z++) {
                int dx = x - cx, dz = z - cz;
                int d2 = dx * dx + dz * dz;
                if (d2 > r2) continue;
                for (int y = y0 + 1; y <= y0 + CLEAR_HEIGHT; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
                float d = Mth.sqrt(d2);
                int h = hash(x, z) % 100;
                boolean path = (d >= 10.5F && d <= 11.7F && h < 80)                                    // round the circle
                        || (dx > 0 && Math.abs(dz) <= 1 && h < 85)                                       // in from the east
                        || ((dx - FIRE_X) * (dx - FIRE_X) + dz * dz <= 12 && h < 70);                    // about the fire
                BlockState top = path ? (h % 4 == 0 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH).defaultBlockState()
                        : Blocks.GRASS_BLOCK.defaultBlockState();
                level.setBlock(pos.set(x, y0, z), top, 2);
                for (int y = y0 - 1; y >= y0 - FILL_DEPTH; y--) {
                    pos.set(x, y, z);
                    if (isGround(level.getBlockState(pos))) break;
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }
    }

    /**
     * A tipi: a cone of hide (white wool) narrowing from {@code radius} at the ground to the smoke hole at
     * {@code height}, a band of paint round its foot and a dark one round its top (the earth and the sky), a symbol
     * painted on two sides, its poles crossing above the smoke hole, and its door on the east. Inside, a fire in the
     * middle; in a family's tipi a bed of robes at the back and a rawhide box, in the council lodge robes all round.
     */
    private void buildTipi(WorldGenLevel level, BoundingBox box, int tx, int tz, int y0, float radius, int height,
                           Block band, Block symbol, int symbolAt, boolean council) {
        int span = Mth.ceil(radius) + 1;
        BlockState hide = Blocks.WHITE_WOOL.defaultBlockState();
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                float d = Mth.sqrt(dx * dx + dz * dz);
                int x = tx + dx, z = tz + dz;
                if (d <= radius + 0.25F) {
                    this.set(level, box, x, y0, z, (hash(x, z) & 3) == 0 ? Blocks.COARSE_DIRT.defaultBlockState()
                            : Blocks.DIRT_PATH.defaultBlockState());
                    this.fillDown(level, box, x, y0 - 1, z, Blocks.DIRT.defaultBlockState());
                }
                for (int k = 1; k <= height; k++) {
                    float r = radius * (1.0F - (k - 1) / (float) height);
                    boolean shell = d <= r + 0.25F && d > r - 1.0F && r >= 0.6F;
                    // the smoke hole
                    if (k >= height - 1 && dx == 0 && dz == 0) shell = false;
                    if (!shell) {
                        if (d <= r + 0.25F) this.set(level, box, x, y0 + k, z, Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    BlockState s = hide;
                    if (k == 1) s = band.defaultBlockState();
                    else if (k == 2 && (dx + dz & 1) == 0) s = band.defaultBlockState();          // a toothed edge to the band
                    else if (k >= height - 2) s = Blocks.BLACK_WOOL.defaultBlockState();
                    else if (k == height / 2 && isSymbol(dx, dz, symbolAt)) s = symbol.defaultBlockState();
                    this.set(level, box, x, y0 + k, z, s);
                }
            }
        }
        // the poles, crossing above the smoke hole
        for (int[] p : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            for (int k = height; k <= height + 2; k++) {
                this.set(level, box, tx + p[0], y0 + k, tz + p[1], Blocks.SPRUCE_FENCE.defaultBlockState());
            }
        }
        // the door, on the east, two high
        int door = Mth.floor(radius);
        while (door > 0 && !(door <= radius + 0.25F && door > radius - 1.0F)) door--;
        for (int k = 1; k <= 2; k++) this.set(level, box, tx + door, y0 + k, tz, Blocks.AIR.defaultBlockState());
        this.set(level, box, tx + door + 1, y0, tz, Blocks.DIRT_PATH.defaultBlockState());
        // the fire in the middle
        this.set(level, box, tx, y0, tz, Blocks.CAMPFIRE.defaultBlockState());
        if (council) {
            // robes laid in a ring round the fire for the council to sit on
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    float d = Mth.sqrt(dx * dx + dz * dz);
                    if (d >= 2.0F && d <= 3.2F && !(dx > 0 && Math.abs(dz) <= 1)) {
                        this.set(level, box, tx + dx, y0 + 1, tz + dz, Blocks.BROWN_CARPET.defaultBlockState());
                    }
                }
            }
            return;
        }
        // a bed of robes along the back (west), a robe on either side, a rawhide box
        BlockState mat = ModBlocks.SLEEPING_MAT.get().defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        this.set(level, box, tx - 2, y0 + 1, tz, mat.setValue(BedBlock.PART, BedPart.FOOT));
        this.set(level, box, tx - 2, y0 + 1, tz - 1, mat.setValue(BedBlock.PART, BedPart.HEAD));
        this.set(level, box, tx, y0 + 1, tz + 2, Blocks.BROWN_CARPET.defaultBlockState());
        this.set(level, box, tx - 1, y0 + 1, tz + 2, Blocks.BROWN_CARPET.defaultBlockState());
        this.set(level, box, tx, y0 + 1, tz - 2, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
    }

    // where on a tipi's side its painted symbols go (two small marks on opposite sides, north/south or west)
    private static boolean isSymbol(int dx, int dz, int at) {
        return switch (at) {
            case 0 -> Math.abs(dx) <= 0 && Math.abs(dz) >= 1;
            case 1 -> dx < 0 && Math.abs(dz) <= 0;
            case 2 -> Math.abs(dx) == 1 && Math.abs(dz) >= 1;
            default -> dx < 0 && Math.abs(dz) == 1;
        };
    }

    /** The camp fire, in the open before the council lodge: a fire in a ring of stones, logs to sit on. */
    private void buildCampFire(WorldGenLevel level, BoundingBox box, int fx, int fz, int y0) {
        this.set(level, box, fx, y0, fz, Blocks.CAMPFIRE.defaultBlockState());
        for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}}) {
            this.set(level, box, fx + o[0], y0, fz + o[1], Blocks.COBBLESTONE.defaultBlockState());
        }
        BlockState log = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X);
        for (int dx = -1; dx <= 1; dx++) {
            this.set(level, box, fx + dx, y0 + 1, fz + 3, log);
            this.set(level, box, fx + dx, y0 + 1, fz - 3, log);
        }
    }

    /** The horse corral: a round fence of rails with a gate facing the middle of the camp, hay inside. */
    private void buildCorral(WorldGenLevel level, BoundingBox box, Layout layout, int y0) {
        for (long p : layout.corral) {
            int x = BlockPos.getX(p), z = BlockPos.getZ(p);
            if (p == layout.corralGate) {
                Direction facing = layout.corralIn.getAxis() == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
                this.set(level, box, x, y0 + 1, z, Blocks.SPRUCE_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing));
                continue;
            }
            this.set(level, box, x, y0 + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState()
                    .setValue(CrossCollisionBlock.NORTH, layout.corral.contains(BlockPos.asLong(x, 0, z - 1)))
                    .setValue(CrossCollisionBlock.SOUTH, layout.corral.contains(BlockPos.asLong(x, 0, z + 1)))
                    .setValue(CrossCollisionBlock.WEST, layout.corral.contains(BlockPos.asLong(x - 1, 0, z)))
                    .setValue(CrossCollisionBlock.EAST, layout.corral.contains(BlockPos.asLong(x + 1, 0, z))));
        }
        Direction back = layout.corralIn.getOpposite();
        this.set(level, box, layout.corralX + back.getStepX() * 3, y0 + 1, layout.corralZ + back.getStepZ() * 3,
                Blocks.HAY_BLOCK.defaultBlockState());
    }

    /** A drying rack beside a tipi, toward the middle of the circle: two posts and a rail hung with meat and hides. */
    private void buildRack(WorldGenLevel level, BoundingBox box, Tipi t, int y0) {
        int cx = this.centre.getX(), cz = this.centre.getZ();
        Direction in = Layout.towards(cx - t.x(), cz - t.z());
        Direction along = in.getClockWise();
        int bx = t.x() + in.getStepX() * 5, bz = t.z() + in.getStepZ() * 5;
        for (int s = -1; s <= 1; s++) {
            int x = bx + along.getStepX() * s, z = bz + along.getStepZ() * s;
            if (Math.abs(s) == 1) this.set(level, box, x, y0 + 1, z, Blocks.SPRUCE_FENCE.defaultBlockState());
            this.set(level, box, x, y0 + 2, z, fence(along, s < 1, along.getOpposite(), s > -1));
            this.set(level, box, x, y0 + 3, z, (s == 0 ? Blocks.RED_CARPET : Blocks.BROWN_CARPET).defaultBlockState());
        }
    }

    private static BlockState fence(Direction a, boolean toA, Direction b, boolean toB) {
        return Blocks.SPRUCE_FENCE.defaultBlockState().setValue(side(a), toA).setValue(side(b), toB);
    }

    private static BooleanProperty side(Direction d) {
        return switch (d) {
            case NORTH -> CrossCollisionBlock.NORTH;
            case SOUTH -> CrossCollisionBlock.SOUTH;
            case WEST -> CrossCollisionBlock.WEST;
            default -> CrossCollisionBlock.EAST;
        };
    }

    // ---------------------------------------------------------------- the people and the horses

    private void spawnVillager(WorldGenLevel level, BoundingBox box, BlockPos pos, boolean elder) {
        if (!box.isInside(pos) || !level.getBlockState(pos).isAir()) return;
        LakotaVillagerEntity villager = VillageRegistry.LAKOTA_VILLAGER.get().create(level.getLevel());
        if (villager == null) return;
        villager.setLeader(elder);
        villager.setVillage(this.centre.above());
        villager.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }

    /** One of the camp's horses: tame (so the camp can breed them), any coat. */
    private void spawnHorse(WorldGenLevel level, BoundingBox box, BlockPos pos) {
        if (!box.isInside(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return;
        Horse horse = EntityType.HORSE.create(level.getLevel());
        if (horse == null) return;
        horse.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        horse.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        horse.setTamed(true);
        horse.setPersistenceRequired();
        level.addFreshEntityWithPassengers(horse);
    }

    // ---------------------------------------------------------------- helpers

    private static int hash(int x, int z) {
        int h = x * 73428767 ^ z * 912931;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return (h ^ (h >>> 15)) & Integer.MAX_VALUE;
    }

    private static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    private void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
    }

    private void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        this.set(level, box, new BlockPos(x, y, z), state);
    }

    /** Fills from {@code y} down until it meets solid ground, so nothing floats. */
    private void fillDown(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        if (!box.isInside(pos)) return;
        for (int i = 0; i < FILL_DEPTH && !isGround(level.getBlockState(pos)); i++) {
            level.setBlock(pos, state, 2);
            pos.move(Direction.DOWN);
        }
    }
}
