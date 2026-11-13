package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.RiceBlock;
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
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A Chinese village of the monsoon subtropics (Cwa), on ground levelled for it.
 *
 * <ul>
 *   <li>Two stone-paved avenues cross at a square, where the village bell hangs in a pavilion (a ting) of red
 *       pillars under a dark tiled roof with upturned corners. Each avenue ends at a paifang, a red gateway with a
 *       little tiled roof and lanterns hanging under its beam; stone posts and red paper lanterns line the way.</li>
 *   <li>Fifteen courtyard houses stand in four quarters, each a single hall of whitewashed walls on a stone footing,
 *       red pillars and lattice windows along its front, a red door, and a grey tiled roof with an overhanging eave
 *       and curled ridge ends, in front a walled yard with a red gate, a little magnolia and bamboo.</li>
 *   <li>A five-storey pagoda stands in one quarter, each storey with its own skirt of tiles, topped with a golden
 *       finial; the village priest lives in it.</li>
 *   <li>Bamboo grows around the village, and rice paddies lie outside it at the end of each avenue, with pigs and
 *       chickens about them.</li>
 *   <li>A Chinese villager lives in every house.</li>
 * </ul>
 */
public class ChineseVillagePiece extends StructurePiece {
    static final int CORE = 36;
    static final int MARGIN = 10;
    static final int BOX = 66;
    // high enough to clear a karst tower off the village ground
    static final int CLEAR = 52;
    static final int PLOT = 15;
    // the plots' near and far corners along each axis (and their mirror images)
    static final int[] PLOT_STARTS = {3, 20};
    static final int[][] FIELDS = {
            {-12, -56, 12, -42},
            {42, -12, 56, 12},
            {-12, 42, 12, 56},
            {-56, -12, -42, 12}};
    static final int APRON = 3;

    static final Block[] WORKSTATIONS = {
            Blocks.COMPOSTER, Blocks.LECTERN, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CARTOGRAPHY_TABLE,
            Blocks.BARREL, Blocks.FLETCHING_TABLE, Blocks.CAULDRON, Blocks.STONECUTTER, Blocks.LOOM, Blocks.SMITHING_TABLE,
            Blocks.GRINDSTONE, Blocks.COMPOSTER, Blocks.LECTERN, Blocks.BARREL};

    final BlockPos centre;
    final long seed;

