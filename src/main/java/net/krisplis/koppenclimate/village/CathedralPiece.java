package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
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
 * The Gothic Cathedral of the temperate forest (Cfb): a scaled-down Cologne Cathedral, built all of deepslate and
 * blackstone, its windows of smoky grey glass - gloomy inside, abandoned, haunted.
 * <ul>
 *   <li>the west front: two great towers, square up to 38 blocks, an open octagonal belfry above (bells still
 *       hanging) and an openwork spire to 67 blocks, with pinnacles at their corners; between them the main portal
 *       and the great west window under a gable;</li>
 *   <li>the nave: a tall vessel (vaulted at 23) between pointed arcades, a side aisle either way, a clerestory of
 *       lancet windows over the aisles' roofs; buttresses along the walls, flying buttresses arching from their
 *       pinnacles to the nave, and a steep roof of deepslate tiles over all;</li>
 *   <li>the transept crossing it, its two arms ending in gabled walls with a portal and a great window, and a
 *       slender spire (flèche) over the crossing;</li>
 *   <li>the choir, and the apse rounding it off at the east end: the ambulatory going round behind the altar.</li>
 * </ul>
 * Inside, cobwebs hang under the vaults and soul lanterns burn down the nave, the aisles and the transept; the pews have been torn out and the floor of
 * the nave bears the same pentagram as the ghost towns' churches, candles on its points and a jukebox in its
 * middle - and on the altar a chest with the one record (CursedCrusades: play it at night...). Right under the
 * altar, sealed in a little crypt, lies another chest: the Ruby ring's (chests/cathedral_crypt). Round the cathedral,
 * its close with a graveyard.
 *
 * <p>In the cathedral's own frame x runs from the west front (0) east to the apse, z across (the north side
 * negative), y up from its floor; the whole is turned one of four ways.
 */
public class CathedralPiece extends StructurePiece {
    static final ResourceKey<LootTable> ALTAR = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chests/cathedral_altar"));
    static final ResourceKey<LootTable> CRYPT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chests/cathedral_crypt"));

    // the plan
    static final int TOWER_TOP = 38, OCT_TOP = 47, SPIRE_TOP = 64;
    static final int NAVE_X0 = 9, TRANS_X0 = 30, TRANS_X1 = 38, TRANS_Z = 18, APSE_X = 50, WALL = 24;
    public static final int PENTAGRAM_X = 19, ALTAR_X = 46;
    // the crypt's chest (the Ruby ring's), right under the altar
    public static final int CRYPT_Y = -4;
    // the close round the cathedral (levelled), and the blend back into the forest beyond it
    static final int CLOSE_X0 = -14, CLOSE_X1 = 66, CLOSE_Z = 26, MARGIN = 12;
    static final int HEIGHT = 70;
    /** How far the building reaches each way in its own frame (for the bounding box and the jukebox check). */
    public static final int X0 = CLOSE_X0 - MARGIN, X1 = CLOSE_X1 + MARGIN, Z = CLOSE_Z + MARGIN;

    final BlockPos origin;
    final long seed;
    final int turn;

    public CathedralPiece(BlockPos origin, long seed, int turn) {
        super(VillageRegistry.CATHEDRAL_PIECE.get(), 0, box(origin, turn));
        this.origin = origin;
        this.seed = seed;
        this.turn = turn;
    }

    public CathedralPiece(CompoundTag tag) {
        super(VillageRegistry.CATHEDRAL_PIECE.get(), tag);
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
        int ax = rx(turn, X0, -Z), az = rz(turn, X0, -Z), bx = rx(turn, X1, Z), bz = rz(turn, X1, Z);
        return new BoundingBox(origin.getX() + Math.min(ax, bx), origin.getY() - 8, origin.getZ() + Math.min(az, bz),
                origin.getX() + Math.max(ax, bx), origin.getY() + HEIGHT, origin.getZ() + Math.max(az, bz));
    }

    // ================================================================ the cathedral's own frame, turned

    static int rx(int turn, int lx, int lz) {
        return switch (turn) {
            case 1 -> -lz;
            case 2 -> -lx;
            case 3 -> lz;
            default -> lx;
        };
    }

    static int rz(int turn, int lx, int lz) {
        return switch (turn) {
            case 1 -> lx;
            case 2 -> -lz;
            case 3 -> -lx;
            default -> lz;
        };
    }

    static final Rotation[] ROTATIONS = {Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90};

    BlockPos at(int lx, int y, int lz) {
        return new BlockPos(this.origin.getX() + rx(this.turn, lx, lz), this.origin.getY() + y, this.origin.getZ() + rz(this.turn, lx, lz));
    }

