package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.FruitLeavesBlock;
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
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
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
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A Japanese village of the Cherry orchards (Cwa), on ground levelled for it.
 *
 * <ul>
 *   <li>In the middle, a palace on the water: a pond full of salmon, with lily pads and a rim of mossy stones, and on
 *       an island in it a pavilion of three storeys - dark timber and paper walls below, paper between gilded posts and rails on top, each
 *       storey under its own skirt of dark tiles with upturned corners, a golden finial over all. Arched red bridges
 *       cross to it from the north and the south.</li>
 *   <li>A gravel walk round the pond; four avenues of gravel and stepping stones out from it, lined with stone
 *       lanterns, each ending at a red torii.</li>
 *   <li>Fifteen houses in four quarters: timber frames with walls of paper (shoji), tatami floors, a sliding paper
 *       door onto a veranda, hipped roofs of dark tiles or cedar shingles, and a little garden behind a bamboo fence,
 *       with a stone lantern and a flowering azalea.</li>
 *   <li>In the far north-western corner a little orchard round a summer cherry tree.</li>
 *   <li>Rice paddies beyond the northern and southern torii, carrot fields beyond the eastern and western.</li>
 *   <li>A Japanese villager in every house, one in the palace, two iron golems and a good many cats.</li>
 * </ul>
 */
public class JapaneseVillagePiece extends StructurePiece {
    static final int CORE = 50;
    static final int MARGIN = 10;
    static final int BOX = 88;
    static final int CLEAR = 40;
    static final int PLOT = 15;
    static final int[] PLOT_STARTS = {18, 34};
    // the pond (out to POND from the middle, its corners rounded) and the palace island in it (out to ISLAND)
    static final int POND = 15, POND_CORNER = 5, POND_DEPTH = 3, ISLAND = 7;
    // rice to the north and south, carrots to the east and west
    static final int[][] RICE_FIELDS = {{-12, -74, 12, -58}, {-12, 58, 12, 74}};
    static final int[][] CARROT_FIELDS = {{58, -12, 74, 12}, {-74, -12, -58, 12}};
    static final int APRON = 3;
    static final int AVENUE_END = CORE + 8;
    // the far north-western plot, left as a little orchard
    static final int ORCHARD_PLOT = 3;

    static final Block[] WORKSTATIONS = {
            Blocks.COMPOSTER, Blocks.LECTERN, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CARTOGRAPHY_TABLE,
            Blocks.BARREL, Blocks.FLETCHING_TABLE, Blocks.CAULDRON, Blocks.STONECUTTER, Blocks.LOOM, Blocks.SMITHING_TABLE,
            Blocks.GRINDSTONE, Blocks.COMPOSTER, Blocks.BARREL, Blocks.COMPOSTER};

    final BlockPos centre;
    final long seed;

