package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.RoofTileBlock;
import net.krisplis.koppenclimate.block.custom.WinePoleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A Tuscan hill town of the Mediterranean (Csa), built on ground levelled for it.
 *
 * <ul>
 *   <li>The town is a square some 60 blocks across, packed with tall limestone houses two and three storeys high,
 *       each with its own pitched roof of terracotta roof tiles, windows closed by a pair of shutters.</li>
 *   <li>In the middle is the piazza, a market square paved in brick with travertine lines, with a small fountain
 *       at its heart, four market stalls and potted nerium.</li>
 *   <li>The church stands on the piazza's north side: a striped facade with a rose window, a long nave of pews
 *       and an altar, and beside it the campanile, its bell hanging in the open belfry at the top under a tiled
 *       pyramid roof.</li>
 *   <li>The streets between the houses are narrow, two blocks wide, deep between the tall houses.</li>
 *   <li>Nerium hedges ring the town outside.</li>
 *   <li>Outside the town lie its fields: a vineyard of seven rows of red wine on poles, a tomato field grown the
 *       same way, and fields of wheat and barley. Donkeys, cows and sheep roam around them.</li>
 *   <li>Dozens of cats live in the streets, and there's a Venetian villager in every house.</li>
 * </ul>
 */
public class TuscanVillagePiece extends StructurePiece {
    // the town square, the reach of the fields, and the slope back to the land around
    static final int CORE = 30;
    static final int MARGIN = 10;
    static final int BOX = 60;
    static final int CLEAR = 40;
    static final int CATS = 30;
    // how many houses are homes, each with a bed and a villager
    static final int HOMES = 20;

    // what each cell of the town is
    static final int FREE = 0, PIAZZA = 1, LANE = 2, CHURCH = 3, TOWER = 4, HOUSE = 5, GARDEN = 6;
    static final int N = 2 * CORE + 1;

    // the church's nave and the campanile
    static final int CH_X0 = -4, CH_X1 = 4, CH_Z0 = -25, CH_Z1 = -8;
    static final int TW_X0 = 5, TW_X1 = 9, TW_Z0 = -13, TW_Z1 = -9;

    // the fields: x0, z0, x1, z1 (inclusive) and what grows there
    static final int VINEYARD = 0, TOMATOES = 1, WHEAT = 2, BARLEY = 3;
    static final int[][] FIELDS = {
            {-8, -46, 8, -34, VINEYARD},
            {34, -8, 44, 8, TOMATOES},
            {-8, 34, 8, 42, WHEAT},
            {-42, -8, -34, 8, BARLEY}};
    // a grass apron around each field, where the animals graze
    static final int APRON = 3;

    static final Block[] WORKSTATIONS = {
            Blocks.COMPOSTER, Blocks.COMPOSTER, Blocks.LECTERN, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CARTOGRAPHY_TABLE,
            Blocks.BARREL, Blocks.FLETCHING_TABLE, Blocks.CAULDRON, Blocks.STONECUTTER, Blocks.LOOM, Blocks.SMITHING_TABLE,
            Blocks.GRINDSTONE};

    final BlockPos centre;
    final long seed;
    transient Layout layout;

