package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
 * A ghost town of the Lush desert (BSh): a Wild West town, long abandoned and falling apart.
 *
 * <ul>
 *   <li>One dusty main street running east-west, lined on both sides by false-fronted wooden buildings with
 *       boardwalks and awnings in front: the saloon, the general store, the bank with its vault, the sheriff's office
 *       and jail, the hotel, the undertaker's (coffins), the blacksmith, the stable, the barber's and a few houses.
 *       Their names are still on the signs over the boardwalks.</li>
 *   <li>The well and the water tower in the middle, the church with its bell tower at the east end of the street,
 *       the gallows at the west end, and the graveyard out behind the south row.</li>
 *   <li>Everything is grey, bleached dead oak, with Joshua tree planks here and there. The boards have holes, the
 *       windows are broken, roofs have fallen in, and there are cobwebs everywhere.</li>
 *   <li>Nobody lives here. By day it's empty under its yellow sky; at night the cowboy souls and skeleton horses come
 *       out (GhostTownNights; the souls are this structure's own spawns).</li>
 *   <li>It isn't levelled: every building, the church, the well, the water tower and the gallows sit at the height of
 *       the ground under them (worked out by GhostTownStructure), cut into the slope on its high side and standing on
 *       a sandstone foundation on its low side; the street, the graves and the litter follow the ground.</li>
 * </ul>
 */
public class GhostTownPiece extends StructurePiece {
    // the town's extent
    static final int HALF_X = 64, HALF_Z = 26, MARGIN = 4;
    static final int BOX_X = HALF_X + MARGIN, BOX_Z = HALF_Z + MARGIN;
    static final int CLEAR = 34;
    // which floor (see floors) each part of the town stands on: the buildings by their index, then these
    static final int CHURCH = 12, WELL = 13, TOWER = 14, GALLOWS = 15, PARTS = 16;
    // the street: |z| <= STREET; the boardwalks STREET+1 and STREET+2; the buildings' fronts at FRONT
    static final int STREET = 2, FRONT = 5;

    static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chests/ghost_town"));
    static final ResourceKey<LootTable> ALTAR = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chests/ghost_town_altar"));
    // the church (bell tower and roof included), relative to the town's centre and floor; and the pentagram's middle
    public static final int CHURCH_X0 = 30, CHURCH_X1 = 62, CHURCH_Z0 = -13, CHURCH_Z1 = 13, CHURCH_TOP = 27;
    // the nave: 27 long, 23 wide
    static final int NAVE_X0 = 34, NAVE_X1 = 60, NAVE_Z0 = -11, NAVE_Z1 = 11, NAVE_WALL = 7;
    // the pentagram on the nave's floor, as drawn (19 x 19, its top towards the altar): # inlaid red nether brick,
    // Y a brick with a lit candle on it (its five points), R the jukebox in the middle
    static final int PENTAGRAM_X = 47, PENTAGRAM_Z = 0;
    static final String[] PENTAGRAM = {
            ".......##Y##.......",
            ".....##..#..##.....",
            "...##...###...##...",
            "..#.....#.#.....#..",
            "..#.....#.#.....#..",
            ".#.....##.##.....#.",
            ".Y###############Y.",
            "#.##...#...#...##.#",
            "#..##..#...#..##..#",
            "#....##..R..##....#",
            "#.....##...##.....#",
            "#....##.#.#.##....#",
            ".#...#..###..#...#.",
            ".#...#.##.##.#...#.",
            "..#.###.....###.#..",
            "..#.##.......##.#..",
            "...#Y.........Y#...",
            ".....##.....##.....",
            ".......#####......."};
    static final ResourceKey<LootTable> VAULT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "chests/ghost_town_vault"));

    enum Kind {
        SALOON("SALOON"), STORE("GENERAL STORE"), BANK("BANK"), SHERIFF("SHERIFF"), HOTEL("HOTEL"),
        UNDERTAKER("UNDERTAKER"), SMITHY("BLACKSMITH"), STABLE("STABLE"), BARBER("BARBER"), HOUSE("");

        final String sign;

        Kind(String sign) {
            this.sign = sign;
        }
    }

    // each building: kind, x0, x1, side (-1 north row, +1 south row), depth, wall height, false-front height
    record Building(Kind kind, int x0, int x1, int side, int depth, int height, int front) {
    }

    static final Building[] BUILDINGS = {
            new Building(Kind.SALOON, -30, -21, -1, 9, 8, 3),
            new Building(Kind.STORE, -19, -13, -1, 8, 5, 3),
            new Building(Kind.BANK, -11, -5, -1, 8, 5, 2),
            new Building(Kind.SHERIFF, 5, 11, -1, 8, 5, 2),
            new Building(Kind.HOTEL, 13, 21, -1, 9, 8, 2),
            new Building(Kind.UNDERTAKER, 23, 28, -1, 7, 5, 2),
            new Building(Kind.SMITHY, -30, -24, 1, 7, 5, 2),
            new Building(Kind.HOUSE, -22, -17, 1, 7, 4, 2),
            new Building(Kind.BARBER, -15, -10, 1, 7, 4, 2),
            new Building(Kind.STABLE, 6, 14, 1, 8, 5, 0),
            new Building(Kind.HOUSE, 16, 21, 1, 7, 4, 2),
            new Building(Kind.HOUSE, 23, 28, 1, 7, 4, 1)};

    final BlockPos centre;
    final long seed;
    // the floor height of each part of the town
    final int[] floors;
    // fences, panes and bars placed this chunk, joined up once everything is built
    private transient List<BlockPos> joins;

    public GhostTownPiece(BlockPos centre, long seed, int[] floors) {
        super(VillageRegistry.GHOST_TOWN_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX_X, java.util.Arrays.stream(floors).min().orElse(centre.getY()) - 40, centre.getZ() - BOX_Z,
                centre.getX() + BOX_X, java.util.Arrays.stream(floors).max().orElse(centre.getY()) + CLEAR, centre.getZ() + BOX_Z));
        this.centre = centre;
        this.seed = seed;
        this.floors = floors;
    }

    public GhostTownPiece(CompoundTag tag) {
        super(VillageRegistry.GHOST_TOWN_PIECE.get(), tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.seed = tag.getLong("Seed");
        int[] f = tag.getIntArray("Floors");
        if (f.length != PARTS) {
            f = new int[PARTS];
            java.util.Arrays.fill(f, this.centre.getY());
        }
        this.floors = f;
    }

    /** The ground each part of the town covers (x0, z0, x1, z1, relative to the centre), in the order of floors. */
    public static int[][] footprints() {
        int[][] out = new int[PARTS][];
        for (int i = 0; i < BUILDINGS.length; i++) {
            Building b = BUILDINGS[i];
            int za = rowZ(b.side(), -3), zb = rowZ(b.side(), b.depth() - 1);
            out[i] = new int[]{b.x0(), Math.min(za, zb), b.x1(), Math.max(za, zb)};
        }
        out[CHURCH] = new int[]{NAVE_X0 - 3, NAVE_Z0, NAVE_X1, NAVE_Z1};
        out[WELL] = new int[]{-1, -9, 1, -7};
        out[TOWER] = new int[]{-2, 7, 2, 12};
        out[GALLOWS] = new int[]{-40, -2, -33, 2};
        return out;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.centre.getX());
        tag.putInt("CY", this.centre.getY());
        tag.putInt("CZ", this.centre.getZ());
        tag.putLong("Seed", this.seed);
        tag.putIntArray("Floors", this.floors);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int[] fl = this.floors;
        this.joins = new ArrayList<>();
        // every part on its own ground: cut into the slope or raised on a foundation
        int[][] feet = footprints();
        for (int i = 0; i < PARTS; i++) this.foundation(level, box, feet[i], fl[i], i == CHURCH ? CHURCH_TOP + 1 : 14);
        this.street(level, box);
        for (int i = 0; i < BUILDINGS.length; i++) this.building(level, box, BUILDINGS[i], fl[i]);
        this.well(level, box, fl[WELL]);
        this.waterTower(level, box, fl[TOWER]);
        this.church(level, box, fl[CHURCH]);
        this.gallows(level, box, fl[GALLOWS]);
        this.graveyard(level, box);
        this.litter(level, box);
        // join the fences, panes and bars up with what's around them
        for (BlockPos p : this.joins) {
            if (box.isInside(p)) {
                BlockState s = level.getBlockState(p);
                level.setBlock(p, Block.updateFromNeighbourShapes(s, level, p), 2);
            }
        }
        this.joins = null;
    }

    // ---------------------------------------------------------------- the church, for DemonRaids

    public BlockPos centre() {
        return this.centre;
    }

    /** The town's centre, at the height of the church's floor: what the church's blocks are measured from. */
    public BlockPos churchOrigin() {
        return new BlockPos(this.centre.getX(), this.floors[CHURCH], this.centre.getZ());
    }

    /** Whether a block lies in this town's church (bell tower and roof included, floor too). */
    public boolean inChurch(BlockPos pos) {
        int x = pos.getX() - this.centre.getX(), z = pos.getZ() - this.centre.getZ(), y = pos.getY() - this.floors[CHURCH];
        return x >= CHURCH_X0 && x <= CHURCH_X1 && z >= CHURCH_Z0 && z <= CHURCH_Z1 && y >= 0 && y <= CHURCH_TOP;
    }

    /** Each cell of the pentagram: x, z (relative to the centre) and what's drawn there. */
    static List<int[]> pentagram() {
        List<int[]> out = new ArrayList<>();
        int half = PENTAGRAM.length / 2;
        for (int row = 0; row < PENTAGRAM.length; row++) {
            for (int col = 0; col < PENTAGRAM[row].length(); col++) {
                char c = PENTAGRAM[row].charAt(col);
                // the drawing's top points east, to the altar
                if (c != '.') out.add(new int[]{PENTAGRAM_X + half - row, PENTAGRAM_Z + col - half, c});
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    BlockPos at(int x, int y, int z) {
        return new BlockPos(this.centre.getX() + x, y, this.centre.getZ() + z);
    }

    void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, state, 2);
        if (this.joins != null && (state.is(BlockTags.FENCES) || state.is(Blocks.IRON_BARS) || state.is(Blocks.GLASS_PANE)
                || state.is(BlockTags.WALLS))) {
            this.joins.add(pos);
        }
    }

    void fill(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) this.set(level, box, x, y, z, state);
            }
        }
    }

    static BlockState st(Block block) {
        return block.defaultBlockState();
    }

    /** A steady random number for a spot (the same whichever chunk is being built): 0-99. */
    int roll(int x, int y, int z, int salt) {
        long h = this.seed ^ (x * 3129871L) ^ (z * 116129781L) ^ (y * 0x9E3779B97F4A7C15L) ^ (salt * 0x632BE59BD9B4E019L);
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (int) Math.floorMod(h, 100L);
    }

    /** The north row's buildings face south onto the street, the south row's north. */
    static Direction facing(int side) {
        return side < 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** The z of a building's row, v blocks back from its front wall (negative: out over the boardwalk). */
    static int rowZ(int side, int v) {
        return side < 0 ? -FRONT - v : FRONT + v;
    }

    static BlockState log(Block block, Direction.Axis axis) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    static BlockState stair(Block block, Direction facing, Half half) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    static BlockState slab(Block block, SlabType type) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    static BlockState trapdoor(Block block, Direction facing, Half half, boolean open) {
        return block.defaultBlockState().setValue(TrapDoorBlock.FACING, facing).setValue(TrapDoorBlock.HALF, half)
                .setValue(TrapDoorBlock.OPEN, open);
    }

    void door(WorldGenLevel level, BoundingBox box, int x, int y, int z, Block door, Direction facing, boolean open) {
        BlockState d = door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.OPEN, open)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.set(level, box, x, y, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.set(level, box, x, y + 1, z, d.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    void chest(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 2);
        RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(this.seed ^ pos.asLong()), pos, loot);
    }

    void barrel(WorldGenLevel level, BoundingBox box, int x, int y, int z, ResourceKey<LootTable> loot) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), 2);
        if (loot != null) RandomizableContainer.setBlockEntityLootTable(level, RandomSource.create(this.seed ^ pos.asLong()), pos, loot);
    }

    void sign(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction facing, String... lines) {
        BlockPos pos = this.at(x, y, z);
        if (!box.isInside(pos)) return;
        level.setBlock(pos, ModBlocks.DEAD_OAK_WALL_SIGN.get().defaultBlockState().setValue(WallSignBlock.FACING, facing), 2);
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            int first = lines.length == 1 ? 1 : 0;
            for (int i = 0; i < lines.length && first + i < 4; i++) text = text.setMessage(first + i, Component.literal(lines[i]));
            // (during world generation the sign has no level yet, so setText - which tells the level the block changed -
            // would crash: load the text into it as saved data instead)
            var ops = level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
            CompoundTag tag = new CompoundTag();
            SignText.DIRECT_CODEC.encodeStart(ops, text).result().ifPresent(t -> tag.put("front_text", t));
            SignText.DIRECT_CODEC.encodeStart(ops, new SignText()).result().ifPresent(t -> tag.put("back_text", t));
            tag.putBoolean("is_waxed", true);
            sign.loadWithComponents(tag, level.registryAccess());
        }
    }

    void cobweb(WorldGenLevel level, BoundingBox box, int x, int y, int z, int chance) {
        if (this.roll(x, y, z, 7) < chance && level.isEmptyBlock(this.at(x, y, z))) this.set(level, box, x, y, z, st(Blocks.COBWEB));
    }

    // ---------------------------------------------------------------- the ground

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS) && !state.is(ModBlocks.SAGUARO.get());
    }

    /** The top of the ground in a column of the town (plants, trees and cacti aside), near the given height. */
    int surface(WorldGenLevel level, int x, int z, int near) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = near + 40; y > near - 40; y--) {
            if (isGround(level.getBlockState(pos.set(this.centre.getX() + x, y, this.centre.getZ() + z)))) return y;
        }
        return near;
    }

    /** Clear away whatever stands on the ground over it: plants, trees, cacti. */
    void clearAbove(WorldGenLevel level, BoundingBox box, int x, int y, int z, int height) {
        for (int i = 1; i <= height; i++) {
            BlockPos p = this.at(x, y + i, z);
            if (!box.isInside(p)) return;
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && !isGround(s)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    /**
     * Ready the ground for one part of the town, standing at floor f: on its high side the slope is cut away above
     * the floor, on its low side a sandstone foundation goes down to the ground, and anything growing there goes.
     */
    void foundation(WorldGenLevel level, BoundingBox box, int[] foot, int f, int height) {
        BlockState sand = st(ModBlocks.ORANGE_SAND.get()), sandstone = st(ModBlocks.ORANGE_SANDSTONE.get());
        for (int x = foot[0]; x <= foot[2]; x++) {
            for (int z = foot[1]; z <= foot[3]; z++) {
                if (!box.isInside(this.at(x, f, z))) continue;
                int ground = this.surface(level, x, z, f);
                // cut into the slope (and clear the plants) right up over the top of it
                for (int y = f + 1; y <= Math.max(ground, f) + height; y++) {
                    BlockPos p = this.at(x, y, z);
                    if (box.isInside(p) && !level.getBlockState(p).isAir()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
                // or build it up
                for (int y = Math.min(ground + 1, f); y <= f; y++) this.set(level, box, x, y, z, y == f ? sand : sandstone);
            }
        }
    }

    /** The street: packed coarse dirt and gravel, with sand drifting over it, following the ground. */
    void street(WorldGenLevel level, BoundingBox box) {
        int near = this.centre.getY();
        for (int x = -HALF_X + 4; x < CHURCH_X0 + 2; x++) {
            for (int z = -STREET; z <= STREET; z++) {
                if (!box.isInside(this.at(x, near, z))) continue;
                int g = this.surface(level, x, z, near);
                int r = this.roll(x, 0, z, 1);
                BlockState s = r < 55 ? st(Blocks.COARSE_DIRT) : r < 70 ? st(Blocks.GRAVEL) : r < 85 ? st(Blocks.DIRT_PATH)
                        : st(ModBlocks.ORANGE_SAND.get());
                this.set(level, box, x, g, z, s);
                this.clearAbove(level, box, x, g, z, 12);
            }
        }
        // the cross street through the middle, past the well and the water tower
        for (int x = -2; x <= 2; x++) {
            for (int z = -HALF_Z + 4; z <= HALF_Z - 4; z++) {
                if (Math.abs(z) <= STREET || !box.isInside(this.at(x, near, z))) continue;
                int g = this.surface(level, x, z, near);
                int r = this.roll(x, 0, z, 1);
                this.set(level, box, x, g, z, r < 60 ? st(Blocks.COARSE_DIRT) : st(ModBlocks.ORANGE_SAND.get()));
                this.clearAbove(level, box, x, g, z, 12);
            }
        }
    }

    // ---------------------------------------------------------------- the buildings

    void building(WorldGenLevel level, BoundingBox box, Building b, int f) {
        boolean joshua = b.kind() == Kind.STORE || b.kind() == Kind.BARBER || b.kind() == Kind.HOTEL;
        Block planks = joshua ? ModBlocks.JOSHUA_PLANKS.get() : ModBlocks.DEAD_OAK_PLANKS.get();
        Block logBlock = ModBlocks.DEAD_OAK_LOG.get();
        Block slabBlock = ModBlocks.DEAD_OAK_SLAB.get();
        Block fence = ModBlocks.DEAD_OAK_FENCE.get();
        Direction out = facing(b.side());
        int x0 = b.x0(), x1 = b.x1(), d = b.depth(), h = b.height(), s = b.side();
        int mid = (x0 + x1) / 2;
        boolean open = b.kind() == Kind.SMITHY || b.kind() == Kind.STABLE;
        // the floor, a step up off the street
        for (int x = x0; x <= x1; x++) {
            for (int v = 0; v < d; v++) {
                int z = rowZ(s, v);
                this.set(level, box, x, f, z, this.roll(x, f, z, 2) < 12 ? st(Blocks.COARSE_DIRT) : st(planks));
                for (int y = f + 1; y <= f + h; y++) this.set(level, box, x, y, z, st(Blocks.AIR));
            }
        }
        // the walls: log corners, plank walls with holes
        for (int x = x0; x <= x1; x++) {
            for (int v = 0; v < d; v++) {
                boolean edgeX = x == x0 || x == x1, edgeV = v == 0 || v == d - 1;
                if (!edgeX && !edgeV) continue;
                int z = rowZ(s, v);
                for (int y = f + 1; y <= f + h; y++) {
                    if (edgeX && edgeV) {
                        this.set(level, box, x, y, z, log(logBlock, Direction.Axis.Y));
                        continue;
                    }
                    if (open && v == 0 && y <= f + h - 1) continue;   // the smithy and stable are open to the street
                    int r = this.roll(x, y, z, 3);
                    // broken boards: more of them up high
                    if (r < (y > f + h - 2 ? 14 : 7)) continue;
                    this.set(level, box, x, y, z, st(planks));
                }
            }
        }
        // the upper floor of the two-storey buildings
        if (h >= 8) {
            for (int x = x0 + 1; x < x1; x++) {
                for (int v = 1; v < d - 1; v++) {
                    if (this.roll(x, f + 4, rowZ(s, v), 4) < 10 || (x == x1 - 1 && v >= d - 4)) continue;   // holes, and the stairwell
                    this.set(level, box, x, f + 4, rowZ(s, v), st(planks));
                }
            }
            // the stairs up, along the back of the side wall
            for (int i = 0; i < 3; i++) {
                Direction up = s < 0 ? Direction.NORTH : Direction.SOUTH;
                this.set(level, box, x1 - 1, f + 1 + i, rowZ(s, d - 4 + i), stair(ModBlocks.DEAD_OAK_STAIRS.get(), up, Half.BOTTOM));
            }
        }
        // the roof: flat, of slabs, fallen in here and there
        for (int x = x0; x <= x1; x++) {
            for (int v = 0; v < d; v++) {
                int z = rowZ(s, v);
                if (this.roll(x, f + h + 1, z, 5) < (b.kind() == Kind.HOUSE ? 22 : 12)) continue;
                this.set(level, box, x, f + h + 1, z, slab(slabBlock, SlabType.BOTTOM));
            }
        }
        // the false front: the front wall carried on up above the roof, stepped up in the middle
        for (int x = x0; x <= x1; x++) {
            int z = rowZ(s, 0);
            int extra = b.front() == 0 ? 0 : b.front() - (Math.abs(x - mid) > (x1 - x0) / 3 ? 1 : 0);
            for (int y = f + h + 1; y <= f + h + extra; y++) {
                if (this.roll(x, y, z, 3) < 6) continue;
                this.set(level, box, x, y, z, st(planks));
            }
            if (b.front() > 0) this.set(level, box, x, f + h + extra + 1, z, slab(slabBlock, SlabType.BOTTOM));
        }
        // windows: broken, the odd pane left
        int wy = f + 2;
        for (int x = x0 + 1; x < x1; x++) {
            if (open || Math.abs(x - mid) < 2 || (x - x0) % 3 != 1) continue;
            this.window(level, box, x, wy, rowZ(s, 0));
            if (h >= 8) this.window(level, box, x, f + 6, rowZ(s, 0));
        }
        for (int v = 2; v < d - 1; v += 3) {
            this.window(level, box, x0, wy, rowZ(s, v));
            this.window(level, box, x1, wy, rowZ(s, v));
        }
        // the door, if it's still there (the saloon has swinging half-doors)
        if (!open) {
            int z = rowZ(s, 0);
            if (b.kind() == Kind.SALOON) {
                this.set(level, box, mid, f + 1, z, trapdoor(ModBlocks.DEAD_OAK_TRAPDOOR.get(), out.getClockWise(), Half.TOP, true));
                this.set(level, box, mid, f + 2, z, st(Blocks.AIR));
            } else if (this.roll(mid, f, z, 6) < 65) {
                this.door(level, box, mid, f + 1, z, ModBlocks.DEAD_OAK_DOOR.get(), out.getOpposite(), this.roll(mid, f, z, 8) < 40);
            } else {
                this.set(level, box, mid, f + 1, z, st(Blocks.AIR));
                this.set(level, box, mid, f + 2, z, st(Blocks.AIR));
            }
        }
        // the boardwalk and its awning on posts
        for (int x = x0; x <= x1; x++) {
            for (int v = -2; v <= -1; v++) {
                int z = rowZ(s, v);
                if (this.roll(x, f, z, 9) < 15) continue;
                this.set(level, box, x, f, z, st(ModBlocks.DEAD_OAK_PLANKS.get()));
            }
            int post = rowZ(s, -2);
            if (b.kind() != Kind.STABLE && (x == x0 || x == x1 || x == mid - 2 || x == mid + 2)) {
                for (int y = f + 1; y < f + 4; y++) this.set(level, box, x, y, post, st(fence));
            }
            if (b.kind() != Kind.STABLE) {
                for (int v = -2; v <= -1; v++) {
                    int z = rowZ(s, v);
                    if (this.roll(x, f + 4, z, 10) < 25) continue;
                    this.set(level, box, x, f + 4, z, slab(slabBlock, SlabType.BOTTOM));
                }
            }
            // the hitching rail in front
            if (b.kind() == Kind.SALOON || b.kind() == Kind.HOTEL || b.kind() == Kind.SHERIFF) {
                int rail = rowZ(s, -3);
                if (x > x0 && x < x1 && x != mid && this.roll(x, f + 1, rail, 11) < 80) this.set(level, box, x, f + 1, rail, st(fence));
            }
        }
        // the name over the boardwalk
        if (!b.kind().sign.isEmpty()) {
            int sy = b.front() > 0 ? f + h + 1 : f + h - 1;
            this.sign(level, box, mid, sy, rowZ(s, -1), out, b.kind().sign);
        }
        this.inside(level, box, b, f, planks);
        // cobwebs in the corners
        for (int x = x0 + 1; x < x1; x++) {
            for (int v = 1; v < d - 1; v++) {
                boolean corner = x == x0 + 1 || x == x1 - 1 || v == 1 || v == d - 2;
                if (!corner) continue;
                this.cobweb(level, box, x, f + h, rowZ(s, v), 30);
                if (h >= 8) this.cobweb(level, box, x, f + 3, rowZ(s, v), 20);
            }
        }
    }

    void window(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        int r = this.roll(x, y, z, 12);
        this.set(level, box, x, y, z, r < 25 ? st(Blocks.GLASS_PANE) : r < 45 ? st(Blocks.COBWEB) : st(Blocks.AIR));
    }

    /** What's left inside each building. */
    void inside(WorldGenLevel level, BoundingBox box, Building b, int f, Block planks) {
        int s = b.side(), x0 = b.x0(), x1 = b.x1(), d = b.depth();
        Direction out = facing(s), in = out.getOpposite();
        int back = d - 2;
        switch (b.kind()) {
            case SALOON -> {
                // the bar along one side, stools, tables and a piano, the chest behind the bar
                for (int v = 2; v <= back - 1; v++) {
                    this.set(level, box, x0 + 2, f + 1, rowZ(s, v), log(ModBlocks.STRIPPED_DEAD_OAK_LOG.get(), Direction.Axis.Z));
                    if (this.roll(x0, f + 2, v, 13) < 35) this.set(level, box, x0 + 2, f + 2, rowZ(s, v), st(Blocks.FLOWER_POT));
                    if (v % 2 == 0) this.set(level, box, x0 + 3, f + 1, rowZ(s, v), st(ModBlocks.DEAD_OAK_FENCE.get()));
                }
                this.chest(level, box, x0 + 1, f + 1, rowZ(s, back), Direction.EAST, LOOT);
                this.barrel(level, box, x0 + 1, f + 1, rowZ(s, 2), null);
                this.barrel(level, box, x0 + 1, f + 2, rowZ(s, 2), null);
                for (int[] t : new int[][]{{x0 + 6, 3}, {x1 - 2, 3}, {x0 + 6, 6}}) {
                    this.set(level, box, t[0], f + 1, rowZ(s, t[1]), st(ModBlocks.DEAD_OAK_FENCE.get()));
                    this.set(level, box, t[0], f + 2, rowZ(s, t[1]), st(ModBlocks.DEAD_OAK_PRESSURE_PLATE.get()));
                    this.set(level, box, t[0] + 1, f + 1, rowZ(s, t[1]), stair(ModBlocks.DEAD_OAK_STAIRS.get(), Direction.EAST, Half.BOTTOM));
                }
                // an overturned chair, a skull on the bar
                this.set(level, box, x0 + 5, f + 1, rowZ(s, 5), stair(ModBlocks.DEAD_OAK_STAIRS.get(), Direction.SOUTH, Half.TOP));
                this.set(level, box, x0 + 2, f + 2, rowZ(s, back - 1), Blocks.SKELETON_SKULL.defaultBlockState()
                        .setValue(SkullBlock.ROTATION, 4));
                // the piano: a dark block with a slab top against the back wall
                this.set(level, box, x1 - 3, f + 1, rowZ(s, back), st(Blocks.DARK_OAK_PLANKS));
                this.set(level, box, x1 - 4, f + 1, rowZ(s, back), st(Blocks.DARK_OAK_PLANKS));
                this.set(level, box, x1 - 3, f + 2, rowZ(s, back), slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
                this.set(level, box, x1 - 4, f + 2, rowZ(s, back), slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
                // the rooms upstairs: two beds, one tipped over (just its frame)
                this.bed(level, box, x0 + 2, f + 5, rowZ(s, back), in, Blocks.RED_BED);
                this.bed(level, box, x0 + 5, f + 5, rowZ(s, back), in, Blocks.RED_BED);
                this.chest(level, box, x1 - 3, f + 5, rowZ(s, 2), out, LOOT);
            }
            case STORE -> {
                // the counter, and shelves of barrels and crates, mostly emptied
                for (int x = x0 + 2; x <= x1 - 2; x++) this.set(level, box, x, f + 1, rowZ(s, 3), st(planks));
                this.set(level, box, (x0 + x1) / 2, f + 1, rowZ(s, 3), st(Blocks.AIR));
                for (int x = x0 + 1; x <= x1 - 1; x++) {
                    if (this.roll(x, f, 0, 14) < 60) this.barrel(level, box, x, f + 1, rowZ(s, back), this.roll(x, f, 1, 14) < 30 ? LOOT : null);
                    if (this.roll(x, f, 2, 14) < 40) this.barrel(level, box, x, f + 2, rowZ(s, back), null);
                }
                this.set(level, box, x0 + 1, f + 1, rowZ(s, 2), st(Blocks.HAY_BLOCK));
            }
            case BANK -> {
                // the tellers' bars across the room, and the vault: iron, with its door hanging open
                for (int x = x0 + 1; x <= x1 - 1; x++) {
                    this.set(level, box, x, f + 1, rowZ(s, 3), st(planks));
                    if (this.roll(x, f + 2, 3, 15) < 80) this.set(level, box, x, f + 2, rowZ(s, 3), st(Blocks.IRON_BARS));
                }
                int vx0 = x1 - 3;
                for (int x = vx0; x <= x1 - 1; x++) {
                    for (int y = f + 1; y <= f + 3; y++) {
                        this.set(level, box, x, y, rowZ(s, back - 2), st(Blocks.IRON_BLOCK));
                    }
                }
                this.set(level, box, vx0 + 1, f + 1, rowZ(s, back - 2), st(Blocks.AIR));
                this.set(level, box, vx0 + 1, f + 2, rowZ(s, back - 2), st(Blocks.AIR));
                this.barrel(level, box, vx0 + 1, f + 1, rowZ(s, back), VAULT);
                this.set(level, box, vx0 + 2, f + 1, rowZ(s, back), st(Blocks.GOLD_BLOCK));
                this.set(level, box, x0 + 1, f + 1, rowZ(s, 1), st(Blocks.COBWEB));
            }
            case SHERIFF -> {
                // the desk, the gun rack and the jail cell, its bars bent open
                this.set(level, box, x0 + 2, f + 1, rowZ(s, 2), st(planks));
                this.set(level, box, x0 + 3, f + 1, rowZ(s, 2), st(planks));
                this.chest(level, box, x0 + 1, f + 1, rowZ(s, 2), in, LOOT);
                this.set(level, box, x0 + 2, f + 2, rowZ(s, 2), st(Blocks.LANTERN));
                for (int x = x0 + 3; x <= x1 - 1; x++) {
                    if (x == x0 + 4) continue;
                    for (int y = f + 1; y <= f + 3; y++) this.set(level, box, x, y, rowZ(s, back - 2), st(Blocks.IRON_BARS));
                }
                this.bed(level, box, x1 - 1, f + 1, rowZ(s, back), in, Blocks.WHITE_BED);
                this.set(level, box, x0 + 4, f + 1, rowZ(s, back), Blocks.SKELETON_SKULL.defaultBlockState()
                        .setValue(SkullBlock.ROTATION, 8));
            }
            case HOTEL -> {
                // the front desk, and rooms upstairs and down
                for (int x = x0 + 2; x <= x0 + 4; x++) this.set(level, box, x, f + 1, rowZ(s, 3), st(planks));
                this.set(level, box, x0 + 3, f + 2, rowZ(s, 3), st(Blocks.CANDLE));
                for (int x = x0 + 1; x <= x1 - 3; x += 3) {
                    this.bed(level, box, x, f + 5, rowZ(s, back), in, Blocks.BROWN_BED);
                }
                this.bed(level, box, x1 - 2, f + 1, rowZ(s, back), in, Blocks.BROWN_BED);
                this.chest(level, box, x0 + 1, f + 5, rowZ(s, 2), out, LOOT);
            }
            case UNDERTAKER -> {
                // coffins: open and shut, on the floor and stood up against the wall
                for (int x = x0 + 1; x <= x1 - 1; x += 2) {
                    for (int v = 2; v <= 3; v++) {
                        this.set(level, box, x, f + 1, rowZ(s, v), trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.NORTH, Half.BOTTOM,
                                this.roll(x, f, v, 16) < 30));
                    }
                }
                for (int x = x0 + 1; x <= x1 - 1; x++) {
                    if (this.roll(x, f, 9, 16) < 50) {
                        this.set(level, box, x, f + 1, rowZ(s, back), trapdoor(Blocks.DARK_OAK_TRAPDOOR, out, Half.BOTTOM, true));
                        this.set(level, box, x, f + 2, rowZ(s, back), trapdoor(Blocks.DARK_OAK_TRAPDOOR, out, Half.BOTTOM, true));
                    }
                }
                this.set(level, box, x1 - 1, f + 1, rowZ(s, 1), Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, 12));
            }
            case SMITHY -> {
                // the forge, the anvil and the quenching barrel
                this.set(level, box, x0 + 1, f + 1, rowZ(s, back), st(Blocks.BLAST_FURNACE).setValue(BlockStateProperties.HORIZONTAL_FACING, out));
                this.set(level, box, x0 + 2, f + 1, rowZ(s, back), st(Blocks.COBBLESTONE));
                this.set(level, box, x0 + 2, f + 2, rowZ(s, back), st(Blocks.COBBLESTONE_WALL));
                this.set(level, box, x0 + 3, f + 1, rowZ(s, 3), st(Blocks.DAMAGED_ANVIL));
                this.set(level, box, x1 - 1, f + 1, rowZ(s, back), st(Blocks.CAULDRON));
                this.set(level, box, x1 - 1, f + 1, rowZ(s, 2), st(Blocks.GRINDSTONE));
                this.chest(level, box, x1 - 1, f + 1, rowZ(s, back - 1), Direction.WEST, LOOT);
            }
            case STABLE -> {
                // stalls of fences, hay, an old saddle chest
                for (int x = x0 + 1; x <= x1 - 1; x += 3) {
                    for (int v = 2; v < d - 1; v++) this.set(level, box, x, f + 1, rowZ(s, v), st(ModBlocks.DEAD_OAK_FENCE.get()));
                }
                for (int x = x0 + 1; x <= x1 - 1; x++) {
                    if (this.roll(x, f, 3, 17) < 45) this.set(level, box, x, f + 1, rowZ(s, back), st(Blocks.HAY_BLOCK));
                }
                this.chest(level, box, x1 - 1, f + 1, rowZ(s, 1), in, LOOT);
            }
            case BARBER -> {
                // the barber's chairs and the mirror (a pane)
                this.set(level, box, x0 + 2, f + 1, rowZ(s, back), stair(ModBlocks.DEAD_OAK_STAIRS.get(), out, Half.BOTTOM));
                this.set(level, box, x0 + 4, f + 1, rowZ(s, back), stair(ModBlocks.DEAD_OAK_STAIRS.get(), out, Half.BOTTOM));
                this.set(level, box, x0 + 3, f + 1, rowZ(s, back), st(Blocks.CAULDRON));
                this.set(level, box, x0 + 3, f + 2, rowZ(s, back + 1), st(Blocks.GLASS_PANE));
            }
            case HOUSE -> {
                this.bed(level, box, x0 + 1, f + 1, rowZ(s, back), in, Blocks.LIGHT_GRAY_BED);
                this.set(level, box, x1 - 1, f + 1, rowZ(s, back), st(ModBlocks.DEAD_OAK_FENCE.get()));
                this.set(level, box, x1 - 1, f + 2, rowZ(s, back), st(ModBlocks.DEAD_OAK_PRESSURE_PLATE.get()));
                if (this.roll(x0, f, x1, 18) < 50) this.chest(level, box, x1 - 1, f + 1, rowZ(s, 1), in, LOOT);
            }
        }
    }

    void bed(WorldGenLevel level, BoundingBox box, int x, int y, int z, Direction headTowards, Block bed) {
        // the head here, against the back wall; the foot one block nearer the street
        BlockState foot = bed.defaultBlockState().setValue(BedBlock.FACING, headTowards).setValue(BedBlock.PART, BedPart.FOOT);
        this.set(level, box, x, y, z - headTowards.getStepZ(), foot);
        this.set(level, box, x, y, z, foot.setValue(BedBlock.PART, BedPart.HEAD));
    }

    // ---------------------------------------------------------------- the middle of town

    /** The town well, long dry. */
    void well(WorldGenLevel level, BoundingBox box, int f) {
        int cx = 0, cz = -8;
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                boolean rim = x != cx || z != cz;
                for (int y = f - 6; y <= f; y++) this.set(level, box, x, y, z, rim ? st(Blocks.COBBLESTONE) : st(Blocks.AIR));
                if (rim) this.set(level, box, x, f + 1, z, this.roll(x, f, z, 19) < 30 ? st(Blocks.MOSSY_COBBLESTONE) : st(Blocks.COBBLESTONE));
            }
        }
        this.set(level, box, cx, f + 1, cz, st(Blocks.AIR));
        this.set(level, box, cx, f - 6, cz, st(ModBlocks.ORANGE_SAND.get()));
        for (int[] p : new int[][]{{cx - 1, cz}, {cx + 1, cz}}) {
            for (int y = f + 2; y <= f + 3; y++) this.set(level, box, p[0], y, p[1], st(ModBlocks.DEAD_OAK_FENCE.get()));
        }
        this.fill(level, box, cx - 1, f + 4, cz, cx + 1, f + 4, cz, log(ModBlocks.DEAD_OAK_LOG.get(), Direction.Axis.X));
        this.set(level, box, cx, f + 3, cz, st(Blocks.CHAIN));
        for (int x = cx - 1; x <= cx + 1; x++) {
            this.set(level, box, x, f + 4, cz - 1, stair(ModBlocks.DEAD_OAK_STAIRS.get(), Direction.SOUTH, Half.BOTTOM));
            this.set(level, box, x, f + 4, cz + 1, stair(ModBlocks.DEAD_OAK_STAIRS.get(), Direction.NORTH, Half.BOTTOM));
        }
    }

    /** The water tower: a wooden tank on four tall legs, empty, with a ladder up. */
    void waterTower(WorldGenLevel level, BoundingBox box, int f) {
        int cx = 0, cz = 10;
        Block legLog = ModBlocks.DEAD_OAK_LOG.get();
        for (int[] p : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) {
            for (int y = f + 1; y <= f + 8; y++) this.set(level, box, cx + p[0], y, cz + p[1], log(legLog, Direction.Axis.Y));
        }
        // cross bracing
        for (int a = -1; a <= 1; a++) {
            this.set(level, box, cx + a, f + 4, cz - 2, st(ModBlocks.DEAD_OAK_FENCE.get()));
            this.set(level, box, cx + a, f + 4, cz + 2, st(ModBlocks.DEAD_OAK_FENCE.get()));
            this.set(level, box, cx - 2, f + 4, cz + a, st(ModBlocks.DEAD_OAK_FENCE.get()));
            this.set(level, box, cx + 2, f + 4, cz + a, st(ModBlocks.DEAD_OAK_FENCE.get()));
        }
        // the tank
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                this.set(level, box, cx + x, f + 9, cz + z, st(ModBlocks.DEAD_OAK_PLANKS.get()));
                boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
                boolean corner = Math.abs(x) == 2 && Math.abs(z) == 2;
                for (int y = f + 10; y <= f + 13; y++) {
                    if (corner) continue;
                    if (wall && this.roll(x, y, z, 20) >= 10) {
                        this.set(level, box, cx + x, y, cz + z, log(ModBlocks.STRIPPED_DEAD_OAK_LOG.get(), Direction.Axis.Y));
                    }
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (this.roll(x, f + 14, z, 21) < 70) this.set(level, box, cx + x, f + 14, cz + z, slab(ModBlocks.DEAD_OAK_SLAB.get(), SlabType.BOTTOM));
            }
        }
        this.cobweb(level, box, cx, f + 10, cz, 100);
        // the ladder up the north leg
        for (int y = f + 1; y <= f + 8; y++) {
            this.set(level, box, cx - 2, y, cz - 3, Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
        }
    }

    /** The church at the east end of the street: a plank nave with a bell tower over its door. */
    void church(WorldGenLevel level, BoundingBox box, int f) {
        int x0 = NAVE_X0, x1 = NAVE_X1, z0 = NAVE_Z0, z1 = NAVE_Z1, wall = NAVE_WALL;
        int half = (z1 - z0) / 2;
        Block planks = ModBlocks.DEAD_OAK_PLANKS.get();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                this.set(level, box, x, f, z, this.roll(x, f, z, 22) < 6 ? st(Blocks.COARSE_DIRT) : st(planks));
                for (int y = f + 1; y <= f + wall + half + 2; y++) this.set(level, box, x, y, z, st(Blocks.AIR));
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                if (!edge) continue;
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                boolean post = corner || ((z == z0 || z == z1) && (x - x0) % 6 == 0);
                for (int y = f + 1; y <= f + wall; y++) {
                    if (post) {
                        this.set(level, box, x, y, z, log(ModBlocks.DEAD_OAK_LOG.get(), Direction.Axis.Y));
                    } else if (this.roll(x, y, z, 23) >= 7) {
                        this.set(level, box, x, y, z, st(planks));
                    }
                }
            }
        }
        // tall windows down the sides and in the altar end, the door in the west end
        for (int x = x0 + 3; x < x1; x += 6) {
            for (int y = f + 2; y <= f + 5; y++) {
                this.window(level, box, x, y, z0);
                this.window(level, box, x, y, z1);
            }
        }
        for (int z : new int[]{-6, 6}) {
            for (int y = f + 2; y <= f + 5; y++) this.window(level, box, x1, y, z);
        }
        this.door(level, box, x0, f + 1, 0, ModBlocks.DEAD_OAK_DOOR.get(), Direction.EAST, true);
        this.set(level, box, x0, f + 1, 1, st(Blocks.AIR));
        this.set(level, box, x0, f + 2, 1, st(Blocks.AIR));
        // a steep gable roof of stairs, fallen in here and there
        for (int i = 0; i <= half + 1; i++) {
            for (int x = x0; x <= x1 + 1; x++) {
                int y = f + wall + 1 + i;
                if (this.roll(x, y, i, 24) < 8 || (x >= x0 + 12 && x <= x0 + 14 && i >= 3 && i <= 7)) continue;
                if (i <= half) {
                    this.set(level, box, x, y, z0 - 1 + i, stair(ModBlocks.DEAD_OAK_STAIRS.get(), Direction.SOUTH, Half.BOTTOM));
                    this.set(level, box, x, y, z1 + 1 - i, stair(ModBlocks.DEAD_OAK_STAIRS.get(), Direction.NORTH, Half.BOTTOM));
                } else {
                    this.set(level, box, x, y - 1, 0, st(planks));
                    this.set(level, box, x, y, 0, slab(ModBlocks.DEAD_OAK_SLAB.get(), SlabType.BOTTOM));
                }
            }
        }
        // the gable ends
        for (int i = 0; i < half; i++) {
            for (int z = z0 + i; z <= z1 - i; z++) {
                this.set(level, box, x1, f + wall + 1 + i, z, st(planks));
                this.set(level, box, x0, f + wall + 1 + i, z, st(planks));
            }
        }
        // the pentagram inlaid in the floor, candles on its points and the jukebox in its middle; the altar with the
        // chest holding the one record (DemonRaids: play that record at night...)
        for (int[] p : pentagram()) {
            char c = (char) p[2];
            if (c == 'R') {
                // the jukebox, right in the middle of the pentagram
                this.set(level, box, p[0], f + 1, p[1], st(Blocks.JUKEBOX));
                continue;
            }
            this.set(level, box, p[0], f, p[1], st(Blocks.RED_NETHER_BRICKS));
            if (c == 'Y') {
                this.set(level, box, p[0], f + 1, p[1], Blocks.CANDLE.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true)
                        .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3));
            }
        }
        for (int z = -2; z <= 2; z++) {
            if (z != 0) this.set(level, box, x1 - 1, f + 1, z, st(ModBlocks.STRIPPED_DEAD_OAK_LOG.get()));
        }
        this.chest(level, box, x1 - 1, f + 1, 0, Direction.WEST, ALTAR);
        for (int z : new int[]{-2, -1, 1, 2}) {
            this.set(level, box, x1 - 1, f + 2, z, Blocks.CANDLE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CandleBlock.LIT, true));
        }
        // the pews left by the door, either side of the aisle
        for (int x = x0 + 1; x <= x0 + 2; x++) {
            for (int z = z0 + 2; z <= z1 - 2; z++) {
                if (Math.abs(z) < 2 || this.roll(x, f + 1, z, 25) < 20) continue;
                this.set(level, box, x, f + 1, z, stair(ModBlocks.DEAD_OAK_STAIRS.get(), Direction.EAST, Half.BOTTOM));
            }
        }
        // the bell tower on the west end, over the door, standing above the roof
        int tx0 = x0 - 3, tx1 = x0 - 1;
        int top = f + wall + half + 4;
        for (int x = tx0; x <= tx1; x++) {
            for (int z = -1; z <= 1; z++) {
                boolean corner = (x == tx0 || x == tx1) && Math.abs(z) == 1;
                for (int y = f + 1; y <= top; y++) {
                    boolean belfry = y >= top - 3 && y <= top - 1;
                    if (corner) {
                        this.set(level, box, x, y, z, log(ModBlocks.DEAD_OAK_LOG.get(), Direction.Axis.Y));
                    } else if (x == tx1 - 1 && z == 0) {
                        this.set(level, box, x, y, z, st(Blocks.AIR));
                    } else if (!belfry && !(x == tx0 && z == 0 && y <= f + 2)) {
                        if (this.roll(x, y, z, 26) >= 6) this.set(level, box, x, y, z, st(planks));
                    } else {
                        this.set(level, box, x, y, z, st(Blocks.AIR));
                    }
                }
                this.set(level, box, x, f, z, st(planks));
            }
        }
        // the way through, from the tower's west door into the nave
        this.set(level, box, tx1, f + 1, 0, st(Blocks.AIR));
        this.set(level, box, tx1, f + 2, 0, st(Blocks.AIR));
        this.door(level, box, tx0, f + 1, 0, ModBlocks.DEAD_OAK_DOOR.get(), Direction.EAST, false);
        this.fill(level, box, tx0, top, -1, tx1, top, 1, st(planks));
        this.set(level, box, tx1 - 1, top - 1, 0, Blocks.BELL.defaultBlockState().setValue(BellBlock.FACING, Direction.NORTH)
                .setValue(BellBlock.ATTACHMENT, BellAttachType.CEILING));
        // a cap, and the cross on top
        for (int x = tx0; x <= tx1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == tx1 - 1 && z == 0) continue;
                this.set(level, box, x, top + 1, z, slab(ModBlocks.DEAD_OAK_SLAB.get(), SlabType.BOTTOM));
            }
        }
        this.set(level, box, tx1 - 1, top + 1, 0, st(planks));
        for (int y = top + 2; y <= top + 4; y++) this.set(level, box, tx1 - 1, y, 0, st(ModBlocks.DEAD_OAK_FENCE.get()));
        this.set(level, box, tx1 - 1, top + 3, -1, st(ModBlocks.DEAD_OAK_FENCE.get()));
        this.set(level, box, tx1 - 1, top + 3, 1, st(ModBlocks.DEAD_OAK_FENCE.get()));
        for (int x = x0 + 1; x < x1; x++) {
            this.cobweb(level, box, x, f + wall, z0 + 1, 30);
            this.cobweb(level, box, x, f + wall, z1 - 1, 30);
        }
    }

    /** The gallows at the west end of the street. */
    void gallows(WorldGenLevel level, BoundingBox box, int f) {
        int x0 = -40, x1 = -36, z0 = -2, z1 = 2;
        Block planks = ModBlocks.DEAD_OAK_PLANKS.get();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                for (int y = f + 1; y <= f + 2; y++) {
                    if (corner) this.set(level, box, x, y, z, log(ModBlocks.DEAD_OAK_LOG.get(), Direction.Axis.Y));
                }
                // the platform, with the trapdoor
                this.set(level, box, x, f + 3, z, x == x0 + 2 && z == 0
                        ? trapdoor(ModBlocks.DEAD_OAK_TRAPDOOR.get(), Direction.EAST, Half.TOP, true) : st(planks));
            }
        }
        // the steps up
        for (int i = 0; i < 3; i++) {
            this.set(level, box, x1 + 1 + (2 - i), f + 1 + i, 2, stair(ModBlocks.DEAD_OAK_STAIRS.get(), Direction.WEST, Half.BOTTOM));
        }
        // the frame and the noose
        for (int y = f + 4; y <= f + 8; y++) this.set(level, box, x0, y, 0, log(ModBlocks.DEAD_OAK_LOG.get(), Direction.Axis.Y));
        for (int x = x0 + 1; x <= x0 + 2; x++) this.set(level, box, x, f + 8, 0, log(ModBlocks.DEAD_OAK_LOG.get(), Direction.Axis.X));
        this.set(level, box, x0 + 1, f + 7, 0, st(ModBlocks.DEAD_OAK_FENCE.get()));
        this.set(level, box, x0 + 2, f + 7, 0, st(Blocks.CHAIN));
        this.set(level, box, x0 + 2, f + 6, 0, st(Blocks.CHAIN));
        this.set(level, box, x0 + 2, f + 5, 0, st(Blocks.LANTERN).setValue(BlockStateProperties.HANGING, true));
    }

    /** Boot Hill: the graveyard out behind the south row. */
    void graveyard(WorldGenLevel level, BoundingBox box) {
        int x0 = -12, x1 = 4, z0 = 15, z1 = 24;
        int near = this.centre.getY();
        Block fence = ModBlocks.DEAD_OAK_FENCE.get();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!box.isInside(this.at(x, near, z))) continue;
                int g = this.surface(level, x, z, near);
                this.clearAbove(level, box, x, g, z, 12);
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                if (edge && !(z == z0 && (x == x0 + 8 || x == x0 + 9)) && this.roll(x, 1, z, 27) >= 15) {
                    this.set(level, box, x, g + 1, z, st(fence));
                }
            }
        }
        if (box.isInside(this.at(x0 + 7, near, z0 - 1))) {
            this.sign(level, box, x0 + 7, this.surface(level, x0 + 7, z0 - 1, near) + 2, z0 - 1, Direction.NORTH, "BOOT HILL");
        }
        for (int z = z0 + 2; z <= z1 - 2; z += 3) {
            for (int x = x0 + 2; x <= x1 - 2; x += 2) {
                if (!box.isInside(this.at(x, near, z))) continue;
                int r = this.roll(x, 0, z, 28);
                if (r < 15) continue;
                int g = this.surface(level, x, z, near);
                // the headstone: a cross of fence, or a slab of stone
                if (r < 55) {
                    this.set(level, box, x, g + 1, z, st(fence));
                    this.set(level, box, x, g + 2, z, st(fence));
                } else if (r < 85) {
                    this.set(level, box, x, g + 1, z, st(Blocks.COBBLESTONE_WALL));
                } else {
                    this.set(level, box, x, g + 1, z, st(Blocks.MOSSY_STONE_BRICK_WALL));
                }
                // the mound
                for (int k = 1; k <= 2; k++) {
                    if (!box.isInside(this.at(x, near, z + k))) continue;
                    int gk = this.surface(level, x, z + k, near);
                    this.set(level, box, x, gk, z + k, st(Blocks.COARSE_DIRT));
                    if (k == 1 && r % 7 == 0) this.set(level, box, x, gk + 1, z + k, st(Blocks.DEAD_BUSH));
                    if (k == 2 && r % 11 == 0) {
                        this.set(level, box, x, gk + 1, z + k, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, r % 16));
                    }
                }
            }
        }
    }

    /** Dead bushes in the street, barrels by the boardwalks, an abandoned wagon. */
    void litter(WorldGenLevel level, BoundingBox box) {
        int near = this.centre.getY();
        for (int x = -HALF_X + 4; x < CHURCH_X0; x++) {
            for (int z = -STREET; z <= STREET; z++) {
                if (this.roll(x, 1, z, 29) >= 4 || !box.isInside(this.at(x, near, z))) continue;
                int g = this.surface(level, x, z, near);
                if (level.isEmptyBlock(this.at(x, g + 1, z))) this.set(level, box, x, g + 1, z, st(Blocks.DEAD_BUSH));
            }
        }
        // barrels on the boardwalks
        for (int i = 0; i < BUILDINGS.length; i++) {
            Building b = BUILDINGS[i];
            int x = b.x0() + 1 + Math.floorMod(this.roll(b.x0(), 0, b.side(), 30), Math.max(1, b.x1() - b.x0() - 1));
            int z = rowZ(b.side(), -1);
            BlockPos p = this.at(x, this.floors[i] + 1, z);
            if (box.isInside(p) && level.isEmptyBlock(p) && this.roll(x, 0, z, 31) < 60) this.barrel(level, box, x, this.floors[i] + 1, z, null);
        }
        // the wagon in the street: a bed of slabs on four wheels (bare trapdoors), its cover long gone
        int wx = -6, wz = 0;
        for (int x = wx - 1; x <= wx + 3; x++) {
            if (!box.isInside(this.at(x, near, wz))) continue;
            int g = this.surface(level, x, wz, near);
            if (x == wx - 1) {
                this.set(level, box, x, g + 1, wz, st(ModBlocks.DEAD_OAK_FENCE.get()));
                continue;
            }
            this.set(level, box, x, g + 2, wz, slab(ModBlocks.DEAD_OAK_SLAB.get(), SlabType.BOTTOM));
            if (x == wx || x == wx + 3) {
                this.set(level, box, x, g + 1, wz - 1, trapdoor(ModBlocks.DEAD_OAK_TRAPDOOR.get(), Direction.NORTH, Half.BOTTOM, true));
                this.set(level, box, x, g + 1, wz + 1, trapdoor(ModBlocks.DEAD_OAK_TRAPDOOR.get(), Direction.SOUTH, Half.BOTTOM, true));
            }
        }
    }
}
