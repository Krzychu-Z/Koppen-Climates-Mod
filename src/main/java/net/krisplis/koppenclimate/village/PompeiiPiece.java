package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
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
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
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
 * Pompeii: a small Roman town that burns and never stops burning, on its patch of red-skied, scorched Mediterranean
 * (PompeiiSites). It stands on a round platform levelled out of the ground ({@link #CORE} blocks across the middle,
 * blended back into the land over {@link #MARGIN} more), and is all ruins, ash, basalt and fire:
 * <ul>
 *   <li>the two streets, the decumanus running right across the town and the cardo crossing it, paved in basalt
 *       flagstones with tuff kerbs;</li>
 *   <li>to the north, the small Colosseum: an oval arena of sand ringed by its podium wall, the tiers of marble
 *       seats rising behind it, an arcaded outer wall in three storeys - part of it fallen - and three gates; and
 *       right under the middle of the arena, sealed in the rock, a little treasury holding a chest (the Amber
 *       ring's: see the pompeii_hoard loot table). A chiselled stone in the middle of the sand marks the spot;</li>
 *   <li>to the south, the Forum: a marble square between two colonnades, entered under an arch, an ever-burning
 *       altar in the middle, and the temple of Jupiter on its high podium at the far end;</li>
 *   <li>to either side, the houses (domus): brick walls with red-painted dados, mosaic floors, an atrium whose
 *       impluvium is a pool of lava, the rooms at the back, amphorae - roofless, gutted, burning;</li>
 *   <li>lava pools glowing between the buildings, basalt everywhere, and fire on netherrack all over - on the
 *       ground, the seats, the walls' broken tops - that never goes out.</li>
 * </ul>
 * Roman Legionnaires (and a couple of blazes) still keep the town.
 */
public class PompeiiPiece extends StructurePiece {
    static final int CORE = 46, MARGIN = 14, BOX = CORE + MARGIN + 1, CLEAR = 32;
    static final ResourceKey<LootTable> HOARD = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chests/pompeii_hoard"));

    // the Colosseum: an oval, its long axis east-west; the arena's half-axes and the outer wall's
    static final int CZ = -27;
    static final double ARENA_A = 8.0, ARENA_B = 5.0, OUTER_A = 17.0, OUTER_B = 12.5;
    static final int WALL_TOP = 14, BAYS = 32;
    // the Forum and the temple at its end
    static final int FORUM_X = 10, FORUM_Z0 = 4, FORUM_Z1 = 29, TEMPLE_X = 6, TEMPLE_Z0 = 30, TEMPLE_Z1 = 40;
    // the houses: centre x, centre z, which way the front faces (0 north, 1 east, 2 south, 3 west)
    static final int[][] HOUSES = {{28, -9, 2}, {-28, -9, 2}, {28, 10, 0}, {-28, 10, 0}, {26, 23, 3}, {-26, 23, 1},
            {29, -23, 3}, {-29, -23, 1}};
    static final int HOUSE_HALF = 5;
    // the lava pools: x, z, radius (tenths)
    static final int[][] POOLS = {{15, 16, 26}, {-15, 16, 26}, {38, 17, 24}, {-38, 17, 24}};

    final BlockPos centre;
    final long seed;

    public PompeiiPiece(BlockPos centre, long seed) {
        super(VillageRegistry.POMPEII_PIECE.get(), 0, new BoundingBox(centre.getX() - BOX, centre.getY() - 12,
                centre.getZ() - BOX, centre.getX() + BOX, centre.getY() + CLEAR, centre.getZ() + BOX));
        this.centre = centre;
        this.seed = seed;
    }

    public PompeiiPiece(CompoundTag tag) {
        super(VillageRegistry.POMPEII_PIECE.get(), tag);
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

    // ================================================================ placing blocks (in the town's own coordinates)

    BlockPos at(int x, int y, int z) {
        return new BlockPos(this.centre.getX() + x, y, this.centre.getZ() + z);
    }

    void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = this.at(x, y, z);
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
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

    /** Netherrack, and fire on it that never goes out. */
    void burn(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        this.set(level, box, x, y, z, st(Blocks.NETHERRACK));
        this.set(level, box, x, y + 1, z, st(Blocks.FIRE));
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

    static BlockState pillar(Block b, Direction.Axis axis) {
        return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    int roll(int x, int z, int salt, int n) {
        long h = (x * 341873128712L + z * 132897987541L + salt * 6971L) ^ this.seed;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) Math.floorMod(h, (long) n);
    }

    // ================================================================ the town's plan

    static boolean road(int x, int z) {
        return Math.abs(z) <= 2 || Math.abs(x) <= 2 && z >= -15 && z <= FORUM_Z0;
    }

    static boolean kerb(int x, int z) {
        return !road(x, z) && (Math.abs(z) == 3 || Math.abs(x) == 3 && z >= -15 && z <= FORUM_Z0);
    }

    /**
     * Where a column lies across the Colosseum's ring: below 0 in the arena, 0 at the arena's edge, 1 at the outer
     * wall's face, above 1 outside it. The ring is a family of ovals between the arena's and the outer wall's.
     */
    static double ring(int x, int z) {
        double dz = z - CZ;
        if ((x / ARENA_A) * (x / ARENA_A) + (dz / ARENA_B) * (dz / ARENA_B) < 1.0) return -1.0;
        double lo = 0.0, hi = 2.0;
        for (int i = 0; i < 22; i++) {
            double u = (lo + hi) * 0.5;
            double a = ARENA_A + (OUTER_A - ARENA_A) * u, b = ARENA_B + (OUTER_B - ARENA_B) * u;
            if ((x / a) * (x / a) + (dz / b) * (dz / b) > 1.0) lo = u;
            else hi = u;
        }
        return (lo + hi) * 0.5;
    }

    /** The angle round the Colosseum (in its own, oval, terms) of a column in its ring. */
    static double ringAngle(int x, int z, double u) {
        double a = ARENA_A + (OUTER_A - ARENA_A) * Math.max(0.0, u), b = ARENA_B + (OUTER_B - ARENA_B) * Math.max(0.0, u);
        return Math.atan2((z - CZ) / b, x / a);
    }

    /** How many blocks thick the ring is at this angle. */
    static double ringWidth(double angle) {
        return Math.sqrt(Math.pow((OUTER_A - ARENA_A) * Math.cos(angle), 2) + Math.pow((OUTER_B - ARENA_B) * Math.sin(angle), 2));
    }

    static boolean inForum(int x, int z) {
        return Math.abs(x) <= FORUM_X && z >= FORUM_Z0 && z <= FORUM_Z1;
    }

    static boolean inTemple(int x, int z) {
        return Math.abs(x) <= TEMPLE_X && z >= TEMPLE_Z0 && z <= TEMPLE_Z1;
    }

    static Direction front(int[] house) {
        return Direction.from2DDataValue((house[2] + 2) % 4);
    }

    static boolean inHouse(int x, int z) {
        for (int[] h : HOUSES) {
            if (Math.abs(x - h[0]) <= HOUSE_HALF && Math.abs(z - h[1]) <= HOUSE_HALF) return true;
        }
        return false;
    }

    static boolean inPool(int x, int z) {
        for (int[] p : POOLS) {
            double r = p[2] / 10.0 + 1.5;
            if ((x - p[0]) * (x - p[0]) + (z - p[1]) * (z - p[1]) <= r * r) return true;
        }
        return false;
    }

    /** Whether a building, or the streets, take this column (the rest is scorched open ground). */
    static boolean claimed(int x, int z) {
        return road(x, z) || kerb(x, z) || ring(x, z) <= 1.0 || inForum(x, z) || inTemple(x, z) || inHouse(x, z) || inPool(x, z);
    }

    // ================================================================ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        REACH.set(box.inflatedBy(15, 0, 15));
        int f = this.centre.getY();
        this.shapeGround(level, box, f);
        int cx = this.centre.getX(), cz = this.centre.getZ();
        LooseTreeCleanup.clean(level, chunkPos, f - 12, f + CLEAR + 4,
                (x, z) -> Mth.length(x - cx, z - cz) < CORE + MARGIN + 8,
                (x, z) -> Mth.length(x - cx, z - cz) <= CORE);
        this.streetsAndGround(level, box, f);
        this.colosseum(level, box, f);
        this.treasury(level, box, f);
        this.forum(level, box, f);
        this.temple(level, box, f);
        for (int[] h : HOUSES) this.house(level, box, f, h);
        for (int[] p : POOLS) this.pool(level, box, f, p[0], p[1], p[2] / 10.0);
        this.life(level, box, f);
    }

    // ---------------------------------------------------------------- the ground

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** Scorched ground: ash, tuff and lava rock, burning in patches. */
    void scorch(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        int patch = this.roll(Math.floorDiv(x + this.roll(z, 0, 1, 2), 3), Math.floorDiv(z, 3), 2, 10);
        int r = this.roll(x, z, 3, 20);
        if (patch < 3) {
            if (r < 14) this.burn(level, box, x, y, z);
            else this.set(level, box, x, y, z, st(r < 17 ? Blocks.MAGMA_BLOCK : Blocks.NETHERRACK));
            return;
        }
        BlockState s;
        if (patch < 7) {
            s = r < 10 ? pillar(Blocks.BASALT, Direction.Axis.Y) : r < 15 ? st(Blocks.SMOOTH_BASALT) : r < 19 ? st(Blocks.BLACKSTONE)
                    : st(Blocks.MAGMA_BLOCK);
        } else {
            s = r < 8 ? st(Blocks.COARSE_DIRT) : r < 13 ? st(Blocks.GRAVEL) : r < 18 ? st(Blocks.TUFF) : st(Blocks.BASALT);
        }
        this.set(level, box, x, y, z, s);
        if (r == 0) this.burn(level, box, x, y, z);
    }

    /** The round platform at the town's floor, its edge blended smoothly back into the ground about it. */
    void shapeGround(WorldGenLevel level, BoundingBox box, int f) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int x = wx - this.centre.getX(), z = wz - this.centre.getZ();
                double dist = Math.max(0.0, Mth.length(x, z) - CORE);
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
                    level.setBlock(pos.set(wx, y, wz), st(y < target - 3 ? Blocks.STONE : Blocks.TUFF), 2);
                }
                // the margin's scorched ground (the town paints its own over the platform)
                if (dist > 0) this.scorch(level, box, x, target, z);
                else level.setBlock(pos.set(wx, target, wz), st(Blocks.TUFF), 2);
            }
        }
    }

    /** The streets, and the scorched open ground between the buildings, strewn with basalt and rubble. */
    void streetsAndGround(WorldGenLevel level, BoundingBox box, int f) {
        for (int wx = box.minX(); wx <= box.maxX(); wx++) {
            for (int wz = box.minZ(); wz <= box.maxZ(); wz++) {
                int x = wx - this.centre.getX(), z = wz - this.centre.getZ();
                if (Mth.length(x, z) > CORE) continue;
                int r = this.roll(x, z, 10, 40);
                if (road(x, z)) {
                    // basalt flagstones, worn and cracked; burning debris lying here and there
                    this.set(level, box, x, f, z, r < 18 ? st(Blocks.SMOOTH_BASALT) : r < 30 ? pillar(Blocks.POLISHED_BASALT, Direction.Axis.Y)
                            : r < 37 ? pillar(Blocks.BASALT, Direction.Axis.Y) : st(Blocks.BLACKSTONE));
                    if (r == 0 && Mth.length(x, z) > 6) this.burn(level, box, x, f, z);
                    continue;
                }
                if (kerb(x, z)) {
                    this.set(level, box, x, f, z, st(r < 30 ? Blocks.POLISHED_TUFF : Blocks.TUFF_BRICKS));
                    continue;
                }
                if (claimed(x, z)) continue;
                this.scorch(level, box, x, f, z);
                if (this.get(level, x, f + 1, z).is(Blocks.FIRE)) continue;
                int d = this.roll(x, z, 11, 60);
                if (d < 2) {
                    // a stump of basalt
                    int h = 1 + this.roll(x, z, 12, 3);
                    for (int y = 1; y <= h; y++) this.set(level, box, x, f + y, z, pillar(Blocks.BASALT, Direction.Axis.Y));
                } else if (d < 4) {
                    this.set(level, box, x, f + 1, z, st(d == 2 ? Blocks.CRACKED_STONE_BRICKS : ModBlocks.CRACKED_MARBLE_BRICKS.get()));
                } else if (d < 6) {
                    this.set(level, box, x, f + 1, z, slab(d == 4 ? Blocks.BRICK_SLAB : ModBlocks.MARBLE_BRICK_SLAB.get(), SlabType.BOTTOM));
                }
            }
        }
    }

    // ---------------------------------------------------------------- the Colosseum

    /** How high the outer wall still stands at this angle: full height, but fallen away over one side. */
    int wallTop(double angle, int x, int z) {
        double fall = (this.seed >>> 7 & 1023) / 1023.0 * Math.PI * 2;
        double off = Math.abs(Mth.wrapDegrees((angle - fall) * Mth.RAD_TO_DEG)) * Mth.DEG_TO_RAD;
        if (off >= 0.95) return WALL_TOP;
        double t = off / 0.95;
        return 3 + (int) Math.round((WALL_TOP - 3) * t * t) + this.roll(x, z, 20, 2);
    }

    void colosseum(WorldGenLevel level, BoundingBox box, int f) {
        BlockState bricks = st(ModBlocks.MARBLE_BRICKS.get()), cracked = st(ModBlocks.CRACKED_MARBLE_BRICKS.get());
        BlockState core = st(Blocks.TUFF_BRICKS), seat = st(ModBlocks.POLISHED_MARBLE.get());
        int x0 = (int) OUTER_A + 1, z0 = (int) OUTER_B + 1;
        for (int x = -x0; x <= x0; x++) {
            for (int z = CZ - z0; z <= CZ + z0; z++) {
                if (!box.isInside(this.at(x, f, z))) continue;
                double u = ring(x, z);
                if (u > 1.0) continue;
                int r = this.roll(x, z, 21, 24);
                if (u < 0) {
                    // the arena: sand, a few burning spots, the bones of the last games
                    this.set(level, box, x, f, z, st(Blocks.SAND));
                    if (x == 0 && z == CZ) {
                        this.set(level, box, x, f, z, st(ModBlocks.CHISELED_MARBLE.get()));
                    } else if (r < 2) {
                        this.burn(level, box, x, f, z);
                    } else if (r == 2) {
                        this.set(level, box, x, f + 1, z, st(Blocks.BONE_BLOCK));
                    } else if (r == 3 && this.roll(x, z, 22, 3) == 0) {
                        this.set(level, box, x, f + 1, z, st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, this.roll(x, z, 23, 16)));
                    }
                    continue;
                }
                double angle = ringAngle(x, z, u), width = ringWidth(angle);
                double in = u * width, out = (1.0 - u) * width;
                int top = f + this.wallTop(angle, x, z);
                boolean gate = Math.abs(z - CZ) <= 1 || Math.abs(x) <= 1 && z > CZ;
                this.set(level, box, x, f, z, bricks);
                int height;
                if (in < 1.2) {
                    // the podium wall round the arena
                    height = f + 3;
                    for (int y = f + 1; y <= height; y++) this.set(level, box, x, y, z, r == 0 ? cracked : bricks);
                } else if (out >= 3.0) {
                    // the tiers of seats, stepping up and away from the arena
                    double s = (in - 1.2) / Math.max(0.5, width - 3.0 - 1.2);
                    height = Math.min(f + 4 + (int) (Mth.clamp(s, 0.0, 0.999) * 6), top);
                    for (int y = f + 1; y < height; y++) this.set(level, box, x, y, z, core);
                    // the step's edge: a seat (a stair with its back to the outer wall) where the next tier in is lower
                    Direction outward = Math.abs(x / (OUTER_A * OUTER_A)) > Math.abs((z - CZ) / (OUTER_B * OUTER_B))
                            ? (x > 0 ? Direction.EAST : Direction.WEST) : (z > CZ ? Direction.SOUTH : Direction.NORTH);
                    int ix = x - outward.getStepX(), iz = z - outward.getStepZ();
                    double iu = ring(ix, iz);
                    boolean edge = false;
                    if (iu >= 0) {
                        double iw = ringWidth(ringAngle(ix, iz, iu)), iin = iu * iw;
                        double is = (iin - 1.2) / Math.max(0.5, iw - 3.0 - 1.2);
                        int ih = iin < 1.2 ? f + 3 : f + 4 + (int) (Mth.clamp(is, 0.0, 0.999) * 6);
                        edge = ih < height;
                    }
                    if (r < 3) {
                        this.burn(level, box, x, height, z);
                    } else {
                        this.set(level, box, x, height, z, edge ? stair(ModBlocks.MARBLE_BRICK_STAIRS.get(), outward, false)
                                : r == 3 ? cracked : seat);
                    }
                } else if (out >= 1.5) {
                    // the arcade inside the outer wall, under the top tier's walk
                    height = Math.min(f + 10, top);
                    for (int y = f + 5; y < height; y++) this.set(level, box, x, y, z, core);
                    if (height >= f + 5) {
                        if (r < 2) this.burn(level, box, x, height, z);
                        else this.set(level, box, x, height, z, seat);
                    }
                } else {
                    // the outer wall: three storeys, arched below, windowed at the top
                    height = top;
                    double bay = (angle / (Math.PI * 2) * BAYS) % 1.0;
                    if (bay < 0) bay += 1.0;
                    boolean pier = bay < 0.34, crown = Math.abs(bay - 0.67) < 0.17;
                    for (int y = f + 1; y <= height; y++) {
                        int storey = y - f;
                        boolean open = !pier && (storey <= 3 || storey == 4 && crown || storey >= 6 && storey <= 8
                                || storey == 9 && crown) || storey == 12 && Math.abs(bay - 0.67) < 0.1;
                        BlockState s = storey == 5 || storey == 10 ? seat : this.roll(x * 31 + y, z, 24, 7) == 0 ? cracked : bricks;
                        this.set(level, box, x, y, z, open ? st(Blocks.AIR) : s);
                    }
                    if (r < 2 && !this.get(level, x, height, z).isAir()) this.burn(level, box, x, height, z);
                }
                if (gate) {
                    // the gates: a passage at the arena's level right through the ring
                    for (int y = f + 1; y <= f + 4; y++) this.set(level, box, x, y, z, st(Blocks.AIR));
                    if (height >= f + 5 && out < 1.5) this.set(level, box, x, f + 5, z, seat);
                }
            }
        }
    }

    /**
     * The treasury, sealed in the rock right under the middle of the arena: a little marble room, dimly lit, and in
     * it the chest of Pompeii's hoard (the Amber ring is in it) among amphorae and gold. The chiselled stone in the
     * middle of the sand above has only sand under it, and under that - the room.
     */
    void treasury(WorldGenLevel level, BoundingBox box, int f) {
        int bottom = f - 7, ceiling = f - 3;
        for (int x = -4; x <= 4; x++) {
            for (int z = CZ - 3; z <= CZ + 3; z++) {
                for (int y = bottom; y <= ceiling; y++) {
                    boolean shell = Math.abs(x) == 4 || Math.abs(z - CZ) == 3 || y == bottom || y == ceiling;
                    BlockState s;
                    if (!shell) s = st(Blocks.AIR);
                    else if (y == bottom) s = st(ModBlocks.MARBLE_TILES.get());
                    else if (y == ceiling) s = st(ModBlocks.POLISHED_MARBLE.get());
                    else s = this.roll(x * 13 + y, z, 30, 5) == 0 ? st(ModBlocks.CRACKED_MARBLE_BRICKS.get()) : st(ModBlocks.MARBLE_BRICKS.get());
                    this.set(level, box, x, y, z, s);
                }
            }
        }
        // the sand under the marker stone, down through the ceiling
        for (int y = ceiling; y < f; y++) this.set(level, box, 0, y, CZ, st(Blocks.SAND));
        // the hoard
        BlockPos chest = this.at(0, bottom + 1, CZ - 2);
        if (box.isInside(chest)) {
            level.setBlock(chest, st(Blocks.CHEST).setValue(ChestBlock.FACING, Direction.SOUTH), 2);
            if (level.getBlockEntity(chest) instanceof ChestBlockEntity entity) entity.setLootTable(HOARD, this.seed ^ chest.asLong());
        }
        this.set(level, box, -1, bottom + 1, CZ - 2, st(Blocks.GOLD_BLOCK));
        this.set(level, box, 1, bottom + 1, CZ - 2, st(Blocks.GOLD_BLOCK));
        this.set(level, box, -3, bottom + 1, CZ - 2, st(Blocks.DECORATED_POT));
        this.set(level, box, 3, bottom + 1, CZ - 2, st(Blocks.DECORATED_POT));
        this.set(level, box, -3, bottom + 1, CZ + 2, st(Blocks.DECORATED_POT));
        this.set(level, box, 3, bottom + 1, CZ + 2, st(Blocks.LANTERN));
        this.set(level, box, -3, bottom + 1, CZ, st(Blocks.LANTERN));
        this.set(level, box, 3, bottom + 1, CZ, st(ModBlocks.CHISELED_MARBLE.get()));
        this.set(level, box, 3, bottom + 2, CZ, st(Blocks.SKELETON_SKULL).setValue(SkullBlock.ROTATION, 12));
    }

    // ---------------------------------------------------------------- the Forum

    /** A marble column of the given full height (base, fluted shaft, capital) - or what is left of it. */
    void column(WorldGenLevel level, BoundingBox box, int x, int y0, int z, int height, int ruin) {
        // ruin: 0 standing whole, 1 broken off part way up, 2 gone but for its base
        this.set(level, box, x, y0, z, st(ModBlocks.MARBLE_COLUMN_BASE.get()));
        int shaft = ruin == 0 ? height - 2 : ruin == 1 ? 1 + this.roll(x, z, 40, Math.max(1, height - 3)) : 0;
        for (int y = 1; y <= shaft; y++) this.set(level, box, x, y0 + y, z, pillar(ModBlocks.FLUTED_MARBLE_COLUMN.get(), Direction.Axis.Y));
        if (ruin == 0) this.set(level, box, x, y0 + height - 1, z, st(ModBlocks.MARBLE_COLUMN_CAPITAL.get()));
        else if (ruin == 1 && this.roll(x, z, 41, 2) == 0) this.burn(level, box, x, y0 + shaft + 1, z);
    }

    int ruin(int x, int z) {
        int r = this.roll(x, z, 42, 10);
        return r < 5 ? 0 : r < 8 ? 1 : 2;
    }

    void forum(WorldGenLevel level, BoundingBox box, int f) {
        for (int x = -FORUM_X; x <= FORUM_X; x++) {
            for (int z = FORUM_Z0; z <= FORUM_Z1; z++) {
                int r = this.roll(x, z, 43, 30);
                boolean border = Math.abs(x) == FORUM_X || z == FORUM_Z0 || z == FORUM_Z1 || Math.abs(x) == 8;
                BlockState s = border ? st(ModBlocks.POLISHED_MARBLE.get()) : r < 3 ? st(ModBlocks.CRACKED_MARBLE_BRICKS.get())
                        : r < 5 ? st(Blocks.SMOOTH_BASALT) : st(ModBlocks.MARBLE_TILES.get());
                this.set(level, box, x, f, z, s);
                if (!border && r >= 27) this.burn(level, box, x, f, z);
            }
        }
        // the colonnades down either side, their architrave where neighbouring columns still stand
        for (int side = -1; side <= 1; side += 2) {
            int x = side * 8;
            int prevZ = Integer.MIN_VALUE;
            for (int z = 8; z <= 26; z += 3) {
                int ruin = this.ruin(x, z);
                this.column(level, box, x, f + 1, z, 7, ruin);
                if (ruin == 0) {
                    if (prevZ == z - 3) {
                        for (int az = prevZ; az <= z; az++) this.set(level, box, x, f + 8, az, st(ModBlocks.POLISHED_MARBLE.get()));
                    }
                    prevZ = z;
                } else if (ruin == 2) {
                    // the fallen column, lying out across the ground behind the colonnade
                    int len = 3 + this.roll(x, z, 44, 2);
                    for (int i = 1; i <= len; i++) {
                        this.set(level, box, x + side * i, f + 1, z, pillar(ModBlocks.FLUTED_MARBLE_COLUMN.get(), Direction.Axis.X));
                    }
                    if (len == 4) this.set(level, box, x + side * (len + 1), f + 1, z, st(ModBlocks.MARBLE_COLUMN_CAPITAL.get()));
                }
            }
        }
        // the arch into the Forum from the street
        for (int x = -5; x <= 5; x++) {
            for (int z = FORUM_Z0; z <= FORUM_Z0 + 1; z++) {
                for (int y = f + 1; y <= f + 8; y++) {
                    boolean opening = Math.abs(x) <= 2 && y <= f + 5;
                    if (opening) continue;
                    BlockState s = y == f + 8 ? st(ModBlocks.POLISHED_MARBLE.get())
                            : this.roll(x * 7 + y, z, 45, 6) == 0 ? st(ModBlocks.CRACKED_MARBLE_BRICKS.get()) : st(ModBlocks.MARBLE_BRICKS.get());
                    this.set(level, box, x, y, z, s);
                }
                // the arch's curve
                this.set(level, box, -2, f + 5, z, stair(ModBlocks.MARBLE_BRICK_STAIRS.get(), Direction.WEST, true));
                this.set(level, box, 2, f + 5, z, stair(ModBlocks.MARBLE_BRICK_STAIRS.get(), Direction.EAST, true));
            }
        }
        this.set(level, box, 0, f + 6, FORUM_Z0, st(ModBlocks.CHISELED_MARBLE.get()));
        this.burn(level, box, -4, f + 8, FORUM_Z0);
        // the ever-burning altar
        int az = 17;
        for (int x = -1; x <= 1; x++) {
            for (int z = az - 1; z <= az + 1; z++) this.set(level, box, x, f + 1, z, slab(ModBlocks.POLISHED_MARBLE_SLAB.get(), SlabType.BOTTOM));
        }
        this.set(level, box, 0, f + 1, az, st(ModBlocks.CHISELED_MARBLE.get()));
        this.burn(level, box, 0, f + 2, az);
    }

    // ---------------------------------------------------------------- the temple of Jupiter

    void temple(WorldGenLevel level, BoundingBox box, int f) {
        int top = f + 3;
        for (int x = -TEMPLE_X; x <= TEMPLE_X; x++) {
            for (int z = TEMPLE_Z0; z <= TEMPLE_Z1; z++) {
                boolean edge = Math.abs(x) == TEMPLE_X || z == TEMPLE_Z0 || z == TEMPLE_Z1;
                for (int y = f; y < top; y++) this.set(level, box, x, y, z, edge ? st(ModBlocks.MARBLE_BRICKS.get()) : st(Blocks.TUFF_BRICKS));
                int r = this.roll(x, z, 50, 12);
                this.set(level, box, x, top, z, r == 0 ? st(ModBlocks.CRACKED_MARBLE_BRICKS.get()) : st(ModBlocks.MARBLE_TILES.get()));
                if (r == 1) this.burn(level, box, x, top, z);
            }
        }
        // the steps up from the Forum
        for (int x = -4; x <= 4; x++) {
            for (int i = 0; i < 3; i++) {
                int z = TEMPLE_Z0 - 3 + i;
                for (int y = f + 1; y < f + 1 + i; y++) this.set(level, box, x, y, z, st(ModBlocks.MARBLE_BRICKS.get()));
                this.set(level, box, x, f + 1 + i, z, stair(ModBlocks.MARBLE_STAIRS.get(), Direction.SOUTH, false));
            }
        }
        // the portico: six columns across the front, two more down each side
        int prev = Integer.MIN_VALUE;
        for (int x = -5; x <= 5; x += 2) {
            int ruin = this.ruin(x, TEMPLE_Z0 + 1);
            this.column(level, box, x, top + 1, TEMPLE_Z0 + 1, 9, ruin == 2 && Math.abs(x) == 1 ? 1 : ruin);
            if (ruin == 0) {
                if (prev == x - 2) {
                    for (int ax = prev; ax <= x; ax++) this.set(level, box, ax, top + 10, TEMPLE_Z0 + 1, st(ModBlocks.POLISHED_MARBLE.get()));
                }
                prev = x;
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            this.column(level, box, side * 5, top + 1, TEMPLE_Z0 + 3, 9, this.ruin(side * 5, TEMPLE_Z0 + 3));
        }
        // the cella: its walls broken down to jagged stumps, the god's plinth inside it burning
        int z0 = TEMPLE_Z0 + 4, z1 = TEMPLE_Z1 - 1;
        for (int x = -4; x <= 4; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = Math.abs(x) == 4 || z == z0 || z == z1;
                if (!wall) {
                    if (this.roll(x, z, 51, 6) == 0) this.burn(level, box, x, top, z);
                    continue;
                }
                if (z == z0 && Math.abs(x) <= 1) continue;
                int h = 7 - this.roll(Math.floorDiv(x, 2), Math.floorDiv(z, 2), 52, 6);
                for (int y = 1; y <= h; y++) {
                    this.set(level, box, x, top + y, z, this.roll(x * 5 + y, z, 53, 6) == 0 ? st(ModBlocks.CRACKED_MARBLE_BRICKS.get())
                            : st(ModBlocks.MARBLE_BRICKS.get()));
                }
                if (this.roll(x, z, 54, 5) == 0) this.burn(level, box, x, top + h, z);
            }
        }
        this.set(level, box, 0, top + 1, z1 - 2, st(ModBlocks.CHISELED_MARBLE.get()));
        this.set(level, box, 0, top + 2, z1 - 2, st(ModBlocks.MARBLE_COLUMN_BASE.get()));
        this.burn(level, box, 0, top + 3, z1 - 2);
    }

    // ---------------------------------------------------------------- the houses

    /**
     * A house (domus), gutted by the fire: across its front the entrance, then the atrium - a mosaic floor round
     * the impluvium, which holds lava now, four columns at its corners - and behind, the tablinum open to the atrium
     * between two small rooms. Brick walls over a red-painted dado, broken down to stumps in places; no roof left.
     * In the house's own frame: u across (-5 to 5), v from the front wall (0) to the back (10).
     */
    void house(WorldGenLevel level, BoundingBox box, int f, int[] h) {
        Direction front = front(h), right = front.getClockWise();
        for (int u = -HOUSE_HALF; u <= HOUSE_HALF; u++) {
            for (int v = 0; v <= 2 * HOUSE_HALF; v++) {
                int x = h[0] + (HOUSE_HALF - v) * front.getStepX() + u * right.getStepX();
                int z = h[1] + (HOUSE_HALF - v) * front.getStepZ() + u * right.getStepZ();
                if (!box.isInside(this.at(x, f, z))) continue;
                int r = this.roll(x, z, 60, 24);
                boolean outer = Math.abs(u) == HOUSE_HALF || v == 0 || v == 2 * HOUSE_HALF;
                boolean partition = v == 6 && Math.abs(u) >= 2 && Math.abs(u) != 3 || v > 6 && v < 10 && Math.abs(u) == 2;
                boolean door = v == 0 && Math.abs(u) <= 1;
                boolean impluvium = Math.abs(u) <= 1 && v >= 2 && v <= 4;
                boolean rim = !impluvium && Math.abs(u) <= 2 && v >= 1 && v <= 5;
                // the floor
                BlockState floor;
                if (impluvium) floor = st(Blocks.LAVA);
                else if (rim) floor = st(ModBlocks.POLISHED_MARBLE.get());
                else if (v >= 7 && Math.abs(u) <= 1) floor = (u + v) % 2 == 0 ? st(Blocks.WHITE_TERRACOTTA) : st(Blocks.RED_TERRACOTTA);
                else floor = (Math.abs(u) + Math.abs(v - 3)) % 3 == 0 ? st(Blocks.POLISHED_BLACKSTONE) : st(Blocks.WHITE_TERRACOTTA);
                this.set(level, box, x, f, z, floor);
                if (impluvium) {
                    this.set(level, box, x, f - 1, z, st(Blocks.BASALT));
                    continue;
                }
                if ((outer || partition) && !door) {
                    // the walls: a red dado, brick and plaster above, standing to different heights
                    int seg = outer ? (v == 0 || v == 2 * HOUSE_HALF ? Math.floorDiv(u, 3) : Math.floorDiv(v, 3)) : Math.floorDiv(u + v, 2);
                    int height = 5 - this.roll(seg + h[0], (outer ? (v == 0 ? 1 : v == 2 * HOUSE_HALF ? 2 : u < 0 ? 3 : 4) : 5) + h[1], 61, 4);
                    if (partition) height = Math.min(height, 4);
                    for (int y = 1; y <= height; y++) {
                        int p = this.roll(x * 3 + y, z, 62, 6);
                        BlockState s = y <= 2 ? st(Blocks.RED_TERRACOTTA) : p == 0 ? st(Blocks.WHITE_TERRACOTTA) : st(Blocks.BRICKS);
                        this.set(level, box, x, f + y, z, s);
                    }
                    if (r < 4) this.burn(level, box, x, f + height, z);
                    continue;
                }
                if (door) {
                    // the lintel over the entrance, where the wall still stands that high
                    if (this.roll(h[0], h[1], 63, 3) != 0) this.set(level, box, x, f + 4, z, st(Blocks.BRICKS));
                    continue;
                }
                // the four columns round the impluvium
                if (Math.abs(u) == 2 && (v == 1 || v == 5)) {
                    this.column(level, box, x, f + 1, z, 5, this.ruin(x, z));
                    continue;
                }
                // inside: the burning wreck of the roof and everything in the house
                if (r < 4) this.burn(level, box, x, f, z);
                else if (r == 4) this.set(level, box, x, f + 1, z, slab(Blocks.BRICK_SLAB, SlabType.BOTTOM));
                else if (r == 5) this.set(level, box, x, f + 1, z, st(Blocks.COARSE_DIRT));
                else if (r == 6 && !rim) this.set(level, box, x, f + 1, z, st(Blocks.DECORATED_POT));
            }
        }
    }

    // ---------------------------------------------------------------- the lava pools

    void pool(WorldGenLevel level, BoundingBox box, int f, int px, int pz, double radius) {
        int reach = (int) Math.ceil(radius + 1.5);
        double phase = this.roll(px, pz, 70, 628) / 100.0;
        for (int x = px - reach; x <= px + reach; x++) {
            for (int z = pz - reach; z <= pz + reach; z++) {
                double d = Mth.length(x - px, z - pz), a = Math.atan2(z - pz, x - px);
                double edge = radius * (1.0 + 0.15 * Math.sin(a * 3 + phase));
                if (d <= edge) {
                    int depth = d < edge * 0.55 ? 2 : 1;
                    for (int y = f; y > f - depth; y--) this.set(level, box, x, y, z, st(Blocks.LAVA));
                    this.set(level, box, x, f - depth, z, st(Blocks.BASALT));
                    if (depth == 1) this.set(level, box, x, f - 2, z, st(Blocks.BASALT));
                    for (int y = f + 1; y <= f + 3; y++) this.set(level, box, x, y, z, st(Blocks.AIR));
                } else if (d <= edge + 1.5) {
                    int r = this.roll(x, z, 71, 5);
                    this.set(level, box, x, f, z, r < 2 ? st(Blocks.MAGMA_BLOCK) : r < 4 ? pillar(Blocks.BASALT, Direction.Axis.Y) : st(Blocks.SMOOTH_BASALT));
                    if (this.get(level, x, f + 1, z).is(Blocks.FIRE)) this.set(level, box, x, f + 1, z, st(Blocks.AIR));
                }
            }
        }
    }

    // ---------------------------------------------------------------- its keepers

    void life(WorldGenLevel level, BoundingBox box, int f) {
        int[][] legion = {{0, 12}, {4, 22}, {-15, 0}, {15, 0}, {-4, CZ}, {4, CZ}};
        for (int[] at : legion) this.mob(level, ModEntities.ROMAN_LEGIONNAIRE.get(), this.at(at[0], f + 1, at[1]), box);
        this.mob(level, EntityType.BLAZE, this.at(0, f + 5, 22), box);
        this.mob(level, EntityType.BLAZE, this.at(0, f + 6, CZ + 2), box);
    }

    void mob(WorldGenLevel level, EntityType<?> type, BlockPos pos, BoundingBox box) {
        if (!box.isInside(pos)) return;
        BlockState here = level.getBlockState(pos);
        if (here.blocksMotion()) return;
        if (!(type.create(level.getLevel()) instanceof Mob mob)) return;
        if (here.is(Blocks.FIRE)) level.setBlock(pos, st(Blocks.AIR), 2);
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
