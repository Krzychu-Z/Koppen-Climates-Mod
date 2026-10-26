package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraftforge.common.world.PieceBeardifierModifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The whole Maasai settlement is one piece, built column by column inside whichever chunk is being decorated.
 * The layout comes from a seed saved with the piece, so every chunk works out the same settlement.
 *
 * <ul>
 *   <li>a round yard of trampled earth about 50 blocks across, cleared of trees and grass;</li>
 *   <li>round it a hedge of thorny branches (mangrove roots) two blocks high, with two gates facing each other;</li>
 *   <li>a ring of low, rounded 7x5 huts of packed mud with mud brick roofs, doorways facing the middle, a small
 *       vent hole, and two sleeping mats inside;</li>
 *   <li>in the middle the cattle kraal, a round acacia fence with a gate, with the cows inside;</li>
 *   <li>cooking fires between the kraal and the huts;</li>
 *   <li>one or two villagers per hut and the settlement's elder, zebras between the huts and the hedge,
 *       and a lot of cats.</li>
 * </ul>
 */
public class AfricanVillagePiece extends StructurePiece implements PieceBeardifierModifier {
    // the yard reaches the hedge, which stands on this ring
    private static final float HEDGE_RADIUS = 24.0F;
    private static final int BOX_RADIUS = 26;
    // the huts' centres stand on this ring
    private static final int HUT_RING = 15;
    private static final float KRAAL_RADIUS = 5.0F;
    private static final int FIRE_RING = 9;
    // trees and plants are cleared this high above the yard
    private static final int CLEAR_HEIGHT = 40;
    // and holes under it are filled this deep
    private static final int FILL_DEPTH = 14;
    // no hut stands this close (in radians) to a gate
    private static final float GATE_CLEARANCE = 0.45F;

    private final BlockPos centre;
    private final long seed;

    public AfricanVillagePiece(BlockPos centre, long seed) {
        super(VillageRegistry.AFRICAN_VILLAGE_PIECE.get(), 0, new BoundingBox(
                centre.getX() - BOX_RADIUS, centre.getY() - FILL_DEPTH, centre.getZ() - BOX_RADIUS,
                centre.getX() + BOX_RADIUS, centre.getY() + CLEAR_HEIGHT, centre.getZ() + BOX_RADIUS));
        this.centre = centre;
        this.seed = seed;
    }