    public ChineseVillagePiece(BlockPos centre, long seed) {
        super(VillageRegistry.CHINESE_VILLAGE_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX, centre.getY() - 30, centre.getZ() - BOX,
                centre.getX() + BOX, centre.getY() + CLEAR, centre.getZ() + BOX));
        this.centre = centre;
        this.seed = seed;
    }

    public ChineseVillagePiece(CompoundTag tag) {
        super(VillageRegistry.CHINESE_VILLAGE_PIECE.get(), tag);
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

    /** A house plot: its corner, and whether its front (the yard and gate) faces south (+z) or north. */
    record Plot(int x0, int z0, boolean frontSouth) {
        int x(int px) {
            return this.x0 + px;
        }

        int z(int pz) {
            return this.frontSouth ? this.z0 + pz : this.z0 + PLOT - 1 - pz;
        }

        /** A direction in the plot's own terms (SOUTH is toward its front) in the world's. */
        Direction dir(Direction local) {
            if (this.frontSouth || local.getAxis() != Direction.Axis.Z) return local;
            return local.getOpposite();
        }
    }

    static java.util.List<Plot> plots() {
        java.util.List<Plot> plots = new java.util.ArrayList<>();
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                for (int a : PLOT_STARTS) {
                    for (int b : PLOT_STARTS) {
                        int x0 = sx > 0 ? a : -a - PLOT + 1;
                        int z0 = sz > 0 ? b : -b - PLOT + 1;
                        // the front faces the middle of the village (north for the southern plots)
                        plots.add(new Plot(x0, z0, sz < 0));
                    }
                }
            }
        }
        return plots;
    }

    // the pagoda takes the far north-west plot
    static boolean isPagodaPlot(Plot p) {
        return p.x0() == -PLOT_STARTS[1] - PLOT + 1 && p.z0() == -PLOT_STARTS[1] - PLOT + 1;
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int f = this.centre.getY();
        RandomSource r = RandomSource.create(this.seed ^ 0x5DEECE66DL);
        this.shapeGround(level, box, f);
        // the trees the levelling cut round the edge: nothing of them left hanging in the air
        int cx = this.centre.getX(), cz = this.centre.getZ();
        LooseTreeCleanup.clean(level, chunkPos, f - 12, f + CLEAR + 6,
                (x, z) -> platformDistance(x - cx, z - cz) < MARGIN + 10,
                (x, z) -> platformDistance(x - cx, z - cz) == 0);
        this.pave(level, box, f);
        this.buildPavilion(level, box, f);
        for (int s : new int[]{-1, 1}) {
            this.buildGate(level, box, f, 0, s * CORE, true);
            this.buildGate(level, box, f, s * CORE, 0, false);
        }
        this.streetLanterns(level, box, f);
        int home = 0;
        for (Plot p : plots()) {
            if (isPagodaPlot(p)) this.buildPagoda(level, box, p, f);
            else this.buildHouse(level, box, p, f, WORKSTATIONS[home++ % WORKSTATIONS.length], r);
        }
        this.bambooRing(level, box, f, r);
        for (int[] field : FIELDS) this.buildPaddy(level, box, field, f, r);
        this.spawnLife(level, box, f);
    }

    BlockPos at(int x, int y, int z) {
        return new BlockPos(this.centre.getX() + x, y, this.centre.getZ() + z);
    }

    void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = this.at(x, y, z);
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
    }

    /** Red lacquered wood: the pillars, the gates and the doors' frames (red-painted planks). */
    static BlockState red() {
        return ModBlocks.PAINTED_PLANKS.get(net.minecraft.world.item.DyeColor.RED).get().defaultBlockState();
    }

    /** Whitewashed lime plaster: the walls (smooth white limestone). */
    static BlockState white() {
        return ModBlocks.SMOOTH_WHITE_LIMESTONE.get().defaultBlockState();
    }

    static BlockState st(Block block) {
        return block.defaultBlockState();
    }

    static BlockState stair(Block block, Direction facing, boolean upsideDown) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
    }

    static final Block TILE_STAIRS = Blocks.DEEPSLATE_TILE_STAIRS;
    static final BlockState TILES = st(Blocks.DEEPSLATE_TILES);
    static final BlockState TILE_SLAB = st(Blocks.DEEPSLATE_TILE_SLAB);
    static final BlockState FOOTING = st(Blocks.STONE_BRICKS);
    static final BlockState BEAM = st(Blocks.DARK_OAK_PLANKS);
    // red paper lanterns everywhere (they hang under beams and eaves, or stand on posts)
    static BlockState lantern() {
        return ModBlocks.CHINESE_LANTERN.get().defaultBlockState();
    }

    // ---------------------------------------------------------------- the ground

    static double platformDistance(int x, int z) {
        double best = Mth.length(Math.max(0, Math.abs(x) - CORE), Math.max(0, Math.abs(z) - CORE));
        for (int[] fl : FIELDS) {
            int dx = Math.max(0, Math.max(fl[0] - APRON - x, x - fl[2] - APRON));
            int dz = Math.max(0, Math.max(fl[1] - APRON - z, z - fl[3] - APRON));
            best = Math.min(best, Mth.length(dx, dz));
        }
        // the avenues run on out to the fields
        if (Math.abs(x) <= 2 && Math.abs(z) <= 44 || Math.abs(z) <= 2 && Math.abs(x) <= 44) best = 0;
        return best;
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    void shapeGround(WorldGenLevel level, BoundingBox box, int f) {
        BlockState grass = st(Blocks.GRASS_BLOCK), dirt = st(Blocks.DIRT);
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
                int clearTo = dist == 0 ? f + CLEAR : Math.max(ground + 2, target + 1);
                for (int y = target + 1; y <= clearTo && y <= box.maxY(); y++) {
                    pos.set(wx, y, wz);
                    BlockState s = level.getBlockState(pos);
                    if (!s.isAir() && (dist == 0 || y <= ground + 1 || !s.blocksMotion() || isGround(s))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                for (int y = ground + 1; y < target; y++) {
                    level.setBlock(pos.set(wx, y, wz), y < target - 3 ? st(ModBlocks.WHITE_LIMESTONE.get()) : dirt, 2);
                }
                level.setBlock(pos.set(wx, target, wz), grass, 2);
                if (level.getBlockState(pos.set(wx, target - 1, wz)).is(Blocks.GRASS_BLOCK)) level.setBlock(pos, dirt, 2);
            }
        }
    }

    // ---------------------------------------------------------------- avenues, lanes and the square

    static boolean inPlot(int x, int z) {
        for (Plot p : plots()) {
            int z0 = Math.min(p.z(0), p.z(PLOT - 1)), z1 = Math.max(p.z(0), p.z(PLOT - 1));
            if (x >= p.x0() && x < p.x0() + PLOT && z >= z0 && z <= z1) return true;
        }
        return false;
    }

    void pave(WorldGenLevel level, BoundingBox box, int f) {
        for (int x = -44; x <= 44; x++) {
            for (int z = -44; z <= 44; z++) {
                long h = (x * 341873128712L + z * 132897987541L) ^ this.seed;
                int roll = (int) ((h >>> 24) & 0xFF);
                boolean square = Math.abs(x) <= 7 && Math.abs(z) <= 7;
                boolean avenue = Math.abs(x) <= 1 || Math.abs(z) <= 1;
                if (square) {
                    // a chequer of grey stone
                    boolean ring = Math.max(Math.abs(x), Math.abs(z)) == 7;
                    this.set(level, box, x, f, z, st(ring ? Blocks.STONE_BRICKS
                            : ((x + z) & 1) == 0 ? Blocks.POLISHED_ANDESITE : Blocks.SMOOTH_STONE));
                } else if (avenue && (Math.abs(x) <= CORE + 8 && Math.abs(z) <= CORE + 8)) {
                    BlockState stone = roll < 30 ? st(Blocks.MOSSY_STONE_BRICKS) : roll < 50 ? st(Blocks.CRACKED_STONE_BRICKS) : FOOTING;
                    this.set(level, box, x, f, z, stone);
                } else if (Math.abs(x) <= CORE && Math.abs(z) <= CORE && !inPlot(x, z)
                        && (Math.abs(z) == 18 || Math.abs(z) == 19 || Math.abs(x) == 18 || Math.abs(x) == 19)) {
                    // the lanes between the quarters' plots
                    this.set(level, box, x, f, z, st(roll < 40 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH));
                }
            }
        }
        // stone lanterns at the corners of the square
        for (int sx : new int[]{-6, 6}) {
            for (int sz : new int[]{-6, 6}) {
                this.set(level, box, sx, f + 1, sz, st(Blocks.STONE_BRICK_WALL));
                this.set(level, box, sx, f + 2, sz, lantern());
            }
        }
    }

    void streetLanterns(WorldGenLevel level, BoundingBox box, int f) {
        for (int a : new int[]{-26, -10, 10, 26}) {
            for (int side : new int[]{-2, 2}) {
                for (int[] p : new int[][]{{side, a}, {a, side}}) {
                    this.set(level, box, p[0], f + 1, p[1], st(Blocks.DARK_OAK_FENCE));
                    this.set(level, box, p[0], f + 2, p[1], st(Blocks.DARK_OAK_FENCE));
                    this.set(level, box, p[0], f + 3, p[1], lantern());
                }
            }
        }
    }

    // ---------------------------------------------------------------- roofs

    /** A ring of tile stairs at Chebyshev distance r round (cx, cz), each rising toward the middle. */
    void roofRing(WorldGenLevel level, BoundingBox box, int cx, int cz, int r, int y) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                Direction in;
                if (Math.abs(dx) >= Math.abs(dz)) in = dx > 0 ? Direction.WEST : Direction.EAST;
                else in = dz > 0 ? Direction.NORTH : Direction.SOUTH;
                this.set(level, box, cx + dx, y, cz + dz, stair(TILE_STAIRS, in, false));
            }
        }
    }

    /** The corners of a ring curl up: a stair a block higher, its back to the middle. */
    void upturnedCorners(WorldGenLevel level, BoundingBox box, int cx, int cz, int r, int y) {
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                Direction out = sx > 0 ? Direction.EAST : Direction.WEST;
                this.set(level, box, cx + sx * r, y, cz + sz * r, stair(TILE_STAIRS, out.getOpposite(), true));
                this.set(level, box, cx + sx * r, y + 1, cz + sz * r, stair(TILE_STAIRS, out, false));
            }
        }
    }

    // ---------------------------------------------------------------- the pavilion with the bell

    void buildPavilion(WorldGenLevel level, BoundingBox box, int f) {
        for (int sx : new int[]{-3, 3}) {
            for (int sz : new int[]{-3, 3}) {
                this.set(level, box, sx, f, sz, FOOTING);
                for (int y = f + 1; y <= f + 4; y++) this.set(level, box, sx, y, sz, red());
            }
        }
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean edge = Math.max(Math.abs(x), Math.abs(z)) == 3;
                this.set(level, box, x, f + 5, z, edge ? red() : BEAM);
            }
        }
        this.roofRing(level, box, 0, 0, 4, f + 5);
        this.upturnedCorners(level, box, 0, 0, 4, f + 5);
        this.roofRing(level, box, 0, 0, 3, f + 6);
        this.roofRing(level, box, 0, 0, 2, f + 7);
        this.roofRing(level, box, 0, 0, 1, f + 8);
        this.set(level, box, 0, f + 8, 0, TILES);
        this.set(level, box, 0, f + 9, 0, st(Blocks.GOLD_BLOCK));
        this.set(level, box, 0, f + 4, 0, Blocks.BELL.defaultBlockState()
                .setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH));
        // lanterns hanging at the corners
        for (int sx : new int[]{-2, 2}) {
            for (int sz : new int[]{-2, 2}) {
                this.set(level, box, sx, f + 4, sz, lantern().setValue(LanternBlock.HANGING, true));
            }
        }
    }

    // ---------------------------------------------------------------- the paifang at the end of each avenue

    void buildGate(WorldGenLevel level, BoundingBox box, int f, int cx, int cz, boolean acrossX) {
        for (int o = -5; o <= 5; o++) {
            int x = acrossX ? cx + o : cx, z = acrossX ? cz : cz + o;
            if (Math.abs(o) == 3) {
                this.set(level, box, x, f, z, FOOTING);
                for (int y = f + 1; y <= f + 5; y++) this.set(level, box, x, y, z, red());
            }
            if (Math.abs(o) <= 4) this.set(level, box, x, f + 6, z, red());
            if (Math.abs(o) <= 4) this.set(level, box, x, f + 7, z, TILE_SLAB);
            if (Math.abs(o) == 2) this.set(level, box, x, f + 5, z, lantern().setValue(LanternBlock.HANGING, true));
            if (Math.abs(o) == 5) {
                Direction out = acrossX ? (o > 0 ? Direction.EAST : Direction.WEST) : (o > 0 ? Direction.SOUTH : Direction.NORTH);
                this.set(level, box, x, f + 7, z, stair(TILE_STAIRS, out, false));
            }
        }
        // a dark plaque on the beam's middle
        this.set(level, box, cx, f + 6, cz, st(Blocks.BLACK_GLAZED_TERRACOTTA));
    }

    // ---------------------------------------------------------------- the courtyard houses

    void plotSet(WorldGenLevel level, BoundingBox box, Plot p, int px, int y, int pz, BlockState state) {
        this.set(level, box, p.x(px), y, p.z(pz), state);
    }

    void buildHouse(WorldGenLevel level, BoundingBox box, Plot p, int f, Block workstation, RandomSource random) {
        Direction front = p.dir(Direction.SOUTH), back = front.getOpposite();
        // the hall: px 2..12, pz 1..8
        for (int px = 2; px <= 12; px++) {
            for (int pz = 1; pz <= 8; pz++) {
                boolean wall = px == 2 || px == 12 || pz == 1 || pz == 8;
                this.plotSet(level, box, p, px, f, pz, wall ? FOOTING : st(Blocks.DARK_OAK_PLANKS));
                for (int y = f + 1; y <= f + 4; y++) {
                    BlockState s;
                    if (!wall) s = st(Blocks.AIR);
                    else if (y == f + 1) s = FOOTING;
                    else s = white();
                    boolean corner = (px == 2 || px == 12) && (pz == 1 || pz == 8);
                    boolean pillar = corner || pz == 8 && (px == 5 || px == 9);
                    if (pillar) s = red();
                    this.plotSet(level, box, p, px, y, pz, s);
                }
                this.plotSet(level, box, p, px, f + 5, pz, BEAM);
            }
        }
        // lattice windows along the front, two on each end wall, one at the back
        BlockState lattice = st(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.OPEN, true);
        for (int px : new int[]{3, 4, 10, 11}) {
            for (int y = f + 2; y <= f + 3; y++) this.plotSet(level, box, p, px, y, 8, lattice.setValue(TrapDoorBlock.FACING, front));
        }
        for (int pz : new int[]{4, 5}) {
            for (int y = f + 2; y <= f + 3; y++) {
                this.plotSet(level, box, p, 2, y, pz, lattice.setValue(TrapDoorBlock.FACING, Direction.WEST));
                this.plotSet(level, box, p, 12, y, pz, lattice.setValue(TrapDoorBlock.FACING, Direction.EAST));
            }
        }
        for (int y = f + 2; y <= f + 3; y++) this.plotSet(level, box, p, 7, y, 1, lattice.setValue(TrapDoorBlock.FACING, back));
        // the red door
        BlockState door = st(Blocks.MANGROVE_DOOR).setValue(DoorBlock.FACING, front).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.plotSet(level, box, p, 7, f + 1, 8, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.plotSet(level, box, p, 7, f + 2, 8, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // the roof: ridge along x, eaves one block out all round
        int[] rise = {5, 6, 7, 8, 9, 9, 8, 7, 6, 5};
        for (int pz = 0; pz <= 9; pz++) {
            Direction up = pz <= 4 ? front : back;
            for (int px = 1; px <= 13; px++) {
                this.plotSet(level, box, p, px, f + rise[pz], pz, stair(TILE_STAIRS, up, false));
            }
            // the gables: whitewash under the roof
            if (pz >= 2 && pz <= 7) {
                for (int y = f + 6; y < f + rise[pz]; y++) {
                    this.plotSet(level, box, p, 2, y, pz, white());
                    this.plotSet(level, box, p, 12, y, pz, white());
                }
            }
        }
        for (int px = 2; px <= 12; px++) {
            this.plotSet(level, box, p, px, f + 10, 4, TILE_SLAB);
            this.plotSet(level, box, p, px, f + 10, 5, TILE_SLAB);
        }
        // the ridge's curled ends and the eaves' upturned corners
        for (int pz : new int[]{4, 5}) {
            this.plotSet(level, box, p, 1, f + 10, pz, stair(TILE_STAIRS, Direction.EAST, false));
            this.plotSet(level, box, p, 13, f + 10, pz, stair(TILE_STAIRS, Direction.WEST, false));
        }
        for (int pz : new int[]{0, 9}) {
            this.plotSet(level, box, p, 1, f + 6, pz, stair(TILE_STAIRS, Direction.EAST, false));
            this.plotSet(level, box, p, 13, f + 6, pz, stair(TILE_STAIRS, Direction.WEST, false));
        }
        // inside: a bed, the workstation, a lantern, a potted bamboo
        Direction toBack = back;
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, toBack);
        this.plotSet(level, box, p, 4, f + 1, 3, bed.setValue(BedBlock.PART, BedPart.HEAD));
        this.plotSet(level, box, p, 4, f + 1, 4, bed.setValue(BedBlock.PART, BedPart.FOOT));
        BlockState ws = workstation.defaultBlockState();
        if (ws.hasProperty(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING)) {
            ws = ws.setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, front);
        }
        this.plotSet(level, box, p, 10, f + 1, 3, ws);
        this.plotSet(level, box, p, 10, f + 1, 6, st(Blocks.POTTED_BAMBOO));
        this.plotSet(level, box, p, 7, f + 4, 4, lantern().setValue(LanternBlock.HANGING, true));

        // the yard: walled, with a red gate in its front wall
        for (int px = 0; px <= 14; px++) {
            for (int pz = 2; pz <= 14; pz++) {
                boolean wall = (px == 0 || px == 14) || pz == 14;
                boolean inHall = px >= 2 && px <= 12 && pz <= 8;
                if (inHall) continue;
                if (!wall) {
                    if (pz >= 9) this.plotSet(level, box, p, px, f, pz, px == 7 ? FOOTING : st(Blocks.GRASS_BLOCK));
                    continue;
                }
                if (pz == 14 && px >= 6 && px <= 8) continue;
                this.plotSet(level, box, p, px, f + 1, pz, white());
                this.plotSet(level, box, p, px, f + 2, pz, white());
                this.plotSet(level, box, p, px, f + 3, pz, TILE_SLAB);
            }
        }
        for (int px : new int[]{6, 8}) {
            for (int y = f + 1; y <= f + 3; y++) this.plotSet(level, box, p, px, y, 14, red());
        }
        for (int px = 5; px <= 9; px++) {
            if (px >= 6 && px <= 8) this.plotSet(level, box, p, px, f + 4, 14, red());
            this.plotSet(level, box, p, px, f + 5, 14, TILE_SLAB);
        }
        for (int y = f + 1; y <= f + 3; y++) this.plotSet(level, box, p, 7, y, 14, st(Blocks.AIR));
        this.plotSet(level, box, p, 7, f, 14, FOOTING);
        this.plotSet(level, box, p, 7, f + 3, 14, lantern().setValue(LanternBlock.HANGING, true));
        // a little magnolia in one corner of the yard, a clump of bamboo in the other
        boolean treeLeft = random.nextBoolean();
        int tx = treeLeft ? 3 : 11, bx = treeLeft ? 12 : 2;
        BlockState log = ModBlocks.MAGNOLIA_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState leaves = ModBlocks.MAGNOLIA_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 1; dz++) {
                for (int dy = 3; dy <= 5; dy++) {
                    int d = dx * dx + dz * dz + (dy - 4) * (dy - 4) * 2;
                    if (d > 5) continue;
                    int px = tx + dx, pz = 11 + dz;
                    if (px <= 0 || px >= 14) continue;
                    this.plotSet(level, box, p, px, f + dy, pz, leaves);
                }
            }
        }
        for (int y = f + 1; y <= f + 4; y++) this.plotSet(level, box, p, tx, y, 11, log);
        for (int[] b : new int[][]{{bx, 12}, {bx, 13}, {bx + (treeLeft ? -1 : 1), 13}}) {
            this.bamboo(level, box, p.x(b[0]), p.z(b[1]), f, 4 + random.nextInt(3));
        }
    }

    void bamboo(WorldGenLevel level, BoundingBox box, int x, int z, int f, int height) {
        for (int i = 0; i < height; i++) {
            BambooLeaves leaves = i >= height - 1 ? BambooLeaves.LARGE : i >= height - 2 ? BambooLeaves.SMALL : BambooLeaves.NONE;
            this.set(level, box, x, f + 1 + i, z, Blocks.BAMBOO.defaultBlockState()
                    .setValue(BambooStalkBlock.LEAVES, leaves).setValue(BambooStalkBlock.AGE, 1));
        }
    }

    // ---------------------------------------------------------------- the pagoda

    void buildPagoda(WorldGenLevel level, BoundingBox box, Plot p, int f) {
        int cx = p.x(7), cz = p.z(7);
        int[] hw = {5, 4, 4, 3, 2};
        int[] height = {5, 4, 4, 4, 4};
        // a stone terrace under it
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) this.set(level, box, cx + dx, f, cz + dz, FOOTING);
        }
        Direction front = p.dir(Direction.SOUTH);
        int base = f;
        for (int k = 0; k < hw.length; k++) {
            int h = hw[k], top = base + height[k];
            for (int dx = -h; dx <= h; dx++) {
                for (int dz = -h; dz <= h; dz++) {
                    boolean edge = Math.max(Math.abs(dx), Math.abs(dz)) == h;
                    boolean corner = Math.abs(dx) == h && Math.abs(dz) == h;
                    boolean mid = edge && (dx == 0 || dz == 0);
                    for (int y = base + 1; y < top; y++) {
                        BlockState s = st(Blocks.AIR);
                        if (corner) s = red();
                        else if (edge) s = y == base + 1 && k == 0 ? FOOTING : white();
                        if (mid && y > base + 1 && y < top - 1 && k > 0) {
                            Direction out = Math.abs(dx) == h ? (dx > 0 ? Direction.EAST : Direction.WEST) : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
                            s = st(Blocks.SPRUCE_TRAPDOOR).setValue(TrapDoorBlock.OPEN, true).setValue(TrapDoorBlock.FACING, out);
                        }
                        this.set(level, box, cx + dx, y, cz + dz, s);
                    }
                    // the floor (and ceiling) of each storey
                    this.set(level, box, cx + dx, top, cz + dz, edge ? BEAM : st(Blocks.DARK_OAK_PLANKS));
                }
            }
            // its skirt of tiles
            this.roofRing(level, box, cx, cz, h + 1, top);
            this.roofRing(level, box, cx, cz, h + 2, top - 1);
            this.upturnedCorners(level, box, cx, cz, h + 2, top - 1);
            base = top;
        }
        // the crowning roof and the golden finial
        int h = hw[hw.length - 1];
        for (int r = h + 1; r >= 1; r--) this.roofRing(level, box, cx, cz, r, base + (h + 1 - r));
        int peak = base + h + 1;
        this.set(level, box, cx, peak - 1, cz, TILES);
        this.set(level, box, cx, peak, cz, st(Blocks.GOLD_BLOCK));
        this.set(level, box, cx, peak + 1, cz, st(Blocks.CHAIN));
        this.set(level, box, cx, peak + 2, cz, st(Blocks.CHAIN));
        this.set(level, box, cx, peak + 3, cz, st(Blocks.LIGHTNING_ROD));
        // the door on the ground floor, facing the lane
        int dx = front.getStepX() * 5, dz = front.getStepZ() * 5;
        for (int y = f + 1; y <= f + 3; y++) this.set(level, box, cx + dx, y, cz + dz, st(Blocks.AIR));
        BlockState door = st(Blocks.MANGROVE_DOOR).setValue(DoorBlock.FACING, front).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.set(level, box, cx + dx, f + 1, cz + dz, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.set(level, box, cx + dx, f + 2, cz + dz, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // inside: the priest's brewing stand, his bed and a lantern
        this.set(level, box, cx, f + 1, cz, st(Blocks.BREWING_STAND));
        BlockState bed = Blocks.YELLOW_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.WEST);
        this.set(level, box, cx + 3, f + 1, cz - 3, bed.setValue(BedBlock.PART, BedPart.FOOT));
        this.set(level, box, cx + 2, f + 1, cz - 3, bed.setValue(BedBlock.PART, BedPart.HEAD));
        this.set(level, box, cx, f + 4, cz, lantern().setValue(LanternBlock.HANGING, true));
    }

    // ---------------------------------------------------------------- bamboo round the village

    void bambooRing(WorldGenLevel level, BoundingBox box, int f, RandomSource random) {
        for (int a = -CORE; a <= CORE; a++) {
            for (int[] p : new int[][]{{a, -CORE}, {a, CORE}, {-CORE, a}, {CORE, a}}) {
                if (Math.abs(p[0]) <= 6 || Math.abs(p[1]) <= 6) continue;
                if (random.nextInt(5) < 2) continue;
                this.set(level, box, p[0], f, p[1], st(Blocks.PODZOL));
                this.bamboo(level, box, p[0], p[1], f, 5 + random.nextInt(6));
            }
        }
    }

    // ---------------------------------------------------------------- the rice paddies

    void buildPaddy(WorldGenLevel level, BoundingBox box, int[] field, int f, RandomSource random) {
        int x0 = field[0], z0 = field[1], x1 = field[2], z1 = field[3];
        boolean alongX = (x1 - x0) >= (z1 - z0);
        RiceBlock rice = ModBlocks.RICE.get();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int line = alongX ? z - z0 : x - x0;
                int along = alongX ? x - x0 : z - z0;
                int length = alongX ? x1 - x0 : z1 - z0;
                boolean bank = x == x0 || x == x1 || z == z0 || z == z1 || line % 5 == 0 || along == length / 2;
                if (bank) {
                    this.set(level, box, x, f, z, st(Blocks.GRASS_BLOCK));
                    continue;
                }
                this.set(level, box, x, f - 1, z, st(Blocks.MUD));
                long h = (x * 341873128712L + z * 132897987541L) ^ this.seed;
                int roll = (int) ((h >>> 20) & 0xFF);
                BlockState s = roll % 10 < 8 ? rice.getStateForAge(2 + roll % (rice.getMaxAge() - 1)) : st(Blocks.WATER);
                this.set(level, box, x, f, z, s);
            }
        }
        this.set(level, box, x0 - 1, f + 1, z0 - 1, st(Blocks.COMPOSTER));
    }

    // ---------------------------------------------------------------- villagers and animals

    void spawnLife(WorldGenLevel level, BoundingBox box, int f) {
        RandomSource random = RandomSource.create(this.seed * 31 + 7);
        for (Plot p : plots()) {
            if (isPagodaPlot(p)) {
                this.spawnVillager(level, box, this.at(p.x(5), f + 1, p.z(7)));
            } else {
                this.spawnVillager(level, box, this.at(p.x(7), f + 1, p.z(5)));
            }
        }
        for (int[] field : FIELDS) {
            int x0 = field[0] - 2, z0 = field[1] - 2, x1 = field[2] + 2, z1 = field[3] + 2;
            EntityType<?>[] kinds = {EntityType.PIG, EntityType.CHICKEN, EntityType.PIG, EntityType.CHICKEN};
            for (int i = 0; i < 4; i++) {
                int x, z;
                if (random.nextBoolean()) {
                    x = x0 + random.nextInt(x1 - x0 + 1);
                    z = random.nextBoolean() ? z0 : z1;
                } else {
                    x = random.nextBoolean() ? x0 : x1;
                    z = z0 + random.nextInt(z1 - z0 + 1);
                }
                this.spawn(level, box, kinds[i], this.at(x, f + 1, z));
            }
        }
        // a few cats about the square
        for (int i = 0; i < 4; i++) this.spawn(level, box, EntityType.CAT, this.at(random.nextInt(11) - 5, f + 1, random.nextBoolean() ? 6 : -6));
    }

    void spawnVillager(WorldGenLevel level, BoundingBox box, BlockPos pos) {
        if (!box.isInside(pos)) return;
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) return;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillageRegistry.CHINESE.get()).setProfession(VillagerProfession.NONE));
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
