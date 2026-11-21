package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.worldgen.biome.AlpineTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
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
import java.util.List;

/**
 * A Tirol village (Dfb) on the flat floor of an Alpine valley, its houses strung along both banks of the stream
 * (South Tyrol's way), each facing the water:
 *
 * <ul>
 *   <li>the houses: a white plastered ground floor with small windows and red flowers in boxes beneath them; above
 *       it a storey of dark wood with a long balcony (Söller) across the front, its parapet one long box of red
 *       flowers; long boxes of red flowers under every window; a broad, gently pitched shingle roof reaching far out over the balcony and the sides; a tiled stove
 *       inside with its chimney through the roof, a woodpile under the eaves;</li>
 *   <li>the chapel, a little back from the water: a white nave under a steep shingle roof, a slender white tower
 *       with its bell and a green copper onion dome;</li>
 *   <li>a wooden bridge across the stream, a path along each bank, little paths from the doors down to it.</li>
 * </ul>
 */
public class TirolVillagePiece extends StructurePiece {
    static final int REACH = TirolVillageStructure.RADIUS + 22;

    static final ResourceKey<LootTable> HOUSE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "chests/german_village_house"));

    static final Block[] WORKSTATIONS = {
            Blocks.COMPOSTER, Blocks.SMOKER, Blocks.BARREL, Blocks.LOOM, Blocks.CARTOGRAPHY_TABLE, Blocks.FLETCHING_TABLE,
            Blocks.CAULDRON, Blocks.STONECUTTER, Blocks.SMITHING_TABLE, Blocks.GRINDSTONE, Blocks.BLAST_FURNACE,
            Blocks.LECTERN, Blocks.COMPOSTER, Blocks.BREWING_STAND};

    final BlockPos centre;
    final long seed;
    final List<int[]> plots;

    public TirolVillagePiece(BlockPos centre, long seed, List<int[]> plots) {
        super(VillageRegistry.TIROL_VILLAGE_PIECE.get(), 0, new BoundingBox(
                centre.getX() - REACH, centre.getY() - 6, centre.getZ() - REACH,
                centre.getX() + REACH, centre.getY() + 40, centre.getZ() + REACH));
        this.centre = centre;
        this.seed = seed;
        this.plots = plots;
    }

    public TirolVillagePiece(CompoundTag tag) {
        super(VillageRegistry.TIROL_VILLAGE_PIECE.get(), tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.seed = tag.getLong("Layout");
        this.plots = new ArrayList<>();
        ListTag list = tag.getList("Plots", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < list.size(); i++) this.plots.add(list.getIntArray(i));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.centre.getX());
        tag.putInt("CY", this.centre.getY());
        tag.putInt("CZ", this.centre.getZ());
        tag.putLong("Layout", this.seed);
        ListTag list = new ListTag();
        for (int[] p : this.plots) list.add(new IntArrayTag(p));
        tag.put("Plots", list);
    }

    // ================================================================ placing blocks

    private transient List<BlockPos> connecting;

    BlockPos at(int x, int y, int z) {
        return new BlockPos(this.centre.getX() + x, y, this.centre.getZ() + z);
    }

    void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, state, 2);
        if ((state.getBlock() instanceof CrossCollisionBlock || state.getBlock() instanceof WallBlock) && this.connecting != null) {
            this.connecting.add(pos.immutable());
        }
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

    static BlockState hanging() {
        return st(Blocks.LANTERN).setValue(LanternBlock.HANGING, true);
    }

    int roll(int x, int z, int salt, int n) {
        long h = (x * 341873128712L + z * 132897987541L + salt * 6971L) ^ this.seed;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) Math.floorMod(h, (long) n);
    }

    /** A building's own frame: u across its front (to its right), v from its front wall backwards. */
    record Frame(int x, int z, Direction front) {
        Direction right() {
            return this.front.getCounterClockWise();
        }

        int x(int u, int v) {
            return this.x + u * this.right().getStepX() - v * this.front.getStepX();
        }

        int z(int u, int v) {
            return this.z + u * this.right().getStepZ() - v * this.front.getStepZ();
        }

        boolean near(BoundingBox box, BlockPos centre, int reach) {
            int cx = centre.getX() + this.x(0, 4), cz = centre.getZ() + this.z(0, 4);
            return cx + reach >= box.minX() && cx - reach <= box.maxX() && cz + reach >= box.minZ() && cz - reach <= box.maxZ();
        }
    }

    void fset(WorldGenLevel level, BoundingBox box, Frame fr, int u, int y, int v, BlockState state) {
        this.set(level, box, fr.x(u, v), y, fr.z(u, v), state);
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int f = AlpineTerrain.VALLEY_Y;
        this.connecting = new ArrayList<>();
        this.paths(level, box, f);
        int index = 0;
        for (int[] p : this.plots) {
            Frame fr = new Frame(p[1], p[2], Direction.from2DDataValue(p[3]));
            switch (p[0]) {
                case TirolVillageStructure.Plot.HOUSE -> {
                    if (fr.near(box, this.centre, 16)) this.house(level, box, fr, f, index);
                    index++;
                }
                case TirolVillageStructure.Plot.CHAPEL -> {
                    if (fr.near(box, this.centre, 20)) this.chapel(level, box, fr, f);
                }
                case TirolVillageStructure.Plot.BRIDGE -> this.bridge(level, box, p[1], p[2], fr.front(), p[4], f);
                default -> {
                }
            }
        }
        for (BlockPos pos : this.connecting) {
            BlockState state = level.getBlockState(pos);
            BlockState joined = Block.updateFromNeighbourShapes(state, level, pos);
            if (joined != state) level.setBlock(pos, joined, 2);
        }
        this.connecting = null;
        this.life(level, box, f);
    }

    /**
     * A path along each bank, three or four blocks from the water, and from each house's door down towards it.
     */
    void paths(WorldGenLevel level, BoundingBox box, int f) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int x = wx - this.centre.getX(), z = wz - this.centre.getZ();
                if (x * x + z * z > TirolVillageStructure.RADIUS * TirolVillageStructure.RADIUS) continue;
                if (!level.getBlockState(pos.set(wx, f, wz)).is(Blocks.GRASS_BLOCK)) continue;
                int nearest = 99;
                for (int dx = -4; dx <= 4 && nearest > 2; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        if (level.getBlockState(pos.set(wx + dx, f, wz + dz)).is(Blocks.WATER)) {
                            nearest = Math.min(nearest, Math.max(Math.abs(dx), Math.abs(dz)));
                        }
                    }
                }
                if (nearest >= 3 && nearest <= 4) {
                    level.setBlock(pos.set(wx, f, wz), this.roll(x, z, 1, 5) == 0 ? st(Blocks.COARSE_DIRT) : st(Blocks.DIRT_PATH), 2);
                    level.setBlock(pos.set(wx, f + 1, wz), st(Blocks.AIR), 2);
                }
            }
        }
        for (int[] p : this.plots) {
            if (p[0] != TirolVillageStructure.Plot.HOUSE && p[0] != TirolVillageStructure.Plot.CHAPEL) continue;
            Frame fr = new Frame(p[1], p[2], Direction.from2DDataValue(p[3]));
            int start = p[0] == TirolVillageStructure.Plot.CHAPEL ? -6 : -3;
            for (int v = start; v >= start - 14; v--) {
                BlockPos at = this.at(fr.x(0, v), f, fr.z(0, v));
                if (!box.isInside(at)) continue;
                BlockState s = level.getBlockState(at);
                if (s.is(Blocks.WATER) || s.is(Blocks.DIRT_PATH)) break;
                if (s.is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(at, st(Blocks.DIRT_PATH), 2);
                    level.setBlock(at.above(), st(Blocks.AIR), 2);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the houses

    static BlockState plaster(int k) {
        return k < 7 ? ModBlocks.SMOOTH_WHITE_LIMESTONE.get().defaultBlockState() : st(Blocks.CALCITE);
    }

    // the red "geraniums" of the window boxes and the balcony: red tulips and poppies, a rose bush here and there
    static final Block[] FLOWERS = {Blocks.RED_TULIP, Blocks.POPPY, Blocks.RED_TULIP, Blocks.POPPY, Blocks.RED_TULIP};

    static Direction frameDir(Frame fr, int du, int dv) {
        if (du > 0) return fr.right();
        if (du < 0) return fr.right().getOpposite();
        return dv > 0 ? fr.front().getOpposite() : fr.front();
    }

    /**
     * A flower box: a row of earth along (u, v) from the first cell to the last (in a straight line), faced with
     * dark oak boards (open trapdoors) on its outer side, its ends and, if asked, its inner side, and planted all
     * along with red flowers - South Tyrol's geraniums.
     */
    void flowerBox(WorldGenLevel level, BoundingBox box, Frame fr, int y, int u0, int v0, int u1, int v1, int outU, int outV,
                   boolean inner, int salt) {
        int lu = Integer.signum(u1 - u0), lv = Integer.signum(v1 - v0);
        int n = Math.max(Math.abs(u1 - u0), Math.abs(v1 - v0));
        BlockState board = st(Blocks.DARK_OAK_TRAPDOOR).setValue(TrapDoorBlock.OPEN, true);
        for (int k = 0; k <= n; k++) {
            int u = u0 + k * lu, v = v0 + k * lv;
            this.fset(level, box, fr, u, y, v, st(Blocks.DIRT));
            this.fset(level, box, fr, u + outU, y, v + outV, board.setValue(TrapDoorBlock.FACING, frameDir(fr, outU, outV)));
            if (inner) this.fset(level, box, fr, u - outU, y, v - outV, board.setValue(TrapDoorBlock.FACING, frameDir(fr, -outU, -outV)));
            Block flower = FLOWERS[Math.floorMod(u * 7 + v * 3 + salt, FLOWERS.length)];
            this.fset(level, box, fr, u, y + 1, v, st(flower));
        }
        if (n > 0 || lu != 0 || lv != 0) {
            int au = lu == 0 && lv == 0 ? (outV != 0 ? 1 : 0) : lu, av = lu == 0 && lv == 0 ? (outU != 0 ? 1 : 0) : lv;
            this.fset(level, box, fr, u0 - au, y, v0 - av, board.setValue(TrapDoorBlock.FACING, frameDir(fr, -au, -av)));
            this.fset(level, box, fr, u1 + au, y, v1 + av, board.setValue(TrapDoorBlock.FACING, frameDir(fr, au, av)));
        }
    }

    /**
     * A South Tyrolean house: 9 wide, 9 deep, facing the stream. White ground floor, dark wooden upper storey with
     * the balcony across its front, a broad low roof of shingles reaching out over everything.
     */
    void house(WorldGenLevel level, BoundingBox box, Frame fr, int f, int index) {
        Direction front = fr.front(), back = front.getOpposite(), right = fr.right(), left = right.getOpposite();
        BlockState white = plaster(this.roll(fr.x(), fr.z(), 2, 10));
        BlockState dark = st(Blocks.DARK_OAK_PLANKS);
        BlockState post = log(Blocks.STRIPPED_DARK_OAK_LOG, Direction.Axis.Y);
        BlockState beamAcross = log(Blocks.DARK_OAK_LOG, right.getAxis()), beamAlong = log(Blocks.DARK_OAK_LOG, front.getAxis());
        int half = 4, depth = 9;
        int top = f + 8;
        // ---- foundation, floors and walls
        for (int u = -half; u <= half; u++) {
            for (int v = 0; v < depth; v++) {
                boolean edge = Math.abs(u) == half || v == 0 || v == depth - 1;
                boolean corner = Math.abs(u) == half && (v == 0 || v == depth - 1);
                for (int y = f - 3; y < f; y++) this.fset(level, box, fr, u, y, v, st(Blocks.COBBLESTONE));
                this.fset(level, box, fr, u, f, v, edge ? st(Blocks.STONE_BRICKS) : st(Blocks.SPRUCE_PLANKS));
                this.fset(level, box, fr, u, f + 4, v, edge ? (v == 0 || v == depth - 1 ? beamAcross : beamAlong) : dark);
                this.fset(level, box, fr, u, top, v, edge ? (v == 0 || v == depth - 1 ? beamAcross : beamAlong) : st(Blocks.SPRUCE_PLANKS));
                for (int dy = 1; dy <= 3; dy++) {
                    // the white ground floor: windows at eye height, the door in the middle of the front
                    BlockState g = edge ? white : st(Blocks.AIR);
                    if (edge && dy == 2 && !corner && this.window(u, v, half, depth)) g = st(Blocks.GLASS_PANE);
                    this.fset(level, box, fr, u, f + dy, v, g);
                    // the dark wooden storey above
                    BlockState w = edge ? (corner ? post : dark) : st(Blocks.AIR);
                    if (edge && !corner && dy == 2 && this.window(u, v, half, depth)) w = st(Blocks.GLASS_PANE);
                    if (edge && !corner && dy == 3) w = v == 0 || v == depth - 1 ? beamAcross : beamAlong;
                    this.fset(level, box, fr, u, f + 4 + dy, v, w);
                }
            }
        }
        // red flowers under every window: a long box of them below each, downstairs and up (the front's upper
        // windows have the balcony's)
        for (int sd : new int[]{-1, 1}) {
            this.flowerBox(level, box, fr, f + 1, sd * 2, -1, sd * 3, -1, 0, -1, false, index);
        }
        this.flowerBox(level, box, fr, f + 1, -3, depth, 3, depth, 0, 1, false, index + 1);
        this.flowerBox(level, box, fr, f + 5, -3, depth, 3, depth, 0, 1, false, index + 2);
        for (int sd : new int[]{-1, 1}) {
            this.flowerBox(level, box, fr, f + 1, sd * (half + 1), 1, sd * (half + 1), 6, sd, 0, false, index + 3);
            this.flowerBox(level, box, fr, f + 5, sd * (half + 1), 1, sd * (half + 1), 6, sd, 0, false, index + 4);
        }
        // the doors: below onto the bank, above onto the balcony
        BlockState door = st(Blocks.DARK_OAK_DOOR).setValue(DoorBlock.FACING, front).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        for (int y : new int[]{f + 1, f + 5}) {
            this.fset(level, box, fr, 0, y, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.fset(level, box, fr, 0, y + 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }
        this.fset(level, box, fr, 0, f + 3, -1, hanging());
        // ---- the balcony (Söller) across the front: its parapet one long box of red flowers from end to end,
        // carved brackets beneath
        for (int u = -half; u <= half; u++) {
            for (int v = -2; v <= -1; v++) this.fset(level, box, fr, u, f + 4, v, slab(Blocks.DARK_OAK_SLAB, SlabType.TOP));
        }
        this.flowerBox(level, box, fr, f + 5, -half, -2, half, -2, 0, -1, true, index + 5);
        for (int s : new int[]{-1, 1}) {
            this.fset(level, box, fr, s * half, f + 5, -1, st(Blocks.DARK_OAK_FENCE));
            this.fset(level, box, fr, s * half, f + 3, -1, stair(Blocks.DARK_OAK_STAIRS, back, true));
            this.fset(level, box, fr, s * (half - 3), f + 3, -1, stair(Blocks.DARK_OAK_STAIRS, back, true));
        }
        // ---- the roof: low and broad, shingles of spruce, out two blocks over the sides and three over the balcony
        int eave = half + 2;
        for (int u = -eave; u <= eave; u++) {
            int k = eave - Math.abs(u);
            int y = top + k / 2;
            BlockState sh = slab(Blocks.SPRUCE_SLAB, k % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP);
            if (u == 0) sh = st(Blocks.SPRUCE_PLANKS);
            for (int v = -3; v <= depth + 1; v++) {
                this.fset(level, box, fr, u, y, v, sh);
            }
            // the gables, front and back, of dark boards, a small window in the front one
            if (Math.abs(u) <= half - 1) {
                for (int gy = top + 1; gy < y + (k % 2 == 1 ? 1 : 0); gy++) {
                    for (int v : new int[]{0, depth - 1}) {
                        BlockState g = (u == 0 && gy == top + 1 && v == 0) ? st(Blocks.GLASS_PANE) : dark;
                        this.fset(level, box, fr, u, gy, v, g);
                    }
                }
            }
        }
        // the attic: hay and a lantern from the ridge
        this.fset(level, box, fr, 0, top + 2, 4, hanging());
        this.fset(level, box, fr, -2, top + 1, 6, st(Blocks.HAY_BLOCK));
        // ---- the tiled stove (Kachelofen) in the back corner, its chimney up through the roof
        for (int u = 1; u <= 2; u++) {
            for (int v = 6; v <= 7; v++) {
                this.fset(level, box, fr, u, f + 1, v, st(Blocks.GREEN_TERRACOTTA));
                this.fset(level, box, fr, u, f + 2, v, u == 2 && v == 7 ? st(Blocks.BRICKS) : st(Blocks.GREEN_TERRACOTTA));
            }
        }
        for (int y = f + 3; y <= top + 5; y++) this.fset(level, box, fr, 2, y, 7, st(Blocks.BRICKS));
        this.fset(level, box, fr, 2, top + 6, 7, st(Blocks.BRICK_WALL));
        // ---- inside: the workshop and the table downstairs, the bedroom above, a ladder up through the floors
        Block ws = WORKSTATIONS[Math.floorMod(index, WORKSTATIONS.length)];
        BlockState wsState = ws.defaultBlockState();
        if (wsState.hasProperty(HorizontalDirectionalBlock.FACING)) wsState = wsState.setValue(HorizontalDirectionalBlock.FACING, back);
        this.fset(level, box, fr, -3, f + 1, 1, wsState);
        this.fset(level, box, fr, 3, f + 1, 1, st(Blocks.CRAFTING_TABLE));
        this.fset(level, box, fr, -1, f + 1, 4, st(Blocks.SPRUCE_FENCE));
        this.fset(level, box, fr, -1, f + 2, 4, st(Blocks.SPRUCE_PRESSURE_PLATE));
        this.fset(level, box, fr, -2, f + 1, 4, stair(Blocks.SPRUCE_STAIRS, left, false));
        this.fset(level, box, fr, 0, f + 1, 4, stair(Blocks.SPRUCE_STAIRS, right, false));
        this.chest(level, box, fr, -3, f + 1, 7, right);
        this.fset(level, box, fr, 0, f + 3, 4, hanging());
        BlockState bed = (index % 2 == 0 ? Blocks.RED_BED : Blocks.GREEN_BED).defaultBlockState().setValue(BedBlock.FACING, back);
        this.fset(level, box, fr, -3, f + 5, 7, bed.setValue(BedBlock.PART, BedPart.HEAD));
        this.fset(level, box, fr, -3, f + 5, 6, bed.setValue(BedBlock.PART, BedPart.FOOT));
        this.chest(level, box, fr, 3, f + 5, 2, left);
        this.fset(level, box, fr, 0, f + 7, 4, hanging());
        for (int y = f + 1; y <= top; y++) {
            this.fset(level, box, fr, 3, y, 7, st(Blocks.LADDER).setValue(LadderBlock.FACING, front));
        }
        // ---- the woodpile under the eaves, behind the flower boxes
        for (int v = 7; v <= 8; v++) {
            for (int y = f + 1; y <= f + 2; y++) this.fset(level, box, fr, half + 1, y, v, log(Blocks.SPRUCE_LOG, front.getAxis()));
        }
    }

    boolean window(int u, int v, int half, int depth) {
        if (v == 0 || v == depth - 1) return Math.abs(u) == 2;
        if (Math.abs(u) == half) return v == 2 || v == 5;
        return false;
    }

    void chest(WorldGenLevel level, BoundingBox box, Frame fr, int u, int y, int v, Direction facing) {
        BlockPos pos = this.at(fr.x(u, v), y, fr.z(u, v));
        if (!box.isInside(pos)) return;
        level.setBlock(pos, st(Blocks.CHEST).setValue(ChestBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) chest.setLootTable(HOUSE_LOOT, this.seed ^ pos.asLong());
    }

    // ---------------------------------------------------------------- the chapel

    /**
     * The chapel: a white nave 7 wide and 13 long under a steep shingle roof, a slender white tower before it (on
     * the side towards the stream) with its bell and a green copper onion dome.
     */
    void chapel(WorldGenLevel level, BoundingBox box, Frame fr, int f) {
        Direction front = fr.front(), back = front.getOpposite(), right = fr.right(), left = right.getOpposite();
        BlockState white = ModBlocks.SMOOTH_WHITE_LIMESTONE.get().defaultBlockState();
        int nh = 3, len = 13, wallTop = f + 6;
        // ---- the nave
        for (int u = -nh; u <= nh; u++) {
            for (int v = 0; v < len; v++) {
                boolean edge = Math.abs(u) == nh || v == 0 || v == len - 1;
                for (int y = f - 3; y < f; y++) this.fset(level, box, fr, u, y, v, st(Blocks.COBBLESTONE));
                this.fset(level, box, fr, u, f, v, edge ? st(Blocks.STONE_BRICKS) : st(Blocks.SPRUCE_PLANKS));
                for (int y = f + 1; y <= wallTop + 6; y++) {
                    BlockState s = st(Blocks.AIR);
                    if (edge && y <= wallTop) {
                        s = white;
                        // tall round-headed windows along the sides
                        if (Math.abs(u) == nh && (v == 3 || v == 6 || v == 9) && y >= f + 2 && y <= f + 4) s = st(Blocks.GLASS_PANE);
                    }
                    this.fset(level, box, fr, u, y, v, s);
                }
            }
        }
        // the steep roof, one up for one across, spruce shingles; white gable at the back
        for (int k = 0; k <= nh + 1; k++) {
            int y = wallTop + 1 + k;
            for (int v = -1; v <= len; v++) {
                if (nh + 1 - k > 0) {
                    this.fset(level, box, fr, -(nh + 1) + k, y, v, stair(Blocks.SPRUCE_STAIRS, right, false));
                    this.fset(level, box, fr, nh + 1 - k, y, v, stair(Blocks.SPRUCE_STAIRS, left, false));
                } else {
                    this.fset(level, box, fr, 0, y, v, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
                }
            }
            for (int u = -(nh - k); u <= nh - k; u++) {
                this.fset(level, box, fr, u, y, len - 1, white);
                this.fset(level, box, fr, u, y, 0, white);
            }
        }
        // inside: pews facing the altar at the back, the altar under a crucifix, lanterns from the ridge
        for (int v = 3; v <= 8; v += 1) {
            for (int u : new int[]{-2, -1, 1, 2}) this.fset(level, box, fr, u, f + 1, v, stair(Blocks.SPRUCE_STAIRS, front, false));
        }
        for (int u = -1; u <= 1; u++) {
            this.fset(level, box, fr, u, f + 1, len - 3, st(Blocks.QUARTZ_BLOCK));
            this.fset(level, box, fr, u, f + 2, len - 3, st(Blocks.WHITE_CARPET));
        }
        this.fset(level, box, fr, -1, f + 3, len - 3, st(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        this.fset(level, box, fr, 1, f + 3, len - 3, st(Blocks.CANDLE).setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true));
        for (int y = f + 2; y <= f + 5; y++) this.fset(level, box, fr, 0, y, len - 2, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        this.fset(level, box, fr, -1, f + 4, len - 2, log(Blocks.DARK_OAK_LOG, right.getAxis()));
        this.fset(level, box, fr, 1, f + 4, len - 2, log(Blocks.DARK_OAK_LOG, right.getAxis()));
        for (int v : new int[]{3, 8}) {
            this.fset(level, box, fr, 0, wallTop + 4, v, st(Blocks.CHAIN));
            this.fset(level, box, fr, 0, wallTop + 3, v, hanging());
        }
        // ---- the tower before the nave, on the stream's side
        int t0 = -5, t1 = -1, tTop = f + 16;
        for (int u = -2; u <= 2; u++) {
            for (int v = t0; v <= t1; v++) {
                boolean rim = Math.abs(u) == 2 || v == t0 || v == t1;
                for (int y = f - 3; y < f; y++) this.fset(level, box, fr, u, y, v, st(Blocks.COBBLESTONE));
                this.fset(level, box, fr, u, f, v, st(Blocks.STONE_BRICKS));
                for (int y = f + 1; y <= tTop; y++) {
                    BlockState s = rim ? white : st(Blocks.AIR);
                    // the belfry's arched openings on all four sides
                    boolean mid = (Math.abs(u) == 2 && v == -3) || ((v == t0 || v == t1) && u == 0);
                    if (rim && mid && y >= tTop - 3 && y <= tTop - 1) s = st(Blocks.AIR);
                    // the clock faces under them
                    if (rim && mid && y == tTop - 6) s = st(Blocks.GOLD_BLOCK);
                    this.fset(level, box, fr, u, y, v, s);
                }
                if (!rim) this.fset(level, box, fr, u, tTop - 4, v, st(Blocks.SPRUCE_PLANKS));
            }
        }
        // the door, and the way through into the nave
        BlockState door = st(Blocks.DARK_OAK_DOOR).setValue(DoorBlock.FACING, front).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.fset(level, box, fr, 0, f + 1, t0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.fset(level, box, fr, 0, f + 2, t0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        for (int v = t1; v <= 0; v++) {
            this.fset(level, box, fr, 0, f + 1, v, st(Blocks.AIR));
            this.fset(level, box, fr, 0, f + 2, v, st(Blocks.AIR));
        }
        this.fset(level, box, fr, 0, f + 3, t0 - 1, hanging().setValue(LanternBlock.HANGING, false));
        for (int y = f + 1; y <= tTop - 4; y++) this.fset(level, box, fr, 1, y, t0 + 1, st(Blocks.LADDER).setValue(LadderBlock.FACING, back));
        this.fset(level, box, fr, 0, tTop - 3, -3, st(Blocks.BELL));
        this.fset(level, box, fr, -1, tTop - 5, -3, hanging());
        // ---- the onion dome of green copper, a gilded cross on its tip
        BlockState copper = st(Blocks.OXIDIZED_CUT_COPPER);
        int[][] layers = {{2, 0}, {3, 1}, {2, 0}, {1, 0}, {0, 0}};
        int y = tTop + 1;
        for (int[] layer : layers) {
            int r = layer[0];
            for (int u = -r; u <= r; u++) {
                for (int v = -r; v <= r; v++) {
                    if (layer[1] == 1 && Math.abs(u) == r && Math.abs(v) == r) continue;
                    this.fset(level, box, fr, u, y, -3 + v, copper);
                }
            }
            y++;
        }
        this.fset(level, box, fr, 0, y, -3, st(Blocks.GOLD_BLOCK));
        this.fset(level, box, fr, 0, y + 1, -3, st(Blocks.LIGHTNING_ROD));
    }

    // ---------------------------------------------------------------- the bridge

    /** A wooden footbridge across the stream: a spruce deck a block over the bank, rails, steps down at each end. */
    void bridge(WorldGenLevel level, BoundingBox box, int x, int z, Direction axis, int halfLength, int f) {
        Direction side = axis.getClockWise();
        for (int t = -halfLength; t <= halfLength; t++) {
            for (int w = -1; w <= 1; w++) {
                int bx = x + t * axis.getStepX() + w * side.getStepX(), bz = z + t * axis.getStepZ() + w * side.getStepZ();
                if (Math.abs(t) == halfLength) {
                    this.set(level, box, bx, f + 1, bz, stair(Blocks.SPRUCE_STAIRS, t > 0 ? axis.getOpposite() : axis, false));
                    continue;
                }
                this.set(level, box, bx, f + 1, bz, st(Blocks.SPRUCE_PLANKS));
                if (w != 0 && Math.abs(t) < halfLength - 1) this.set(level, box, bx, f + 2, bz, st(Blocks.SPRUCE_FENCE));
                // posts down into the stream every few blocks
                if (w != 0 && t % 3 == 0) {
                    for (int y = f - 2; y <= f; y++) this.set(level, box, bx, y, bz, log(Blocks.SPRUCE_LOG, Direction.Axis.Y));
                }
            }
        }
        this.set(level, box, x + side.getStepX(), f + 3, z + side.getStepZ(), st(Blocks.LANTERN));
    }

    // ---------------------------------------------------------------- the people

    void life(WorldGenLevel level, BoundingBox box, int f) {
        RandomSource random = RandomSource.create(this.seed * 13 + 5);
        int n = 0;
        for (int[] p : this.plots) {
            Frame fr = new Frame(p[1], p[2], Direction.from2DDataValue(p[3]));
            if (p[0] == TirolVillageStructure.Plot.HOUSE) {
                this.villager(level, this.at(fr.x(0, 3), f + 1, fr.z(0, 3)), box);
                if (n++ % 3 == 0) this.mob(level, EntityType.CAT, this.at(fr.x(1, -4), f + 1, fr.z(1, -4)), box);
            } else if (p[0] == TirolVillageStructure.Plot.CHAPEL) {
                this.villager(level, this.at(fr.x(0, -8), f + 1, fr.z(0, -8)), box);
                this.mob(level, EntityType.IRON_GOLEM, this.at(fr.x(4, -9), f + 1, fr.z(4, -9)), box);
            }
        }
        for (int i = 0; i < 3; i++) {
            this.mob(level, EntityType.COW, this.at(random.nextInt(40) - 20, f + 1, random.nextInt(40) - 20), box);
        }
    }

    void villager(WorldGenLevel level, BlockPos pos, BoundingBox box) {
        if (!box.isInside(pos) || !level.getBlockState(pos).isAir()) return;
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) return;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillageRegistry.TIROL.get()).setProfession(VillagerProfession.NONE));
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }

    void mob(WorldGenLevel level, EntityType<?> type, BlockPos pos, BoundingBox box) {
        if (!box.isInside(pos) || !level.getBlockState(pos).isAir()) return;
        if (!(type.create(level.getLevel()) instanceof Mob mob)) return;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