    public TuscanVillagePiece(BlockPos centre, long seed) {
        super(VillageRegistry.TUSCAN_VILLAGE_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX, centre.getY() - 30, centre.getZ() - BOX,
                centre.getX() + BOX, centre.getY() + CLEAR, centre.getZ() + BOX));
        this.centre = centre;
        this.seed = seed;
    }

    /** For other towns built the same way (CroatianVillagePiece). */
    protected TuscanVillagePiece(StructurePieceType type, BlockPos centre, long seed, BoundingBox box) {
        super(type, 0, box);
        this.centre = centre;
        this.seed = seed;
    }

    protected TuscanVillagePiece(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.seed = tag.getLong("Layout");
    }

    public TuscanVillagePiece(CompoundTag tag) {
        super(VillageRegistry.TUSCAN_VILLAGE_PIECE.get(), tag);
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

    // ================================================================ layout

    /** A house: its corner and size, storeys, wall stone, shutters, door, and what it holds (null: just a home). */
    record House(int x0, int z0, int w, int d, int floors, BlockState wall, BlockState trim, Block shutter,
                 Direction door, int doorAt, Block workstation, boolean home) {
        int x1() {
            return this.x0 + this.w - 1;
        }

        int z1() {
            return this.z0 + this.d - 1;
        }
    }

    static final class Layout {
        final int[][] cells = new int[N][N];
        final List<House> houses = new ArrayList<>();

        int at(int x, int z) {
            if (x < -CORE || x > CORE || z < -CORE || z > CORE) return -1;
            return this.cells[x + CORE][z + CORE];
        }

        void put(int x, int z, int what) {
            if (x >= -CORE && x <= CORE && z >= -CORE && z <= CORE) this.cells[x + CORE][z + CORE] = what;
        }

        boolean walkable(int x, int z) {
            int c = this.at(x, z);
            return c == PIAZZA || c == LANE || c == GARDEN || c == -1;
        }
    }

    Layout layout() {
        if (this.layout == null) this.layout = makeLayout(this.seed);
        return this.layout;
    }

    static Layout makeLayout(long seed) {
        RandomSource random = RandomSource.create(seed);
        Layout l = new Layout();
        for (int x = -CORE; x <= CORE; x++) {
            for (int z = -CORE; z <= CORE; z++) {
                int c = FREE;
                if (Math.abs(x) <= 7 && Math.abs(z) <= 7) c = PIAZZA;
                else if (x >= CH_X0 && x <= CH_X1 && z >= CH_Z0 && z <= CH_Z1) c = CHURCH;
                else if (x >= TW_X0 && x <= TW_X1 && z >= TW_Z0 && z <= TW_Z1) c = TOWER;
                else if (isLane(x, z)) c = LANE;
                l.put(x, z, c);
            }
        }
        // pack the rest with houses, row by row
        List<Block> uses = new ArrayList<>(List.of(WORKSTATIONS));
        for (int z = -CORE; z <= CORE; z++) {
            for (int x = -CORE; x <= CORE; x++) {
                if (l.at(x, z) != FREE) continue;
                int w = 5 + random.nextInt(3), d = 5 + random.nextInt(4);
                int run = 0;
                while (run < w && l.at(x + run, z) == FREE) run++;
                w = run;
                int depth = d;
                for (int i = 0; i < w; i++) {
                    int k = 0;
                    while (k < depth && l.at(x + i, z + k) == FREE) k++;
                    depth = k;
                }
                d = depth;
                // don't leave a sliver too thin for a house behind it
                if (w >= 4 && d >= 4) {
                    int restX = 0;
                    while (l.at(x + w + restX, z) == FREE && restX < 4) restX++;
                    if (restX > 0 && restX < 4 && w + restX <= 9) w += restX;
                    int restZ = 0;
                    while (l.at(x, z + d + restZ) == FREE && restZ < 4) restZ++;
                    if (restZ > 0 && restZ < 4 && d + restZ <= 10) {
                        boolean ok = true;
                        for (int i = 0; i < w && ok; i++) for (int k = 0; k < d + restZ; k++) if (l.at(x + i, z + k) != FREE) ok = false;
                        if (ok) d += restZ;
                    }
                }
                int mark = w >= 4 && d >= 4 ? HOUSE : GARDEN;
                for (int i = 0; i < Math.max(1, w); i++) {
                    for (int k = 0; k < Math.max(1, d); k++) {
                        if (l.at(x + i, z + k) == FREE) l.put(x + i, z + k, mark);
                    }
                }
                if (mark == HOUSE) l.houses.add(newHouse(l, random, x, z, w, d));
            }
        }
        // of the houses with a door, a score are homes with a bed and a villager; the workstations go among them
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < l.houses.size(); i++) if (l.houses.get(i).door() != null) open.add(i);
        for (int i = open.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Integer t = open.get(i);
            open.set(i, open.get(j));
            open.set(j, t);
        }
        for (int k = 0; k < open.size() && k < HOMES; k++) {
            House h = l.houses.get(open.get(k));
            Block work = k < uses.size() ? uses.get(k) : null;
            l.houses.set(open.get(k), new House(h.x0(), h.z0(), h.w(), h.d(), h.floors(), h.wall(), h.trim(), h.shutter(),
                    h.door(), h.doorAt(), work, true));
        }
        return l;
    }

    static boolean isLane(int x, int z) {
        // the main streets out of the piazza, east, west and south
        if ((z == -1 || z == 0) && Math.abs(x) >= 8) return true;
        if ((x == -1 || x == 0) && z >= 8) return true;
        // the lanes either side of the church and campanile
        if ((x == -6 || x == -5 || x == 11 || x == 12) && z <= -8) return true;
        // the ring of lanes around the middle of town
        if ((Math.abs(x) == 18 || Math.abs(x) == 19)) return true;
        return Math.abs(z) == 18 || Math.abs(z) == 19;
    }

    static House newHouse(Layout l, RandomSource random, int x0, int z0, int w, int d) {
        // facing the piazza, a house stands three storeys tall
        boolean onPiazza = x0 - 1 <= 8 && x0 + w >= -8 && z0 - 1 <= 8 && z0 + d >= -8;
        int floors = onPiazza ? 3 : 2 + random.nextInt(2);
        BlockState[] walls = {
                ModBlocks.WHITE_LIMESTONE_BRICKS.get().defaultBlockState(),
                ModBlocks.YELLOW_LIMESTONE_BRICKS.get().defaultBlockState(),
                ModBlocks.SMOOTH_YELLOW_LIMESTONE.get().defaultBlockState(),
                ModBlocks.SMOOTH_WHITE_LIMESTONE.get().defaultBlockState(),
                ModBlocks.COBBLED_YELLOW_LIMESTONE.get().defaultBlockState()};
        BlockState wall = walls[random.nextInt(walls.length)];
        BlockState trim = random.nextBoolean() ? ModBlocks.CUT_YELLOW_LIMESTONE.get().defaultBlockState()
                : ModBlocks.WHITE_LIMESTONE_BRICKS.get().defaultBlockState();
        Block shutter = random.nextBoolean() ? Blocks.OAK_TRAPDOOR : ModBlocks.ITALIAN_PINE_TRAPDOOR.get();
        // a door onto a street, the piazza or a garden, in the middle of a side if it can be
        Direction door = null;
        int doorAt = 0;
        List<Direction> sides = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        for (int i = sides.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Direction t = sides.get(i);
            sides.set(i, sides.get(j));
            sides.set(j, t);
        }
        search:
        for (Direction side : sides) {
            int len = side.getAxis() == Direction.Axis.X ? d : w;
            for (int k = 0; k < len - 2; k++) {
                int a = len / 2 + ((k % 2 == 0) ? k / 2 : -(k / 2 + 1));
                if (a < 1 || a > len - 2) continue;
                int[] out = outside(x0, z0, w, d, side, a);
                if (l.walkable(out[0], out[1]) && l.at(out[0], out[1]) != GARDEN) {
                    door = side;
                    doorAt = a;
                    break search;
                }
            }
        }
        return new House(x0, z0, w, d, floors, wall, trim, shutter, door, doorAt, null, false);
    }

    /** The cell outside a house's wall on the given side, at position a along that wall. */
    static int[] outside(int x0, int z0, int w, int d, Direction side, int a) {
        return switch (side) {
            case NORTH -> new int[]{x0 + a, z0 - 1};
            case SOUTH -> new int[]{x0 + a, z0 + d};
            case WEST -> new int[]{x0 - 1, z0 + a};
            default -> new int[]{x0 + w, z0 + a};
        };
    }

    /** The cell in a house's wall on the given side, at position a along it. */
    static int[] inWall(House h, Direction side, int a) {
        return switch (side) {
            case NORTH -> new int[]{h.x0() + a, h.z0()};
            case SOUTH -> new int[]{h.x0() + a, h.z1()};
            case WEST -> new int[]{h.x0(), h.z0() + a};
            default -> new int[]{h.x1(), h.z0() + a};
        };
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Layout l = this.layout();
        int f = this.centre.getY();
        this.shapeGround(level, box, f);
        this.paveTown(level, box, l, f);
        this.buildFountain(level, box, f);
        for (int[] s : new int[][]{{-5, -5}, {5, -5}, {-5, 4}, {5, 4}}) this.buildStall(level, box, s[0], s[1], f);
        this.buildChurch(level, box, f);
        this.buildTower(level, box, f);
        RandomSource layoutRandom = RandomSource.create(this.seed ^ 0x5DEECE66DL);
        for (House h : l.houses) {
            this.buildHouse(level, box, l, h, f, layoutRandom);
        }
        for (int[] field : FIELDS) this.buildField(level, box, field, f, layoutRandom);
        this.spawnLife(level, box, l, f);
    }

    BlockPos at(int x, int y, int z) {
        return new BlockPos(this.centre.getX() + x, y, this.centre.getZ() + z);
    }

    void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = this.at(x, y, z);
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
    }

    static BlockState st(Block block) {
        return block.defaultBlockState();
    }

    // ---------------------------------------------------------------- the ground

    /** How far a column lies outside the levelled ground (the town and its fields with their aprons). */
    static double platformDistance(int x, int z) {
        double best = Mth.length(Math.max(0, Math.abs(x) - (CORE + 1)), Math.max(0, Math.abs(z) - (CORE + 1)));
        for (int[] fl : FIELDS) {
            int dx = Math.max(0, Math.max(fl[0] - APRON - x, x - fl[2] - APRON));
            int dz = Math.max(0, Math.max(fl[1] - APRON - z, z - fl[3] - APRON));
            best = Math.min(best, Mth.length(dx, dz));
        }
        return best;
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** Level the town and fields at the floor, and slope the land back up or down to its own height around them. */
    void shapeGround(WorldGenLevel level, BoundingBox box, int f) {
        BlockState grass = st(ModBlocks.CINNAMON_GRASS_BLOCK.get()), dirt = st(ModBlocks.CINNAMON_DIRT.get());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int x = wx - this.centre.getX(), z = wz - this.centre.getZ();
                if (Math.abs(x) > BOX || Math.abs(z) > BOX) continue;
                double dist = platformDistance(x, z);
                if (dist >= MARGIN) continue;
                int ground = f - 30;
                for (int y = f + CLEAR; y > f - 30; y--) {
                    if (isGround(level.getBlockState(pos.set(wx, y, wz)))) {
                        ground = y;
                        break;
                    }
                }
                double t = dist / MARGIN;
                t = t * t * (3 - 2 * t);
                int target = dist == 0 ? f : (int) Math.round(f + (ground - f) * t);
                BlockState top = level.getBlockState(pos.set(wx, ground, wz));
                if (!top.is(BlockTags.DIRT)) top = grass;
                // everything above the new ground goes: on the platform all of it, beside it the land and its plants
                int clearTo = dist == 0 ? f + CLEAR : Math.max(ground + 2, target + 1);
                for (int y = target + 1; y <= clearTo && y <= box.maxY(); y++) {
                    pos.set(wx, y, wz);
                    BlockState s = level.getBlockState(pos);
                    if (!s.isAir() && (dist == 0 || y <= ground + 1 || !s.blocksMotion() || isGround(s))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                for (int y = ground + 1; y < target; y++) {
                    level.setBlock(pos.set(wx, y, wz), y < target - 3 ? st(Blocks.STONE) : dirt, 2);
                }
                level.setBlock(pos.set(wx, target, wz), top, 2);
            }
        }
    }

    // ---------------------------------------------------------------- streets and the piazza

    void paveTown(WorldGenLevel level, BoundingBox box, Layout l, int f) {
        BlockState cobble = st(ModBlocks.COBBLED_YELLOW_LIMESTONE.get()), bricks = st(ModBlocks.YELLOW_LIMESTONE_BRICKS.get());
        BlockState travertine = st(ModBlocks.SMOOTH_WHITE_LIMESTONE.get());
        for (int x = -CORE; x <= CORE; x++) {
            for (int z = -CORE; z <= CORE; z++) {
                int c = l.at(x, z);
                BlockState floor = switch (c) {
                    case PIAZZA -> (x % 4 == 0 || z % 4 == 0) ? travertine : st(Blocks.BRICKS);
                    case LANE -> ((x * 31 + z * 17) & 3) == 0 ? bricks : cobble;
                    case GARDEN -> ((x * 31 + z * 17) & 3) == 0 ? travertine : cobble;
                    default -> cobble;
                };
                this.set(level, box, x, f, z, floor);
                this.set(level, box, x, f - 1, z, st(Blocks.STONE));
                // a little courtyard here and there, with a potted nerium
                if (c == GARDEN && Math.floorMod(x * 7 + z * 13, 9) == 0) this.set(level, box, x, f + 1, z, st(ModBlocks.POTTED_NERIUM.get()));
                // and rose bushes in little beds
                if (c == GARDEN && Math.floorMod(x * 7 + z * 13, 9) == 4) this.roseBush(level, box, x, z, f);
            }
        }
        // nerium hedges all round the outside of the town, with gaps where the streets leave it and toward the fields
        BlockState grass = st(ModBlocks.CINNAMON_GRASS_BLOCK.get()), nerium = st(ModBlocks.NERIUM.get());
        int ring = CORE + 2;
        for (int a = -ring; a <= ring; a++) {
            if (Math.abs(a) <= 11) continue;
            int edge = Mth.clamp(a, -CORE, CORE);
            for (int[] p : new int[][]{{a, -ring, edge, -CORE}, {a, ring, edge, CORE}, {-ring, a, -CORE, edge}, {ring, a, CORE, edge}}) {
                if (l.at(p[2], p[3]) == LANE) continue;
                this.set(level, box, p[0], f, p[1], grass);
                this.set(level, box, p[0], f + 1, p[1], nerium);
            }
        }
        // rose bushes at the corners of the piazza
        for (int[] p : new int[][]{{-5, -7}, {5, -7}, {-7, 5}, {7, 5}, {-7, -5}, {7, -5}}) this.roseBush(level, box, p[0], p[1], f);
        for (int[] p : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            this.set(level, box, p[0], f + 1, p[1], st(ModBlocks.POTTED_NERIUM.get()));
            // lamp posts at the corners of the square
            this.set(level, box, p[0] + Integer.signum(p[0]), f + 1, p[1] + Integer.signum(p[1]), st(ModBlocks.WHITE_LIMESTONE_BRICK_WALL.get()));
            this.set(level, box, p[0] + Integer.signum(p[0]), f + 2, p[1] + Integer.signum(p[1]), st(ModBlocks.WHITE_LIMESTONE_BRICK_WALL.get()));
            this.set(level, box, p[0] + Integer.signum(p[0]), f + 3, p[1] + Integer.signum(p[1]), st(Blocks.LANTERN));
        }
    }

    /** A rose bush in a bed of earth. */
    void roseBush(WorldGenLevel level, BoundingBox box, int x, int z, int f) {
        BlockState rose = Blocks.ROSE_BUSH.defaultBlockState();
        this.set(level, box, x, f, z, st(Blocks.GRASS_BLOCK));
        this.set(level, box, x, f + 1, z, rose.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        this.set(level, box, x, f + 2, z, rose.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** A small fountain: a round basin with a column in the middle, water spilling from its top. */
    void buildFountain(WorldGenLevel level, BoundingBox box, int f) {
        BlockState rim = st(ModBlocks.WHITE_LIMESTONE_BRICKS.get());
        BlockState rimTop = ModBlocks.SMOOTH_WHITE_LIMESTONE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean edge = Math.abs(x) == 2 || Math.abs(z) == 2;
                if (edge && Math.abs(x) == 2 && Math.abs(z) == 2) {
                    this.set(level, box, x, f, z, rim);
                    this.set(level, box, x, f + 1, z, st(ModBlocks.POTTED_NERIUM.get()));
                    continue;
                }
                this.set(level, box, x, f - 1, z, rim);
                if (edge) {
                    this.set(level, box, x, f, z, rim);
                    this.set(level, box, x, f + 1, z, rimTop);
                } else if (x == 0 && z == 0) {
                    this.set(level, box, 0, f, 0, rim);
                    this.set(level, box, 0, f + 1, 0, st(ModBlocks.WHITE_LIMESTONE_BRICK_WALL.get()));
                    this.set(level, box, 0, f + 2, 0, st(ModBlocks.WHITE_LIMESTONE_BRICK_WALL.get()));
                    this.set(level, box, 0, f + 3, 0, st(ModBlocks.CHISELED_WHITE_LIMESTONE_BRICKS.get()));
                    this.set(level, box, 0, f + 4, 0, st(Blocks.WATER));
                    // water set during world generation lies still until something nudges it: start it flowing
                    BlockPos spout = this.at(0, f + 4, 0);
                    if (box.isInside(spout)) level.scheduleTick(spout, net.minecraft.world.level.material.Fluids.WATER, 1);
                } else {
                    this.set(level, box, x, f, z, st(Blocks.WATER));
                }
            }
        }
    }

    /** A market stall: four posts, a striped awning, and a counter with a barrel of goods. */
    void buildStall(WorldGenLevel level, BoundingBox box, int cx, int cz, int f) {
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = 0; dz <= 1; dz++) {
                this.set(level, box, cx + dx, f + 1, cz + dz, st(Blocks.SPRUCE_FENCE));
                this.set(level, box, cx + dx, f + 2, cz + dz, st(Blocks.SPRUCE_FENCE));
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                this.set(level, box, cx + dx, f + 3, cz + dz, st((cx + dx & 1) == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL));
            }
        }
        this.set(level, box, cx, f + 1, cz, st(Blocks.BARREL));
        this.set(level, box, cx, f + 1, cz + 1, ModBlocks.CUT_YELLOW_LIMESTONE_SLAB.get().defaultBlockState());
    }

    // ---------------------------------------------------------------- roofs

    static BlockState roof(Direction rising) {
        return ModBlocks.TERRACOTTA_ROOF_TILES.get().defaultBlockState().setValue(RoofTileBlock.FACING, rising);
    }

    /**
     * A gabled roof of roof tiles over the rectangle x0..x1, z0..z1, whose walls end at y = top - 1. The ridge runs
     * along the longer side; the eaves stick out a block, and the two gable ends are walled up in {@code gable}.
     */
    void gableRoof(WorldGenLevel level, BoundingBox box, Layout l, int x0, int z0, int x1, int z1, int top, BlockState gable) {
        boolean ridgeAlongZ = (z1 - z0) >= (x1 - x0);
        int w = ridgeAlongZ ? x1 - x0 + 1 : z1 - z0 + 1;
        int a0 = ridgeAlongZ ? z0 : x0, a1 = ridgeAlongZ ? z1 : x1;
        Direction up = ridgeAlongZ ? Direction.EAST : Direction.SOUTH;
        int half = w / 2;
        boolean odd = w % 2 == 1;
        for (int along = a0; along <= a1; along++) {
            boolean end = along == a0 || along == a1;
            for (int i = -1; i < half; i++) {
                int y = top + i;
                this.roofCell(level, box, l, i < 0, ridgeAlongZ ? x0 + i : along, ridgeAlongZ ? along : z0 + i, y, roof(up));
                this.roofCell(level, box, l, i < 0, ridgeAlongZ ? x1 - i : along, ridgeAlongZ ? along : z1 - i, y, roof(up.getOpposite()));
            }
            if (odd) {
                BlockState ridge = end ? gable : st(Blocks.TERRACOTTA);
                this.roofCell(level, box, l, false, ridgeAlongZ ? x0 + half : along, ridgeAlongZ ? along : z0 + half, top + half - 1, ridge);
            }
            if (end) {
                // the gable: walled up under the slopes
                for (int i = 0; i < w; i++) {
                    int h = Math.min(i, w - 1 - i);
                    if (odd && i == half) h = half - 1;
                    for (int y = top; y < top + h; y++) {
                        this.set(level, box, ridgeAlongZ ? x0 + i : along, y, ridgeAlongZ ? along : z0 + i, gable);
                    }
                }
            }
        }
    }

    // a roof cell (x, z at height y); the eaves sticking out over the wall only where nothing else stands
    void roofCell(WorldGenLevel level, BoundingBox box, Layout l, boolean eave, int x, int z, int y, BlockState state) {
        if (eave && !l.walkable(x, z)) return;
        this.set(level, box, x, y, z, state);
    }

    // ---------------------------------------------------------------- the church and campanile

    void buildChurch(WorldGenLevel level, BoundingBox box, int f) {
        BlockState side = st(ModBlocks.YELLOW_LIMESTONE_BRICKS.get());
        BlockState white = st(ModBlocks.SMOOTH_WHITE_LIMESTONE.get()), band = st(ModBlocks.CUT_YELLOW_LIMESTONE.get());
        BlockState floor = st(ModBlocks.POLISHED_MARBLE.get());
        int top = f + 10;
        for (int x = CH_X0; x <= CH_X1; x++) {
            for (int z = CH_Z0; z <= CH_Z1; z++) {
                boolean wall = x == CH_X0 || x == CH_X1 || z == CH_Z0 || z == CH_Z1;
                this.set(level, box, x, f, z, floor);
                for (int y = f + 1; y < top; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (wall) {
                        // the facade striped in white and ochre, like Siena's
                        s = z == CH_Z1 ? ((y - f) % 3 == 0 ? band : white) : side;
                    }
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        this.gableRoof(level, box, this.layout(), CH_X0, CH_Z0, CH_X1, CH_Z1, top, white);
        // the door and the rose window above it
        for (int y = f + 1; y <= f + 3; y++) this.set(level, box, 0, y, CH_Z1, Blocks.AIR.defaultBlockState());
        this.door(level, box, 0, f + 1, CH_Z1, Direction.SOUTH, Blocks.SPRUCE_DOOR);
        this.set(level, box, 0, f + 3, CH_Z1, st(ModBlocks.CHISELED_WHITE_LIMESTONE_BRICKS.get()));
        for (int[] g : new int[][]{{0, 7}, {-1, 7}, {1, 7}, {0, 6}, {0, 8}, {0, 11}}) {
            this.set(level, box, g[0], f + g[1], CH_Z1, st(Blocks.ORANGE_STAINED_GLASS));
        }
        this.set(level, box, 0, f, CH_Z1 + 1, ModBlocks.WHITE_LIMESTONE_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        // tall windows down both sides
        for (int z = CH_Z0 + 2; z < CH_Z1 - 1; z += 3) {
            for (int y = f + 4; y <= f + 7; y++) {
                this.set(level, box, CH_X0, y, z, st(Blocks.YELLOW_STAINED_GLASS));
                this.set(level, box, CH_X1, y, z, st(Blocks.YELLOW_STAINED_GLASS));
            }
        }
        // pews, facing the altar
        for (int z = CH_Z1 - 3; z >= CH_Z0 + 6; z -= 2) {
            for (int x = CH_X0 + 1; x <= CH_X1 - 1; x++) {
                if (x == 0) continue;
                this.set(level, box, x, f + 1, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
            }
        }
        // the altar, with candles and the priest's brewing stand beside it
        for (int x = -1; x <= 1; x++) {
            this.set(level, box, x, f + 1, CH_Z0 + 2, st(ModBlocks.CHISELED_MARBLE.get()));
            this.set(level, box, x, f + 2, CH_Z0 + 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        }
        this.set(level, box, 2, f + 1, CH_Z0 + 2, st(Blocks.BREWING_STAND));
        for (int[] p : new int[][]{{CH_X0 + 1, CH_Z0 + 1}, {CH_X1 - 1, CH_Z0 + 1}, {CH_X0 + 1, CH_Z1 - 1}, {CH_X1 - 1, CH_Z1 - 1},
                {CH_X0 + 1, (CH_Z0 + CH_Z1) / 2}, {CH_X1 - 1, (CH_Z0 + CH_Z1) / 2 + 2}}) {
            this.set(level, box, p[0], f + 1, p[1], st(Blocks.LANTERN));
        }
        // the way through into the campanile
        for (int y = f + 1; y <= f + 2; y++) {
            this.set(level, box, CH_X1, y, -11, Blocks.AIR.defaultBlockState());
            this.set(level, box, TW_X0, y, -11, Blocks.AIR.defaultBlockState());
        }
    }

    void buildTower(WorldGenLevel level, BoundingBox box, int f) {
        BlockState wall = st(ModBlocks.YELLOW_LIMESTONE_BRICKS.get()), quoin = st(ModBlocks.WHITE_LIMESTONE_BRICKS.get());
        int belfry = f + 16, ceiling = f + 20;
        int mx = (TW_X0 + TW_X1) / 2, mz = (TW_Z0 + TW_Z1) / 2;
        for (int x = TW_X0; x <= TW_X1; x++) {
            for (int z = TW_Z0; z <= TW_Z1; z++) {
                boolean edge = x == TW_X0 || x == TW_X1 || z == TW_Z0 || z == TW_Z1;
                boolean corner = (x == TW_X0 || x == TW_X1) && (z == TW_Z0 || z == TW_Z1);
                this.set(level, box, x, f, z, st(ModBlocks.POLISHED_MARBLE.get()));
                for (int y = f + 1; y <= ceiling; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (edge) {
                        s = corner ? quoin : wall;
                        // the open arches of the belfry
                        if (!corner && y > belfry && y < ceiling) s = Blocks.AIR.defaultBlockState();
                        // little slit windows up the shaft
                        if (!corner && (x == mx || z == mz) && (y - f) % 5 == 3 && y < belfry) s = st(Blocks.IRON_BARS);
                    } else if (y == belfry || y == ceiling) {
                        s = y == ceiling ? st(ModBlocks.SMOOTH_WHITE_LIMESTONE.get()) : st(Blocks.SPRUCE_PLANKS);
                    }
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        // the ladder up to the belfry
        for (int y = f + 1; y <= belfry; y++) {
            this.set(level, box, mx, y, TW_Z0 + 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        this.set(level, box, mx, f + 1, TW_Z1 - 1, st(Blocks.LANTERN));
        // the bell, hanging from the belfry's ceiling
        this.set(level, box, mx, ceiling - 1, mz, Blocks.BELL.defaultBlockState()
                .setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH));
        // a pyramid roof of tiles, a terracotta cap and a lightning rod
        for (int ring = 0; ring <= 1; ring++) {
            int y = ceiling + 1 + ring;
            int x0 = TW_X0 + ring, x1 = TW_X1 - ring, z0 = TW_Z0 + ring, z1 = TW_Z1 - ring;
            for (int x = x0; x <= x1; x++) {
                // the corners bend round, as hips
                boolean west = x == x0, east = x == x1;
                this.set(level, box, x, y, z0, roof(Direction.SOUTH).setValue(RoofTileBlock.SHAPE,
                        west ? net.minecraft.world.level.block.state.properties.StairsShape.OUTER_LEFT
                                : east ? net.minecraft.world.level.block.state.properties.StairsShape.OUTER_RIGHT
                                : net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT));
                this.set(level, box, x, y, z1, roof(Direction.NORTH).setValue(RoofTileBlock.SHAPE,
                        east ? net.minecraft.world.level.block.state.properties.StairsShape.OUTER_LEFT
                                : west ? net.minecraft.world.level.block.state.properties.StairsShape.OUTER_RIGHT
                                : net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT));
            }
            for (int z = z0 + 1; z < z1; z++) {
                this.set(level, box, x0, y, z, roof(Direction.EAST));
                this.set(level, box, x1, y, z, roof(Direction.WEST));
            }
        }
        this.set(level, box, mx, ceiling + 3, mz, st(Blocks.TERRACOTTA));
        this.set(level, box, mx, ceiling + 4, mz, st(Blocks.LIGHTNING_ROD));
    }

    // ---------------------------------------------------------------- houses

    void buildHouse(WorldGenLevel level, BoundingBox box, Layout l, House h, int f, RandomSource random) {
        int top = f + 4 * h.floors();
        BlockState planks = st(Blocks.SPRUCE_PLANKS);
        for (int x = h.x0(); x <= h.x1(); x++) {
            for (int z = h.z0(); z <= h.z1(); z++) {
                boolean wall = x == h.x0() || x == h.x1() || z == h.z0() || z == h.z1();
                boolean corner = (x == h.x0() || x == h.x1()) && (z == h.z0() || z == h.z1());
                this.set(level, box, x, f, z, wall ? h.trim() : st(Blocks.TERRACOTTA));
                for (int y = f + 1; y <= top; y++) {
                    BlockState s;
                    if (wall) s = corner || y == f + 1 || y == top ? h.trim() : h.wall();
                    else s = (y - f) % 4 == 0 ? planks : Blocks.AIR.defaultBlockState();
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        this.gableRoof(level, box, l, h.x0(), h.z0(), h.x1(), h.z1(), top + 1, h.wall());

        // windows on every floor, on the sides that look out onto a street, the piazza or a garden
        for (Direction side : Direction.Plane.HORIZONTAL) {
            int len = side.getAxis() == Direction.Axis.X ? h.d() : h.w();
            for (int a = 1; a <= len - 2; a++) {
                if (a % 2 == 0 && len > 4) continue;
                int[] out = outside(h.x0(), h.z0(), h.w(), h.d(), side, a);
                if (!l.walkable(out[0], out[1])) continue;
                int[] wc = inWall(h, side, a);
                for (int floor = 0; floor < h.floors(); floor++) {
                    if (floor == 0 && side == h.door() && Math.abs(a - h.doorAt()) <= 1) continue;
                    this.shutteredWindow(level, box, wc[0], f + 4 * floor + 1, wc[1], side, h.shutter());
                }
            }
        }

        // a ladder up through the floors in one corner, and a lantern on each floor in the other
        for (int y = f + 1; y < top; y++) {
            this.set(level, box, h.x0() + 1, y, h.z0() + 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        for (int floor = 0; floor < h.floors(); floor++) {
            this.set(level, box, h.x1() - 1, f + 4 * floor + 1, h.z1() - 1, st(Blocks.LANTERN));
        }

        if (h.door() == null) return;
        int[] dc = inWall(h, h.door(), h.doorAt());
        this.door(level, box, dc[0], f + 1, dc[1], h.door(), Blocks.SPRUCE_DOOR);
        // the bed and the workstation, inside away from the door
        int ix0 = h.x0() + 1, iz0 = h.z0() + 1, ix1 = h.x1() - 1, iz1 = h.z1() - 1;
        if (!h.home()) return;
        int[] bed = this.bedSpot(h, dc, ix0, iz0, ix1, iz1);
        if (bed != null) this.bed(level, box, bed[0], f + 1, bed[1], Direction.EAST);
        if (h.workstation() != null) {
            int[] ws = this.workSpot(h, dc, ix0, iz0, ix1, iz1, bed);
            if (ws != null) this.set(level, box, ws[0], f + 1, ws[1], st(h.workstation()));
        }
    }

    /**
     * A window two blocks tall in a wall facing {@code out}, with no glass: a pair of shutters (trapdoors, one above
     * the other) stands in it, shut flush with the wall's outer face. Click one to open it.
     */
    void shutteredWindow(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction out, Block shutter) {
        BlockState s = shutter.defaultBlockState().setValue(TrapDoorBlock.FACING, out.getOpposite())
                .setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.HALF, Half.BOTTOM);
        this.set(level, box, x, y, z, s);
        this.set(level, box, x, y + 1, z, s);
    }

    /** A spot inside a house for a bed (two long, head to the east), clear of the door, ladder and lantern. */
    int[] bedSpot(House h, int[] door, int ix0, int iz0, int ix1, int iz1) {
        for (int z = iz1; z >= iz0; z--) {
            for (int x = ix0; x < ix1; x++) {
                if (clear(h, door, x, z, ix0, iz0, ix1, iz1) && clear(h, door, x + 1, z, ix0, iz0, ix1, iz1)) return new int[]{x, z};
            }
        }
        return null;
    }

    /** A spot for the workstation: clear, and not where the bed is. */
    int[] workSpot(House h, int[] door, int ix0, int iz0, int ix1, int iz1, int[] bed) {
        for (int z = iz0; z <= iz1; z++) {
            for (int x = ix1; x >= ix0; x--) {
                if (!clear(h, door, x, z, ix0, iz0, ix1, iz1)) continue;
                if (bed != null && z == bed[1] && (x == bed[0] || x == bed[0] + 1)) continue;
                return new int[]{x, z};
            }
        }
        return null;
    }

    static boolean clear(House h, int[] door, int x, int z, int ix0, int iz0, int ix1, int iz1) {
        if (x == ix0 && z == iz0) return false;                       // the ladder
        if (x == ix1 && z == iz1) return false;                       // the lantern
        return Math.abs(x - door[0]) + Math.abs(z - door[1]) > 1;     // the way in
    }

    void door(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, Block door) {
        BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.set(level, box, x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.set(level, box, x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    void bed(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction toward) {
        BlockState s = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, toward);
        this.set(level, box, x, y, z, s.setValue(BedBlock.PART, BedPart.FOOT));
        this.set(level, box, x + toward.getStepX(), y, z + toward.getStepZ(), s.setValue(BedBlock.PART, BedPart.HEAD));
    }

    // ---------------------------------------------------------------- the fields

    void buildField(WorldGenLevel level, BoundingBox box, int[] field, int f, RandomSource random) {
        int x0 = field[0], z0 = field[1], x1 = field[2], z1 = field[3], kind = field[4];
        boolean alongX = (x1 - x0) >= (z1 - z0);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                // which line across the field (lines run along its longer side)
                int line = alongX ? z - z0 : x - x0;
                int lines = alongX ? z1 - z0 : x1 - x0;
                long h = (x * 341873128712L + z * 132897987541L) ^ this.seed;
                int roll = (int) ((h >>> 20) & 0xFF);
                switch (kind) {
                    case VINEYARD, TOMATOES -> {
                        // a line of poles, then a one-block path, and so on
                        if (line % 2 != 0) {
                            this.set(level, box, x, f, z, st(Blocks.DIRT_PATH));
                            continue;
                        }
                        WinePoleBlock.Grape grape = kind == VINEYARD ? WinePoleBlock.Grape.RED : WinePoleBlock.Grape.TOMATO;
                        this.set(level, box, x, f, z, st(Blocks.COARSE_DIRT));
                        BlockState pole = ModBlocks.WINE_POLE.get().defaultBlockState().setValue(WinePoleBlock.GRAPE, grape);
                        this.set(level, box, x, f + 1, z, pole.setValue(WinePoleBlock.AGE, 1 + roll % 3));
                        this.set(level, box, x, f + 2, z, pole.setValue(WinePoleBlock.AGE, 1 + (roll >> 2) % 3));
                    }
                    default -> {
                        // a channel of water down the middle, farmland either side
                        if (line == lines / 2) {
                            this.set(level, box, x, f, z, st(Blocks.WATER));
                            continue;
                        }
                        this.set(level, box, x, f, z, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
                        CropBlock crop = kind == WHEAT ? (CropBlock) Blocks.WHEAT : ModBlocks.BARLEY.get();
                        this.set(level, box, x, f + 1, z, crop.getStateForAge(roll % (crop.getMaxAge() + 1)));
                    }
                }
            }
        }
        // a composter for the farmers at the corner of the corn fields
        if (kind == WHEAT || kind == BARLEY) this.set(level, box, x0 - 1, f + 1, z0 - 1, st(Blocks.COMPOSTER));
    }

    // ---------------------------------------------------------------- villagers, cats and animals

    void spawnLife(WorldGenLevel level, BoundingBox box, Layout l, int f) {
        RandomSource random = RandomSource.create(this.seed * 31 + 7);
        for (House h : l.houses) {
            if (!h.home()) continue;
            this.spawnVillager(level, box, this.at((h.x0() + h.x1()) / 2, f + 1, (h.z0() + h.z1()) / 2 + 0));
        }
        // the priest
        this.spawnVillager(level, box, this.at(0, f + 1, CH_Z0 + 4));
        // tens of cats, in the streets and on the piazza
        int cats = 0;
        for (int tries = 0; tries < 400 && cats < CATS; tries++) {
            int x = random.nextInt(N) - CORE, z = random.nextInt(N) - CORE;
            int c = l.at(x, z);
            if ((c != LANE && c != PIAZZA) || Math.abs(x) <= 2 && Math.abs(z) <= 2) continue;
            cats++;
            this.spawn(level, box, EntityType.CAT, this.at(x, f + 1, z));
        }
        // donkeys, cows and sheep about the fields
        for (int[] field : FIELDS) {
            int x0 = field[0] - 2, z0 = field[1] - 2, x1 = field[2] + 2, z1 = field[3] + 2;
            EntityType<?>[] kinds = {EntityType.COW, EntityType.SHEEP, EntityType.DONKEY, EntityType.SHEEP, EntityType.COW};
            for (int i = 0; i < 4; i++) {
                int x, z;
                // on the apron round the field
                if (random.nextBoolean()) {
                    x = x0 + random.nextInt(x1 - x0 + 1);
                    z = random.nextBoolean() ? z0 : z1;
                } else {
                    x = random.nextBoolean() ? x0 : x1;
                    z = z0 + random.nextInt(z1 - z0 + 1);
                }
                if (l.at(x, z) != -1) continue;
                this.spawn(level, box, kinds[random.nextInt(kinds.length)], this.at(x, f + 1, z));
            }
        }
    }

    void spawnVillager(WorldGenLevel level, BoundingBox box, BlockPos pos) {
        if (!box.isInside(pos)) return;
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) return;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillageRegistry.VENETIAN.get()).setProfession(VillagerProfession.NONE));
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }

    void spawn(WorldGenLevel level, BoundingBox box, EntityType<?> type, BlockPos pos) {
        if (!box.isInside(pos)) return;
        if (!(type.create(level.getLevel()) instanceof Mob mob)) return;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