    public AfricanVillagePiece(CompoundTag tag) {
        super(VillageRegistry.AFRICAN_VILLAGE_PIECE.get(), tag);
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

    // ---------------------------------------------------------------- terrain adaptation

    @Override
    public BoundingBox getBeardifierBox() {
        return new BoundingBox(this.centre.getX() - 25, this.centre.getY(), this.centre.getZ() - 25,
                this.centre.getX() + 25, this.centre.getY() + 12, this.centre.getZ() + 25);
    }

    @Override
    public TerrainAdjustment getTerrainAdjustment() {
        return TerrainAdjustment.BEARD_THIN;
    }

    @Override
    public int getGroundLevelDelta() {
        return 1;
    }

    // ---------------------------------------------------------------- layout

    record Hut(int x, int z, Direction door, boolean secondVillager) {
    }

    /** Where everything goes; worked out the same way from the seed for every chunk. */
    static final class Layout {
        final List<Hut> huts = new ArrayList<>();
        // the hedge and the kraal fence, as (x, z) columns; the gates are in them too
        final Set<Long> hedge = new HashSet<>();
        final Set<Long> hedgeGates = new HashSet<>();
        final Set<Long> kraal = new HashSet<>();
        long kraalGate;
        Direction kraalGateFacing;
        final List<BlockPos> fires = new ArrayList<>();
        final List<BlockPos> cows = new ArrayList<>();
        final List<BlockPos> zebras = new ArrayList<>();
        final List<BlockPos> cats = new ArrayList<>();
        BlockPos elderSpawn;

        Layout(BlockPos centre, long seed) {
            RandomSource random = RandomSource.create(seed);
            int cx = centre.getX(), cz = centre.getZ(), y = centre.getY() + 1;
            float gateAngle = random.nextFloat() * Mth.TWO_PI;

            // the thorn hedge and the kraal fence: every column inside the circle that touches one outside it,
            // which makes a closed ring with no diagonal gaps
            ring(cx, cz, HEDGE_RADIUS, this.hedge);
            ring(cx, cz, KRAAL_RADIUS, this.kraal);
            for (int g = 0; g < 2; g++) {
                float a = gateAngle + g * Mth.PI;
                this.hedgeGates.add(nearest(this.hedge, cx + HEDGE_RADIUS * Mth.cos(a), cz + HEDGE_RADIUS * Mth.sin(a)));
            }
            this.kraalGate = nearest(this.kraal, cx + KRAAL_RADIUS * Mth.cos(gateAngle), cz + KRAAL_RADIUS * Mth.sin(gateAngle));
            this.kraalGateFacing = towards(BlockPos.getX(this.kraalGate) - cx, BlockPos.getZ(this.kraalGate) - cz);

            // the huts, in two arcs between the gates
            int perArc = 3 + random.nextInt(2);
            for (int arc = 0; arc < 2; arc++) {
                float from = gateAngle + arc * Mth.PI + GATE_CLEARANCE;
                float span = Mth.PI - 2 * GATE_CLEARANCE;
                for (int i = 0; i < perArc; i++) {
                    float a = from + span * (i + 0.5F) / perArc;
                    int x = cx + Math.round(HUT_RING * Mth.cos(a));
                    int z = cz + Math.round(HUT_RING * Mth.sin(a));
                    this.huts.add(new Hut(x, z, towards(cx - x, cz - z), random.nextFloat() < 0.5F));
                }
            }
            // cooking fires, one in each quarter, away from the gates
            for (int i = 0; i < 4; i++) {
                float a = gateAngle + Mth.HALF_PI * (i + 0.5F);
                this.fires.add(new BlockPos(cx + Math.round(FIRE_RING * Mth.cos(a)), y - 1, cz + Math.round(FIRE_RING * Mth.sin(a))));
            }
            float elderAngle = gateAngle + Mth.PI + 0.35F;
            this.elderSpawn = new BlockPos(cx + Math.round(7 * Mth.cos(elderAngle)), y, cz + Math.round(7 * Mth.sin(elderAngle)));

            // animals: the cows in the kraal, zebras out by the hedge, cats everywhere
            int cowCount = 5 + random.nextInt(3);
            for (int i = 0; i < cowCount; i++) {
                float a = random.nextFloat() * Mth.TWO_PI, r = random.nextFloat() * 2.5F;
                this.cows.add(new BlockPos(cx + Math.round(r * Mth.cos(a)), y, cz + Math.round(r * Mth.sin(a))));
            }
            int zebraCount = 3 + random.nextInt(3);
            for (int i = 0; i < zebraCount; i++) {
                float a = random.nextFloat() * Mth.TWO_PI, r = 19.5F + random.nextFloat() * 2.0F;
                this.zebras.add(new BlockPos(cx + Math.round(r * Mth.cos(a)), y, cz + Math.round(r * Mth.sin(a))));
            }
            int catCount = 12 + random.nextInt(7);
            for (int i = 0; i < catCount; i++) {
                float a = random.nextFloat() * Mth.TWO_PI, r = 7.0F + random.nextFloat() * 14.0F;
                this.cats.add(new BlockPos(cx + Math.round(r * Mth.cos(a)), y, cz + Math.round(r * Mth.sin(a))));
            }
        }

        private static void ring(int cx, int cz, float radius, Set<Long> out) {
            int r = Mth.ceil(radius) + 1;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (!inside(dx, dz, radius)) continue;
                    boolean edge = false;
                    for (int ex = -1; ex <= 1 && !edge; ex++) {
                        for (int ez = -1; ez <= 1; ez++) {
                            if (!inside(dx + ex, dz + ez, radius)) {
                                edge = true;
                                break;
                            }
                        }
                    }
                    if (edge) out.add(BlockPos.asLong(cx + dx, 0, cz + dz));
                }
            }
        }

        private static boolean inside(int dx, int dz, float radius) {
            return dx * dx + dz * dz <= radius * radius;
        }

        private static long nearest(Set<Long> ring, float x, float z) {
            long best = 0;
            float bestD = Float.MAX_VALUE;
            for (long p : ring) {
                float dx = BlockPos.getX(p) - x, dz = BlockPos.getZ(p) - z;
                // only a straight stretch of ring takes a gate: its neighbours along the ring line up with it
                float d = dx * dx + dz * dz;
                if (d < bestD && isStraight(ring, p)) {
                    bestD = d;
                    best = p;
                }
            }
            return best;
        }

