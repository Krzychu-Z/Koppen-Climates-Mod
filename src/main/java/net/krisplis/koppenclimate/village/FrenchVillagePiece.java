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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * The French village of the temperate forest (Cfb): a small, close-packed Norman village of cobblestone in the summer
 * of 1944, fought over and occupied. Its plan is grown fresh for every village (see plan): no two alike.
 * <ul>
 *   <li>an irregular little square somewhere near the middle, with its war memorial, a tree or two, a bench and a
 *       sandbagged gun nest; the church on its north side, its square tower and slate spire (a shell hole through it)
 *       over the square;</li>
 *   <li>a maze of narrow cobbled streets and lanes wandering out from the square - turning corners, jogging aside,
 *       branching, running into one another, ending blind - cratered by shellfire, barricaded with sandbags where
 *       they leave the village;</li>
 *   <li>cobblestone houses packed tight along every street, two and three storeys under steep slate roofs with
 *       chimneys, whatever size the frontage allows; shops on the square; ladders up to the attics, cellars under
 *       some; many with broken windows, some with their upper floors blown out, a few no more than rubble;</li>
 *   <li>between them walled yards and gardens, low cobblestone walls along the lanes, vegetable plots, a fruit tree.</li>
 * </ul>
 * It is held by evoker soldiers (EvokerSoldierEntity) - in the square's nest, at the barricades, in the church
 * tower, at upstairs windows. Four Kar98k rifles are hidden about the town (chests/french_village_rifle): in the
 * church tower's belfry, in an attic, in a cellar, and under the rubble of a ruined house; other houses keep a
 * little food and ammunition (chests/french_village_house).
 */
public class FrenchVillagePiece extends StructurePiece {
    static final ResourceKey<LootTable> RIFLE = loot("french_village_rifle"), HOUSE = loot("french_village_house");

    static ResourceKey<LootTable> loot(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chests/" + name));
    }

    // the plan: the high street along x, the cross street along z, the square where they meet, the church north
    static final int CH_X = 5, CH_Z0 = -48, CH_Z1 = -30, TOWER_Z0 = -29, TOWER_Z1 = -23, CH_WALL = 10, TOWER_TOP = 22, SPIRE_TOP = 36;
    // the levelled ground, and the blend back into the forest round it
    static final int CLOSE_X = 42, CLOSE_Z0 = -42, CLOSE_Z1 = 42, MARGIN = 12, HEIGHT = 40;

    final BlockPos origin;
    final long seed;
    final int turn;

    public FrenchVillagePiece(BlockPos origin, long seed, int turn) {
        super(VillageRegistry.FRENCH_VILLAGE_PIECE.get(), 0, box(origin, turn));
        this.origin = origin;
        this.seed = seed;
        this.turn = turn;
    }

    public FrenchVillagePiece(CompoundTag tag) {
        super(VillageRegistry.FRENCH_VILLAGE_PIECE.get(), tag);
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
        int x0 = -CLOSE_X - MARGIN, x1 = CLOSE_X + MARGIN, z0 = CLOSE_Z0 - MARGIN, z1 = CLOSE_Z1 + MARGIN;
        int ax = CathedralPiece.rx(turn, x0, z0), az = CathedralPiece.rz(turn, x0, z0);
        int bx = CathedralPiece.rx(turn, x1, z1), bz = CathedralPiece.rz(turn, x1, z1);
        return new BoundingBox(origin.getX() + Math.min(ax, bx), origin.getY() - 8, origin.getZ() + Math.min(az, bz),
                origin.getX() + Math.max(ax, bx), origin.getY() + HEIGHT, origin.getZ() + Math.max(az, bz));
    }

    // ================================================================ placing blocks (in the town's own frame)

    static final Rotation[] ROTATIONS = CathedralPiece.ROTATIONS;