    /** The world position in the cathedral's frame: x, z (y is just the height above its floor). */
    int[] local(int wx, int wz) {
        int dx = wx - this.origin.getX(), dz = wz - this.origin.getZ();
        return switch (this.turn) {
            case 1 -> new int[]{dz, -dx};
            case 2 -> new int[]{-dx, -dz};
            case 3 -> new int[]{-dz, dx};
            default -> new int[]{dx, dz};
        };
    }

    /** Whether a world position lies in the cathedral itself (its walls and everything in them). */
    public boolean inCathedral(BlockPos pos) {
        int[] l = this.local(pos.getX(), pos.getZ());
        int y = pos.getY() - this.origin.getY();
        return y >= -4 && y <= HEIGHT && footprint(l[0], l[1]);
    }

    public BlockPos origin() {
        return this.origin;
    }

    /** A point in the cathedral's frame, in the world. */
    public BlockPos world(int lx, int y, int lz) {
        return this.at(lx, y, lz);
    }

    void set(WorldGenLevel level, BoundingBox box, int lx, int y, int lz, BlockState state) {
        BlockPos pos = this.at(lx, y, lz);
        if (box.isInside(pos)) level.setBlock(pos, state.rotate(ROTATIONS[this.turn]), 2);
    }

    /**
     * The block at a spot in the structure's frame - read only near the chunk being built (the chunk and the ones
     * round it, all the world lets a structure see while it's being generated); anything further off reads as solid
     * stone, so nothing is put anywhere on the strength of it.
     */
    BlockState get(WorldGenLevel level, int lx, int y, int lz) {
        BlockPos pos = this.at(lx, y, lz);
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

    // the stone of the cathedral: deepslate bricks, now and then cracked
    BlockState wall(int x, int y, int z) {
        int r = this.roll(x, y, z, 13);
        return r == 0 ? st(Blocks.CRACKED_DEEPSLATE_BRICKS) : r == 1 ? st(Blocks.DEEPSLATE_TILES) : st(Blocks.DEEPSLATE_BRICKS);
    }

    // ================================================================ the plan, cell by cell

    static boolean inTowers(int x, int z) {
        return x >= 0 && x <= 8 && Math.abs(z) >= 5 && Math.abs(z) <= 13;
    }

    /** Whether the column is inside the cathedral's walls (its floor), buttresses not counted. */
    static boolean footprint(int x, int z) {
        int az = Math.abs(z);
        if (x >= 0 && x <= 8 && az <= 13) return true;
        if (x >= NAVE_X0 && x <= APSE_X && az <= 10) return true;
        if (x >= TRANS_X0 && x <= TRANS_X1 && az <= TRANS_Z) return true;
        return x > APSE_X && Mth.length(x - APSE_X, z) <= 10.5;
    }

    /** The pointed vault over the central vessel, by how far from its middle. */
    static int vault(int off) {
        return off <= 1 ? 23 : off == 2 ? 22 : off == 3 ? 21 : 20;
    }

    static boolean pierLine(int x) {
        return x >= NAVE_X0 && x <= 29 && (x - NAVE_X0) % 4 == 0 || x >= 39 && x <= 47 && (x - 39) % 4 == 0;
    }

    /** How far from the middle of its bay a nave or choir column is (0 or 1), the piers being at 2. */
    static int bayOff(int x) {
        if (pierLine(x)) return 2;
        int p = x <= 29 ? NAVE_X0 + Math.floorDiv(x - NAVE_X0, 4) * 4 : 39 + Math.floorDiv(x - 39, 4) * 4;
        return Math.abs(x - (p + 2));
    }

    // the steep roofs: the height of the roof's surface over a column of the nave (by z), the transept (by x)
    static int naveRoof(int az) {
        return 25 + 2 * (5 - az);
    }

    static int transeptRoof(int x) {
        return 25 + 2 * (4 - Math.abs(x - 34));
    }

    // the aisles' lean-to roofs, from the outer wall (12) up against the nave (16)
    static int aisleRoof(int az) {
        return 12 + (10 - az);
    }

    static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /**
     * What the cathedral has at a cell of its frame: a block, air (inside it, or opened through its walls), or null
     * where it has nothing (the close, the sky).
     */
    BlockState design(int x, int y, int z) {
        int az = Math.abs(z);
        Direction toMiddle = z > 0 ? Direction.NORTH : Direction.SOUTH;
        // ---- the west towers (and their buttresses)
        if (x >= -1 && x <= 9 && az >= 4 && az <= 14) {
            BlockState t = this.tower(x, y, az);
            if (t != null || inTowers(x, z) || x == -1 || az == 14) return t;
        }
        if (y == 0) return footprint(x, z) ? this.floor(x, z) : null;
        if (y < 0) return null;
        // ---- the west front between the towers
        if (x >= 0 && x <= 1 && az <= 4) {
            // the main portal, its jambs stepping in
            if (az <= 2 && y <= (x == 0 ? 8 : 7) - az) return AIR;
            // the great west window over it
            int top = az <= 1 ? 24 : az == 2 ? 22 : 20;
            if (az <= 3 && y >= 11 && y <= top) {
                if (x == 1) return AIR;
                return z == 0 || y == 17 ? st(Blocks.BLACK_STAINED_GLASS) : st(Blocks.GRAY_STAINED_GLASS);
            }
            if (az <= 2 && y >= 6 && y <= 9 && x == 1) return st(Blocks.CHISELED_DEEPSLATE);
            if (y <= WALL) return this.wall(x, y, z);
            // the gable, up to the roof's line, a pinnacle on its point
            if (y <= naveRoof(az)) return this.wall(x, y, z);
            if (z == 0 && y <= naveRoof(0) + 3) return y == naveRoof(0) + 3 ? st(Blocks.LIGHTNING_ROD) : st(Blocks.DEEPSLATE_BRICK_WALL);
            return null;
        }
        // ---- the transept and the crossing (and the flèche over it)
        if (x >= TRANS_X0 && x <= TRANS_X1 && az <= TRANS_Z) {
            BlockState t = this.transept(x, y, z, az);
            if (t != null) return t;
        }
        // ---- the apse at the east end
        if (x > APSE_X) return this.apse(x, y, z);
        // ---- the nave and the choir (and the hall behind the west front, under the same vault)
        if (x < 2 || x > APSE_X || az > 12) return null;
        if (x <= 8 && az > 4) return null;
        int off = bayOff(x);
        boolean pier = off == 2;
        if (az <= 4) {
            if (y <= vault(az)) return AIR;
            if (y <= WALL) return pier && y == vault(az) + 1 ? st(Blocks.POLISHED_DEEPSLATE) : st(Blocks.DEEPSLATE_TILES);
            return this.roof(y, naveRoof(az), toMiddle);
        }
        if (az == 5) {
            if (pier) return y <= 22 ? st(Blocks.POLISHED_DEEPSLATE) : y <= WALL ? this.wall(x, y, z) : this.roof(y, naveRoof(5), toMiddle);
            // the arcade's pointed arches, the blind triforium, the clerestory windows
            if (y <= 9 - off) return AIR;
            if (y >= 11 && y <= 12 && off == 0) return st(Blocks.POLISHED_BLACKSTONE_BRICKS);
            if (y >= 17 && y <= 22 - off) return off == 0 && y == 19 ? st(Blocks.BLACK_STAINED_GLASS) : st(Blocks.GRAY_STAINED_GLASS);
            if (y <= WALL) return this.wall(x, y, z);
            return this.roof(y, naveRoof(5), toMiddle);
        }
        if (az <= 9) {
            // the aisles
            if (y <= 10) return AIR;
            if (y == 11) return st(Blocks.DEEPSLATE_TILES);
            BlockState flyer = this.flyer(x, y, az, pier);
            if (flyer != null) return flyer;
            return this.roof(y, aisleRoof(az), toMiddle);
        }
        if (az == 10) {
            if (!pier && y >= 3 && y <= 9 - off) return off == 0 && y == 6 ? st(Blocks.BLACK_STAINED_GLASS) : st(Blocks.GRAY_STAINED_GLASS);
            if (y <= 11) return this.wall(x, y, z);
            BlockState flyer = this.flyer(x, y, az, pier);
            if (flyer != null) return flyer;
            return this.roof(y, aisleRoof(10), toMiddle);
        }
        // the buttresses, at each pier: a pinnacle on the outer one
        if (!pier || x < NAVE_X0) return null;
        if (az == 11) {
            if (y <= 18) return this.wall(x, y, z);
            if (y <= 20) return st(Blocks.DEEPSLATE_BRICK_WALL);
            return y == 21 ? st(Blocks.LIGHTNING_ROD) : null;
        }
        return y <= 9 ? this.wall(x, y, z) : y == 10 ? stair(Blocks.DEEPSLATE_BRICK_STAIRS, toMiddle, false) : null;
    }

    /** The roof over a column: its tiles' stair on top, solid under it (down to the vault). */
    BlockState roof(int y, int top, Direction toMiddle) {
        if (y < top) return st(Blocks.DEEPSLATE_TILES);
        if (y == top) return stair(Blocks.DEEPSLATE_TILE_STAIRS, toMiddle, false);
        return null;
    }

    /** The flying buttresses, from the outer pinnacles up across the aisle roofs to the nave's wall. */
    BlockState flyer(int x, int y, int az, boolean pier) {
        if (!pier || x < NAVE_X0 || az < 6 || az > 10) return null;
        int fy = 17 + Math.round((10 - az) * 0.75F);
        if (y == fy) return st(Blocks.DEEPSLATE_BRICKS);
        if (y == fy - 1 && az <= 9) return st(Blocks.POLISHED_DEEPSLATE);
        return null;
    }

    /** The floor: polished deepslate and tiles, in a chequer. */
    BlockState floor(int x, int z) {
        if (this.roll(x, 0, z, 17) == 0) return st(Blocks.CRACKED_DEEPSLATE_TILES);
        return (x + z & 1) == 0 ? st(Blocks.POLISHED_DEEPSLATE) : st(Blocks.DEEPSLATE_TILES);
    }

    /** A west tower (az from 4 to 14 covers the two, mirrored): tx across the front, tz out from the middle. */
    BlockState tower(int x, int y, int az) {
        int tx = x, tz = az - 5;
        boolean inside = tx >= 0 && tx <= 8 && tz >= 0 && tz <= 8;
        // the buttresses at the corners, on the west and the outer faces, stepping in as they rise
        if (!inside) {
            boolean west = x == -1 && (tz <= 1 || tz >= 7) && tz >= 0 && tz <= 8;
            boolean outer = tz == 9 && (tx <= 1 || tx >= 7) && tx >= 0;
            if ((west || outer) && y >= 0) {
                int h = (west ? (tz == 0 || tz == 8) : (tx == 0 || tx == 8)) ? 32 : 26;
                if (y <= h) return this.wall(x, y, az);
                if (y == h + 1) return st(Blocks.DEEPSLATE_BRICK_WALL);
            }
            return null;
        }
        if (y < 0) return null;
        if (y == 0) return this.floor(x, az);
        boolean shell = tx == 0 || tx == 8 || tz == 0 || tz == 8;
        if (y <= TOWER_TOP) {
            if (!shell) {
                // the floors of its storeys (each with a hole for the bell ropes), the bell under the top one
                if ((y == 12 || y == 24 || y == TOWER_TOP) && !(tx == 4 && tz == 4)) return st(Blocks.DEEPSLATE_TILES);
                if (y == TOWER_TOP - 1 && tx == 4 && tz == 4) {
                    return Blocks.BELL.defaultBlockState().setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING);
                }
                return AIR;
            }
            // its faces: portals and windows in the middle three columns
            int c = (tx == 0 || tx == 8) ? Math.abs(tz - 4) : Math.abs(tx - 4);
            boolean west = tx == 0, east = tx == 8, inner = tz == 0, outer = tz == 8;
            if (c <= 1) {
                if (west && y <= 6 - c) return AIR;
                if ((west || outer) && y >= 14 && y <= 21 - c) return c == 0 && y == 17 ? st(Blocks.BLACK_STAINED_GLASS) : st(Blocks.GRAY_STAINED_GLASS);
                if (y >= 27 && y <= 35 - c) return y == 27 ? st(Blocks.IRON_BARS) : AIR;
                if (inner && y <= 8 - c && (tx >= 3 && tx <= 5)) return AIR;
            }
            // into the aisle (east face, over the aisle's width) and into the hall between the towers
            if (east && tz >= 1 && tz <= 4 && y <= 8) return AIR;
            return this.wall(x, y, az);
        }
        // above: a parapet, a pinnacle at each corner, and inside it the octagonal belfry and the spire
        boolean corner = (tx == 0 || tx == 8) && (tz == 0 || tz == 8);
        if (corner && y <= TOWER_TOP + 6) return y == TOWER_TOP + 6 ? st(Blocks.LIGHTNING_ROD) : st(Blocks.DEEPSLATE_BRICK_WALL);
        if (shell) return y == TOWER_TOP + 1 && (tx + tz) % 2 == 0 ? st(Blocks.DEEPSLATE_BRICK_WALL) : null;
        int dx = Math.abs(tx - 4), dz = Math.abs(tz - 4);
        if (y <= OCT_TOP) {
            boolean in = dx <= 3 && dz <= 3 && dx + dz <= 5;
            if (!in) return null;
            boolean ring = !(dx <= 2 && dz <= 2 && dx + dz <= 3);
            if (!ring) return y == OCT_TOP ? st(Blocks.DEEPSLATE_TILES) : AIR;
            if (y >= 41 && y <= 46 && (dx == 0 || dz == 0)) return y == 41 ? st(Blocks.IRON_BARS) : AIR;
            return this.wall(x, y, az);
        }
        // the openwork spire
        if (y <= SPIRE_TOP) {
            float r = 3.6F * (SPIRE_TOP + 1 - y) / (SPIRE_TOP + 1 - OCT_TOP);
            float m = Math.max(Math.max(dx, dz), (dx + dz) * 0.7071F);
            if (m > r) return null;
            if (m > r - 1.2F && r > 1.5F && Math.floorMod(y + tx + 2 * tz, 3) == 0) return AIR;
            if (m > r - 1.2F || r <= 1.5F) return this.wall(x, y, az);
            return AIR;
        }
        if (dx == 0 && dz == 0) {
            if (y == SPIRE_TOP + 1) return st(Blocks.CHISELED_DEEPSLATE);
            if (y == SPIRE_TOP + 2) return st(Blocks.DEEPSLATE_BRICK_WALL);
            if (y == SPIRE_TOP + 3) return st(Blocks.LIGHTNING_ROD);
        }
        return null;
    }

