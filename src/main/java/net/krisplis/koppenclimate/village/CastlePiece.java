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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The Highland Castle of the Scottish highlands (Cfc): a grey stone castle in the Scottish baronial manner, held by
 * the woodland mansions' illagers and haunted by swarms of tiny ghasts. Turned one of four ways at random.
 * <ul>
 *   <li>a curtain wall round a courtyard, battlemented, with a wall-walk; a round tower at each corner and two
 *       flanking the gatehouse, all under tall conical slate roofs;</li>
 *   <li>the great hall along the back: one vast space from its floor to the rafters of its steep slate roof, tall
 *       windows in two tiers, a dais with the high table at one end and a great hearth at the other, tables down the
 *       sides and the middle left clear, chandeliers on long chains from the beams, banners between the windows;
 *       lower ranges down both sides; in the courtyard the keep, five storeys high, a corbelled turret with its own
 *       cone at each corner of its battlements; archery butts, a smithy's corner, dummies, woodpiles about it; the
 *       castle's banners - the saltire, a clan's plaid, gold - over the gate, on its towers and round the keep;</li>
 *   <li>below the great hall, the dungeon: a vaulted passage between cells (the illagers keep their allays there),
 *       and at its far end a wall of cracked stone - behind which, sealed in, is the chest with the Sapphire ring
 *       (chests/highland_castle_vault);</li>
 *   <li>inside: floors and ladders up through every range and tower, lanterns hung everywhere (nothing spawns in it
 *       once it's cleared), the woodland mansions' chests about the rooms;</li>
 *   <li>its garrison: vindicators, evokers and their vexes; and tiny ghasts drifting over the courtyard (more keep
 *       coming while anyone is in the castle: see event/CastleGhasts).</li>
 * </ul>
 */
public class CastlePiece extends StructurePiece {
    static final ResourceKey<LootTable> MANSION_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.withDefaultNamespace("chests/woodland_mansion"));
    static final ResourceKey<LootTable> VAULT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chests/highland_castle_vault"));

    // the curtain wall's outer faces, its height
    static final int WX = 28, WZ = 24, WALL = 12;
    // the round towers: x, z, radius, wall top, cone top - the four corners, the tall tower in the middle of the back,
    // one in the middle of each side, the two flanking the gate
    static final int[][] TOWERS = {{-WX, -WZ, 5, 24, 36}, {WX, -WZ, 5, 24, 36}, {-WX, WZ, 5, 24, 36}, {WX, WZ, 5, 24, 36},
            {0, -WZ, 4, 36, 50}, {-WX, 0, 4, 18, 28}, {WX, 0, 4, 18, 28}, {-6, WZ, 4, 18, 27}, {6, WZ, 4, 18, 27}};
    // the ranges: x0, z0, x1, z1, storeys (of 6), ridge along x?
    static final int[][] RANGES = {{-26, -22, 26, -9, 3, 1}, {-26, -8, -17, 18, 2, 0}, {17, -8, 26, 18, 2, 0}};
    // the great hall: the first range, open from its floor to its roof
    // the keep
    static final int KX = 7, KZ0 = -4, KZ1 = 8, KEEP_TOP = 30;
    static final int STOREY = 6;
    // the dungeon, under the great hall: its floor, and the vault at its west end behind the cracked wall
    static final int DUNGEON = -6, VAULT_WALL = -17;
    static final int CLOSE = 44, MARGIN = 14, HEIGHT = 52;

    final BlockPos origin;
    final long seed;
    final int turn;

    public CastlePiece(BlockPos origin, long seed, int turn) {
        super(VillageRegistry.CASTLE_PIECE.get(), 0, box(origin, turn));
        this.origin = origin;
        this.seed = seed;
        this.turn = turn;
    }

    public CastlePiece(CompoundTag tag) {
        super(VillageRegistry.CASTLE_PIECE.get(), tag);
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
        int r = CLOSE + MARGIN;
        return new BoundingBox(origin.getX() - r, origin.getY() + DUNGEON - 4, origin.getZ() - r, origin.getX() + r,
                origin.getY() + HEIGHT, origin.getZ() + r);
    }

    public BlockPos origin() {
        return this.origin;
    }

    // ================================================================ placing blocks (in the castle's own frame)

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

    static BlockState log(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, axis);
    }

    static BlockState pane(Direction.Axis along) {
        boolean x = along == Direction.Axis.X;
        return Blocks.GLASS_PANE.defaultBlockState().setValue(CrossCollisionBlock.EAST, x).setValue(CrossCollisionBlock.WEST, x)
                .setValue(CrossCollisionBlock.NORTH, !x).setValue(CrossCollisionBlock.SOUTH, !x);
    }

    static final BlockState AIR = Blocks.AIR.defaultBlockState();
    static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);

    // the castle's grey stone: stone bricks, mossy and cracked here and there, a little cobble and andesite
    BlockState stone(int x, int y, int z) {
        int r = this.roll(x, y, z, 20);
        return r < 2 ? st(Blocks.MOSSY_STONE_BRICKS) : r < 4 ? st(Blocks.CRACKED_STONE_BRICKS) : r == 4 ? st(Blocks.ANDESITE)
                : r == 5 ? st(Blocks.COBBLESTONE) : st(Blocks.STONE_BRICKS);
    }

    // ================================================================ the plan, cell by cell

    /** A round tower's part at this cell, or null. */
    BlockState tower(int[] t, int x, int y, int z) {
        double d = Mth.length(x - t[0], z - t[1]);
        int r = t[2], top = t[3], cone = t[4];
        if (y <= top) {
            if (d > r + 0.5) return null;
            boolean shell = d > r - 0.7;
            if (y == 0) return st(Blocks.STONE_BRICKS);
            if (shell) {
                // arrow slits and windows, every storey
                if (Math.floorMod(y, STOREY) >= 2 && Math.floorMod(y, STOREY) <= 3 && Math.floorMod(Math.round(Math.atan2(z - t[1], x - t[0]) * 8 / Math.PI), 2) == 0
                        && Math.abs(x - t[0]) + Math.abs(z - t[1]) > r) {
                    return pane(Math.abs(x - t[0]) > Math.abs(z - t[1]) ? Direction.Axis.Z : Direction.Axis.X);
                }
                return this.stone(x, y, z);
            }
            // floors every storey (a hole for the ladder against the inner side), the ladder
            if (x == t[0] && z == ladderZ(t)) return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, t[1] <= 0 ? Direction.NORTH : Direction.SOUTH);
            if (Math.floorMod(y, STOREY) == 0 || y == top) return st(Blocks.SPRUCE_PLANKS);
            return AIR;
        }
        // the wall goes on two more blocks round the top room (room to climb off the ladder), then the cone
        if (y <= top + 2) {
            if (d > r + 0.5) return null;
            if (d > r - 0.7) return y == top + 1 && Math.floorMod(Math.round(Math.atan2(z - t[1], x - t[0]) * 4 / Math.PI), 2) == 0
                    && Math.abs(x - t[0]) + Math.abs(z - t[1]) > r ? pane(Math.abs(x - t[0]) > Math.abs(z - t[1]) ? Direction.Axis.Z : Direction.Axis.X)
                    : this.stone(x, y, z);
            return AIR;
        }
        float rc = (r + 1.2F) * (cone + 1 - y) / (cone - 1 - top);
        if (y <= cone && d <= rc) return d > rc - 1.3F ? st(Blocks.DEEPSLATE_TILES) : AIR;
        if (y == cone + 1 && d < 0.5) return st(Blocks.LIGHTNING_ROD);
        return null;
    }

    // a tower's ladder runs up its side nearest the courtyard (the wall it hangs on: one block further)
    static int ladderZ(int[] t) {
        return t[1] + (t[1] <= 0 ? 1 : -1) * (t[2] - 1);
    }

    /** A range's part at this cell, or null: walls with windows, floors, a steep gabled roof. */
    BlockState range(int[] g, int x, int y, int z) {
        int x0 = g[0], z0 = g[1], x1 = g[2], z1 = g[3], storeys = g[4];
        boolean hall = g == RANGES[0];
        boolean alongX = g[5] == 1;
        int top = storeys * STOREY;
        // the roof
        int across0 = alongX ? z0 : x0, across1 = alongX ? z1 : x1, a = alongX ? z : x, l = alongX ? x : z;
        int l0 = alongX ? x0 : z0, l1 = alongX ? x1 : z1;
        if (l < l0 || l > l1 || a < across0 - 1 || a > across1 + 1) return null;
        int roof = top + Math.min(a - (across0 - 1), across1 + 1 - a);
        boolean inside = a >= across0 && a <= across1;
        if (y > top) {
            if (y > roof) return null;
            Direction up = alongX ? (a - across0 < across1 - a ? Direction.SOUTH : Direction.NORTH)
                    : (a - across0 < across1 - a ? Direction.EAST : Direction.WEST);
            int d0 = a - (across0 - 1), d1 = across1 + 1 - a;
            if (y == roof) return d0 == d1 ? st(Blocks.DEEPSLATE_TILES) : stair(Blocks.DEEPSLATE_TILE_STAIRS, up, false);
            if (!inside) return null;
            if (l == l0 || l == l1) return this.stone(x, y, z);                       // the gables
            // the great hall is open to its roof, crossed by dark oak beams; elsewhere, lanterns down the loft
            if (hall) {
                if (y == top + 1 && Math.floorMod(l - l0, 6) == 3 && y < roof - 1) return log(Blocks.STRIPPED_DARK_OAK_LOG, alongX ? Direction.Axis.Z : Direction.Axis.X);
                return y == roof - 1 ? st(Blocks.DEEPSLATE_TILES) : AIR;
            }
            if (y == top + 1 && a == (across0 + across1) / 2 && Math.floorMod(l - l0, 7) == 3) return st(Blocks.LANTERN);
            return y == roof - 1 ? st(Blocks.DEEPSLATE_TILES) : AIR;
        }
        if (!inside) return null;
        boolean wall = l == l0 || l == l1 || a == across0 || a == across1;
        int k = Math.floorMod(y, STOREY);
        // a ladder up through its floors, against its outer long wall near one end
        int outer = Math.abs(across0) > Math.abs(across1) ? across0 : across1, ladderA = outer + (outer == across0 ? 1 : -1);
        if (!hall && a == ladderA && l == l0 + 3 && y > 0 && y < top) {
            Direction facing = alongX ? (outer == across0 ? Direction.SOUTH : Direction.NORTH) : (outer == across0 ? Direction.EAST : Direction.WEST);
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
        }
        if (hall && !wall && y > 0) {
            // the beams across under the roof (the open truss), nothing else: one space from floor to rafters
            if (y == top && Math.floorMod(l - l0, 6) == 3) return log(Blocks.STRIPPED_DARK_OAK_LOG, alongX ? Direction.Axis.Z : Direction.Axis.X);
            return AIR;
        }
        if (k == 0 && !(hall && wall && y > 0 && y < top)) return wall ? this.stone(x, y, z) : st(y == 0 ? Blocks.POLISHED_ANDESITE : Blocks.SPRUCE_PLANKS);
        if (hall && wall && y > 0) {
            int inner = outer == across0 ? across1 : across0;
            if (a == inner && Math.abs(l - (l0 + l1) / 2) <= 1 && y <= 4) return AIR;                       // the great door
            boolean longSide = a == across0 || a == across1;
            // tall windows in pairs, two tiers, a stone transom between
            int m = Math.floorMod(l - l0, 6);
            if (longSide && (m == 2 || m == 4) && l > l0 + 1 && l < l1 - 1 && (y >= 3 && y <= 8 || y >= 10 && y <= 15)) {
                return pane(alongX ? Direction.Axis.X : Direction.Axis.Z);
            }
            return this.stone(x, y, z);
        }
        if (k == 0) return wall ? this.stone(x, y, z) : st(y == 0 ? Blocks.STONE_BRICKS : Blocks.SPRUCE_PLANKS);
        if (wall) {
            // the door from the courtyard, in the middle of its courtyard side
            int inner = outer == across0 ? across1 : across0;
            if (a == inner && Math.abs(l - (l0 + l1) / 2) <= 1 && y <= 3) return AIR;
            boolean longSide = a == across0 || a == across1;
            if (longSide && k >= 2 && k <= 4 && Math.floorMod(l, 4) == 1 && l > l0 + 1 && l < l1 - 1) {
                return pane(alongX ? Direction.Axis.X : Direction.Axis.Z);
            }
            return this.stone(x, y, z);
        }
        // partitions across the range every ten blocks, a doorway in each
        if (Math.floorMod(l - l0, 10) == 0 && l != l0) {
            int mid = (across0 + across1) / 2;
            return Math.abs(a - mid) <= 0 && k <= 3 || a == mid + 1 && k <= 3 ? AIR : st(Blocks.SPRUCE_PLANKS);
        }
        return AIR;
    }

    /** The keep's part at this cell: five storeys, battlements, a solid corbelled turret with a cone at each corner. */
    BlockState keep(int x, int y, int z) {
        int ax = Math.abs(x);
        boolean footprint = ax <= KX && z >= KZ0 && z <= KZ1;
        for (int tz : new int[]{KZ0, KZ1}) {
            double d = Mth.length(ax - KX, z - tz);
            if (d > 2.5 || y < (footprint ? KEEP_TOP + 1 : KEEP_TOP - 3)) continue;
            int cap = KEEP_TOP + 4;
            if (y == KEEP_TOP - 3) return d > 1.5 ? stair(Blocks.STONE_BRICK_STAIRS, ax > KX ? (x > 0 ? Direction.WEST : Direction.EAST)
                    : z < tz ? Direction.SOUTH : Direction.NORTH, true) : this.stone(x, y, z);
            if (y <= cap) return this.stone(x, y, z);
            float rc = 3.4F * (cap + 9 - y) / 9.0F;
            if (y < cap + 9 && d <= rc) return st(Blocks.DEEPSLATE_TILES);
            if (y == cap + 9 && d < 0.5) return st(Blocks.LIGHTNING_ROD);
            return null;
        }
        if (!footprint) return null;
        boolean wall = ax == KX || z == KZ0 || z == KZ1;
        boolean ladder = x == 0 && z == KZ0 + 1;
        if (y > KEEP_TOP) {
            if (y == KEEP_TOP + 1 && wall) return (x + z & 1) == 0 ? this.stone(x, y, z) : null;
            return null;
        }
        if (ladder && y > 0) return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
        int k = Math.floorMod(y, STOREY);
        if (k == 0 || y == KEEP_TOP) {
            if (wall) return this.stone(x, y, z);
            return y == 0 ? st(Blocks.POLISHED_ANDESITE) : y == KEEP_TOP ? st(Blocks.STONE_BRICKS) : st(Blocks.DARK_OAK_PLANKS);
        }
        if (wall) {
            if (z == KZ1 && ax <= 1 && y <= 3) return AIR;                                // the door
            if (k >= 2 && k <= 4 && (ax == 3 || ax == 4) && (z == KZ0 || z == KZ1)) return pane(Direction.Axis.X);
            if (k >= 2 && k <= 4 && ax == KX && (z == KZ0 + 3 || z == KZ0 + 4 || z == KZ0 + 8 || z == KZ0 + 9)) return pane(Direction.Axis.Z);
            return this.stone(x, y, z);
        }
        return AIR;
    }

    /** The curtain wall: two thick, battlemented, a wall-walk along its top; the gate through its south side. */
    BlockState curtain(int x, int y, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        if (ax > WX || az > WZ) return null;
        boolean band = ax >= WX - 1 || az >= WZ - 1;
        if (!band) return null;
        boolean outer = ax == WX || az == WZ;
        if (z >= WZ - 1 && ax <= 2 && y >= 1 && y <= (ax <= 1 ? 6 : 5)) return AIR;            // the gate
        if (y <= WALL) return y == 0 ? st(Blocks.STONE_BRICKS) : this.stone(x, y, z);
        if (y == WALL + 1 && outer) return (x + z & 1) == 0 ? this.stone(x, y, z) : null;    // merlons
        return null;
    }

    BlockState design(int x, int y, int z) {
        if (DOORS.contains(key(x, y, z))) return AIR;
        return designShell(x, y, z);
    }

    static long key(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    // the doorways through the towers' walls, worked out once (the design is the same for every castle)
    static final java.util.Set<Long> DOORS = new java.util.HashSet<>();

    static {
        CastlePiece shape = new CastlePiece();
        for (int[] t : TOWERS) {
            int r = t[2];
            for (int y0 = 0; y0 < t[3]; y0 += STOREY) {
                for (int x = t[0] - r; x <= t[0] + r; x++) {
                    for (int z = t[1] - r; z <= t[1] + r; z++) {
                        double d = Mth.length(x - t[0], z - t[1]);
                        if (d <= r - 0.7 || d > r + 0.5 || x == t[0] && z == ladderZ(t) + (t[1] <= 0 ? 1 : -1)) continue;
                        // step outward along the nearest of the eight directions, if it's within a few degrees of it
                        double ang = Math.atan2(z - t[1], x - t[0]);
                        long oct = Math.round(ang / (Math.PI / 4));
                        if (Math.abs(ang - oct * Math.PI / 4) > Math.toRadians(17)) continue;
                        int dx = (int) Math.round(Math.cos(oct * Math.PI / 4)), dz = (int) Math.round(Math.sin(oct * Math.PI / 4));
                        int ox = x + dx, oz = z + dz, ix = x - dx, iz = z - dz;
                        if (Mth.length(ox - t[0], oz - t[1]) <= r + 0.5) continue;             // still in the tower's wall
                        if (shape.solid(ox, y0, oz) && shape.open(ox, y0 + 1, oz) && shape.open(ox, y0 + 2, oz)
                                && shape.open(ix, y0 + 1, iz) && shape.open(ix, y0 + 2, iz)) {
                            DOORS.add(key(x, y0 + 1, z));
                            DOORS.add(key(x, y0 + 2, z));
                        }
                    }
                }
            }
        }
    }

    // a shape-only castle, for working out the doors
    private CastlePiece() {
        super(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType.JIGSAW, 0, new BoundingBox(0, 0, 0, 0, 0, 0));
        this.origin = BlockPos.ZERO;
        this.seed = 0;
        this.turn = 0;
    }

    // something to stand on: a floor, a wall-walk, the courtyard's paving
    boolean solid(int x, int y, int z) {
        BlockState s = this.designShell(x, y, z);
        if (s == null) return y == 0 && Math.abs(x) < WX - 1 && Math.abs(z) < WZ - 1;
        return !s.isAir() && !s.is(Blocks.LADDER) && !s.is(Blocks.GLASS_PANE) && !s.is(Blocks.LIGHTNING_ROD);
    }

    boolean open(int x, int y, int z) {
        BlockState s = this.designShell(x, y, z);
        return s == null || s.isAir() || s.is(Blocks.LADDER);
    }

    BlockState designShell(int x, int y, int z) {
        for (int[] t : TOWERS) {
            BlockState s = this.tower(t, x, y, z);
            if (s != null) return s;
        }
        BlockState k = this.keep(x, y, z);
        if (k != null) return k;
        for (int[] g : RANGES) {
            BlockState s = this.range(g, x, y, z);
            if (s != null) return s;
        }
        return this.curtain(x, y, z);
    }

    static boolean inTower(int x, int z) {
        for (int[] t : TOWERS) if (Mth.length(x - t[0], z - t[1]) <= t[2] + 0.5) return true;
        return false;
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        REACH.set(box.inflatedBy(15, 0, 15));
        this.shapeGround(level, box);
        int ox = this.origin.getX(), oz = this.origin.getZ();
        LooseTreeCleanup.clean(level, chunkPos, this.origin.getY() + DUNGEON, this.origin.getY() + HEIGHT,
                (wx, wz) -> Math.max(Math.abs(wx - ox), Math.abs(wz - oz)) < CLOSE + MARGIN + 6,
                (wx, wz) -> Math.max(Math.abs(wx - ox), Math.abs(wz - oz)) <= CLOSE);
        this.courtyard(level, box);
        this.buildCastle(level, box);
        this.dungeon(level, box);
        this.furnish(level, box);
        this.garrison(level, box);
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** The castle's rock: levelled at its floor, the ground about it blended back into the hills. */
    void shapeGround(WorldGenLevel level, BoundingBox box) {
        int f = this.origin.getY();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int dx = Math.max(Math.abs(wx - this.origin.getX()) - CLOSE, 0), dz = Math.max(Math.abs(wz - this.origin.getZ()) - CLOSE, 0);
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist >= MARGIN) continue;
                int ground = f - 30;
                for (int y = f + 40; y > f - 30; y--) {
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
                int[] l = this.local(wx, wz);
                level.setBlock(pos.set(wx, target, wz), this.roll(l[0], 0, l[1], 14) == 0 ? st(Blocks.COARSE_DIRT) : st(Blocks.GRASS_BLOCK), 2);
            }
        }
    }

    /** The courtyard inside the walls: cobbles and flags, a well, a path from the gate to the keep's door. */
    void courtyard(WorldGenLevel level, BoundingBox box) {
        for (int x = -WX + 2; x <= WX - 2; x++) {
            for (int z = -WZ + 2; z <= WZ + 6; z++) {
                boolean path = Math.abs(x) <= 1 && z > KZ1;
                int r = this.roll(x, 1, z, 10);
                if (z > WZ - 2 && Math.abs(x) > 2) continue;
                this.set(level, box, x, 0, z, path ? st(Blocks.STONE_BRICKS) : r < 4 ? st(Blocks.COBBLESTONE) : r < 6 ? st(Blocks.GRAVEL)
                        : r < 8 ? st(Blocks.ANDESITE) : st(Blocks.MOSSY_COBBLESTONE));
            }
        }
        // the well
        int wx = -13, wz = 7;
        for (int x = wx - 1; x <= wx + 1; x++) {
            for (int z = wz - 1; z <= wz + 1; z++) {
                boolean rim = x != wx || z != wz;
                this.set(level, box, x, 1, z, rim ? st(Blocks.COBBLESTONE_WALL) : AIR);
                if (!rim) for (int y = 0; y >= -3; y--) this.set(level, box, x, y, z, y > -3 ? st(Blocks.WATER) : st(Blocks.COBBLESTONE));
            }
        }
    }

    void buildCastle(WorldGenLevel level, BoundingBox box) {
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int[] l = this.local(wx, wz);
                int x = l[0], z = l[1];
                if (Math.abs(x) > WX + 7 || Math.abs(z) > WZ + 7) continue;
                boolean any = false;
                for (int y = 0; y <= HEIGHT; y++) {
                    BlockState s = this.design(x, y, z);
                    if (s == null) continue;
                    any = true;
                    BlockPos pos = new BlockPos(wx, this.origin.getY() + y, wz);
                    if (box.isInside(pos)) level.setBlock(pos, s.rotate(ROTATIONS[this.turn]), 2);
                }
                if (any) {
                    for (int y = -1; y >= DUNGEON - 2; y--) {
                        BlockPos pos = new BlockPos(wx, this.origin.getY() + y, wz);
                        if (box.isInside(pos) && !level.getBlockState(pos).blocksMotion()) level.setBlock(pos, st(Blocks.STONE), 2);
                    }
                }
            }
        }
    }

    /**
     * The dungeon under the great hall: steps down from the hall, a vaulted passage, cells either side behind iron
     * bars - and at the passage's west end a wall of cracked stone. Behind it, sealed in, the vault.
     */
    void dungeon(WorldGenLevel level, BoundingBox box) {
        int x0 = -23, x1 = 23, z0 = -21, z1 = -13, f = DUNGEON;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = f - 1; y <= -1; y++) {
                    boolean shell = x == x0 || x == x1 || z == z0 || z == z1 || y == f - 1;
                    BlockState s = shell ? this.stone(x, y, z) : AIR;
                    if (!shell && y == f) s = this.roll(x, y, z, 4) == 0 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE);
                    // the cells either side of the passage (z -18 to -16), behind iron bars
                    if (!shell && y > f && (z == -19 || z == -15) && x > VAULT_WALL && x < x1 - 1) {
                        s = Math.floorMod(x, 5) == 0 ? this.stone(x, y, z) : st(Blocks.IRON_BARS);
                    }
                    if (!shell && y > f && (z < -19 || z > -15) && Math.floorMod(x, 5) == 0 && x > VAULT_WALL) s = this.stone(x, y, z);
                    // the cracked wall, and the vault sealed behind it
                    if (!shell && x == VAULT_WALL) s = y == f ? this.stone(x, y, z) : st(Blocks.CRACKED_STONE_BRICKS);
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        // the steps down from the hall, along its back wall
        for (int k = 1; k <= 6; k++) {
            int z = -18;
            this.set(level, box, 5 + k, -k, z, stair(Blocks.STONE_BRICK_STAIRS, Direction.WEST, false));
            for (int y = -k + 1; y <= 1; y++) this.set(level, box, 5 + k, y, z, AIR);
        }
        // the vault: the chest, gold, a lantern
        BlockPos chest = this.at(x0 + 2, f + 1, -17);
        if (box.isInside(chest)) {
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST).rotate(ROTATIONS[this.turn]), 2);
            if (level.getBlockEntity(chest) instanceof ChestBlockEntity entity) entity.setLootTable(VAULT, this.seed ^ chest.asLong());
        }
        this.set(level, box, x0 + 1, f + 1, -19, st(Blocks.GOLD_BLOCK));
        this.set(level, box, x0 + 1, f + 1, -15, st(Blocks.GOLD_BLOCK));
        this.set(level, box, x0 + 3, f + 3, -17, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        this.set(level, box, x0 + 3, f + 4, -17, st(Blocks.CHAIN));
        // the passage's lanterns
        for (int x = VAULT_WALL + 3; x < x1; x += 5) {
            this.set(level, box, x, -1, -17, LANTERN);
            this.set(level, box, x + 1, f + 1, -20, LANTERN.setValue(LanternBlock.HANGING, false));
            this.set(level, box, x + 1, f + 1, -14, LANTERN.setValue(LanternBlock.HANGING, false));
        }
    }

    /** Lanterns in every room and tower, the hall's furniture, the chests. */
    void furnish(WorldGenLevel level, BoundingBox box) {
        for (int x = -WX + 1; x <= WX - 1; x++) {
            for (int z = -WZ + 1; z <= WZ - 1; z++) {
                if (Math.floorMod(x + 1, 5) != 0 || Math.floorMod(z + 1, 5) != 0) continue;
                for (int y = 5; y <= KEEP_TOP + 6; y += STOREY) {
                    if (this.freeAt(level, x, y, z) && !this.get(level, x, y + 1, z).isAir() && !this.get(level, x, y + 1, z).is(Blocks.LADDER)) {
                        this.set(level, box, x, y, z, LANTERN);
                    }
                }
            }
        }
        for (int[] t : TOWERS) {
            for (int y = 5; y < t[3]; y += STOREY) {
                if (this.freeAt(level, t[0], y, t[1])) this.set(level, box, t[0], y, t[1], LANTERN);
            }
            if (this.freeAt(level, t[0], t[3] + 1, t[1])) this.set(level, box, t[0], t[3] + 1, t[1], st(Blocks.LANTERN));
        }
        // lanterns on the battlements, in the courtyard
        for (int x = -WX + 2; x <= WX - 2; x += 6) {
            this.set(level, box, x, WALL + 2, -WZ, st(Blocks.LANTERN));
            this.set(level, box, x, WALL + 2, WZ, st(Blocks.LANTERN));
        }
        for (int z = -WZ + 6; z <= WZ - 6; z += 6) {
            this.set(level, box, -WX, WALL + 2, z, st(Blocks.LANTERN));
            this.set(level, box, WX, WALL + 2, z, st(Blocks.LANTERN));
        }
        for (int[] p : new int[][]{{-12, -2}, {12, -2}, {-12, 14}, {12, 14}, {-4, 18}, {-4, 12}, {4, 18}, {4, 12}, {-21, 20}, {21, 20}, {-12, -7}, {12, -7}}) {
            for (int y = 1; y <= 3; y++) this.set(level, box, p[0], y, p[1], st(Blocks.SPRUCE_FENCE));
            this.set(level, box, p[0], 4, p[1], st(Blocks.LANTERN));
        }
        this.greatHall(level, box);
        for (int[] c : new int[][]{{-20, 1, -11}, {20, 1, -11}, {-24, 7, 15}, {24, 7, -5}, {0, 25, -26}, {-21, 1, 0}, {21, 7, 10},
                {0, 7, 0}, {-4, 19, -2}}) {
            this.chest(level, box, c[0], c[1], c[2]);
        }
        this.banners(level, box);
        this.courtyardLife(level, box);
        // the keep: the lord's chamber at the top, a library under it
        for (int x = -5; x <= 5; x += 2) {
            for (int y = 13; y <= 14; y++) this.set(level, box, x, y, KZ0 + 1, st(Blocks.BOOKSHELF));
        }
        this.set(level, box, -3, 25, KZ0 + 2, Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD));
        this.set(level, box, -3, 25, KZ0 + 3, Blocks.RED_BED.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.FACING, Direction.NORTH)
                .setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT));
    }

    /**
     * The great hall: one space from the floor to the rafters, forty-odd blocks long. At its west end the dais and
     * the high table across it, the lord's chair in the middle, banners on the gable behind; down both long sides,
     * tables lit with candles and benches either side, leaving the middle of the hall clear for whatever's to be
     * held there - a red runner down it from the dais to the great hearth in the east gable; chandeliers hanging
     * from the beams, standing lanterns round the walls.
     */
    void greatHall(WorldGenLevel level, BoundingBox box) {
        int z0 = RANGES[0][1] + 1, z1 = RANGES[0][3] - 1, x0 = RANGES[0][0] + 1, x1 = RANGES[0][2] - 1;
        // the dais, a step up, its edge stepped (clear of the corner tower's door)
        for (int z = z0 + 2; z <= z1; z++) {
            for (int x = x0; x <= x0 + 4; x++) this.set(level, box, x, 1, z, st(Blocks.POLISHED_ANDESITE));
            this.set(level, box, x0 + 5, 1, z, stair(Blocks.POLISHED_ANDESITE_STAIRS, Direction.WEST, false));
        }
        // the high table across it, the chairs behind, the lord's in the middle
        for (int z = -18; z <= -13; z++) {
            this.set(level, box, x0 + 2, 2, z, slab(Blocks.DARK_OAK_SLAB, SlabType.TOP));
            if (z != -16 && z != -15) this.set(level, box, x0 + 1, 2, z, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
            if (Math.floorMod(z, 2) == 0) this.set(level, box, x0 + 2, 3, z, candles(3));
        }
        for (int z = -16; z <= -15; z++) {
            this.set(level, box, x0 + 1, 2, z, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
            this.set(level, box, x0, 2, z, st(Blocks.GOLD_BLOCK));
        }
        for (int z = z0 + 2; z <= z1; z++) if (this.freeAt(level, x0 + 3, 2, z)) this.set(level, box, x0 + 3, 2, z, st(Blocks.RED_CARPET));
        // the long tables down each side, benches either side, candles
        for (int x = x0 + 8; x <= x1 - 8; x++) {
            boolean backRow = !(x >= 4 && x <= 13), frontRow = Math.abs(x) > 3;
            if (backRow) {
                this.set(level, box, x, 1, z0 + 2, slab(Blocks.DARK_OAK_SLAB, SlabType.TOP));
                this.set(level, box, x, 1, z0 + 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
                this.set(level, box, x, 1, z0 + 3, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
                if (Math.floorMod(x, 3) == 0) this.set(level, box, x, 2, z0 + 2, candles(Math.floorMod(x, 2) + 2));
            }
            if (frontRow) {
                this.set(level, box, x, 1, z1 - 2, slab(Blocks.DARK_OAK_SLAB, SlabType.TOP));
                this.set(level, box, x, 1, z1 - 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
                this.set(level, box, x, 1, z1 - 3, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
                if (Math.floorMod(x, 3) == 0) this.set(level, box, x, 2, z1 - 2, candles(Math.floorMod(x, 2) + 2));
            }
        }
        // a rail round the stairs down to the dungeon
        for (int x = 6; x <= 12; x++) this.set(level, box, x, 1, -17, st(Blocks.DARK_OAK_FENCE));
        this.set(level, box, 12, 1, -18, st(Blocks.DARK_OAK_FENCE));
        // the runner down the middle, from the dais to the hearth
        for (int x = x0 + 6; x <= x1 - 1; x++) {
            for (int z = -16; z <= -15; z++) if (this.freeAt(level, x, 1, z)) this.set(level, box, x, 1, z, st(Blocks.RED_CARPET));
        }
        // the great hearth in the east gable: a wide opening, three fires, a blackstone surround
        for (int z = -18; z <= -13; z++) {
            for (int y = 1; y <= 4; y++) {
                boolean opening = z >= -17 && z <= -14 && y <= 3;
                this.set(level, box, x1 + 1, y, z, opening ? AIR : st(Blocks.POLISHED_BLACKSTONE_BRICKS));
            }
            if (z >= -16 && z <= -15 || z == -14) this.set(level, box, x1 + 1, 1, z, Blocks.CAMPFIRE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CampfireBlock.FACING, Direction.WEST));
        }
        // chandeliers from the beams: a long chain, a lantern at its foot and one halfway
        int beamY = RANGES[0][4] * STOREY;
        for (int x = x0 + 2; x <= x1; x++) {
            if (Math.floorMod(x - RANGES[0][0], 6) != 3) continue;
            for (int z : new int[]{-17, -14}) {
                for (int y = beamY - 1; y >= 9; y--) this.set(level, box, x, y, z, st(Blocks.CHAIN));
                this.set(level, box, x, 8, z, LANTERN);
                this.set(level, box, x, 13, z, LANTERN.setValue(LanternBlock.HANGING, true));
            }
        }
        // standing lanterns round the walls, between the windows
        for (int x = x0 + 6; x <= x1 - 4; x += 4) {
            for (int z : new int[]{z0, z1}) if (this.freeAt(level, x, 1, z)) this.set(level, box, x, 1, z, st(Blocks.LANTERN));
        }
        for (int z = z0 + 3; z <= z1; z += 3) if (this.freeAt(level, x1, 1, z)) this.set(level, box, x1, 1, z, st(Blocks.LANTERN));
    }

    static BlockState candles(int n) {
        return Blocks.CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.LIT, true)
                .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, n);
    }

    static BlockState slab(Block b, SlabType type) {
        return b.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    /**
     * The castle's banners: the blue and white saltire, a clan's green plaid, and gold with a red border and
     * flower - in the great hall between its windows and behind the dais, over the gate, on the gate towers, round
     * the keep, along the courtyard side of the curtain wall.
     */
    void banners(WorldGenLevel level, BoundingBox box) {
        int z0 = RANGES[0][1] + 1, z1 = RANGES[0][3] - 1;
        int i = 0;
        for (int x = RANGES[0][0] + 1; x <= RANGES[0][2] - 1; x++) {
            if (Math.floorMod(x - RANGES[0][0], 6) != 0) continue;
            this.banner(level, box, x, 13, z0, Direction.SOUTH, i++);
            this.banner(level, box, x, 13, z1, Direction.NORTH, i++);
        }
        for (int z = -19; z <= -12; z += 7) this.banner(level, box, RANGES[0][0] + 1, 6, z, Direction.EAST, 0);
        this.banner(level, box, RANGES[0][0] + 1, 7, -16, Direction.EAST, 2);
        this.banner(level, box, RANGES[0][0] + 1, 7, -15, Direction.EAST, 2);
        // over the gate, on the gate towers' fronts, round the keep
        this.banner(level, box, 0, 9, WZ + 1, Direction.SOUTH, 0);
        for (int side = -1; side <= 1; side += 2) {
            this.banner(level, box, side * 6, 11, WZ + 5, Direction.SOUTH, 1);
            this.banner(level, box, side * (KX + 1), 22, (KZ0 + KZ1) / 2, side > 0 ? Direction.EAST : Direction.WEST, 0);
        }
        this.banner(level, box, 0, 22, KZ1 + 1, Direction.SOUTH, 2);
        this.banner(level, box, 0, 22, KZ0 - 1, Direction.NORTH, 2);
        for (int x : new int[]{-20, -12, 12, 20}) this.banner(level, box, x, 9, WZ - 2, Direction.NORTH, Math.abs(x) == 12 ? 0 : 1);
    }

    /** A wall banner of one of the castle's three designs, if the wall behind is there (and not a window). */
    void banner(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, int design) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.below()).isAir()) return;
        BlockState behind = this.get(level, x - facing.getStepX(), y, z - facing.getStepZ());
        if (!behind.blocksMotion() || behind.is(Blocks.GLASS_PANE)) return;
        net.minecraft.world.item.DyeColor base = switch (Math.floorMod(design, 3)) {
            case 0 -> net.minecraft.world.item.DyeColor.BLUE;
            case 1 -> net.minecraft.world.item.DyeColor.GREEN;
            default -> net.minecraft.world.item.DyeColor.YELLOW;
        };
        Block block = switch (base) {
            case BLUE -> Blocks.BLUE_WALL_BANNER;
            case GREEN -> Blocks.GREEN_WALL_BANNER;
            default -> Blocks.YELLOW_WALL_BANNER;
        };
        level.setBlock(pos, block.defaultBlockState().setValue(net.minecraft.world.level.block.WallBannerBlock.FACING, facing)
                .rotate(ROTATIONS[this.turn]), 2);
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BannerBlockEntity banner)) return;
        if (level.registryAccess() == null) return;
        var patterns = level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        var layers = new net.minecraft.world.level.block.entity.BannerPatternLayers.Builder();
        switch (Math.floorMod(design, 3)) {
            case 0 -> layers.addIfRegistered(patterns, net.minecraft.world.level.block.entity.BannerPatterns.CROSS, net.minecraft.world.item.DyeColor.WHITE);
            case 1 -> layers.addIfRegistered(patterns, net.minecraft.world.level.block.entity.BannerPatterns.STRIPE_SMALL, net.minecraft.world.item.DyeColor.BLUE)
                    .addIfRegistered(patterns, net.minecraft.world.level.block.entity.BannerPatterns.STRIPE_CENTER, net.minecraft.world.item.DyeColor.RED)
                    .addIfRegistered(patterns, net.minecraft.world.level.block.entity.BannerPatterns.STRIPE_MIDDLE, net.minecraft.world.item.DyeColor.BLACK)
                    .addIfRegistered(patterns, net.minecraft.world.level.block.entity.BannerPatterns.BORDER, net.minecraft.world.item.DyeColor.GREEN);
            default -> layers.addIfRegistered(patterns, net.minecraft.world.level.block.entity.BannerPatterns.FLOWER, net.minecraft.world.item.DyeColor.RED)
                    .addIfRegistered(patterns, net.minecraft.world.level.block.entity.BannerPatterns.BORDER, net.minecraft.world.item.DyeColor.RED);
        }
        net.minecraft.world.item.ItemStack item = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHITE_BANNER);
        item.set(net.minecraft.core.component.DataComponents.BANNER_PATTERNS, layers.build());
        banner.fromItem(item, base);
    }

    /**
     * The courtyard's life: archery butts against the south wall, hay and barrels by the gate, a smithy's corner
     * (anvil, grindstone, a blast furnace, a quenching trough), a woodpile, training dummies, benches.
     */
    void courtyardLife(WorldGenLevel level, BoundingBox box) {
        for (int x : new int[]{-21, -18, -15}) {
            this.set(level, box, x, 1, WZ - 2, st(Blocks.HAY_BLOCK));
            this.set(level, box, x, 2, WZ - 2, st(Blocks.TARGET));
        }
        for (int x = 9; x <= 11; x++) {
            this.set(level, box, x, 1, WZ - 2, st(Blocks.HAY_BLOCK));
            if (x < 11) this.set(level, box, x, 2, WZ - 2, st(Blocks.HAY_BLOCK));
        }
        for (int x = 13; x <= 15; x++) this.set(level, box, x, 1, WZ - 2, st(Blocks.BARREL));
        this.set(level, box, 14, 2, WZ - 2, st(Blocks.BARREL));
        // the smithy's corner
        this.set(level, box, 12, 1, 7, st(Blocks.ANVIL));
        this.set(level, box, 13, 1, 7, st(Blocks.GRINDSTONE));
        this.set(level, box, 14, 1, 7, st(Blocks.BLAST_FURNACE));
        this.set(level, box, 15, 1, 7, Blocks.WATER_CAULDRON.defaultBlockState().setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        this.set(level, box, 15, 1, 9, st(Blocks.SMITHING_TABLE));
        // the woodpile against the hall's front
        for (int x = -15; x <= -12; x++) {
            for (int y = 1; y <= 2; y++) this.set(level, box, x, y, RANGES[0][3] + 1, log(Blocks.SPRUCE_LOG, Direction.Axis.X));
        }
        // training dummies: a post, a bale for a body, a carved pumpkin for a head
        for (int x : new int[]{-16, -10}) {
            this.set(level, box, x, 1, 18, st(Blocks.SPRUCE_FENCE));
            this.set(level, box, x, 2, 18, st(Blocks.HAY_BLOCK));
            this.set(level, box, x, 3, 18, Blocks.CARVED_PUMPKIN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING, Direction.NORTH));
        }
        // benches along the keep
        for (int z = KZ0 + 1; z <= KZ1 - 1; z += 3) {
            this.set(level, box, -KX - 1, 1, z, stair(Blocks.SPRUCE_STAIRS, Direction.EAST, false));
            this.set(level, box, KX + 1, 1, z, stair(Blocks.SPRUCE_STAIRS, Direction.WEST, false));
        }
    }

    boolean freeAt(WorldGenLevel level, int x, int y, int z) {
        return this.get(level, x, y, z).isAir();
    }

    void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos) || !level.getBlockState(pos).isAir()) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().rotate(ROTATIONS[this.turn]), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity entity) entity.setLootTable(MANSION_LOOT, this.seed ^ pos.asLong());
    }

    // ---------------------------------------------------------------- its garrison

    void garrison(WorldGenLevel level, BoundingBox box) {
        // the allays kept in the dungeon's cells
        this.mob(level, box, EntityType.ALLAY, -12, DUNGEON + 1, -20);
        this.mob(level, box, EntityType.ALLAY, 3, DUNGEON + 1, -14);
        this.mob(level, box, EntityType.ALLAY, 13, DUNGEON + 1, -20);
        int[][] vindicators = {{-15, 1, -17}, {5, 1, -16}, {15, 1, -15}, {-10, 1, -13}, {12, 1, -13}, {0, 1, -15}, {-21, 1, 5}, {21, 1, 5},
                {-21, 7, 12}, {21, 7, -4}, {0, 1, 0}, {3, 7, -1}, {-3, 13, 2}, {0, WALL + 1, WZ - 1}, {-20, WALL + 1, -WZ + 1},
                {-28, 7, -24}, {28, 7, 24}, {6, 7, 24}};
        for (int[] v : vindicators) this.mob(level, box, EntityType.VINDICATOR, v[0], v[1], v[2]);
        int[][] evokers = {{-22, 2, -15}, {0, 19, 0}, {-21, 7, 2}, {21, 7, 2}};
        for (int[] e : evokers) this.mob(level, box, EntityType.EVOKER, e[0], e[1], e[2]);
        this.mob(level, box, EntityType.VEX, 0, 3, -2);
        this.mob(level, box, EntityType.VEX, -10, 9, -16);
        this.mob(level, box, EntityType.VEX, 10, 15, -16);
        // the tiny ghasts over the courtyard
        for (int i = 0; i < 8; i++) {
            int x = -14 + this.roll(i, 1, 0, 29), z = 4 + this.roll(i, 2, 0, 16), y = 8 + this.roll(i, 3, 0, 10);
            this.mob(level, box, ModEntities.TINY_GHAST.get(), x, y, z);
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
}
