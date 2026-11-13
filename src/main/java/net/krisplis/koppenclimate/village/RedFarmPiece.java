package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.krisplis.koppenclimate.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A red farm of the American South (Cfa), on its own big patch of mown grass in the American farmland:
 * <ul>
 *   <li>a great red barn - red painted planks, with white painted planks for its corner posts, studs, trim and door
 *       frames - under a gambrel roof with a white cupola and a weathervane on its ridge: big doors at both gable
 *       ends (the south ones flanked by white-braced sliding door panels), a hayloft door with its hay hood above,
 *       stalls with haylofts over them down both sides of the aisle, and at the north end a great pile of hay.
 *       Hidden at the heart of the pile's bottom layer is a chest with a shotgun and a box of copper bullets;</li>
 *   <li>two stone-stave silos beside the barn;</li>
 *   <li>the farmer's house: a big, expensive mid-century (1950s) ranch house of dark board-and-batten wood on a
 *       stone base, with white trim, a long low roof and a cross-gabled two-car garage - a stone chimney and
 *       fireplace, a picture window, shutters, a teal front door under the deep eaves, flower beds, a white picket
 *       fence, lamp posts and a mailbox; inside, a living room (sofa, armchairs, coffee table, the radio-phonograph,
 *       bookshelves, lamps), a kitchen (fridge, stove, sink, counters, cabinets) with a dining room, the master
 *       bedroom, a child's bedroom, a pink-tiled bathroom and a hall; in the garage a two-tone tail-finned car and a
 *       workbench; behind it a patio with a barbecue and picnic table and a kidney-shaped swimming pool; and a TV
 *       aerial on the roof;</li>
 *   <li>a machine shed, open to the yard, with a red tractor, a workbench and the farm's tools;</li>
 *   <li>a chicken coop with its fenced run and chickens;</li>
 *   <li>a big fenced paddock with a trough, hay and a shelter, and in it horses, pigs, sheep and cows;</li>
 *   <li>a water-pumping windmill with its trough;</li>
 *   <li>a farm lane and dirt paths between them all, and a driveway to the garage.</li>
 * </ul>
 * At night the skeleton horsemen come (RedFarmNights). The whole farm may face any of the four ways.
 *
 * <p>Laid out in local coordinates: u east, v south (for an unturned farm), y the true height; ground level is
 * {@code centre.y}.
 */
public class RedFarmPiece extends StructurePiece {
    public static final int R = 44;
    private static final int TOP = 26;
    private final BlockPos centre;
    private final long seed;
    private final int turn;

    private static final BlockState RED = ModBlocks.PAINTED_PLANKS.get(DyeColor.RED).get().defaultBlockState();
    private static final BlockState WHITE = ModBlocks.PAINTED_PLANKS.get(DyeColor.WHITE).get().defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    public RedFarmPiece(BlockPos centre, long seed, int turn) {
        super(VillageRegistry.RED_FARM_PIECE.get(), 0, new BoundingBox(centre.getX() - R, centre.getY() - 6, centre.getZ() - R,
                centre.getX() + R, centre.getY() + TOP, centre.getZ() + R));
        this.centre = centre;
        this.seed = seed;
        this.turn = turn;
    }

    public RedFarmPiece(CompoundTag tag) {
        super(VillageRegistry.RED_FARM_PIECE.get(), tag);
        this.centre = new BlockPos(tag.getInt("CX"), tag.getInt("CY"), tag.getInt("CZ"));
        this.seed = tag.getLong("Seed");
        this.turn = tag.getInt("Turn");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CX", this.centre.getX());
        tag.putInt("CY", this.centre.getY());
        tag.putInt("CZ", this.centre.getZ());
        tag.putLong("Seed", this.seed);
        tag.putInt("Turn", this.turn);
    }

    // ================================================================ local coordinates

