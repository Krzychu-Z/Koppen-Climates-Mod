package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.block.custom.SaunaStoveBlock;
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
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
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
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.List;

/**
 * A Viking village of the Subpolar taiga (Dfc), on a round platform levelled for it, everything of spruce:
 *
 * <ul>
 *   <li>in the middle the longhouse: long walls of spruce logs laid one on another between upright posts, a steep
 *       spruce roof with crossed "horns" at the gable ends, doors at both ends; inside, a long hearth of three fires
 *       down the middle, benches the length of both walls under a row of red and white shields, the chieftain's high
 *       seat at the far end, beds, a lectern and a brewing stand;</li>
 *   <li>six huts in two rows facing the longhouse across two lanes: log walls, steep roofs, a bed, a chest, a
 *       workstation, a fur rug; a woodpile beside each;</li>
 *   <li>before the longhouse a fire pit, the bell, and a carved rune stone; behind it a pen of reindeer and sheep,
 *       and two little fields of wheat;</li>
 *   <li>two dry saunas: log cabins with a lit sauna stove heaped with stones, two tiers of benches and a cauldron of
 *       water - working ones (see SaunaStoveBlock, SaunaWarmth).</li>
 * </ul>
 */
public class VikingVillagePiece extends StructurePiece {
    static final int CORE = 27, MARGIN = 10, BOX = CORE + MARGIN + 2, CLEAR = 30;

