package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.FruitLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The English Mansion of the English countryside (Cfb): a dark Jacobean manor house, taken over by the woodland
 * mansions' illagers, on a perfect lawn. Turned one of four ways at random.
 * <ul>
 *   <li>the house: an E in plan - a long main range, two wings coming forward at its ends, a tall porch in the middle
 *       - three storeys of deepslate brick under steep slate roofs, rows of tall mullioned windows, stepped gables,
 *       and tall chimney stacks;</li>
 *   <li>inside, dark oak floors and panelled partitions. The middle of the house is the great hall, two storeys high,
 *       with a white marble imperial staircase - one broad flight to a landing, two flights back up to galleries round
 *       the hall, red runners, marble balustrades and columns, chandeliers, banners. West of it, the great dining hall,
 *       also two storeys high and galleried, its long table set for twenty; east, the drawing room and the study (its
 *       enchanting table among the bookshelves); a kitchen, pantry, smithy and still-room in the wings. Bedrooms above,
 *       the long gallery (the library) over the dining hall, the state bedroom over the great hall, and a gaol where
 *       the illagers keep their allays; a balcony over the front door;</li>
 *   <li>working fireplaces: each chimney stack carries two flues, a lit campfire on a bale of hay at the foot of each
 *       (it cooks, and its smoke rises up the flue and out of the stack's top); lanterns hang in every room - the
 *       house is bright throughout, so once it's cleared nothing more spawns in it;</li>
 *   <li>its garrison: vindicators, evokers and their vexes (and the caged allays); the woodland mansion's chests;</li>
 *   <li>outside, a gravel forecourt with a fountain between the wings, and a perfect striped lawn all round, hedged in;
 *       from the front door the long walk runs a hundred blocks out - an unbroken lawn, the drive down its middle
 *       between lamp posts, one avenue of apple trees each side - to an obelisk in a round and the gate.</li>
 * </ul>
 * In the house's own frame x runs along the front (east positive), z out from the back towards the front and the
 * drive (south positive), y up from the ground floor.
 */
public class MansionPiece extends StructurePiece {
    static final ResourceKey<LootTable> MANSION_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.withDefaultNamespace("chests/woodland_mansion"));

    // the plan: the main range, the wings, the porch; three storeys of five
    static final int MX = 24, MZ0 = -7, MZ1 = 6, WX0 = 14, WZ1 = 20, PX = 4, PZ1 = 11, STOREY = 5, STOREYS = 3, ATTIC = 15;
    static final int[] MAIN_PARTITIONS = {-9, 9, 16};
    static final int[] STACKS = {-19, -13, 12, 20};
    // the great hall (two storeys high, the staircase) and the great dining hall west of it (two storeys, galleried)
    static final int HALL_X = 8, DINE_X0 = -23, DINE_X1 = -10;
    // the grounds: the lawns about the house, and the long walk running out from its front (levelled), blended into
    // the land round them
    static final int CLOSE_X = 46, CLOSE_Z0 = -26, CLOSE_Z1 = 44, WALK_X = 24, WALK_Z1 = 104, MARGIN = 12, HEIGHT = 32;

    final BlockPos origin;
    final long seed;
    final int turn;

    public MansionPiece(BlockPos origin, long seed, int turn) {
        super(VillageRegistry.MANSION_PIECE.get(), 0, box(origin, turn));
        this.origin = origin;
        this.seed = seed;
        this.turn = turn;
    }

    public MansionPiece(CompoundTag tag) {
        super(VillageRegistry.MANSION_PIECE.get(), tag);
        this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        this.seed = tag.getLong("Layout");
        this.turn = tag.getInt("Turn");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OY", this.origin.getY());
        tag.putInt("OZ", this.origin.getZ());
        tag.putLong("Layout", this.seed);
        tag.putInt("Turn", this.turn);
    }

    static BoundingBox box(BlockPos origin, int turn) {
        int x0 = -CLOSE_X - MARGIN, x1 = CLOSE_X + MARGIN, z0 = CLOSE_Z0 - MARGIN, z1 = WALK_Z1 + MARGIN;
        int ax = CathedralPiece.rx(turn, x0, z0), az = CathedralPiece.rz(turn, x0, z0);
        int bx = CathedralPiece.rx(turn, x1, z1), bz = CathedralPiece.rz(turn, x1, z1);
        return new BoundingBox(origin.getX() + Math.min(ax, bx), origin.getY() - 8, origin.getZ() + Math.min(az, bz),
                origin.getX() + Math.max(ax, bx), origin.getY() + HEIGHT, origin.getZ() + Math.max(az, bz));
    }

    // ================================================================ placing blocks (in the house's own frame)

    static final Rotation[] ROTATIONS = CathedralPiece.ROTATIONS;

    BlockPos at(int x, int y, int z) {
        return new BlockPos(this.origin.getX() + CathedralPiece.rx(this.turn, x, z), this.origin.getY() + y,
                this.origin.getZ() + CathedralPiece.rz(this.turn, x, z));
    }

    int[] local(int wx, int wz) {
        int dx = wx - this.origin.getX(), dz = wz - this.origin.getZ();
        return switch (this.turn) {
            case 1 -> new int[]{dz, -dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{-dz, dx};
            default -> new int[]{dx, dz};
        };
    }

    void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = this.at(x, y, z);
        if (box.isInside(pos)) level.setBlock(pos, state.rotate(ROTATIONS[this.turn]), 2);
    }

    /**
     * The block at a spot in the structure's frame - read only near the chunk being built (the chunk and the ones
     * round it, all the world lets a structure see while it's being generated); anything further off reads as solid
     * stone, so nothing is put anywhere on the strength of it.
     */
    BlockState get(WorldGenLevel level, int x, int y, int z) {
        BlockPos pos = this.at(x, y, z);
        BoundingBox reach = REACH.get();
        if (reach != null && !reach.isInside(pos)) return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        return level.getBlockState(pos);
    }

    // the part of the world this thread's chunk may read while it's built (chunks are built on several threads at once)
    static final ThreadLocal<BoundingBox> REACH = new ThreadLocal<>();

    int roll(int x, int y, int z, int n) {
        long h = (x * 341873128712L + z * 132897987541L + y * 6971L) ^ this.seed;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) Math.floorMod(h, (long) n);
    }

    static BlockState st(Block b) {
        return b.defaultBlockState();
    }

    static BlockState stair(Block b, Direction facing, boolean top) {
        return b.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    static BlockState slab(Block b, SlabType type) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    static BlockState log(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    static BlockState pane(Direction.Axis along) {
        boolean x = along == Direction.Axis.X;
        return Blocks.GLASS_PANE.defaultBlockState().setValue(CrossCollisionBlock.EAST, x).setValue(CrossCollisionBlock.WEST, x)
                .setValue(CrossCollisionBlock.NORTH, !x).setValue(CrossCollisionBlock.SOUTH, !x);
    }

    static final BlockState AIR = Blocks.AIR.defaultBlockState();
    static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

    // ================================================================ the plan

    static boolean inMain(int x, int z) {
        return Math.abs(x) <= MX && z >= MZ0 && z <= MZ1;
    }

    static boolean inWing(int x, int z) {
        return Math.abs(x) >= WX0 && Math.abs(x) <= MX && z > MZ1 && z <= WZ1;
    }

    static boolean inPorch(int x, int z) {
        return Math.abs(x) <= PX && z > MZ1 && z <= PZ1;
    }

    static boolean inHouse(int x, int z) {
        return inMain(x, z) || inWing(x, z) || inPorch(x, z);
    }

    static boolean inGreatHall(int x, int z) {
        return Math.abs(x) <= HALL_X && z > MZ0 && z < MZ1 + 1;
    }

    static boolean inDiningHall(int x, int z) {
        return x >= DINE_X0 && x <= DINE_X1 && z > MZ0 && z < MZ1;
    }

    /**
     * What's left of the first floor over the two great halls: in the great hall, the galleries along its sides and
     * across its front (over the entrance), reached by the staircase; in the dining hall, a gallery two wide all round.
     */
    static boolean gallery(int x, int z) {
        int ax = Math.abs(x);
        if (inGreatHall(x, z)) return ax >= 3 && z >= 0 || z >= 3;
        if (inDiningHall(x, z)) return x <= DINE_X0 + 1 || x >= DINE_X1 - 1 || z <= MZ0 + 2 || z >= MZ1 - 2;
        return true;
    }

    /** An outer wall: in the house, beside a column that isn't. */
    static boolean outerWall(int x, int z) {
        if (!inHouse(x, z)) return false;
        for (Direction d : Direction.Plane.HORIZONTAL) if (!inHouse(x + d.getStepX(), z + d.getStepZ())) return true;
        return false;
    }

    /** The panelled partitions between the rooms. */
    static boolean partition(int x, int z) {
        int ax = Math.abs(x);
        if (inMain(x, z) && !outerWall(x, z)) {
            for (int p : MAIN_PARTITIONS) if (x == p) return true;
        }
        // between the wings and the main range, and across each wing
        return ax > WX0 && ax < MX && (z == MZ1 || z == 13);
    }

    /** The doorways through the partitions (two wide, three high). */
    static boolean doorway(int x, int z) {
        int ax = Math.abs(x);
        if (inMain(x, z)) for (int p : MAIN_PARTITIONS) if (x == p) return z == 0 || z == 1;
        return ax == 19 || ax == 20;
    }

    // the roofs: steep slate gables over the main range (ridge along x) and the wings (ridge along z)
    static int mainRoof(int x, int z) {
        if (Math.abs(x) > MX || z < MZ0 - 1 || z > MZ1 + 1) return -1;
        return 14 + Math.min(z - (MZ0 - 1), MZ1 + 1 - z);
    }

    static int wingRoof(int x, int z) {
        int ax = Math.abs(x);
        if (ax < WX0 - 1 || ax > MX + 1 || z < 0 || z > WZ1 + 1) return -1;
        return 14 + Math.min(ax - (WX0 - 1), MX + 1 - ax);
    }

    // the chimney stacks: on the main range's back wall, and on the wings' outer walls
    static boolean mainStack(int x, int z) {
        if (z != MZ0 - 1 && z != MZ0 - 2) return false;
        for (int c : STACKS) if (Math.abs(x - c) <= 2) return true;
        return false;
    }

    static boolean wingStack(int x, int z) {
        int ax = Math.abs(x);
        return (ax == MX + 1 || ax == MX + 2) && z >= 15 && z <= 19;
    }

    /**
     * The house's shell, cell by cell: walls, floors, windows, roofs, stacks; air inside (returned as AIR), or null
     * where the house has nothing.
     */
    BlockState design(int x, int y, int z) {
        int ax = Math.abs(x);
        // ---- the chimney stacks, and their flues (a hay bale and a lit campfire at the foot of each)
        if (mainStack(x, z) || wingStack(x, z)) {
            int top = mainStack(x, z) ? 24 : 23;
            boolean main = mainStack(x, z);
            // the two flues: the ground floor's, and the first floor's
            boolean inner = main ? z == MZ0 - 1 : ax == MX + 1;
            int along = main ? x - nearestStack(x) : z - 17;
            int flueFloor = along == -1 ? 0 : along == 1 ? STOREY : -1;
            if (inner && flueFloor >= 0 && y >= flueFloor) {
                if (y == flueFloor) return st(Blocks.HAY_BLOCK);
                if (y == flueFloor + 1) return Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true)
                        .setValue(CampfireBlock.SIGNAL_FIRE, true)
                        .setValue(CampfireBlock.FACING, main ? Direction.SOUTH : (x > 0 ? Direction.WEST : Direction.EAST));
                if (y <= top) return AIR;
                return null;
            }
            if (y < 0) return null;
            if (y <= top) return y == top ? st(Blocks.POLISHED_DEEPSLATE) : st(Blocks.DEEPSLATE_BRICKS);
            return null;
        }
        if (y < 0) return null;
        boolean house = inHouse(x, z);
        // ---- the roofs (the porch has its own flat roof and parapet)
        if (y > 14 && !inPorch(x, z)) {
            int m = mainRoof(x, z), w = wingRoof(x, z);
            if (inPorch(x, z - 1) && z <= MZ1 + 1 && ax <= PX) m = -1;
            int h = Math.max(m, w);
            if (h < 0) return null;
            boolean byWing = w > m;
            if (y == h) {
                if (byWing) {
                    int d = ax - (WX0 - 1), e = MX + 1 - ax;
                    if (d == e) return st(Blocks.DEEPSLATE_TILES);
                    boolean towardOut = d > e;
                    Direction f = (x > 0) == towardOut ? Direction.WEST : Direction.EAST;
                    return stair(Blocks.DEEPSLATE_TILE_STAIRS, f, false);
                }
                return stair(Blocks.DEEPSLATE_TILE_STAIRS, z < 0 ? Direction.SOUTH : Direction.NORTH, false);
            }
            if (y > h) return null;
            if (!house) return y == h - 1 && h > 15 ? st(Blocks.DEEPSLATE_TILES) : null;
            if (outerWall(x, z)) return y == h - 1 ? st(Blocks.DEEPSLATE_TILES) : this.wall(x, y, z);   // the gables
            if (y == h - 1) return st(Blocks.DEEPSLATE_TILES);
            if (y == ATTIC) return st(Blocks.DARK_OAK_PLANKS);
            return AIR;
        }
        if (!house) return null;
        int s = Math.min(y / STOREY, STOREYS), b = s * STOREY, k = y - b;
        boolean porch = inPorch(x, z);
        // ---- the porch's tower: up to 18, a flat roof and a parapet
        if (porch && y > 14) {
            if (outerWall(x, z) || z == MZ1 + 1 && ax <= PX) {
                if (y <= 17) return this.wall(x, y, z);
                return y == 18 && (x + z & 1) == 0 ? st(Blocks.POLISHED_DEEPSLATE) : null;
            }
            if (y == ATTIC) return st(Blocks.DEEPSLATE_TILES);
            return y <= 18 ? AIR : null;
        }
        if (y > 14) return null;
        // ---- floors
        if (k == 0) {
            if (outerWall(x, z)) return st(Blocks.POLISHED_DEEPSLATE);
            if (s == 0 && (ax <= HALL_X && z > MZ0 && z < MZ1 || porch)) {
                return (x + z & 1) == 0 ? st(Blocks.POLISHED_DEEPSLATE) : st(ModBlocks.POLISHED_MARBLE.get());
            }
            // over the two great halls the first floor is only their galleries
            if (s == 1 && !gallery(x, z)) return AIR;
            if (s == 1 && (inGreatHall(x, z) || inDiningHall(x, z))) return st(ModBlocks.POLISHED_MARBLE.get());
            return st(Blocks.DARK_OAK_PLANKS);
        }
        // ---- the outer walls, their windows and doors
        if (outerWall(x, z)) {
            boolean alongX = !inHouse(x, z + 1) || !inHouse(x, z - 1);
            int coord = alongX ? x : z;
            // the front door in the porch, the fireplaces
            if (porch && z == PZ1 && (x == -1 || x == 0) && k <= 2 && s <= 1) {
                return Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                        .setValue(DoorBlock.HALF, k == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER)
                        // (each leaf hinged on its outer side, so the pair open back against the jambs, not into the doorway)
                        .setValue(DoorBlock.HINGE, x == -1 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
            }
            if (porch && z == PZ1 && (x == -1 || x == 0) && (k == 3 && s <= 1 || s == 2 && k >= 1 && k <= 3)) return pane(Direction.Axis.X);
            // (no back door: the staircase's landing stands against the back wall there)
            BlockState hearth = this.fireplace(x, z, s, k);
            if (hearth != null) return hearth;
            if (k >= 2 && k <= 4 && !this.nearStack(x, z) && this.windowColumn(x, z, alongX, coord)) {
                return pane(alongX ? Direction.Axis.X : Direction.Axis.Z);
            }
            if (k == 0 || y == 14) return st(Blocks.POLISHED_DEEPSLATE);
            return this.wall(x, y, z);
        }
        // ---- the partitions, panelled
        if (partition(x, z)) {
            if (doorway(x, z) && k <= 3) return AIR;
            return k == 4 ? log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.X) : st(Blocks.DARK_OAK_PLANKS);
        }
        return AIR;
    }

    static int nearestStack(int x) {
        int best = STACKS[0];
        for (int c : STACKS) if (Math.abs(x - c) < Math.abs(x - best)) best = c;
        return best;
    }

    // no windows right by a chimney stack
    boolean nearStack(int x, int z) {
        if (z == MZ0) for (int c : STACKS) if (Math.abs(x - c) <= 3) return true;
        return Math.abs(x) == MX && z >= 14 && z <= 20;
    }

    // the windows: pairs of lights between stone piers, not at the corners
    boolean windowColumn(int x, int z, boolean alongX, int coord) {
        int m = Math.floorMod(coord, 4);
        if (m != 1 && m != 2) return false;
        int dx = alongX ? 1 : 0, dz = alongX ? 0 : 1;
        return outerWall(x + dx, z + dz) && outerWall(x - dx, z - dz)
                && outerWall(x + 2 * dx, z + 2 * dz) && outerWall(x - 2 * dx, z - 2 * dz);
    }

    /** The fireplace openings in the walls before the flues (the ground floor's and the first floor's). */
    BlockState fireplace(int x, int z, int s, int k) {
        if (s > 1) return null;
        int open = s == 0 ? -1 : 1;
        if (z == MZ0) {
            int c = nearestStack(x);
            if (Math.abs(x - c) > 2) return null;
            int d = x - c - open;
            if (Math.abs(d) <= 1 && k <= 2) return AIR;
            if (Math.abs(d) <= 1 && k == 3) return st(Blocks.POLISHED_BLACKSTONE_BRICKS);
            return null;
        }
        if (Math.abs(x) == MX && z >= 15 && z <= 19) {
            int d = z - 17 - open;
            if (Math.abs(d) <= 1 && k <= 2) return AIR;
            if (Math.abs(d) <= 1 && k == 3) return st(Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
        return null;
    }

    BlockState wall(int x, int y, int z) {
        int r = this.roll(x, y, z, 14);
        return r == 0 ? st(Blocks.CRACKED_DEEPSLATE_BRICKS) : st(Blocks.DEEPSLATE_BRICKS);
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        REACH.set(box.inflatedBy(15, 0, 15));
        this.shapeGround(level, box);
        LooseTreeCleanup.clean(level, chunkPos, this.origin.getY() - 8, this.origin.getY() + HEIGHT,
                (wx, wz) -> {
                    int[] l = this.local(wx, wz);
                    return closeDistance(l[0], l[1]) < MARGIN + 8;
                },
                (wx, wz) -> {
                    int[] l = this.local(wx, wz);
                    return closeDistance(l[0], l[1]) <= 0;
                });
        this.grounds(level, box);
        this.buildHouse(level, box);
        this.furnish(level, box);
        this.garrison(level, box);
    }

    /** How far outside the grounds (the lawns about the house, and the long walk): 0 inside. */
    static double closeDistance(int x, int z) {
        return Math.min(rect(x, z, CLOSE_X, CLOSE_Z0, CLOSE_Z1), rect(x, z, WALK_X, CLOSE_Z0, WALK_Z1));
    }

    static double rect(int x, int z, int halfX, int z0, int z1) {
        double dx = Math.max(Math.abs(x) - halfX, 0), dz = Math.max(Math.max(z0 - z, z - z1), 0);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** How far inside the grounds' edge (0 on the edge itself, negative outside). */
    static int inset(int x, int z) {
        int a = Math.min(CLOSE_X - Math.abs(x), Math.min(z - CLOSE_Z0, CLOSE_Z1 - z));
        int b = Math.min(WALK_X - Math.abs(x), Math.min(z - CLOSE_Z0, WALK_Z1 - z));
        return Math.max(a, b);
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** The lawn: levelled, cleared of everything growing on it, blended back into the land round it. */
    void shapeGround(WorldGenLevel level, BoundingBox box) {
        int f = this.origin.getY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int[] l = this.local(wx, wz);
                double dist = closeDistance(l[0], l[1]);
                if (dist >= MARGIN) continue;
                int ground = f - 20;
                for (int y = f + 30; y > f - 20; y--) {
                    if (isGround(level.getBlockState(pos.set(wx, y, wz)))) {
                        ground = y;
                        break;
                    }
                }
                double t = dist / MARGIN;
                t = t * t * (3 - 2 * t);
                int target = dist <= 0 ? f : (int) Math.round(f + (ground - f) * t);
                int clearTo = dist <= 0 ? f + HEIGHT : Math.max(ground + 2, target + 1);
                for (int y = target + 1; y <= clearTo && y <= box.maxY(); y++) {
                    pos.set(wx, y, wz);
                    BlockState s = level.getBlockState(pos);
                    if (!s.isAir() && (dist <= 0 || y <= ground + 1 || !s.blocksMotion() || isGround(s))) level.setBlock(pos, AIR, 2);
                }
                for (int y = ground + 1; y < target; y++) level.setBlock(pos.set(wx, y, wz), st(y < target - 3 ? Blocks.STONE : Blocks.DIRT), 2);
                level.setBlock(pos.set(wx, target, wz), st(Blocks.GRASS_BLOCK), 2);
                if (target - 1 > ground) level.setBlock(pos.set(wx, target - 1, wz), st(Blocks.DIRT), 2);
            }
        }
    }

    // ---------------------------------------------------------------- the grounds

    static boolean drive(int x, int z) {
        return Math.abs(x) <= 2 && z > WZ1 + 2 && z <= WALK_Z1 && !circus(x, z);
    }

    static boolean forecourt(int x, int z) {
        return Math.abs(x) < WX0 && z > PZ1 && z <= WZ1 + 2 || Math.abs(x) <= MX + 3 && z > WZ1 && z <= WZ1 + 2;
    }

    // the round at the far end of the long walk, where the drive goes round the obelisk
    static final int CIRCUS_Z = WALK_Z1 - 14;

    static boolean circus(int x, int z) {
        return Mth.length(x, z - CIRCUS_Z) <= 7.5;
    }

    /**
     * The grounds: the lawn in broad mowing stripes (light and dark bands of grass and moss) all round the house
     * and down the long walk - a straight lawn running a hundred blocks out from the front door, the drive down its
     * middle and a single avenue of apple trees each side, to a round with an obelisk at the far end and the gate
     * beyond - all hedged in round its edge; the gravel forecourt with its fountain between the wings.
     */
    void grounds(WorldGenLevel level, BoundingBox box) {
        for (int x = -CLOSE_X; x <= CLOSE_X; x++) {
            for (int z = CLOSE_Z0; z <= WALK_Z1; z++) {
                int in = inset(x, z);
                if (in < 0 || !box.isInside(this.at(x, 0, z))) continue;
                boolean edge = in == 2, inside = in > 2;
                double c = Mth.length(x, z - CIRCUS_Z);
                if (drive(x, z) || c >= 5.5 && c <= 7.5) {
                    this.set(level, box, x, 0, z, st(Blocks.GRAVEL));
                    this.set(level, box, x, -1, z, st(Blocks.COBBLESTONE));
                } else if (Math.abs(x) == 3 && z > WZ1 + 2 && z <= WALK_Z1 && !circus(x, z) || c > 7.5 && c <= 8.5 && in > 0) {
                    this.set(level, box, x, 0, z, st(Blocks.STONE_BRICKS));
                } else if (forecourt(x, z) && !inHouse(x, z)) {
                    this.set(level, box, x, 0, z, st(Blocks.GRAVEL));
                    this.set(level, box, x, -1, z, st(Blocks.COBBLESTONE));
                } else if (inside || edge) {
                    // the stripes, five blocks wide, running the length of the lawn
                    this.set(level, box, x, 0, z, Math.floorMod(Math.floorDiv(x + 2, 5), 2) == 0 ? st(Blocks.GRASS_BLOCK) : st(Blocks.MOSS_BLOCK));
                }
                if (edge && !(Math.abs(x) <= 3 && z >= WALK_Z1 - 2)) {
                    this.set(level, box, x, 1, z, Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                }
            }
        }
        // the fountain in the forecourt
        int fz = 17;
        for (int x = -3; x <= 3; x++) {
            for (int z = fz - 3; z <= fz + 3; z++) {
                int m = Math.max(Math.abs(x), Math.abs(z - fz));
                if (m == 3) this.set(level, box, x, 1, z, slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
                if (m == 3) this.set(level, box, x, 0, z, st(Blocks.STONE_BRICKS));
                if (m < 3) {
                    this.set(level, box, x, 0, z, st(Blocks.WATER));
                    this.set(level, box, x, -1, z, st(Blocks.STONE_BRICKS));
                    this.set(level, box, x, 1, z, AIR);
                }
            }
        }
        this.set(level, box, 0, 0, fz, st(Blocks.STONE_BRICKS));
        this.set(level, box, 0, 1, fz, st(Blocks.CHISELED_STONE_BRICKS));
        this.set(level, box, 0, 2, fz, st(Blocks.STONE_BRICK_WALL));
        this.set(level, box, 0, 3, fz, st(Blocks.LANTERN));
        // the obelisk in the round: a stepped plinth, a tall tapering shaft, a lantern atop
        for (int x = -2; x <= 2; x++) {
            for (int z = CIRCUS_Z - 2; z <= CIRCUS_Z + 2; z++) {
                this.set(level, box, x, 1, z, st(Blocks.STONE_BRICKS));
                if (Math.abs(x) <= 1 && Math.abs(z - CIRCUS_Z) <= 1) this.set(level, box, x, 2, z, st(Blocks.SMOOTH_STONE));
            }
        }
        for (int y = 3; y <= 9; y++) this.set(level, box, 0, y, CIRCUS_Z, y == 3 ? st(Blocks.CHISELED_STONE_BRICKS) : st(Blocks.QUARTZ_PILLAR));
        this.set(level, box, 0, 10, CIRCUS_Z, st(Blocks.QUARTZ_SLAB));
        for (Direction d : Direction.Plane.HORIZONTAL) this.set(level, box, d.getStepX(), 2, CIRCUS_Z + d.getStepZ(), st(Blocks.LANTERN));
        // the lamp posts down the drive, the gate's piers
        for (int z = WZ1 + 6; z < WALK_Z1 - 3; z += 10) {
            if (Math.abs(z - CIRCUS_Z) <= 9) continue;
            for (int side = -1; side <= 1; side += 2) this.lamp(level, box, side * 4, z);
        }
        for (int side = -1; side <= 1; side += 2) {
            for (int y = 1; y <= 3; y++) this.set(level, box, side * 4, y, WALK_Z1 - 2, st(Blocks.STONE_BRICKS));
            this.set(level, box, side * 4, 4, WALK_Z1 - 2, st(Blocks.CHISELED_STONE_BRICKS));
            this.set(level, box, side * 4, 5, WALK_Z1 - 2, st(Blocks.LANTERN));
        }
        // the avenue of apple trees each side of the long walk, ten apart, well clear of the drive
        for (int z = WZ1 + 14; z <= CIRCUS_Z - 10; z += 10) {
            for (int side = -1; side <= 1; side += 2) this.appleTree(level, box, side * 15, z);
        }
        // and a lamp at each corner of the house's lawns
        for (int sx = -1; sx <= 1; sx += 2) {
            this.lamp(level, box, sx * (CLOSE_X - 5), CLOSE_Z0 + 5);
            this.lamp(level, box, sx * (CLOSE_X - 5), CLOSE_Z1 - 5);
            this.lamp(level, box, sx * (WALK_X - 5), WALK_Z1 - 5);
        }
    }

    void lamp(WorldGenLevel level, BoundingBox box, int x, int z) {
        for (int y = 1; y <= 3; y++) this.set(level, box, x, y, z, st(Blocks.DARK_OAK_FENCE));
        this.set(level, box, x, 4, z, st(Blocks.LANTERN));
    }

    /** An apple tree, every one alike: a trunk of four, a round crown hung with apples. */
    void appleTree(WorldGenLevel level, BoundingBox box, int x, int z) {
        for (int y = 1; y <= 4; y++) this.set(level, box, x, y, z, log(ModBlocks.APPLE_LOG.get(), Direction.Axis.Y));
        for (int dy = 0; dy <= 3; dy++) {
            int r = dy == 0 || dy == 1 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (r == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    if (dy == 3 && Math.abs(dx) + Math.abs(dz) > 1) continue;
                    if (dx == 0 && dz == 0 && dy < 2) continue;
                    BlockState leaves = ModBlocks.APPLE_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                    if (leaves.hasProperty(FruitLeavesBlock.FRUIT)) leaves = leaves.setValue(FruitLeavesBlock.FRUIT, this.roll(x + dx, dy, z + dz, 3));
                    this.set(level, box, x + dx, 4 + dy, z + dz, leaves);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the house

    void buildHouse(WorldGenLevel level, BoundingBox box) {
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int[] l = this.local(wx, wz);
                int x = l[0], z = l[1];
                if (Math.abs(x) > MX + 3 || z < MZ0 - 3 || z > WZ1 + 2) continue;
                for (int y = 0; y <= HEIGHT; y++) {
                    BlockState s = this.design(x, y, z);
                    if (s == null) continue;
                    BlockPos pos = new BlockPos(wx, this.origin.getY() + y, wz);
                    if (box.isInside(pos)) level.setBlock(pos, s.rotate(ROTATIONS[this.turn]), 2);
                }
                if (inHouse(x, z) || mainStack(x, z) || wingStack(x, z)) {
                    for (int y = -1; y >= -4; y--) {
                        BlockPos pos = new BlockPos(wx, this.origin.getY() + y, wz);
                        if (box.isInside(pos) && !level.getBlockState(pos).blocksMotion()) level.setBlock(pos, st(Blocks.COBBLED_DEEPSLATE), 2);
                    }
                }
            }
        }
    }

    // the rooms, by their interior: x0, z0, x1, z1 - the great dining hall, the great hall, the drawing room, the study
    static final int[][] MAIN_ROOMS = {{DINE_X0, -6, DINE_X1, 5}, {-HALL_X, -6, HALL_X, 5}, {10, -6, 15, 5}, {17, -6, 23, 5}};
    static final int[][] WING_ROOMS = {{-23, 7, -15, 12}, {-23, 14, -15, 19}, {15, 7, 23, 12}, {15, 14, 23, 19}};

    boolean freeAt(WorldGenLevel level, int x, int y, int z) {
        return this.get(level, x, y, z).isAir();
    }

    /** The staircase, the lanterns and chandeliers, the furniture, the chests. */
    void furnish(WorldGenLevel level, BoundingBox box) {
        this.staircase(level, box);
        this.balcony(level, box);
        // the lanterns: hung from every ceiling five apart, so no room has a dark corner; in the attic too
        for (int x = -MX + 1; x <= MX - 1; x++) {
            for (int z = MZ0 + 1; z <= WZ1 - 1; z++) {
                if (!inHouse(x, z) || outerWall(x, z) || partition(x, z)) continue;
                if (Math.floorMod(x + 2, 5) != 0 || Math.floorMod(z + 4, 5) != 0) continue;
                for (int s = 0; s < STOREYS; s++) {
                    int y = s * STOREY + 4;
                    if (this.freeAt(level, x, y, z) && !this.get(level, x, y + 1, z).isAir()) this.set(level, box, x, y, z, LANTERN);
                }
                if (!inPorch(x, z)) this.set(level, box, x, ATTIC + 1, z, st(Blocks.LANTERN));
            }
        }
        // chandeliers over the open middles of the two great halls, from the ceiling two storeys up
        for (int[] c : new int[][]{{-16, -1}, {-16, 2}, {-13, -1}, {-19, -1}, {-13, 2}, {-19, 2}, {0, 1}, {0, -4}, {-5, -5}, {5, -5}, {-8, -3}, {8, -3}}) {
            this.chandelier(level, box, c[0], c[1]);
        }
        // the rooms
        int[][] g = MAIN_ROOMS;
        this.diningHall(level, box, g[0]);
        this.drawingRoom(level, box, g[2], 0);
        this.study(level, box, g[3], 0);
        this.kitchen(level, box, WING_ROOMS[0], 0);
        this.pantry(level, box, WING_ROOMS[1], 0);
        this.smithy(level, box, WING_ROOMS[2], 0);
        this.stillRoom(level, box, WING_ROOMS[3], 0);
        this.bedroom(level, box, g[2], 1, 3);
        this.bedroom(level, box, g[3], 1, 5);
        // the top floor: the long gallery (the library) over the dining hall, the state bedroom over the great hall
        this.library(level, box, g[0], 2);
        this.stateBedroom(level, box, g[1], 2);
        this.bedroom(level, box, g[2], 2, 7);
        this.gaol(level, box, g[3], 2);
        for (int s = 1; s < STOREYS; s++) {
            for (int[] w : WING_ROOMS) this.bedroom(level, box, w, s, w[0] + w[1] + s);
        }
        // a red runner from the front door to the foot of the stairs, and along the galleries; the hall's ferns
        for (int z = -1; z <= PZ1 - 1; z++) {
            for (int x = -1; x <= 1; x++) if (this.freeAt(level, x, 1, z)) this.set(level, box, x, 1, z, st(Blocks.RED_CARPET));
        }
        for (int z = 0; z <= 5; z++) {
            for (int side = -1; side <= 1; side += 2) if (this.freeAt(level, side * 5, STOREY + 1, z)) this.set(level, box, side * 5, STOREY + 1, z, st(Blocks.RED_CARPET));
        }
        for (int x = -5; x <= 5; x++) if (this.freeAt(level, x, STOREY + 1, 4)) this.set(level, box, x, STOREY + 1, 4, st(Blocks.RED_CARPET));
        for (int side = -1; side <= 1; side += 2) {
            this.set(level, box, side * 2, 1, 5, st(Blocks.POTTED_FERN));
            this.set(level, box, side * 8, 1, 4, st(Blocks.POTTED_FERN));
            this.set(level, box, side * 8, STOREY + 1, 5, st(Blocks.POTTED_FERN));
        }
        // banners of the house high on the great hall's walls: behind the landing, and down the sides over the galleries
        for (int x : new int[]{-7, -6, -4, -3, 3, 4, 6, 7}) {
            this.banner(level, box, x, 8, MZ0 + 1, Direction.SOUTH, Math.abs(x) % 2 == 0 ? Blocks.RED_WALL_BANNER : Blocks.BLUE_WALL_BANNER);
        }
        for (int z = 1; z <= 4; z += 3) {
            this.banner(level, box, -HALL_X, 8, z, Direction.EAST, Blocks.BLUE_WALL_BANNER);
            this.banner(level, box, HALL_X, 8, z, Direction.WEST, Blocks.BLUE_WALL_BANNER);
        }
    }

    /** A plain wall banner hung on the wall behind it (if there's wall there, not a window). */
    void banner(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, Block banner) {
        BlockState behind = this.get(level, x - facing.getStepX(), y, z - facing.getStepZ());
        if (!this.freeAt(level, x, y, z) || !this.freeAt(level, x, y - 1, z) || !behind.blocksMotion() || behind.is(Blocks.GLASS_PANE)) return;
        this.set(level, box, x, y, z, banner.defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, facing));
    }

    /** A chandelier: a chain down from the ceiling two storeys up, a ring of lanterns on it. */
    void chandelier(WorldGenLevel level, BoundingBox box, int x, int z) {
        int top = 2 * STOREY - 1;
        if (!this.get(level, x, top + 1, z).blocksMotion()) return;
        for (int y = top; y >= top - 1; y--) {
            if (!this.freeAt(level, x, y, z)) return;
            this.set(level, box, x, y, z, st(Blocks.CHAIN));
        }
        this.set(level, box, x, top - 2, z, LANTERN);
    }

    static BlockState fence(Direction.Axis along) {
        boolean x = along == Direction.Axis.X;
        return Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(CrossCollisionBlock.EAST, x).setValue(CrossCollisionBlock.WEST, x)
                .setValue(CrossCollisionBlock.NORTH, !x).setValue(CrossCollisionBlock.SOUTH, !x);
    }

    static BlockState rail(Block wall, Direction.Axis along) {
        BlockState s = wall.defaultBlockState();
        net.minecraft.world.level.block.state.properties.WallSide low = net.minecraft.world.level.block.state.properties.WallSide.LOW;
        if (along == Direction.Axis.X) return s.setValue(net.minecraft.world.level.block.WallBlock.EAST_WALL, low).setValue(net.minecraft.world.level.block.WallBlock.WEST_WALL, low);
        return s.setValue(net.minecraft.world.level.block.WallBlock.NORTH_WALL, low).setValue(net.minecraft.world.level.block.WallBlock.SOUTH_WALL, low);
    }

    /**
     * The great hall's staircase, of white marble, in the imperial manner: a broad flight five wide up the middle
     * from the entrance to a landing across the back of the hall, then two flights, one each side, turning back
     * towards the front up to the galleries round the hall at the first floor - every flight with a red runner up
     * its middle (crimson steps), marble balustrades along every open edge, marble columns under the galleries. It
     * stands free in the hall: the side flights rise between balustrades with an aisle between them and the side
     * walls, and the landing touches only the back wall.
     */
    void staircase(WorldGenLevel level, BoundingBox box) {
        Block step = ModBlocks.MARBLE_STAIRS.get(), runner = Blocks.CRIMSON_STAIRS;
        BlockState solid = st(ModBlocks.POLISHED_MARBLE.get());
        Block baluster = ModBlocks.POLISHED_MARBLE_WALL.get();
        // the landing across the back, its top at 2 (standing at 3)
        for (int x = -7; x <= 7; x++) {
            for (int z = MZ0 + 1; z <= -4; z++) {
                for (int y = 1; y <= 2; y++) this.set(level, box, x, y, z, solid);
                if (Math.abs(x) <= 6 && this.freeAt(level, x, 3, z)) this.set(level, box, x, 3, z, st(Blocks.RED_CARPET));
            }
        }
        // the middle flight: two steps up to the landing, facing the entrance
        for (int x = -2; x <= 2; x++) {
            for (int k = 1; k <= 2; k++) {
                int z = -1 - k;
                for (int y = 1; y < k; y++) this.set(level, box, x, y, z, solid);
                this.set(level, box, x, k, z, stair(Math.abs(x) <= 1 ? runner : step, Direction.NORTH, false));
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            // the side flights: from the landing's ends, three steps up towards the front, to the galleries; the
            // stringers either side of each carry its balustrades (the inner one rises beside the middle flight)
            for (int k = 1; k <= 3; k++) {
                int z = -4 + k, y = 2 + k;
                for (int ax = 3; ax <= 7; ax++) {
                    for (int yy = 1; yy < y; yy++) this.set(level, box, side * ax, yy, z, solid);
                    boolean stringer = ax == 3 || ax == 7;
                    this.set(level, box, side * ax, y, z, stringer ? solid : stair(ax == 5 ? runner : step, Direction.SOUTH, false));
                    if (stringer) this.set(level, box, side * ax, y + 1, z, rail(baluster, Direction.Axis.Z));
                    else for (int yy = y + 1; yy <= STOREY + 3; yy++) this.set(level, box, side * ax, yy, z, AIR);
                }
            }
            // the landing's ends, open to the aisles; the galleries' balustrades (inner edges, the aisles' ends, the front)
            for (int z = MZ0 + 1; z <= -4; z++) this.set(level, box, side * 7, 3, z, rail(baluster, Direction.Axis.Z));
            for (int z = 0; z <= 2; z++) this.set(level, box, side * 3, STOREY + 1, z, rail(baluster, Direction.Axis.Z));
            this.set(level, box, side * HALL_X, STOREY + 1, 0, rail(baluster, Direction.Axis.X));
            for (int ax = 0; ax <= 3; ax++) this.set(level, box, side * ax, STOREY + 1, 3, rail(baluster, Direction.Axis.X));
            // the columns holding up the galleries' inner corners
            for (int z : new int[]{0, 3}) {
                this.set(level, box, side * 3, 1, z, st(ModBlocks.MARBLE_COLUMN_BASE.get()));
                for (int y = 2; y <= 3; y++) this.set(level, box, side * 3, y, z, log(ModBlocks.FLUTED_MARBLE_COLUMN.get(), Direction.Axis.Y));
                this.set(level, box, side * 3, 4, z, st(ModBlocks.MARBLE_COLUMN_CAPITAL.get()));
            }
            // newel lanterns at the foot of the middle flight
            this.set(level, box, side * 3, 1, -1, solid);
            this.set(level, box, side * 3, 2, -1, st(Blocks.LANTERN));
        }
        // up to the top floor: a plain flight along the back of the porch's first-floor room
        for (int k = 1; k <= 5; k++) {
            int x = -3 + k, y = STOREY + k;
            for (int z = MZ1 + 1; z <= MZ1 + 2; z++) {
                for (int yy = STOREY + 1; yy < y; yy++) this.set(level, box, x, yy, z, st(Blocks.DARK_OAK_PLANKS));
                this.set(level, box, x, y, z, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
                if (k < 5) for (int yy = y + 1; yy <= 2 * STOREY + 1; yy++) this.set(level, box, x, yy, z, AIR);
            }
        }
        // the stairwell's rail on the top floor
        for (int x = -2; x <= 1; x++) {
            this.set(level, box, x, 2 * STOREY + 1, MZ1 + 3, st(Blocks.DARK_OAK_FENCE));
            this.set(level, box, x, 2 * STOREY + 1, MZ1, st(Blocks.DARK_OAK_FENCE));
        }
        for (int z = MZ1 + 1; z <= MZ1 + 2; z++) this.set(level, box, -3, 2 * STOREY + 1, z, st(Blocks.DARK_OAK_FENCE));
    }

    /** The balcony over the front door, off the porch's first-floor room. */
    void balcony(WorldGenLevel level, BoundingBox box) {
        for (int x = -3; x <= 2; x++) {
            for (int z = PZ1 + 1; z <= PZ1 + 2; z++) {
                this.set(level, box, x, STOREY, z, st(Blocks.POLISHED_DEEPSLATE));
                boolean edge = x == -3 || x == 2 || z == PZ1 + 2;
                this.set(level, box, x, STOREY + 1, z, edge ? rail(Blocks.POLISHED_DEEPSLATE_WALL, z == PZ1 + 2 ? Direction.Axis.X : Direction.Axis.Z) : AIR);
            }
            // brackets under it
            if (x == -3 || x == 2) this.set(level, box, x, STOREY - 1, PZ1 + 1, stair(Blocks.POLISHED_DEEPSLATE_STAIRS, Direction.NORTH, true));
        }
        this.set(level, box, -1, STOREY - 1, PZ1 + 1, LANTERN);
        this.set(level, box, 0, STOREY - 1, PZ1 + 1, LANTERN);
        this.set(level, box, -3, STOREY + 2, PZ1 + 2, st(Blocks.LANTERN));
        this.set(level, box, 2, STOREY + 2, PZ1 + 2, st(Blocks.LANTERN));
    }

    /**
     * The great dining hall: two storeys high, a gallery all round at the first floor (its fireplaces open onto it);
     * a long table down the middle with high-backed chairs, candles all along it, a carver at each end; sideboards,
     * banners, a dresser of plate.
     */
    void diningHall(WorldGenLevel level, BoundingBox box, int[] r) {
        int cz = 0;
        for (int x = r[0] + 3; x <= r[2] - 3; x++) {
            for (int z = cz; z <= cz + 1; z++) {
                this.set(level, box, x, 1, z, st(Blocks.DARK_OAK_PLANKS));
                this.set(level, box, x, 2, z, Math.floorMod(x, 3) == 0 && z == cz ? Blocks.CANDLE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true).setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3)
                        : st(Blocks.WHITE_CARPET));
            }
            if (Math.floorMod(x, 2) == 0) {
                this.set(level, box, x, 1, cz - 1, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
                this.set(level, box, x, 1, cz + 2, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
            }
        }
        // the carvers at the ends
        this.set(level, box, r[0] + 2, 1, cz, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
        this.set(level, box, r[2] - 2, 1, cz + 1, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
        // sideboards along the front wall under the gallery, plate on them
        for (int x = r[0] + 3; x <= r[2] - 3; x += 4) {
            this.set(level, box, x, 1, r[3], st(Blocks.BARREL));
            this.set(level, box, x + 1, 1, r[3], stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, true));
            this.set(level, box, x + 1, 2, r[3], st(Blocks.FLOWER_POT));
        }
        // banners high on the walls over the galleries, red and gold
        for (int x = r[0] + 1; x <= r[2] - 1; x += 2) {
            this.banner(level, box, x, 8, MZ1 - 1, Direction.NORTH, Math.floorMod(x, 4) == 1 ? Blocks.RED_WALL_BANNER : Blocks.YELLOW_WALL_BANNER);
            this.banner(level, box, x, 8, MZ0 + 1, Direction.SOUTH, Math.floorMod(x, 4) == 1 ? Blocks.YELLOW_WALL_BANNER : Blocks.RED_WALL_BANNER);
        }
        // the gallery's balustrade, round its inner edge
        for (int x = r[0] + 1; x <= r[2] - 1; x++) {
            this.set(level, box, x, STOREY + 1, MZ0 + 2, fence(Direction.Axis.X));
            this.set(level, box, x, STOREY + 1, MZ1 - 2, fence(Direction.Axis.X));
        }
        for (int z = MZ0 + 2; z <= MZ1 - 2; z++) {
            this.set(level, box, r[0] + 1, STOREY + 1, z, fence(Direction.Axis.Z));
            this.set(level, box, r[2] - 1, STOREY + 1, z, fence(Direction.Axis.Z));
        }
        this.chest(level, box, r[0], 1, r[1] + 1, Direction.EAST);
    }

    void stateBedroom(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1;
        // a great bed under a canopy of four posts, a rug, a pair of chests
        for (int x = -1; x <= 0; x++) {
            this.set(level, box, x, y, r[1], Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
            this.set(level, box, x, y, r[1] + 1, Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        }
        for (int x : new int[]{-2, 1}) {
            for (int z : new int[]{r[1], r[1] + 2}) {
                for (int k = 0; k < 3; k++) this.set(level, box, x, y + k, z, st(Blocks.DARK_OAK_FENCE));
            }
        }
        for (int x = -2; x <= 1; x++) for (int z = r[1]; z <= r[1] + 2; z++) this.set(level, box, x, y + 3, z, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        for (int x = -3; x <= 2; x++) for (int z = r[1] + 3; z <= r[1] + 6; z++) if (this.freeAt(level, x, y, z)) this.set(level, box, x, y, z, st(Blocks.PURPLE_CARPET));
        this.chest(level, box, -4, y, r[1], Direction.SOUTH);
        this.chest(level, box, 3, y, r[1], Direction.SOUTH);
        this.set(level, box, -6, y, r[3], st(Blocks.BOOKSHELF));
        this.set(level, box, 6, y, r[3], st(Blocks.BOOKSHELF));
    }

    void library(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1;
        // shelves along the side walls, and in rows across the room
        for (int z = r[1]; z <= r[3]; z++) {
            if (z == 0 || z == 1) continue;
            for (int k = 0; k < 3; k++) this.set(level, box, r[2], y + k, z, st(Blocks.BOOKSHELF));
        }
        for (int x = r[0] + 2; x <= r[2] - 3; x += 3) {
            for (int z = r[1] + 4; z <= r[3] - 1; z++) {
                for (int k = 0; k < 2; k++) this.set(level, box, x, y + k, z, st(Blocks.BOOKSHELF));
            }
        }
        this.set(level, box, r[0] + 1, y, r[3], st(Blocks.LECTERN));
        this.chest(level, box, r[0], y, r[3], Direction.NORTH);
    }

    void drawingRoom(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1;
        for (int x = r[0] + 2; x <= r[2] - 2; x++) {
            for (int z = r[1] + 3; z <= r[3] - 2; z++) this.set(level, box, x, y, z, st(Blocks.GREEN_CARPET));
        }
        int cx = (r[0] + r[2]) / 2, cz = (r[1] + r[3]) / 2;
        this.set(level, box, cx - 2, y, cz, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
        this.set(level, box, cx + 2, y, cz, stair(Blocks.DARK_OAK_STAIRS, Direction.EAST, false));
        this.set(level, box, cx, y, cz, st(Blocks.DARK_OAK_FENCE));
        this.set(level, box, cx, y + 1, cz, st(Blocks.POTTED_RED_TULIP));
        this.set(level, box, r[2], y, r[3], st(Blocks.JUKEBOX));
    }

    void study(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1, cx = (r[0] + r[2]) / 2, cz = (r[1] + r[3]) / 2 + 2;
        // an enchanting table, ringed by fifteen bookshelves two blocks off
        this.set(level, box, cx, y, cz, st(Blocks.ENCHANTING_TABLE));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != 2 || dz == 2 && Math.abs(dx) <= 1) continue;
                this.set(level, box, cx + dx, y, cz + dz, st(Blocks.BOOKSHELF));
                if (Math.abs(dx) == 2 || dz == -2) this.set(level, box, cx + dx, y + 1, cz + dz, st(Blocks.BOOKSHELF));
            }
        }
        this.chest(level, box, r[2], y, r[3], Direction.NORTH);
    }

    void kitchen(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1;
        Block[] row = {Blocks.FURNACE, Blocks.SMOKER, Blocks.CRAFTING_TABLE, Blocks.CAULDRON, Blocks.BARREL, Blocks.BARREL};
        for (int i = 0; i < row.length && r[0] + 1 + i <= r[2]; i++) {
            BlockState st = st(row[i]);
            if (st.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
                st = st.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, Direction.SOUTH);
            }
            this.set(level, box, r[0] + 1 + i, y, r[1], st);
        }
        for (int x = r[0] + 3; x <= r[2] - 2; x++) this.set(level, box, x, y, r[1] + 3, st(Blocks.SPRUCE_PLANKS));
    }

    void pantry(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1;
        for (int x = r[0]; x <= r[0] + 3; x++) this.set(level, box, x, y, r[3], st(Blocks.BARREL));
        this.chest(level, box, r[0] + 5, y, r[3], Direction.NORTH);
        this.set(level, box, r[0], y, r[1] + 1, st(Blocks.HAY_BLOCK));
    }

    void smithy(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1;
        this.set(level, box, r[0] + 1, y, r[1], st(Blocks.ANVIL));
        this.set(level, box, r[0] + 3, y, r[1], st(Blocks.SMITHING_TABLE));
        this.set(level, box, r[0] + 5, y, r[1], st(Blocks.GRINDSTONE));
        this.set(level, box, r[0] + 7, y, r[1], st(Blocks.BLAST_FURNACE));
    }

    void stillRoom(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1;
        this.set(level, box, r[0] + 1, y, r[3], st(Blocks.BREWING_STAND));
        this.set(level, box, r[0] + 3, y, r[3], st(Blocks.CAULDRON));
        this.set(level, box, r[0] + 5, y, r[3], st(Blocks.CARTOGRAPHY_TABLE));
        this.chest(level, box, r[0] + 7, y, r[3], Direction.NORTH);
    }

    void bedroom(WorldGenLevel level, BoundingBox box, int[] r, int s, int salt) {
        int y = s * STOREY + 1;
        // the bed against the back wall of the room, its head at the wall; a rug, a chest at its foot
        int bx = r[0] + 2 + Math.floorMod(salt, 3), bz = r[1];
        Block[] colours = {Blocks.RED_BED, Blocks.GREEN_BED, Blocks.BLUE_BED, Blocks.WHITE_BED, Blocks.BROWN_BED};
        Block bed = colours[Math.floorMod(salt, colours.length)];
        if (!this.freeAt(level, bx, y, bz) || !this.freeAt(level, bx, y, bz + 1)) return;
        this.set(level, box, bx, y, bz, bed.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
        this.set(level, box, bx, y, bz + 1, bed.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
        for (int x = bx - 1; x <= bx + 1; x++) {
            for (int z = bz + 2; z <= bz + 4; z++) if (this.freeAt(level, x, y, z)) this.set(level, box, x, y, z, st(Blocks.RED_CARPET));
        }
        if (this.roll(r[0], y, r[1], 3) == 0) this.chest(level, box, bx + 2, y, bz, Direction.SOUTH);
        else this.set(level, box, bx + 2, y, bz, st(Blocks.BARREL));
    }

    /** The gaol, where the illagers keep their allays: cells behind iron bars. */
    void gaol(WorldGenLevel level, BoundingBox box, int[] r, int s) {
        int y = s * STOREY + 1;
        for (int c = 0; c < 2; c++) {
            int x0 = r[0] + c * 4, z0 = r[1];
            for (int x = x0; x <= x0 + 3; x++) {
                for (int z = z0; z <= z0 + 3; z++) {
                    boolean bars = x == x0 + 3 || z == z0 + 3;
                    for (int k = 0; k < 4; k++) if (bars && this.freeAt(level, x, y + k, z)) this.set(level, box, x, y + k, z, st(Blocks.IRON_BARS));
                }
            }
        }
    }

    void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).rotate(ROTATIONS[this.turn]), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity entity) entity.setLootTable(MANSION_LOOT, this.seed ^ pos.asLong());
    }

    // ---------------------------------------------------------------- its garrison

    /** Vindicators in the rooms, evokers (and their vexes) in the great ones. */
    void garrison(WorldGenLevel level, BoundingBox box) {
        // the allays in the gaol's cells
        for (int c = 0; c < 2; c++) this.mob(level, box, EntityType.ALLAY, MAIN_ROOMS[3][0] + c * 4 + 1, 2 * STOREY + 1, MAIN_ROOMS[3][1] + 1);
        for (int s = 0; s < STOREYS; s++) {
            int y = s * STOREY + 1;
            for (int i = 0; i < MAIN_ROOMS.length; i++) {
                int[] r = MAIN_ROOMS[i];
                int cx = (r[0] + r[2]) / 2, cz = (r[1] + r[3]) / 2;
                this.mob(level, box, EntityType.VINDICATOR, cx - 1, y, cz + 2);
                if ((i + s) % 2 == 0) this.mob(level, box, EntityType.VINDICATOR, cx + 1, y, cz - 2);
            }
            for (int[] w : WING_ROOMS) {
                if ((w[1] + s) % 2 == 0) this.mob(level, box, EntityType.VINDICATOR, (w[0] + w[2]) / 2, y, (w[1] + w[3]) / 2);
            }
        }
        // on the galleries
        for (int[] g : new int[][]{{-22, 0}, {-10, -2}, {-16, 5}, {-6, 2}, {6, 2}, {0, 5}}) this.mob(level, box, EntityType.VINDICATOR, g[0], STOREY + 1, g[1]);
        this.mob(level, box, EntityType.EVOKER, 0, 1, 0);
        this.mob(level, box, EntityType.EVOKER, -9, STOREY + 1, 2);
        this.mob(level, box, EntityType.EVOKER, 9, 2 * STOREY + 1, 2);
        this.mob(level, box, EntityType.EVOKER, 19, 2 * STOREY + 1, 16);
        this.mob(level, box, EntityType.VEX, -9, 2 * STOREY + 2, 0);
        this.mob(level, box, EntityType.VEX, 9, STOREY + 2, 0);
    }

    void mob(WorldGenLevel level, BoundingBox box, EntityType<?> type, int x, int y, int z) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        if (level.getBlockState(pos).blocksMotion() || level.getBlockState(pos.above()).blocksMotion()) return;
        // (nothing that walks is left in mid-air over the great halls)
        if (type != EntityType.VEX && type != EntityType.ALLAY && level.getBlockState(pos.below()).isAir()) return;
        if (!(type.create(level.getLevel()) instanceof Mob mob)) return;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
