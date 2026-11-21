package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
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
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
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
 * "Chernobyl": an abandoned Soviet housing estate (a mikrorayon of the 1970s) in the subpolar taiga and the tundra,
 * long since left to the forest and its mutants. Turned one of four ways at random.
 * <ul>
 *   <li>four ten-storey panel blocks round a courtyard, of grey panels with their seams showing, rows of windows (many
 *       broken), balconies (some glazed in, washing hung out on some), entrances under canopies with benches by them,
 *       aerials and lift motor rooms on the flat roofs; on one gable a mosaic of the peaceful atom;</li>
 *   <li>inside each entrance a stairwell climbing all ten floors to the roof, two flats off every landing: parquet,
 *       a rug on the wall, a bed, a wardrobe, a table, a stove, a sink, a television - and dust, cobwebs, damp;</li>
 *   <li>in the courtyard a ring road (asphalt gone to cracks and weeds, the cars still parked on it, lamp posts), a
 *       kindergarten, a playground, a row of garages, a kiosk, the yellow Ferris wheel that never turned; birches
 *       everywhere; the heating main on its posts, arching over the paths; the boiler house with its striped chimney;
 *       a bus stop with a mosaic out on the road;</li>
 *   <li>its treasures: a Mosin-Nagant hidden in a chest in one block's lift motor room up on the roof
 *       (chests/chernobyl_rooftop); an AK-47 in a chest in one of the garages (chests/chernobyl_garage); and in the
 *       basement of the north block, behind an iron door at the end of its corridor, the Graphite ring
 *       (chests/chernobyl_vault) - guarded by the mutant dog;</li>
 *   <li>mutant zombies everywhere, in the flats and the courtyard (and more keep coming: event/ChernobylLife, which
 *       also turns the sky green over the estate).</li>
 * </ul>
 */
public class ChernobylPiece extends StructurePiece {
    static final ResourceKey<LootTable> FLAT = loot("chests/chernobyl_flat");
    static final ResourceKey<LootTable> ROOFTOP = loot("chests/chernobyl_rooftop");
    static final ResourceKey<LootTable> GARAGE = loot("chests/chernobyl_garage");
    static final ResourceKey<LootTable> VAULT = loot("chests/chernobyl_vault");

