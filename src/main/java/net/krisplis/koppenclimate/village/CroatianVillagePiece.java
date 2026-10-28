package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.WinePoleBlock;
import net.krisplis.koppenclimate.worldgen.tree.ModTreeGrowers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A little walled island town off a Mediterranean beach, like Sveti Stefan or Trogir on the Dalmatian coast.
 *
 * <ul>
 *   <li>A lone rock rises out of the sea, some forty blocks across, ringed by a white limestone sea wall with
 *       battlements and rubble at its foot in the water. A walk runs all round inside the wall.</li>
 *   <li>On it stand at most sixteen white stone houses with red tiled roofs and shuttered windows, about a small
 *       square with a well, and a little chapel with a bell gable (preslica) on its front.</li>
 *   <li>Between the houses are gardens full of lavender, with Italian stone pines, cypresses and coconut palms, a small vineyard of red
 *       wine and a plot of tomatoes, both grown on poles in lines.</li>
 *   <li>On the side facing the shore is a small harbour: a quay at the foot of steps down from the town, and a
 *       breakwater sheltering a basin of still water. No boats are moored there.</li>
 *   <li>The townsfolk are the Venetian villagers, one to a house, with a few cats.</li>
 * </ul>
 * It is built like the Tuscan town (TuscanVillagePiece), sharing its houses, roofs and windows, but never with
 * yellow limestone.
 */
public class CroatianVillagePiece extends TuscanVillagePiece {
    private static final int BOX_R = 40;
    private static final int MAX_HOUSES = 16;
    // what else a cell may be
    private static final int WALL = 7, VINES = 8, TOMATO_PLOT = 9, SEA = 10;

    // the chapel (in harbour-facing coordinates: u toward the harbour, v across)
    private static final int CH_U0 = -11, CH_U1 = -4, CH_V0 = -2, CH_V1 = 2;
    private static final Block[] WORKSTATIONS = {
            Blocks.BARREL, Blocks.COMPOSTER, Blocks.BARREL, Blocks.COMPOSTER, Blocks.SMOKER, Blocks.LECTERN,
            Blocks.CARTOGRAPHY_TABLE, Blocks.LOOM, Blocks.CAULDRON, Blocks.STONECUTTER, Blocks.SMITHING_TABLE,
            Blocks.FLETCHING_TABLE, Blocks.GRINDSTONE, Blocks.BLAST_FURNACE};

    // the harbour's side of the island
    private final Direction harbour;
    private transient Layout island;

    public CroatianVillagePiece(BlockPos centre, long seed, Direction harbour) {
        super(VillageRegistry.CROATIAN_VILLAGE_PIECE.get(), centre, seed, new BoundingBox(
                centre.getX() - BOX_R, centre.getY() - 32, centre.getZ() - BOX_R,
                centre.getX() + BOX_R, centre.getY() + 34, centre.getZ() + BOX_R));
        this.harbour = harbour;
    }