    /** The transept and the crossing; null where the nave's own plan goes on (its aisles beside the crossing). */
    BlockState transept(int x, int y, int z, int az) {
        int ox = Math.abs(x - 34);
        boolean side = x == TRANS_X0 || x == TRANS_X1;
        Direction toRidge = x > 34 ? Direction.WEST : Direction.EAST;
        // the flèche over the crossing
        if (ox <= 2 && az <= 2 && y >= 34) {
            boolean shell = ox == 2 || az == 2;
            if (y <= 40) {
                if (!shell) return AIR;
                if (y >= 36 && y <= 39 && (ox == 0 || az == 0)) return AIR;
                return this.wall(x, y, z);
            }
            float r = 2.4F * (53 - y) / 12;
            float m = Math.max(ox, az);
            if (y <= 52 && m <= r) return m > r - 1.1F || r <= 1.2F ? this.wall(x, y, z) : AIR;
            if (ox == 0 && az == 0 && y <= 54) return y == 54 ? st(Blocks.LIGHTNING_ROD) : st(Blocks.DEEPSLATE_BRICK_WALL);
            if (y > 34) return null;
        }
        if (y == 0) return footprint(x, z) ? this.floor(x, z) : null;
        if (az == TRANS_Z) {
            // the end wall: a portal, the great window over it, the gable
            if (ox <= 1 && y <= 6 - ox) return AIR;
            if (ox <= 2 && y >= 9 && y <= 22 - ox) return ox == 0 || y == 15 ? st(Blocks.BLACK_STAINED_GLASS) : st(Blocks.GRAY_STAINED_GLASS);
            if (y <= transeptRoof(x)) return this.wall(x, y, z);
            if (ox == 0 && y <= transeptRoof(x) + 3) return y == transeptRoof(x) + 3 ? st(Blocks.LIGHTNING_ROD) : st(Blocks.DEEPSLATE_BRICK_WALL);
            return null;
        }
        if (side && az >= 11) {
            // the arms' side walls, a tall lancet in each
            if (Math.abs(az - 14) <= 1 && y >= 4 && y <= 21 - Math.abs(az - 14)) {
                return az == 14 && y == 12 ? st(Blocks.BLACK_STAINED_GLASS) : st(Blocks.GRAY_STAINED_GLASS);
            }
            if (y <= WALL) return this.wall(x, y, z);
            return this.roof(y, transeptRoof(x), toRidge);
        }
        if (side) {
            // the crossing's piers; arches into the aisles and across the nave
            if (az == 5 || az == 10) return y <= WALL ? st(Blocks.POLISHED_DEEPSLATE) : this.roof(y, transeptRoof(x), toRidge);
            if (az <= 4 ? y <= 20 : y <= 10) return AIR;
            if (y <= WALL) return this.wall(x, y, z);
            return this.roofOver(x, y, z, toRidge);
        }
        // inside: the transept's vault (where it crosses the nave, the higher of the two)
        int v = az <= 4 ? Math.max(vault(ox), vault(az)) : vault(ox);
        if (y <= v) return AIR;
        if (y <= WALL) return st(Blocks.DEEPSLATE_TILES);
        return this.roofOver(x, y, z, toRidge);
    }