    static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, path));
    }

    // the panel blocks: length, depth, storeys of three
    static final int L = 44, D = 12, STOREYS = 10, H = 3, ROOF = STOREYS * H;
    // the two entrances (stairwells) of each block, by their position along it
    static final int[] ENTRANCES = {10, 32};
    // the estate's ground (levelled), the margin it's blended back over
    static final int X0 = -56, X1 = 56, Z0 = -56, Z1 = 64, MARGIN = 12, HEIGHT = 42;

    /** A panel block: its front left corner, the unit along its front, the unit from its front to its back. */
    record Block10(int ox, int oz, int ux, int uz, int vx, int vz, boolean basement, int roofChest) {
        int x(int u, int v) {
            return this.ox + u * this.ux + v * this.vx;
        }

        int z(int u, int v) {
            return this.oz + u * this.uz + v * this.vz;
        }

        Direction dir(int du, int dv) {
            return Direction.getNearest(du * this.ux + dv * this.vx, 0, du * this.uz + dv * this.vz);
        }
    }

    static final Block10[] BLOCKS = {
            new Block10(-22, -39, 1, 0, 0, -1, true, -1),      // the north block, its front to the south
            new Block10(21, 38, -1, 0, 0, 1, false, -1),       // the south block
            new Block10(-39, 21, 0, -1, -1, 0, false, 1),      // the west block (the Mosin on its roof)
            new Block10(38, -22, 0, 1, 1, 0, false, -1)};      // the east block

    final BlockPos origin;
    final long seed;
    final int turn;

    public ChernobylPiece(BlockPos origin, long seed, int turn) {
        super(VillageRegistry.CHERNOBYL_PIECE.get(), 0, box(origin));
        this.origin = origin;
        this.seed = seed;
        this.turn = turn;
    }

    public ChernobylPiece(CompoundTag tag) {
        super(VillageRegistry.CHERNOBYL_PIECE.get(), tag);
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

    static BoundingBox box(BlockPos origin) {
        int r = Math.max(Math.max(-X0, X1), Math.max(-Z0, Z1)) + MARGIN;
        return new BoundingBox(origin.getX() - r, origin.getY() - 8, origin.getZ() - r, origin.getX() + r, origin.getY() + HEIGHT,
                origin.getZ() + r);
    }

    public BlockPos origin() {
        return this.origin;
    }

    // ================================================================ placing blocks (in the estate's own frame)

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

    boolean freeAt(WorldGenLevel level, int x, int y, int z) {
        return this.get(level, x, y, z).isAir();
    }

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

    static BlockState pane(Block b, Direction.Axis along) {
        boolean x = along == Direction.Axis.X;
        return b.defaultBlockState().setValue(CrossCollisionBlock.EAST, x).setValue(CrossCollisionBlock.WEST, x)
                .setValue(CrossCollisionBlock.NORTH, !x).setValue(CrossCollisionBlock.SOUTH, !x);
    }

    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        REACH.set(box.inflatedBy(15, 0, 15));
        this.shapeGround(level, box);
        LooseTreeCleanup.clean(level, chunkPos, this.origin.getY() - 6, this.origin.getY() + HEIGHT,
                (wx, wz) -> {
                    int[] l = this.local(wx, wz);
                    return outside(l[0], l[1]) < MARGIN + 6;
                },
                (wx, wz) -> {
                    int[] l = this.local(wx, wz);
                    return outside(l[0], l[1]) <= 0;
                });
        this.grounds(level, box);
        for (Block10 b : BLOCKS) {
            if (this.touches(b, box)) this.panelBlock(level, box, b);
        }
        this.mosaic(level, box);
        this.kindergarten(level, box);
        this.playground(level, box);
        this.ferrisWheel(level, box);
        this.garages(level, box);
        this.boilerHouse(level, box);
        this.heatingMain(level, box);
        this.busStop(level, box);
        this.kiosk(level, box);
        this.cars(level, box);
        this.trees(level, box);
        this.inhabitants(level, box);
    }

    static double outside(int x, int z) {
        double dx = Math.max(Math.max(X0 - x, x - X1), 0), dz = Math.max(Math.max(Z0 - z, z - Z1), 0);
        return Math.sqrt(dx * dx + dz * dz);
    }

    boolean touches(Block10 b, BoundingBox box) {
        int ax = b.x(0, 0), az = b.z(0, 0), bx = b.x(L - 1, D - 1), bz = b.z(L - 1, D - 1);
        BlockPos p = this.at(Math.min(ax, bx) - 4, 0, Math.min(az, bz) - 4), q = this.at(Math.max(ax, bx) + 4, 0, Math.max(az, bz) + 4);
        return Math.max(p.getX(), q.getX()) >= box.minX() && Math.min(p.getX(), q.getX()) <= box.maxX()
                && Math.max(p.getZ(), q.getZ()) >= box.minZ() && Math.min(p.getZ(), q.getZ()) <= box.maxZ();
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** The estate's ground: levelled, cleared, blended back into the land round it. */
    void shapeGround(WorldGenLevel level, BoundingBox box) {
        int f = this.origin.getY() - 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int[] l = this.local(wx, wz);
                double dist = outside(l[0], l[1]);
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
            }
        }
    }

    // ================================================================ the courtyard and the roads

    // the ring road round the courtyard, and the road out of it to the south-east
    static boolean road(int x, int z) {
        boolean ring = x >= -36 && x <= 35 && z >= -35 && z <= 34 && !(x > -32 && x < 31 && z > -31 && z < 30);
        boolean out = x >= 26 && x <= 30 && z > 30 && z <= Z1;
        return ring || out;
    }

    static boolean sidewalk(int x, int z) {
        boolean ring = x >= -38 && x <= 37 && z >= -37 && z <= 36 && !(x > -30 && x < 29 && z > -29 && z < 28);
        boolean out = x >= 24 && x <= 32 && z > 30 && z <= Z1;
        return (ring || out) && !road(x, z);
    }

    /** Asphalt gone to cracks, weeds and puddles; the pavements of cracked slabs; tall grass and weeds everywhere else. */
    void grounds(WorldGenLevel level, BoundingBox box) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                if (!box.isInside(this.at(x, 0, z))) continue;
                int r = this.roll(x, -1, z, 100);
                if (road(x, z)) {
                    BlockState s = r < 6 ? st(Blocks.GRASS_BLOCK) : r < 9 ? st(Blocks.MOSS_BLOCK) : r < 11 ? st(Blocks.GRAVEL)
                            : r < 13 ? st(Blocks.WATER) : r < 45 ? st(Blocks.BLACKSTONE) : st(Blocks.POLISHED_BLACKSTONE);
                    this.set(level, box, x, -1, z, s);
                    if (s.is(Blocks.WATER)) this.set(level, box, x, -2, z, st(Blocks.BLACKSTONE));
                    if (s.is(Blocks.GRASS_BLOCK) && r < 3) this.set(level, box, x, 0, z, st(Blocks.SHORT_GRASS));
                    // the white lines down the middle of the road
                    if (s.is(Blocks.POLISHED_BLACKSTONE) && this.centreLine(x, z) && r % 3 != 0) this.set(level, box, x, -1, z, st(Blocks.SMOOTH_QUARTZ));
                } else if (sidewalk(x, z)) {
                    this.set(level, box, x, -1, z, r < 8 ? st(Blocks.GRASS_BLOCK) : r < 30 ? st(Blocks.CRACKED_STONE_BRICKS)
                            : r < 40 ? st(Blocks.ANDESITE) : st(Blocks.SMOOTH_STONE));
                } else {
                    this.set(level, box, x, -1, z, r < 10 ? st(Blocks.PODZOL) : r < 14 ? st(Blocks.COARSE_DIRT) : st(Blocks.GRASS_BLOCK));
                    if (r < 30 && this.freeAt(level, x, 0, z)) this.set(level, box, x, 0, z, st(Blocks.SHORT_GRASS));
                    else if (r < 33 && this.freeAt(level, x, 0, z)) {
                        this.set(level, box, x, 0, z, Blocks.TALL_GRASS.defaultBlockState());
                        this.set(level, box, x, 1, z, Blocks.TALL_GRASS.defaultBlockState()
                                .setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
                    } else if (r < 35 && this.freeAt(level, x, 0, z)) this.set(level, box, x, 0, z, st(Blocks.FERN));
                    else if (r == 35 && this.freeAt(level, x, 0, z)) this.set(level, box, x, 0, z, st(Blocks.DEAD_BUSH));
                }
            }
        }
        // the lamp posts round the ring road: a grey post, an arm, a lamp (long dead, some still glowing)
        for (int i = -32; i <= 30; i += 12) {
            this.lampPost(level, box, i, -30, Direction.NORTH);
            this.lampPost(level, box, i, 29, Direction.SOUTH);
            this.lampPost(level, box, -31, i, Direction.WEST);
            this.lampPost(level, box, 30, i, Direction.EAST);
        }
    }

    boolean centreLine(int x, int z) {
        boolean along = (z == -33 || z == 32) && x > -34 && x < 33, across = (x == -34 || x == 33) && z > -33 && z < 32;
        return (along && Math.floorMod(x, 4) < 2) || (across && Math.floorMod(z, 4) < 2) || (x == 28 && z > 34 && Math.floorMod(z, 4) < 2);
    }

    void lampPost(WorldGenLevel level, BoundingBox box, int x, int z, Direction arm) {
        for (int y = 0; y <= 5; y++) this.set(level, box, x, y, z, st(Blocks.ANDESITE_WALL));
        int ax = x + arm.getStepX(), az = z + arm.getStepZ();
        this.set(level, box, ax, 5, az, pane(Blocks.IRON_BARS, arm.getAxis()));
        this.set(level, box, ax, 4, az, this.roll(x, 4, z, 3) == 0 ? st(Blocks.AIR)
                : Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    // ================================================================ the panel blocks

    /**
     * One ten-storey panel block: its walls of grey panels, windows, balconies, entrances, stairwells, flats, roof -
     * and the north block's basement, with the vault at the end of its corridor.
     */
    void panelBlock(WorldGenLevel level, BoundingBox box, Block10 b) {
        int bottom = b.basement() ? -4 : -1;
        for (int u = 0; u < L; u++) {
            for (int v = 0; v < D; v++) {
                int x = b.x(u, v), z = b.z(u, v);
                if (!box.isInside(this.at(x, 0, z))) continue;
                for (int y = bottom; y <= ROOF + 1; y++) this.set(level, box, x, y, z, this.shell(b, u, v, y));
            }
        }
        for (int e = 0; e < ENTRANCES.length; e++) this.stairwell(level, box, b, ENTRANCES[e], e);
        this.flats(level, box, b);
        this.balconies(level, box, b);
        this.roof(level, box, b);
        if (b.basement()) this.basement(level, box, b);
    }

    static boolean stairwellAt(int u) {
        for (int c : ENTRANCES) if (u >= c - 3 && u <= c + 2) return true;
        return false;
    }

    // the flats along the block: between the ends and the stairwells, the middle stretch split in two
    static final int[][] FLATS = {{1, 6}, {13, 20}, {22, 28}, {35, 42}};

    /** The block's shell, cell by cell: plinth, walls of panels, windows, floors, roof, parapet. */
    BlockState shell(Block10 b, int u, int v, int y) {
        boolean wall = u == 0 || u == L - 1 || v == 0 || v == D - 1;
        if (y < 0) {
            if (!b.basement()) return y == -1 ? st(Blocks.STONE_BRICKS) : null;
            if (y == -4 || wall) return st(Blocks.STONE_BRICKS);
            return AIR;
        }
        if (y == ROOF) return st(Blocks.SMOOTH_STONE);
        if (y == ROOF + 1) return wall ? slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM) : AIR;
        int s = y / H, k = y % H;
        boolean front = v == 0, back = v == D - 1;
        if (!wall) {
            if (k == 0) return st(y == 0 ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE);
            // the partitions between the flats and the stairwells; across each flat, between its front and back rooms
            for (int c : ENTRANCES) if (u == c - 3 || u == c + 2) return this.panel(u, y, v);
            if (u == 21) return st(Blocks.SMOOTH_STONE);
            if (v == 6 && !stairwellAt(u)) return (Math.floorMod(u, 7) == 3 && k >= 1) ? AIR : st(Blocks.BIRCH_PLANKS);
            return AIR;
        }
        // windows: pairs on the long sides, two to a flat room; narrow ones lighting the stairwells half a storey up
        if (front || back) {
            if (stairwellAt(u)) {
                for (int c : ENTRANCES) {
                    if (front && s == 0 && (u == c - 1 || u == c) && k >= 1) return AIR;   // the entrance (its doors put in later)
                    if ((u == c - 1 || u == c) && k == 2 && s < STOREYS) return this.glass(u, y, v, b);
                }
                return this.panel(u, y, v);
            }
            int m = Math.floorMod(u, 4);
            if ((m == 1 || m == 2) && k >= 1 && u > 0 && u < L - 1) return this.glass(u, y, v, b);
            return this.panel(u, y, v);
        }
        // the gable ends: blank panels
        return this.panel(u, y, v);
    }

    /** A grey wall panel - its seams showing between, a stain or a crack here and there. */
    BlockState panel(int u, int y, int v) {
        if (y < H) return this.roll(u, y, v, 6) == 0 ? st(Blocks.CRACKED_STONE_BRICKS) : st(Blocks.STONE_BRICKS);
        if (y % H == 0 || Math.floorMod(u, 4) == 0) return st(Blocks.ANDESITE);
        int r = this.roll(u, y, v, 40);
        return r == 0 ? st(Blocks.TUFF) : r == 1 ? st(Blocks.MOSSY_STONE_BRICKS) : st(Blocks.POLISHED_ANDESITE);
    }

    /** A window: a pane of glass, or (one in four) broken out. */
    BlockState glass(int u, int y, int v, Block10 b) {
        if (this.roll(u, y, v + b.ox() * 7, 4) == 0) return AIR;
        Direction along = b.dir(1, 0);
        return pane(Blocks.GLASS_PANE, along.getAxis());
    }

    /**
     * A stairwell, two lanes wide, climbing all ten floors and on to the roof: each storey one flight of three steps,
     * up one lane from the front landing to the back landing, then up the other lane back to the front; the flats'
     * doors off both landings. The entrance at the front under its canopy, a bench either side, a dead lamp.
     */
    void stairwell(WorldGenLevel level, BoundingBox box, Block10 b, int c, int index) {
        int laneL = c - 2, laneR = c;
        int top = STOREYS;
        // the north block's basement stair (down the right lane, from the ground floor's front landing)
        int s0 = b.basement() && index == 1 ? -1 : 0;
        for (int s = s0; s < top; s++) {
            boolean up = Math.floorMod(s, 2) == 0;           // even storeys climb toward the back on the left lane
            int lane = s < 0 ? laneR : up ? laneL : laneR;
            for (int i = 1; i <= 3; i++) {
                int v = up && s >= 0 ? 3 + i : 7 - i;
                int y = s * H + i;
                Direction facing = up && s >= 0 ? b.dir(0, 1) : b.dir(0, -1);
                for (int du = 0; du <= 1; du++) {
                    int x = b.x(lane + du, v), z = b.z(lane + du, v);
                    this.set(level, box, x, y, z, stair(Blocks.STONE_BRICK_STAIRS, facing, false));
                    this.set(level, box, x, y - 1, z, stair(Blocks.STONE_BRICK_STAIRS, facing.getOpposite(), true));
                    // headroom: nothing in the two blocks over a step
                    for (int h = 1; h <= 2; h++) {
                        BlockState above = this.get(level, x, y + h, z);
                        if (!above.isAir() && !above.is(Blocks.STONE_BRICK_STAIRS)) this.set(level, box, x, y + h, z, AIR);
                    }
                }
            }
            // and over the landing's edge where the flight starts: the floor above it would catch your head as you
            // step up onto the first stair
            int startV = up && s >= 0 ? 3 : 7;
            for (int du = 0; du <= 1; du++) {
                int x = b.x(lane + du, startV), z = b.z(lane + du, startV), y = s * H + H;
                BlockState above = this.get(level, x, y, z);
                if (!above.isAir() && !above.is(Blocks.STONE_BRICK_STAIRS)) this.set(level, box, x, y, z, AIR);
            }
        }
        // the flats' doors off the landings (front and back), on every floor
        for (int s = 0; s < top; s++) {
            int y = s * H + 1;
            for (int v : new int[]{2, 8}) {
                for (int side = 0; side <= 1; side++) {
                    int u = side == 0 ? c - 3 : c + 2;
                    if (u <= 0 || u >= L - 1) continue;
                    Direction facing = b.dir(side == 0 ? -1 : 1, 0);
                    BlockState door = (this.roll(u, y, v + index, 5) == 0 ? Blocks.AIR : Blocks.SPRUCE_DOOR).defaultBlockState();
                    if (door.isAir()) {
                        this.set(level, box, b.x(u, v), y, b.z(u, v), AIR);
                        this.set(level, box, b.x(u, v), y + 1, b.z(u, v), AIR);
                    } else {
                        boolean open = this.roll(u, y, v, 2) == 0;
                        this.set(level, box, b.x(u, v), y, b.z(u, v), door.setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, open)
                                .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
                        this.set(level, box, b.x(u, v), y + 1, b.z(u, v), door.setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, open)
                                .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
                    }
                }
            }
            // a hanging light on each landing (most of them long dead), cobwebs in the corners
            if (this.roll(c, y, index, 3) == 0) this.set(level, box, b.x(c - 1, 2), y + 1, b.z(c - 1, 2),
                    Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            if (this.roll(c, y, index, 4) == 0) this.set(level, box, b.x(c + 1, 10), y + 1, b.z(c + 1, 10), st(Blocks.COBWEB));
            // the post boxes and the radiator on the ground floor
            if (s == 0) {
                this.set(level, box, b.x(c + 1, 1), 1, b.z(c + 1, 1), st(Blocks.CHISELED_BOOKSHELF));
                this.set(level, box, b.x(c - 2, 1), 1, b.z(c - 2, 1), st(Blocks.IRON_BARS));
            }
        }
        // the entrance: a pair of doors, the canopy over them, benches either side, a lamp
        Direction out = b.dir(0, -1);
        for (int du = 0; du <= 1; du++) {
            int u = c - 1 + du;
            // each leaf hinged on its outer side (a door's left is its facing's counter-clockwise side), so the pair
            // open back against the jambs
            Direction outer = b.dir(du == 0 ? -1 : 1, 0);
            DoorHingeSide hinge = out.getCounterClockWise() == outer ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
            BlockState door = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, out).setValue(DoorBlock.HINGE, hinge);
            this.set(level, box, b.x(u, 0), 1, b.z(u, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(level, box, b.x(u, 0), 2, b.z(u, 0), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }
        for (int u = c - 3; u <= c + 2; u++) {
            for (int v = -2; v <= -1; v++) this.set(level, box, b.x(u, v), 3, b.z(u, v), slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
        }
        for (int v = -2; v <= -1; v++) {
            this.set(level, box, b.x(c - 3, v), 1, b.z(c - 3, v), stair(Blocks.SPRUCE_STAIRS, b.dir(-1, 0), false));
            this.set(level, box, b.x(c + 2, v), 1, b.z(c + 2, v), stair(Blocks.SPRUCE_STAIRS, b.dir(1, 0), false));
        }
        this.set(level, box, b.x(c - 1, -1), 2, b.z(c - 1, -1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        // the stair's way out onto the roof: the motor room over the stairwell, its door
        int ry = ROOF;
        for (int u = c - 3; u <= c + 2; u++) {
            for (int v = 0; v <= 7; v++) {
                boolean wall = u == c - 3 || u == c + 2 || v == 0 || v == 7;
                for (int y = ry + 1; y <= ry + 4; y++) {
                    BlockState s = y == ry + 4 ? st(Blocks.SMOOTH_STONE) : wall ? this.panel(u, y, v) : AIR;
                    this.set(level, box, b.x(u, v), y, b.z(u, v), s);
                }
            }
        }
        // the last flight comes up to the roof itself: open the roof over it, and the landing at roof level
        for (int u = laneL; u <= laneR + 1; u++) {
            for (int v = 1; v <= 6; v++) {
                if (v >= 4 && u >= laneR) continue;
                if (v >= 1 && v <= 3) this.set(level, box, b.x(u, v), ry, b.z(u, v), st(Blocks.SMOOTH_STONE));
            }
        }
        for (int u = laneR; u <= laneR + 1; u++) for (int v = 5; v <= 6; v++) this.set(level, box, b.x(u, v), ry, b.z(u, v), AIR);
        // the motor room's door onto the roof, its old machinery
        Direction side = b.dir(-1, 0);
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, side).setValue(DoorBlock.OPEN, true);
        this.set(level, box, b.x(c - 3, 2), ry + 1, b.z(c - 3, 2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.set(level, box, b.x(c - 3, 2), ry + 2, b.z(c - 3, 2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        this.set(level, box, b.x(c - 2, 6), ry + 1, b.z(c - 2, 6), st(Blocks.IRON_BLOCK));
        this.set(level, box, b.x(c - 1, 6), ry + 1, b.z(c - 1, 6), st(Blocks.GRINDSTONE));
        this.set(level, box, b.x(c - 2, 6), ry + 2, b.z(c - 2, 6), st(Blocks.CHAIN));
        if (b.roofChest() == index) this.chest(level, box, b.x(c + 1, 1), ry + 1, b.z(c + 1, 1), b.dir(-1, 0), ROOFTOP);
    }

    /** The flats on every floor: parquet, a rug on the wall, the furniture of a life left in a hurry. */
    void flats(WorldGenLevel level, BoundingBox box, Block10 b) {
        for (int s = 0; s < STOREYS; s++) {
            int y = s * H + 1;
            for (int f = 0; f < FLATS.length; f++) {
                int u0 = FLATS[f][0], u1 = FLATS[f][1], salt = s * 7 + f * 3 + b.ox();
                // the floors: parquet, warped and missing here and there
                for (int u = u0; u <= u1; u++) {
                    for (int v = 1; v <= D - 2; v++) {
                        if (v == 6) continue;
                        int r = this.roll(u, y - 1, v + b.oz(), 30);
                        this.set(level, box, b.x(u, v), y - 1, b.z(u, v), r == 0 ? st(Blocks.COARSE_DIRT) : r < 3 ? st(Blocks.SPRUCE_PLANKS)
                                : st(Blocks.OAK_PLANKS));
                    }
                }
                // the front room: the rug on the wall (on the partition), a sofa, a table, the television
                int mid = (u0 + u1) / 2;
                for (int u = Math.max(u0, mid - 1); u <= Math.min(u1, mid + 1); u++) {
                    this.set(level, box, b.x(u, 6), y, b.z(u, 6), st(u == mid ? Blocks.RED_GLAZED_TERRACOTTA : Blocks.RED_TERRACOTTA));
                    this.set(level, box, b.x(u, 6), y + 1, b.z(u, 6), st(u == mid ? Blocks.ORANGE_GLAZED_TERRACOTTA : Blocks.RED_TERRACOTTA));
                }
                this.set(level, box, b.x(mid, 5), y, b.z(mid, 5), stair(Blocks.RED_WOOL.defaultBlockState().is(Blocks.RED_WOOL) ? Blocks.SPRUCE_STAIRS
                        : Blocks.SPRUCE_STAIRS, b.dir(0, 1), false));
                this.set(level, box, b.x(mid, 3), y, b.z(mid, 3), st(Blocks.SPRUCE_FENCE));
                this.set(level, box, b.x(mid, 3), y + 1, b.z(mid, 3), this.roll(mid, y, salt, 3) == 0 ? st(Blocks.FLOWER_POT) : st(Blocks.SPRUCE_PRESSURE_PLATE));
                this.set(level, box, b.x(u1, 1), y, b.z(u1, 1), st(Blocks.JUKEBOX));
                this.set(level, box, b.x(u0, 1), y, b.z(u0, 1), st(Blocks.BOOKSHELF));
                // the back room: bed, wardrobe, stove and sink
                Block bed = switch (Math.floorMod(salt, 4)) {
                    case 0 -> Blocks.RED_BED;
                    case 1 -> Blocks.BLUE_BED;
                    case 2 -> Blocks.BROWN_BED;
                    default -> Blocks.GREEN_BED;
                };
                Direction head = b.dir(0, 1);
                this.set(level, box, b.x(u0, D - 2), y, b.z(u0, D - 2), bed.defaultBlockState().setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.HEAD));
                this.set(level, box, b.x(u0, D - 3), y, b.z(u0, D - 3), bed.defaultBlockState().setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.FOOT));
                this.container(level, box, b.x(u0 + 1, D - 2), y, b.z(u0 + 1, D - 2), b.dir(0, -1), this.roll(u0, y, salt, 3) == 0);
                this.set(level, box, b.x(u0 + 1, D - 2), y + 1, b.z(u0 + 1, D - 2), st(Blocks.BARREL));
                this.set(level, box, b.x(u1, D - 2), y, b.z(u1, D - 2), st(Blocks.SMOKER).setValue(net.minecraft.world.level.block.AbstractFurnaceBlock.FACING, b.dir(0, -1)));
                this.set(level, box, b.x(u1 - 1, D - 2), y, b.z(u1 - 1, D - 2), st(Blocks.CAULDRON));
                if (u1 - u0 > 4) this.set(level, box, b.x(u1 - 2, D - 2), y, b.z(u1 - 2, D - 2), st(Blocks.CRAFTING_TABLE));
                // dust and damp: cobwebs, a dead plant, moss creeping in, a hole in the floor
                for (int k = 0; k < 3; k++) {
                    int u = u0 + this.roll(salt, k, 1, u1 - u0 + 1), v = 1 + this.roll(salt, k, 2, D - 2);
                    if (v == 6 || !this.freeAt(level, b.x(u, v), y + 1, b.z(u, v))) continue;
                    int r = this.roll(salt, k, 3, 6);
                    if (r <= 1) this.set(level, box, b.x(u, v), y + 1, b.z(u, v), st(Blocks.COBWEB));
                    else if (r == 2 && this.freeAt(level, b.x(u, v), y, b.z(u, v))) this.set(level, box, b.x(u, v), y, b.z(u, v), st(Blocks.POTTED_DEAD_BUSH));
                    else if (r == 3 && this.freeAt(level, b.x(u, v), y, b.z(u, v))) this.set(level, box, b.x(u, v), y, b.z(u, v), st(Blocks.MOSS_CARPET));
                }
            }
        }
    }

    /** The balconies on the front: a slab out from every other window pair, a panel parapet; some glazed in, some with washing hung out. */
    void balconies(WorldGenLevel level, BoundingBox box, Block10 b) {
        for (int s = 1; s < STOREYS; s++) {
            int y = s * H;
            for (int u = 1; u < L - 1; u += 8) {
                if (stairwellAt(u) || stairwellAt(u + 3)) continue;
                boolean glazed = this.roll(u, y, b.oz(), 4) == 0, washing = !glazed && this.roll(u, y, b.ox(), 3) == 0;
                for (int du = 0; du <= 3; du++) {
                    int uu = u + du;
                    for (int v = -2; v <= -1; v++) this.set(level, box, b.x(uu, v), y, b.z(uu, v), slab(Blocks.SMOOTH_STONE_SLAB, SlabType.TOP));
                    boolean edge = du == 0 || du == 3;
                    this.set(level, box, b.x(uu, -2), y + 1, b.z(uu, -2), st(Blocks.POLISHED_ANDESITE));
                    if (edge) this.set(level, box, b.x(uu, -1), y + 1, b.z(uu, -1), st(Blocks.POLISHED_ANDESITE));
                    if (glazed) {
                        this.set(level, box, b.x(uu, -2), y + 2, b.z(uu, -2), pane(Blocks.GLASS_PANE, b.dir(1, 0).getAxis()));
                        if (edge) this.set(level, box, b.x(uu, -1), y + 2, b.z(uu, -1), pane(Blocks.GLASS_PANE, b.dir(0, 1).getAxis()));
                    }
                    // the door out to it: one of the window's lights left open
                    if (du == 1) for (int k = 1; k <= 2; k++) this.set(level, box, b.x(uu, 0), y + k, b.z(uu, 0), AIR);
                    if (washing && (du == 1 || du == 2)) {
                        Block cloth = du == 1 ? Blocks.WHITE_WALL_BANNER : Blocks.LIGHT_BLUE_WALL_BANNER;
                        this.set(level, box, b.x(uu, -3), y + 1, b.z(uu, -3), cloth.defaultBlockState().setValue(WallBannerBlock.FACING, b.dir(0, -1)));
                    }
                }
            }
        }
    }

    /** The flat roof: aerials, vent stacks, a sapling taking root in the gravel. */
    void roof(WorldGenLevel level, BoundingBox box, Block10 b) {
        int y = ROOF + 1;
        for (int u = 3; u < L - 3; u += 5) {
            if (stairwellAt(u)) continue;
            int v = 3 + this.roll(u, y, b.ox(), 6);
            int r = this.roll(u, y, b.oz(), 4);
            if (r == 0) {
                // an aerial: a mast, its cross arms, a guy chain
                for (int k = 0; k <= 4; k++) this.set(level, box, b.x(u, v), y + k, b.z(u, v), k == 4 ? st(Blocks.LIGHTNING_ROD) : st(Blocks.IRON_BARS));
                this.set(level, box, b.x(u, v), y + 3, b.z(u, v), pane(Blocks.IRON_BARS, b.dir(1, 0).getAxis()));
                this.set(level, box, b.x(u - 1, v), y + 3, b.z(u - 1, v), pane(Blocks.IRON_BARS, b.dir(1, 0).getAxis()));
                this.set(level, box, b.x(u + 1, v), y + 3, b.z(u + 1, v), pane(Blocks.IRON_BARS, b.dir(1, 0).getAxis()));
            } else if (r == 1) {
                this.set(level, box, b.x(u, v), y, b.z(u, v), st(Blocks.ANDESITE));
                this.set(level, box, b.x(u, v), y + 1, b.z(u, v), st(Blocks.ANDESITE_WALL));
            } else if (r == 2) {
                this.set(level, box, b.x(u, v), y, b.z(u, v), st(Blocks.GRAVEL));
                this.set(level, box, b.x(u, v), y + 1, b.z(u, v), st(Blocks.BIRCH_SAPLING));
            }
        }
    }

    /**
     * The north block's basement, reached by the stair down from its east entrance: a long corridor between storage
     * cells behind slatted fronts, the heating pipes along its ceiling - and at the far end, past the stairwell, the
     * vault behind its iron door: graphite blocks, the chest with the Graphite ring, and the mutant dog.
     */
    void basement(WorldGenLevel level, BoundingBox box, Block10 b) {
        int f = -3;
        for (int u = 1; u < L - 1; u++) {
            if (stairwellAt(u) && u >= ENTRANCES[1] - 3) continue;
            for (int v = 1; v < D - 1; v++) {
                BlockState s = AIR;
                boolean corridor = v >= 4 && v <= 6;
                if (!corridor && u < ENTRANCES[1] - 3 && (v == 3 || v == 7)) s = Math.floorMod(u, 4) == 1 ? AIR : st(Blocks.SPRUCE_FENCE);
                if (!corridor && u < ENTRANCES[1] - 3 && Math.floorMod(u, 4) == 0) s = st(Blocks.STONE_BRICKS);
                for (int y = f + 1; y <= -1; y++) this.set(level, box, b.x(u, v), y, b.z(u, v), y == f + 1 || !s.is(Blocks.SPRUCE_FENCE) ? s : s);
                this.set(level, box, b.x(u, v), f, b.z(u, v), this.roll(u, f, v, 7) == 0 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE));
            }
        }
        // the stairwell's ways into the corridor, west and east
        int c = ENTRANCES[1];
        for (int y = f + 1; y <= f + 2; y++) {
            this.set(level, box, b.x(c - 3, 5), y, b.z(c - 3, 5), AIR);
            this.set(level, box, b.x(c + 2, 5), y, b.z(c + 2, 5), AIR);
        }
        // the vault, its iron door and button
        int vu = c + 5;
        for (int v = 1; v < D - 1; v++) {
            for (int y = f + 1; y <= -1; y++) this.set(level, box, b.x(vu, v), y, b.z(vu, v), st(Blocks.IRON_BLOCK));
        }
        Direction east = b.dir(1, 0);
        BlockState door = Blocks.IRON_DOOR.defaultBlockState().setValue(DoorBlock.FACING, east.getOpposite());
        this.set(level, box, b.x(vu, 5), f + 1, b.z(vu, 5), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.set(level, box, b.x(vu, 5), f + 2, b.z(vu, 5), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        this.set(level, box, b.x(vu - 1, 4), f + 2, b.z(vu - 1, 4), Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, east.getOpposite()));
        this.set(level, box, b.x(vu + 1, 4), f + 2, b.z(vu + 1, 4), Blocks.STONE_BUTTON.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, east));
        for (int u = vu + 1; u < L - 1; u++) {
            for (int v = 1; v < D - 1; v++) {
                for (int y = f + 1; y <= -1; y++) this.set(level, box, b.x(u, v), y, b.z(u, v), AIR);
                this.set(level, box, b.x(u, v), f, b.z(u, v), this.roll(u, f, v, 3) == 0 ? st(Blocks.COAL_BLOCK) : st(Blocks.POLISHED_BLACKSTONE));
            }
        }
        for (int[] g : new int[][]{{L - 2, 1}, {L - 2, D - 2}, {L - 3, 1}, {vu + 2, D - 2}}) {
            this.set(level, box, b.x(g[0], g[1]), f + 1, b.z(g[0], g[1]), st(Blocks.COAL_BLOCK));
            this.set(level, box, b.x(g[0], g[1]), f + 2, b.z(g[0], g[1]), st(Blocks.GLOW_LICHEN.defaultBlockState().is(Blocks.GLOW_LICHEN)
                    ? Blocks.VERDANT_FROGLIGHT : Blocks.VERDANT_FROGLIGHT));
        }
        this.chest(level, box, b.x(L - 2, 5), f + 1, b.z(L - 2, 5), east.getOpposite(), VAULT);
        // a dim light in the corridor here and there
        for (int u = 3; u < c - 3; u += 8) this.set(level, box, b.x(u, 5), -1, b.z(u, 5),
                Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        this.set(level, box, b.x(c + 3, 5), -1, b.z(c + 3, 5), Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    /** On the south gable of the west block, the estate's mosaic: the peaceful atom, its orbits round a red nucleus, on blue. */
    void mosaic(WorldGenLevel level, BoundingBox box) {
        Block10 b = BLOCKS[2];
        int u = 0, cy = 18, cv = 6;
        for (int v = 1; v <= D - 2; v++) {
            for (int y = 7; y <= 29; y++) {
                double dx = v - cv + 0.5, dy = y - cy;
                boolean orbit = false;
                for (int k = 0; k < 3; k++) {
                    double a = k * Math.PI / 3, cos = Math.cos(a), sin = Math.sin(a);
                    double px = dx * cos + dy * sin, py = -dx * sin + dy * cos;
                    double e = (px * px) / (5.2 * 5.2) + (py * py) / (1.9 * 1.9);
                    if (Math.abs(e - 1.0) < 0.22) orbit = true;
                }
                double r = Math.sqrt(dx * dx + dy * dy);
                Block block = r < 1.6 ? Blocks.RED_TERRACOTTA : orbit ? Blocks.WHITE_TERRACOTTA : y > 26 || y < 9 ? Blocks.YELLOW_TERRACOTTA
                        : Blocks.LIGHT_BLUE_TERRACOTTA;
                if (Math.abs(dx) > 4.6 && block == Blocks.LIGHT_BLUE_TERRACOTTA) block = Blocks.CYAN_TERRACOTTA;
                this.set(level, box, b.x(u, v), y, b.z(u, v), st(block));
            }
        }
    }

    // ================================================================ the courtyard's buildings and things

    /** The kindergarten: two low storeys, a flat roof, bands of colour, its own fenced yard with a sandpit. */
    void kindergarten(WorldGenLevel level, BoundingBox box) {
        int x0 = -26, x1 = -8, z0 = 6, z1 = 18, top = 8;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                for (int y = 0; y <= top; y++) {
                    BlockState s;
                    if (y == 0 || y == 4 || y == top) s = st(Blocks.SMOOTH_STONE);
                    else if (!wall) s = AIR;
                    else if ((y == 2 || y == 6) && (x + z) % 3 != 0 && x != x0 && x != x1) s = this.roll(x, y, z, 4) == 0 ? AIR : pane(Blocks.GLASS_PANE, Direction.Axis.X);
                    else if ((y == 2 || y == 6) && (z % 3 != 0) && (x == x0 || x == x1)) s = pane(Blocks.GLASS_PANE, Direction.Axis.Z);
                    else s = y == 3 || y == 7 ? st(Blocks.ORANGE_TERRACOTTA) : st(Blocks.WHITE_TERRACOTTA);
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        // the door, its little porch; inside, tiny beds in rows, toys (a doll's chair), the stairs up
        for (int y = 1; y <= 2; y++) this.set(level, box, (x0 + x1) / 2, y, z1, AIR);
        for (int x = (x0 + x1) / 2 - 1; x <= (x0 + x1) / 2 + 1; x++) this.set(level, box, x, 3, z1 + 1, slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
        for (int x = x0 + 2; x <= x1 - 2; x += 2) {
            for (int fl = 0; fl <= 1; fl++) {
                int y = fl * 4 + 1;
                this.set(level, box, x, y, z0 + 2, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.HEAD));
                this.set(level, box, x, y, z0 + 3, Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT));
                if (this.roll(x, y, 5, 3) == 0) this.set(level, box, x, y, z0 + 6, st(Blocks.COBWEB));
            }
        }
        for (int k = 1; k <= 4; k++) {
            this.set(level, box, x1 - 1 - k, k, z1 - 2, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
            this.set(level, box, x1 - 1 - k, 4, z1 - 2, k < 4 ? AIR : stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
        }
        this.set(level, box, x0 + 2, 5, z1 - 2, st(Blocks.RED_WOOL));
        this.set(level, box, x0 + 3, 5, z1 - 2, st(Blocks.YELLOW_WOOL));
        this.set(level, box, x0 + 2, 1, z1 - 2, stair(Blocks.BIRCH_STAIRS, Direction.EAST, false));
        // its yard
        for (int x = x0 - 2; x <= x1 + 2; x++) {
            for (int z = z1 + 2; z <= z1 + 8; z++) {
                boolean edge = x == x0 - 2 || x == x1 + 2 || z == z1 + 8;
                if (edge && !(Math.abs(x - (x0 + x1) / 2) <= 1 && z == z1 + 8)) this.set(level, box, x, 0, z, st(Blocks.IRON_BARS));
            }
        }
        for (int x = x0 + 2; x <= x0 + 6; x++) {
            for (int z = z1 + 3; z <= z1 + 6; z++) {
                boolean rim = x == x0 + 2 || x == x0 + 6 || z == z1 + 3 || z == z1 + 6;
                this.set(level, box, x, -1, z, st(rim ? Blocks.SPRUCE_PLANKS : Blocks.SAND));
                this.set(level, box, x, 0, z, rim ? slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM) : AIR);
            }
        }
    }

    /** The playground: a climbing frame, swings, a slide, a roundabout. */
    void playground(WorldGenLevel level, BoundingBox box) {
        int px = 2, pz = 10;
        // swings: two posts, a beam, chains, seats
        for (int y = 0; y <= 3; y++) {
            this.set(level, box, px, y, pz, st(Blocks.IRON_BARS));
            this.set(level, box, px + 5, y, pz, st(Blocks.IRON_BARS));
        }
        for (int x = px; x <= px + 5; x++) this.set(level, box, x, 4, pz, pane(Blocks.IRON_BARS, Direction.Axis.X));
        for (int x : new int[]{px + 2, px + 3}) {
            for (int y = 2; y <= 3; y++) this.set(level, box, x, y, pz, st(Blocks.CHAIN));
            this.set(level, box, x, 1, pz, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        }
        // the slide: a ladder up, a platform, the chute
        int sx = px, sz = pz + 6;
        for (int y = 0; y <= 2; y++) this.set(level, box, sx, y, sz, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
        this.set(level, box, sx + 1, 2, sz, st(Blocks.IRON_BLOCK));
        for (int y = 0; y <= 2; y++) this.set(level, box, sx + 1, y, sz, y == 2 ? st(Blocks.IRON_BLOCK) : st(Blocks.IRON_BARS));
        for (int k = 1; k <= 3; k++) this.set(level, box, sx + 1 + k, 3 - k, sz, stair(Blocks.CUT_COPPER_STAIRS, Direction.WEST, false));
        // the climbing frame: a dome of bars
        int cx = px + 9, cz = pz + 6;
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 0; y <= 3; y++) {
                    double d = Math.sqrt(x * x + z * z + (y - 0.0) * (y - 0.0));
                    if (Math.abs(d - 2.5) < 0.5) this.set(level, box, cx + x, y, cz + z, st(Blocks.IRON_BARS));
                }
            }
        }
        // the roundabout
        this.set(level, box, px + 9, 0, pz, st(Blocks.WAXED_EXPOSED_CUT_COPPER));
        for (Direction d : Direction.Plane.HORIZONTAL) this.set(level, box, px + 9 + d.getStepX(), 0, pz + d.getStepZ(), slab(Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, SlabType.BOTTOM));
        this.set(level, box, px + 9, 1, pz, st(Blocks.IRON_BARS));
    }

    /**
     * The Ferris wheel, yellow paint peeling, that was to open the spring it all ended: a rim of yellow on spokes of
     * bars, eight gondolas hanging from it, on two great A-frames.
     */
    void ferrisWheel(WorldGenLevel level, BoundingBox box) {
        int cx = 14, cy = 13, cz = -14, r = 10;
        for (int x = -r - 1; x <= r + 1; x++) {
            for (int y = -r - 1; y <= r + 1; y++) {
                double d = Math.sqrt(x * x + y * y);
                if (Math.abs(d - r) < 0.55) this.set(level, box, cx + x, cy + y, cz, this.roll(x, y, cz, 9) == 0 ? st(Blocks.ORANGE_TERRACOTTA) : st(Blocks.YELLOW_TERRACOTTA));
                // the spokes: eight of them
                if (d < r - 0.5 && d > 1) {
                    double a = Math.atan2(y, x), step = Math.PI / 4;
                    double off = Math.abs(a - Math.round(a / step) * step);
                    if (off * d < 0.5) this.set(level, box, cx + x, cy + y, cz, st(Blocks.IRON_BARS));
                }
            }
        }
        this.set(level, box, cx, cy, cz, st(Blocks.IRON_BLOCK));
        for (int z = cz - 2; z <= cz + 2; z++) this.set(level, box, cx, cy, z, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Z));
        // the A-frames
        for (int side = -1; side <= 1; side += 2) {
            int z = cz + side * 2;
            for (int y = 0; y < cy; y++) {
                int spread = (cy - y) / 2;
                this.set(level, box, cx - spread, y, z, st(Blocks.YELLOW_TERRACOTTA));
                this.set(level, box, cx + spread, y, z, st(Blocks.YELLOW_TERRACOTTA));
            }
        }
        // the gondolas
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + Math.PI / 8;
            int gx = cx + (int) Math.round(Math.cos(a) * r), gy = cy + (int) Math.round(Math.sin(a) * r);
            this.set(level, box, gx, gy - 1, cz, st(Blocks.CHAIN));
            for (int x = gx - 1; x <= gx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    this.set(level, box, x, gy - 2, z, st(Blocks.YELLOW_TERRACOTTA));
                    this.set(level, box, x, gy - 4, z, slab(Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, SlabType.TOP));
                    if (x != gx || z != cz) this.set(level, box, x, gy - 3, z, (x == gx || z == cz) ? pane(Blocks.IRON_BARS, x == gx ? Direction.Axis.X : Direction.Axis.Z)
                            : st(Blocks.YELLOW_TERRACOTTA));
                }
            }
        }
    }

    /** A row of lock-up garages, their doors rusted; in one, a chest with the AK-47. */
    void garages(WorldGenLevel level, BoundingBox box) {
        int x0 = -28, z0 = -26, n = 7;
        for (int g = 0; g < n; g++) {
            int gx = x0 + g * 3;
            for (int x = gx; x <= gx + 3; x++) {
                for (int z = z0; z <= z0 + 6; z++) {
                    boolean wall = x == gx || x == gx + 3 || z == z0;
                    for (int y = 0; y <= 3; y++) {
                        BlockState s = y == 3 ? st(Blocks.SMOOTH_STONE) : wall ? st(Blocks.STONE_BRICKS) : AIR;
                        if (z == z0 + 6 && !wall && y < 3) s = this.roll(g, y, x, 4) == 0 ? AIR : st(Blocks.WAXED_WEATHERED_COPPER);
                        if (z == z0 + 6 && wall && y < 3) s = st(Blocks.STONE_BRICKS);
                        this.set(level, box, x, y, z, s);
                    }
                }
            }
            this.set(level, box, gx + 1, 0, z0 + 1, st(Blocks.BARREL));
            if (g == 4) this.chest(level, box, gx + 2, 0, z0 + 1, Direction.SOUTH, GARAGE);
            else if (this.roll(g, 0, 9, 2) == 0) this.container(level, box, gx + 2, 0, z0 + 1, Direction.SOUTH, true);
        }
    }

    /** The boiler house: a brick shed, its tall chimney striped red and white. */
    void boilerHouse(WorldGenLevel level, BoundingBox box) {
        int x0 = -50, x1 = -40, z0 = 30, z1 = 40;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                for (int y = 0; y <= 7; y++) {
                    BlockState s = y == 7 ? st(Blocks.SMOOTH_STONE) : wall ? (y == 4 && (x + z) % 2 == 0 ? pane(Blocks.GLASS_PANE, x == x0 || x == x1 ? Direction.Axis.Z : Direction.Axis.X)
                            : st(Blocks.BRICKS)) : AIR;
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        for (int y = 1; y <= 3; y++) this.set(level, box, x1, y, 35, AIR);
        this.set(level, box, x0 + 2, 1, z0 + 2, st(Blocks.BLAST_FURNACE));
        this.set(level, box, x0 + 3, 1, z0 + 2, st(Blocks.BLAST_FURNACE));
        int cx = -45, cz = 35;
        for (int y = 8; y <= 34; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockState s = x == 0 && z == 0 ? AIR : (y / 4) % 2 == 0 ? st(Blocks.RED_TERRACOTTA) : st(Blocks.WHITE_TERRACOTTA);
                    this.set(level, box, cx + x, y, cz + z, s);
                }
            }
        }
    }

    /**
     * The heating main, as on every Soviet estate: two fat lagged pipes on posts along the front of the south block,
     * from the boiler house, rising in a square arch over each path and the road.
     */
    void heatingMain(WorldGenLevel level, BoundingBox box) {
        int z = 35;
        for (int x = -40; x <= 23; x++) {
            boolean arch = Math.abs(x - (21 - ENTRANCES[0] + 1)) <= 2 || Math.abs(x - (21 - ENTRANCES[1] + 1)) <= 2;
            boolean riser = !arch && (Math.abs(x - (21 - ENTRANCES[0] + 1)) == 3 || Math.abs(x - (21 - ENTRANCES[1] + 1)) == 3);
            int y = arch ? 5 : 1;
            for (int k = 0; k <= 1; k++) this.set(level, box, x, y + k, z, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.X));
            if (riser) for (int yy = 1; yy <= 6; yy++) this.set(level, box, x, yy, z, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
            if (!arch && Math.floorMod(x, 4) == 0) this.set(level, box, x, 0, z, st(Blocks.STONE_BRICK_WALL));
        }
    }

    /** The bus stop out on the road: a shelter with a mosaic on its back wall, a bench. */
    void busStop(WorldGenLevel level, BoundingBox box) {
        int x = 33, z0 = 46;
        for (int z = z0; z <= z0 + 6; z++) {
            for (int y = 0; y <= 3; y++) {
                Block b = y == 3 ? Blocks.SMOOTH_STONE : switch ((z + y * 2) % 4) {
                    case 0 -> Blocks.ORANGE_TERRACOTTA;
                    case 1 -> Blocks.YELLOW_TERRACOTTA;
                    case 2 -> Blocks.LIGHT_BLUE_TERRACOTTA;
                    default -> Blocks.WHITE_TERRACOTTA;
                };
                this.set(level, box, x + 1, y, z, st(b));
            }
            this.set(level, box, x, 3, z, slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
            this.set(level, box, x - 1, 3, z, slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
            if (z > z0 && z < z0 + 6) this.set(level, box, x, 0, z, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
        }
        for (int y = 0; y <= 2; y++) {
            this.set(level, box, x, y, z0, st(Blocks.WHITE_TERRACOTTA));
            this.set(level, box, x, y, z0 + 6, st(Blocks.WHITE_TERRACOTTA));
        }
    }

    /** The newspaper kiosk by the road. */
    void kiosk(WorldGenLevel level, BoundingBox box) {
        int x0 = -3, z0 = -27;
        for (int x = x0; x <= x0 + 3; x++) {
            for (int z = z0; z <= z0 + 3; z++) {
                boolean wall = x == x0 || x == x0 + 3 || z == z0 || z == z0 + 3;
                for (int y = 0; y <= 3; y++) {
                    BlockState s = y == 3 ? st(Blocks.LIGHT_BLUE_TERRACOTTA) : !wall ? AIR
                            : y == 2 && z == z0 + 3 ? (this.roll(x, y, z, 3) == 0 ? AIR : pane(Blocks.GLASS_PANE, Direction.Axis.X))
                            : st(Blocks.LIGHT_BLUE_TERRACOTTA);
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        this.set(level, box, x0 + 1, 0, z0 + 1, st(Blocks.LECTERN));
        this.set(level, box, x0 + 2, 0, z0 + 1, st(Blocks.BOOKSHELF));
    }

    /** The cars, parked on the road where they were left: little boxy saloons, their paint gone dull, a few on their axles. */
    void cars(WorldGenLevel level, BoundingBox box) {
        int[][] spots = {{-20, -33, 0}, {-6, -34, 0}, {12, -33, 0}, {-34, -12, 1}, {-34, 10, 1}, {33, -4, 1}, {8, 32, 0}, {28, 44, 1}};
        Block[] paints = {Blocks.WHITE_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA, Blocks.GREEN_TERRACOTTA,
                Blocks.YELLOW_TERRACOTTA, Blocks.BROWN_TERRACOTTA};
        for (int i = 0; i < spots.length; i++) {
            int x = spots[i][0], z = spots[i][1];
            boolean alongX = spots[i][2] == 0;
            Block paint = paints[this.roll(i, 0, 3, paints.length)];
            for (int a = 0; a < 4; a++) {
                for (int b = 0; b < 2; b++) {
                    int cx = x + (alongX ? a : b), cz = z + (alongX ? b : a);
                    boolean wheel = a == 0 || a == 3;
                    this.set(level, box, cx, 0, cz, wheel ? st(Blocks.BLACK_TERRACOTTA) : st(paint));
                    if (a == 1 || a == 2) this.set(level, box, cx, 1, cz, pane(Blocks.GLASS_PANE, alongX ? Direction.Axis.X : Direction.Axis.Z));
                    else this.set(level, box, cx, 1, cz, slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
                }
            }
            for (int a = 1; a <= 2; a++) {
                for (int b = 0; b < 2; b++) {
                    int cx = x + (alongX ? a : b), cz = z + (alongX ? b : a);
                    this.set(level, box, cx, 2, cz, slab(paint == Blocks.WHITE_TERRACOTTA ? Blocks.SMOOTH_QUARTZ_SLAB : Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
                }
            }
        }
    }

    /** Birches - everywhere the asphalt isn't, and some where it is. */
    void trees(WorldGenLevel level, BoundingBox box) {
        for (int i = 0; i < 160; i++) {
            int x = X0 + 3 + this.roll(i, 1, 0, X1 - X0 - 6), z = Z0 + 3 + this.roll(i, 2, 0, Z1 - Z0 - 6);
            if (!this.clearForTree(level, x, z)) continue;
            int h = 5 + this.roll(i, 3, 0, 4);
            for (int y = 0; y < h; y++) this.set(level, box, x, y, z, log(Blocks.BIRCH_LOG, Direction.Axis.Y));
            BlockState leaves = Blocks.BIRCH_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
            for (int y = h - 3; y <= h; y++) {
                int rr = y == h ? 1 : y == h - 3 ? 1 : 2;
                for (int dx = -rr; dx <= rr; dx++) {
                    for (int dz = -rr; dz <= rr; dz++) {
                        if (Math.abs(dx) == rr && Math.abs(dz) == rr && rr == 2) continue;
                        if (dx == 0 && dz == 0 && y < h) continue;
                        if (this.freeAt(level, x + dx, y, z + dz)) this.set(level, box, x + dx, y, z + dz, leaves);
                    }
                }
            }
        }
    }

    /**
     * Whether a tree may stand here - decided from the estate's plan alone (not by looking at the world, which a chunk
     * being built can't see far into), so that every chunk a tree's crown reaches agrees it's there: clear of the
     * roads and pavements, the blocks, and everything in the courtyard.
     */
    boolean clearForTree(WorldGenLevel level, int x, int z) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) if (occupied(x + dx, z + dz)) return false;
        return true;
    }

    static boolean occupied(int x, int z) {
        if (road(x, z) || sidewalk(x, z)) return true;
        for (Block10 b : BLOCKS) {
            int ax = b.x(0, -3), az = b.z(0, -3), bx = b.x(L - 1, D), bz = b.z(L - 1, D);
            if (x >= Math.min(ax, bx) && x <= Math.max(ax, bx) && z >= Math.min(az, bz) && z <= Math.max(az, bz)) return true;
        }
        int[][] things = {{-28, 4, -6, 26}, {0, 8, 16, 24}, {2, -18, 27, -10}, {-29, -27, -6, -19}, {-51, 29, -39, 41}, {30, 45, 36, 54},
                {-4, -28, 1, -23}, {-40, 34, 24, 36}};
        for (int[] t : things) if (x >= t[0] && x <= t[2] && z >= t[1] && z <= t[3]) return true;
        return false;
    }

    // ================================================================ its inhabitants

    /** The mutants: in the flats, on the landings, about the courtyard. (The dog's in the vault - see basement.) */
    void inhabitants(WorldGenLevel level, BoundingBox box) {
        // the dog, shut in the vault with the ring
        Block10 north = BLOCKS[0];
        int vu = ENTRANCES[1] + 5;
        this.mob(level, box, ModEntities.MUTANT_DOG.get(), north.x(vu + 3, 5), -2, north.z(vu + 3, 5));
        EntityType<?> mutant = ModEntities.MUTANT_ZOMBIE.get();
        for (int i = 0; i < 18; i++) {
            Block10 b = BLOCKS[i % BLOCKS.length];
            int f = this.roll(i, 1, 1, FLATS.length), s = this.roll(i, 2, 1, STOREYS);
            int u = (FLATS[f][0] + FLATS[f][1]) / 2, v = 3;
            this.mob(level, box, mutant, b.x(u, v), s * H + 1, b.z(u, v));
        }
        for (int i = 0; i < 14; i++) {
            int x = -30 + this.roll(i, 4, 2, 60), z = -30 + this.roll(i, 5, 2, 60);
            this.mob(level, box, i % 4 == 0 ? ModEntities.CRAWLING_MUTANT.get() : mutant, x, 0, z);
        }
    }

    void mob(WorldGenLevel level, BoundingBox box, EntityType<?> type, int x, int y, int z) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        if (level.getBlockState(pos).blocksMotion() || level.getBlockState(pos.above()).blocksMotion()) return;
        if (!(type.create(level.getLevel()) instanceof Mob mob)) return;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }

    void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).rotate(ROTATIONS[this.turn]), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity entity) entity.setLootTable(loot, this.seed ^ pos.asLong());
    }

    /** A wardrobe or a cupboard: a barrel, now and then with something left in it. */
    void container(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, boolean stocked) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(net.minecraft.world.level.block.BarrelBlock.FACING, facing)
                .rotate(ROTATIONS[this.turn]), 2);
        if (stocked && level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity entity) entity.setLootTable(FLAT, this.seed ^ pos.asLong());
    }

    /** Whether a block position lies in the estate's courtyard or among its blocks (for event/ChernobylLife). */
    public boolean inEstate(BlockPos pos) {
        int[] l = this.local(pos.getX(), pos.getZ());
        return l[0] >= X0 && l[0] <= X1 && l[1] >= Z0 && l[1] <= Z1 && pos.getY() >= this.origin.getY() - 8
                && pos.getY() <= this.origin.getY() + HEIGHT;
    }
}