    public JapaneseVillagePiece(BlockPos centre, long seed) {
        super(VillageRegistry.JAPANESE_VILLAGE_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX, centre.getY() - 30, centre.getZ() - BOX,
                centre.getX() + BOX, centre.getY() + CLEAR, centre.getZ() + BOX));
        this.centre = centre;
        this.seed = seed;
    }

    public JapaneseVillagePiece(CompoundTag tag) {
        super(VillageRegistry.JAPANESE_VILLAGE_PIECE.get(), tag);
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

    /** A house plot: its corner, and whether its front (the veranda and the gate) faces south (+z) or north. */
    record Plot(int x0, int z0, boolean frontSouth, int index) {
        int x(int px) {
            return this.x0 + px;
        }

        int z(int pz) {
            return this.frontSouth ? this.z0 + pz : this.z0 + PLOT - 1 - pz;
        }

        Direction dir(Direction local) {
            if (this.frontSouth || local.getAxis() != Direction.Axis.Z) return local;
            return local.getOpposite();
        }
    }

    static java.util.List<Plot> plots() {
        java.util.List<Plot> plots = new java.util.ArrayList<>();
        int i = 0;
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                for (int a : PLOT_STARTS) {
                    for (int b : PLOT_STARTS) {
                        int x0 = sx > 0 ? a : -a - PLOT + 1;
                        int z0 = sz > 0 ? b : -b - PLOT + 1;
                        // the front faces the middle of the village (north for the southern plots)
                        plots.add(new Plot(x0, z0, sz < 0, i++));
                    }
                }
            }
        }
        return plots;
    }

    /** Whether a spot is inside one of the house plots. */
    static boolean inPlot(int x, int z) {
        return inPlotRange(Math.abs(x)) && inPlotRange(Math.abs(z));
    }

    private static boolean inPlotRange(int a) {
        for (int s : PLOT_STARTS) {
            if (a >= s && a < s + PLOT) return true;
        }
        return false;
    }

    /** Whether a spot is in the pond (water or island). */
    static boolean inPond(int x, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        if (ax > POND || az > POND) return false;
        int inner = POND - POND_CORNER;
        if (ax > inner && az > inner) {
            int dx = ax - inner, dz = az - inner;
            return dx * dx + dz * dz <= POND_CORNER * POND_CORNER;
        }
        return true;
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int f = this.centre.getY();
        RandomSource r = RandomSource.create(this.seed ^ 0x2545F4914F6CDD1DL);
        this.shapeGround(level, box, f);
        // the trees the levelling cut round the edge: nothing of them left hanging in the air
        int cx = this.centre.getX(), cz = this.centre.getZ();
        LooseTreeCleanup.clean(level, chunkPos, f - 12, f + CLEAR + 6,
                (x, z) -> platformDistance(x - cx, z - cz) < MARGIN + 10,
                (x, z) -> platformDistance(x - cx, z - cz) == 0);
        this.pave(level, box, f);
        this.buildPond(level, box, f);
        this.buildPalace(level, box, f);
        for (int s : new int[]{-1, 1}) {
            this.buildBridge(level, box, f, s);
            this.buildTorii(level, box, f, 0, s * (CORE + 3), true);
            this.buildTorii(level, box, f, s * (CORE + 3), 0, false);
        }
        this.stoneLanterns(level, box, f);
        int home = 0;
        for (Plot p : plots()) {
            if (p.index() == ORCHARD_PLOT) continue;   // the far north-western plot is left as an orchard
            this.buildHouse(level, box, p, f, WORKSTATIONS[home++ % WORKSTATIONS.length], r);
        }
        this.orchardCorner(level, box, f);
        for (int[] field : RICE_FIELDS) this.buildPaddy(level, box, field, f);
        for (int[] field : CARROT_FIELDS) this.buildCarrots(level, box, field, f);
        this.spawnLife(level, box, f);
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

    static BlockState stair(Block block, Direction facing, boolean upsideDown) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
    }

    static final Block TILE_STAIRS = Blocks.DEEPSLATE_TILE_STAIRS;
    static final BlockState TILES = st(Blocks.DEEPSLATE_TILES);
    static final BlockState TILE_SLAB = st(Blocks.DEEPSLATE_TILE_SLAB);
    static final BlockState POST = st(Blocks.DARK_OAK_LOG);
    static final BlockState BEAM = st(Blocks.DARK_OAK_PLANKS);
    static final BlockState FOOTING = st(Blocks.COBBLESTONE);

    /** Vermilion lacquered wood: the torii and the bridges' rails (red-painted planks). */
    static BlockState red() {
        return ModBlocks.PAINTED_PLANKS.get(net.minecraft.world.item.DyeColor.RED).get().defaultBlockState();
    }

    static BlockState shoji() {
        return ModBlocks.SHOJI_SCREEN.get().defaultBlockState();
    }

    static BlockState tatami(Direction.Axis axis) {
        return ModBlocks.TATAMI.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    static BlockState paperLantern() {
        return ModBlocks.CHINESE_LANTERN.get().defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    // ---------------------------------------------------------------- the ground

    static double platformDistance(int x, int z) {
        double best = Mth.length(Math.max(0, Math.abs(x) - CORE), Math.max(0, Math.abs(z) - CORE));
        for (int[][] fields : new int[][][]{RICE_FIELDS, CARROT_FIELDS}) {
            for (int[] fl : fields) {
                int dx = Math.max(0, Math.max(fl[0] - APRON - x, x - fl[2] - APRON));
                int dz = Math.max(0, Math.max(fl[1] - APRON - z, z - fl[3] - APRON));
                best = Math.min(best, Mth.length(dx, dz));
            }
        }
        // the avenues run on out to the fields
        if (Math.abs(x) <= 2 && Math.abs(z) <= AVENUE_END || Math.abs(z) <= 2 && Math.abs(x) <= AVENUE_END) best = 0;
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
                    level.setBlock(pos.set(wx, y, wz), y < target - 3 ? st(Blocks.STONE) : dirt, 2);
                }
                level.setBlock(pos.set(wx, target, wz), grass, 2);
                if (level.getBlockState(pos.set(wx, target - 1, wz)).is(Blocks.GRASS_BLOCK)) level.setBlock(pos, dirt, 2);
            }
        }
    }

    // ---------------------------------------------------------------- paths

    void pave(WorldGenLevel level, BoundingBox box, int f) {
        int reach = AVENUE_END;
        for (int x = Math.max(-reach, box.minX() - this.centre.getX()); x <= Math.min(reach, box.maxX() - this.centre.getX()); x++) {
            for (int z = Math.max(-reach, box.minZ() - this.centre.getZ()); z <= Math.min(reach, box.maxZ() - this.centre.getZ()); z++) {
                if (inPond(x, z)) continue;
                long h = (x * 341873128712L + z * 132897987541L) ^ this.seed;
                int roll = (int) ((h >>> 24) & 0xFF);
                int ring = Math.max(Math.abs(x), Math.abs(z));
                boolean avenue = Math.abs(x) <= 1 || Math.abs(z) <= 1;
                if (ring >= POND + 1 && ring <= POND + 2 && !inPlot(x, z)) {
                    // the walk round the pond: raked gravel
                    this.set(level, box, x, f, z, st(Blocks.GRAVEL));
                } else if (avenue) {
                    // gravel with a line of stepping stones down the middle
                    boolean step = (Math.abs(x) == 0 || Math.abs(z) == 0) && ((x + z) & 1) == 0;
                    this.set(level, box, x, f, z, step ? st(Blocks.SMOOTH_STONE) : st(Blocks.GRAVEL));
                } else if (ring <= CORE && !inPlot(x, z) && (Math.abs(x) == PLOT_STARTS[1] - 1 || Math.abs(z) == PLOT_STARTS[1] - 1)) {
                    this.set(level, box, x, f, z, st(roll < 60 ? Blocks.COARSE_DIRT : Blocks.DIRT_PATH));
                }
            }
        }
    }

    void stoneLanterns(WorldGenLevel level, BoundingBox box, int f) {
        for (int a : new int[]{-46, -38, -26, 26, 38, 46}) {
            for (int side : new int[]{-2, 2}) {
                for (int[] p : new int[][]{{side, a}, {a, side}}) {
                    this.stoneLantern(level, box, p[0], p[1], f);
                }
            }
        }
    }

    /** A tōrō: a stone post, a lantern in it, a stone cap. */
    void stoneLantern(WorldGenLevel level, BoundingBox box, int x, int z, int f) {
        this.set(level, box, x, f + 1, z, st(Blocks.STONE_BRICK_WALL));
        this.set(level, box, x, f + 2, z, st(Blocks.LANTERN));
        this.set(level, box, x, f + 3, z, st(Blocks.STONE_BRICK_SLAB));
    }

    // ---------------------------------------------------------------- the torii

    void buildTorii(WorldGenLevel level, BoundingBox box, int f, int cx, int cz, boolean acrossX) {
        Direction plus = acrossX ? Direction.EAST : Direction.SOUTH;
        for (int o = -6; o <= 6; o++) {
            int x = acrossX ? cx + o : cx, z = acrossX ? cz : cz + o;
            if (Math.abs(o) == 3) {
                this.set(level, box, x, f, z, st(Blocks.STONE));
                for (int y = f + 1; y <= f + 5; y++) this.set(level, box, x, y, z, red());
            }
            if (Math.abs(o) <= 4) this.set(level, box, x, f + 4, z, red());           // the nuki
            if (Math.abs(o) <= 4) this.set(level, box, x, f + 6, z, red());           // the kasagi, red under black
            if (Math.abs(o) <= 5) this.set(level, box, x, f + 7, z, st(Blocks.POLISHED_BLACKSTONE));
            if (Math.abs(o) == 6) {
                this.set(level, box, x, f + 7, z, stair(Blocks.POLISHED_BLACKSTONE_STAIRS, o > 0 ? plus : plus.getOpposite(), false));
            }
        }
        // the gakuzuka, the little tablet between the beams
        this.set(level, box, cx, f + 5, cz, ModBlocks.PAINTED_PLANKS.get(net.minecraft.world.item.DyeColor.BLACK).get().defaultBlockState());
    }

    // ---------------------------------------------------------------- the pond, the bridges and the palace

    void buildPond(WorldGenLevel level, BoundingBox box, int f) {
        for (int x = -POND - 1; x <= POND + 1; x++) {
            for (int z = -POND - 1; z <= POND + 1; z++) {
                long h = (x * 734287L + z * 912931L) ^ this.seed;
                int roll = (int) ((h >>> 16) & 0xFF);
                boolean island = Math.abs(x) <= ISLAND && Math.abs(z) <= ISLAND;
                if (!inPond(x, z)) {
                    // its rim: mossy stones, with a boulder here and there
                    boolean rim = inPond(x + 1, z) || inPond(x - 1, z) || inPond(x, z + 1) || inPond(x, z - 1);
                    if (rim && !(Math.abs(x) <= 1 || Math.abs(z) <= 1)) {
                        this.set(level, box, x, f, z, st(roll < 90 ? Blocks.MOSSY_COBBLESTONE : roll < 170 ? Blocks.COBBLESTONE : Blocks.STONE));
                        if (roll < 18) this.set(level, box, x, f + 1, z, st(Blocks.MOSSY_COBBLESTONE));
                    }
                    continue;
                }
                if (island) {
                    for (int y = f - POND_DEPTH - 1; y <= f; y++) this.set(level, box, x, y, z, st(Blocks.STONE_BRICKS));
                    continue;
                }
                this.set(level, box, x, f - POND_DEPTH - 1, z, st(roll < 100 ? Blocks.SAND : roll < 200 ? Blocks.GRAVEL : Blocks.CLAY));
                for (int y = f - POND_DEPTH; y <= f; y++) this.set(level, box, x, y, z, st(Blocks.WATER));
                for (int y = f + 1; y <= f + 3; y++) this.set(level, box, x, y, z, st(Blocks.AIR));
                if (roll > 235 && Math.max(Math.abs(x), Math.abs(z)) > ISLAND + 2) this.set(level, box, x, f + 1, z, st(Blocks.LILY_PAD));
                else if (roll < 6) this.set(level, box, x, f - POND_DEPTH, z, st(Blocks.SEAGRASS));
            }
        }
    }

    /** An arched red bridge across the pond, from the shore (north or south) to the island. */
    void buildBridge(WorldGenLevel level, BoundingBox box, int f, int side) {
        int[] rise = {0, 1, 1, 2, 2, 2, 1, 1, 0};
        Direction toIsland = side > 0 ? Direction.NORTH : Direction.SOUTH;
        for (int i = 0; i < rise.length; i++) {
            int z = side * (POND + 1 - i);
            int y = f + rise[i];
            boolean up = i > 0 && rise[i] > rise[i - 1];
            boolean down = i < rise.length - 1 && rise[i] > rise[i + 1];
            for (int x = -1; x <= 1; x++) {
                BlockState deck = up ? stair(Blocks.MANGROVE_STAIRS, toIsland, false)
                        : down ? stair(Blocks.MANGROVE_STAIRS, toIsland.getOpposite(), false) : st(Blocks.MANGROVE_PLANKS);
                if (up || down) {
                    this.set(level, box, x, y - 1, z, st(Blocks.MANGROVE_PLANKS));
                }
                this.set(level, box, x, y, z, deck);
            }
            for (int x : new int[]{-2, 2}) {
                this.set(level, box, x, y, z, red());
                this.set(level, box, x, y + 1, z, st(Blocks.MANGROVE_FENCE));
            }
        }
    }

    /** A ring of tile stairs at Chebyshev distance r round the middle, each rising toward it. */
    void roofRing(WorldGenLevel level, BoundingBox box, int r, int y, Block stairs) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                Direction in;
                if (Math.abs(dx) >= Math.abs(dz)) in = dx > 0 ? Direction.WEST : Direction.EAST;
                else in = dz > 0 ? Direction.NORTH : Direction.SOUTH;
                this.set(level, box, dx, y, dz, stair(stairs, in, false));
            }
        }
    }

    void upturnedCorners(WorldGenLevel level, BoundingBox box, int r, int y, Block stairs) {
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                Direction out = sx > 0 ? Direction.EAST : Direction.WEST;
                this.set(level, box, sx * r, y + 1, sz * r, stair(stairs, out, false));
            }
        }
    }

    /** One storey of the palace: posts, walls of paper (or gold), a floor under it and a beam over it. */
    void storey(WorldGenLevel level, BoundingBox box, int half, int floorY, int height, boolean gilded) {
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                boolean edge = Math.max(Math.abs(dx), Math.abs(dz)) == half;
                this.set(level, box, dx, floorY, dz, edge ? BEAM : tatami((dx + dz & 2) == 0 ? Direction.Axis.X : Direction.Axis.Z));
                for (int y = floorY + 1; y < floorY + height; y++) {
                    BlockState s = st(Blocks.AIR);
                    if (edge) {
                        boolean post = Math.abs(dx) == half && Math.abs(dz) == half || (dx + dz) % 2 == 0 && (Math.abs(dx) == half || Math.abs(dz) == half);
                        if (gilded) {
                            s = post || y == floorY + 2 ? st(Blocks.GOLD_BLOCK) : shoji();
                        } else {
                            s = post ? POST : shoji();
                        }
                    }
                    this.set(level, box, dx, y, dz, s);
                }
                this.set(level, box, dx, floorY + height, dz, edge ? BEAM : st(Blocks.SPRUCE_PLANKS));
            }
        }
    }

    void buildPalace(WorldGenLevel level, BoundingBox box, int f) {
        // the veranda round it
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) == 6) this.set(level, box, dx, f, dz, st(Blocks.DARK_OAK_PLANKS));
            }
        }
        // the ground floor, the first floor, and the golden top floor
        this.storey(level, box, 5, f, 5, false);
        this.roofRing(level, box, 7, f + 4, TILE_STAIRS);
        this.roofRing(level, box, 6, f + 5, TILE_STAIRS);
        this.upturnedCorners(level, box, 7, f + 4, TILE_STAIRS);
        this.storey(level, box, 4, f + 5, 5, false);
        this.roofRing(level, box, 6, f + 9, TILE_STAIRS);
        this.roofRing(level, box, 5, f + 10, TILE_STAIRS);
        this.upturnedCorners(level, box, 6, f + 9, TILE_STAIRS);
        this.storey(level, box, 3, f + 10, 4, true);
        for (int r = 5; r >= 1; r--) this.roofRing(level, box, r, f + 14 + (5 - r), TILE_STAIRS);
        this.upturnedCorners(level, box, 5, f + 14, TILE_STAIRS);
        this.set(level, box, 0, f + 18, 0, TILES);
        this.set(level, box, 0, f + 19, 0, st(Blocks.GOLD_BLOCK));
        this.set(level, box, 0, f + 20, 0, st(Blocks.LIGHTNING_ROD));
        // doors on the north and south, to the bridges
        for (int side : new int[]{-1, 1}) {
            Direction out = side > 0 ? Direction.SOUTH : Direction.NORTH;
            BlockState door = ModBlocks.SHOJI_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, out).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
            this.set(level, box, 0, f + 1, side * 5, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.set(level, box, 0, f + 2, side * 5, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            this.set(level, box, 0, f + 3, side * 5, shoji());
        }
        // stairs up through the floors (a ladder by the west wall)
        for (int y = f + 1; y <= f + 10; y++) {
            this.set(level, box, -3, y, 0, st(Blocks.LADDER).setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        }
        this.set(level, box, -4, f + 5, 0, BEAM);
        this.set(level, box, -4, f + 10, 0, BEAM);
        for (int y = f + 1; y <= f + 10; y++) this.set(level, box, -4, y, 0, y == f + 5 || y == f + 10 ? BEAM : shoji());
        // the bell, the lord's lectern and bed, paper lanterns
        this.set(level, box, 2, f + 1, -2, st(Blocks.LECTERN).setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        BlockState bed = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH);
        this.set(level, box, 3, f + 6, -2, bed.setValue(BedBlock.PART, BedPart.HEAD));
        this.set(level, box, 3, f + 6, -1, bed.setValue(BedBlock.PART, BedPart.FOOT));
        this.set(level, box, 0, f + 4, 2, Blocks.BELL.defaultBlockState()
                .setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING).setValue(BellBlock.FACING, Direction.NORTH));
        for (int[] c : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) this.set(level, box, c[0], f + 4, c[1], paperLantern());
        for (int[] c : new int[][]{{6, 6}, {-6, 6}, {6, -6}, {-6, -6}}) this.stoneLantern(level, box, c[0], c[1], f);
    }

    // ---------------------------------------------------------------- the houses

    void plotSet(WorldGenLevel level, BoundingBox box, Plot p, int px, int y, int pz, BlockState state) {
        this.set(level, box, p.x(px), y, p.z(pz), state);
    }

    void buildHouse(WorldGenLevel level, BoundingBox box, Plot p, int f, Block workstation, RandomSource random) {
        Direction front = p.dir(Direction.SOUTH), back = front.getOpposite();
        boolean tiled = p.index() % 3 != 1;
        Block roofStairs = tiled ? TILE_STAIRS : Blocks.SPRUCE_STAIRS;
        BlockState roofSlab = tiled ? TILE_SLAB : st(Blocks.SPRUCE_SLAB);
        // the hall: px 2..12, pz 2..9, on a stone footing, its floor of tatami
        for (int px = 2; px <= 12; px++) {
            for (int pz = 2; pz <= 9; pz++) {
                boolean wall = px == 2 || px == 12 || pz == 2 || pz == 9;
                this.plotSet(level, box, p, px, f, pz, wall ? FOOTING : tatami((px & 2) == 0 ? Direction.Axis.X : Direction.Axis.Z));
                for (int y = f + 1; y <= f + 3; y++) {
                    BlockState s = st(Blocks.AIR);
                    if (wall) {
                        boolean corner = (px == 2 || px == 12) && (pz == 2 || pz == 9);
                        boolean post = corner || (pz == 2 || pz == 9) && (px == 5 || px == 9) || (px == 2 || px == 12) && pz == 6;
                        s = post ? POST : (px == 2 || px == 12) && y == f + 1 ? BEAM : shoji();
                    }
                    this.plotSet(level, box, p, px, y, pz, s);
                }
                this.plotSet(level, box, p, px, f + 4, pz, wall ? BEAM : st(Blocks.SPRUCE_PLANKS));
            }
        }
        // the sliding paper door onto the veranda
        BlockState door = ModBlocks.SHOJI_DOOR.get().defaultBlockState().setValue(DoorBlock.FACING, front).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.plotSet(level, box, p, 7, f + 1, 9, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.plotSet(level, box, p, 7, f + 2, 9, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // the veranda (engawa), its posts holding up the eaves
        for (int px = 2; px <= 12; px++) {
            this.plotSet(level, box, p, px, f, 10, st(Blocks.SPRUCE_PLANKS));
            this.plotSet(level, box, p, px, f + 4, 10, BEAM);
        }
        for (int px : new int[]{2, 12}) {
            for (int y = f + 1; y <= f + 3; y++) this.plotSet(level, box, p, px, y, 10, st(Blocks.DARK_OAK_FENCE));
        }
        // the hipped roof: rings of stairs climbing in from all four sides to a ridge
        for (int k = 0; ; k++) {
            int x0 = 1 + k, x1 = 13 - k, z0 = 1 + k, z1 = 11 - k;
            int y = f + 4 + k;
            if (z0 >= z1) {
                for (int px = x0; px <= x1; px++) this.plotSet(level, box, p, px, y, z0, roofSlab);
                this.plotSet(level, box, p, x0 - 1, y, z0, stair(roofStairs, Direction.WEST, false));
                this.plotSet(level, box, p, x1 + 1, y, z0, stair(roofStairs, Direction.EAST, false));
                break;
            }
            for (int px = x0; px <= x1; px++) {
                for (int pz = z0; pz <= z1; pz++) {
                    if (px != x0 && px != x1 && pz != z0 && pz != z1) continue;
                    Direction in;
                    if (pz == z0) in = front;
                    else if (pz == z1) in = back;
                    else in = px == x0 ? Direction.EAST : Direction.WEST;
                    this.plotSet(level, box, p, px, y, pz, stair(roofStairs, in, false));
                }
            }
            if (k == 0) {
                // the eaves' corners curl up
                for (int[] c : new int[][]{{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
                    this.plotSet(level, box, p, c[0], y + 1, c[1], stair(roofStairs, c[0] == x0 ? Direction.WEST : Direction.EAST, false));
                }
            }
        }
        // inside: a bed, the workstation, a paper lantern, a potted cherry
        BlockState bed = Blocks.WHITE_BED.defaultBlockState().setValue(BedBlock.FACING, back);
        this.plotSet(level, box, p, 4, f + 1, 4, bed.setValue(BedBlock.PART, BedPart.HEAD));
        this.plotSet(level, box, p, 4, f + 1, 5, bed.setValue(BedBlock.PART, BedPart.FOOT));
        BlockState ws = workstation.defaultBlockState();
        if (ws.hasProperty(HorizontalDirectionalBlock.FACING)) ws = ws.setValue(HorizontalDirectionalBlock.FACING, front);
        this.plotSet(level, box, p, 10, f + 1, 4, ws);
        this.plotSet(level, box, p, 10, f + 1, 8, st(Blocks.POTTED_CHERRY_SAPLING));
        this.plotSet(level, box, p, 7, f + 3, 6, paperLantern());
        this.plotSet(level, box, p, 7, f + 3, 10, paperLantern());
        // the garden: a bamboo fence with a gate, a gravel path, a stone lantern and an azalea
        for (int px = 0; px <= 14; px++) {
            for (int pz = 0; pz <= 14; pz++) {
                boolean fence = px == 0 || px == 14 || pz == 0 || pz == 14;
                if (fence && !(pz == 14 && px == 7)) this.plotSet(level, box, p, px, f + 1, pz, st(Blocks.BAMBOO_FENCE));
                if (pz >= 11 && pz <= 14 && px == 7) this.plotSet(level, box, p, px, f, pz, st(Blocks.GRAVEL));
            }
        }
        boolean treeLeft = random.nextBoolean();
        this.stoneLanternAt(level, box, p, treeLeft ? 11 : 3, 12, f);
        // and a flowering azalea (tsutsuji) bush
        this.set(level, box, p.x(treeLeft ? 3 : 11), f + 1, p.z(12), st(Blocks.FLOWERING_AZALEA));
    }

    void stoneLanternAt(WorldGenLevel level, BoundingBox box, Plot p, int px, int pz, int f) {
        this.stoneLantern(level, box, p.x(px), p.z(pz), f);
    }

    /** The far north-western plot: a little orchard round one summer cherry tree, ringed by stepping stones. */
    void orchardCorner(WorldGenLevel level, BoundingBox box, int f) {
        Plot p = plots().get(ORCHARD_PLOT);
        this.cherryTree(level, box, p.x(7), f + 1, p.z(7), this.seed * 31 + 7);
        for (int[] c : new int[][]{{7, 1}, {7, 13}, {1, 7}, {13, 7}}) this.plotSet(level, box, p, c[0], f, c[1], st(Blocks.SMOOTH_STONE));
    }

    /**
     * A summer cherry tree shaped like vanilla's cherry tree (its trunk and foliage placers): a trunk seven or eight
     * high, one to three branches leaning out two to four blocks and up, and on the trunk's top and each branch's end
     * a broad, flat crown five layers deep with ragged corners and a few hanging leaves - in green leaves hung with
     * cherries. Its shape comes from its own seed, so every chunk draws the same tree, each only its own part of it,
     * and its leaves only fill air.
     */
    void cherryTree(WorldGenLevel level, BoundingBox box, int x, int y, int z, long treeSeed) {
        RandomSource r = RandomSource.create(treeSeed);
        int height = 7 + r.nextInt(2);
        java.util.List<int[]> tops = new java.util.ArrayList<>();
        BlockState log = st(Blocks.CHERRY_LOG);
        for (int dy = 0; dy < height; dy++) this.set(level, box, x, y + dy, z, log);
        tops.add(new int[]{x, y + height, z});
        int branches = 1 + r.nextInt(3);
        Direction first = Direction.from2DDataValue(r.nextInt(4));
        for (int b = 0; b < branches; b++) {
            Direction d = b == 0 ? first : b == 1 ? first.getOpposite() : first.getClockWise();
            int bx = x, bz = z, by = y + height - 4 + r.nextInt(2);
            int length = 2 + r.nextInt(3);
            for (int i = 0; i < length; i++) {
                bx += d.getStepX();
                bz += d.getStepZ();
                if (i % 2 == 1 || i == length - 1) by++;
                this.set(level, box, bx, by, bz, Blocks.CHERRY_LOG.defaultBlockState()
                        .setValue(RotatedPillarBlock.AXIS, d.getAxis()));
            }
            tops.add(new int[]{bx, by + 1, bz});
        }
        for (int[] t : tops) {
            for (int layer = 0; layer < 5; layer++) {
                int ly = t[1] - 3 + layer;
                int radius = layer == 0 || layer == 4 ? 3 : 4;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                        long h = mix(treeSeed, t[0] + dx, ly, t[2] + dz);
                        if (corner && (h & 3) == 0) continue;                     // a quarter of the corners gone
                        if (corner && layer != 0) continue;
                        if (layer == 0 && (Math.abs(dx) == radius || Math.abs(dz) == radius) && ((h >>> 2) & 3) == 0) continue;
                        this.cherryLeaf(level, box, t[0] + dx, ly, t[2] + dz, h);
                        // hanging leaves under the crown's edge, now and then two deep
                        if (layer == 0 && (Math.abs(dx) == radius || Math.abs(dz) == radius) && ((h >>> 5) % 6) == 0) {
                            this.cherryLeaf(level, box, t[0] + dx, ly - 1, t[2] + dz, h >>> 9);
                            if (((h >>> 12) % 3) == 0) this.cherryLeaf(level, box, t[0] + dx, ly - 2, t[2] + dz, h >>> 15);
                        }
                    }
                }
            }
        }
    }

    private void cherryLeaf(WorldGenLevel level, BoundingBox box, int x, int y, int z, long h) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos) || !level.isEmptyBlock(pos)) return;
        int roll = (int) ((h >>> 20) % 10);
        int fruit = roll < 2 ? 2 : roll < 4 ? 1 : 0;
        level.setBlock(pos, ModBlocks.SUMMER_CHERRY_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.PERSISTENT, true).setValue(FruitLeavesBlock.FRUIT, fruit), 2);
    }

    private static long mix(long seed, int x, int y, int z) {
        long h = seed ^ (x * 341873128712L) ^ (y * 2654435761L) ^ (z * 132897987541L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return h & Long.MAX_VALUE;
    }

    // ---------------------------------------------------------------- the fields

    void buildPaddy(WorldGenLevel level, BoundingBox box, int[] field, int f) {
        int x0 = field[0], z0 = field[1], x1 = field[2], z1 = field[3];
        RiceBlock rice = ModBlocks.RICE.get();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean bank = x == x0 || x == x1 || z == z0 || z == z1 || (x - x0) % 5 == 0 || z == (z0 + z1) / 2;
                if (bank) {
                    this.set(level, box, x, f, z, st(Blocks.GRASS_BLOCK));
                    continue;
                }
                this.set(level, box, x, f - 1, z, st(Blocks.MUD));
                long h = (x * 341873128712L + z * 132897987541L) ^ this.seed;
                int roll = (int) ((h >>> 20) & 0xFF);
                this.set(level, box, x, f, z, roll % 10 < 8 ? rice.getStateForAge(2 + roll % (rice.getMaxAge() - 1)) : st(Blocks.WATER));
            }
        }
        this.set(level, box, x0 - 1, f + 1, z0 - 1, st(Blocks.COMPOSTER));
    }

    void buildCarrots(WorldGenLevel level, BoundingBox box, int[] field, int f) {
        int x0 = field[0], z0 = field[1], x1 = field[2], z1 = field[3];
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (x == x0 || x == x1 || z == z0 || z == z1) {
                    this.set(level, box, x, f, z, st(Blocks.DIRT_PATH));
                    continue;
                }
                // a water channel down every fourth row
                if ((z - z0) % 4 == 0) {
                    this.set(level, box, x, f, z, st(Blocks.WATER));
                    this.set(level, box, x, f - 1, z, st(Blocks.DIRT));
                    continue;
                }
                long h = (x * 341873128712L + z * 132897987541L) ^ this.seed;
                int age = 2 + (int) ((h >>> 20) & 0xFF) % 6;
                this.set(level, box, x, f, z, st(Blocks.FARMLAND).setValue(FarmBlock.MOISTURE, 7));
                this.set(level, box, x, f + 1, z, st(Blocks.CARROTS).setValue(CropBlock.AGE, Math.min(age, 7)));
            }
        }
        this.set(level, box, x0 - 1, f + 1, z0 - 1, st(Blocks.COMPOSTER));
    }

    // ---------------------------------------------------------------- villagers, golems, cats and fish

    void spawnLife(WorldGenLevel level, BoundingBox box, int f) {
        RandomSource random = RandomSource.create(this.seed * 31 + 11);
        for (Plot p : plots()) {
            if (p.index() == ORCHARD_PLOT) continue;
            this.spawnVillager(level, box, this.at(p.x(7), f + 1, p.z(6)));
        }
        this.spawnVillager(level, box, this.at(0, f + 1, 0));
        // the village's two iron golems, on the walk round the pond
        this.spawn(level, box, EntityType.IRON_GOLEM, this.at(POND + 2, f + 1, 4));
        this.spawn(level, box, EntityType.IRON_GOLEM, this.at(-POND - 2, f + 1, -4));
        // a great many cats
        for (int i = 0; i < 12; i++) {
            int a = random.nextInt(2 * CORE - 10) - CORE + 5;
            int x = random.nextBoolean() ? a : (random.nextBoolean() ? -1 : 1) * (POND + 1 + random.nextInt(2));
            int z = x == a ? (random.nextBoolean() ? -1 : 1) * (POND + 1 + random.nextInt(2)) : a;
            this.spawn(level, box, EntityType.CAT, this.at(x, f + 1, z));
        }
        // salmon in the pond, there to stay
        for (int i = 0; i < 14; i++) {
            int x, z;
            do {
                x = random.nextInt(2 * POND - 3) - POND + 2;
                z = random.nextInt(2 * POND - 3) - POND + 2;
            } while (!inPond(x, z) || Math.abs(x) <= ISLAND + 1 && Math.abs(z) <= ISLAND + 1);
            BlockPos pos = this.at(x, f - 1, z);
            if (!box.isInside(pos)) continue;
            if (!(EntityType.SALMON.create(level.getLevel()) instanceof AbstractFish fish)) continue;
            fish.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            fish.setFromBucket(true);
            level.addFreshEntity(fish);
        }
        // chickens by the carrot fields
        for (int[] field : CARROT_FIELDS) {
            for (int i = 0; i < 3; i++) this.spawn(level, box, EntityType.CHICKEN, this.at(field[0] - 2, f + 1, field[1] + 2 + i * 8));
        }
    }

    void spawnVillager(WorldGenLevel level, BoundingBox box, BlockPos pos) {
        if (!box.isInside(pos)) return;
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) return;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillageRegistry.JAPANESE.get()).setProfession(VillagerProfession.NONE));
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