    static final ResourceKey<LootTable> HOUSE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "chests/german_village_house"));

    // the huts' workstations; the longhouse has the lectern and the brewing stand
    static final Block[] WORKSTATIONS = {Blocks.COMPOSTER, Blocks.BARREL, Blocks.FLETCHING_TABLE, Blocks.LOOM,
            Blocks.SMITHING_TABLE, Blocks.SMOKER};

    // the huts: (x, z) of the middle of their front wall, in the village's own frame (the longhouse runs along z)
    static final int[][] HUTS = {{-10, -12}, {-10, 0}, {-10, 12}, {10, -12}, {10, 0}, {10, 12}};
    static final int[][] SAUNAS = {{-7, -19}, {7, -19}};
    static final int LONG_HALF = 4, LONG_LEN = 23;

    final BlockPos centre;
    final long seed;
    final int turn;

    public VikingVillagePiece(BlockPos centre, long seed, int turn) {
        super(VillageRegistry.VIKING_VILLAGE_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX, centre.getY() - 24, centre.getZ() - BOX,
                centre.getX() + BOX, centre.getY() + CLEAR, centre.getZ() + BOX));
        this.centre = centre;
        this.seed = seed;
        this.turn = turn;
    }

    public VikingVillagePiece(CompoundTag tag) {
        super(VillageRegistry.VIKING_VILLAGE_PIECE.get(), tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.seed = tag.getLong("Layout");
        this.turn = tag.getInt("Turn");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.centre.getX());
        tag.putInt("CY", this.centre.getY());
        tag.putInt("CZ", this.centre.getZ());
        tag.putLong("Layout", this.seed);
        tag.putInt("Turn", this.turn);
    }

    // ================================================================ the village's own frame, turned

    int rx(int lx, int lz) {
        return switch (this.turn) {
            case 1 -> -lz;
            case 2 -> -lx;
            case 3 -> lz;
            default -> lx;
        };
    }

    int rz(int lx, int lz) {
        return switch (this.turn) {
            case 1 -> lx;
            case 2 -> -lz;
            case 3 -> -lx;
            default -> lz;
        };
    }

    Direction rd(Direction d) {
        for (int i = 0; i < this.turn; i++) d = d.getClockWise();
        return d;
    }

    /** A building's frame at (lx, lz) in the village's frame, its front facing that way. */
    Frame frame(int lx, int lz, Direction front) {
        return new Frame(this.rx(lx, lz), this.rz(lx, lz), this.rd(front));
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
        if ((state.getBlock() instanceof CrossCollisionBlock || state.getBlock() instanceof WallBlock
                || state.getBlock() instanceof FenceGateBlock) && this.connecting != null) {
            this.connecting.add(pos.immutable());
        }
    }

    void fset(WorldGenLevel level, BoundingBox box, Frame fr, int u, int y, int v, BlockState state) {
        this.set(level, box, fr.x(u, v), y, fr.z(u, v), state);
    }

    /** In the village's own (unturned) frame. */
    void lset(WorldGenLevel level, BoundingBox box, int lx, int y, int lz, BlockState state) {
        this.set(level, box, this.rx(lx, lz), y, this.rz(lx, lz), state);
    }

    BlockState lget(WorldGenLevel level, int lx, int y, int lz) {
        return level.getBlockState(this.at(this.rx(lx, lz), y, this.rz(lx, lz)));
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

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int f = this.centre.getY();
        this.connecting = new ArrayList<>();
        this.shapeGround(level, box, f);
        int cx = this.centre.getX(), cz = this.centre.getZ();
        LooseTreeCleanup.clean(level, chunkPos, f - 12, f + CLEAR + 6,
                (x, z) -> platformDistance(x - cx, z - cz) < MARGIN + 10,
                (x, z) -> platformDistance(x - cx, z - cz) == 0);
        this.paths(level, box, f);
        this.longhouse(level, box, this.frame(0, -11, Direction.NORTH), f);
        for (int i = 0; i < HUTS.length; i++) {
            int[] h = HUTS[i];
            this.hut(level, box, this.frame(h[0], h[1], h[0] < 0 ? Direction.EAST : Direction.WEST), f, i);
        }
        for (int[] s : SAUNAS) this.sauna(level, box, this.frame(s[0], s[1], Direction.SOUTH), f);
        this.square(level, box, f);
        this.pen(level, box, f);
        for (int side : new int[]{-1, 1}) this.field(level, box, f, side);
        for (BlockPos pos : this.connecting) {
            BlockState state = level.getBlockState(pos);
            BlockState joined = Block.updateFromNeighbourShapes(state, level, pos);
            if (joined != state) level.setBlock(pos, joined, 2);
        }
        this.connecting = null;
        this.life(level, box, f);
    }

    // ---------------------------------------------------------------- the ground

    static double platformDistance(int x, int z) {
        return Math.max(0.0, Mth.length(x, z) - CORE);
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** The round platform at the village's floor, its edge blended smoothly back into the ground about it. */
    void shapeGround(WorldGenLevel level, BoundingBox box, int f) {
        BlockState grass = st(Blocks.GRASS_BLOCK), dirt = st(Blocks.DIRT);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int x = wx - this.centre.getX(), z = wz - this.centre.getZ();
                double dist = platformDistance(x, z);
                if (dist >= MARGIN) continue;
                int ground = f - 24;
                for (int y = f + CLEAR; y > f - 24; y--) {
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

    /**
     * The lanes: one down each side of the longhouse, between it and the huts, joined across before it (round the
     * fire pit) and behind it; trodden earth with a stone or two of gravel.
     */
    void paths(WorldGenLevel level, BoundingBox box, int f) {
        for (int lx = -CORE; lx <= CORE; lx++) {
            for (int lz = -CORE; lz <= CORE; lz++) {
                int ax = Math.abs(lx);
                boolean lane = ax >= 6 && ax <= 9 && lz >= -17 && lz <= 16;
                boolean front = ax <= 9 && lz >= -17 && lz <= -13;
                boolean back = ax <= 9 && lz >= 13 && lz <= 14;
                if (!lane && !front && !back) continue;
                if (!this.lget(level, lx, f, lz).is(Blocks.GRASS_BLOCK)) continue;
                int r = this.roll(lx, lz, 3, 9);
                this.lset(level, box, lx, f, lz, r == 0 ? st(Blocks.GRAVEL) : r == 1 ? st(Blocks.COARSE_DIRT) : st(Blocks.DIRT_PATH));
            }
        }
    }

    // ---------------------------------------------------------------- the longhouse

    void longhouse(WorldGenLevel level, BoundingBox box, Frame fr, int f) {
        Direction front = fr.front(), back = front.getOpposite(), right = fr.right(), left = right.getOpposite();
        int half = LONG_HALF, len = LONG_LEN;
        BlockState post = log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        BlockState along = log(Blocks.STRIPPED_SPRUCE_LOG, front.getAxis()), across = log(Blocks.STRIPPED_SPRUCE_LOG, right.getAxis());
        for (int u = -half; u <= half; u++) {
            for (int v = 0; v < len; v++) {
                boolean side = Math.abs(u) == half, end = v == 0 || v == len - 1;
                boolean edge = side || end;
                for (int y = f - 3; y < f; y++) this.fset(level, box, fr, u, y, v, st(Blocks.COBBLESTONE));
                this.fset(level, box, fr, u, f, v, edge ? st(Blocks.COBBLESTONE) : st(Blocks.SPRUCE_PLANKS));
                for (int dy = 1; dy <= 4; dy++) {
                    BlockState s = st(Blocks.AIR);
                    if (edge) {
                        boolean upright = (side && (end || v % 4 == 0)) || (end && Math.abs(u) == 2);
                        s = upright ? post : side ? along : across;
                        if (side && !end && dy == 2 && v % 4 == 2) s = st(Blocks.GLASS_PANE);
                    }
                    this.fset(level, box, fr, u, f + dy, v, s);
                }
            }
        }
        // the long hearth down the middle: a bed of stones, three fires
        for (int v = 7; v <= 15; v++) {
            for (int u = -1; u <= 1; u++) this.fset(level, box, fr, u, f, v, st(Blocks.COBBLESTONE));
        }
        for (int v : new int[]{8, 11, 14}) this.fset(level, box, fr, 0, f + 1, v, st(Blocks.CAMPFIRE));
        // the roof: steep, of spruce, out a block over the walls and the gable ends
        this.roof(level, box, fr, f + 4, half + 1, -1, len, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB);
        this.gables(level, box, fr, f + 4, half, 0, len - 1);
        this.fset(level, box, fr, 0, f + 6, 0, st(Blocks.GLASS_PANE));
        this.fset(level, box, fr, 0, f + 6, len - 1, st(Blocks.GLASS_PANE));
        // the crossed horns at both gable ends
        for (int v : new int[]{-1, len}) {
            for (int s : new int[]{-1, 1}) {
                this.fset(level, box, fr, s, f + 9, v, st(Blocks.SPRUCE_FENCE));
                this.fset(level, box, fr, s, f + 10, v, st(Blocks.SPRUCE_FENCE));
            }
        }
        // the doors, front and back
        BlockState door = st(Blocks.SPRUCE_DOOR).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        for (int[] d : new int[][]{{0, 0}, {len - 1, 1}}) {
            Direction facing = d[1] == 0 ? front : back;
            this.fset(level, box, fr, 0, f + 1, d[0], door.setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            this.fset(level, box, fr, 0, f + 2, d[0], door.setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            this.fset(level, box, fr, 0, f, d[0], st(Blocks.SPRUCE_PLANKS));
        }
        this.fset(level, box, fr, 1, f + 3, -1, st(Blocks.LANTERN).setValue(LanternBlock.HANGING, false));
        this.fset(level, box, fr, 1, f + 1, -1, st(Blocks.SPRUCE_FENCE));
        this.fset(level, box, fr, 1, f + 2, -1, st(Blocks.SPRUCE_FENCE));
        // the benches the length of both walls, the shields above them
        for (int v = 2; v <= 18; v++) {
            this.fset(level, box, fr, half - 1, f + 1, v, stair(Blocks.SPRUCE_STAIRS, right, false));
            this.fset(level, box, fr, -(half - 1), f + 1, v, stair(Blocks.SPRUCE_STAIRS, left, false));
        }
        for (int v = 3; v <= 19; v += 4) {
            Block red = Blocks.RED_WALL_BANNER, white = Blocks.WHITE_WALL_BANNER;
            this.fset(level, box, fr, half - 1, f + 4, v, st((v / 4) % 2 == 0 ? red : white).setValue(WallBannerBlock.FACING, left));
            this.fset(level, box, fr, -(half - 1), f + 4, v, st((v / 4) % 2 == 0 ? white : red).setValue(WallBannerBlock.FACING, right));
        }
        // the chieftain's high seat, at the far end facing the door, beds either side of it behind
        this.fset(level, box, fr, 0, f + 1, len - 4, stair(Blocks.SPRUCE_STAIRS, back, false));
        this.fset(level, box, fr, -1, f + 1, len - 4, st(Blocks.SPRUCE_FENCE));
        this.fset(level, box, fr, 1, f + 1, len - 4, st(Blocks.SPRUCE_FENCE));
        for (int s : new int[]{-1, 1}) {
            BlockState bed = (s < 0 ? Blocks.RED_BED : Blocks.WHITE_BED).defaultBlockState().setValue(BedBlock.FACING, back);
            this.fset(level, box, fr, s * 2, f + 1, len - 2, bed.setValue(BedBlock.PART, BedPart.HEAD));
            this.fset(level, box, fr, s * 2, f + 1, len - 3, bed.setValue(BedBlock.PART, BedPart.FOOT));
        }
        this.fset(level, box, fr, -(half - 1), f + 1, len - 3, st(Blocks.LECTERN).setValue(HorizontalDirectionalBlock.FACING, right));
        this.fset(level, box, fr, half - 1, f + 1, len - 3, st(Blocks.BREWING_STAND));
        this.chest(level, box, fr, -(half - 1), f + 1, len - 2, right);
        this.chest(level, box, fr, half - 1, f + 1, len - 2, left);
        // lanterns down the hall from the ridge
        for (int v : new int[]{4, 10, 16, 20}) {
            this.fset(level, box, fr, 0, f + 7, v, st(Blocks.CHAIN));
            this.fset(level, box, fr, 0, f + 6, v, hanging());
        }
    }

    /** The gable ends (v0 and v1) filled in with planks, from the walls' top (y) up under the roof. */
    void gables(WorldGenLevel level, BoundingBox box, Frame fr, int y, int half, int v0, int v1) {
        for (int v : new int[]{v0, v1}) {
            for (int k = 1; k <= half; k++) {
                for (int u = -(half - k); u <= half - k; u++) this.fset(level, box, fr, u, y + k, v, st(Blocks.SPRUCE_PLANKS));
            }
        }
    }

    /**
     * A steep gable roof of stairs (one up for one across) from the eaves at (±eave, y) - beside the walls' top - to
     * the ridge, from v0 to v1; the ridge a beam of planks under a slab.
     */
    void roof(WorldGenLevel level, BoundingBox box, Frame fr, int y, int eave, int v0, int v1, Block stairs, Block slab) {
        Direction right = fr.right(), left = right.getOpposite();
        for (int v = v0; v <= v1; v++) {
            for (int k = 0; k < eave; k++) {
                this.fset(level, box, fr, -(eave - k), y + k, v, stair(stairs, right, false));
                this.fset(level, box, fr, eave - k, y + k, v, stair(stairs, left, false));
                // under the eaves, the stairs' undersides
                if (k == 0) {
                    this.fset(level, box, fr, -eave, y - 1, v, stair(stairs, left, true));
                    this.fset(level, box, fr, eave, y - 1, v, stair(stairs, right, true));
                }
            }
            this.fset(level, box, fr, 0, y + eave - 1, v, st(Blocks.SPRUCE_PLANKS));
            this.fset(level, box, fr, 0, y + eave, v, slab(slab, SlabType.BOTTOM));
        }
    }

    // ---------------------------------------------------------------- the huts

    void hut(WorldGenLevel level, BoundingBox box, Frame fr, int f, int index) {
        Direction front = fr.front(), back = front.getOpposite(), right = fr.right(), left = right.getOpposite();
        int half = 3, depth = 8;
        BlockState post = log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        BlockState along = log(Blocks.SPRUCE_LOG, front.getAxis()), across = log(Blocks.SPRUCE_LOG, right.getAxis());
        for (int u = -half; u <= half; u++) {
            for (int v = 0; v < depth; v++) {
                boolean side = Math.abs(u) == half, end = v == 0 || v == depth - 1;
                boolean edge = side || end;
                for (int y = f - 3; y < f; y++) this.fset(level, box, fr, u, y, v, st(Blocks.COBBLESTONE));
                this.fset(level, box, fr, u, f, v, edge ? st(Blocks.COBBLESTONE) : st(Blocks.SPRUCE_PLANKS));
                for (int dy = 1; dy <= 3; dy++) {
                    BlockState s = st(Blocks.AIR);
                    if (edge) {
                        s = side && end ? post : side ? along : across;
                        if (dy == 2 && ((side && v == 4) || (v == depth - 1 && u == 0))) s = st(Blocks.GLASS_PANE);
                    }
                    this.fset(level, box, fr, u, f + dy, v, s);
                }
            }
        }
        this.roof(level, box, fr, f + 3, half + 1, -1, depth, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB);
        this.gables(level, box, fr, f + 3, half, 0, depth - 1);
        // the gable horns over the door
        this.fset(level, box, fr, -1, f + 6, -1, st(Blocks.SPRUCE_FENCE));
        this.fset(level, box, fr, 1, f + 6, -1, st(Blocks.SPRUCE_FENCE));
        BlockState door = st(Blocks.SPRUCE_DOOR).setValue(DoorBlock.FACING, front).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.fset(level, box, fr, 0, f + 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.fset(level, box, fr, 0, f + 2, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        this.fset(level, box, fr, 0, f, 0, st(Blocks.SPRUCE_PLANKS));
        // a lantern on a post by the door
        this.fset(level, box, fr, 2, f + 1, -1, st(Blocks.SPRUCE_FENCE));
        this.fset(level, box, fr, 2, f + 2, -1, st(Blocks.LANTERN));
        // inside: the bed, a chest, the workstation, a fur rug, a lantern from the ridge
        BlockState bed = (index % 2 == 0 ? Blocks.BROWN_BED : Blocks.RED_BED).defaultBlockState().setValue(BedBlock.FACING, back);
        this.fset(level, box, fr, -2, f + 1, depth - 2, bed.setValue(BedBlock.PART, BedPart.HEAD));
        this.fset(level, box, fr, -2, f + 1, depth - 3, bed.setValue(BedBlock.PART, BedPart.FOOT));
        this.chest(level, box, fr, 2, f + 1, depth - 2, left);
        BlockState ws = WORKSTATIONS[index % WORKSTATIONS.length].defaultBlockState();
        if (ws.hasProperty(HorizontalDirectionalBlock.FACING)) ws = ws.setValue(HorizontalDirectionalBlock.FACING, back);
        this.fset(level, box, fr, -2, f + 1, 1, ws);
        this.fset(level, box, fr, 2, f + 1, 1, st(Blocks.CRAFTING_TABLE));
        for (int u = -1; u <= 1; u++) {
            for (int v = 3; v <= 4; v++) this.fset(level, box, fr, u, f + 1, v, st(Blocks.BROWN_CARPET));
        }
        this.fset(level, box, fr, 0, f + 5, 4, hanging());
        // the woodpile against the outside of the wall
        for (int v = 4; v <= 6; v++) {
            for (int y = f + 1; y <= f + 2; y++) this.fset(level, box, fr, half + 1, y, v, log(Blocks.SPRUCE_LOG, front.getAxis()));
        }
    }

    // ---------------------------------------------------------------- the saunas

    /**
     * A dry sauna: a little log cabin, no window but a slit; inside, the stove (lit, heaped with stones) by the door,
     * two tiers of benches along the back, a cauldron of water for the löyly.
     */
    void sauna(WorldGenLevel level, BoundingBox box, Frame fr, int f) {
        Direction front = fr.front(), back = front.getOpposite(), right = fr.right(), left = right.getOpposite();
        int half = 3, depth = 7;
        BlockState post = log(Blocks.SPRUCE_LOG, Direction.Axis.Y);
        BlockState along = log(Blocks.STRIPPED_SPRUCE_LOG, front.getAxis()), across = log(Blocks.STRIPPED_SPRUCE_LOG, right.getAxis());
        for (int u = -half; u <= half; u++) {
            for (int v = 0; v < depth; v++) {
                boolean side = Math.abs(u) == half, end = v == 0 || v == depth - 1;
                boolean edge = side || end;
                for (int y = f - 3; y < f; y++) this.fset(level, box, fr, u, y, v, st(Blocks.COBBLESTONE));
                this.fset(level, box, fr, u, f, v, edge ? st(Blocks.COBBLESTONE) : st(Blocks.SPRUCE_PLANKS));
                for (int dy = 1; dy <= 3; dy++) {
                    BlockState s = st(Blocks.AIR);
                    if (edge) {
                        s = side && end ? post : side ? along : across;
                        if (dy == 2 && v == depth - 1 && u == -1) s = st(Blocks.GLASS_PANE);
                    }
                    this.fset(level, box, fr, u, f + dy, v, s);
                }
            }
        }
        this.roof(level, box, fr, f + 3, half + 1, -1, depth, Blocks.SPRUCE_STAIRS, Blocks.SPRUCE_SLAB);
        this.gables(level, box, fr, f + 3, half, 0, depth - 1);
        // a low ceiling inside keeps the heat in
        for (int u = -(half - 1); u <= half - 1; u++) {
            for (int v = 1; v < depth - 1; v++) this.fset(level, box, fr, u, f + 4, v, st(Blocks.SPRUCE_PLANKS));
        }
        BlockState door = st(Blocks.SPRUCE_DOOR).setValue(DoorBlock.FACING, front).setValue(DoorBlock.HINGE, DoorHingeSide.RIGHT);
        this.fset(level, box, fr, 0, f + 1, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.fset(level, box, fr, 0, f + 2, 0, door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        this.fset(level, box, fr, 0, f, 0, st(Blocks.SPRUCE_PLANKS));
        // the stove by the door, burning; the benches, the lower step and the upper tier, along the back
        this.fset(level, box, fr, -2, f + 1, 2, st(ModBlocks.SAUNA_STOVE.get()).setValue(SaunaStoveBlock.LIT, true));
        this.fset(level, box, fr, -2, f + 1, 1, st(Blocks.COBBLESTONE_WALL));
        for (int u = -2; u <= 2; u++) {
            this.fset(level, box, fr, u, f + 1, 4, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
            this.fset(level, box, fr, u, f + 1, 5, st(Blocks.SPRUCE_PLANKS));
            this.fset(level, box, fr, u, f + 2, 5, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        }
        this.fset(level, box, fr, 2, f + 1, 1, st(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
        // outside: a bench by the door to cool off on, and a log to sit on
        this.fset(level, box, fr, -2, f + 1, -1, stair(Blocks.SPRUCE_STAIRS, back, false));
        this.fset(level, box, fr, -1, f + 1, -1, stair(Blocks.SPRUCE_STAIRS, back, false));
        this.fset(level, box, fr, half + 1, f + 1, 3, log(Blocks.SPRUCE_LOG, front.getAxis()));
        this.fset(level, box, fr, half + 1, f + 1, 4, log(Blocks.SPRUCE_LOG, front.getAxis()));
        this.fset(level, box, fr, half + 1, f + 2, 3, log(Blocks.SPRUCE_LOG, front.getAxis()));
    }

    // ---------------------------------------------------------------- before and behind the longhouse

    /** Before the longhouse: the fire pit ringed with stones, the bell on its post, and the rune stone. */
    void square(WorldGenLevel level, BoundingBox box, int f) {
        int px = 0, pz = -15;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                this.lset(level, box, px + dx, f, pz + dz, dx == 0 && dz == 0 ? st(Blocks.COBBLESTONE) : st(Blocks.MOSSY_COBBLESTONE));
                if (dx != 0 || dz != 0) this.lset(level, box, px + dx, f + 1, pz + dz, slab(Blocks.COBBLESTONE_SLAB, SlabType.BOTTOM));
            }
        }
        this.lset(level, box, px, f + 1, pz, st(Blocks.CAMPFIRE));
        // the bell, on a post of its own
        this.lset(level, box, 4, f + 1, -15, st(Blocks.BELL).setValue(BellBlock.FACING, this.rd(Direction.NORTH))
                .setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR));
        // the rune stone, between the saunas
        for (int y = f + 1; y <= f + 3; y++) this.lset(level, box, 0, y, -21, y == f + 3 ? st(Blocks.CHISELED_STONE_BRICKS) : st(Blocks.CRACKED_STONE_BRICKS));
        this.lset(level, box, 0, f + 2, -21, st(Blocks.CHISELED_STONE_BRICKS));
        this.lset(level, box, 0, f + 4, -21, slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
    }

    /** Behind the longhouse: a pen of spruce fencing, a gate in its front, hay inside. */
    void pen(WorldGenLevel level, BoundingBox box, int f) {
        int x0 = -5, x1 = 5, z0 = 16, z1 = 22;
        for (int lx = x0; lx <= x1; lx++) {
            for (int lz = z0; lz <= z1; lz++) {
                boolean rim = lx == x0 || lx == x1 || lz == z0 || lz == z1;
                if (!rim) continue;
                BlockState s = st(Blocks.SPRUCE_FENCE);
                if (lz == z0 && lx == 0) {
                    s = st(Blocks.SPRUCE_FENCE_GATE).setValue(FenceGateBlock.FACING, this.rd(Direction.NORTH));
                }
                this.lset(level, box, lx, f + 1, lz, s);
            }
        }
        this.lset(level, box, x0 + 1, f + 1, z1 - 1, st(Blocks.HAY_BLOCK));
        this.lset(level, box, x0 + 2, f + 1, z1 - 1, st(Blocks.HAY_BLOCK));
        this.lset(level, box, x0 + 1, f + 2, z1 - 1, st(Blocks.HAY_BLOCK));
        this.lset(level, box, x1 - 1, f + 1, z1 - 1, st(Blocks.WATER_CAULDRON).setValue(LayeredCauldronBlock.LEVEL, 3));
    }

    /** A little field of wheat behind the huts, a channel of water down its middle. */
    void field(WorldGenLevel level, BoundingBox box, int f, int side) {
        int xa = side * 9, xb = side * 16;
        for (int lx = Math.min(xa, xb); lx <= Math.max(xa, xb); lx++) {
            for (int lz = 17; lz <= 21; lz++) {
                if (lz == 19) {
                    this.lset(level, box, lx, f, lz, st(Blocks.WATER));
                    continue;
                }
                this.lset(level, box, lx, f, lz, st(Blocks.FARMLAND).setValue(net.minecraft.world.level.block.FarmBlock.MOISTURE, 7));
                this.lset(level, box, lx, f + 1, lz, st(Blocks.WHEAT).setValue(CropBlock.AGE, this.roll(lx, lz, 9, 8)));
            }
        }
        this.lset(level, box, xb + side, f + 1, 17, st(Blocks.COMPOSTER));
    }

    void chest(WorldGenLevel level, BoundingBox box, Frame fr, int u, int y, int v, Direction facing) {
        BlockPos pos = this.at(fr.x(u, v), y, fr.z(u, v));
        if (!box.isInside(pos)) return;
        level.setBlock(pos, st(Blocks.CHEST).setValue(ChestBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) chest.setLootTable(HOUSE_LOOT, this.seed ^ pos.asLong());
    }

    // ---------------------------------------------------------------- the people

    void life(WorldGenLevel level, BoundingBox box, int f) {
        for (int[] h : HUTS) {
            Frame fr = this.frame(h[0], h[1], h[0] < 0 ? Direction.EAST : Direction.WEST);
            this.villager(level, this.at(fr.x(0, 3), f + 1, fr.z(0, 3)), box);
        }
        Frame hall = this.frame(0, -11, Direction.NORTH);
        this.villager(level, this.at(hall.x(1, 6), f + 1, hall.z(1, 6)), box);
        this.villager(level, this.at(hall.x(-1, 17), f + 1, hall.z(-1, 17)), box);
        this.mob(level, EntityType.IRON_GOLEM, this.at(this.rx(-4, -16), f + 1, this.rz(-4, -16)), box);
        int[][] inPen = {{-3, 18}, {0, 19}, {2, 18}, {3, 20}, {-1, 20}};
        for (int i = 0; i < inPen.length; i++) {
            EntityType<?> type = i < 3 ? ModEntities.REINDEER.get() : EntityType.SHEEP;
            this.mob(level, type, this.at(this.rx(inPen[i][0], inPen[i][1]), f + 1, this.rz(inPen[i][0], inPen[i][1])), box);
        }
        this.mob(level, EntityType.CAT, this.at(this.rx(7, -5), f + 1, this.rz(7, -5)), box);
    }

    void villager(WorldGenLevel level, BlockPos pos, BoundingBox box) {
        if (!box.isInside(pos) || !level.getBlockState(pos).isAir() && !level.getBlockState(pos).is(Blocks.BROWN_CARPET)) return;
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) return;
        villager.moveTo(pos.getX() + 0.5, pos.getY() + (level.getBlockState(pos).isAir() ? 0.0 : 0.0625), pos.getZ() + 0.5,
                level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillageRegistry.VIKING.get()).setProfession(VillagerProfession.NONE));
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