    private Rotation rotation() {
        return switch (this.turn) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    private BlockPos at(int u, int y, int v) {
        int x, z;
        switch (this.turn) {
            case 1 -> { x = -v; z = u; }
            case 2 -> { x = -u; z = -v; }
            case 3 -> { x = v; z = -u; }
            default -> { x = u; z = v; }
        }
        return new BlockPos(this.centre.getX() + x, y, this.centre.getZ() + z);
    }

    private static int du(Direction d) {
        return d == Direction.EAST ? 1 : d == Direction.WEST ? -1 : 0;
    }

    private static int dv(Direction d) {
        return d == Direction.SOUTH ? 1 : d == Direction.NORTH ? -1 : 0;
    }

    private final List<BlockPos> connecting = new ArrayList<>();
    private WorldGenLevel level;
    private BoundingBox box;

    private void put(int u, int y, int v, BlockState state) {
        BlockPos p = this.at(u, y, v);
        if (!this.box.isInside(p)) return;
        BlockState s = state.rotate(this.rotation());
        this.level.setBlock(p, s, 2);
        if (s.getBlock() instanceof net.minecraft.world.level.block.FenceBlock
                || s.getBlock() instanceof net.minecraft.world.level.block.IronBarsBlock
                || s.getBlock() instanceof net.minecraft.world.level.block.WallBlock) {
            this.connecting.add(p);
        }
    }

    private void put(int u, int y, int v, Block block) {
        this.put(u, y, v, block.defaultBlockState());
    }

    private void fill(int u0, int y0, int v0, int u1, int y1, int v1, BlockState state) {
        for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++) this.put(u, y, v, state);
            }
        }
    }

    private void fill(int u0, int y0, int v0, int u1, int y1, int v1, Block block) {
        this.fill(u0, y0, v0, u1, y1, v1, block.defaultBlockState());
    }

    private static BlockState stairs(Block block, Direction facing, Half half) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    private static BlockState slab(Block block, SlabType type) {
        return block.defaultBlockState().setValue(SlabBlock.TYPE, type);
    }

    private static BlockState trapdoor(Block block, Direction facing, Half half, boolean open) {
        return block.defaultBlockState().setValue(TrapDoorBlock.FACING, facing).setValue(TrapDoorBlock.HALF, half)
                .setValue(TrapDoorBlock.OPEN, open);
    }

    private static BlockState log(Block block) {
        return block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
    }

    private static BlockState facing(Block block, Direction facing) {
        return block.defaultBlockState().setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, facing);
    }

    private void door(int u, int y, int v, Block door, Direction facing) {
        BlockState s = door.defaultBlockState().setValue(DoorBlock.FACING, facing);
        this.put(u, y, v, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        this.put(u, y + 1, v, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    /** A bed with its head at (u, v), lying towards {@code facing} from its foot. */
    private void bed(int u, int y, int v, Block bed, Direction facing) {
        BlockState s = bed.defaultBlockState().setValue(BedBlock.FACING, facing);
        this.put(u, y, v, s.setValue(BedBlock.PART, BedPart.HEAD));
        this.put(u - du(facing), y, v - dv(facing), s.setValue(BedBlock.PART, BedPart.FOOT));
    }

    private void tall(int u, int y, int v, Block plant) {
        this.put(u, y, v, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER));
        this.put(u, y + 1, v, plant.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private void hangingLantern(int u, int y, int v) {
        this.put(u, y, v, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }

    private void lantern(int u, int y, int v) {
        this.put(u, y, v, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
    }

    private void chest(int u, int y, int v, Direction facing, ItemStack... items) {
        BlockPos p = this.at(u, y, v);
        if (!this.box.isInside(p)) return;
        this.level.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing).rotate(this.rotation()), 2);
        if (this.level.getBlockEntity(p) instanceof ChestBlockEntity chest) {
            for (int i = 0; i < items.length && i < 27; i++) chest.setItem(i == 0 && items.length == 1 ? 13 : i, items[i]);
        }
    }

    // ================================================================ building

    // one chunk at a time: the building keeps the chunk it is working on in fields, and chunks of the same
    // structure are generated on several threads at once
    @Override
    public synchronized void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunk, BlockPos pivot) {
        this.level = level;
        this.box = box;
        this.connecting.clear();
        int g = this.centre.getY();
        this.ground(g);
        this.paths(g);
        this.house(g);
        this.barn(g);
        this.silo(g, -17, -12, 20);
        this.silo(g, -17, -5, 17);
        this.machineShed(g);
        this.chickenCoop(g);
        this.paddock(g);
        this.windmill(g);
        // fences, walls and panes join up with what's around them
        for (BlockPos p : this.connecting) {
            BlockState s = level.getBlockState(p);
            level.setBlock(p, Block.updateFromNeighbourShapes(s, level, p), 2);
        }
        this.animals(g);
        this.level = null;
        this.box = null;
    }

    // the farm's own patch: mown grass on solid ground, cleared of everything over it
    private void ground(int g) {
        for (int u = -R + 1; u <= R - 1; u++) {
            for (int v = -R + 1; v <= R - 1; v++) {
                BlockPos top = this.at(u, g, v);
                if (!this.box.isInside(top)) continue;
                for (int y = g - 5; y < g; y++) {
                    BlockPos p = this.at(u, y, v);
                    BlockState s = this.level.getBlockState(p);
                    if (s.isAir() || !s.getFluidState().isEmpty() || s.canBeReplaced() || s.is(net.minecraft.tags.BlockTags.LOGS)) {
                        this.level.setBlock(p, Blocks.DIRT.defaultBlockState(), 2);
                    }
                }
                this.level.setBlock(top, Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = g + 1; y <= g + TOP - 1; y++) {
                    BlockPos p = this.at(u, y, v);
                    if (!this.level.getBlockState(p).isAir()) this.level.setBlock(p, AIR, 2);
                }
            }
        }
    }

    // ---------------------------------------------------------------- the lane, the paths and the driveway
    private void paths(int g) {
        BlockState path = Blocks.DIRT_PATH.defaultBlockState();
        // the farm lane, from the east edge (where the farm is reached) to the barnyard
        this.fill(-21, g, -1, R - 1, g, 1, path);
        this.fill(-21, g, 1, -19, g, 17, path);
        // the barnyard, before the barn's big doors and the machine shed
        this.fill(-34, g, 13, -19, g, 18, Blocks.COARSE_DIRT);
        this.fill(-25, g, 19, -21, g, 34, Blocks.COARSE_DIRT);
        for (int u = -34; u <= -19; u++) {
            for (int v = 13; v <= 18; v++) {
                if ((u * 7 + v * 13) % 5 == 0) this.put(u, g, v, path);
            }
        }
        // on to the paddock gate
        this.fill(-18, g, 15, -9, g, 17, path);
        // the house's front walk (from the picket fence gate) and the garage driveway
        this.fill(12, g, -8, 12, g, -2, path);
        this.fill(12, g, -19, 12, g, -10, Blocks.SMOOTH_STONE);
        this.fill(29, g, -15, 39, g, -10, Blocks.SMOOTH_STONE);
        this.fill(33, g, -9, 35, g, -2, Blocks.SMOOTH_STONE);
    }

    // ================================================================ the farmer's house
    // the main wing u 0..28, v -32..-20 (front, south, at v -20); the garage u 28..40, v -32..-16; walls up to g+4,
    // the top plate and ceiling at g+5, the low roof above
    private static final int H_U0 = 0, H_U1 = 28, H_V0 = -32, H_V1 = -20, G_U0 = 28, G_U1 = 40, G_V1 = -16;

    private static final BlockState SIDING = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState BATTEN = log(Blocks.STRIPPED_DARK_OAK_LOG);
    private static final BlockState BASE = Blocks.STONE_BRICKS.defaultBlockState();

    /** The house's outer wall at this spot: stone base, dark board-and-batten above, white plate on top. */
    private BlockState siding(int g, int y, int along, boolean corner) {
        if (y == g + 1) return BASE;
        if (corner || Math.floorMod(along, 4) == 0) return BATTEN;
        return SIDING;
    }

    private void houseWall(int g, int u0, int v0, int u1, int v1) {
        for (int y = g + 1; y <= g + 4; y++) {
            for (int u = u0; u <= u1; u++) {
                this.put(u, y, v0, this.siding(g, y, u, u == u0 || u == u1));
                this.put(u, y, v1, this.siding(g, y, u, u == u0 || u == u1));
            }
            for (int v = v0; v <= v1; v++) {
                this.put(u0, y, v, this.siding(g, y, v, v == v0 || v == v1));
                this.put(u1, y, v, this.siding(g, y, v, v == v0 || v == v1));
            }
        }
    }

    /**
     * A white-trimmed window in an outer wall: panes from a0 to a1 along the wall (u if alongU, else v) and y0 to y1
     * up, a white sill, header and jambs round it, and open shutters either side if wanted.
     */
    private void window(boolean alongU, int fixed, int a0, int a1, int y0, int y1, Direction out, BlockState pane,
                        boolean shutters) {
        for (int a = a0 - 1; a <= a1 + 1; a++) {
            for (int y = y0 - 1; y <= y1 + 1; y++) {
                boolean inside = a >= a0 && a <= a1 && y >= y0 && y <= y1;
                BlockState s = inside ? pane : WHITE;
                // the stone base stays stone under the sill
                if (!inside && y == y0 - 1 && y0 - 1 == this.centre.getY() + 1) s = BASE;
                if (alongU) this.put(a, y, fixed, s);
                else this.put(fixed, y, a, s);
            }
        }
        if (!shutters) return;
        BlockState shutter = trapdoor(Blocks.WARPED_TRAPDOOR, out, Half.BOTTOM, true);
        for (int y = y0; y <= y1; y++) {
            for (int a : new int[]{a0 - 2, a1 + 2}) {
                if (alongU) this.put(a, y, fixed + dv(out), shutter);
                else this.put(fixed + du(out), y, a, shutter);
            }
        }
    }

    /** The house's roof height, in half blocks above the plate (g+5), or -1 where there's no roof. */
    private static int roofHalf(int u, int v) {
        int h = -1;
        // the main wing's: ridge along u, two blocks of overhang
        if (u >= H_U0 - 2 && u <= G_U1 + 2 && v >= H_V0 - 2 && v <= H_V1 + 2) {
            h = Math.min(v - (H_V0 - 2), (H_V1 + 2) - v);
        }
        // the garage's cross gable: ridge along v, out over its front
        if (u >= G_U0 - 2 && u <= G_U1 + 2 && v >= -26 && v <= G_V1 + 2) {
            h = Math.max(h, Math.min(u - (G_U0 - 2), (G_U1 + 2) - u));
        }
        return h;
    }

    private void house(int g) {
        int yb = g + 5;
        // the foundation and floors: oak in the rooms, black and white tiles in the kitchen, pink in the bathroom,
        // smooth stone in the garage
        this.fill(H_U0, g, H_V0, H_U1, g, H_V1, Blocks.STONE_BRICKS);
        this.fill(H_U0 + 1, g, H_V0 + 1, H_U1 - 1, g, H_V1 - 1, Blocks.OAK_PLANKS);
        for (int u = 21; u <= 27; u++) {
            for (int v = -31; v <= -27; v++) {
                this.put(u, g, v, (u + v) % 2 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.BLACK_TERRACOTTA);
            }
        }
        for (int u = 7; u <= 9; u++) {
            for (int v = -24; v <= -21; v++) {
                this.put(u, g, v, (u + v) % 2 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.PINK_TERRACOTTA);
            }
        }
        this.fill(G_U0, g, H_V0, G_U1, g, G_V1, Blocks.SMOOTH_STONE);
        // the outer walls
        this.houseWall(g, H_U0, H_V0, H_U1, H_V1);
        this.houseWall(g, G_U0, H_V0, G_U1, G_V1);
        // inside the garage, the main wing's front wall line is open
        this.fill(G_U0 + 1, g + 1, H_V1, G_U1 - 1, g + 4, H_V1, AIR);
        // the plate and the ceilings: white
        this.fill(H_U0, yb, H_V0, H_U1, yb, H_V1, WHITE);
        this.fill(G_U0, yb, H_V0, G_U1, yb, G_V1, WHITE);

        this.roof(g);
        this.windowsAndDoors(g);
        this.interior(g);
        this.garage(g);
        this.chimney(g);
        this.frontYard(g);
        this.backYard(g);
        // the TV aerial on the ridge
        this.fill(6, yb + 5, -26, 6, yb + 7, -26, Blocks.IRON_BARS);
        this.fill(5, yb + 7, -26, 7, yb + 7, -26, Blocks.IRON_BARS);
        this.put(6, yb + 8, -26, Blocks.LIGHTNING_ROD);
    }

    private void roof(int g) {
        int yb = g + 5;
        BlockState low = slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM), high = slab(Blocks.DARK_OAK_SLAB, SlabType.TOP);
        for (int u = H_U0 - 2; u <= G_U1 + 2; u++) {
            for (int v = H_V0 - 2; v <= G_V1 + 2; v++) {
                int h = roofHalf(u, v);
                if (h < 0) continue;
                this.put(u, yb + h / 2, v, h % 2 == 0 ? low : high);
            }
        }
        // the gable ends, board and batten up to the roof
        for (int v = H_V0; v <= H_V1; v++) {
            for (int u : new int[]{H_U0, G_U1}) {
                int h = roofHalf(u, v);
                for (int y = yb + 1; y < yb + h / 2; y++) this.put(u, y, v, Math.floorMod(v, 3) == 0 ? BATTEN : SIDING);
            }
        }
        for (int u = G_U0; u <= G_U1; u++) {
            int h = roofHalf(u, G_V1);
            for (int y = yb + 1; y < yb + h / 2; y++) this.put(u, y, G_V1, Math.floorMod(u, 3) == 1 ? BATTEN : SIDING);
        }
        // a round vent high in each gable
        this.put(H_U0, yb + 2, -26, Blocks.IRON_BARS);
        this.put(G_U1, yb + 2, -26, Blocks.IRON_BARS);
        this.put(34, yb + 2, G_V1, Blocks.IRON_BARS);
    }

    /** A lamp hung under the eaves, from a white soffit block (the roof's slabs alone won't hold it up). */
    private void soffitLantern(int g, int u, int v) {
        this.put(u, g + 5, v, WHITE);
        this.hangingLantern(u, g + 4, v);
    }

    private void windowsAndDoors(int g) {
        BlockState glass = Blocks.GLASS_PANE.defaultBlockState();
        BlockState frosted = Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState();
        // the front (south): the child's bedroom, the bathroom (frosted), the picture window, the dining room
        this.window(true, H_V1, 3, 3, g + 2, g + 3, Direction.SOUTH, glass, true);
        this.window(true, H_V1, 8, 8, g + 3, g + 3, Direction.SOUTH, frosted, false);
        this.window(true, H_V1, 15, 19, g + 2, g + 3, Direction.SOUTH, glass, false);
        this.window(true, H_V1, 22, 25, g + 2, g + 3, Direction.SOUTH, glass, true);
        // the back (north): the master bedroom, the living room's glass wall, the kitchen
        this.window(true, H_V0, 3, 3, g + 2, g + 3, Direction.NORTH, glass, true);
        this.window(true, H_V0, 7, 7, g + 2, g + 3, Direction.NORTH, glass, true);
        this.window(true, H_V0, 17, 19, g + 2, g + 3, Direction.NORTH, glass, false);
        this.window(true, H_V0, 23, 24, g + 2, g + 3, Direction.NORTH, glass, false);
        // the west end: the master bedroom and the child's room
        this.window(false, H_U0, -30, -29, g + 2, g + 3, Direction.WEST, glass, true);
        this.window(false, H_U0, -23, -22, g + 2, g + 3, Direction.WEST, glass, true);
        // the garage's side windows
        this.window(false, G_U1, -28, -27, g + 2, g + 3, Direction.EAST, glass, false);
        this.window(false, G_U1, -21, -20, g + 2, g + 3, Direction.EAST, glass, false);
        // the front door: teal, in a white frame, with a lamp either side under the eaves
        this.fill(11, g + 1, H_V1, 13, g + 3, H_V1, WHITE);
        this.door(12, g + 1, H_V1, Blocks.WARPED_DOOR, Direction.NORTH);
        this.put(12, g, H_V1 + 1, Blocks.SMOOTH_STONE);
        this.put(12, g + 1, H_V1 + 1, Blocks.BROWN_CARPET);
        this.soffitLantern(g, 10, H_V1 + 1);
        this.soffitLantern(g, 14, H_V1 + 1);
        // the back door, from the kitchen onto the patio
        this.fill(26, g + 1, H_V0, 28, g + 3, H_V0, WHITE);
        this.put(28, g + 1, H_V0, BASE);
        this.door(27, g + 1, H_V0, Blocks.SPRUCE_DOOR, Direction.SOUTH);
        // the garage doors: two wide openings, white headers
        for (int u0 : new int[]{30, 35}) {
            this.fill(u0, g + 1, G_V1, u0 + 3, g + 3, G_V1, AIR);
            this.fill(u0 - 1, g + 4, G_V1, u0 + 4, g + 4, G_V1, WHITE);
            this.soffitLantern(g, u0 - 1, G_V1 + 1);
        }
        this.soffitLantern(g, 39, G_V1 + 1);
        // the door between the kitchen and the garage
        this.door(G_U0, g + 1, -28, Blocks.SPRUCE_DOOR, Direction.EAST);
    }

    private void interior(int g) {
        BlockState partition = Blocks.BIRCH_PLANKS.defaultBlockState();
        // the walls inside: the bedroom wing (west of u 10), its hall (v -26), the bathroom, the kitchen's wall
        this.fill(10, g + 1, -31, 10, g + 4, -21, partition);
        this.fill(1, g + 1, -27, 9, g + 4, -27, partition);
        this.fill(1, g + 1, -25, 9, g + 4, -25, partition);
        this.fill(6, g + 1, -24, 6, g + 4, -21, partition);
        this.fill(20, g + 1, -31, 20, g + 4, -21, partition);
        this.fill(20, g + 1, -25, 20, g + 3, -23, AIR);
        this.put(20, g + 4, -24, WHITE);
        this.door(10, g + 1, -26, Blocks.BIRCH_DOOR, Direction.EAST);
        this.door(5, g + 1, -27, Blocks.BIRCH_DOOR, Direction.NORTH);
        this.door(3, g + 1, -25, Blocks.BIRCH_DOOR, Direction.SOUTH);
        this.door(8, g + 1, -25, Blocks.BIRCH_DOOR, Direction.SOUTH);

        // the hall: a runner and a lamp
        this.fill(1, g + 1, -26, 9, g + 1, -26, Blocks.RED_CARPET);
        this.hangingLantern(5, g + 4, -26);

        // the master bedroom (u 1..9, v -31..-28): a double bed, nightstands with lamps, a dresser, a wardrobe, a rug
        this.bed(4, g + 1, -31, Blocks.LIGHT_BLUE_BED, Direction.NORTH);
        this.bed(5, g + 1, -31, Blocks.LIGHT_BLUE_BED, Direction.NORTH);
        for (int u : new int[]{3, 6}) {
            this.put(u, g + 1, -31, log(Blocks.STRIPPED_DARK_OAK_LOG));
            this.lantern(u, g + 2, -31);
        }
        this.put(8, g + 1, -31, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.SOUTH));
        this.put(9, g + 1, -31, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.SOUTH));
        this.put(8, g + 2, -31, Blocks.POTTED_AZURE_BLUET);
        this.put(9, g + 2, -31, Blocks.POTTED_RED_TULIP);
        this.fill(1, g + 1, -29, 1, g + 3, -28, Blocks.DARK_OAK_PLANKS);
        this.chest(1, g + 1, -31, Direction.EAST, new ItemStack(Items.WHITE_WOOL, 4), new ItemStack(Items.LEATHER_BOOTS),
                new ItemStack(Items.CLOCK), new ItemStack(Items.BOOK, 3));
        this.fill(3, g + 1, -29, 6, g + 1, -28, Blocks.WHITE_CARPET);
        this.hangingLantern(5, g + 4, -29);

        // the child's bedroom (u 1..5, v -24..-21): a bed, a desk and chair, a bookshelf, a toy chest
        this.bed(1, g + 1, -22, Blocks.RED_BED, Direction.WEST);
        this.put(5, g + 1, -24, Blocks.BOOKSHELF);
        this.put(5, g + 2, -24, Blocks.BOOKSHELF);
        this.put(5, g + 1, -22, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        this.put(4, g + 1, -22, stairs(Blocks.SPRUCE_STAIRS, Direction.WEST, Half.BOTTOM));
        this.put(5, g + 2, -22, Blocks.POTTED_CACTUS);
        this.chest(1, g + 1, -24, Direction.SOUTH, new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 8),
                new ItemStack(Items.PAPER, 6), new ItemStack(Items.WHITE_WOOL, 2));
        this.fill(2, g + 1, -24, 3, g + 1, -23, Blocks.BLUE_CARPET);
        this.hangingLantern(3, g + 4, -23);

        // the bathroom (u 7..9, v -24..-21), pink-tiled: a bathtub, a basin, a toilet
        this.fill(7, g + 1, -22, 7, g + 1, -21, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        this.put(9, g + 1, -24, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2));
        this.put(9, g + 1, -21, Blocks.HOPPER);
        this.put(9, g + 2, -21, trapdoor(Blocks.BIRCH_TRAPDOOR, Direction.NORTH, Half.BOTTOM, true));
        this.put(8, g + 1, -21, Blocks.WHITE_CARPET);
        this.hangingLantern(8, g + 4, -23);

        // the living room (u 11..19): the sofa facing the fireplace, armchairs, a coffee table, the radio-phonograph,
        // bookshelves, floor lamps, plants and a big rug
        this.fill(12, g + 1, -30, 16, g + 1, -27, Blocks.LIGHT_GRAY_CARPET);
        for (int u = 13; u <= 15; u++) this.put(u, g + 1, -26, stairs(Blocks.WARPED_STAIRS, Direction.SOUTH, Half.BOTTOM));
        this.put(12, g + 1, -26, trapdoor(Blocks.WARPED_TRAPDOOR, Direction.WEST, Half.BOTTOM, true));
        this.put(16, g + 1, -26, trapdoor(Blocks.WARPED_TRAPDOOR, Direction.EAST, Half.BOTTOM, true));
        for (int u = 13; u <= 15; u++) this.put(u, g + 1, -28, trapdoor(Blocks.DARK_OAK_TRAPDOOR, Direction.NORTH, Half.TOP, false));
        this.put(11, g + 1, -28, stairs(Blocks.CRIMSON_STAIRS, Direction.WEST, Half.BOTTOM));
        this.put(18, g + 1, -28, stairs(Blocks.CRIMSON_STAIRS, Direction.EAST, Half.BOTTOM));
        this.put(19, g + 1, -26, Blocks.JUKEBOX);
        this.put(19, g + 2, -26, Blocks.POTTED_FERN);
        this.fill(11, g + 1, -31, 11, g + 3, -31, Blocks.BOOKSHELF);
        this.put(12, g + 1, -31, Blocks.BOOKSHELF);
        for (int[] lamp : new int[][]{{11, -30}, {19, -29}, {11, -22}}) {
            this.put(lamp[0], g + 1, lamp[1], Blocks.DARK_OAK_FENCE);
            this.lantern(lamp[0], g + 2, lamp[1]);
        }
        this.put(19, g + 1, -21, Blocks.POTTED_BAMBOO);
        this.put(14, g + 1, -21, Blocks.POTTED_FLOWERING_AZALEA);
        this.hangingLantern(15, g + 4, -24);
        // the entry: a coat stand and a mat by the front door
        this.put(13, g + 1, -21, Blocks.SPRUCE_FENCE);
        this.put(13, g + 2, -21, Blocks.SPRUCE_FENCE);
        this.put(12, g + 1, -21, Blocks.BROWN_CARPET);

        // the kitchen (u 21..27, v -31..-27): the fridge, counters with the sink under the window, the stove,
        // cabinets above, and a peninsula counter between it and the dining room
        this.fill(21, g + 1, -31, 21, g + 2, -31, Blocks.QUARTZ_BLOCK);
        this.put(21, g + 3, -31, WHITE);
        this.put(21, g + 2, -30, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.SOUTH));
        for (int u = 22; u <= 26; u++) this.put(u, g + 1, -31, Blocks.SMOOTH_QUARTZ);
        this.put(23, g + 1, -31, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        this.put(25, g + 1, -31, facing(Blocks.SMOKER, Direction.SOUTH));
        this.put(26, g + 1, -31, facing(Blocks.FURNACE, Direction.SOUTH));
        this.put(22, g + 2, -31, Blocks.POTTED_RED_TULIP);
        for (int u : new int[]{21, 22, 25, 26}) this.put(u, g + 4, -31, WHITE);
        for (int u : new int[]{22, 25, 26}) this.put(u, g + 4, -30, trapdoor(Blocks.BIRCH_TRAPDOOR, Direction.SOUTH, Half.TOP, true));
        this.fill(21, g + 1, -27, 23, g + 1, -27, WHITE);
        this.put(22, g + 2, -27, Blocks.CAKE);
        this.chest(27, g + 1, -30, Direction.WEST, new ItemStack(Items.BREAD, 8), new ItemStack(ModItems.COOKED_CORN.get(), 8),
                new ItemStack(ModItems.CORN.get(), 12), new ItemStack(Items.APPLE, 6), new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.SUGAR, 6), new ItemStack(Items.EGG, 8), new ItemStack(Items.COOKED_PORKCHOP, 4),
                new ItemStack(ModItems.COTTON_BOLL.get(), 12), new ItemStack(ModItems.COTTON_SEEDS.get(), 6));
        this.hangingLantern(24, g + 4, -29);

        // the dining room (u 21..27, v -26..-21): a table for six under its lamp, a china cabinet
        for (int u = 23; u <= 24; u++) {
            this.put(u, g + 1, -23, Blocks.DARK_OAK_FENCE);
            this.put(u, g + 2, -23, Blocks.WHITE_CARPET);
            this.put(u, g + 1, -22, stairs(Blocks.CRIMSON_STAIRS, Direction.SOUTH, Half.BOTTOM));
            this.put(u, g + 1, -24, stairs(Blocks.CRIMSON_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        this.put(22, g + 1, -23, stairs(Blocks.CRIMSON_STAIRS, Direction.WEST, Half.BOTTOM));
        this.put(25, g + 1, -23, stairs(Blocks.CRIMSON_STAIRS, Direction.EAST, Half.BOTTOM));
        this.put(27, g + 1, -25, Blocks.CHISELED_BOOKSHELF.defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
        this.put(27, g + 2, -25, Blocks.GLASS);
        this.put(27, g + 1, -21, Blocks.POTTED_FERN);
        this.hangingLantern(24, g + 4, -23);
    }

    private void garage(int g) {
        // the car: a two-tone 1950s sedan with tail fins, nose out
        int c0 = 31, c1 = 33;
        BlockState body = Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState();
        for (int u = c0; u <= c1; u++) {
            for (int v = -26; v <= -19; v++) this.put(u, g + 1, v, body);
        }
        for (int u : new int[]{c0, c1}) {
            for (int v : new int[]{-25, -20}) this.put(u, g + 1, v, Blocks.BLACK_TERRACOTTA);
            this.put(u, g + 1, -26, Blocks.RED_STAINED_GLASS);    // the tail lights
            this.put(u, g + 1, -19, Blocks.SMOOTH_QUARTZ);   // the headlights
            this.put(u, g + 2, -26, body);                    // the fins
        }
        this.put(32, g + 1, -19, Blocks.IRON_BARS);          // the grille
        this.fill(c0, g + 2, -20, c1, g + 2, -19, slab(Blocks.SMOOTH_QUARTZ_SLAB, SlabType.BOTTOM));   // the hood
        this.put(32, g + 2, -26, slab(Blocks.SMOOTH_QUARTZ_SLAB, SlabType.BOTTOM));
        this.put(32, g + 2, -25, slab(Blocks.SMOOTH_QUARTZ_SLAB, SlabType.BOTTOM));
        this.put(c0, g + 2, -25, slab(Blocks.SMOOTH_QUARTZ_SLAB, SlabType.BOTTOM));
        this.put(c1, g + 2, -25, slab(Blocks.SMOOTH_QUARTZ_SLAB, SlabType.BOTTOM));
        BlockState pane = Blocks.LIGHT_GRAY_STAINED_GLASS_PANE.defaultBlockState();
        for (int v = -24; v <= -22; v++) {
            this.put(c0, g + 2, v, pane);
            this.put(c1, g + 2, v, pane);
            this.put(32, g + 2, v, v == -22 ? stairs(Blocks.QUARTZ_STAIRS, Direction.SOUTH, Half.BOTTOM) : AIR);
        }
        this.fill(c0, g + 2, -21, c1, g + 2, -21, pane);
        this.fill(c0, g + 3, -24, c1, g + 3, -21, slab(Blocks.QUARTZ_SLAB, SlabType.BOTTOM));
        // the other bay: an oil stain, a lawn mower's place, the bicycle hooks
        this.put(36, g + 1, -22, Blocks.BLACK_CARPET);
        // the workbench along the back wall, and shelves over it
        this.put(30, g + 1, -31, Blocks.CRAFTING_TABLE);
        this.put(31, g + 1, -31, Blocks.SMITHING_TABLE);
        this.put(32, g + 1, -31, facing(Blocks.ANVIL, Direction.EAST));
        this.put(33, g + 1, -31, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        this.put(34, g + 1, -31, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        this.chest(35, g + 1, -31, Direction.SOUTH, new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.IRON_HOE),
                new ItemStack(Items.SHEARS), new ItemStack(Items.BUCKET), new ItemStack(Items.IRON_INGOT, 5),
                new ItemStack(Items.LEAD, 2), new ItemStack(Items.FLINT_AND_STEEL));
        this.put(36, g + 1, -31, facing(Blocks.GRINDSTONE, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.GrindstoneBlock.FACE, AttachFace.FLOOR));
        this.fill(30, g + 3, -31, 36, g + 3, -31, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        this.put(30, g + 4, -31, Blocks.POTTED_DEAD_BUSH);
        this.hangingLantern(32, g + 4, -27);
        this.hangingLantern(37, g + 4, -22);
    }

    private void chimney(int g) {
        int top = g + 11;
        // the stone mass outside the back wall, and in the wall line
        for (int y = g + 1; y <= top; y++) {
            for (int u = 13; u <= 15; u++) {
                this.put(u, y, H_V0 - 1, (u + y) % 3 == 0 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS);
                this.put(u, y, H_V0, (u + y) % 4 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
            }
        }
        this.fill(12, g + 1, H_V0 - 1, 12, g + 6, H_V0 - 1, Blocks.COBBLESTONE);
        this.fill(16, g + 1, H_V0 - 1, 16, g + 6, H_V0 - 1, Blocks.COBBLESTONE);
        this.fill(13, g, H_V0 - 1, 15, g, H_V0 - 1, Blocks.STONE_BRICKS);
        // the flue up through it, open at the top, and the fire at its foot
        this.fill(14, g + 2, H_V0, 14, top, H_V0, AIR);
        this.put(14, g + 1, H_V0, Blocks.CAMPFIRE);
        this.fill(13, top + 1, H_V0 - 1, 15, top + 1, H_V0 - 1, slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
        this.put(13, top + 1, H_V0, slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
        this.put(15, top + 1, H_V0, slab(Blocks.STONE_BRICK_SLAB, SlabType.BOTTOM));
        // the fireplace inside: its stone surround, the opening, a mantel, the hearth
        this.fill(13, g + 1, H_V0 + 1, 13, g + 2, H_V0 + 1, Blocks.STONE_BRICKS);
        this.fill(15, g + 1, H_V0 + 1, 15, g + 2, H_V0 + 1, Blocks.STONE_BRICKS);
        this.fill(14, g + 1, H_V0 + 1, 14, g + 2, H_V0 + 1, AIR);
        this.fill(12, g + 3, H_V0 + 1, 16, g + 3, H_V0 + 1, stairs(Blocks.STONE_BRICK_STAIRS, Direction.NORTH, Half.TOP));
        this.fill(13, g + 4, H_V0 + 1, 15, g + 4, H_V0 + 1, Blocks.STONE_BRICKS);
        this.fill(13, g, H_V0 + 1, 15, g, H_V0 + 2, Blocks.POLISHED_ANDESITE);
        this.put(12, g + 4, H_V0 + 1, Blocks.POTTED_FERN);
        this.put(16, g + 4, H_V0 + 1, Blocks.CANDLE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 3));
    }

    private void frontYard(int g) {
        // the flower beds along the front, under the eaves: roses, peonies and lilacs, tulips and alliums before them
        Block[] tall = {Blocks.ROSE_BUSH, Blocks.PEONY, Blocks.LILAC};
        Block[] low = {Blocks.RED_TULIP, Blocks.ALLIUM, Blocks.WHITE_TULIP, Blocks.AZURE_BLUET, Blocks.PINK_TULIP};
        for (int u = H_U0; u <= G_U0 - 1; u++) {
            if (u >= 10 && u <= 14) continue;
            // (not where the shutters are)
            if (u == 1 || u == 5 || u == 20 || u == 27) this.put(u, g + 1, H_V1 + 1, low[Math.floorMod(u + 2, low.length)]);
            else this.tall(u, g + 1, H_V1 + 1, tall[Math.floorMod(u, tall.length)]);
            this.put(u, g + 1, H_V1 + 2, low[Math.floorMod(u, low.length)]);
        }
        // clipped boxwood at the corners and by the walk
        BlockState box = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int[] b : new int[][]{{H_U0 - 1, H_V1 + 1}, {H_U0 - 1, H_V1}, {11, H_V1 + 3}, {13, H_V1 + 3}, {11, -11}, {13, -11}}) {
            this.put(b[0], g + 1, b[1], box);
        }
        this.put(G_U1 + 1, g + 1, G_V1 + 1, Blocks.FLOWERING_AZALEA);
        this.put(G_U1 + 1, g + 1, H_V0 - 1, Blocks.FLOWERING_AZALEA);
        // the white picket fence, with its gate on the walk, lamp posts and the mailbox
        for (int u = H_U0 - 1; u <= G_U0 - 1; u++) {
            if (u == 12) continue;
            this.put(u, g + 1, -9, Blocks.BIRCH_FENCE);
        }
        this.fill(H_U0 - 1, g + 1, -18, H_U0 - 1, g + 1, -10, Blocks.BIRCH_FENCE);
        this.put(12, g + 1, -9, Blocks.BIRCH_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        for (int u : new int[]{10, 14}) {
            this.fill(u, g + 1, -10, u, g + 2, -10, Blocks.DARK_OAK_FENCE);
            this.lantern(u, g + 3, -10);
        }
        this.put(14, g + 1, -8, Blocks.DARK_OAK_FENCE);
        this.put(14, g + 2, -8, Blocks.DECORATED_POT);
        // by the driveway too
        for (int v : new int[]{-14, -10}) {
            this.fill(41, g + 1, v, 41, g + 2, v, Blocks.DARK_OAK_FENCE);
            this.lantern(41, g + 3, v);
        }
    }

    private void backYard(int g) {
        // the patio along the back, with the barbecue and the picnic table
        this.fill(17, g, -36, 28, g, -33, Blocks.POLISHED_ANDESITE);
        this.fill(18, g, -35, 27, g, -34, Blocks.SMOOTH_STONE);
        this.put(27, g + 1, -36, Blocks.BRICKS);
        this.put(26, g + 1, -36, facing(Blocks.SMOKER, Direction.SOUTH));
        this.put(25, g + 1, -36, Blocks.BRICKS);
        this.put(27, g + 2, -36, Blocks.BRICK_WALL);
        this.fill(20, g + 1, -35, 22, g + 1, -35, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        this.fill(20, g + 1, -36, 22, g + 1, -36, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        this.fill(20, g + 1, -34, 22, g + 1, -34, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        // the kidney-shaped swimming pool: two blocks deep, a light blue lining, a white coping round it
        java.util.function.BiPredicate<Integer, Integer> pool = (u, v) -> {
            double a = (u - 5.5) * (u - 5.5) / 13.0 + (v + 39.5) * (v + 39.5) / 9.0;
            double b = (u - 11.5) * (u - 11.5) / 8.0 + (v + 38.5) * (v + 38.5) / 7.0;
            double bite = (u - 8.5) * (u - 8.5) + (v + 36.0) * (v + 36.0);
            return (a <= 1.0 || b <= 1.0) && bite > 2.3;
        };
        for (int u = 0; u <= 16; u++) {
            for (int v = -43; v <= -34; v++) {
                if (pool.test(u, v)) {
                    this.put(u, g, v, Blocks.WATER);
                    this.put(u, g - 1, v, Blocks.WATER);
                    this.put(u, g - 2, v, Blocks.LIGHT_BLUE_TERRACOTTA);
                    continue;
                }
                boolean edge = false;
                for (int du = -1; du <= 1 && !edge; du++) {
                    for (int dv = -1; dv <= 1; dv++) if (pool.test(u + du, v + dv)) { edge = true; break; }
                }
                if (edge) {
                    this.put(u, g, v, Blocks.SMOOTH_QUARTZ);
                    this.put(u, g - 1, v, Blocks.LIGHT_BLUE_TERRACOTTA);
                    this.put(u, g - 2, v, Blocks.LIGHT_BLUE_TERRACOTTA);
                }
            }
        }
        // the diving board, at the deep end
        this.put(0, g + 1, -40, Blocks.SMOOTH_QUARTZ);
        this.put(1, g + 1, -40, slab(Blocks.BIRCH_SLAB, SlabType.BOTTOM));
        this.put(2, g + 1, -40, slab(Blocks.BIRCH_SLAB, SlabType.BOTTOM));
        // loungers and a striped sun umbrella
        for (int u : new int[]{14, 16}) {
            this.put(u, g + 1, -43, stairs(Blocks.BIRCH_STAIRS, Direction.NORTH, Half.BOTTOM));
            this.put(u, g + 1, -42, slab(Blocks.BIRCH_SLAB, SlabType.BOTTOM));
        }
        this.fill(15, g + 1, -43, 15, g + 2, -43, Blocks.DARK_OAK_FENCE);
        for (int u = 14; u <= 16; u++) {
            for (int v = -43; v <= -42; v++) this.put(u, g + 3, v, (u + v) % 2 == 0 ? Blocks.RED_WOOL : Blocks.WHITE_WOOL);
        }
    }

    // ================================================================ the barn
    // u -40..-22 (its centre line u -31), v -16..12; walls to g+8; a gambrel roof; the big doors at the south end
    private static final int BU0 = -40, BU1 = -22, BV0 = -16, BV1 = 12, BWALL = 8, BC = -31;

    /** The barn roof's height (half blocks above g+BWALL+1) at this column: steep low slopes, shallow upper ones. */
    private static int barnRoofHalf(int u) {
        int e = 10 - Math.abs(u - BC);     // 0 at the overhang, 10 at the ridge
        return e <= 4 ? 4 * e : 16 + (e - 4);
    }

    private void barn(int g) {
        int yb = g + BWALL + 1;
        // the floor and the stone footing
        this.fill(BU0 + 1, g, BV0 + 1, BU1 - 1, g, BV1 - 1, Blocks.COARSE_DIRT);
        this.fill(BC - 2, g, BV0, BC + 2, g, BV1, Blocks.PACKED_MUD);
        for (int u = BU0; u <= BU1; u++) {
            this.put(u, g, BV0, Blocks.STONE_BRICKS);
            this.put(u, g, BV1, Blocks.STONE_BRICKS);
        }
        this.fill(BU0, g, BV0, BU0, g, BV1, Blocks.STONE_BRICKS);
        this.fill(BU1, g, BV0, BU1, g, BV1, Blocks.STONE_BRICKS);
        // red walls, white posts every four blocks down the sides, white corners and a white band under the eaves
        for (int y = g + 1; y <= g + BWALL; y++) {
            for (int v = BV0; v <= BV1; v++) {
                boolean post = (v - BV0) % 4 == 0 || y == g + BWALL;
                this.put(BU0, y, v, post ? WHITE : RED);
                this.put(BU1, y, v, post ? WHITE : RED);
            }
            for (int u = BU0; u <= BU1; u++) {
                boolean post = u == BU0 || u == BU1 || y == g + BWALL;
                this.put(u, y, BV0, post ? WHITE : RED);
                this.put(u, y, BV1, post ? WHITE : RED);
            }
        }
        // windows down both sides, between the posts
        for (int v = BV0 + 2; v < BV1; v += 4) {
            for (int u : new int[]{BU0, BU1}) {
                this.put(u, g + 3, v, Blocks.GLASS_PANE);
                this.put(u, g + 4, v, Blocks.GLASS_PANE);
            }
        }
        // the gambrel roof: dark stairs down the steep slopes, slabs over the shallow ones, two blocks of overhang
        // at the gable ends
        for (int u = BU0 - 1; u <= BU1 + 1; u++) {
            int h = barnRoofHalf(u);
            int yTop = yb + h / 2;
            int e = 10 - Math.abs(u - BC);
            int outer = e > 0 ? yb + barnRoofHalf(u + (u < BC ? -1 : 1)) / 2 : yTop;
            // (over the walls, the roof is closed right down to them)
            int fillFrom = e == 1 ? yb : outer + 1;
            boolean steep = e <= 4;
            Direction in = u < BC ? Direction.EAST : Direction.WEST;
            for (int v = BV0 - 2; v <= BV1 + 2; v++) {
                if (u == BC) this.put(u, yTop, v, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
                else if (steep) this.put(u, yTop, v, stairs(Blocks.DARK_OAK_STAIRS, in, Half.BOTTOM));
                else this.put(u, yTop, v, slab(Blocks.DARK_OAK_SLAB, h % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP));
                for (int y = fillFrom; y < yTop; y++) this.put(u, y, v, Blocks.DARK_OAK_PLANKS);
            }
            // the gable ends: red, edged in white under the roof
            if (u >= BU0 && u <= BU1) {
                for (int y = yb; y < yTop; y++) {
                    BlockState s = y == yTop - 1 || u == BU0 || u == BU1 ? WHITE : RED;
                    this.put(u, y, BV0, s);
                    this.put(u, y, BV1, s);
                }
            }
        }
        // the big doors, south: an opening five wide and six high in a white frame, and the slid-back door panels
        // either side, red with white cross-braces
        this.fill(BC - 2, g + 1, BV1, BC + 2, g + 6, BV1, AIR);
        this.fill(BC - 3, g + 1, BV1, BC - 3, g + 7, BV1, WHITE);
        this.fill(BC + 3, g + 1, BV1, BC + 3, g + 7, BV1, WHITE);
        this.fill(BC - 3, g + 7, BV1, BC + 3, g + 7, BV1, WHITE);
        for (int side : new int[]{-1, 1}) {
            int a = BC + side * 4, b = BC + side * 8;
            int u0 = Math.min(a, b), u1 = Math.max(a, b);
            for (int u = u0; u <= u1; u++) {
                for (int y = g + 1; y <= g + 6; y++) {
                    boolean frame = u == u0 || u == u1 || y == g + 1 || y == g + 6;
                    int du = u - u0, dy = y - (g + 1);
                    boolean brace = du == Math.round(dy * 4 / 5.0F) || du == Math.round((5 - dy) * 4 / 5.0F);
                    this.put(u, y, BV1 + 1, frame || brace ? WHITE : RED);
                }
            }
        }
        // the hayloft door above, white-framed, and the hay hood with its pulley chain
        this.fill(BC - 1, g + 11, BV1, BC + 1, g + 13, BV1, AIR);
        this.fill(BC - 2, g + 10, BV1, BC + 2, g + 10, BV1, WHITE);
        this.fill(BC - 2, g + 14, BV1, BC + 2, g + 14, BV1, WHITE);
        this.fill(BC - 2, g + 11, BV1, BC - 2, g + 13, BV1, WHITE);
        this.fill(BC + 2, g + 11, BV1, BC + 2, g + 13, BV1, WHITE);
        this.fill(BC, g + 16, BV1 + 1, BC, g + 16, BV1 + 2, Blocks.DARK_OAK_FENCE);
        this.put(BC, g + 15, BV1 + 2, Blocks.CHAIN);
        this.fill(BC - 2, g + 10, BV1 - 1, BC + 2, g + 10, BV1 - 1, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        // the smaller door at the north end
        this.fill(BC - 1, g + 1, BV0, BC + 1, g + 4, BV0, AIR);
        this.fill(BC - 2, g + 1, BV0, BC - 2, g + 5, BV0, WHITE);
        this.fill(BC + 2, g + 1, BV0, BC + 2, g + 5, BV0, WHITE);
        this.fill(BC - 2, g + 5, BV0, BC + 2, g + 5, BV0, WHITE);
        // a round window high in the north gable
        this.put(BC, g + 13, BV0, Blocks.GLASS_PANE);
        // the cupola on the ridge, white with louvres, its little roof and the weathervane
        int ridge = yb + barnRoofHalf(BC) / 2;
        this.fill(BC - 1, ridge, -3, BC + 1, ridge + 2, -1, WHITE);
        this.put(BC, ridge + 1, -3, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.NORTH, Half.BOTTOM, true));
        this.put(BC, ridge + 1, -1, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.SOUTH, Half.BOTTOM, true));
        this.put(BC - 1, ridge + 1, -2, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.WEST, Half.BOTTOM, true));
        this.put(BC + 1, ridge + 1, -2, trapdoor(Blocks.SPRUCE_TRAPDOOR, Direction.EAST, Half.BOTTOM, true));
        this.put(BC, ridge + 1, -2, AIR);
        this.put(BC - 1, ridge + 3, -2, stairs(Blocks.DARK_OAK_STAIRS, Direction.EAST, Half.BOTTOM));
        this.put(BC + 1, ridge + 3, -2, stairs(Blocks.DARK_OAK_STAIRS, Direction.WEST, Half.BOTTOM));
        this.put(BC, ridge + 3, -3, stairs(Blocks.DARK_OAK_STAIRS, Direction.SOUTH, Half.BOTTOM));
        this.put(BC, ridge + 3, -1, stairs(Blocks.DARK_OAK_STAIRS, Direction.NORTH, Half.BOTTOM));
        this.put(BC, ridge + 3, -2, Blocks.DARK_OAK_PLANKS);
        this.put(BC - 1, ridge + 3, -3, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        this.put(BC + 1, ridge + 3, -3, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        this.put(BC - 1, ridge + 3, -1, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        this.put(BC + 1, ridge + 3, -1, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        this.put(BC, ridge + 4, -2, Blocks.LIGHTNING_ROD);

        // inside: the great pile of hay at the north end, and the chest hidden at its heart
        int pu0 = BU0 + 1, pu1 = BU1 - 1, pv0 = BV0 + 1, pv1 = -7;
        for (int k = 0; k < 5; k++) {
            this.fill(pu0 + k, g + 1 + k, pv0, pu1 - k, g + 1 + k, pv1 - k, Blocks.HAY_BLOCK);
        }
        this.chest(BC, g + 1, -11, Direction.SOUTH, new ItemStack(ModItems.SHOTGUN.get()),
                new ItemStack(ModItems.BULLET.get(), 16), new ItemStack(ModItems.BULLET.get(), 8));
        // the stalls down both sides of the aisle, with the haylofts over them, ladders up
        for (int side : new int[]{-1, 1}) {
            int aisleEdge = BC + side * 3;           // the fence along the aisle
            int wall = side < 0 ? BU0 : BU1;
            int in0 = Math.min(aisleEdge, wall) + 1, in1 = Math.max(aisleEdge, wall) - 1;   // the stalls between
            for (int v = -5; v <= BV1 - 1; v++) this.put(aisleEdge, g + 1, v, Blocks.SPRUCE_FENCE);
            for (int v : new int[]{-6, -1, 3, 7}) {
                for (int u = in0; u <= in1; u++) {
                    this.put(u, g + 1, v, Blocks.SPRUCE_FENCE);
                    this.put(u, g + 2, v, Blocks.SPRUCE_FENCE);
                }
            }
            for (int v : new int[]{-3, 1, 5, 9}) {
                this.put(aisleEdge, g + 1, v, Blocks.SPRUCE_FENCE_GATE.defaultBlockState()
                        .setValue(FenceGateBlock.FACING, side < 0 ? Direction.EAST : Direction.WEST));
                this.put(wall - side, g + 1, v + 1, Blocks.HAY_BLOCK);
            }
            for (int v : new int[]{-5, 4}) {
                this.put(wall - side, g + 1, v, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
            }
            // the loft over the stalls, railed along the aisle, a few bales on it
            this.fill(in0, g + 5, -6, in1, g + 5, BV1 - 1, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
            this.fill(aisleEdge, g + 6, -6, aisleEdge, g + 6, BV1 - 1, Blocks.SPRUCE_FENCE);
            this.fill(wall - side, g + 6, 6, wall - 2 * side, g + 6, 8, Blocks.HAY_BLOCK);
            this.fill(wall - side, g + 7, 7, wall - side, g + 7, 8, Blocks.HAY_BLOCK);
            // the ladder up, against the south wall in the last stall
            int lu = aisleEdge + side;
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH);
            this.fill(lu, g + 1, BV1 - 1, lu, g + 6, BV1 - 1, ladder);
        }
        // the tack by the door, and lanterns down the aisle on long chains
        this.chest(BU1 - 1, g + 1, BV1 - 1, Direction.WEST, new ItemStack(Items.SADDLE, 2), new ItemStack(Items.LEAD, 3),
                new ItemStack(Items.WHEAT, 16), new ItemStack(Items.CARROT, 8), new ItemStack(Items.SHEARS));
        for (int v : new int[]{-4, 4}) {
            int roofY = yb + barnRoofHalf(BC) / 2;
            this.fill(BC, g + 12, v, BC, roofY - 1, v, Blocks.CHAIN);
            this.hangingLantern(BC, g + 11, v);
        }
    }

    // ---------------------------------------------------------------- the silos, by the barn
    private void silo(int g, int su, int sv, int height) {
        for (int du = -2; du <= 2; du++) {
            for (int dv = -2; dv <= 2; dv++) {
                int d2 = du * du + dv * dv;
                if (d2 > 5) continue;
                boolean wall = d2 >= 3;
                for (int y = g; y <= g + height; y++) {
                    this.put(su + du, y, sv + dv, wall || y == g ? Blocks.SMOOTH_STONE.defaultBlockState() : AIR);
                }
                // its domed cap
                this.put(su + du, g + height + 1, sv + dv, d2 >= 4 ? Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
                        : Blocks.SMOOTH_STONE.defaultBlockState());
                if (d2 <= 1) this.put(su + du, g + height + 2, sv + dv, Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
            }
        }
        // its steel hoops
        for (int y = g + 4; y <= g + height - 1; y += 5) {
            for (int du = -2; du <= 2; du++) {
                for (int dv = -2; dv <= 2; dv++) {
                    int d2 = du * du + dv * dv;
                    if (d2 >= 3 && d2 <= 5) this.put(su + du, y, sv + dv, Blocks.POLISHED_ANDESITE.defaultBlockState());
                }
            }
        }
        // the chute down its side, towards the barn
        this.fill(su - 3, g + 1, sv, su - 3, g + height, sv, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST));
    }

    // ================================================================ the machine shed: u -40..-26, v 20..34,
    // open to the yard on its east side, under a lean-to roof
    private static final int SU0 = -40, SU1 = -26, SV0 = 20, SV1 = 34;

    private void machineShed(int g) {
        this.fill(SU0, g, SV0, SU1, g, SV1, Blocks.GRAVEL);
        this.fill(SU0 + 1, g, SV0 + 1, SU1, g, SV1 - 1, Blocks.COARSE_DIRT);
        // the walls: weathered boards on posts; the front open but for its posts
        for (int y = g + 1; y <= g + 5; y++) {
            for (int u = SU0; u <= SU1; u++) {
                BlockState s = Math.floorMod(u - SU0, 4) == 0 || u == SU1 ? log(Blocks.STRIPPED_SPRUCE_LOG) : Blocks.SPRUCE_PLANKS.defaultBlockState();
                this.put(u, y, SV0, s);
                this.put(u, y, SV1, s);
            }
            for (int v = SV0; v <= SV1; v++) {
                BlockState s = Math.floorMod(v - SV0, 4) == 0 || v == SV1 ? log(Blocks.STRIPPED_SPRUCE_LOG) : Blocks.SPRUCE_PLANKS.defaultBlockState();
                this.put(SU0, y, v, s);
                this.put(SU1, y, v, v == SV0 || v == SV1 || Math.floorMod(v - SV0, 7) == 0 ? log(Blocks.STRIPPED_SPRUCE_LOG) : AIR);
            }
        }
        // windows in the back wall
        for (int v = SV0 + 2; v < SV1; v += 4) {
            this.put(SU0, g + 3, v, Blocks.GLASS_PANE);
            this.put(SU0, g + 3, v + 1, Blocks.GLASS_PANE);
        }
        // the lean-to roof: high at the back, low over the open front
        for (int u = SU0 - 1; u <= SU1 + 1; u++) {
            int h = ((SU1 + 1) - u) / 2;   // half blocks
            int y = g + 7 + h / 2;
            for (int v = SV0 - 1; v <= SV1 + 1; v++) {
                this.put(u, y, v, slab(Blocks.SPRUCE_SLAB, h % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP));
            }
            if (u >= SU0 && u <= SU1) {
                for (int yy = g + 6; yy < y; yy++) {
                    this.put(u, yy, SV0, Blocks.SPRUCE_PLANKS);
                    this.put(u, yy, SV1, Blocks.SPRUCE_PLANKS);
                }
            }
            if (u == SU0) {
                for (int yy = g + 6; yy < y; yy++) this.fill(u, yy, SV0, u, yy, SV1, Blocks.SPRUCE_PLANKS);
            }
        }
        this.fill(SU1, g + 6, SV0, SU1, g + 6, SV1, log(Blocks.STRIPPED_SPRUCE_LOG).setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
        // the red tractor, facing out of the shed: big rear wheels, small front ones, seat, wheel, hood and stack
        int tv = 26;
        BlockState red = Blocks.RED_TERRACOTTA.defaultBlockState(), black = Blocks.BLACK_TERRACOTTA.defaultBlockState();
        for (int dv : new int[]{-2, 2}) this.fill(-36, g + 1, tv + dv, -35, g + 2, tv + dv, black);
        this.put(-31, g + 1, tv - 1, black);
        this.put(-31, g + 1, tv + 1, black);
        this.fill(-36, g + 1, tv - 1, -36, g + 1, tv + 1, red);
        this.fill(-35, g + 1, tv, -31, g + 2, tv, red);
        this.put(-31, g + 1, tv, red);
        this.put(-30, g + 2, tv, Blocks.IRON_BARS);
        this.put(-36, g + 2, tv, red);
        this.put(-36, g + 3, tv, stairs(Blocks.BLACKSTONE_STAIRS, Direction.WEST, Half.BOTTOM));
        this.put(-34, g + 3, tv, Blocks.DARK_OAK_FENCE);
        this.put(-34, g + 4, tv, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        this.fill(-32, g + 3, tv, -32, g + 4, tv, Blocks.IRON_BARS);
        this.fill(-35, g + 2, tv - 1, -35, g + 2, tv + 1, red);
        // the plough behind it, and a hay wagon
        this.fill(-38, g + 1, tv - 1, -38, g + 1, tv + 1, Blocks.IRON_BARS);
        this.put(-39, g + 1, tv - 1, facing(Blocks.ANVIL, Direction.NORTH));
        this.fill(-37, g + 1, SV1 - 5, -30, g + 1, SV1 - 3, Blocks.SPRUCE_PLANKS);
        this.fill(-37, g + 2, SV1 - 5, -30, g + 2, SV1 - 3, Blocks.HAY_BLOCK);
        this.fill(-36, g + 3, SV1 - 4, -31, g + 3, SV1 - 4, Blocks.HAY_BLOCK);
        for (int u : new int[]{-37, -30}) {
            this.put(u, g + 1, SV1 - 6, black);
            this.put(u, g + 1, SV1 - 2, black);
        }
        // the workbench along the back wall, the tools and the oil drums
        this.put(SU0 + 1, g + 1, SV0 + 2, Blocks.CRAFTING_TABLE);
        this.put(SU0 + 1, g + 1, SV0 + 3, Blocks.SMITHING_TABLE);
        this.put(SU0 + 1, g + 1, SV0 + 4, facing(Blocks.GRINDSTONE, Direction.EAST)
                .setValue(net.minecraft.world.level.block.GrindstoneBlock.FACE, AttachFace.FLOOR));
        this.chest(SU0 + 1, g + 1, SV0 + 5, Direction.EAST, new ItemStack(Items.IRON_HOE), new ItemStack(Items.IRON_SHOVEL),
                new ItemStack(Items.IRON_AXE), new ItemStack(Items.BUCKET, 2), new ItemStack(Items.SHEARS),
                new ItemStack(Items.SADDLE), new ItemStack(Items.LEAD, 3), new ItemStack(ModItems.CORN.get(), 16),
                new ItemStack(ModItems.COTTON_SEEDS.get(), 16), new ItemStack(Items.WHEAT_SEEDS, 16));
        this.fill(SU0 + 1, g + 3, SV0 + 1, SU0 + 1, g + 3, SV0 + 6, slab(Blocks.SPRUCE_SLAB, SlabType.TOP));
        this.put(SU0 + 1, g + 1, SV0 + 1, Blocks.CAULDRON);
        this.put(SU0 + 2, g + 1, SV0 + 1, Blocks.CAULDRON);
        this.put(SU0 + 1, g + 1, SV1 - 1, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        this.put(SU0 + 2, g + 1, SV1 - 1, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        this.put(SU0 + 1, g + 2, SV1 - 1, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        this.hangingLantern(-33, g + 8, 23);
        this.hangingLantern(-33, g + 8, 31);
    }

    // ---------------------------------------------------------------- the chicken coop, u -20..-15, v 22..27,
    // and its run to the east
    private void chickenCoop(int g) {
        int u0 = -20, u1 = -15, v0 = 22, v1 = 27;
        this.fill(u0, g, v0, u1, g, v1, Blocks.SPRUCE_PLANKS);
        for (int y = g + 1; y <= g + 3; y++) {
            for (int u = u0; u <= u1; u++) {
                this.put(u, y, v0, u == u0 || u == u1 ? RED : WHITE);
                this.put(u, y, v1, u == u0 || u == u1 ? RED : WHITE);
            }
            for (int v = v0; v <= v1; v++) {
                this.put(u0, y, v, v == v0 || v == v1 ? RED : WHITE);
                this.put(u1, y, v, v == v0 || v == v1 ? RED : WHITE);
            }
        }
        for (int u = u0 - 1; u <= u1 + 1; u++) {
            int e = Math.min(u - (u0 - 1), (u1 + 1) - u);
            this.fill(u, g + 4 + e / 2, v0 - 1, u, g + 4 + e / 2, v1 + 1,
                    e >= 3 ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : slab(Blocks.DARK_OAK_SLAB, e % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP));
        }
        for (int v = v0; v <= v1; v++) {
            this.fill(u0 + 1, g + 4, v, u1 - 1, g + 4, v, WHITE);
        }
        // the pop hole into the run, a window, the nest boxes and perches
        this.fill(u1, g + 1, 24, u1, g + 2, 24, AIR);
        this.put(u0, g + 2, 25, Blocks.GLASS_PANE);
        for (int v = v0 + 1; v <= v1 - 1; v++) this.put(u0 + 1, g + 1, v, Blocks.HAY_BLOCK);
        this.put(u0 + 1, g + 2, v0 + 1, Blocks.COMPOSTER);
        this.fill(u1 - 2, g + 2, v0 + 1, u1 - 2, g + 2, v1 - 1, Blocks.SPRUCE_FENCE);
        this.fill(u1 - 2, g + 1, v0 + 1, u1 - 2, g + 1, v0 + 1, Blocks.SPRUCE_FENCE);
        this.hangingLantern(-17, g + 3, 25);
        // the run, fenced, with a gate on the yard side
        for (int u = u1; u <= -10; u++) {
            this.put(u, g + 1, 20, Blocks.OAK_FENCE);
            this.put(u, g + 1, 30, Blocks.OAK_FENCE);
        }
        for (int v = 20; v <= 30; v++) this.put(-10, g + 1, v, Blocks.OAK_FENCE);
        for (int v = 20; v <= v0 - 1; v++) this.put(u1, g + 1, v, Blocks.OAK_FENCE);
        for (int v = v1 + 1; v <= 30; v++) this.put(u1, g + 1, v, Blocks.OAK_FENCE);
        this.put(-12, g + 1, 20, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        this.put(-11, g + 1, 29, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
    }

    // ================================================================ the paddock: u -8..24, v 14..40
    private static final int PU0 = -8, PU1 = 24, PV0 = 14, PV1 = 40;

    private void paddock(int g) {
        for (int u = PU0; u <= PU1; u++) {
            this.put(u, g + 1, PV0, Blocks.OAK_FENCE);
            this.put(u, g + 1, PV1, Blocks.OAK_FENCE);
        }
        for (int v = PV0; v <= PV1; v++) {
            this.put(PU0, g + 1, v, Blocks.OAK_FENCE);
            this.put(PU1, g + 1, v, Blocks.OAK_FENCE);
        }
        this.put(PU0, g + 1, 16, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.WEST));
        this.put(12, g + 1, PV0, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
        this.fill(12, g, 2, 12, g, PV0 - 1, Blocks.DIRT_PATH);
        // the water trough and the hay
        this.fill(PU1 - 6, g, PV1 - 3, PU1 - 2, g, PV1 - 3, Blocks.WATER);
        this.fill(PU1 - 7, g, PV1 - 4, PU1 - 1, g, PV1 - 4, Blocks.STONE_BRICKS);
        this.fill(PU1 - 7, g, PV1 - 2, PU1 - 1, g, PV1 - 2, Blocks.STONE_BRICKS);
        this.put(PU1 - 7, g, PV1 - 3, Blocks.STONE_BRICKS);
        this.put(PU1 - 1, g, PV1 - 3, Blocks.STONE_BRICKS);
        this.fill(PU0 + 2, g + 1, PV1 - 3, PU0 + 4, g + 1, PV1 - 2, Blocks.HAY_BLOCK);
        this.put(PU0 + 3, g + 2, PV1 - 3, Blocks.HAY_BLOCK);
        // the field shelter in its corner: an open-fronted shed
        int s0 = PU1 - 7, s1 = PU1 - 1, t0 = PV0 + 1, t1 = PV0 + 5;
        for (int y = g + 1; y <= g + 3; y++) {
            for (int u = s0; u <= s1; u++) this.put(u, y, t0, RED);
            for (int v = t0; v <= t1; v++) {
                this.put(s0, y, v, v == t1 ? WHITE : RED);
                this.put(s1, y, v, v == t1 ? WHITE : RED);
            }
        }
        this.fill(s0 - 1, g + 4, t0 - 1, s1 + 1, g + 4, t1 + 1, slab(Blocks.DARK_OAK_SLAB, SlabType.BOTTOM));
        this.fill(s0 + 1, g + 1, t0 + 1, s0 + 2, g + 1, t0 + 1, Blocks.HAY_BLOCK);
    }

    // ---------------------------------------------------------------- the windmill, east of the paddock
    private void windmill(int g) {
        int cu = 31, cv = 25, top = g + 12;
        // the tower: four legs, cross-braced, and a platform
        for (int du = -1; du <= 1; du += 2) {
            for (int dv = -1; dv <= 1; dv += 2) {
                this.fill(cu + du, g + 1, cv + dv, cu + du, top, cv + dv, Blocks.DARK_OAK_FENCE);
            }
        }
        for (int y = g + 4; y <= top; y += 4) {
            this.fill(cu - 1, y, cv - 1, cu + 1, y, cv - 1, Blocks.DARK_OAK_FENCE);
            this.fill(cu - 1, y, cv + 1, cu + 1, y, cv + 1, Blocks.DARK_OAK_FENCE);
            this.fill(cu - 1, y, cv, cu - 1, y, cv, Blocks.DARK_OAK_FENCE);
            this.fill(cu + 1, y, cv, cu + 1, y, cv, Blocks.DARK_OAK_FENCE);
        }
        this.fill(cu - 1, top + 1, cv - 1, cu + 1, top + 1, cv + 1, slab(Blocks.SPRUCE_SLAB, SlabType.BOTTOM));
        this.fill(cu, g + 1, cv, cu, top + 2, cv, Blocks.IRON_BARS);
        // the wheel, facing south, and the tail vane behind it
        int hy = top + 5;
        this.put(cu, hy, cv, Blocks.DARK_OAK_PLANKS);
        this.put(cu, hy, cv - 1, Blocks.DARK_OAK_PLANKS);
        this.fill(cu, top + 3, cv, cu, hy - 1, cv, Blocks.DARK_OAK_FENCE);
        for (int a = -3; a <= 3; a++) {
            for (int b = -3; b <= 3; b++) {
                if (a == 0 && b == 0) continue;
                int r2 = a * a + b * b;
                boolean spoke = a == 0 || b == 0 || Math.abs(a) == Math.abs(b);
                if (r2 <= 10 && spoke && r2 >= 1) this.put(cu + a, hy + b, cv - 1, r2 >= 4 ? WHITE : Blocks.DARK_OAK_FENCE.defaultBlockState());
            }
        }
        this.fill(cu, hy, cv + 1, cu, hy, cv + 3, Blocks.DARK_OAK_FENCE);
        this.fill(cu, hy - 1, cv + 4, cu, hy + 1, cv + 5, RED);
        // the pump's trough at its foot
        this.fill(cu + 2, g, cv - 1, cu + 4, g, cv + 1, Blocks.STONE_BRICKS);
        this.put(cu + 3, g, cv, Blocks.WATER);
    }

    // ---------------------------------------------------------------- the animals
    private void animals(int g) {
        RandomSource random = RandomSource.create(this.seed);
        EntityType<?>[] kinds = {EntityType.HORSE, EntityType.HORSE, EntityType.HORSE, EntityType.HORSE, EntityType.PIG,
                EntityType.PIG, EntityType.PIG, EntityType.SHEEP, EntityType.SHEEP, EntityType.SHEEP, EntityType.COW,
                EntityType.COW, EntityType.COW};
        for (EntityType<?> kind : kinds) {
            this.spawn(kind, PU0 + 2 + random.nextInt(PU1 - PU0 - 10), g + 1, PV0 + 7 + random.nextInt(PV1 - PV0 - 12), random);
        }
        for (int i = 0; i < 6; i++) {
            this.spawn(EntityType.CHICKEN, -14 + random.nextInt(4), g + 1, 21 + random.nextInt(9), random);
        }
        this.spawn(EntityType.CAT, 15, g + 1, -24, random);
    }

    private void spawn(EntityType<?> kind, int u, int y, int v, RandomSource random) {
        BlockPos p = this.at(u, y, v);
        if (!this.box.isInside(p)) return;
        if (!(kind.create(this.level.getLevel()) instanceof Mob mob)) return;
        mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        this.level.addFreshEntityWithPassengers(mob);
    }
}