    // where the transept's roof crosses the nave's: whichever is higher
    BlockState roofOver(int x, int y, int z, Direction toRidge) {
        int t = transeptRoof(x), az = Math.abs(z);
        if (az <= 5 && naveRoof(az) >= t) return this.roof(y, naveRoof(az), z > 0 ? Direction.NORTH : Direction.SOUTH);
        return this.roof(y, t, toRidge);
    }

    /** The apse: its arcade round the altar, the ambulatory, the outer wall with its chapels' windows, buttresses. */
    BlockState apse(int x, int y, int z) {
        double dx = x - APSE_X, r = Math.sqrt(dx * dx + z * z);
        double angle = Math.toDegrees(Math.atan2(z, dx));
        Direction toMiddle = Math.abs(dx) >= Math.abs(z) ? Direction.WEST : z > 0 ? Direction.NORTH : Direction.SOUTH;
        if (y == 0) return r <= 10.5 ? this.floor(x, z) : null;
        if (r <= 4.5) {
            if (y <= vault((int) Math.round(r))) return AIR;
            if (y <= WALL) return st(Blocks.DEEPSLATE_TILES);
            return this.roof(y, (int) Math.round(25 + 2 * (5 - r)), toMiddle);
        }
        if (r <= 5.5) {
            boolean pier = Math.abs(angle - Math.round(angle / 36.0) * 36.0) < 8.0;
            if (pier) return y <= 22 ? st(Blocks.POLISHED_DEEPSLATE) : y <= WALL ? this.wall(x, y, z) : this.roof(y, 25, toMiddle);
            if (y <= 8) return AIR;
            if (y >= 17 && y <= 22) return st(Blocks.GRAY_STAINED_GLASS);
            if (y <= WALL) return this.wall(x, y, z);
            return this.roof(y, 25, toMiddle);
        }
        boolean buttress = Math.abs(angle - Math.round(angle / 30.0) * 30.0) < 60.0 / r;
        if (r <= 9.5) {
            if (y <= 10) return AIR;
            if (y == 11) return st(Blocks.DEEPSLATE_TILES);
            if (buttress) {
                int fy = 17 + (int) Math.round((10 - r) * 0.75);
                if (y == fy) return st(Blocks.DEEPSLATE_BRICKS);
                if (y == fy - 1) return st(Blocks.POLISHED_DEEPSLATE);
            }
            return this.roof(y, (int) Math.round(12 + (10 - r)), toMiddle);
        }
        if (r <= 10.5) {
            if (!buttress && y >= 3 && y <= 9) return st(Blocks.GRAY_STAINED_GLASS);
            if (y <= 11) return this.wall(x, y, z);
            if (buttress && y == 17) return st(Blocks.DEEPSLATE_BRICKS);
            return this.roof(y, 12, toMiddle);
        }
        if (buttress && r <= 12.5) {
            if (r <= 11.5) {
                if (y <= 18) return this.wall(x, y, z);
                if (y <= 20) return st(Blocks.DEEPSLATE_BRICK_WALL);
                return y == 21 ? st(Blocks.LIGHTNING_ROD) : null;
            }
            return y <= 9 ? this.wall(x, y, z) : null;
        }
        return null;
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        REACH.set(box.inflatedBy(15, 0, 15));
        this.shapeGround(level, box);
        int ox = this.origin.getX(), oz = this.origin.getZ();
        LooseTreeCleanup.clean(level, chunkPos, this.origin.getY() - 8, this.origin.getY() + HEIGHT,
                (wx, wz) -> {
                    int[] l = this.local(wx, wz);
                    return closeDistance(l[0], l[1]) < MARGIN + 8;
                },
                (wx, wz) -> {
                    int[] l = this.local(wx, wz);
                    return closeDistance(l[0], l[1]) <= 0;
                });
        this.buildCathedral(level, box);
        this.furnish(level, box);
        this.graveyard(level, box);
    }