    public CroatianVillagePiece(CompoundTag tag) {
        super(VillageRegistry.CROATIAN_VILLAGE_PIECE.get(), tag);
        this.harbour = Direction.from2DDataValue(tag.getInt("Harbour"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        super.addAdditionalSaveData(context, tag);
        tag.putInt("Harbour", this.harbour.get2DDataValue());
    }

    // ================================================================ geometry

    // from (u, v) - u out toward the harbour, v across it - to the town's x and z, and back
    private int toX(int u, int v) {
        return u * this.harbour.getStepX() - v * this.harbour.getStepZ();
    }

    private int toZ(int u, int v) {
        return u * this.harbour.getStepZ() + v * this.harbour.getStepX();
    }

    private int toU(int x, int z) {
        return x * this.harbour.getStepX() + z * this.harbour.getStepZ();
    }

    private int toV(int x, int z) {
        return -x * this.harbour.getStepZ() + z * this.harbour.getStepX();
    }

    /** The island's radius in the direction of (x, z): a rounded, lobed shore. */
    private double radius(double x, double z) {
        RandomSource r = RandomSource.create(this.seed ^ 0x2545F4914F6CDD1DL);
        double p1 = r.nextDouble() * Mth.TWO_PI, p2 = r.nextDouble() * Mth.TWO_PI;
        double a = Math.atan2(z, x);
        return 21.0 + 3.0 * Math.sin(2 * a + p1) + 2.0 * Math.sin(3 * a + p2);
    }

    /** Where along the harbour's axis the sea wall stands. */
    private int wallU() {
        return (int) Math.floor(this.radius(this.harbour.getStepX(), this.harbour.getStepZ()) - 1.5);
    }

    private enum Harbour { NONE, QUAY, STAIRS, BASIN, BREAKWATER }

    private Harbour harbourAt(int x, int z) {
        int u = this.toU(x, z), v = this.toV(x, z), w = this.wallU();
        if (u >= w - 3 && u <= w - 1 && v >= -1 && v <= 1) return Harbour.STAIRS;
        if (u >= w && u <= w + 2 && v >= -5 && v <= 5) return Harbour.QUAY;
        if (u >= w + 3 && u <= w + 10 && v >= 4 && v <= 5) return Harbour.BREAKWATER;
        if (u >= w + 9 && u <= w + 10 && v >= -1 && v <= 5) return Harbour.BREAKWATER;
        if (u >= w + 3 && u <= w + 10 && v >= -5 && v <= 3) return Harbour.BASIN;
        return Harbour.NONE;
    }

    // ================================================================ layout

    private Layout island() {
        if (this.island == null) this.island = this.makeIsland();
        return this.island;
    }

    private Layout makeIsland() {
        RandomSource random = RandomSource.create(this.seed);
        Layout l = new Layout();
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                double d = Mth.length(x, z), r = this.radius(x, z);
                int u = this.toU(x, z), v = this.toV(x, z);
                int c;
                if (d >= r) c = SEA;
                else if (d >= r - 1.5) c = WALL;
                else if (d >= r - 3.5) c = LANE;                                        // the walk inside the wall
                else if (Math.abs(u) <= 3 && Math.abs(v) <= 3) c = PIAZZA;
                else if (u >= CH_U0 && u <= CH_U1 && v >= CH_V0 && v <= CH_V1) c = CHURCH;
                else if ((v == -1 || v == 0) && u >= 4) c = LANE;                        // the street down to the harbour
                else if ((u == -1 || u == 0) && Math.abs(v) >= 4) c = LANE;              // the cross street
                else if (u >= -13 && u <= -5 && v >= 4 && v <= 12) c = VINES;
                else if (u >= -13 && u <= -5 && v >= -12 && v <= -4) c = TOMATO_PLOT;
                else c = FREE;
                if (this.harbourAt(x, z) == Harbour.STAIRS) c = LANE;
                l.put(x, z, c);
            }
        }
        // the houses, packed outward from the square, each on a street; at most sixteen
        List<int[]> cells = new ArrayList<>();
        for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++) if (l.at(x, z) == FREE) cells.add(new int[]{x, z});
        cells.sort(Comparator.comparingInt(c -> c[0] * c[0] + c[1] * c[1]));
        List<Block> uses = new ArrayList<>(List.of(WORKSTATIONS));
        BlockState[] walls = {
                st(ModBlocks.WHITE_LIMESTONE_BRICKS.get()), st(ModBlocks.SMOOTH_WHITE_LIMESTONE.get()),
                st(ModBlocks.COBBLED_WHITE_LIMESTONE.get()), st(ModBlocks.COBBLED_WHITE_LIMESTONE.get()),
                st(ModBlocks.MOSSY_COBBLED_WHITE_LIMESTONE.get())};
        for (int[] cell : cells) {
            if (l.houses.size() >= MAX_HOUSES) break;
            if (l.at(cell[0], cell[1]) != FREE) continue;
            for (int size = 7; size >= 5; size--) {
                int w = Math.min(size, 5 + random.nextInt(3)), d = Math.min(size, 5 + random.nextInt(3));
                int x0 = cell[0] >= 0 ? cell[0] : cell[0] - w + 1;
                int z0 = cell[1] >= 0 ? cell[1] : cell[1] - d + 1;
                if (!fits(l, x0, z0, w, d)) continue;
                for (int i = 0; i < w; i++) for (int k = 0; k < d; k++) l.put(x0 + i, z0 + k, HOUSE);
                House h = newHouse(l, random, x0, z0, w, d);
                if (h.door() == null) {
                    for (int i = 0; i < w; i++) for (int k = 0; k < d; k++) l.put(x0 + i, z0 + k, FREE);
                    continue;
                }
                Block work = uses.isEmpty() ? null : uses.remove(0);
                l.houses.add(new House(x0, z0, w, d, 2 + random.nextInt(2), walls[random.nextInt(walls.length)],
                        random.nextBoolean() ? st(ModBlocks.CUT_WHITE_LIMESTONE.get()) : st(ModBlocks.WHITE_LIMESTONE_BRICKS.get()),
                        random.nextBoolean() ? Blocks.OAK_TRAPDOOR : ModBlocks.ITALIAN_PINE_TRAPDOOR.get(),
                        h.door(), h.doorAt(), work, true));
                break;
            }
        }
        // what's left are gardens
        for (int x = -30; x <= 30; x++) for (int z = -30; z <= 30; z++) if (l.at(x, z) == FREE) l.put(x, z, GARDEN);
        return l;
    }

    /** Whether a house fits here, on free ground and with a street or the square along one of its sides. */
    private static boolean fits(Layout l, int x0, int z0, int w, int d) {
        boolean street = false;
        for (int i = -1; i <= w; i++) {
            for (int k = -1; k <= d; k++) {
                boolean inside = i >= 0 && i < w && k >= 0 && k < d;
                int c = l.at(x0 + i, z0 + k);
                if (inside && c != FREE) return false;
                boolean corner = (i == -1 || i == w) && (k == -1 || k == d);
                if (!inside && !corner && (c == LANE || c == PIAZZA)) street = true;
            }
        }
        return street;
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Layout l = this.island();
        int sea = this.centre.getY();
        int f = sea + 4;
        this.buildRock(level, box, l, sea, f);
        this.buildHarbour(level, box, sea, f);
        this.paveIsland(level, box, l, f);
        this.buildChapel(level, box, l, f);
        RandomSource houseRandom = RandomSource.create(this.seed ^ 0x5DEECE66DL);
        for (House h : l.houses) this.buildHouse(level, box, l, h, f, houseRandom);
        this.plantGardens(level, box, l, f, generator);
        this.populate(level, box, l, f);
    }

    /** The rock: solid from the seabed to the town's ground, ringed by the sea wall, rubble at its foot. */
    private void buildRock(WorldGenLevel level, BoundingBox box, Layout l, int sea, int f) {
        BlockState stone = st(Blocks.STONE), bricks = st(ModBlocks.WHITE_LIMESTONE_BRICKS.get());
        BlockState mossy = st(ModBlocks.MOSSY_WHITE_LIMESTONE_BRICKS.get());
        BlockState coping = ModBlocks.SMOOTH_WHITE_LIMESTONE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState[] rubble = {st(Blocks.STONE), st(Blocks.COBBLESTONE), st(Blocks.MOSSY_COBBLESTONE),
                st(ModBlocks.COBBLED_WHITE_LIMESTONE.get()), st(ModBlocks.MOSSY_COBBLED_WHITE_LIMESTONE.get())};
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int x = wx - this.centre.getX(), z = wz - this.centre.getZ();
                if (Math.abs(x) > BOX_R || Math.abs(z) > BOX_R) continue;
                double d = Mth.length(x, z), r = this.radius(x, z);
                if (d >= r + 4 || this.harbourAt(x, z) != Harbour.NONE) continue;
                int bed = sea - 30;
                for (int y = sea; y > sea - 30; y--) {
                    if (isGround(level.getBlockState(pos.set(wx, y, wz)))) {
                        bed = y;
                        break;
                    }
                }
                long hash = (x * 3129871L) ^ (z * 116129781L) ^ this.seed;
                int c = l.at(x, z);
                if (d >= r) {
                    // rubble and rocks at the foot of the wall, a few poking out of the water
                    int top = sea - 2 + (int) Math.round(3 * (1 - (d - r) / 4.0)) + (int) Math.floorMod(hash >> 7, 2L);
                    for (int y = bed + 1; y <= sea + 1; y++) {
                        BlockState s = y <= top ? rubble[(int) Math.floorMod(hash >> (y & 15), (long) rubble.length)]
                                : y <= sea ? st(Blocks.WATER) : Blocks.AIR.defaultBlockState();
                        level.setBlock(pos.set(wx, y, wz), s, 2);
                    }
                    continue;
                }
                for (int y = bed + 1; y < f; y++) level.setBlock(pos.set(wx, y, wz), stone, 2);
                if (c == WALL) {
                    // the sea wall: weathered by the waves at the bottom, with battlements on top
                    for (int y = Math.min(bed + 1, sea - 2); y <= f + 1; y++) {
                        level.setBlock(pos.set(wx, y, wz), y <= sea ? mossy : bricks, 2);
                    }
                    BlockState crenel = Math.floorMod(x + z, 2) == 0 ? bricks : coping;
                    if (Math.floorMod(x * 7 + z * 11, 9) == 0) crenel = st(Blocks.LANTERN);
                    level.setBlock(pos.set(wx, f + 2, wz), crenel, 2);
                }
                // clear the air above (but not a tree that's grown there from next door)
                for (int y = c == WALL ? f + 3 : f + 1; y <= f + 30; y++) {
                    BlockState s = level.getBlockState(pos.set(wx, y, wz));
                    if (!s.isAir() && !s.is(BlockTags.LEAVES) && !s.is(BlockTags.LOGS)) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    /** The harbour on the shore side: a quay, steps up into the town, and a breakwater round a basin. */
    private void buildHarbour(WorldGenLevel level, BoundingBox box, int sea, int f) {
        BlockState deck = st(ModBlocks.SMOOTH_WHITE_LIMESTONE.get()), edge = st(ModBlocks.CUT_WHITE_LIMESTONE.get());
        BlockState bricks = st(ModBlocks.WHITE_LIMESTONE_BRICKS.get()), stone = st(Blocks.STONE);
        BlockState post = st(ModBlocks.WHITE_LIMESTONE_BRICK_WALL.get());
        int w = this.wallU();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int u = w - 3; u <= w + 10; u++) {
            for (int v = -5; v <= 5; v++) {
                int x = this.toX(u, v), z = this.toZ(u, v);
                Harbour part = this.harbourAt(x, z);
                if (part == Harbour.NONE) continue;
                BlockPos column = this.at(x, 0, z);
                if (!box.isInside(column.atY(sea))) continue;
                int bed = sea - 30;
                for (int y = sea; y > sea - 30; y--) {
                    if (isGround(level.getBlockState(pos.set(column.getX(), y, column.getZ())))) {
                        bed = y;
                        break;
                    }
                }
                int top = switch (part) {
                    case STAIRS -> sea + 1 + (w - u);             // u = w-1, w-2, w-3: one step up each
                    case QUAY, BREAKWATER -> sea + 1;
                    default -> sea - 4;                             // the basin's floor
                };
                for (int y = Math.min(bed + 1, top); y <= f + 4; y++) {
                    BlockState s;
                    if (y < top) s = part == Harbour.BASIN ? stone : (y <= sea ? st(ModBlocks.MOSSY_WHITE_LIMESTONE_BRICKS.get()) : bricks);
                    else if (y == top) {
                        s = switch (part) {
                            case STAIRS -> ModBlocks.WHITE_LIMESTONE_STAIRS.get().defaultBlockState()
                                    .setValue(StairBlock.FACING, this.harbour.getOpposite());
                            case QUAY -> u == w + 2 ? edge : deck;
                            case BREAKWATER -> bricks;
                            default -> stone;
                        };
                    } else s = part == Harbour.BASIN && y <= sea ? st(Blocks.WATER) : Blocks.AIR.defaultBlockState();
                    level.setBlock(pos.set(column.getX(), y, column.getZ()), s, 2);
                }
                // bollards along the quay's edge, and lamps at its ends and on the breakwater's head
                if (part == Harbour.QUAY && u == w + 2 && (v == -4 || v == -1 || v == 2)) {
                    level.setBlock(pos.set(column.getX(), sea + 2, column.getZ()), post, 2);
                }
                boolean lamp = part == Harbour.QUAY && u == w + 2 && Math.abs(v) == 5
                        || part == Harbour.BREAKWATER && u == w + 10 && (v == -1 || v == 5);
                if (lamp) {
                    level.setBlock(pos.set(column.getX(), sea + 2, column.getZ()), post, 2);
                    level.setBlock(pos.set(column.getX(), sea + 3, column.getZ()), st(Blocks.LANTERN), 2);
                }
            }
        }
    }

    private void paveIsland(WorldGenLevel level, BoundingBox box, Layout l, int f) {
        BlockState smooth = st(ModBlocks.SMOOTH_WHITE_LIMESTONE.get()), bricks = st(ModBlocks.WHITE_LIMESTONE_BRICKS.get());
        BlockState cobble = st(ModBlocks.COBBLED_WHITE_LIMESTONE.get()), grass = st(ModBlocks.CINNAMON_GRASS_BLOCK.get());
        java.util.Set<Long> trees = new java.util.HashSet<>();
        for (int[] t : this.treeSpots(l)) trees.add(((long) t[0] << 32) ^ (t[1] & 0xFFFFFFFFL));
        for (int x = -30; x <= 30; x++) {
            for (int z = -30; z <= 30; z++) {
                int c = l.at(x, z);
                if (c == SEA || c == WALL || this.harbourAt(x, z) == Harbour.STAIRS) continue;
                long hash = (x * 31L + z * 17L) & 7;
                BlockState floor = switch (c) {
                    case PIAZZA -> (x + z & 1) == 0 ? smooth : bricks;
                    case LANE -> hash == 0 ? bricks : hash < 3 ? smooth : cobble;
                    case GARDEN -> grass;
                    case VINES, TOMATO_PLOT -> st(Blocks.COARSE_DIRT);
                    default -> cobble;
                };
                this.set(level, box, x, f, z, floor);
                // the gardens are full of lavender, all but where the trees stand
                if (c == GARDEN && !trees.contains(((long) x << 32) ^ (z & 0xFFFFFFFFL))
                        && Math.floorMod((x * 92821L + z * 68917L) ^ this.seed, 10L) < 7) {
                    this.set(level, box, x, f + 1, z, st(ModBlocks.LAVENDER.get()));
                }
                if (c == VINES || c == TOMATO_PLOT) {
                    // a line of poles, then a one-block path, and so on
                    int line = Math.floorMod(this.toU(x, z), 2);
                    if (line == 1) {
                        this.set(level, box, x, f, z, st(Blocks.DIRT_PATH));
                        continue;
                    }
                    long roll = (x * 341873128712L + z * 132897987541L) ^ this.seed;
                    BlockState pole = ModBlocks.WINE_POLE.get().defaultBlockState()
                            .setValue(WinePoleBlock.GRAPE, c == VINES ? WinePoleBlock.Grape.RED : WinePoleBlock.Grape.TOMATO);
                    this.set(level, box, x, f + 1, z, pole.setValue(WinePoleBlock.AGE, 1 + (int) Math.floorMod(roll >> 20, 3L)));
                    this.set(level, box, x, f + 2, z, pole.setValue(WinePoleBlock.AGE, 1 + (int) Math.floorMod(roll >> 24, 3L)));
                }
            }
        }
        // the square: a well in the middle, potted nerium and lamps at its corners
        BlockState rim = ModBlocks.CUT_WHITE_LIMESTONE_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int du = -1; du <= 1; du++) {
            for (int dv = -1; dv <= 1; dv++) {
                int x = this.toX(du, dv), z = this.toZ(du, dv);
                if (du == 0 && dv == 0) {
                    this.set(level, box, x, f, z, st(Blocks.WATER));
                    this.set(level, box, x, f - 1, z, st(Blocks.WATER));
                    this.set(level, box, x, f - 2, z, smooth);
                } else {
                    this.set(level, box, x, f + 1, z, rim);
                }
            }
        }
        for (int[] p : new int[][]{{3, 3}, {3, -3}, {-3, 3}, {-3, -3}}) {
            int x = this.toX(p[0], p[1]), z = this.toZ(p[0], p[1]);
            boolean lamp = p[0] > 0;
            if (lamp) {
                this.set(level, box, x, f + 1, z, st(ModBlocks.WHITE_LIMESTONE_BRICK_WALL.get()));
                this.set(level, box, x, f + 2, z, st(Blocks.LANTERN));
            } else {
                this.set(level, box, x, f + 1, z, st(ModBlocks.POTTED_NERIUM.get()));
            }
        }
    }

    /** The little chapel behind the square, its front toward it, with a bell gable above the door. */
    private void buildChapel(WorldGenLevel level, BoundingBox box, Layout l, int f) {
        BlockState wall = st(ModBlocks.WHITE_LIMESTONE_BRICKS.get()), smooth = st(ModBlocks.SMOOTH_WHITE_LIMESTONE.get());
        Direction front = this.harbour;
        int top = f + 7;
        int x0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z0 = Integer.MAX_VALUE, z1 = Integer.MIN_VALUE;
        for (int u = CH_U0; u <= CH_U1; u++) {
            for (int v = CH_V0; v <= CH_V1; v++) {
                int x = this.toX(u, v), z = this.toZ(u, v);
                x0 = Math.min(x0, x);
                x1 = Math.max(x1, x);
                z0 = Math.min(z0, z);
                z1 = Math.max(z1, z);
                boolean edge = u == CH_U0 || u == CH_U1 || v == CH_V0 || v == CH_V1;
                this.set(level, box, x, f, z, st(ModBlocks.POLISHED_MARBLE.get()));
                for (int y = f + 1; y < top; y++) {
                    this.set(level, box, x, y, z, edge ? (u == CH_U1 && (y - f) % 3 == 0 ? smooth : wall) : Blocks.AIR.defaultBlockState());
                }
            }
        }
        this.gableRoof(level, box, l, x0, z0, x1, z1, top, wall);
        // the door, a round window above it, and narrow windows down the sides
        this.door(level, box, this.toX(CH_U1, 0), f + 1, this.toZ(CH_U1, 0), front, ModBlocks.ITALIAN_PINE_DOOR.get());
        this.set(level, box, this.toX(CH_U1, 0), f + 5, this.toZ(CH_U1, 0), st(Blocks.ORANGE_STAINED_GLASS));
        for (int u = CH_U0 + 2; u < CH_U1; u += 3) {
            for (int v : new int[]{CH_V0, CH_V1}) {
                this.shutteredWindow(level, box, this.toX(u, v), f + 3, this.toZ(u, v),
                        v < 0 ? rotate(Direction.NORTH) : rotate(Direction.SOUTH), ModBlocks.ITALIAN_PINE_TRAPDOOR.get());
            }
        }
        // the bell gable: two little piers on the front's peak, a lintel, and the bell hanging between
        int peak = top + 1;   // the ridge of a five-wide roof
        for (int v = -1; v <= 1; v++) {
            int x = this.toX(CH_U1, v), z = this.toZ(CH_U1, v);
            if (v != 0) for (int y = peak; y <= peak + 3; y++) this.set(level, box, x, y, z, wall);
            this.set(level, box, x, peak + 4, z, smooth);
        }
        this.set(level, box, this.toX(CH_U1, 0), peak + 3, this.toZ(CH_U1, 0), Blocks.BELL.defaultBlockState()
                .setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, rotate(Direction.EAST)));
        // pews, the altar with candles, the priest's brewing stand, lanterns
        for (int u = CH_U1 - 2; u >= CH_U0 + 4; u -= 2) {
            for (int v : new int[]{-1, 1}) {
                this.set(level, box, this.toX(u, v), f + 1, this.toZ(u, v),
                        Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, front));
            }
        }
        for (int v = -1; v <= 1; v++) {
            this.set(level, box, this.toX(CH_U0 + 1, v), f + 1, this.toZ(CH_U0 + 1, v), st(ModBlocks.CHISELED_MARBLE.get()));
            this.set(level, box, this.toX(CH_U0 + 1, v), f + 2, this.toZ(CH_U0 + 1, v),
                    Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 2).setValue(CandleBlock.LIT, true));
        }
        this.set(level, box, this.toX(CH_U0 + 2, 1), f + 1, this.toZ(CH_U0 + 2, 1), st(Blocks.BREWING_STAND));
        this.set(level, box, this.toX(CH_U0 + 2, -1), f + 1, this.toZ(CH_U0 + 2, -1), st(Blocks.LANTERN));
        this.set(level, box, this.toX(CH_U1 - 1, 1), f + 1, this.toZ(CH_U1 - 1, 1), st(Blocks.LANTERN));
    }

    /** A direction given as if the harbour lay to the east, turned to where it really is. */
    private Direction rotate(Direction asIfEast) {
        int u = asIfEast.getStepX(), v = asIfEast.getStepZ();
        return Direction.fromDelta(this.toX(u, v), 0, this.toZ(u, v));
    }

    private static final List<ResourceKey<ConfiguredFeature<?, ?>>> TREES = List.of(
            ModTreeGrowers.ITALIAN_PINE_KEY, ModTreeGrowers.CYPRESS_KEY, ModTreeGrowers.COCONUT_PALM_KEY);

    /** Italian stone pines, cypresses and coconut palms in the gardens, spaced apart. */
    private void plantGardens(WorldGenLevel level, BoundingBox box, Layout l, int f, ChunkGenerator generator) {
        var registry = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        int n = 0;
        for (int[] spot : this.treeSpots(l)) {
            ResourceKey<ConfiguredFeature<?, ?>> tree = TREES.get(n++ % TREES.size());
            BlockPos pos = this.at(spot[0], f + 1, spot[1]);
            if (!box.isInside(pos)) continue;
            RandomSource random = RandomSource.create(this.seed ^ pos.asLong());
            registry.getHolder(tree).ifPresent(h -> h.value().place(level, generator, random, pos));
        }
    }

    /** Where the garden trees stand: in the open, spaced apart. */
    private List<int[]> treeSpots(Layout l) {
        List<int[]> planted = new ArrayList<>();
        for (int x = -28; x <= 28; x++) {
            for (int z = -28; z <= 28; z++) {
                if (!openGarden(l, x, z)) continue;
                boolean near = false;
                for (int[] p : planted) if (Math.abs(p[0] - x) <= 3 && Math.abs(p[1] - z) <= 3) near = true;
                if (near || Math.floorMod((x * 734287L + z * 912931L) ^ this.seed, 5L) != 0) continue;
                planted.add(new int[]{x, z});
            }
        }
        return planted;
    }

    /** A garden cell with gardens all round it, room for a tree. */
    private static boolean openGarden(Layout l, int x, int z) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (l.at(x + dx, z + dz) != GARDEN) return false;
        return true;
    }

    private void populate(WorldGenLevel level, BoundingBox box, Layout l, int f) {
        for (House h : l.houses) {
            this.spawnVillager(level, box, this.at((h.x0() + h.x1()) / 2, f + 1, (h.z0() + h.z1()) / 2));
        }
        // a few cats about the streets
        RandomSource random = RandomSource.create(this.seed * 17 + 3);
        int cats = 0;
        for (int tries = 0; tries < 200 && cats < 8; tries++) {
            int x = random.nextInt(51) - 25, z = random.nextInt(51) - 25;
            int c = l.at(x, z);
            if (c != LANE && c != PIAZZA && c != GARDEN) continue;
            if (Math.abs(this.toU(x, z)) <= 1 && Math.abs(this.toV(x, z)) <= 1) continue;
            cats++;
            this.spawn(level, box, EntityType.CAT, this.at(x, f + 1, z));
        }
    }
}