        private static boolean isStraight(Set<Long> ring, long p) {
            int x = BlockPos.getX(p), z = BlockPos.getZ(p);
            boolean ns = ring.contains(BlockPos.asLong(x, 0, z - 1)) && ring.contains(BlockPos.asLong(x, 0, z + 1));
            boolean ew = ring.contains(BlockPos.asLong(x - 1, 0, z)) && ring.contains(BlockPos.asLong(x + 1, 0, z));
            return ns != ew;
        }

        static Direction towards(int dx, int dz) {
            if (Math.abs(dx) > Math.abs(dz)) {
                return dx > 0 ? Direction.EAST : Direction.WEST;
            }
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    // ---------------------------------------------------------------- building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Layout layout = new Layout(this.centre, this.seed);
        int y0 = this.centre.getY();

        this.buildYard(level, box, y0);
        this.buildHedge(level, box, layout, y0);
        this.buildKraal(level, box, layout, y0);
        for (Hut hut : layout.huts) {
            this.buildHut(level, box, hut, y0);
        }
        for (BlockPos fire : layout.fires) {
            this.set(level, box, fire, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            for (Direction d : Direction.Plane.HORIZONTAL) {
                this.set(level, box, fire.relative(d), Blocks.PACKED_MUD.defaultBlockState());
            }
        }

        for (Hut hut : layout.huts) {
            this.spawnVillager(level, box, new BlockPos(hut.x(), y0 + 1, hut.z()), false);
            if (hut.secondVillager()) {
                this.spawnVillager(level, box, new BlockPos(hut.x(), y0 + 1, hut.z()), false);
            }
        }
        this.spawnVillager(level, box, layout.elderSpawn, true);
        for (BlockPos pos : layout.cows) this.spawnAnimal(level, box, EntityType.COW, pos);
        for (BlockPos pos : layout.zebras) this.spawnAnimal(level, box, net.krisplis.koppenclimate.entity.ModEntities.ZEBRA.get(), pos);
        for (BlockPos pos : layout.cats) this.spawnAnimal(level, box, EntityType.CAT, pos);
    }

    // a cheap hash of a column, the same in every chunk
    private static int hash(int x, int z) {
        int h = x * 73428767 ^ z * 912931;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return (h ^ (h >>> 15)) & 0x7fffffff;
    }

    /** Clears the yard of trees and grass, fills any holes and lays trampled earth over it. */
    private void buildYard(WorldGenLevel level, BoundingBox box, int y0) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cx = this.centre.getX(), cz = this.centre.getZ();
        float r2 = (HEDGE_RADIUS + 0.5F) * (HEDGE_RADIUS + 0.5F);
        for (int x = Math.max(box.minX(), cx - BOX_RADIUS); x <= Math.min(box.maxX(), cx + BOX_RADIUS); x++) {
            for (int z = Math.max(box.minZ(), cz - BOX_RADIUS); z <= Math.min(box.maxZ(), cz + BOX_RADIUS); z++) {
                int dx = x - cx, dz = z - cz;
                if (dx * dx + dz * dz > r2) continue;
                for (int y = y0 + 1; y <= y0 + CLEAR_HEIGHT; y++) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                int h = hash(x, z) % 10;
                BlockState top = h < 6 ? Blocks.COARSE_DIRT.defaultBlockState()
                        : h < 9 ? Blocks.DIRT_PATH.defaultBlockState() : Blocks.ROOTED_DIRT.defaultBlockState();
                pos.set(x, y0, z);
                level.setBlock(pos, top, 2);
                for (int y = y0 - 1; y >= y0 - FILL_DEPTH; y--) {
                    pos.set(x, y, z);
                    if (isGround(level.getBlockState(pos))) break;
                    level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }
    }

    private static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion()
                && !state.is(BlockTags.LEAVES) && !state.is(BlockTags.LOGS);
    }

    /** The thorn hedge: tangled branches two blocks high, with an acacia gate under a branch lintel at each entrance. */
    private void buildHedge(WorldGenLevel level, BoundingBox box, Layout layout, int y0) {
        BlockState roots = Blocks.MANGROVE_ROOTS.defaultBlockState();
        int cx = this.centre.getX(), cz = this.centre.getZ();
        for (long p : layout.hedge) {
            int x = BlockPos.getX(p), z = BlockPos.getZ(p);
            if (!box.isInside(x, y0, z)) continue;
            this.fillDown(level, box, x, y0 - 1, z, Blocks.DIRT.defaultBlockState());
            this.set(level, box, x, y0, z, Blocks.COARSE_DIRT.defaultBlockState());
            if (layout.hedgeGates.contains(p)) {
                Direction facing = gateFacing(layout.hedge, p, x - cx, z - cz);
                this.set(level, box, x, y0 + 1, z, Blocks.ACACIA_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing));
                this.set(level, box, x, y0 + 2, z, Blocks.AIR.defaultBlockState());
                this.set(level, box, x, y0 + 3, z, roots);
            } else {
                this.set(level, box, x, y0 + 1, z, roots);
                this.set(level, box, x, y0 + 2, z, roots);
                // the hedge is uneven: here and there a branch sticks up higher
                if (hash(x, z) % 5 == 0) this.set(level, box, x, y0 + 3, z, roots);
            }
        }
    }