    /** How far a column is outside the close (0 or less inside it). */
    static double closeDistance(int x, int z) {
        double dx = Math.max(Math.max(CLOSE_X0 - x, x - CLOSE_X1), 0), dz = Math.max(Math.abs(z) - CLOSE_Z, 0);
        return Math.sqrt(dx * dx + dz * dz);
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** The close: levelled at the cathedral's floor, blended smoothly back into the forest floor round it. */
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
                        level.setBlock(pos, AIR, 2);
                    }
                }
                for (int y = ground + 1; y < target; y++) {
                    level.setBlock(pos.set(wx, y, wz), st(y < target - 3 ? Blocks.STONE : Blocks.DIRT), 2);
                }
                // the close's ground: grass gone patchy, podzol and bare earth (a gravel path to the west portal)
                int r = this.roll(l[0], 0, l[1], 12);
                BlockState top = r < 6 ? st(Blocks.GRASS_BLOCK) : r < 9 ? st(Blocks.PODZOL) : r < 11 ? st(Blocks.COARSE_DIRT) : st(Blocks.ROOTED_DIRT);
                if (l[0] < 0 && Math.abs(l[1]) <= 2 && dist <= 0) top = this.roll(l[0], 1, l[1], 4) == 0 ? st(Blocks.COBBLED_DEEPSLATE) : st(Blocks.GRAVEL);
                level.setBlock(pos.set(wx, target, wz), top, 2);
                if (target - 1 > ground) level.setBlock(pos.set(wx, target - 1, wz), st(Blocks.DIRT), 2);
            }
        }
    }

    void buildCathedral(WorldGenLevel level, BoundingBox box) {
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int[] l = this.local(wx, wz);
                int x = l[0], z = l[1];
                if (x < -2 || x > 64 || Math.abs(z) > TRANS_Z + 1) continue;
                boolean inside = footprint(x, z);
                for (int y = 0; y <= HEIGHT; y++) {
                    BlockState s = this.design(x, y, z);
                    if (s == null) continue;
                    if (s.isAir() && inside && y >= 2 && this.roll(x, y, z, cobwebOdds(x, y, z)) == 0) s = st(Blocks.COBWEB);
                    BlockPos pos = new BlockPos(wx, this.origin.getY() + y, wz);
                    if (box.isInside(pos)) level.setBlock(pos, s.rotate(ROTATIONS[this.turn]), 2);
                }
                // its foundations, down into the ground
                if (inside || this.design(x, 1, z) != null) {
                    for (int y = -1; y >= -4; y--) {
                        BlockPos pos = new BlockPos(wx, this.origin.getY() + y, wz);
                        if (box.isInside(pos) && !level.getBlockState(pos).blocksMotion()) level.setBlock(pos, st(Blocks.COBBLED_DEEPSLATE), 2);
                    }
                }
            }
        }
    }

    // cobwebs here and there: mostly high up under the vaults and in the aisles' corners
    static int cobwebOdds(int x, int y, int z) {
        int az = Math.abs(z);
        if (y >= 18) return 45;
        if (az >= 6 && az <= 9 && y >= 9) return 40;
        if (az == 9 || az == 6) return 120;
        return 600;
    }

    /** Each cell of the pentagram in the cathedral's frame: x, z and what's drawn there. */
    static List<int[]> pentagram() {
        List<int[]> out = new ArrayList<>();
        int half = GhostTownPiece.PENTAGRAM.length / 2;
        for (int row = 0; row < GhostTownPiece.PENTAGRAM.length; row++) {
            for (int col = 0; col < GhostTownPiece.PENTAGRAM[row].length(); col++) {
                char c = GhostTownPiece.PENTAGRAM[row].charAt(col);
                // its top points east, to the altar
                if (c != '.') out.add(new int[]{PENTAGRAM_X + half - row, col - half, c});
            }
        }
        return out;
    }

    void furnish(WorldGenLevel level, BoundingBox box) {
        // the pentagram, inlaid in the nave's floor: candles on its points, the jukebox in its middle
        for (int[] p : pentagram()) {
            char c = (char) p[2];
            if (c == 'R') {
                this.set(level, box, p[0], 1, p[1], st(Blocks.JUKEBOX));
                continue;
            }
            this.set(level, box, p[0], 0, p[1], st(Blocks.RED_NETHER_BRICKS));
            if (c == 'Y' && !this.get(level, p[0], 1, p[1]).blocksMotion()) {
                this.set(level, box, p[0], 1, p[1], Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 3));
            }
        }
        // the altar on its dais in the choir, the chest with the record on it, candles
        for (int x = ALTAR_X - 2; x <= ALTAR_X + 2; x++) {
            for (int z = -3; z <= 3; z++) {
                this.set(level, box, x, 1, z, st(Blocks.POLISHED_DEEPSLATE));
                if (Math.abs(x - ALTAR_X) <= 1 && Math.abs(z) <= 2) this.set(level, box, x, 2, z, st(Blocks.POLISHED_DEEPSLATE));
                else this.set(level, box, x, 2, z, AIR);
            }
        }
        for (int z = -1; z <= 1; z += 2) {
            this.set(level, box, ALTAR_X, 3, z, st(Blocks.POLISHED_BLACKSTONE_BRICKS));
            this.set(level, box, ALTAR_X, 4, z, Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 4));
        }
        this.chest(level, box, ALTAR_X, 3, 0, Direction.WEST, ALTAR);
        for (int z = -2; z <= 2; z += 4) this.set(level, box, ALTAR_X + 1, 3, z, st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, 12));
        // the crypt, sealed right under the altar: the Ruby ring's chest
        for (int x = ALTAR_X - 2; x <= ALTAR_X + 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -5; y <= -1; y++) {
                    boolean shell = Math.abs(x - ALTAR_X) == 2 || Math.abs(z) == 2 || y == -5 || y == -1;
                    this.set(level, box, x, y, z, shell ? (y == -5 ? st(Blocks.POLISHED_BLACKSTONE_BRICKS) : st(Blocks.DEEPSLATE_BRICKS)) : AIR);
                }
            }
        }
        this.chest(level, box, ALTAR_X, CRYPT_Y, 0, Direction.WEST, CRYPT);
        this.set(level, box, ALTAR_X, 0, 0, st(Blocks.CHISELED_DEEPSLATE));
        this.set(level, box, ALTAR_X - 1, -4, -1, st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, 4));
        this.set(level, box, ALTAR_X + 1, -4, 1, st(Blocks.COBWEB));
        // soul lanterns hanging down the middle of the nave, and lower in the aisles and the transept's arms; candles
        int[] lanterns = {5, 13, 21, 29, 34, 42, 53};
        for (int x : lanterns) {
            int top = x == 53 ? 21 : 23;
            for (int y = 15; y <= top; y++) this.set(level, box, x, y, 0, st(Blocks.CHAIN));
            this.set(level, box, x, 14, 0, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        for (int x = 11; x <= 47; x += 6) {
            if (x >= TRANS_X0 && x <= TRANS_X1) continue;
            for (int side = -1; side <= 1; side += 2) {
                for (int y = 8; y <= 10; y++) this.set(level, box, x, y, side * 8, st(Blocks.CHAIN));
                this.set(level, box, x, 7, side * 8, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            for (int az = 8; az <= 16; az += 4) {
                for (int y = 12; y <= 23; y++) this.set(level, box, 34, y, side * az, st(Blocks.CHAIN));
                this.set(level, box, 34, 11, side * az, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            }
        }
        for (int[] c : new int[][]{{56, -7}, {56, 7}, {59, 0}, {52, -9}, {52, 9}}) {
            this.set(level, box, c[0], 1, c[1], Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.LIT, true).setValue(CandleBlock.CANDLES, 4));
        }
        // what's left of the pews, thrown into the aisles; bones and skulls about the floor
        for (int x = NAVE_X0 + 1; x <= 28; x++) {
            for (int side = -1; side <= 1; side += 2) {
                for (int az = 7; az <= 8; az++) {
                    int r = this.roll(x, 1, az * side, 9);
                    if (r < 3 && !pierLine(x)) this.set(level, box, x, 1, az * side, stair(Blocks.DARK_OAK_STAIRS, Direction.WEST, false));
                    else if (r == 3) this.set(level, box, x, 1, az * side, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                }
            }
        }
        for (int i = 0; i < 14; i++) {
            int x = 3 + this.roll(i, 2, 0, 58), z = this.roll(i, 3, 0, 17) - 8;
            if (!footprint(x, z) || !this.get(level, x, 1, z).isAir()) continue;
            this.set(level, box, x, 1, z, i % 3 == 0 ? st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, this.roll(i, 4, 0, 16))
                    : i % 3 == 1 ? st(Blocks.BONE_BLOCK) : st(Blocks.COBWEB));
        }
    }

    void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).rotate(ROTATIONS[this.turn]), 2);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity entity) entity.setLootTable(loot, this.seed ^ pos.asLong());
    }

    /** The graveyard in the close, to either side of the nave: headstones, sunken graves, a skull here and there. */
    void graveyard(WorldGenLevel level, BoundingBox box) {
        for (int side = -1; side <= 1; side += 2) {
            for (int x = 10; x <= 58; x += 3) {
                if (x >= TRANS_X0 - 2 && x <= TRANS_X1 + 2) continue;
                for (int az = 15; az <= 23; az += 4) {
                    if (x > APSE_X && Mth.length(x - APSE_X, az) < 14) continue;
                    int z = az * side;
                    if (this.roll(x, 7, z, 10) < 4) continue;
                    // the headstone at the grave's west end, the grave sunk before it
                    int r = this.roll(x, 8, z, 6);
                    this.set(level, box, x, 1, z, r == 0 ? st(Blocks.CRACKED_DEEPSLATE_BRICKS) : r == 1 ? st(Blocks.MOSSY_COBBLESTONE)
                            : st(Blocks.CHISELED_DEEPSLATE));
                    if (r == 2) this.set(level, box, x, 2, z, st(Blocks.DEEPSLATE_BRICK_WALL));
                    if (r == 3) this.set(level, box, x, 2, z, st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, 4));
                    for (int gx = x + 1; gx <= x + 2; gx++) this.set(level, box, gx, 0, z, st(r == 4 ? Blocks.ROOTED_DIRT : Blocks.COARSE_DIRT));
                }
            }
        }
    }
}