    BlockPos at(int lx, int y, int lz) {
        return new BlockPos(this.origin.getX() + CathedralPiece.rx(this.turn, lx, lz), this.origin.getY() + y,
                this.origin.getZ() + CathedralPiece.rz(this.turn, lx, lz));
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

    static BlockState leaves(Block b) {
        return b.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** A glass pane in a wall running along the given axis (joined to the wall either side). */
    static BlockState pane(Direction.Axis along) {
        boolean x = along == Direction.Axis.X;
        return Blocks.GLASS_PANE.defaultBlockState().setValue(CrossCollisionBlock.EAST, x).setValue(CrossCollisionBlock.WEST, x)
                .setValue(CrossCollisionBlock.NORTH, !x).setValue(CrossCollisionBlock.SOUTH, !x);
    }

    void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).rotate(ROTATIONS[this.turn]), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity entity) entity.setLootTable(loot, this.seed ^ pos.asLong());
    }

    // ================================================================ the houses: their plots along the streets

    /**
     * A house on its plot: its front's corner (fx, fz), facing the street (front), its frontage running along (along)
     * for width blocks; depth back from the street, storeys of 4; its style, whether a shop, how badly hit.
     * In its own terms u runs along the frontage (0 to width-1), v back from the front wall (0 to depth-1).
     */
    record House(int fx, int fz, Direction front, Direction along, int width, int depth, int storeys, int style, boolean shop,
                 int damage, int door, boolean cellar, boolean chimney, boolean brickRoof) {
        int x(int u, int v) {
            return this.fx + u * this.along.getStepX() - v * this.front.getStepX();
        }

        int z(int u, int v) {
            return this.fz + u * this.along.getStepZ() - v * this.front.getStepZ();
        }

        int top() {
            return 4 * this.storeys - 1;
        }

        /** The roof's height over row v (the overhang rows -1 and depth included). */
        int roof(int v) {
            return 4 * this.storeys + Math.min(v + 1, this.depth - v);
        }

        int ridge() {
            return 4 * this.storeys + (this.depth + 1) / 2;
        }
    }

    static final int STONE = 0, TIMBER = 1, RENDER = 2;

    // ================================================================ the plan: grown, not drawn

    // the village's grid (its own frame, x and z from -R to R): what each column is
    static final int R = 40;
    static final byte FREE = 0, STREET = 1, PLAZA = 2, BUILT = 3, CHURCH = 4;

    /**
     * The village's plan, worked out once from its seed: the grid of streets, square, church ground and houses; the
     * houses; where the church stands (the offset of its own frame); where streets leave the village (for the
     * barricades); and which three houses keep a hidden rifle.
     */
    record Layout(byte[][] grid, List<House> houses, int churchX, int churchZ, int plazaX, int plazaZ, List<int[]> exits,
                  int atticRifle, int cellarRifle, int rubbleRifle) {
        byte at(int x, int z) {
            if (x < -R || x > R || z < -R || z > R) return FREE;
            return this.grid[x + R][z + R];
        }

        boolean open(int x, int z) {
            byte b = this.at(x, z);
            return b == STREET || b == PLAZA;
        }
    }

    private transient volatile Layout layout;

    List<House> houses() {
        return this.layout().houses();
    }

    Layout layout() {
        Layout l = this.layout;
        if (l == null) {
            l = this.plan();
            this.layout = l;
        }
        return l;
    }

    private static final Direction[] DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /**
     * Grow the village. An irregular square somewhere near the middle, the church on its north side; streets set out
     * from the square's sides and wander - running on, turning a corner now and then, jogging a step aside, putting
     * out narrow lanes that wander off on their own - until they leave the village, run into another street, or give
     * out in a dead end. Then the houses: wherever a street leaves free frontage, the largest house that fits is
     * built, its front on the street, terraced against its neighbours, as deep as the ground behind allows.
     */
    private Layout plan() {
        RandomSource random = RandomSource.create(this.seed ^ 0x5EEDL);
        byte[][] g = new byte[2 * R + 1][2 * R + 1];
        // which street carved each cell (so a street runs into another, not into itself after a corner)
        int[][] owner = new int[2 * R + 1][2 * R + 1];
        // the square: a few overlapping rectangles
        int px = random.nextInt(13) - 6, pz = random.nextInt(9);
        fill(g, px - 5 - random.nextInt(3), pz - 4 - random.nextInt(2), px + 5 + random.nextInt(3), pz + 4 + random.nextInt(2), PLAZA);
        for (int k = 0; k < 2; k++) {
            int ox = px + (random.nextBoolean() ? 1 : -1) * (4 + random.nextInt(4)), oz = pz + random.nextInt(7) - 1;
            fill(g, ox - 2 - random.nextInt(3), oz - 2, ox + 2 + random.nextInt(3), oz + 2 + random.nextInt(3), PLAZA);
        }
        int top = pz;
        for (int x = px - 2; x <= px + 2; x++) {
            for (int z = -R; z <= R; z++) {
                if (g[x + R][z + R] == PLAZA) {
                    top = Math.min(top, z);
                    break;
                }
            }
        }
        // the church on the square's north side, its tower's door onto the square, a strip of churchyard round it
        int cz = top + 21, cx = px;
        fill(g, cx - 8, cz - 51, cx + 8, cz - 22, CHURCH);
        fill(g, cx - 2, cz - 22, cx + 2, top, PLAZA);
        // the streets out of the square
        List<int[]> walkers = new ArrayList<>();   // x, z, dir, width, steps left
        List<int[]> exits = new ArrayList<>();
        for (int d = 0; d < 4; d++) {
            Direction dir = DIRS[d];
            int count = dir == Direction.NORTH ? 2 : 1 + (random.nextInt(3) == 0 ? 1 : 0);
            for (int k = 0; k < count; k++) {
                int[] start = this.edgeOfPlaza(g, dir, random, cx);
                if (start != null) walkers.add(new int[]{start[0], start[1], d, 3, 70 + random.nextInt(20)});
            }
        }
        int spawned = walkers.size();
        for (int w = 0; w < walkers.size(); w++) {
            int[] walker = walkers.get(w);
            int x = walker[0], z = walker[1], d = walker[2], width = walker[3], steps = walker[4];
            int since = 0;
            for (int step = 0; step < steps; step++) {
                Direction dir = DIRS[d];
                int nx = x + dir.getStepX(), nz = z + dir.getStepZ();
                if (Math.abs(nx) > R - 1 || Math.abs(nz) > R - 1) {
                    if (width == 3) exits.add(new int[]{x, z, d});
                    break;
                }
                if (blocked(g, nx, nz, dir, width)) {
                    // the church in the way: round the corner, or stop
                    int nd = (d + (random.nextBoolean() ? 1 : 3)) % 4;
                    if (blocked(g, x + DIRS[nd].getStepX(), z + DIRS[nd].getStepZ(), DIRS[nd], width)) break;
                    d = nd;
                    since = 0;
                    continue;
                }
                int o = owner[nx + R][nz + R];
                boolean meets = step > 3 && g[nx + R][nz + R] == STREET && o != w + 1 || step > 2 && g[nx + R][nz + R] == PLAZA;
                carve(g, owner, w + 1, nx, nz, dir, width);
                x = nx;
                z = nz;
                since++;
                if (meets) break;
                float r = random.nextFloat();
                if (since > 6 && r < 0.10F) {
                    d = (d + (random.nextBoolean() ? 1 : 3)) % 4;            // round a corner
                    since = 0;
                } else if (since > 3 && r < 0.20F) {
                    Direction side = random.nextBoolean() ? dir.getClockWise() : dir.getCounterClockWise();
                    if (!blocked(g, x + side.getStepX(), z + side.getStepZ(), dir, width)) {
                        x += side.getStepX();                                  // a jog: the street steps aside
                        z += side.getStepZ();
                        carve(g, owner, w + 1, x, z, dir, width);
                        since = 0;
                    }
                }
                if (spawned < 22 && random.nextFloat() < (width == 3 ? 0.09F : 0.05F) && step > 4) {
                    int bd = (d + (random.nextBoolean() ? 1 : 3)) % 4;
                    walkers.add(new int[]{x, z, bd, 2, 18 + random.nextInt(30)});
                    spawned++;
                }
            }
        }
        // the houses, along every street's free frontage
        List<int[]> frontage = new ArrayList<>();
        for (int x = -R + 1; x < R; x++) {
            for (int z = -R + 1; z < R; z++) {
                if (g[x + R][z + R] != FREE) continue;
                for (Direction dir : DIRS) {
                    byte n = g[x + dir.getStepX() + R][z + dir.getStepZ() + R];
                    if (n == STREET || n == PLAZA) {
                        frontage.add(new int[]{x, z});
                        break;
                    }
                }
            }
        }
        for (int i = frontage.size() - 1; i > 0; i--) {
            int k = random.nextInt(i + 1);
            int[] t = frontage.get(i);
            frontage.set(i, frontage.get(k));
            frontage.set(k, t);
        }
        List<House> houses = new ArrayList<>();
        for (int[] c : frontage) {
            if (g[c[0] + R][c[1] + R] != FREE) continue;
            House h = this.fit(g, c[0], c[1], random);
            if (h != null) houses.add(h);
        }
        // the rifles' hiding places: an attic, a cellar, a ruin (there is always one of each)
        int attic = -1, cellar = -1, rubble = -1;
        for (int i = 0; i < houses.size(); i++) {
            House h = houses.get(i);
            if (attic < 0 && h.damage() == 0 && i > 2) attic = i;
            else if (cellar < 0 && h.cellar() && h.damage() < 3) cellar = i;
            else if (rubble < 0 && h.damage() == 3) rubble = i;
        }
        if (attic < 0) attic = remake(houses, 3, 0, false, attic, cellar, rubble);
        if (cellar < 0) cellar = remake(houses, houses.size() / 2, 1, true, attic, cellar, rubble);
        if (rubble < 0) rubble = remake(houses, houses.size() - 4, 3, false, attic, cellar, rubble);
        return new Layout(g, houses, cx, cz, px, pz, exits, attic, cellar, rubble);
    }

    static void fill(byte[][] g, int x0, int z0, int x1, int z1, byte what) {
        for (int x = Math.max(-R, x0); x <= Math.min(R, x1); x++) {
            for (int z = Math.max(-R, z0); z <= Math.min(R, z1); z++) g[x + R][z + R] = what;
        }
    }

    // the cells across a street of this width at (x, z) running that way
    static boolean blocked(byte[][] g, int x, int z, Direction dir, int width) {
        Direction side = dir.getClockWise();
        for (int k = -(width - 1) / 2; k <= width / 2; k++) {
            int cx = x + k * side.getStepX(), cz = z + k * side.getStepZ();
            if (Math.abs(cx) > R || Math.abs(cz) > R) return true;
            if (g[cx + R][cz + R] == CHURCH || g[cx + R][cz + R] == BUILT) return true;
        }
        return false;
    }

    static void carve(byte[][] g, int[][] owner, int id, int x, int z, Direction dir, int width) {
        Direction side = dir.getClockWise();
        for (int k = -(width - 1) / 2; k <= width / 2; k++) {
            int cx = x + k * side.getStepX(), cz = z + k * side.getStepZ();
            if (Math.abs(cx) > R || Math.abs(cz) > R) continue;
            if (g[cx + R][cz + R] == FREE) {
                g[cx + R][cz + R] = STREET;
                owner[cx + R][cz + R] = id;
            }
        }
    }

    /** A point on the square's edge facing that way (for the north side, clear of the church). */
    int[] edgeOfPlaza(byte[][] g, Direction dir, RandomSource random, int churchX) {
        List<int[]> edge = new ArrayList<>();
        for (int x = -R + 1; x < R; x++) {
            for (int z = -R + 1; z < R; z++) {
                if (g[x + R][z + R] != PLAZA) continue;
                if (g[x + dir.getStepX() + R][z + dir.getStepZ() + R] != FREE) continue;
                if (dir == Direction.NORTH && Math.abs(x - churchX) < 10) continue;
                edge.add(new int[]{x, z});
            }
        }
        if (edge.isEmpty()) return null;
        return edge.get(random.nextInt(edge.size()));
    }

    /** The largest house that fits at a free cell on a street: its front on the street, as deep as there is room. */
    House fit(byte[][] g, int cx, int cz, RandomSource random) {
        List<Direction> fronts = new ArrayList<>();
        for (Direction dir : DIRS) {
            byte n = g[cx + dir.getStepX() + R][cz + dir.getStepZ() + R];
            if (n == STREET || n == PLAZA) fronts.add(dir);
        }
        if (fronts.isEmpty()) return null;
        Direction front = fronts.get(random.nextInt(fronts.size()));
        Direction along = front.getClockWise();
        int maxW = 5 + random.nextInt(4), maxD = 6 + random.nextInt(4);
        for (int w = maxW; w >= 4; w--) {
            for (int d = maxD; d >= 5; d--) {
                for (int off : new int[]{0, w - 1, w / 2}) {
                    int fx = cx - off * along.getStepX(), fz = cz - off * along.getStepZ();
                    int onStreet = 0;
                    boolean ok = true;
                    for (int u = 0; u < w && ok; u++) {
                        for (int v = 0; v < d && ok; v++) {
                            int x = fx + u * along.getStepX() - v * front.getStepX(), z = fz + u * along.getStepZ() - v * front.getStepZ();
                            if (Math.abs(x) > R - 1 || Math.abs(z) > R - 1 || g[x + R][z + R] != FREE) ok = false;
                            if (ok && v == 0) {
                                byte n = g[x + front.getStepX() + R][z + front.getStepZ() + R];
                                if (n == STREET || n == PLAZA) onStreet++;
                            }
                        }
                    }
                    if (!ok || onStreet * 10 < w * 6) continue;
                    // built: mark it, and pick its door where it opens on the street
                    int door = -1;
                    for (int u = 0; u < w; u++) {
                        for (int v = 0; v < d; v++) {
                            int x = fx + u * along.getStepX() - v * front.getStepX(), z = fz + u * along.getStepZ() - v * front.getStepZ();
                            g[x + R][z + R] = BUILT;
                        }
                        int x = fx + u * along.getStepX(), z = fz + u * along.getStepZ();
                        byte n = g[x + front.getStepX() + R][z + front.getStepZ() + R];
                        if ((n == STREET || n == PLAZA) && u > 0 && u < w - 1 && (door < 0 || Math.abs(u - w / 2) < Math.abs(door - w / 2))) door = u;
                    }
                    if (door < 0) door = Math.max(1, Math.min(w - 2, w / 2));
                    boolean onSquare = false;
                    for (int u = 0; u < w; u++) {
                        int x = fx + u * along.getStepX() + front.getStepX(), z = fz + u * along.getStepZ() + front.getStepZ();
                        if (g[x + R][z + R] == PLAZA) onSquare = true;
                    }
                    int storeys = onSquare ? 3 : random.nextInt(3) == 0 ? 3 : 2;
                    if (d <= 5) storeys = 2;
                    int r = random.nextInt(20);
                    int damage = r < 9 ? 0 : r < 15 ? 1 : r < 18 ? 2 : 3;
                    boolean shop = w >= 5 && (onSquare ? random.nextInt(5) < 3 : random.nextInt(6) == 0);
                    return new House(fx, fz, front, along, w, d, storeys, STONE, shop, damage, door, d >= 6 && w >= 5 && random.nextInt(3) == 0,
                            random.nextInt(3) != 0, false);
                }
            }
        }
        return null;
    }

    // a house made over (its damage, its cellar) to keep a rifle; the first one from 'from' not already keeping one
    static int remake(List<House> out, int from, int damage, boolean cellar, int a, int b, int c) {
        for (int k = 0; k < out.size(); k++) {
            int i = Math.floorMod(from + k, out.size());
            if (i == a || i == b || i == c) continue;
            House h = out.get(i);
            if (cellar && (h.width() < 5 || h.depth() < 6)) continue;
            out.set(i, new House(h.fx(), h.fz(), h.front(), h.along(), h.width(), h.depth(), h.storeys(), h.style(), h.shop(), damage,
                    h.door(), cellar || h.cellar(), h.chimney(), h.brickRoof()));
            return i;
        }
        return -1;
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
        this.streets(level, box);
        this.gardens(level, box);
        List<House> houses = this.houses();
        for (int i = 0; i < houses.size(); i++) {
            House h = houses.get(i);
            if (!this.touches(box, h)) continue;
            this.house(level, box, h, i);
        }
        this.square(level, box);
        this.church(level, box);
        this.barricades(level, box);
        this.garrison(level, box);
    }

    boolean touches(BoundingBox box, House h) {
        BlockPos a = this.at(h.x(-1, -2), 0, h.z(-1, -2)), b = this.at(h.x(h.width(), h.depth() + 1), 0, h.z(h.width(), h.depth() + 1));
        return Math.max(a.getX(), b.getX()) >= box.minX() && Math.min(a.getX(), b.getX()) <= box.maxX()
                && Math.max(a.getZ(), b.getZ()) >= box.minZ() && Math.min(a.getZ(), b.getZ()) <= box.maxZ();
    }

    static double closeDistance(int x, int z) {
        double dx = Math.max(Math.abs(x) - CLOSE_X, 0), dz = Math.max(Math.max(CLOSE_Z0 - z, z - CLOSE_Z1), 0);
        return Math.sqrt(dx * dx + dz * dz);
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** The town's ground, levelled at its floor and blended back into the forest round it. */
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
                    if (!s.isAir() && (dist <= 0 || y <= ground + 1 || !s.blocksMotion() || isGround(s))) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                for (int y = ground + 1; y < target; y++) {
                    level.setBlock(pos.set(wx, y, wz), st(y < target - 3 ? Blocks.STONE : Blocks.DIRT), 2);
                }
                level.setBlock(pos.set(wx, target, wz), this.roll(l[0], 0, l[1], 9) == 0 ? st(Blocks.COARSE_DIRT) : st(Blocks.GRASS_BLOCK), 2);
                if (target - 1 > ground) level.setBlock(pos.set(wx, target - 1, wz), st(Blocks.DIRT), 2);
            }
        }
    }

    // ---------------------------------------------------------------- the streets and the square

    /** The streets' cobbles, worn and patched; the square's paving; the shell craters. */
    void streets(WorldGenLevel level, BoundingBox box) {
        Layout l = this.layout();
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                byte b = l.at(x, z);
                if (b != STREET && b != PLAZA) continue;
                int r = this.roll(x, 0, z, 20);
                BlockState s;
                if (b == PLAZA) {
                    s = (Math.floorMod(x - l.plazaX(), 4) == 0 || Math.floorMod(z - l.plazaZ(), 4) == 0) ? st(Blocks.POLISHED_ANDESITE)
                            : r < 3 ? st(Blocks.CRACKED_STONE_BRICKS) : r < 5 ? st(Blocks.COBBLESTONE) : st(Blocks.STONE_BRICKS);
                } else {
                    s = r < 9 ? st(Blocks.COBBLESTONE) : r < 13 ? st(Blocks.STONE) : r < 16 ? st(Blocks.ANDESITE)
                            : r < 18 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.GRAVEL);
                }
                this.set(level, box, x, 0, z, s);
                this.set(level, box, x, -1, z, st(Blocks.DIRT));
            }
        }
        // shell craters, out in the streets
        RandomSource random = RandomSource.create(this.seed ^ 0xC4A7L);
        for (int k = 0, placed = 0; k < 200 && placed < 5; k++) {
            int cx = random.nextInt(2 * R - 7) - R + 3, cz = random.nextInt(2 * R - 7) - R + 3;
            if (l.at(cx, cz) != STREET || l.at(cx + 1, cz) != STREET || l.at(cx, cz + 1) != STREET || l.at(cx - 1, cz) != STREET
                    || l.at(cx, cz - 1) != STREET) continue;
            placed++;
            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    double d = Mth.length(x - cx, z - cz);
                    if (d > 2.2 || !l.open(x, z)) continue;
                    if (d <= 1.5) {
                        int depth = d < 0.8 ? 2 : 1;
                        for (int y = 0; y > -depth; y--) this.set(level, box, x, y, z, st(Blocks.AIR));
                        this.set(level, box, x, -depth, z, this.roll(x, 2, z, 3) == 0 ? st(Blocks.GRAVEL) : st(Blocks.COARSE_DIRT));
                    } else if (this.roll(x, 3, z, 3) == 0) {
                        this.set(level, box, x, 1, z, slab(Blocks.COBBLESTONE_SLAB, SlabType.BOTTOM));
                    }
                }
            }
        }
    }

    /** The square: its war memorial, a tree or two, a bench, the sandbagged nest - wherever there's room on it. */
    void square(WorldGenLevel level, BoundingBox box) {
        Layout l = this.layout();
        int mx = l.plazaX(), mz = l.plazaZ();
        for (int x = mx - 2; x <= mx + 2; x++) {
            for (int z = mz - 2; z <= mz + 2; z++) {
                this.set(level, box, x, 1, z, Math.max(Math.abs(x - mx), Math.abs(z - mz)) == 2 ? slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM)
                        : st(Blocks.STONE_BRICKS));
            }
        }
        this.set(level, box, mx, 2, mz, st(Blocks.CHISELED_STONE_BRICKS));
        for (int y = 3; y <= 6; y++) this.set(level, box, mx, y, mz, st(Blocks.POLISHED_ANDESITE));
        this.set(level, box, mx, 7, mz, st(Blocks.CHISELED_STONE_BRICKS));
        this.set(level, box, mx, 8, mz, slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
        // the nest, a tree, a bench: at spots of the square clear all round
        int[][] spots = {{-6, 3}, {6, -2}, {-5, -3}, {5, 4}, {0, 5}, {-8, 0}, {8, 1}};
        int used = 0;
        for (int[] o : spots) {
            int x = mx + o[0], z = mz + o[1];
            boolean clear = true;
            for (int dx = -2; dx <= 2 && clear; dx++) {
                for (int dz = -2; dz <= 2 && clear; dz++) clear = l.at(x + dx, z + dz) == PLAZA;
            }
            if (!clear) continue;
            if (used == 0) this.nest(level, box, x, z);
            else if (used == 1 || used == 3) this.tree(level, box, x, z, 6);
            else if (used == 2) {
                for (int dx = -1; dx <= 1; dx++) this.set(level, box, x + dx, 1, z, stair(Blocks.SPRUCE_STAIRS, z > mz ? Direction.SOUTH : Direction.NORTH, false));
            }
            if (++used > 3) break;
        }
    }

    void nest(WorldGenLevel level, BoundingBox box, int nx, int nz) {
        for (int x = nx - 2; x <= nx + 2; x++) {
            for (int z = nz - 2; z <= nz + 2; z++) {
                if (Math.max(Math.abs(x - nx), Math.abs(z - nz)) != 2 || z == nz - 2 && Math.abs(x - nx) <= 1) continue;
                this.set(level, box, x, 1, z, st(Blocks.PACKED_MUD));
                this.set(level, box, x, 2, z, (x + z & 1) == 0 ? st(Blocks.PACKED_MUD) : slab(Blocks.MUD_BRICK_SLAB, SlabType.BOTTOM));
            }
        }
        this.chest(level, box, nx + 1, 1, nz - 1, Direction.WEST, HOUSE);
    }

    void tree(WorldGenLevel level, BoundingBox box, int x, int z, int h) {
        for (int y = 1; y <= h; y++) this.set(level, box, x, y, z, log(Blocks.OAK_LOG, Direction.Axis.Y));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int y = h + dy;
                    if (Math.abs(dx) + Math.abs(dz) + Math.abs(dy) > 3 || dx == 0 && dz == 0 && dy < 1) continue;
                    if (this.roll(x + dx, y, z + dz, 7) == 0) continue;
                    this.set(level, box, x + dx, y, z + dz, leaves(Blocks.OAK_LEAVES));
                }
            }
        }
        this.set(level, box, x, h + 2, z, leaves(Blocks.OAK_LEAVES));
    }

    /**
     * Whatever ground the houses left: walled gardens and yards. Where one meets a street, a low cobblestone wall
     * (with a gap for a gate now and then); behind, grass, a vegetable plot, an apple tree, a hedge.
     */
    void gardens(WorldGenLevel level, BoundingBox box) {
        Layout l = this.layout();
        for (int x = -R + 1; x < R; x++) {
            for (int z = -R + 1; z < R; z++) {
                if (l.at(x, z) != FREE) continue;
                boolean onStreet = false, byHouse = false;
                for (Direction dir : DIRS) {
                    byte n = l.at(x + dir.getStepX(), z + dir.getStepZ());
                    if (n == STREET || n == PLAZA) onStreet = true;
                    if (n == BUILT) byHouse = true;
                }
                int r = this.roll(x, 5, z, 40);
                if (!onStreet && !byHouse && !this.near(l, x, z, 5)) {
                    if (r == 0 && this.clearAround(l, x, z, 2) && this.roll(x, 7, z, 4) == 0) this.tree(level, box, x, z, 5);
                    continue;
                }
                if (onStreet) {
                    if (r < 4) continue;                                  // a gate
                    this.set(level, box, x, 1, z, r < 12 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE));
                    if (r % 3 == 0) this.set(level, box, x, 2, z, st(Blocks.COBBLESTONE_WALL));
                } else if (l.at(x, z) == FREE && r == 0 && !byHouse && this.clearAround(l, x, z, 2)) {
                    this.tree(level, box, x, z, 4 + this.roll(x, 6, z, 2));
                } else if (r < 3 && !byHouse) {
                    this.set(level, box, x, 1, z, leaves(Blocks.OAK_LEAVES));
                } else if (r >= 30 && r < 34 && byHouse) {
                    this.set(level, box, x, 0, z, st(Blocks.FARMLAND));
                    net.minecraft.world.level.block.CropBlock crop = (net.minecraft.world.level.block.CropBlock)
                            (r == 30 ? Blocks.CARROTS : r == 31 ? Blocks.POTATOES : Blocks.BEETROOTS);
                    this.set(level, box, x, 1, z, crop.getStateForAge(crop.getMaxAge()));
                }
            }
        }
    }

    // whether anything of the village (a street, a house) is this near
    boolean near(Layout l, int x, int z, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                byte b = l.at(x + dx, z + dz);
                if (b == STREET || b == BUILT || b == PLAZA) return true;
            }
        }
        return false;
    }

    boolean clearAround(Layout l, int x, int z, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) if (l.at(x + dx, z + dz) != FREE) return false;
        }
        return true;
    }

    // ---------------------------------------------------------------- a house

    BlockState wallBlock(House h, int u, int y, int v, boolean corner) {
        int r = this.roll(h.x(u, v), y, h.z(u, v), 12);
        Direction.Axis along = h.along().getAxis();
        switch (h.style()) {
            case TIMBER -> {
                if (y <= 3) return corner || r < 3 ? st(Blocks.STONE_BRICKS) : r < 7 ? st(Blocks.COBBLESTONE) : st(Blocks.CALCITE);
                // the frame: posts at the corners and every other column, a beam at each floor and the eaves
                if (corner || Math.floorMod(u, 2) == 0 && (u == 0 || u == h.width() - 1 || v == 0 || v == h.depth() - 1)) {
                    return log(Blocks.DARK_OAK_LOG, Direction.Axis.Y);
                }
                if (y % 4 == 0 || y == h.top()) return log(Blocks.STRIPPED_DARK_OAK_LOG, (v == 0 || v == h.depth() - 1) ? along
                        : (along == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X));
                return st(Blocks.WHITE_TERRACOTTA);
            }
            case RENDER -> {
                if (corner) return st(Blocks.STONE_BRICKS);
                return r == 0 ? st(Blocks.SANDSTONE) : st(Blocks.SMOOTH_SANDSTONE);
            }
            default -> {
                // rough cobblestone, mossy here and there (more so low down, in the damp)
                return r < (y <= 2 ? 4 : 2) ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE);
            }
        }
    }

    /** Where the upper floors have been blown out (damage 2): the stretch of the frontage, and how high what's left stands. */
    boolean blownOut(House h, int u, int y) {
        if (h.damage() < 2) return false;
        if (h.damage() == 3) return y > 1 + this.roll(h.x(u, 0) / 2, 9, h.z(u, 0) / 2, 4);
        int half = h.width() / 2;
        boolean left = this.roll(h.fx(), 8, h.fz(), 2) == 0;
        boolean in = left ? u < half + 1 : u >= h.width() - half - 1;
        return in && y > 4 + this.roll(h.x(u, 0), 10, h.z(u, 0), 3);
    }

    void house(WorldGenLevel level, BoundingBox box, House h, int index) {
        int w = h.width(), d = h.depth(), top = h.top();
        Direction front = h.front(), back = front.getOpposite();
        Direction.Axis along = h.along().getAxis();
        boolean ruin = h.damage() == 3;
        // clear the plot, lay its floors
        for (int u = 0; u < w; u++) {
            for (int v = 0; v < d; v++) {
                for (int y = 1; y <= h.ridge() + 3; y++) this.set(level, box, h.x(u, v), y, h.z(u, v), st(Blocks.AIR));
                this.set(level, box, h.x(u, v), 0, h.z(u, v), h.shop() ? ((u + v & 1) == 0 ? st(Blocks.POLISHED_ANDESITE) : st(Blocks.STONE_BRICKS))
                        : st(Blocks.SPRUCE_PLANKS));
            }
        }
        // the walls, with their doors and windows
        for (int u = 0; u < w; u++) {
            for (int v = 0; v < d; v++) {
                boolean frontWall = v == 0, backWall = v == d - 1, sideWall = u == 0 || u == w - 1;
                boolean inside = !frontWall && !backWall && !sideWall;
                int x = h.x(u, v), z = h.z(u, v);
                if (inside) {
                    // the upper floors and the attic floor (with the ladder's hole)
                    boolean hole = u == w - 2 && v == d - 2;
                    for (int y = 4; y <= 4 * h.storeys(); y += 4) {
                        if (ruin || this.blownOut(h, u, y)) continue;
                        if (!hole) this.set(level, box, x, y, z, st(Blocks.SPRUCE_PLANKS));
                    }
                    continue;
                }
                boolean corner = sideWall && (frontWall || backWall);
                for (int y = 1; y <= top; y++) {
                    if (this.blownOut(h, u, y)) break;
                    if (ruin && y > 4) break;
                    int floorY = y % 4;
                    boolean windowRow = floorY == 2 || floorY == 3;
                    BlockState s = this.wallBlock(h, u, y, v, corner);
                    if (frontWall && !corner) {
                        if (u == h.door() && y <= 2) {
                            s = h.damage() > 0 ? st(Blocks.AIR) : Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, front)
                                    .setValue(DoorBlock.HALF, y == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER)
                                    .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
                        } else if (h.shop() && y <= 3 && u != h.door() && !sideWall) {
                            // the shop window
                            s = y == 3 ? st(Blocks.DARK_OAK_PLANKS) : this.glass(h, u, y, v, along);
                        } else if (windowRow && Math.floorMod(u - h.door(), 2) == 1 && y > 3) {
                            s = this.glass(h, u, y, v, along);
                        } else if (windowRow && y <= 3 && Math.abs(u - h.door()) >= 2 && Math.floorMod(u, 2) == 1) {
                            s = this.glass(h, u, y, v, along);
                        }
                    } else if (backWall && !corner && windowRow && Math.floorMod(u, 2) == 1) {
                        s = this.glass(h, u, y, v, along);
                    }
                    // shell holes through the walls of the houses that were hit
                    if (h.damage() >= 1 && !corner && this.roll(x, y, z, 30) == 0) s = st(Blocks.AIR);
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        // the roof: slate (or tiles) in a steep gable over the street, the gable ends walled up, a chimney
        if (!ruin) {
            Block roofStair = h.brickRoof() ? Blocks.BRICK_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS;
            Block roofBlock = h.brickRoof() ? Blocks.BRICKS : Blocks.DEEPSLATE_TILES;
            for (int u = 0; u < w; u++) {
                if (this.blownOut(h, u, top + 1)) continue;
                for (int v = -1; v <= d; v++) {
                    int ry = h.roof(v);
                    boolean frontHalf = v + 1 < d - v, backHalf = v + 1 > d - v;
                    int x = h.x(u, v), z = h.z(u, v);
                    if (h.damage() >= 1 && this.roll(x, ry, z, 9) == 0) continue;
                    if (frontHalf) this.set(level, box, x, ry, z, stair(roofStair, back, false));
                    else if (backHalf) this.set(level, box, x, ry, z, stair(roofStair, front, false));
                    else this.set(level, box, x, ry, z, st(roofBlock));
                    if (v >= 0 && v < d && ry - 1 > top) {
                        // the gable ends
                        if (u == 0 || u == w - 1) {
                            for (int y = top + 1; y < ry; y++) this.set(level, box, x, y, z, this.wallBlock(h, u, y, v, false));
                        }
                    }
                }
            }
            if (h.chimney() && !this.blownOut(h, 0, top + 1)) {
                int v = d / 2;
                for (int y = top + 1; y <= h.ridge() + 2; y++) this.set(level, box, h.x(0, v), y, h.z(0, v), st(Blocks.COBBLESTONE));
            }
        }
        // the rubble of what came down
        if (h.damage() >= 2) {
            for (int u = 0; u < w; u++) {
                for (int v = -2; v < d - 1; v++) {
                    int x = h.x(u, v), z = h.z(u, v);
                    int r = this.roll(x, 11, z, 10);
                    if (r >= (h.damage() == 3 ? 7 : 4)) continue;
                    if (v < 0 && r > 1) continue;
                    if (v > 0 && v < d - 1 && u > 0 && u < w - 1 || v < 0) {
                        BlockState s = r == 0 ? st(Blocks.GRAVEL) : r == 1 ? st(Blocks.COBBLESTONE) : r == 2 ? slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM)
                                : r == 3 ? stair(Blocks.DEEPSLATE_TILE_STAIRS, front, false) : r == 4 ? st(Blocks.COBBLESTONE) : st(Blocks.GRAVEL);
                        this.set(level, box, x, 1, z, s);
                        if (h.damage() == 3 && r < 3) this.set(level, box, x, 2, z, st(Blocks.COBBLESTONE));
                    }
                }
            }
        }
        if (ruin) {
            if (index == this.layout().rubbleRifle()) {
                // the rifle under the rubble: a chest buried in gravel and stone
                int u = w / 2, v = d / 2;
                this.chest(level, box, h.x(u, v), 1, h.z(u, v), front, RIFLE);
                for (int du = -1; du <= 1; du++) {
                    for (int dv = -1; dv <= 1; dv++) {
                        this.set(level, box, h.x(u + du, v + dv), 2, h.z(u + du, v + dv), (du + dv & 1) == 0 ? st(Blocks.GRAVEL) : st(Blocks.COBBLESTONE));
                        if (du != 0 || dv != 0) this.set(level, box, h.x(u + du, v + dv), 1, h.z(u + du, v + dv), st(Blocks.COBBLESTONE));
                    }
                }
            }
            return;
        }
        // the ladder up through the floors to the attic
        for (int y = 1; y < 4 * h.storeys(); y++) {
            if (this.blownOut(h, w - 2, y)) break;
            this.set(level, box, h.x(w - 2, d - 2), y, h.z(w - 2, d - 2), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, front));
        }
        // a table and chairs downstairs (or the shop's counter), a rug upstairs
        int tu = 1 + this.roll(h.fx(), 12, h.fz(), Math.max(1, w - 3)), tv = 2 + this.roll(h.fx(), 13, h.fz(), Math.max(1, d - 5));
        if (h.shop()) {
            for (int u = 1; u < w - 2; u++) this.set(level, box, h.x(u, d - 3), 1, h.z(u, d - 3), st(Blocks.SPRUCE_PLANKS));
            this.set(level, box, h.x(1, d - 3), 2, h.z(1, d - 3), st(Blocks.BARREL));
        } else {
            this.set(level, box, h.x(tu, tv), 1, h.z(tu, tv), st(Blocks.SPRUCE_FENCE));
            this.set(level, box, h.x(tu, tv), 2, h.z(tu, tv), st(Blocks.SPRUCE_PRESSURE_PLATE));
            if (tu + 1 < w - 1) this.set(level, box, h.x(tu + 1, tv), 1, h.z(tu + 1, tv), stair(Blocks.SPRUCE_STAIRS, h.along(), false));
        }
        if (!this.blownOut(h, 1, 5)) {
            for (int u = 1; u <= Math.min(3, w - 3); u++) this.set(level, box, h.x(u, 2), 5, h.z(u, 2), st(Blocks.RED_CARPET));
        }
        // the cellar under a hatch, the attic
        if (h.cellar()) {
            for (int u = 1; u < w - 1; u++) {
                for (int v = 1; v < d - 1; v++) {
                    for (int y = -4; y <= -1; y++) {
                        boolean shell = u == 1 || u == w - 2 || v == 1 || v == d - 2 || y == -4;
                        this.set(level, box, h.x(u, v), y, h.z(u, v), shell ? st(Blocks.COBBLESTONE) : st(Blocks.AIR));
                    }
                }
            }
            int hu = 2, hv = d - 3;
            this.set(level, box, h.x(hu, hv), 0, h.z(hu, hv), Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, front)
                    .setValue(TrapDoorBlock.HALF, Half.TOP));
            for (int y = -3; y <= -1; y++) {
                this.set(level, box, h.x(hu, hv + 1), y, h.z(hu, hv + 1), st(Blocks.COBBLESTONE));
                this.set(level, box, h.x(hu, hv), y, h.z(hu, hv), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, front));
            }
            this.chest(level, box, h.x(w - 3, 2), -3, h.z(w - 3, 2), back, index == this.layout().cellarRifle() ? RIFLE : HOUSE);
            this.set(level, box, h.x(w - 4 >= 2 ? w - 4 : 2, 2), -3, h.z(w - 4 >= 2 ? w - 4 : 2, 2), st(Blocks.BARREL));
        }
        int attic = 4 * h.storeys() + 1;
        if (!this.blownOut(h, 1, attic) && h.roof(d / 2) > attic) {
            if (index == this.layout().atticRifle()) this.chest(level, box, h.x(1, d / 2), attic, h.z(1, d / 2), along == Direction.Axis.X ? Direction.EAST : Direction.SOUTH, RIFLE);
            else if (this.roll(h.fx(), 14, h.fz(), 4) == 0) this.chest(level, box, h.x(1, d / 2), attic, h.z(1, d / 2), back, HOUSE);
        }
        if (!h.cellar() && this.roll(h.fx(), 15, h.fz(), 3) == 0) this.chest(level, box, h.x(1, d - 2), 1, h.z(1, d - 2), front, HOUSE);
        // cobwebs in the corners of the houses left empty
        if (h.damage() > 0) {
            for (int y = 3; y < top; y += 4) {
                if (!this.blownOut(h, 1, y)) this.set(level, box, h.x(1, 1), y, h.z(1, 1), st(Blocks.COBWEB));
            }
        }
    }

    BlockState glass(House h, int u, int y, int v, Direction.Axis along) {
        int x = h.x(u, v), z = h.z(u, v);
        // broken panes in the houses that were hit
        if (h.damage() >= 1 && this.roll(x, y, z, 2) == 0) return st(Blocks.AIR);
        return pane(along);
    }

    // ---------------------------------------------------------------- the church

    // the church is drawn in its own frame, shifted to where the plan put it (on the square's north side)
    void cset(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        Layout l = this.layout();
        this.set(level, box, x + l.churchX(), y, z + l.churchZ(), state);
    }

    void church(WorldGenLevel level, BoundingBox box) {
        BlockState air = st(Blocks.AIR);
        // the nave
        for (int x = -CH_X - 1; x <= CH_X + 1; x++) {
            for (int z = CH_Z0 - 1; z <= CH_Z1 + 1; z++) {
                int ax = Math.abs(x);
                boolean wall = ax == CH_X || z == CH_Z0 || z == CH_Z1;
                boolean outside = ax > CH_X || z < CH_Z0 || z > CH_Z1;
                if (!outside) this.cset(level, box, x, 0, z, (x + z & 1) == 0 ? st(Blocks.POLISHED_ANDESITE) : st(Blocks.STONE_BRICKS));
                for (int y = 1; y <= CH_WALL + CH_X + 3; y++) {
                    BlockState s = null;
                    if (!outside && y <= CH_WALL) {
                        if (wall) {
                            s = this.roll(x, y, z, 5) == 0 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE);
                            // lancet windows down the sides, a rose over the altar
                            if (ax == CH_X && Math.floorMod(z, 4) == 1 && z > CH_Z0 + 1 && z < CH_Z1 - 1 && y >= 3 && y <= 7) s = st(Blocks.GRAY_STAINED_GLASS);
                            if (z == CH_Z0 && ax <= 2 && y >= 4 && y <= 8 - Math.max(0, ax - 1)) s = ax == 0 && y == 6 ? st(Blocks.RED_STAINED_GLASS)
                                    : st(Blocks.ORANGE_STAINED_GLASS);
                            // the way in from the tower
                            if (z == CH_Z1 && ax <= 1 && y <= 4) s = air;
                        } else {
                            s = air;
                        }
                    }
                    // the steep slate roof
                    int ry = CH_WALL + 1 + (CH_X + 1 - ax);
                    if (y > CH_WALL && ax <= CH_X + 1 && z >= CH_Z0 - 1 && z <= CH_Z1 + 1) {
                        if (y == ry) s = x == 0 ? st(Blocks.DEEPSLATE_TILES) : stair(Blocks.DEEPSLATE_TILE_STAIRS, x > 0 ? Direction.WEST : Direction.EAST, false);
                        else if (y < ry && (z == CH_Z0 || z == CH_Z1) && ax <= CH_X) s = st(Blocks.COBBLESTONE);
                        else if (y < ry && !outside) s = air;
                    }
                    if (s != null) this.cset(level, box, x, y, z, s);
                }
            }
        }
        // the pews (facing the altar, to the north) and the altar
        for (int z = CH_Z0 + 5; z <= CH_Z1 - 3; z += 2) {
            for (int x = -CH_X + 1; x <= CH_X - 1; x++) {
                if (Math.abs(x) <= 1 || this.roll(x, 1, z, 5) == 0) continue;
                this.cset(level, box, x, 1, z, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            }
        }
        for (int x = -1; x <= 1; x++) this.cset(level, box, x, 1, CH_Z0 + 2, st(Blocks.CHISELED_STONE_BRICKS));
        this.cset(level, box, -1, 2, CH_Z0 + 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 3));
        this.cset(level, box, 1, 2, CH_Z0 + 2, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 3));
        // the tower over the door, a belfry, the spire with a shell hole through it
        for (int x = -4; x <= 4; x++) {
            for (int z = TOWER_Z0 - 1; z <= TOWER_Z1 + 1; z++) {
                int ax = Math.abs(x), dz = Math.abs(z - (TOWER_Z0 + TOWER_Z1) / 2);
                int m = Math.max(ax, dz);
                if (m > 4) continue;
                boolean shell = m == 3, inside = m < 3;
                if (inside) this.cset(level, box, x, 0, z, st(Blocks.STONE_BRICKS));
                for (int y = 1; y <= SPIRE_TOP + 2; y++) {
                    BlockState s = null;
                    if (y <= TOWER_TOP) {
                        if (m == 4) {
                            // the buttresses at its corners
                            if ((ax == 4 && dz == 3 || dz == 4 && ax == 3) && y <= 14) s = st(Blocks.COBBLESTONE);
                            else if ((ax == 4 && dz == 3 || dz == 4 && ax == 3) && y == 15) s = st(Blocks.COBBLESTONE_SLAB);
                        } else if (shell) {
                            s = this.roll(x, y, z, 6) == 0 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE);
                            boolean face = ax == 0 || dz == 0;
                            if (face && y >= 17 && y <= 20) s = y == 17 ? st(Blocks.IRON_BARS) : air;           // the belfry
                            if (face && y >= 9 && y <= 11 && z != TOWER_Z1) s = st(Blocks.GRAY_STAINED_GLASS);
                            if (z == TOWER_Z1 && ax <= 1 && y <= 4 - (ax == 1 ? 1 : 0)) s = air;                 // the door
                            if (z == TOWER_Z0 && ax <= 1 && y <= 4) s = air;
                        } else if (inside) {
                            s = y == 15 && !(x == 2 && z == TOWER_Z0 + 1) ? st(Blocks.SPRUCE_PLANKS) : air;
                            if (y == 21 && x == 0 && z == (TOWER_Z0 + TOWER_Z1) / 2) s = st(Blocks.SPRUCE_PLANKS);
                            if (y == 20 && x == 0 && z == (TOWER_Z0 + TOWER_Z1) / 2) {
                                s = Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING);
                            }
                            if (x == 2 && z == TOWER_Z0 + 1 && y < 15) s = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
                        }
                    } else if (m <= 3) {
                        // the spire: a square pyramid of slate
                        float r = 3.4F * (SPIRE_TOP + 1 - y) / (SPIRE_TOP - TOWER_TOP);
                        if (m <= r) {
                            boolean surface = m > r - 1.0F;
                            // the shell hole, through its south face
                            boolean hole = y >= 26 && y <= 29 && x >= -1 && x <= 0 && z > (TOWER_Z0 + TOWER_Z1) / 2;
                            s = surface ? (hole ? air : st(Blocks.DEEPSLATE_TILES)) : air;
                        } else if (y == TOWER_TOP + 1 && m == 3) {
                            s = st(Blocks.COBBLESTONE_WALL);
                        }
                    }
                    if (y == SPIRE_TOP + 1 && m == 0) s = st(Blocks.IRON_BARS);
                    if (y == SPIRE_TOP + 2 && m == 0) s = st(Blocks.LIGHTNING_ROD);
                    if (s != null) this.cset(level, box, x, y, z, s);
                }
            }
        }
        // a rifle hidden in the belfry
        this.chest(level, box, this.layout().churchX() - 2, 16, this.layout().churchZ() + TOWER_Z0 + 1, Direction.SOUTH, RIFLE);
    }

    // ---------------------------------------------------------------- the barricades and the garrison

    /** Sandbags across each street where it leaves the village (a gap left to pass), hedgehogs out in front. */
    void barricades(WorldGenLevel level, BoundingBox box) {
        for (int[] e : this.layout().exits()) {
            Direction out = DIRS[e[2]], side = out.getClockWise();
            int bx = e[0] - 2 * out.getStepX(), bz = e[1] - 2 * out.getStepZ();
            for (int k = -2; k <= 2; k++) {
                if (k == 1) continue;   // the gap
                int x = bx + k * side.getStepX(), z = bz + k * side.getStepZ();
                if (!this.layout().open(x, z)) continue;
                this.set(level, box, x, 1, z, st(Blocks.PACKED_MUD));
                this.set(level, box, x, 2, z, (k & 1) == 0 ? st(Blocks.PACKED_MUD) : slab(Blocks.MUD_BRICK_SLAB, SlabType.BOTTOM));
            }
            for (int k = -1; k <= 1; k += 2) {
                int x = e[0] + k * side.getStepX(), z = e[1] + k * side.getStepZ();
                this.set(level, box, x, 1, z, st(Blocks.IRON_BARS));
                this.set(level, box, x, 2, z, st(Blocks.CHAIN));
            }
        }
    }

    /** The garrison: soldiers on the square, at the barricades, in the church tower, in the streets and upstairs. */
    void garrison(WorldGenLevel level, BoundingBox box) {
        Layout l = this.layout();
        this.soldier(level, box, l.plazaX() + 3, 1, l.plazaZ() + 2);
        this.soldier(level, box, l.plazaX() - 3, 1, l.plazaZ() - 2);
        this.soldier(level, box, l.churchX(), 16, l.churchZ() + TOWER_Z0 + 2);
        for (int[] e : l.exits()) {
            Direction out = DIRS[e[2]];
            this.soldier(level, box, e[0] - 4 * out.getStepX(), 1, e[1] - 4 * out.getStepZ());
        }
        RandomSource random = RandomSource.create(this.seed ^ 0x50DL);
        for (int k = 0, placed = 0; k < 300 && placed < 4; k++) {
            int x = random.nextInt(2 * R - 3) - R + 1, z = random.nextInt(2 * R - 3) - R + 1;
            if (l.at(x, z) != STREET) continue;
            this.soldier(level, box, x, 1, z);
            placed++;
        }
        List<House> houses = l.houses();
        int placed = 0;
        for (int i = 0; i < houses.size() && placed < 4; i++) {
            House h = houses.get(i);
            if (h.damage() >= 2 || h.width() < 5 || this.roll(h.fx(), 16, h.fz(), 3) != 0) continue;
            this.soldier(level, box, h.x(h.width() / 2, 2), 5, h.z(h.width() / 2, 2));
            placed++;
        }
    }

    void soldier(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        if (level.getBlockState(pos).blocksMotion() || level.getBlockState(pos.above()).blocksMotion()) return;
        Mob mob = ModEntities.EVOKER_SOLDIER.get().create(level.getLevel());
        if (mob == null) return;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