    // a gate faces across the ring, so it swings open into and out of the yard
    private static Direction gateFacing(Set<Long> ring, long p, int dx, int dz) {
        int x = BlockPos.getX(p), z = BlockPos.getZ(p);
        boolean ringAlongZ = ring.contains(BlockPos.asLong(x, 0, z - 1)) && ring.contains(BlockPos.asLong(x, 0, z + 1));
        if (ringAlongZ) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /** The cattle kraal: a round acacia fence with one gate, around a floor of trodden mud and dung. */
    private void buildKraal(WorldGenLevel level, BoundingBox box, Layout layout, int y0) {
        int cx = this.centre.getX(), cz = this.centre.getZ();
        int r = Mth.ceil(KRAAL_RADIUS);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > KRAAL_RADIUS * KRAAL_RADIUS) continue;
                int x = cx + dx, z = cz + dz;
                int h = hash(x, z) % 4;
                this.set(level, box, x, y0, z, h == 0 ? Blocks.MUD.defaultBlockState()
                        : h == 1 ? Blocks.PACKED_MUD.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        for (long p : layout.kraal) {
            int x = BlockPos.getX(p), z = BlockPos.getZ(p);
            if (p == layout.kraalGate) {
                Direction facing = gateFacing(layout.kraal, p, x - cx, z - cz);
                this.set(level, box, x, y0 + 1, z, Blocks.ACACIA_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, facing));
                continue;
            }
            // the fence joins up with its neighbours (and the gate), as it would if placed by hand
            BlockState fence = Blocks.ACACIA_FENCE.defaultBlockState();
            fence = fence.setValue(CrossCollisionBlock.NORTH, layout.kraal.contains(BlockPos.asLong(x, 0, z - 1)))
                    .setValue(CrossCollisionBlock.SOUTH, layout.kraal.contains(BlockPos.asLong(x, 0, z + 1)))
                    .setValue(CrossCollisionBlock.WEST, layout.kraal.contains(BlockPos.asLong(x - 1, 0, z)))
                    .setValue(CrossCollisionBlock.EAST, layout.kraal.contains(BlockPos.asLong(x + 1, 0, z)));
            this.set(level, box, x, y0 + 1, z, fence);
        }
    }

