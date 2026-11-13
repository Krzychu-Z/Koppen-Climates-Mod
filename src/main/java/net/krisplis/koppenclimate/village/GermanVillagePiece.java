package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.FruitLeavesBlock;
import net.krisplis.koppenclimate.block.custom.HopPoleBlock;
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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A walled German town of the Continental forest (Dfb), on ground levelled for it.
 *
 * <ul>
 *   <li>A ring of town walls (the old fortifications) with round towers under slate cones and four gate towers;
 *       a lane runs round inside the wall.</li>
 *   <li>Inside, a medieval grid: an irregular net of narrow lanes (kinked here and there, some blocks run together)
 *       and two wider main streets crossing the town from gate to gate, meeting at the market square.</li>
 *   <li>The blocks between the streets are lined with half-timbered houses (Fachwerkhäuser) packed wall to wall:
 *       two to four storeys, the upper storeys jutting out over the street, often on a ground floor of cobblestone
 *       or stone; shutters, window boxes, lanterns; every house with its fireplace and chimney. Behind them, small
 *       yards (Hinterhöfe).</li>
 *   <li>The market square with the Maibaum, the market fountain, a linden tree with its bench, market stalls.</li>
 *   <li>The Gothic cathedral squeezed into the block north of the square, its long side to the market: west tower
 *       and spire on the main street, buttresses with pinnacles, tall traceried windows, an organ gallery, pulpit,
 *       choir stalls, a font and the altar under a crucifix.</li>
 *   <li>The Rathaus on the south side of the square, an arcade on the ground floor, its library (lecterns and
 *       shelves) and council chamber above, a ridge turret with a bell.</li>
 *   <li>Outside the walls the town's fields: a lot of barley, potatoes and carrots, some wheat, an apple orchard
 *       and a hop garden (tall hop poles), with paths between them and a hedge round them.</li>
 * </ul>
 */
public class GermanVillagePiece extends StructurePiece {
    // the town wall's inner face; the lane inside it; the fields' outer edge (the levelled ground), and the blend
    static final int R = 56, TOWN = R - 4, WALL = 3, FARM = R + 22, MARGIN = 10, BOX = FARM + MARGIN + 1, CLEAR = 46;
    // the market square (half sizes)
    static final int SQ_X = 12, SQ_Z = 9;
    // the cathedral: the tower's west face, the nave's east end (the apse's middle), its axis and half-width
    static final int C_X0 = -10, C_TX1 = -4, C_NX1 = 16, C_Z = -19, C_NH = 5;
    // the Rathaus: x -RH_HALF..RH_HALF, z RH_Z0..RH_Z0 + RH_DEPTH - 1
    static final int RH_HALF = 6, RH_Z0 = 13, RH_DEPTH = 12;

    // the plan's cell kinds
    static final byte OUT = 0, YARD = 1, STREET = 2, MAIN = 3, SQUARE = 4, LANE = 5, WALL_C = 6, TOWER = 7, GATE = 8,
            CATH = 9, RATH = 10, HOUSE = 11, ROAD = 12;

