package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A Thai floating village, built on a river where it runs past the monsoon forest and its rice terraces.
 *
 * <ul>
 *   <li>A lagoon about 43 blocks across is dug out of the river and its bank: still water three blocks deep
 *       on a mud bed, with a low bank around it.</li>
 *   <li>Everything stands on fence legs (mahogany or bamboo) rising out of the water: a square bamboo deck in
 *       the middle with the village bell, and from it four railed boardwalks out to the four sides.</li>
 *   <li>Along both sides of each boardwalk stand two stilt houses (sixteen in all), each joined to the boardwalk
 *       by a short gangway: a bamboo floor, coconut palm walls on mahogany corner posts, a door and windows, and
 *       a very steep gabled mahogany roof whose ridge ends and eaves turn up in horn-like finials, as on
 *       traditional Thai houses. The cleric's house is a little temple: mahogany walls, a pale roof and a spire.</li>
 *   <li>Thirteen houses each have one profession's workstation and a bed; three are homes with two beds.
 *       There are no crop fields: the village's farmers work the rice terraces next to it (ThaiFarmers).</li>
 *   <li>One Thai villager per house. They are ordinary villagers and take up the professions as usual.</li>
 * </ul>
 */
public class ThaiVillagePiece extends StructurePiece {
    private static final int HALF = 21;
    private static final int CORNER_RADIUS = 27;
    private static final int BOX = HALF + 3;
    private static final int CLEAR_HEIGHT = 34;
    private static final int ARM_END = 19;
    private static final int[] HUT_STARTS = {6, 13};
    private static final int HUT_SIZE = 5;
    // the landing stage to the shore: the boardwalk carries on past the lagoon to here, then a stair climbs the bank
    private static final int LANDING_END = 28;
    private static final int RAMP_MAX = 10;
    private static final int REACH = LANDING_END + RAMP_MAX + 2;
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> HOUSE_LOOT =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("koppenclimatemod", "chests/thai_village_house"));

    // every vanilla workstation, so every profession is possible
    private static final Block[] WORKSTATIONS = {
            Blocks.COMPOSTER, Blocks.LECTERN, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CARTOGRAPHY_TABLE,
            Blocks.BREWING_STAND, Blocks.BARREL, Blocks.FLETCHING_TABLE, Blocks.CAULDRON, Blocks.STONECUTTER,
            Blocks.LOOM, Blocks.SMITHING_TABLE, Blocks.GRINDSTONE};
    private static final int HOMES = 3;

    private final BlockPos centre;
    private final long seed;
    // the boardwalk that carries on to the shore (toward the rice terraces), or null for none
    private final Direction landing;

    public ThaiVillagePiece(BlockPos centre, long seed, Direction landing) {
        super(VillageRegistry.THAI_VILLAGE_PIECE.get(), 0, box(centre, landing));
        this.centre = centre;
        this.seed = seed;
        this.landing = landing;
    }

    public ThaiVillagePiece(CompoundTag tag) {
        super(VillageRegistry.THAI_VILLAGE_PIECE.get(), tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.seed = tag.getLong("Layout");
        this.landing = tag.contains("Landing") ? Direction.from2DDataValue(tag.getInt("Landing")) : null;
    }

    private static BoundingBox box(BlockPos centre, Direction landing) {
        BoundingBox box = new BoundingBox(
                centre.getX() - BOX, centre.getY() - 10, centre.getZ() - BOX,
                centre.getX() + BOX, centre.getY() + CLEAR_HEIGHT, centre.getZ() + BOX);
        if (landing != null) {
            BlockPos end = centre.relative(landing, REACH);
            box.encapsulate(new BlockPos(end.getX() - 3, centre.getY() - 10, end.getZ() - 3));
            box.encapsulate(new BlockPos(end.getX() + 3, centre.getY() + CLEAR_HEIGHT, end.getZ() + 3));
        }
        return box;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.centre.getX());
        tag.putInt("CY", this.centre.getY());
        tag.putInt("CZ", this.centre.getZ());
        tag.putLong("Layout", this.seed);
        if (this.landing != null) tag.putInt("Landing", this.landing.get2DDataValue());
    }

    // ---------------------------------------------------------------- layout

    /** One house: on which boardwalk, which side of it, how far out, and what it holds (null: a home). */
    record House(Direction arm, int side, int start, Block workstation, boolean bambooLegs) {
    }

    private List<House> houses() {
        RandomSource random = RandomSource.create(this.seed);
        List<Block> uses = new ArrayList<>(List.of(WORKSTATIONS));
        for (int i = 0; i < HOMES; i++) uses.add(null);
        for (int i = uses.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            Block t = uses.get(i);
            uses.set(i, uses.get(j));
            uses.set(j, t);
        }
        List<House> houses = new ArrayList<>();
        int n = 0;
        for (Direction arm : Direction.Plane.HORIZONTAL) {
            for (int side = -1; side <= 1; side += 2) {
                for (int start : HUT_STARTS) {
                    houses.add(new House(arm, side, start, uses.get(n++), random.nextBoolean()));
                }
            }
        }
        return houses;
    }

    // ---------------------------------------------------------------- building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int water = this.centre.getY();
        int deck = water + 2;
        this.digLagoon(level, box, water);
        this.buildCentre(level, box, water, deck);
        for (Direction arm : Direction.Plane.HORIZONTAL) {
            this.buildArm(level, box, arm, water, deck);
        }
        if (this.landing != null) this.buildLanding(level, box, this.landing, water, deck);
        for (House house : this.houses()) {
            this.buildHouse(level, box, house, water, deck, random);
            this.spawnVillager(level, box, this.at(house.arm(), house.side(), house.start() + 2, 5, deck + 1));
        }
    }

    private boolean inLagoon(int dx, int dz) {
        return Math.abs(dx) <= HALF && Math.abs(dz) <= HALF && dx * dx + dz * dz <= CORNER_RADIUS * CORNER_RADIUS;
    }

    /** Still water three deep on a mud bed, cleared to the sky, inside a low bank. */
    private void digLagoon(WorldGenLevel level, BoundingBox box, int water) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState mud = Blocks.MUD.defaultBlockState();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                int dx = x - this.centre.getX(), dz = z - this.centre.getZ();
                if (this.inLagoon(dx, dz)) {
                    for (int y = water + 1; y <= water + CLEAR_HEIGHT; y++) {
                        pos.set(x, y, z);
                        if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                    for (int y = water - 2; y <= water; y++) level.setBlock(pos.set(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                    level.setBlock(pos.set(x, water - 3, z), mud, 2);
                    for (int y = water - 4; y > water - 10 && !solid(level.getBlockState(pos.set(x, y, z))); y--) {
                        level.setBlock(pos, mud, 2);
                    }
                } else if (this.inLagoon(dx - Integer.signum(dx) * 3, dz - Integer.signum(dz) * 3)) {
                    // the bank: no lower than the water, no higher than a block above it near the lagoon
                    int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                    if (!level.getFluidState(pos.set(x, top + 1, z)).isEmpty()) continue;
                    for (int y = top + 1; y <= water; y++) level.setBlock(pos.set(x, y, z), mud, 2);
                    if (top > water + 1) {
                        for (int y = water + 2; y <= Math.min(top + 2, water + CLEAR_HEIGHT); y++) {
                            pos.set(x, y, z);
                            if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                        }
                        level.setBlock(pos.set(x, water + 1, z), ModBlocks.OXISOL_GRASS.get().defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static boolean solid(BlockState state) {
        return state.blocksMotion() && state.getFluidState().isEmpty();
    }

    /** The middle deck, 9x9, railed except where the boardwalks leave it, with the bell and four torch posts. */
    private void buildCentre(WorldGenLevel level, BoundingBox box, int water, int deck) {
        BlockState floor = Blocks.BAMBOO_MOSAIC.defaultBlockState();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                this.set(level, box, this.centre.getX() + dx, deck, this.centre.getZ() + dz, floor);
                if ((Math.abs(dx) == 4 || dx == 0) && (Math.abs(dz) == 4 || dz == 0)) {
                    this.legs(level, box, this.centre.getX() + dx, this.centre.getZ() + dz, water, deck, Blocks.BAMBOO_FENCE);
                }
            }
        }
        for (int dx = -4; dx <= 4; dx += 8) {
            for (int dz = -4; dz <= 4; dz += 8) {
                this.set(level, box, this.centre.getX() + dx, deck + 1, this.centre.getZ() + dz, Blocks.BAMBOO_FENCE.defaultBlockState());
                this.set(level, box, this.centre.getX() + dx, deck + 2, this.centre.getZ() + dz, Blocks.TORCH.defaultBlockState());
            }
        }
        this.set(level, box, this.centre.getX(), deck + 1, this.centre.getZ(), Blocks.BELL.defaultBlockState()
                .setValue(BellBlock.ATTACHMENT, BellAttachType.FLOOR).setValue(BellBlock.FACING, Direction.NORTH));
    }

    /** A boardwalk three wide from the middle deck out to the end, railed along both edges, on bamboo legs. */
    private void buildArm(WorldGenLevel level, BoundingBox box, Direction arm, int water, int deck) {
        BlockState floor = Blocks.BAMBOO_MOSAIC.defaultBlockState();
        for (int a = 5; a <= ARM_END; a++) {
            for (int v = -1; v <= 1; v++) {
                BlockPos p = this.at(arm, 1, a, v, deck);
                this.set(level, box, p, floor);
                if (v != 0 && a % 3 == 0) this.legs(level, box, p.getX(), p.getZ(), water, deck, Blocks.BAMBOO_FENCE);
            }
            for (int side = -1; side <= 1; side += 2) {
                if (isGangway(a)) {
                    // the gangway over to the house's door
                    BlockPos g = this.at(arm, side, a, 2, deck);
                    this.set(level, box, g, floor);
                    this.legs(level, box, g.getX(), g.getZ(), water, deck, Blocks.BAMBOO_FENCE);
                    continue;
                }
                this.railing(level, box, this.at(arm, side, a, 2, deck + 1), arm, a > 5 && !isGangway(a - 1), !isGangway(a + 1));
            }
        }
        // the far end is railed across, except where the boardwalk goes on to the shore
        if (arm == this.landing) return;
        for (int v = -2; v <= 2; v++) {
            this.railing(level, box, this.at(arm, 1, ARM_END + 1, v, deck + 1), arm.getClockWise(), v > -2, v < 2);
        }
    }

    /**
     * The way ashore: the boardwalk goes on, railed and on legs, across the rest of the lagoon and its bank to
     * {@link #LANDING_END}; from there, wherever the land is higher, a mahogany stair is cut into it, a block up
     * for each block on, until it meets the ground. Every column is worked out on its own, so it builds the same
     * whichever chunk is generated first.
     */
    private void buildLanding(WorldGenLevel level, BoundingBox box, Direction arm, int water, int deck) {
        BlockState floor = Blocks.BAMBOO_MOSAIC.defaultBlockState();
        BlockState fill = ModBlocks.OXISOL.get().defaultBlockState();
        for (int a = ARM_END + 1; a <= LANDING_END + RAMP_MAX; a++) {
            boolean ramp = a > LANDING_END;
            int step = ramp ? deck + (a - LANDING_END) : deck;
            for (int v = -2; v <= 2; v++) {
                BlockPos p = this.at(arm, 1, a, v, step);
                if (!box.isInside(p)) continue;
                int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, p.getX(), p.getZ()) - 1;
                if (ramp && ground < step) {
                    // the stair has met the ground here: nothing more to cut
                    continue;
                }
                // headroom over the walk
                for (int y = step + 1; y <= step + 4; y++) {
                    BlockPos q = new BlockPos(p.getX(), y, p.getZ());
                    if (!level.getBlockState(q).isAir()) level.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
                }
                if (Math.abs(v) == 2) {
                    if (!ramp) this.railing(level, box, p.above(), arm, true, a < LANDING_END);
                    continue;
                }
                if (!ramp) {
                    level.setBlock(p, floor, 2);
                    if (a <= HALF) {
                        if (v != 0 && a % 3 == 0) this.legs(level, box, p.getX(), p.getZ(), water, deck, Blocks.BAMBOO_FENCE);
                    } else {
                        // on the bank and beyond: banked up with earth where the ground is lower, cut level into it
                        // where it is higher
                        for (int y = step - 1; y > ground && y > step - 6; y--) level.setBlock(new BlockPos(p.getX(), y, p.getZ()), fill, 2);
                    }
                    continue;
                }
                level.setBlock(p, ModBlocks.MAHOGANY_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, arm), 2);
                for (int y = step - 1; y > ground && y > step - 6; y--) level.setBlock(new BlockPos(p.getX(), y, p.getZ()), fill, 2);
            }
        }
    }

    private static boolean isGangway(int a) {
        for (int start : HUT_STARTS) {
            if (a == start + 2) return true;
        }
        return false;
    }

    /** One house on its platform of fence legs, with its steep gabled roof. */
    private void buildHouse(WorldGenLevel level, BoundingBox box, House h, int water, int deck, RandomSource random) {
        boolean temple = h.workstation() == Blocks.BREWING_STAND;
        Block legs = h.bambooLegs() ? Blocks.BAMBOO_FENCE : ModBlocks.MAHOGANY_FENCE.get();
        BlockState floor = Blocks.BAMBOO_PLANKS.defaultBlockState();
        BlockState wall = (temple ? ModBlocks.MAHOGANY_PLANKS : ModBlocks.COCONUT_PALM_PLANKS).get().defaultBlockState();
        BlockState post = ModBlocks.MAHOGANY_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        Block roofPlanks = (temple ? ModBlocks.COCONUT_PALM_PLANKS : ModBlocks.MAHOGANY_PLANKS).get();
        Block roofStairs = (temple ? ModBlocks.COCONUT_PALM_STAIRS : ModBlocks.MAHOGANY_STAIRS).get();
        Block roofSlab = (temple ? ModBlocks.COCONUT_PALM_SLAB : ModBlocks.MAHOGANY_SLAB).get();
        Block finial = ModBlocks.MAHOGANY_FENCE.get();
        int u0 = h.start(), u1 = u0 + HUT_SIZE - 1;
        Direction armDir = h.arm();
        Direction out = this.sideDirection(armDir, h.side());       // from the boardwalk out through the house

        // floor and legs
        for (int a = u0; a <= u1; a++) {
            for (int v = 3; v <= 7; v++) {
                BlockPos p = this.at(armDir, h.side(), a, v, deck);
                this.set(level, box, p, floor);
                if ((a == u0 || a == u1 || a == u0 + 2) && (v == 3 || v == 7 || v == 5)) {
                    this.legs(level, box, p.getX(), p.getZ(), water, deck, legs);
                }
            }
        }
        // walls: corner posts, planks between, a door facing the boardwalk, windows in the sides and back
        for (int y = deck + 1; y <= deck + 4; y++) {
            for (int a = u0; a <= u1; a++) {
                for (int v = 3; v <= 7; v++) {
                    boolean edgeA = a == u0 || a == u1, edgeV = v == 3 || v == 7;
                    if (!edgeA && !edgeV) {
                        this.set(level, box, this.at(armDir, h.side(), a, v, y), Blocks.AIR.defaultBlockState());
                        continue;
                    }
                    BlockState s = edgeA && edgeV ? post : wall;
                    if (y == deck + 4 && !edgeV) continue;  // only the gables go above the eaves line... handled by the roof
                    boolean window = y == deck + 2 && ((edgeA && v == 5) || (v == 7 && a == u0 + 2));
                    if (window) s = Blocks.AIR.defaultBlockState();
                    this.set(level, box, this.at(armDir, h.side(), a, v, y), s);
                }
            }
        }
        BlockState door = ModBlocks.COCONUT_PALM_DOOR.get().defaultBlockState()
                .setValue(DoorBlock.FACING, out.getOpposite()).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
        this.set(level, box, this.at(armDir, h.side(), u0 + 2, 3, deck + 1), door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.set(level, box, this.at(armDir, h.side(), u0 + 2, 3, deck + 2), door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));

        // the roof: ridge running from the front gable to the back, slopes falling to the sides two blocks for
        // every one across, overhanging by one all round
        Direction alongDir = armDir;    // +a
        for (int a = u0 - 1; a <= u1 + 1; a++) {
            int d = Math.min(a - (u0 - 1), (u1 + 1) - a);
            int top = deck + 4 + 2 * d;
            for (int v = 2; v <= 8; v++) {
                this.set(level, box, this.at(armDir, h.side(), a, v, top - 1), roofPlanks.defaultBlockState());
                if (d < 3) {
                    Direction toRidge = a < u0 + 2 ? alongDir : alongDir.getOpposite();
                    this.set(level, box, this.at(armDir, h.side(), a, v, top), roofStairs.defaultBlockState()
                            .setValue(StairBlock.FACING, toRidge));
                } else {
                    this.set(level, box, this.at(armDir, h.side(), a, v, top), roofSlab.defaultBlockState()
                            .setValue(SlabBlock.TYPE, SlabType.BOTTOM));
                }
                // the walls and gables close up under the roof
                boolean inside = a >= u0 && a <= u1 && v >= 3 && v <= 7;
                boolean gable = v == 3 || v == 7;
                boolean sideWall = a == u0 || a == u1;
                if (inside && (gable || sideWall)) {
                    for (int y = deck + 4; y <= top - 2; y++) {
                        this.set(level, box, this.at(armDir, h.side(), a, v, y), (gable && sideWall) ? post : wall);
                    }
                }
            }
            // upturned eave tips at the four corners
            if (d == 0) {
                this.set(level, box, this.at(armDir, h.side(), a, 2, top + 1), finial.defaultBlockState());
                this.set(level, box, this.at(armDir, h.side(), a, 8, top + 1), finial.defaultBlockState());
            }
        }
        // horn finials at both ends of the ridge, and the temple's spire
        int ridgeTop = deck + 10;
        for (int v : new int[]{2, 8}) {
            // a double slab under each horn, so it stands on a full block rather than over a half-block gap
            this.set(level, box, this.at(armDir, h.side(), u0 + 2, v, ridgeTop), roofSlab.defaultBlockState()
                    .setValue(SlabBlock.TYPE, SlabType.DOUBLE));
            this.set(level, box, this.at(armDir, h.side(), u0 + 2, v, ridgeTop + 1), finial.defaultBlockState());
            this.set(level, box, this.at(armDir, h.side(), u0 + 2, v, ridgeTop + 2), finial.defaultBlockState());
        }
        if (temple) {
            for (int y = ridgeTop + 1; y <= ridgeTop + 4; y++) {
                this.set(level, box, this.at(armDir, h.side(), u0 + 2, 5, y), finial.defaultBlockState());
            }
        }

        // inside: a bed (two in a home), the workstation, a torch
        Direction back = out;
        this.bed(level, box, this.at(armDir, h.side(), u0 + 1, 5, deck + 1), back);
        if (h.workstation() == null) {
            this.bed(level, box, this.at(armDir, h.side(), u0 + 3, 5, deck + 1), back);
        } else {
            BlockState station = h.workstation().defaultBlockState();
            if (station.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                station = station.setValue(BlockStateProperties.HORIZONTAL_FACING, back.getOpposite());
            }
            this.set(level, box, this.at(armDir, h.side(), u0 + 3, 6, deck + 1), station);
        }
        // a chest of household things by the door
        BlockPos chest = this.at(armDir, h.side(), u0 + 3, 4, deck + 1);
        if (box.isInside(chest)) {
            level.setBlock(chest, Blocks.CHEST.defaultBlockState().setValue(net.minecraft.world.level.block.ChestBlock.FACING,
                    alongDir.getOpposite()), 2);
            net.minecraft.world.RandomizableContainer.setBlockEntityLootTable(level, random, chest, HOUSE_LOOT);
        }
        this.set(level, box, this.at(armDir, h.side(), u0 + 3, 4, deck + 3),
                Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, alongDir.getOpposite()));
    }

    // ---------------------------------------------------------------- helpers

    /** World position of a spot given along a boardwalk (a), out to one side of it (v), at height y. */
    private BlockPos at(Direction arm, int side, int a, int v, int y) {
        Direction across = this.sideDirection(arm, side);
        return new BlockPos(this.centre.getX() + arm.getStepX() * a + across.getStepX() * v, y,
                this.centre.getZ() + arm.getStepZ() * a + across.getStepZ() * v);
    }

    private Direction sideDirection(Direction arm, int side) {
        return side > 0 ? arm.getClockWise() : arm.getCounterClockWise();
    }

    /** A fence leg from the lagoon bed up to the underside of the deck (waterlogged where it stands in water). */
    private void legs(WorldGenLevel level, BoundingBox box, int x, int z, int water, int deck, Block fence) {
        for (int y = water - 2; y < deck; y++) {
            BlockState s = fence.defaultBlockState();
            if (y <= water && s.hasProperty(BlockStateProperties.WATERLOGGED)) s = s.setValue(BlockStateProperties.WATERLOGGED, true);
            this.set(level, box, new BlockPos(x, y, z), s);
        }
    }

    /** One railing post, joined to the posts before and after it along the given direction. */
    private void railing(WorldGenLevel level, BoundingBox box, BlockPos pos, Direction along, boolean joinBack, boolean joinAhead) {
        BlockState s = Blocks.BAMBOO_FENCE.defaultBlockState();
        if (joinAhead) s = s.setValue(propertyFor(along), true);
        if (joinBack) s = s.setValue(propertyFor(along.getOpposite()), true);
        this.set(level, box, pos, s);
    }

    private static net.minecraft.world.level.block.state.properties.BooleanProperty propertyFor(Direction d) {
        return switch (d) {
            case NORTH -> CrossCollisionBlock.NORTH;
            case SOUTH -> CrossCollisionBlock.SOUTH;
            case EAST -> CrossCollisionBlock.EAST;
            default -> CrossCollisionBlock.WEST;
        };
    }

    /** A bed with its foot at pos and its head toward the given direction. */
    private void bed(WorldGenLevel level, BoundingBox box, BlockPos foot, Direction toward) {
        BlockState bed = Blocks.BROWN_BED.defaultBlockState().setValue(BedBlock.FACING, toward);
        this.set(level, box, foot, bed.setValue(BedBlock.PART, BedPart.FOOT));
        this.set(level, box, foot.relative(toward), bed.setValue(BedBlock.PART, BedPart.HEAD));
    }

    private void spawnVillager(WorldGenLevel level, BoundingBox box, BlockPos pos) {
        if (!box.isInside(pos)) return;
        Villager villager = EntityType.VILLAGER.create(level.getLevel());
        if (villager == null) return;
        villager.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setVillagerData(villager.getVillagerData().setType(VillageRegistry.THAI.get()).setProfession(VillagerProfession.NONE));
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }

    private void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) level.setBlock(pos, state, 2);
    }

    private void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        this.set(level, box, new BlockPos(x, y, z), state);
    }
}