    /**
     * A low, rounded hut of packed mud, 7 blocks along and 5 deep, with corners cut off. The walls are two blocks
     * high, the roof a layer of packed mud edged with mud brick slabs and a low ridge along the middle. The doorway
     * faces the middle of the settlement; a small vent hole in each end lets the smoke out. Inside: two sleeping
     * mats along the back wall and a torch between them.
     */
    private void buildHut(WorldGenLevel level, BoundingBox box, Hut hut, int y0) {
        BlockState mud = Blocks.PACKED_MUD.defaultBlockState();
        BlockState slab = Blocks.MUD_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        BlockState air = Blocks.AIR.defaultBlockState();
        Direction f = hut.door(), s = f.getClockWise();
        for (int a = -3; a <= 3; a++) {
            for (int b = -2; b <= 2; b++) {
                if (!inHut(a, b)) continue;
                int x = hut.x() + s.getStepX() * a + f.getStepX() * b;
                int z = hut.z() + s.getStepZ() * a + f.getStepZ() * b;
                boolean wall = !inHut(a + 1, b) || !inHut(a - 1, b) || !inHut(a, b + 1) || !inHut(a, b - 1);
                this.fillDown(level, box, x, y0 - 1, z, Blocks.DIRT.defaultBlockState());
                this.set(level, box, x, y0, z, wall ? mud : Blocks.COARSE_DIRT.defaultBlockState());
                for (int y = y0 + 1; y <= y0 + 2; y++) {
                    this.set(level, box, x, y, z, wall ? mud : air);
                }
                this.set(level, box, x, y0 + 3, z, wall ? slab : mud);
                if (b == 0 && Math.abs(a) <= 1) {
                    this.set(level, box, x, y0 + 4, z, slab);
                }
            }
        }
        // the doorway, two blocks high, in the middle of the front wall
        for (int y = y0 + 1; y <= y0 + 2; y++) {
            this.set(level, box, this.at(hut, 0, 2, y), air);
        }
        // a vent hole high in each end wall
        this.set(level, box, this.at(hut, 3, 0, y0 + 2), air);
        this.set(level, box, this.at(hut, -3, 0, y0 + 2), air);
        // a torch on the back wall, facing the door
        this.set(level, box, this.at(hut, 0, -1, y0 + 2), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, f));
        // two sleeping mats along the back wall, pillows to the ends of the hut
        BlockState mat = ModBlocks.SLEEPING_MAT.get().defaultBlockState();
        this.set(level, box, this.at(hut, -1, -1, y0 + 1), mat.setValue(BedBlock.FACING, s.getOpposite()).setValue(BedBlock.PART, BedPart.FOOT));
        this.set(level, box, this.at(hut, -2, -1, y0 + 1), mat.setValue(BedBlock.FACING, s.getOpposite()).setValue(BedBlock.PART, BedPart.HEAD));
        this.set(level, box, this.at(hut, 1, -1, y0 + 1), mat.setValue(BedBlock.FACING, s).setValue(BedBlock.PART, BedPart.FOOT));
        this.set(level, box, this.at(hut, 2, -1, y0 + 1), mat.setValue(BedBlock.FACING, s).setValue(BedBlock.PART, BedPart.HEAD));
    }

    // the hut's floor plan: 7x5 with the corners cut off
    private static boolean inHut(int a, int b) {
        return Math.abs(a) <= 3 && Math.abs(b) <= 2 && !(Math.abs(a) == 3 && Math.abs(b) == 2);
    }

    // a position in a hut: a blocks to the side, b blocks towards the door
    private BlockPos at(Hut hut, int a, int b, int y) {
        Direction f = hut.door(), s = f.getClockWise();
        return new BlockPos(hut.x() + s.getStepX() * a + f.getStepX() * b, y, hut.z() + s.getStepZ() * a + f.getStepZ() * b);
    }

    private void spawnVillager(WorldGenLevel level, BoundingBox box, BlockPos pos, boolean elder) {
        if (!box.isInside(pos)) {
            return;
        }
        AfricanVillagerEntity villager = VillageRegistry.AFRICAN_VILLAGER.get().create(level.getLevel());
        if (villager == null) {
            return;
        }
        villager.setLeader(elder);
        villager.setVillage(this.centre.above());
        villager.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        villager.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        villager.setPersistenceRequired();
        level.addFreshEntityWithPassengers(villager);
    }

    private void spawnAnimal(WorldGenLevel level, BoundingBox box, EntityType<? extends Mob> type, BlockPos pos) {
        // not inside a wall, nor on a fire
        if (!box.isInside(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()
                || level.getBlockState(pos.below()).is(Blocks.CAMPFIRE)) {
            return;
        }
        Mob mob = type.create(level.getLevel());
        if (mob == null) {
            return;
        }
        mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }

    // ---------------------------------------------------------------- helpers

    private void set(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    private void set(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        this.set(level, box, new BlockPos(x, y, z), state);
    }

    /** Fills from {@code y} down until it meets solid ground, so nothing floats. */
    private void fillDown(WorldGenLevel level, BoundingBox box, int x, int y, int z, BlockState state) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        if (!box.isInside(pos)) {
            return;
        }
        for (int i = 0; i < FILL_DEPTH && !isGround(level.getBlockState(pos)); i++) {
            level.setBlock(pos, state, 2);
            pos.move(Direction.DOWN);
        }
    }
}