    static final ResourceKey<LootTable> HOUSE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "chests/german_village_house"));
    static final ResourceKey<LootTable> TREASURY_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "chests/german_village_treasury"));

    static final Block[] WORKSTATIONS = {
            Blocks.SMOKER, Blocks.BLAST_FURNACE, Blocks.COMPOSTER, Blocks.CARTOGRAPHY_TABLE, Blocks.BARREL,
            Blocks.FLETCHING_TABLE, Blocks.CAULDRON, Blocks.STONECUTTER, Blocks.LOOM, Blocks.SMITHING_TABLE,
            Blocks.GRINDSTONE, Blocks.BREWING_STAND, Blocks.COMPOSTER, Blocks.SMOKER, Blocks.BARREL};

    // the fields outside the walls, sector by sector round the town
    static final int F_BARLEY = 0, F_POTATO = 1, F_CARROT = 2, F_WHEAT = 3, F_ORCHARD = 4, F_HOPS = 5;
    static final int[] FIELD_KINDS = {F_BARLEY, F_POTATO, F_BARLEY, F_HOPS, F_CARROT, F_BARLEY, F_POTATO, F_ORCHARD,
            F_CARROT, F_WHEAT};

    final BlockPos centre;
    final long seed;
    private transient Plan plan;

    public GermanVillagePiece(BlockPos centre, long seed) {
        super(VillageRegistry.GERMAN_VILLAGE_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX, centre.getY() - 30, centre.getZ() - BOX,
                centre.getX() + BOX, centre.getY() + CLEAR, centre.getZ() + BOX));
        this.centre = centre;
        this.seed = seed;
    }

    public GermanVillagePiece(CompoundTag tag) {
        super(VillageRegistry.GERMAN_VILLAGE_PIECE.get(), tag);
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

    // ================================================================ the plan

    /**
     * A house: the middle of its front wall (fx, fz), the way it faces (out to its street), its half-width (the front
     * is 2 * half + 1 wide), depth, storeys, how far its upper storeys may jut over the street, and its looks.
     */
    static final class House {
        final int fx, fz, half, depth, storeys, maxJut, index;
        final Direction front;
        final boolean slate;
        final int base;          // ground floor: 0 timbered, 1 cobblestone, 2 stone
        final int look;          // timber, plaster and shutters
        boolean special;         // the Rathaus: no ordinary furnishing

        House(int fx, int fz, Direction front, int half, int depth, int storeys, int maxJut, boolean slate, int base,
              int look, int index) {
            this.fx = fx;
            this.fz = fz;
            this.front = front;
            this.half = half;
            this.depth = depth;
            this.storeys = storeys;
            this.maxJut = maxJut;
            this.slate = slate;
            this.base = base;
            this.look = look;
            this.index = index;
        }

        Direction right() {
            return this.front.getCounterClockWise();
        }

        int x(int u, int v) {
            return this.fx + u * this.right().getStepX() - v * this.front.getStepX();
        }

        int z(int u, int v) {
            return this.fz + u * this.right().getStepZ() - v * this.front.getStepZ();
        }

        int jut(int storey) {
            return storey == 0 ? 0 : Math.min(storey, this.maxJut);
        }

        int top(int f) {
            return f + 4 * this.storeys;
        }

        int ridge(int f) {
            return this.top(f) + this.half + 2;
        }

        /** The house's reach on the ground (roof and chimney included), to skip it in chunks it doesn't touch. */
        boolean touches(BoundingBox box, int cx, int cz) {
            int r = Math.max(this.half + 2, this.depth + 3);
            int mx = cx + this.x(0, this.depth / 2), mz = cz + this.z(0, this.depth / 2);
            return mx + r >= box.minX() && mx - r <= box.maxX() && mz + r >= box.minZ() && mz - r <= box.maxZ();
        }
    }

    /** A street line of the grid: constant x (running north-south) or constant z, kinked once along its length. */
    record Line(boolean constX, int base, int kinkAt, int shift, int kink2, int shift2, int hw, boolean main, long removed) {
        int at(int t) {
            return this.base + (t >= this.kinkAt ? this.shift : 0) + (t >= this.kink2 ? this.shift2 : 0);
        }
    }

    static final class Plan {
        final byte[] cells = new byte[(2 * BOX + 1) * (2 * BOX + 1)];
        final List<House> houses = new ArrayList<>();
        final List<int[]> towers = new ArrayList<>();      // x, z of the wall towers
        final List<int[]> gates = new ArrayList<>();       // x, z, step x, step z (outward)
        double fieldTurn;

        byte get(int x, int z) {
            if (Math.abs(x) > BOX || Math.abs(z) > BOX) return OUT;
            return this.cells[(x + BOX) * (2 * BOX + 1) + z + BOX];
        }

        void put(int x, int z, byte c) {
            if (Math.abs(x) > BOX || Math.abs(z) > BOX) return;
            this.cells[(x + BOX) * (2 * BOX + 1) + z + BOX] = c;
        }
    }

    static boolean open(byte c) {
        return c == STREET || c == MAIN || c == SQUARE || c == LANE;
    }

    Plan plan() {
        if (this.plan == null) this.plan = this.makePlan();
        return this.plan;
    }

    private Plan makePlan() {
        RandomSource r = RandomSource.create(this.seed ^ 0x6A09E667F3BCC909L);
        Plan p = new Plan();
        p.fieldTurn = r.nextDouble() * Math.PI * 2;
        // ---- the ground inside the walls: yards to start with; the lane round inside the wall
        for (int x = -BOX; x <= BOX; x++) {
            for (int z = -BOX; z <= BOX; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d < TOWN) p.put(x, z, YARD);
                else if (d < R) p.put(x, z, LANE);
                else if (d < R + WALL) p.put(x, z, WALL_C);
            }
        }
        // ---- the grid: the square's edge streets, the two main streets, then lanes further out
        List<Line> lines = new ArrayList<>();
        int[][] bases = new int[2][];
        for (int axis = 0; axis < 2; axis++) {
            boolean constX = axis == 0;
            List<Integer> list = new ArrayList<>();
            int edge = constX ? SQ_X + 1 : SQ_Z + 1;
            list.add(-edge);
            list.add(edge);
            for (int s : new int[]{-1, 1}) {
                int pos = s * edge;
                boolean first = true;
                while (true) {
                    int block = 12 + r.nextInt(5);
                    // the block north of the square holds the cathedral
                    if (!constX && s < 0 && first) block = 17;
                    pos += s * (3 + block);
                    first = false;
                    if (Math.abs(pos) > TOWN - 5) break;
                    list.add(pos);
                }
            }
            list.sort(Integer::compare);
            bases[axis] = list.stream().mapToInt(Integer::intValue).toArray();
        }
        for (int axis = 0; axis < 2; axis++) {
            boolean constX = axis == 0;
            int edge = constX ? SQ_X + 1 : SQ_Z + 1;
            for (int b : bases[axis]) {
                // the main streets: north-south along the square's west side, east-west along its south side
                boolean main = constX ? b == -edge : b == edge;
                boolean squareEdge = Math.abs(b) == edge;
                int kinkAt = 0, shift = 0, kink2 = BOX + 1, shift2 = 0;
                long removed = 0;
                if (!squareEdge) {
                    // the old lanes bend: twice along their length, by a block or three
                    kinkAt = r.nextInt(41) - 30;
                    shift = (r.nextBoolean() ? 1 : -1) * (1 + r.nextInt(3));
                    kink2 = kinkAt + 12 + r.nextInt(25);
                    shift2 = (r.nextBoolean() ? 1 : -1) * (1 + r.nextInt(2));
                    // some stretches between crossings are built over (the blocks run together)
                    for (int seg = 0; seg < 16; seg++) if (r.nextInt(100) < 15) removed |= 1L << seg;
                }
                lines.add(new Line(constX, b, kinkAt, shift, kink2, shift2, main ? 2 : 1, main, removed));
            }
        }
        for (Line line : lines) {
            int[] crossing = bases[line.constX ? 1 : 0];
            for (int t = -BOX; t <= BOX; t++) {
                int seg = 0;
                while (seg < crossing.length && crossing[seg] < t) seg++;
                if ((line.removed >>> seg & 1) != 0) continue;
                int c = line.at(t);
                for (int d = -line.hw; d <= line.hw; d++) {
                    int x = line.constX ? c + d : t, z = line.constX ? t : c + d;
                    double dist = Math.sqrt(x * x + z * z);
                    if (dist < TOWN) {
                        byte now = p.get(x, z);
                        if (now == YARD || (line.main && now == STREET)) p.put(x, z, line.main ? MAIN : STREET);
                    } else if (line.main && dist >= R + WALL && dist < FARM + MARGIN) {
                        p.put(x, z, ROAD);
                    }
                }
            }
        }
        // ---- the square, the cathedral's and the Rathaus's ground
        for (int x = -SQ_X; x <= SQ_X; x++) for (int z = -SQ_Z; z <= SQ_Z; z++) p.put(x, z, SQUARE);
        for (int x = C_X0; x <= C_NX1 + C_NH + 1; x++) {
            for (int z = C_Z - C_NH - 3; z <= C_Z + C_NH + 2; z++) p.put(x, z, CATH);
        }
        for (int x = -RH_HALF; x <= RH_HALF; x++) {
            for (int z = RH_Z0; z < RH_Z0 + RH_DEPTH; z++) p.put(x, z, RATH);
        }
        // ---- the gates, where the main streets meet the wall
        double gr = R + 1.5;
        int mx = -(SQ_X + 1), mz = SQ_Z + 1;
        int gz = (int) Math.round(Math.sqrt(gr * gr - mx * mx)), gx = (int) Math.round(Math.sqrt(gr * gr - mz * mz));
        p.gates.add(new int[]{mx, -gz, 0, -1});
        p.gates.add(new int[]{mx, gz, 0, 1});
        p.gates.add(new int[]{gx, mz, 1, 0});
        p.gates.add(new int[]{-gx, mz, -1, 0});
        for (int[] g : p.gates) {
            for (int a = -3; a <= 3; a++) {
                for (int c = -5; c <= 5; c++) {
                    int x = g[0] + a * g[2] + c * Math.abs(g[3]), z = g[1] + a * g[3] + c * Math.abs(g[2]);
                    p.put(x, z, Math.abs(c) <= 2 ? MAIN : GATE);
                }
            }
        }
        // ---- the round towers along the wall, clear of the gates
        for (int k = 0; k < 8; k++) {
            double a = (k + 0.5) * Math.PI / 4 + 0.15;
            int tx = (int) Math.round(Math.cos(a) * (R + 1.5)), tz = (int) Math.round(Math.sin(a) * (R + 1.5));
            boolean near = false;
            for (int[] g : p.gates) if (Mth.length(tx - g[0], tz - g[1]) < 15) near = true;
            if (near) continue;
            p.towers.add(new int[]{tx, tz});
        }
        // ---- the houses, packed wall to wall along the streets: the square and main streets first
        int[] index = {0};
        this.placeHouses(p, r, index, true, false);
        this.placeHouses(p, r, index, false, false);
        this.placeHouses(p, r, index, false, true);
        this.placeHouses(p, r, index, false, false);
        // the Rathaus is a house too, a grand one
        House rathaus = new House(0, RH_Z0, Direction.NORTH, RH_HALF, RH_DEPTH, 3, 0, true, 2, 0, -1);
        rathaus.special = true;
        p.houses.add(rathaus);
        p.houses.sort(Comparator.comparingInt(h -> h.storeys));
        return p;
    }

    private void placeHouses(Plan p, RandomSource r, int[] index, boolean grand, boolean reverse) {
        Direction[] dirs = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (int x = -TOWN; x <= TOWN; x++) {
            for (int z = -TOWN; z <= TOWN; z++) {
                if (p.get(x, z) != YARD) continue;
                for (Direction d : dirs) {
                    byte facing = p.get(x + d.getStepX(), z + d.getStepZ());
                    if (!open(facing)) continue;
                    if (grand && facing != SQUARE && facing != MAIN) continue;
                    // the house runs from this cell along the street, the way the scan goes
                    Direction along = d.getAxis() == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
                    if (reverse) along = along.getOpposite();
                    int[] halves = grand ? new int[]{3 + r.nextInt(2), 3} : new int[]{2 + r.nextInt(2), 2};
                    boolean placed = false;
                    for (int half : halves) {
                        for (int k = 0; k <= 2 * half && !placed; k++)
                        for (int depth = 9; depth >= 5 && !placed; depth--) {
                            // the house reaches from this cell along the street (or k cells back the other way)
                            int fx = x + along.getStepX() * (half - k), fz = z + along.getStepZ() * (half - k);
                            int storeys = grand ? 3 + r.nextInt(2) : 2 + r.nextInt(3) / 2 + r.nextInt(2);
                            int base = r.nextInt(10) < 4 ? 1 : r.nextInt(10) < 3 ? 2 : 0;
                            House h = new House(fx, fz, d, half, depth, Math.min(4, storeys), 0, r.nextBoolean(),
                                    base, r.nextInt(1 << 12), index[0]);
                            if (!this.fits(p, h)) continue;
                            int jut = this.jutRoom(p, h);
                            h = new House(fx, fz, d, half, depth, h.storeys, jut, h.slate, base, h.look, index[0]++);
                            for (int u = -half; u <= half; u++) {
                                for (int v = 0; v < depth; v++) p.put(h.x(u, v), h.z(u, v), HOUSE);
                            }
                            // the lane goes round the house's front
                            if (p.get(h.x(0, -1), h.z(0, -1)) == LANE) {
                                for (int u = -half - 1; u <= half + 1; u++) {
                                    if (p.get(h.x(u, -1), h.z(u, -1)) == YARD) p.put(h.x(u, -1), h.z(u, -1), LANE);
                                }
                            }
                            p.houses.add(h);
                            placed = true;
                        }
                        if (placed) break;
                    }
                    if (placed) break;
                }
            }
        }
    }

    private boolean fits(Plan p, House h) {
        byte facing = p.get(h.x(0, -1), h.z(0, -1));
        if (!open(facing)) return false;
        for (int u = -h.half; u <= h.half; u++) {
            for (int v = 0; v < h.depth; v++) {
                byte c = p.get(h.x(u, v), h.z(u, v));
                // along the curving lane inside the wall, a house's front corners may stand in the lane
                if (c == LANE && facing == LANE && v <= 1 && Math.abs(u) >= h.half - 1) continue;
                if (c != YARD) return false;
            }
        }
        return true;
    }

    /** How far the upper storeys may jut: one block over a lane, two over the main streets and the square. */
    private int jutRoom(Plan p, House h) {
        int jut = 2;
        for (int u = -h.half; u <= h.half; u++) {
            for (int v = -1; v >= -2; v--) {
                byte c = p.get(h.x(u, v), h.z(u, v));
                boolean ok = c == MAIN || c == SQUARE || (v == -1 && (c == STREET || c == LANE || c == YARD));
                if (!ok) jut = Math.min(jut, -v - 1);
            }
        }
        return jut;
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int f = this.centre.getY();
        Plan p = this.plan();
        this.connecting = new ArrayList<>();
        this.shapeGround(level, box, f);
        int cx = this.centre.getX(), cz = this.centre.getZ();
        LooseTreeCleanup.clean(level, chunkPos, f - 12, f + CLEAR + 6,
                (x, z) -> platformDistance(x - cx, z - cz) < MARGIN + 10,
                (x, z) -> platformDistance(x - cx, z - cz) == 0);
        this.paveAndPlant(level, box, f, p);
        this.buildWalls(level, box, f, p);
        this.buildSquare(level, box, f);
        this.buildCathedral(level, box, f);
        for (House h : p.houses) {
            Block ws = WORKSTATIONS[Math.floorMod(h.index, WORKSTATIONS.length)];
            if (h.touches(box, cx, cz)) this.buildHouse(level, box, h, f, ws);
        }
        this.furnishRathaus(level, box, f);
        this.joinUp(level, box);
        this.spawnLife(level, box, f, p);
    }

    BlockPos at(int x, int y, int z) {
        return new BlockPos(this.centre.getX() + x, y, this.centre.getZ() + z);
    }

    void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, state, 2);
        if (connects(state) && this.connecting != null) this.connecting.add(pos.immutable());
    }

    // panes, bars, fences and walls placed in this chunk: joined up to their neighbours once everything stands
    private transient List<BlockPos> connecting;

    static boolean connects(BlockState state) {
        return state.getBlock() instanceof CrossCollisionBlock || state.getBlock() instanceof WallBlock;
    }

    void joinUp(WorldGenLevel level, BoundingBox box) {
        java.util.Set<BlockPos> done = new java.util.HashSet<>();
        for (BlockPos pos : this.connecting) {
            this.join(level, pos, done);
            // and those already standing just over the chunk's edge, towards these
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = pos.relative(d);
                if (!box.isInside(n) && connects(level.getBlockState(n))) this.join(level, n, done);
            }
        }
        this.connecting = null;
    }

    private void join(WorldGenLevel level, BlockPos pos, java.util.Set<BlockPos> done) {
        if (!done.add(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (!connects(state)) return;
        BlockState joined = Block.updateFromNeighbourShapes(state, level, pos);
        if (joined != state) level.setBlock(pos, joined, 2);
    }

    static BlockState st(Block block) {
        return block.defaultBlockState();
    }

    static BlockState stair(Block block, Direction facing, boolean upsideDown) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
    }

    static BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    static BlockState slab(Block block, SlabType type) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    static BlockState hanging() {
        return st(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
    }

    long hash(int x, int z, int salt) {
        long h = (x * 341873128712L + z * 132897987541L + salt * 6971L) ^ this.seed;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        return h ^ (h >>> 32);
    }

    int roll(int x, int z, int salt, int n) {
        return (int) Math.floorMod(this.hash(x, z, salt), (long) n);
    }

    // ---------------------------------------------------------------- the ground

    static double platformDistance(int x, int z) {
        return Math.max(0.0, Math.sqrt((double) x * x + (double) z * z) - FARM);
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
                    level.setBlock(pos.set(wx, y, wz), y < target - 3 ? st(Blocks.STONE) : dirt, 2);
                }
                level.setBlock(pos.set(wx, target, wz), grass, 2);
                if (level.getBlockState(pos.set(wx, target - 1, wz)).is(Blocks.GRASS_BLOCK)) level.setBlock(pos, dirt, 2);
            }
        }
    }

    // ---------------------------------------------------------------- streets, yards and fields

    int fieldKind(Plan p, int x, int z) {
        double a = Math.atan2(z, x) - p.fieldTurn;
        a = Mth.positiveModulo(a, Math.PI * 2);
        return FIELD_KINDS[Math.min(FIELD_KINDS.length - 1, (int) (a / (Math.PI * 2) * FIELD_KINDS.length))];
    }

    /** Whether (x, z) lies on a path between two fields (along the sector boundaries). */
    boolean fieldPath(Plan p, int x, int z, double d) {
        double a = Mth.positiveModulo(Math.atan2(z, x) - p.fieldTurn, Math.PI * 2);
        double sector = Math.PI * 2 / FIELD_KINDS.length;
        double off = a % sector;
        double gap = Math.min(off, sector - off) * d;
        return gap < 1.0;
    }

    void paveAndPlant(WorldGenLevel level, BoundingBox box, int f, Plan p) {
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int x = wx - this.centre.getX(), z = wz - this.centre.getZ();
                if (Math.abs(x) > BOX || Math.abs(z) > BOX) continue;
                double d = Math.sqrt(x * x + z * z);
                if (d >= FARM + 1) continue;
                byte c = p.get(x, z);
                int roll = this.roll(x, z, 1, 100);
                BlockState s;
                switch (c) {
                    case STREET -> s = roll < 45 ? st(Blocks.COBBLESTONE) : roll < 70 ? st(Blocks.ANDESITE)
                            : roll < 85 ? st(Blocks.MOSSY_COBBLESTONE) : roll < 93 ? st(Blocks.GRAVEL) : st(Blocks.STONE);
                    case LANE -> s = roll < 40 ? st(Blocks.GRAVEL) : roll < 75 ? st(Blocks.COBBLESTONE)
                            : roll < 90 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COARSE_DIRT);
                    case MAIN -> s = roll < 40 ? st(Blocks.STONE_BRICKS) : roll < 65 ? st(Blocks.POLISHED_ANDESITE)
                            : roll < 85 ? st(Blocks.COBBLESTONE) : st(Blocks.CRACKED_STONE_BRICKS);
                    case SQUARE -> {
                        boolean edge = Math.abs(x) == SQ_X || Math.abs(z) == SQ_Z;
                        int ring = Math.max(Math.abs(x), Math.abs(z));
                        s = edge ? st(Blocks.CHISELED_STONE_BRICKS) : ring % 4 == 0 ? st(Blocks.POLISHED_ANDESITE)
                                : ((x + z) & 1) == 0 ? st(Blocks.STONE_BRICKS)
                                : roll < 30 ? st(Blocks.CRACKED_STONE_BRICKS) : st(Blocks.POLISHED_DIORITE);
                    }
                    case ROAD -> s = roll < 70 ? st(Blocks.DIRT_PATH) : st(Blocks.GRAVEL);
                    case YARD -> s = st(Blocks.GRASS_BLOCK);
                    case OUT -> s = null;
                    default -> s = st(Blocks.COBBLESTONE);
                }
                if (c == OUT) {
                    this.field(level, box, f, p, x, z, d);
                    continue;
                }
                this.set(level, box, x, f, z, s);
                if (c == YARD) this.yardDecor(level, box, f, x, z);
            }
        }
    }

    /** The back yards: grass, a few flowers, woodpiles, barrels, hay, a bench. */
    void yardDecor(WorldGenLevel level, BoundingBox box, int f, int x, int z) {
        int k = this.roll(x, z, 2, 400);
        BlockState s = null;
        if (k < 70) s = st(Blocks.SHORT_GRASS);
        else if (k < 80) s = st(Blocks.DANDELION);
        else if (k < 86) s = st(Blocks.POPPY);
        else if (k < 90) s = st(Blocks.CORNFLOWER);
        else if (k < 96) s = log(Blocks.SPRUCE_LOG, (x & 1) == 0 ? Direction.Axis.X : Direction.Axis.Z);
        else if (k < 100) s = st(Blocks.BARREL);
        else if (k < 103) s = st(Blocks.HAY_BLOCK);
        else if (k < 105) s = st(Blocks.CHIPPED_ANVIL);
        else if (k < 108) s = st(Blocks.LILAC);
        if (s == null) return;
        if (s.is(Blocks.LILAC)) {
            this.set(level, box, x, f + 1, z, s.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(level, box, x, f + 2, z, s.setValue(net.minecraft.world.level.block.DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
            return;
        }
        this.set(level, box, x, f + 1, z, s);
        if (s.is(Blocks.SPRUCE_LOG) && this.roll(x, z, 3, 2) == 0) this.set(level, box, x, f + 2, z, s);
    }

    void field(WorldGenLevel level, BoundingBox box, int f, Plan p, int x, int z, double d) {
        if (d < R + WALL + 2) {
            // the path round the outside of the wall
            this.set(level, box, x, f, z, this.roll(x, z, 4, 3) == 0 ? st(Blocks.GRAVEL) : st(Blocks.DIRT_PATH));
            return;
        }
        if (d >= FARM - 1.2) {
            // the hedge round the fields
            this.set(level, box, x, f, z, st(Blocks.GRASS_BLOCK));
            if (d < FARM) this.set(level, box, x, f + 1, z, st(Blocks.OAK_LEAVES).setValue(LeavesBlock.PERSISTENT, true));
            return;
        }
        if (this.fieldPath(p, x, z, d)) {
            this.set(level, box, x, f, z, st(Blocks.DIRT_PATH));
            return;
        }
        int kind = this.fieldKind(p, x, z);
        boolean alongX = Math.floorMod((int) ((Mth.positiveModulo(Math.atan2(z, x) - p.fieldTurn, Math.PI * 2))
                / (Math.PI * 2) * FIELD_KINDS.length), 2) == 0;
        int across = alongX ? z : x, along = alongX ? x : z;
        switch (kind) {
            case F_ORCHARD -> {
                this.set(level, box, x, f, z, st(Blocks.GRASS_BLOCK));
                if (Math.floorMod(x, 7) == 3 && Math.floorMod(z, 7) == 3 && d > R + WALL + 6 && d < FARM - 4) {
                    this.appleTree(level, box, f, x, z);
                } else if (this.roll(x, z, 5, 6) == 0) {
                    this.set(level, box, x, f + 1, z, st(Blocks.SHORT_GRASS));
                }
            }
            case F_HOPS -> {
                this.set(level, box, x, f, z, st(Blocks.COARSE_DIRT));
                if (Math.floorMod(across, 3) == 1 && Math.floorMod(along, 2) == 0) {
                    for (int y = f + 1; y <= f + 6; y++) {
                        int age = y >= f + 5 ? this.roll(x, z, y, 2) : 1 + this.roll(x, z, y, 3);
                        this.set(level, box, x, y, z, ModBlocks.HOP_POLE.get().defaultBlockState()
                                .setValue(HopPoleBlock.HOPS, true).setValue(HopPoleBlock.AGE, age));
                    }
                } else if (Math.floorMod(across, 3) != 1 && this.roll(x, z, 6, 5) == 0) {
                    this.set(level, box, x, f, z, st(Blocks.GRASS_BLOCK));
                }
            }
            default -> {
                if (Math.floorMod(across, 9) == 4) {
                    this.set(level, box, x, f, z, st(Blocks.WATER));
                    return;
                }
                this.set(level, box, x, f, z, st(Blocks.FARMLAND).setValue(FarmBlock.MOISTURE, 7));
                int age = 3 + this.roll(x, z, 7, 5);
                BlockState crop = switch (kind) {
                    case F_BARLEY -> ModBlocks.BARLEY.get().getStateForAge(Math.min(age, ModBlocks.BARLEY.get().getMaxAge()));
                    case F_POTATO -> ((CropBlock) Blocks.POTATOES).getStateForAge(age);
                    case F_CARROT -> ((CropBlock) Blocks.CARROTS).getStateForAge(age);
                    default -> ((CropBlock) Blocks.WHEAT).getStateForAge(age);
                };
                this.set(level, box, x, f + 1, z, crop);
                // a scarecrow now and then
                if (this.roll(x, z, 8, 900) == 0) {
                    this.set(level, box, x, f, z, st(Blocks.COARSE_DIRT));
                    this.set(level, box, x, f + 1, z, st(Blocks.SPRUCE_FENCE));
                    this.set(level, box, x, f + 2, z, st(Blocks.HAY_BLOCK));
                    this.set(level, box, x, f + 3, z, st(Blocks.CARVED_PUMPKIN));
                }
            }
        }
    }

    void appleTree(WorldGenLevel level, BoundingBox box, int f, int x, int z) {
        BlockState wood = log(ModBlocks.APPLE_LOG.get(), Direction.Axis.Y);
        for (int y = f + 1; y <= f + 3; y++) this.set(level, box, x, y, z, wood);
        for (int dy = 3; dy <= 6; dy++) {
            int rad = dy <= 4 ? 2 : dy == 5 ? 1 : 0;
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (rad == 2 && Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
                    if (dx == 0 && dz == 0 && dy <= 3) continue;
                    if (rad == 1 && Math.abs(dx) + Math.abs(dz) == 2 && this.roll(x + dx, z + dz, dy, 2) == 0) continue;
                    BlockState leaf = ModBlocks.APPLE_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
                    if (leaf.hasProperty(FruitLeavesBlock.FRUIT)) leaf = leaf.setValue(FruitLeavesBlock.FRUIT, this.roll(x + dx, z + dz, dy, 3));
                    this.set(level, box, x + dx, f + dy, z + dz, leaf);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the town walls, towers and gates

    BlockState wallStone(int x, int y, int z) {
        int k = this.roll(x * 31 + y, z, 9, 100);
        return k < 45 ? st(Blocks.STONE_BRICKS) : k < 70 ? st(Blocks.COBBLESTONE) : k < 82 ? st(Blocks.MOSSY_COBBLESTONE)
                : k < 93 ? st(Blocks.MOSSY_STONE_BRICKS) : st(Blocks.CRACKED_STONE_BRICKS);
    }

    void buildWalls(WorldGenLevel level, BoundingBox box, int f, Plan p) {
        int x0 = box.minX() - this.centre.getX(), x1 = box.maxX() - this.centre.getX();
        int z0 = box.minZ() - this.centre.getZ(), z1 = box.maxZ() - this.centre.getZ();
        for (int x = Math.max(x0, -R - WALL); x <= Math.min(x1, R + WALL); x++) {
            for (int z = Math.max(z0, -R - WALL); z <= Math.min(z1, R + WALL); z++) {
                if (p.get(x, z) != WALL_C) continue;
                double d = Math.sqrt(x * x + z * z);
                for (int y = f; y <= f + 6; y++) this.set(level, box, x, y, z, this.wallStone(x, y, z));
                // the wall walk on top, the battlements on its outer edge
                if (d >= R + WALL - 1) {
                    int step = (int) Math.round(Math.atan2(z, x) * (R + 2.5));
                    if (Math.floorMod(step, 2) == 0) this.set(level, box, x, f + 7, z, this.wallStone(x, f + 7, z));
                } else if (d < R + 0.7) {
                    // the inner edge: a low parapet of slabs
                    this.set(level, box, x, f + 7, z, slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
                }
            }
        }
        for (int[] t : p.towers) this.buildTower(level, box, f, t[0], t[1]);
        for (int[] g : p.gates) this.buildGate(level, box, f, g[0], g[1], g[2], g[3]);
    }

    /** A round wall tower: stone, a floor at the wall walk with doors onto it, a corbelled top, a slate cone. */
    void buildTower(WorldGenLevel level, BoundingBox box, int f, int tx, int tz) {
        int top = f + 11;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                int x = tx + dx, z = tz + dz;
                if (d <= 3.6) {
                    boolean rim = d > 2.5;
                    this.set(level, box, x, f, z, st(Blocks.COBBLESTONE));
                    for (int y = f + 1; y <= top; y++) {
                        BlockState s = rim ? this.wallStone(x, y, z) : st(Blocks.AIR);
                        // arrow slits
                        if (rim && (y == f + 3 || y == f + 9) && (dx == 0 || dz == 0)) s = st(Blocks.AIR);
                        this.set(level, box, x, y, z, s);
                    }
                    if (!rim) {
                        this.set(level, box, x, f + 6, z, st(Blocks.SPRUCE_PLANKS));
                        this.set(level, box, x, top, z, st(Blocks.SPRUCE_PLANKS));
                    }
                }
                // the corbelled parapet
                if (d > 3.6 && d <= 4.4) this.set(level, box, x, top, z, slab(Blocks.STONE_BRICK_SLAB, SlabType.TOP));
                // the slate cone
                for (int k = 0; k <= 7; k++) {
                    double rad = 4.4 - k * 0.6;
                    if (d <= rad && d > rad - 1.3) this.set(level, box, x, top + 1 + k, z, st(Blocks.DEEPSLATE_TILES));
                }
            }
        }
        this.set(level, box, tx, top + 9, tz, st(Blocks.DEEPSLATE_TILE_WALL));
        this.set(level, box, tx, top + 10, tz, st(Blocks.LIGHTNING_ROD));
        // the doors: at the foot on the town side, and onto the wall walk either side
        double len = Math.sqrt(tx * tx + tz * tz);
        int ix = (int) Math.round(-tx / len * 3), iz = (int) Math.round(-tz / len * 3);
        this.set(level, box, tx + ix, f + 1, tz + iz, st(Blocks.AIR));
        this.set(level, box, tx + ix, f + 2, tz + iz, st(Blocks.AIR));
        int sx = (int) Math.round(-tz / len * 3), sz = (int) Math.round(tx / len * 3);
        for (int s : new int[]{-1, 1}) {
            this.set(level, box, tx + s * sx, f + 7, tz + s * sz, st(Blocks.AIR));
            this.set(level, box, tx + s * sx, f + 8, tz + s * sz, st(Blocks.AIR));
        }
        // the ladder up the inside
        Direction face = Math.abs(ix) >= Math.abs(iz) ? (ix > 0 ? Direction.EAST : Direction.WEST) : (iz > 0 ? Direction.SOUTH : Direction.NORTH);
        int lx = tx - face.getStepX() * 2, lz = tz - face.getStepZ() * 2;
        for (int y = f + 1; y <= top; y++) this.set(level, box, lx, y, lz, st(Blocks.LADDER).setValue(LadderBlock.FACING, face));
        this.set(level, box, tx, top - 1, tz, hanging());
        this.set(level, box, tx, top + 1, tz, st(Blocks.LANTERN));
        this.set(level, box, tx, f + 5, tz, hanging());
    }

    /**
     * A gate tower over the road: a vaulted passage with its portcullis raised, two storeys above, a hipped slate
     * roof, the town's banners on its outer face.
     */
    void buildGate(WorldGenLevel level, BoundingBox box, int f, int gx, int gz, int ox, int oz) {
        // a: along the road (outward), c: across it
        int cx = Math.abs(oz), cz = Math.abs(ox);
        Direction outward = Direction.fromDelta(ox, 0, oz), side = Direction.fromDelta(cx, 0, cz);
        int top = f + 13;
        for (int a = -3; a <= 3; a++) {
            for (int c = -5; c <= 5; c++) {
                int x = gx + a * ox + c * cx, z = gz + a * oz + c * cz;
                boolean rim = Math.abs(a) == 3 || Math.abs(c) == 5;
                boolean passage = Math.abs(c) <= 2;
                for (int y = f + 1; y <= top; y++) {
                    BlockState s = rim ? this.wallStone(x, y, z) : st(Blocks.AIR);
                    if (passage && y <= f + 5) s = st(Blocks.AIR);
                    if (passage && y == f + 5 && Math.abs(c) == 2) s = stair(Blocks.STONE_BRICK_STAIRS, c < 0 ? side.getOpposite() : side, true);
                    if (passage && y == f + 6) s = this.wallStone(x, y, z);
                    if (!rim && !passage && y <= f + 6) s = this.wallStone(x, y, z);
                    // windows above
                    if (rim && Math.abs(a) == 3 && (y == f + 8 || y == f + 11) && (c == -2 || c == 2)) s = st(Blocks.AIR);
                    if (!rim && (y == f + 10)) s = st(Blocks.SPRUCE_PLANKS);
                    this.set(level, box, x, y, z, s);
                }
                // the portcullis, raised, at each end of the passage
                if (passage && Math.abs(a) == 3) this.set(level, box, x, f + 5, z, st(Blocks.IRON_BARS));
                // the battlements and the hipped roof
                if (rim && Math.floorMod(a + c, 2) == 0) this.set(level, box, x, top + 1, z, this.wallStone(x, top + 1, z));
            }
        }
        for (int k = 0; k <= 3; k++) {
            for (int a = -2 + Math.min(k, 2); a <= 2 - Math.min(k, 2); a++) {
                for (int c = -4 + k; c <= 4 - k; c++) {
                    boolean edge = Math.abs(c) == 4 - k || Math.abs(a) == 2 - Math.min(k, 2);
                    if (!edge && k < 3) continue;
                    int x = gx + a * ox + c * cx, z = gz + a * oz + c * cz;
                    this.set(level, box, x, top + 2 + k, z, k == 3 ? slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM) : st(Blocks.DEEPSLATE_TILES));
                }
            }
        }
        // the side room off the passage, the ladder up from it, a lantern in the vault, the banners
        for (int y = f + 1; y <= f + 10; y++) {
            this.set(level, box, gx + 4 * cx, y, gz + 4 * cz, st(Blocks.LADDER).setValue(LadderBlock.FACING, side.getOpposite()));
        }
        for (int y = f + 1; y <= f + 2; y++) this.set(level, box, gx + 3 * cx, y, gz + 3 * cz, st(Blocks.AIR));
        this.set(level, box, gx, f + 5, gz, hanging());
        // lanterns in the rooms above the passage
        this.set(level, box, gx, f + 9, gz, hanging());
        this.set(level, box, gx, f + 11, gz, st(Blocks.LANTERN));
        for (int s : new int[]{-1, 1}) {
            int x = gx + 4 * ox + s * 3 * cx, z = gz + 4 * oz + s * 3 * cz;
            this.set(level, box, x, f + 9, z, st(s < 0 ? Blocks.RED_WALL_BANNER : Blocks.WHITE_WALL_BANNER).setValue(WallBannerBlock.FACING, outward));
            int ix = gx - 4 * ox + s * 3 * cx, iz = gz - 4 * oz + s * 3 * cz;
            this.set(level, box, ix, f + 9, iz, st(s < 0 ? Blocks.RED_WALL_BANNER : Blocks.WHITE_WALL_BANNER).setValue(WallBannerBlock.FACING, outward.getOpposite()));
        }
    }

    // ---------------------------------------------------------------- the market square

    void buildSquare(WorldGenLevel level, BoundingBox box, int f) {
        this.buildMaibaum(level, box, f);
        this.buildFountain(level, box, f, -7, 4);
        this.buildLinden(level, box, f, 7, -4);
        int[][] stalls = {{3, 4}, {8, 5}, {-10, -6}, {-4, -7}};
        Block[] awnings = {Blocks.RED_WOOL, Blocks.WHITE_WOOL, Blocks.YELLOW_WOOL, Blocks.WHITE_WOOL};
        Block[] goods = {Blocks.HAY_BLOCK, Blocks.PUMPKIN, Blocks.MELON, Blocks.BARREL, Blocks.BEEHIVE};
        int n = 0;
        for (int[] s : stalls) {
            for (int dx = 0; dx <= 2; dx++) {
                for (int dz = 0; dz <= 1; dz++) this.set(level, box, s[0] + dx, f + 4, s[1] + dz, st(dx == 1 ? awnings[n] : Blocks.WHITE_WOOL));
                this.set(level, box, s[0] + dx, f + 1, s[1], st(dx == 1 ? goods[n % goods.length] : Blocks.BARREL));
            }
            for (int[] c : new int[][]{{0, 1}, {2, 1}}) {
                for (int y = f + 1; y <= f + 3; y++) this.set(level, box, s[0] + c[0], y, s[1] + c[1], st(Blocks.SPRUCE_FENCE));
            }
            n++;
        }
        // lamp posts at the corners
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                int x = sx * (SQ_X - 1), z = sz * (SQ_Z - 1);
                for (int y = f + 1; y <= f + 3; y++) this.set(level, box, x, y, z, st(Blocks.DARK_OAK_FENCE));
                this.set(level, box, x, f + 4, z, st(Blocks.LANTERN));
            }
        }
        // benches along the square's edge
        for (int x = -9; x <= 9; x += 6) {
            this.set(level, box, x, f + 1, -SQ_Z + 1, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
            this.set(level, box, x + 1, f + 1, -SQ_Z + 1, stair(Blocks.SPRUCE_STAIRS, Direction.NORTH, false));
        }
    }

    /** The Maibaum: a tall stripped birch pole, a ring garland, arms with coloured ribbons, a crown of birch leaves. */
    void buildMaibaum(WorldGenLevel level, BoundingBox box, int f) {
        int top = f + 22;
        this.set(level, box, 0, f, 0, st(Blocks.CHISELED_STONE_BRICKS));
        for (int y = f + 1; y <= top; y++) this.set(level, box, 0, y, 0, log(Blocks.STRIPPED_BIRCH_LOG, Direction.Axis.Y));
        BlockState leaves = st(Blocks.BIRCH_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    if (Math.abs(dx) + Math.abs(dz) > (dy == 3 ? 0 : dy == 0 ? 1 : 2)) continue;
                    if (dx == 0 && dz == 0 && dy == 0) continue;
                    this.set(level, box, dx, top + dy, dz, leaves);
                }
            }
        }
        this.set(level, box, 0, top + 1, 0, log(Blocks.BIRCH_LOG, Direction.Axis.Y));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 2 && Math.abs(dx) + Math.abs(dz) < 4) {
                    this.set(level, box, dx, top - 3, dz, st(Blocks.SPRUCE_LEAVES).setValue(LeavesBlock.PERSISTENT, true));
                }
            }
        }
        Block[] ribbons = {Blocks.RED_WALL_BANNER, Blocks.YELLOW_WALL_BANNER, Blocks.GREEN_WALL_BANNER, Blocks.WHITE_WALL_BANNER};
        int n = 0;
        for (int y = f + 9; y <= top - 5; y += 3) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                this.set(level, box, d.getStepX(), y, d.getStepZ(), st(Blocks.BIRCH_FENCE));
                this.set(level, box, d.getStepX(), y - 1, d.getStepZ(), ribbons[(n++) % ribbons.length].defaultBlockState().setValue(WallBannerBlock.FACING, d));
            }
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            this.set(level, box, d.getStepX() * 3, f + 1, d.getStepZ() * 3, stair(Blocks.SPRUCE_STAIRS, d, false));
        }
    }

    /** The market fountain: a stone basin, a column in the middle with a gilded figure (a golden block) on top. */
    void buildFountain(WorldGenLevel level, BoundingBox box, int f, int wx, int wz) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                if (corner) continue;
                boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                this.set(level, box, wx + dx, f - 1, wz + dz, st(Blocks.STONE_BRICKS));
                this.set(level, box, wx + dx, f, wz + dz, rim ? st(Blocks.STONE_BRICKS) : st(Blocks.WATER));
                if (rim) this.set(level, box, wx + dx, f + 1, wz + dz, slab(Blocks.SMOOTH_STONE_SLAB, SlabType.BOTTOM));
            }
        }
        this.set(level, box, wx, f, wz, st(Blocks.STONE_BRICKS));
        this.set(level, box, wx, f + 1, wz, st(Blocks.STONE_BRICK_WALL));
        this.set(level, box, wx, f + 2, wz, st(Blocks.CHISELED_STONE_BRICKS));
        this.set(level, box, wx, f + 3, wz, st(Blocks.STONE_BRICK_WALL));
        this.set(level, box, wx, f + 4, wz, st(Blocks.GOLD_BLOCK));
        for (Direction d : Direction.Plane.HORIZONTAL) {
            this.set(level, box, wx + d.getStepX(), f + 2, wz + d.getStepZ(), stair(Blocks.STONE_BRICK_STAIRS, d.getOpposite(), true));
        }
    }

    /** The town linden (Dorflinde) with a round bench about its trunk. */
    void buildLinden(WorldGenLevel level, BoundingBox box, int f, int tx, int tz) {
        for (int y = f + 1; y <= f + 5; y++) this.set(level, box, tx, y, tz, log(Blocks.OAK_LOG, Direction.Axis.Y));
        BlockState leaf = st(Blocks.OAK_LEAVES).setValue(LeavesBlock.PERSISTENT, true);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 4; dy <= 9; dy++) {
                    double d = Math.sqrt(dx * dx + dz * dz + (dy - 6.5) * (dy - 6.5) * 1.4);
                    if (d > 3.4 || (dx == 0 && dz == 0 && dy <= 5)) continue;
                    if (d > 2.9 && this.roll(tx + dx, tz + dz, dy, 3) == 0) continue;
                    this.set(level, box, tx + dx, f + dy, tz + dz, leaf);
                }
            }
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            this.set(level, box, tx + d.getStepX(), f + 1, tz + d.getStepZ(), stair(Blocks.SPRUCE_STAIRS, d.getOpposite(), false));
            Direction c = d.getClockWise();
            this.set(level, box, tx + d.getStepX() + c.getStepX(), f + 1, tz + d.getStepZ() + c.getStepZ(), slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        }
    }

    // ---------------------------------------------------------------- the cathedral

    void buildCathedral(WorldGenLevel level, BoundingBox box, int f) {
        BlockState wall = st(Blocks.STONE_BRICKS), mossy = st(Blocks.MOSSY_STONE_BRICKS);
        int wallTop = f + 14;
        int zN = C_Z - C_NH, zS = C_Z + C_NH;
        RandomSource r = RandomSource.create(this.seed * 7 + 3);
        // ---- the nave
        for (int x = C_TX1; x <= C_NX1; x++) {
            for (int z = zN; z <= zS; z++) {
                boolean side = z == zN || z == zS, end = x == C_TX1 || x == C_NX1;
                this.set(level, box, x, f, z, ((x + z) & 1) == 0 ? st(Blocks.POLISHED_ANDESITE) : st(Blocks.POLISHED_DIORITE));
                for (int y = f + 1; y <= wallTop + 14; y++) {
                    BlockState s = st(Blocks.AIR);
                    if ((side || end) && y <= wallTop) {
                        s = r.nextInt(9) == 0 ? mossy : wall;
                        int bay = Math.floorMod(x - C_TX1, 4);
                        // the paired lancets with their mullion, a tracery stone over them
                        if (side && x > C_TX1 && x < C_NX1 && bay != 0) {
                            if (y >= f + 4 && y <= f + 11 && bay != 2) s = glass(x + z, y);
                            if (y == f + 12 && bay == 2) s = st(Blocks.CHISELED_STONE_BRICKS);
                        }
                    }
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        // the vault: a pointed vault filling the whole roof space (no dark loft above it), plastered between stone
        // ribs: a rib springs from the walls every second block and meets the ridge rib along the middle
        for (int x = C_TX1; x <= C_NX1; x++) {
            boolean end = x == C_TX1 || x == C_NX1;
            boolean rib = Math.floorMod(x - C_TX1, 2) == 0;
            for (int z = zN; z <= zS; z++) {
                int j = Math.min(z - zN, zS - z);
                int top = wallTop + 2 + 2 * j;                 // just under the slates
                int low = j == 0 ? wallTop + 1 : wallTop + 2 * j - 2;
                if (end) low = wallTop + 1;
                for (int y = low; y <= top; y++) {
                    BlockState s = end ? wall : ModBlocks.SMOOTH_WHITE_LIMESTONE.get().defaultBlockState();
                    if (!end && j > 0 && y == low && (rib || z == C_Z)) s = wall;
                    this.set(level, box, x, y, z, s);
                }
                // the ribs hang a block below the vault
                if (!end && j > 0 && (rib || z == C_Z)) this.set(level, box, x, low - 1, z, wall);
            }
            if (Math.floorMod(x - C_TX1, 4) == 0 && !end) {
                // the piers along the walls carry the ribs down
                for (int y = f + 1; y <= wallTop - 2; y++) {
                    this.set(level, box, x, y, zN + 1, st(Blocks.STONE_BRICK_WALL));
                    this.set(level, box, x, y, zS - 1, st(Blocks.STONE_BRICK_WALL));
                }
            }
        }
        // the buttresses: stepped piers with pinnacles between the windows
        for (int x = C_TX1; x <= C_NX1; x += 4) {
            for (int s : new int[]{-1, 1}) {
                int z = s < 0 ? zN - 1 : zS + 1;
                Direction away = s < 0 ? Direction.SOUTH : Direction.NORTH;
                for (int y = f; y <= f + 11; y++) this.set(level, box, x, y, z, wall);
                for (int y = f; y <= f + 6; y++) this.set(level, box, x, y, z + s, wall);
                this.set(level, box, x, f + 12, z, stair(Blocks.STONE_BRICK_STAIRS, away, false));
                // a pinnacle on the outer step
                this.set(level, box, x, f + 7, z + s, st(Blocks.STONE_BRICK_WALL));
                this.set(level, box, x, f + 8, z + s, st(Blocks.STONE_BRICK_WALL));
                this.set(level, box, x, f + 9, z + s, st(Blocks.LIGHTNING_ROD));
            }
        }
        // the apse: a half-round end, three tall windows, a half cone of slate
        for (int dx = 1; dx <= C_NH + 1; dx++) {
            for (int z = zN; z <= zS; z++) {
                int dz = z - C_Z;
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > C_NH + 0.5) continue;
                int x = C_NX1 + dx;
                boolean rim = d > C_NH - 0.6;
                this.set(level, box, x, f, z, st(Blocks.POLISHED_ANDESITE));
                for (int y = f + 1; y <= wallTop - 1; y++) {
                    BlockState s = rim ? wall : st(Blocks.AIR);
                    if (rim && (dz == 0 || Math.abs(dz) == 3) && y >= f + 4 && y <= f + 11) s = glass(dz, y);
                    this.set(level, box, x, y, z, s);
                }
                int ry = wallTop + (int) Math.round((C_NH + 0.5 - d) * 1.5);
                for (int y = wallTop; y <= ry; y++) this.set(level, box, x, y, z, st(Blocks.DEEPSLATE_TILES));
            }
        }
        for (int z = zN + 2; z <= zS - 2; z++) {
            for (int y = f + 1; y <= wallTop - 3; y++) this.set(level, box, C_NX1, y, z, st(Blocks.AIR));
        }
        // the steep slate roof, two up for every one in
        for (int k = 0; k <= C_NH + 1; k++) {
            int zl = zN - 1 + k, zr = zS + 1 - k;
            for (int x = C_TX1; x <= C_NX1; x++) {
                for (int h = 0; h < 2; h++) {
                    int y = wallTop + 1 + 2 * k + h;
                    if (zl < zr) {
                        BlockState lb = h == 1 ? stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, false) : st(Blocks.DEEPSLATE_TILES);
                        BlockState rb = h == 1 ? stair(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, false) : st(Blocks.DEEPSLATE_TILES);
                        this.set(level, box, x, y, zl, lb);
                        this.set(level, box, x, y, zr, rb);
                        // the east gable of stone
                        if (x == C_NX1) for (int z = zl + 1; z < zr; z++) this.set(level, box, x, y, z, wall);
                    } else if (zl == zr && h == 0) {
                        this.set(level, box, x, y, zl, st(Blocks.DEEPSLATE_TILES));
                        this.set(level, box, x, y + 1, zl, (x & 1) == 0 ? st(Blocks.DEEPSLATE_TILE_WALL) : slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM));
                    }
                }
            }
        }
        // ---- the south portal onto the market, under a little gable
        int px = C_TX1 + 10;
        for (int dx = -1; dx <= 1; dx++) {
            for (int y = f + 1; y <= f + 9; y++) this.set(level, box, px + dx, y, zS, y <= f + 4 ? wall : glass(px + dx, y));
        }
        for (int dx : new int[]{-1, 1}) {
            BlockState door = st(Blocks.DARK_OAK_DOOR).setValue(DoorBlock.FACING, Direction.SOUTH)
                    .setValue(DoorBlock.HINGE, dx < 0 ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT);
            this.set(level, box, px + dx, f + 1, zS, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(level, box, px + dx, f + 2, zS, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            this.set(level, box, px + dx, f + 3, zS, stair(Blocks.STONE_BRICK_STAIRS, dx < 0 ? Direction.WEST : Direction.EAST, true));
        }
        this.set(level, box, px, f + 1, zS, st(Blocks.CHISELED_STONE_BRICKS));
        this.set(level, box, px, f + 2, zS, st(Blocks.STONE_BRICK_WALL));
        this.set(level, box, px, f + 3, zS, st(Blocks.CHISELED_STONE_BRICKS));
        // the porch between the two buttresses, under its little gable (Wimperg)
        for (int dz = 1; dz <= 2; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                this.set(level, box, px + dx, f, zS + dz, st(Blocks.STONE_BRICKS));
                for (int y = f + 1; y <= f + 3; y++) this.set(level, box, px + dx, y, zS + dz, st(Blocks.AIR));
                this.set(level, box, px + dx, f + 4, zS + dz, wall);
            }
            this.set(level, box, px - 1, f + 5, zS + dz, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
            this.set(level, box, px + 1, f + 5, zS + dz, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
            this.set(level, box, px, f + 5, zS + dz, wall);
            this.set(level, box, px, f + 6, zS + dz, slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
        }
        this.set(level, box, px, f + 7, zS + 2, st(Blocks.STONE_BRICK_WALL));
        this.set(level, box, px, f + 3, zS + 1, hanging());
        // ---- inside: the red carpet, pews to either side facing the altar
        for (int x = C_TX1 + 1; x < C_NX1; x++) this.set(level, box, x, f + 1, C_Z, st(Blocks.RED_CARPET));
        for (int z = C_Z + 1; z < zS; z++) this.set(level, box, px, f + 1, z, st(Blocks.RED_CARPET));
        for (int x = C_TX1 + 4; x <= C_NX1 - 5; x += 2) {
            if (Math.abs(x - px) <= 1) continue;
            for (int z = C_Z - 3; z <= C_Z + 3; z++) {
                if (Math.abs(z - C_Z) < 2) continue;
                this.set(level, box, x, f + 1, z, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
            }
        }
        // the choir stalls, facing each other across the choir
        for (int x = C_NX1 - 3; x <= C_NX1 - 1; x++) {
            this.set(level, box, x, f + 1, zN + 2, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
            this.set(level, box, x, f + 1, zS - 2, stair(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, false));
            this.set(level, box, x, f + 2, zN + 1, st(Blocks.DARK_OAK_TRAPDOOR).setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.OPEN, true));
        }
        // the altar under the crucifix in the apse
        int ax = C_NX1 + 2;
        for (int z = C_Z - 1; z <= C_Z + 1; z++) {
            this.set(level, box, ax, f + 1, z, st(Blocks.QUARTZ_BLOCK));
            this.set(level, box, ax, f + 2, z, st(Blocks.WHITE_CARPET));
        }
        BlockState candles = st(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true);
        this.set(level, box, ax, f + 3, C_Z - 1, candles);
        this.set(level, box, ax, f + 3, C_Z + 1, candles);
        this.set(level, box, ax, f + 3, C_Z, st(Blocks.GOLD_BLOCK));
        int crx = C_NX1 + 4;
        this.set(level, box, crx, f + 1, C_Z, st(Blocks.CHISELED_STONE_BRICKS));
        for (int y = f + 2; y <= f + 7; y++) this.set(level, box, crx, y, C_Z, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y));
        this.set(level, box, crx, f + 6, C_Z - 1, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z));
        this.set(level, box, crx, f + 6, C_Z + 1, log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Z));
        this.chest(level, box, C_NX1 + 1, f + 1, C_Z - 3, Direction.SOUTH, TREASURY_LOOT);
        this.chest(level, box, C_NX1 + 1, f + 1, C_Z + 3, Direction.NORTH, HOUSE_LOOT);
        // the pulpit (Kanzel) on the north wall, its sounding board above
        int kx = C_TX1 + 13;
        for (int y = f + 1; y <= f + 3; y++) this.set(level, box, kx, y, zN + 2, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = 1; dz <= 2; dz++) this.set(level, box, kx + dx, f + 4, zN + dz, st(Blocks.DARK_OAK_PLANKS));
        }
        this.set(level, box, kx - 1, f + 5, zN + 3, st(Blocks.DARK_OAK_FENCE));
        this.set(level, box, kx, f + 5, zN + 3, st(Blocks.DARK_OAK_FENCE));
        this.set(level, box, kx + 1, f + 5, zN + 2, st(Blocks.DARK_OAK_FENCE));
        for (int y = f + 1; y <= f + 4; y++) this.set(level, box, kx + 1, y, zN + 1, st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 1; dz <= 3; dz++) this.set(level, box, kx + dx, f + 8, zN + dz, slab(Blocks.DARK_OAK_SLAB, SlabType.TOP));
        }
        // the font by the south door
        this.set(level, box, px - 3, f + 1, zS - 2, st(Blocks.STONE_BRICK_WALL));
        this.set(level, box, px - 3, f + 2, zS - 2, st(Blocks.CHISELED_STONE_BRICKS));
        this.set(level, box, px - 3, f + 3, zS - 2, st(Blocks.CANDLE).setValue(CandleBlock.LIT, true));
        // the organ gallery at the west end, its pipes rising against the tower wall
        for (int x = C_TX1 + 1; x <= C_TX1 + 3; x++) {
            for (int z = zN + 1; z < zS; z++) this.set(level, box, x, f + 6, z, st(Blocks.DARK_OAK_PLANKS));
        }
        for (int z = zN + 1; z < zS; z++) this.set(level, box, C_TX1 + 3, f + 7, z, st(Blocks.DARK_OAK_FENCE));
        for (int z = zN + 2; z <= zS - 2; z++) {
            int h = 6 - Math.abs(z - C_Z);
            this.set(level, box, C_TX1 + 1, f + 7, z, st(Blocks.DARK_OAK_PLANKS));
            for (int y = f + 8; y <= f + 8 + h; y++) this.set(level, box, C_TX1 + 1, y, z, st(Blocks.IRON_BARS));
        }
        for (int y = f + 1; y <= f + 6; y++) this.set(level, box, C_TX1 + 2, y, zN + 1, st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
        // chandeliers on long chains from the ridge rib, and lanterns along the aisles
        for (int x = C_TX1 + 2; x < C_NX1; x += 4) {
            for (int y = wallTop - 2; y <= wallTop + 6; y++) this.set(level, box, x, y, C_Z, st(Blocks.CHAIN));
            this.set(level, box, x, wallTop - 3, C_Z, hanging());
            for (int z : new int[]{zN + 2, zS - 2}) {
                this.set(level, box, x, wallTop + 1, z, st(Blocks.CHAIN));
                this.set(level, box, x, wallTop, z, hanging());
            }
        }
        // ---- the west tower on the main street
        int towerTop = f + 28, tz0 = C_Z - 3, tz1 = C_Z + 3, tx0 = C_X0, tx1 = C_TX1;
        for (int x = tx0; x <= tx1; x++) {
            for (int z = tz0; z <= tz1; z++) {
                boolean rim = x == tx0 || x == tx1 || z == tz0 || z == tz1;
                this.set(level, box, x, f, z, st(Blocks.POLISHED_ANDESITE));
                for (int y = f + 1; y <= towerTop; y++) {
                    BlockState s = rim ? (r.nextInt(9) == 0 ? mossy : wall) : st(Blocks.AIR);
                    // the belfry's pointed openings
                    boolean mid = (x == tx0 || x == tx1) ? Math.abs(z - C_Z) <= 1 : Math.abs(x - (tx0 + tx1) / 2) <= 1;
                    if (rim && mid && y >= towerTop - 5 && y <= towerTop - 2) s = st(Blocks.AIR);
                    if (rim && mid && y == towerTop - 2 && !((x == tx0 || x == tx1) ? z == C_Z : x == (tx0 + tx1) / 2)) s = wall;
                    // slit windows up the stair
                    if (rim && mid && (y == f + 15 || y == f + 16) && ((x == tx0 || x == tx1) ? z == C_Z : x == (tx0 + tx1) / 2)) s = st(Blocks.AIR);
                    this.set(level, box, x, y, z, s);
                }
                if (!rim) {
                    for (int y : new int[]{f + 12, f + 18, towerTop - 6}) this.set(level, box, x, y, z, st(Blocks.SPRUCE_PLANKS));
                }
            }
        }
        // the corner buttresses of the tower
        for (int[] c : new int[][]{{tx0, tz0}, {tx0, tz1}}) {
            int sz = c[1] == tz0 ? -1 : 1;
            for (int y = f; y <= f + 16; y++) {
                this.set(level, box, c[0] - 1, y, c[1], wall);
                this.set(level, box, c[0], y, c[1] + sz, wall);
            }
            this.set(level, box, c[0] - 1, f + 17, c[1], stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, false));
            this.set(level, box, c[0], f + 17, c[1] + sz, stair(Blocks.STONE_BRICK_STAIRS, sz < 0 ? Direction.SOUTH : Direction.NORTH, false));
        }
        // the tower opens into the nave under the gallery
        for (int z = tz0 + 1; z <= tz1 - 1; z++) {
            for (int y = f + 1; y <= f + 5; y++) this.set(level, box, tx1, y, z, st(Blocks.AIR));
        }
        // the west portal: two doors either side of a pillar, a pointed arch, the rose window above
        for (int dz : new int[]{-1, 1}) {
            BlockState door = st(Blocks.DARK_OAK_DOOR).setValue(DoorBlock.FACING, Direction.WEST)
                    .setValue(DoorBlock.HINGE, dz < 0 ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT);
            this.set(level, box, tx0, f + 1, C_Z + dz, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(level, box, tx0, f + 2, C_Z + dz, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            this.set(level, box, tx0, f + 3, C_Z + dz, stair(Blocks.STONE_BRICK_STAIRS, dz < 0 ? Direction.NORTH : Direction.SOUTH, true));
        }
        this.set(level, box, tx0, f + 1, C_Z, st(Blocks.CHISELED_STONE_BRICKS));
        this.set(level, box, tx0, f + 2, C_Z, st(Blocks.STONE_BRICK_WALL));
        this.set(level, box, tx0, f + 4, C_Z, st(Blocks.CHISELED_STONE_BRICKS));
        int rose = f + 9;
        for (int dz = -2; dz <= 2; dz++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dz * dz + dy * dy > 5) continue;
                Block g = (dz == 0 && dy == 0) ? Blocks.YELLOW_STAINED_GLASS
                        : (dz == 0 || dy == 0) ? Blocks.RED_STAINED_GLASS : Blocks.BLUE_STAINED_GLASS;
                this.set(level, box, tx0, rose + dy, C_Z + dz, st(g));
            }
        }
        // the ladder up the inside, the bell in the belfry
        for (int y = f + 1; y <= towerTop - 6; y++) {
            this.set(level, box, tx0 + 1, y, tz0 + 1, st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.SOUTH));
        }
        this.set(level, box, (tx0 + tx1) / 2, towerTop - 5, C_Z, st(Blocks.BELL));
        // a lantern on every floor of the tower
        for (int y : new int[]{f + 12, f + 18, towerTop - 6}) {
            this.set(level, box, tx1 - 1, y - 1, tz1 - 1, hanging());
            this.set(level, box, tx1 - 1, y + 1, tz1 - 1, st(Blocks.LANTERN));
        }
        // the clock: a face on the west and south sides under the belfry
        for (int[] face : new int[][]{{tx0 - 1, C_Z, 0}, {(tx0 + tx1) / 2, tz1 + 1, 1}}) {
            for (int a = -1; a <= 1; a++) {
                for (int dy = -1; dy <= 1; dy++) {
                    int x = face[2] == 0 ? face[0] : face[0] + a, z = face[2] == 0 ? face[1] + a : face[1];
                    this.set(level, box, x, f + 21 + dy, z, (a == 0 && dy == 0) ? st(Blocks.GOLD_BLOCK)
                            : (a == 0 || dy == 0) ? st(Blocks.BLACKSTONE) : st(Blocks.QUARTZ_BLOCK));
                }
            }
        }
        // the spire: pinnacles at the corners, a tall slate pyramid, a golden ball and cross on its tip
        int tcx = (tx0 + tx1) / 2;
        for (int[] c : new int[][]{{tx0, tz0}, {tx0, tz1}, {tx1, tz0}, {tx1, tz1}}) {
            this.set(level, box, c[0], towerTop + 1, c[1], st(Blocks.STONE_BRICK_WALL));
            this.set(level, box, c[0], towerTop + 2, c[1], st(Blocks.STONE_BRICK_WALL));
            this.set(level, box, c[0], towerTop + 3, c[1], st(Blocks.LIGHTNING_ROD));
        }
        int y = towerTop + 1;
        for (int half = 3; half >= 0; half--) {
            for (int s = 0; s < 3; s++, y++) {
                for (int dx = -half; dx <= half; dx++) {
                    for (int dz = -half; dz <= half; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != half && half > 0) continue;
                        if (half == 3 && Math.abs(dx) == 3 && Math.abs(dz) == 3) continue;
                        this.set(level, box, tcx + dx, y, C_Z + dz, st(Blocks.DEEPSLATE_TILES));
                    }
                }
            }
        }
        this.set(level, box, tcx, y, C_Z, st(Blocks.GOLD_BLOCK));
        this.set(level, box, tcx, y + 1, C_Z, st(Blocks.LIGHTNING_ROD));
    }

    /** The cathedral's coloured glass: a pattern of panes in red, blue, yellow and green. */
    static BlockState glass(int a, int y) {
        Block[] g = {Blocks.BLUE_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS_PANE,
                Blocks.BLUE_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS_PANE};
        return st(g[Math.floorMod(a * 3 + y, g.length)]);
    }

    void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, st(Blocks.CHEST).setValue(ChestBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
            chest.setLootTable(loot, this.seed ^ pos.asLong());
        }
    }

    // ---------------------------------------------------------------- the half-timbered houses

    void hset(WorldGenLevel level, BoundingBox box, House h, int u, int y, int v, BlockState state) {
        this.set(level, box, h.x(u, v), y, h.z(u, v), state);
    }

    static BlockState plaster(int look) {
        int k = look & 15;
        if (k < 11) return ModBlocks.SMOOTH_WHITE_LIMESTONE.get().defaultBlockState();
        if (k < 14) return st(Blocks.CALCITE);
        return st(Blocks.SMOOTH_SANDSTONE);
    }

    static Block timber(int look) {
        int k = look >> 4 & 7;
        return k < 5 ? Blocks.DARK_OAK_LOG : k < 7 ? Blocks.SPRUCE_LOG : Blocks.STRIPPED_DARK_OAK_LOG;
    }

    static Block shutterWood(int look) {
        int k = look >> 7 & 3;
        return k == 0 ? Blocks.MANGROVE_TRAPDOOR : k == 1 ? Blocks.DARK_OAK_TRAPDOOR : Blocks.SPRUCE_TRAPDOOR;
    }

    BlockState baseStone(House h, int x, int y, int z) {
        if (h.base == 2) return this.roll(x * 13 + y, z, 21, 6) == 0 ? st(Blocks.MOSSY_STONE_BRICKS) : st(Blocks.STONE_BRICKS);
        int k = this.roll(x * 13 + y, z, 22, 10);
        return k < 7 ? st(Blocks.COBBLESTONE) : k < 9 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.STONE);
    }

    void buildHouse(WorldGenLevel level, BoundingBox box, House h, int f, Block workstation) {
        int half = h.half, depth = h.depth;
        Direction front = h.front, right = h.right(), left = right.getOpposite(), back = front.getOpposite();
        Block tb = timber(h.look);
        BlockState post = log(tb, Direction.Axis.Y);
        BlockState beamAcross = log(tb, right.getAxis()), beamAlong = log(tb, front.getAxis());
        BlockState floorPlanks = st(Blocks.SPRUCE_PLANKS);
        int top = h.top(f);
        int jutTop = h.jut(h.storeys - 1);
        // ---- the storeys: the upper ones jut out over the street
        for (int s = 0; s < h.storeys; s++) {
            int y0 = f + 4 * s;
            int jut = -h.jut(s);
            for (int u = -half; u <= half; u++) {
                for (int v = jut; v <= depth - 1; v++) {
                    boolean onFront = v == jut, onBack = v == depth - 1, onSide = Math.abs(u) == half;
                    boolean wall = onFront || onBack || onSide;
                    BlockState fl = s == 0 ? (wall ? this.baseStone(h, h.x(u, v), y0, h.z(u, v)) : floorPlanks)
                            : (v < 0 ? (onSide ? beamAlong : v == jut ? beamAcross : floorPlanks)
                            : onFront || onBack ? beamAcross : onSide ? beamAlong : floorPlanks);
                    this.hset(level, box, h, u, y0, v, fl);
                    for (int dy = 1; dy <= 3; dy++) {
                        BlockState b = st(Blocks.AIR);
                        if (wall) b = this.wallBlock(h, s, u, dy, v, jut, post, beamAcross, beamAlong);
                        this.hset(level, box, h, u, y0 + dy, v, b);
                    }
                }
                // the carved brackets under the jetty
                if (s > 0 && h.jut(s) > h.jut(s - 1) && (Math.abs(u) == half || u == 0)) {
                    this.hset(level, box, h, u, y0 - 1, jut, stair(Blocks.DARK_OAK_STAIRS, front, true));
                }
            }
        }
        // the attic floor
        for (int u = -half; u <= half; u++) {
            for (int v = -jutTop; v <= depth - 1; v++) {
                boolean edge = v == -jutTop || v == depth - 1 || Math.abs(u) == half;
                this.hset(level, box, h, u, top, v, edge ? (v == -jutTop || v == depth - 1 ? beamAcross : beamAlong) : floorPlanks);
            }
        }
        // ---- the steep gabled roof, its gable to the street: slate or red tile
        Block roofStairs = h.slate ? Blocks.DEEPSLATE_TILE_STAIRS : Blocks.BRICK_STAIRS;
        BlockState roofSlab = h.slate ? slab(Blocks.DEEPSLATE_TILE_SLAB, SlabType.BOTTOM) : slab(Blocks.BRICK_SLAB, SlabType.BOTTOM);
        for (int k = 0; ; k++) {
            int ul = -half - 1 + k, ur = half + 1 - k;
            int y = top + 1 + k;
            if (ul > ur) break;
            for (int v = -jutTop - 1; v <= depth; v++) {
                if (ul == ur) {
                    this.hset(level, box, h, ul, y, v, roofSlab);
                    continue;
                }
                this.hset(level, box, h, ul, y, v, stair(roofStairs, right, false));
                this.hset(level, box, h, ur, y, v, stair(roofStairs, left, false));
                if (v == -jutTop || v == depth - 1) {
                    for (int u = ul + 1; u < ur; u++) {
                        BlockState g = (u == 0 && k % 3 == 1) ? st(Blocks.GLASS_PANE) : ((u + k) % 3 == 0 ? post : plaster(h.look));
                        if (k == 0) g = beamAcross;
                        this.hset(level, box, h, u, y, v, g);
                    }
                    // the Rathaus's stepped gable
                    if (h.special && k % 2 == 0) {
                        this.hset(level, box, h, ul, y + 1, v, st(Blocks.STONE_BRICKS));
                        this.hset(level, box, h, ur, y + 1, v, st(Blocks.STONE_BRICKS));
                    }
                } else if (v > -jutTop && v < depth - 1) {
                    for (int u = ul + 1; u < ur; u++) this.hset(level, box, h, u, y, v, st(Blocks.AIR));
                }
            }
        }
        // ---- the fireplace against the back wall and its chimney up through the roof
        int ridge = top + half + 1;   // the chimney stands a block over the ridge
        BlockState brick = st(Blocks.BRICKS);
        for (int y = f + 1; y <= ridge + 2; y++) {
            boolean mouth = y <= f + 2;
            this.hset(level, box, h, 0, y, depth - 1, y == f + 1 ? st(Blocks.CAMPFIRE).setValue(CampfireBlock.FACING, front) : st(Blocks.AIR));
            this.hset(level, box, h, -1, y, depth - 1, brick);
            this.hset(level, box, h, 1, y, depth - 1, brick);
            this.hset(level, box, h, 0, y, depth, brick);
            if (y <= top) {
                this.hset(level, box, h, -1, y, depth - 2, brick);
                this.hset(level, box, h, 1, y, depth - 2, brick);
                if (!mouth) this.hset(level, box, h, 0, y, depth - 2, brick);
            } else {
                this.hset(level, box, h, 0, y, depth - 2, brick);
            }
        }
        this.hset(level, box, h, 0, f + 3, depth - 3, slab(Blocks.BRICK_SLAB, SlabType.TOP));
        this.hset(level, box, h, 0, f, depth - 2, brick);
        for (int y = ridge + 1; y <= ridge + 2; y++) this.hset(level, box, h, 0, y, depth - 1, st(Blocks.AIR));
        // ---- the front door, a lantern under the jetty, shutters and window boxes
        Block doorBlock = (h.look >> 9 & 1) == 0 ? Blocks.SPRUCE_DOOR : Blocks.DARK_OAK_DOOR;
        BlockState door = st(doorBlock).setValue(DoorBlock.FACING, front).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        if (!h.special) {
            this.hset(level, box, h, 0, f + 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.hset(level, box, h, 0, f + 2, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            this.hset(level, box, h, 0, f + 3, 0, beamAcross);
            if (h.jut(1) > 0 && h.storeys > 1) {
                this.hset(level, box, h, 1, f + 3, -1, hanging());
            } else {
                this.hset(level, box, h, 1, f + 1, -1, st(Blocks.BARREL));
                this.hset(level, box, h, 1, f + 2, -1, st(Blocks.LANTERN));
            }
            if ((h.look >> 10 & 1) == 0) this.hset(level, box, h, -1, f + 1, -1, st(Blocks.BARREL));
            else this.hset(level, box, h, -1, f + 1, -1, st(Blocks.POTTED_RED_TULIP));
        }
        Block shutter = shutterWood(h.look);
        for (int s = 0; s < h.storeys; s++) {
            if (s == 0 && h.base != 0) continue;
            int y0 = f + 4 * s, jut = -h.jut(s);
            for (int u = -half + 1; u <= half - 1; u++) {
                BlockState w = this.wallBlock(h, s, u, 2, jut, jut, post, beamAcross, beamAlong);
                if (!w.is(Blocks.GLASS_PANE)) continue;
                for (int side : new int[]{-1, 1}) {
                    int su = u + side;
                    BlockState next = this.wallBlock(h, s, su, 2, jut, jut, post, beamAcross, beamAlong);
                    if (next.is(Blocks.GLASS_PANE) || Math.abs(su) > half) continue;
                    // an open shutter folded back against the post beside the window
                    this.hset(level, box, h, su, y0 + 2, jut - 1, st(shutter).setValue(TrapDoorBlock.FACING, front)
                            .setValue(TrapDoorBlock.OPEN, true));
                }
                if (s > 0 && (h.look >> 11 & 1) == 0) {
                    this.hset(level, box, h, u, y0 + 1, jut - 1, st(Blocks.FLOWERING_AZALEA_LEAVES).setValue(LeavesBlock.PERSISTENT, true));
                }
            }
        }
        if (h.special) {
            this.hset(level, box, h, 0, top + half + 1, depth / 2 - 2, hanging());
            this.hset(level, box, h, 0, top + 1, 3, st(Blocks.LANTERN));
            return;
        }
        // ---- inside: the workshop downstairs, the bedrooms above, a ladder up through the floors
        BlockState ws = workstation.defaultBlockState();
        if (ws.hasProperty(HorizontalDirectionalBlock.FACING)) ws = ws.setValue(HorizontalDirectionalBlock.FACING, front);
        this.hset(level, box, h, -half + 1, f + 1, 1, ws);
        this.hset(level, box, h, half - 1, f + 1, 1, st(Blocks.CRAFTING_TABLE));
        int lu = half - 1, lv = half == 2 ? depth - 3 : depth - 2;
        this.chestH(level, box, h, -half + 1, f + 1, depth - 3, right, HOUSE_LOOT);
        if (half >= 3 && depth >= 7) {
            // a table and two stools by the fire
            this.hset(level, box, h, -1, f + 1, depth - 4, st(Blocks.SPRUCE_FENCE));
            this.hset(level, box, h, -1, f + 2, depth - 4, st(Blocks.SPRUCE_PRESSURE_PLATE));
            this.hset(level, box, h, -2, f + 1, depth - 4, stair(Blocks.SPRUCE_STAIRS, right, false));
            this.hset(level, box, h, 0, f + 1, depth - 4, stair(Blocks.SPRUCE_STAIRS, left, false));
        }
        this.hset(level, box, h, 0, f + 3, 2, hanging());
        for (int s = 1; s < h.storeys; s++) {
            int y0 = f + 4 * s;
            BlockState bed = ((h.index + s) % 3 == 0 ? Blocks.BLUE_BED : (h.index + s) % 3 == 1 ? Blocks.RED_BED : Blocks.GREEN_BED)
                    .defaultBlockState().setValue(BedBlock.FACING, back);
            this.hset(level, box, h, -half + 1, y0 + 1, depth - 3, bed.setValue(BedBlock.PART, BedPart.HEAD));
            this.hset(level, box, h, -half + 1, y0 + 1, depth - 4, bed.setValue(BedBlock.PART, BedPart.FOOT));
            int vin = 1 - h.jut(s);
            // in a shallow house the bed reaches the front room: the chest goes to the other side
            int cu = depth - 4 <= vin ? half - 1 : -half + 1;
            this.hset(level, box, h, 0, y0 + 3, 1, hanging());
            if (s == h.storeys - 1) this.chestH(level, box, h, cu, y0 + 1, vin, back, HOUSE_LOOT);
            else this.hset(level, box, h, cu, y0 + 1, vin, st(Blocks.BOOKSHELF));
            if (half >= 3 && cu < 0) this.hset(level, box, h, half - 1, y0 + 1, vin, st(Blocks.BARREL));
        }
        for (int y = f + 1; y <= top; y++) {
            this.hset(level, box, h, lu, y, lv, st(Blocks.LADDER).setValue(LadderBlock.FACING, front));
        }
        // the attic: a lantern from the ridge, hay and barrels under the roof
        this.hset(level, box, h, -half + 1, top + 1, 1, st(Blocks.HAY_BLOCK));
        this.hset(level, box, h, -half + 1, top + 1, 2, st(Blocks.BARREL));
        this.hset(level, box, h, 0, top + half + 1, Math.min(2, depth - 3), hanging());
    }

    void chestH(WorldGenLevel level, BoundingBox box, House h, int u, int y, int v, Direction facing, ResourceKey<LootTable> loot) {
        this.chest(level, box, h.x(u, v), y, h.z(u, v), facing, loot);
    }

    /**
     * The block in a house's wall at (u, dy above the storey's floor, v): corner posts and posts every few blocks,
     * a rail under the windows, braces running diagonally across the panels, plaster between, windows in the
     * middle of the panels; the ground floor of many houses is cobblestone or stone instead.
     */
    BlockState wallBlock(House h, int s, int u, int dy, int v, int jut, BlockState post,
                         BlockState beamAcross, BlockState beamAlong) {
        int half = h.half, depth = h.depth;
        boolean onFront = v == jut, onBack = v == depth - 1, onSide = Math.abs(u) == half;
        int a = onFront || onBack ? u + half : v - jut;
        int len = onFront || onBack ? 2 * half : depth - 1 - jut;
        boolean corner = (onFront || onBack) && onSide;
        if (s == 0 && h.base != 0) {
            boolean window = (a % 3 == 1 || a % 3 == 2) && dy == 2 && !corner && !(onFront && Math.abs(u) <= 1)
                    && !(onBack && Math.abs(u) <= 1) && a != len / 2;
            return window ? st(Blocks.GLASS_PANE) : this.baseStone(h, h.x(u, v), dy, h.z(u, v));
        }
        if (corner) return post;
        boolean isPost = a % 3 == 0;
        if (isPost) return post;
        int panel = a / 3;
        int inPanel = a % 3;
        boolean windowPanel = (panel + s) % 2 == 0 && !(onFront && s == 0 && Math.abs(u) <= 1) && !(onBack && Math.abs(u) <= 1);
        // the side walls between terraced houses have no windows (the neighbour's wall is there)
        if (onSide && !onFront && !onBack) windowPanel = false;
        if (dy == 2 && windowPanel) return st(Blocks.GLASS_PANE);
        if (!windowPanel) {
            boolean down = (panel % 2 == 0);
            int braceRow = down ? (inPanel == 1 ? 3 : 1) : (inPanel == 1 ? 1 : 3);
            if (dy == braceRow) return onFront || onBack ? beamAcross : beamAlong;
        }
        if (dy == 1 && windowPanel) return onFront || onBack ? beamAcross : beamAlong;
        return plaster(h.look);
    }

    // ---------------------------------------------------------------- the Rathaus

    /**
     * The Rathaus (built as a grand house): an open arcade on the ground floor towards the square, the hall behind it;
     * the town library on the first floor (lecterns, shelves), the council chamber above; a ridge turret with a bell.
     */
    void furnishRathaus(WorldGenLevel level, BoundingBox box, int f) {
        int z0 = RH_Z0, z1 = RH_Z0 + RH_DEPTH - 1, xh = RH_HALF;
        // the arcade: pillars and pointed arches open to the square, three blocks deep
        for (int x = -xh + 1; x <= xh - 1; x++) {
            for (int z = z0; z <= z0 + 3; z++) {
                this.set(level, box, x, f, z, ((x + z) & 1) == 0 ? st(Blocks.POLISHED_ANDESITE) : st(Blocks.STONE_BRICKS));
                for (int y = f + 1; y <= f + 3; y++) this.set(level, box, x, y, z, st(Blocks.AIR));
            }
        }
        for (int x = -xh + 1; x <= xh - 1; x++) {
            boolean pillar = Math.floorMod(x + xh, 4) == 0;
            for (int y = f + 1; y <= f + 3; y++) {
                if (pillar) this.set(level, box, x, y, z0, st(Blocks.STONE_BRICKS));
            }
            if (!pillar) {
                int m = Math.floorMod(x + xh, 4);
                if (m == 1) this.set(level, box, x, f + 3, z0, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, true));
                if (m == 3) this.set(level, box, x, f + 3, z0, stair(Blocks.STONE_BRICK_STAIRS, Direction.EAST, true));
            }
        }
        for (int x = -xh + 1; x <= xh - 1; x += 4) this.set(level, box, x + 1, f + 3, z0 + 2, hanging());
        // the wall behind the arcade with the hall's door, a bench along it
        for (int x = -xh + 1; x <= xh - 1; x++) {
            for (int y = f + 1; y <= f + 3; y++) {
                BlockState s = st(Blocks.STONE_BRICKS);
                if (Math.abs(x) >= 3 && y == f + 2 && Math.floorMod(x, 3) != 0) s = st(Blocks.GLASS_PANE);
                this.set(level, box, x, y, z0 + 4, s);
            }
            if (Math.abs(x) >= 2) this.set(level, box, x, f + 1, z0 + 3, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
        }
        BlockState door = st(Blocks.DARK_OAK_DOOR).setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.set(level, box, 0, f + 1, z0 + 4, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.set(level, box, 0, f + 2, z0 + 4, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        this.set(level, box, 0, f + 3, z0 + 3, st(Blocks.AIR));
        // the town's banners on the front
        for (int x : new int[]{-xh + 2, xh - 2}) {
            this.set(level, box, x, f + 7, z0 - 1, st(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.NORTH));
        }
        // the hall: benches, a chest, lanterns
        for (int z = z0 + 6; z <= z1 - 2; z += 2) {
            this.set(level, box, -3, f + 1, z, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
            this.set(level, box, 3, f + 1, z, stair(Blocks.SPRUCE_STAIRS, Direction.SOUTH, false));
        }
        this.chest(level, box, -xh + 1, f + 1, z1 - 1, Direction.EAST, HOUSE_LOOT);
        this.set(level, box, 0, f + 3, z0 + 7, hanging());
        // the library on the first floor: shelves in rows, lecterns to read at
        int y1 = f + 4;
        for (int z : new int[]{z0 + 2, z0 + 5}) {
            for (int x = -xh + 2; x <= xh - 2; x++) {
                if (Math.abs(x) <= 1) continue;
                this.set(level, box, x, y1 + 1, z, st(Blocks.BOOKSHELF));
                this.set(level, box, x, y1 + 2, z, st(Blocks.BOOKSHELF));
            }
        }
        for (int x : new int[]{-3, 0, 3}) {
            this.set(level, box, x, y1 + 1, z0 + 8, st(Blocks.LECTERN).setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        }
        this.chest(level, box, -xh + 1, y1 + 1, z0 + 9, Direction.EAST, HOUSE_LOOT);
        this.set(level, box, 0, y1 + 3, z0 + 4, hanging());
        this.set(level, box, 0, y1 + 3, z0 + 9, hanging());
        // the council chamber above: the long table and its chairs
        int y2 = f + 8;
        for (int z = z0 + 3; z <= z0 + 8; z++) {
            this.set(level, box, 0, y2 + 1, z, slab(Blocks.DARK_OAK_SLAB, SlabType.TOP));
            this.set(level, box, -1, y2 + 1, z, stair(Blocks.OAK_STAIRS, Direction.WEST, false));
            this.set(level, box, 1, y2 + 1, z, stair(Blocks.OAK_STAIRS, Direction.EAST, false));
        }
        this.set(level, box, 0, y2 + 1, z0 + 2, stair(Blocks.DARK_OAK_STAIRS, Direction.NORTH, false));
        this.set(level, box, 0, y2 + 3, z0 + 5, hanging());
        for (int x = -xh + 1; x <= xh - 1; x += 2) {
            if (Math.abs(x) <= 1) continue;
            this.set(level, box, x, y2 + 2, z1 - 1, st(Blocks.RED_WALL_BANNER).setValue(WallBannerBlock.FACING, Direction.NORTH));
        }
        // the ladder up through the floors, in the back corner
        for (int y = f + 1; y <= f + 12; y++) {
            this.set(level, box, xh - 1, y, z1 - 2, st(Blocks.LADDER).setValue(LadderBlock.FACING, Direction.NORTH));
        }
        // the ridge turret (Dachreiter) with its bell, on the ridge over the middle of the house
        int ridgeY = f + 12 + xh + 2;
        int tzc = z0 + RH_DEPTH / 2;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                for (int y = ridgeY - 1; y <= ridgeY + 3; y++) {
                    BlockState s = corner ? log(Blocks.DARK_OAK_LOG, Direction.Axis.Y) : st(Blocks.AIR);
                    if (y == ridgeY - 1) s = st(Blocks.DARK_OAK_PLANKS);
                    this.set(level, box, dx, y, tzc + dz, s);
                }
                this.set(level, box, dx, ridgeY + 4, tzc + dz, st(Blocks.DEEPSLATE_TILES));
            }
        }
        this.set(level, box, 0, ridgeY, tzc, st(Blocks.BELL));
        this.set(level, box, 0, ridgeY + 5, tzc, st(Blocks.DEEPSLATE_TILES));
        this.set(level, box, 0, ridgeY + 6, tzc, st(Blocks.GOLD_BLOCK));
        this.set(level, box, 0, ridgeY + 7, tzc, st(Blocks.LIGHTNING_ROD));
    }

    // ---------------------------------------------------------------- the people

    void spawnLife(WorldGenLevel level, BoundingBox box, int f, Plan p) {
        RandomSource random = RandomSource.create(this.seed * 31 + 11);
        int n = 0;
        for (House h : p.houses) {
            if (h.special) continue;
            // someone in most houses (the rest fill up as the town breeds)
            if (n++ % 2 == 1) continue;
            this.spawnVillager(level, box, this.at(h.x(0, 2), f + 1, h.z(0, 2)));
        }
        this.spawnVillager(level, box, this.at(4, f + 1, 2));
        this.spawnVillager(level, box, this.at(-4, f + 1, -3));
        this.spawnVillager(level, box, this.at(2, f + 1, -6));
        this.spawnVillager(level, box, this.at(0, f + 1, RH_Z0 + 2));
        this.spawn(level, box, EntityType.IRON_GOLEM, this.at(SQ_X - 3, f + 1, SQ_Z - 3));
        this.spawn(level, box, EntityType.IRON_GOLEM, this.at(-SQ_X - 1, f + 1, -SQ_Z - 12));
        for (int i = 0; i < 6; i++) {
            this.spawn(level, box, EntityType.CAT, this.at(random.nextInt(2 * SQ_X - 4) - SQ_X + 2, f + 1, random.nextInt(2 * SQ_Z - 4) - SQ_Z + 2));
        }
        for (int i = 0; i < 4; i++) this.spawn(level, box, EntityType.CHICKEN, this.at(-SQ_X + 2 + i, f + 1, SQ_Z - 2));
    }

    void spawnVillager(WorldGenLevel level, BoundingBox box, BlockPos pos) {
        if (!box.isInside(pos)) return;
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) return;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillageRegistry.GERMAN.get()).setProfession(VillagerProfession.NONE));
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
