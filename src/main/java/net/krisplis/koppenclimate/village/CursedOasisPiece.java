package net.krisplis.koppenclimate.village;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * A Cursed Oasis: a round pond of clear water in the middle of the desert, ringed with green - grass, sugar cane
 * along the water, date palms - and a wandering trader resting by it with a couple of camels. But it is a mirage:
 * the moment anyone steps into the water, the whole pond is gone (see event/CursedOasis), and by day Dust Devils
 * whirl about it.
 *
 * <p>The piece's box reaches well beyond the pond (RANGE), so that anyone near enough to the oasis to see its Dust
 * Devils is "at" it; it only builds within REACH of the middle.
 */
public class CursedOasisPiece extends StructurePiece {
    public static final double POND = 5.5;
    static final int FLAT = 10, MARGIN = 6, REACH = FLAT + MARGIN, RANGE = 40;
    private static final String[] PALMS = {"coconut_palm", "coconut_palm_small", "coconut_palm_tall", "coconut_palm_crooked"};

    final BlockPos centre;
    final long seed;

    public CursedOasisPiece(BlockPos centre, long seed) {
        super(VillageRegistry.CURSED_OASIS_PIECE.get(), 0, new BoundingBox(centre.getX() - RANGE, centre.getY() - 12,
                centre.getZ() - RANGE, centre.getX() + RANGE, centre.getY() + 30, centre.getZ() + RANGE));
        this.centre = centre;
        this.seed = seed;
    }

    public CursedOasisPiece(CompoundTag tag) {
        super(VillageRegistry.CURSED_OASIS_PIECE.get(), tag);
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

    public BlockPos centre() {
        return this.centre;
    }

    /** The pond's outline, a little wavy: how far out the water reaches at this angle. */
    double edge(double angle) {
        double phase = (this.seed & 1023) / 1023.0 * Math.PI * 2;
        return POND * (1.0 + 0.1 * Math.sin(angle * 3.0 + phase) + 0.05 * Math.sin(angle * 5.0 - phase));
    }

    int roll(int x, int z, int salt, int n) {
        long h = (x * 341873128712L + z * 132897987541L + salt * 6971L) ^ this.seed;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) Math.floorMod(h, (long) n);
    }

    static boolean isGround(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.blocksMotion() && !state.is(BlockTags.LEAVES)
                && !state.is(BlockTags.LOGS) && !state.is(Blocks.CACTUS);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        int f = this.centre.getY(), cx = this.centre.getX(), cz = this.centre.getZ();
        if (box.maxX() < cx - REACH || box.minX() > cx + REACH || box.maxZ() < cz - REACH || box.minZ() > cz + REACH) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX(), cx - REACH); x <= Math.min(box.maxX(), cx + REACH); x++) {
            for (int z = Math.max(box.minZ(), cz - REACH); z <= Math.min(box.maxZ(), cz + REACH); z++) {
                int dx = x - cx, dz = z - cz;
                double d = Mth.length(dx, dz);
                if (d > REACH) continue;
                // the ground: levelled round the pond, blending back into the dunes beyond
                int ground = f - 12;
                for (int y = f + 20; y > f - 12; y--) {
                    if (isGround(level.getBlockState(pos.set(x, y, z)))) {
                        ground = y;
                        break;
                    }
                }
                double t = Mth.clamp((d - FLAT) / MARGIN, 0.0, 1.0);
                t = t * t * (3 - 2 * t);
                int target = (int) Math.round(f + (ground - f) * t);
                for (int y = target + 1; y <= Math.max(ground, target) + 8; y++) {
                    // (the dunes and what grows on them - but never a palm already standing here)
                    BlockState above = level.getBlockState(pos.set(x, y, z));
                    if (!above.isAir() && !above.is(BlockTags.LOGS) && !above.is(BlockTags.LEAVES)) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
                for (int y = ground + 1; y <= target; y++) {
                    level.setBlock(pos.set(x, y, z), y < target - 2 ? Blocks.SANDSTONE.defaultBlockState() : Blocks.SAND.defaultBlockState(), 2);
                }
                if (d > FLAT) continue;
                double q = d / this.edge(Math.atan2(dz, dx));
                if (q <= 1.0) {
                    // the pond: a shallow bowl of clear water on a sandy bed
                    int depth = q < 0.6 ? 2 : 1;
                    for (int y = f; y > f - depth; y--) level.setBlock(pos.set(x, y, z), Blocks.WATER.defaultBlockState(), 2);
                    level.setBlock(pos.set(x, f - depth, z), this.roll(x, z, 1, 5) == 0 ? Blocks.CLAY.defaultBlockState() : Blocks.SAND.defaultBlockState(), 2);
                    if (!isGround(level.getBlockState(pos.set(x, f - depth - 1, z)))) level.setBlock(pos, Blocks.SANDSTONE.defaultBlockState(), 2);
                    continue;
                }
                // the green ring about it
                if (q <= 1.7 && this.roll(x, z, 2, 10) < (q < 1.4 ? 9 : 5)) {
                    level.setBlock(pos.set(x, f, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    level.setBlock(pos.set(x, f - 1, z), Blocks.DIRT.defaultBlockState(), 2);
                    int r = this.roll(x, z, 3, 12);
                    if (q <= 1.25 && r < 5) {
                        // sugar cane along the water
                        int h = 2 + this.roll(x, z, 4, 2);
                        for (int y = 1; y <= h; y++) level.setBlock(pos.set(x, f + y, z), Blocks.SUGAR_CANE.defaultBlockState(), 2);
                    } else if (r < 7) {
                        level.setBlock(pos.set(x, f + 1, z), Blocks.SHORT_GRASS.defaultBlockState(), 2);
                    } else if (r == 7) {
                        level.setBlock(pos.set(x, f + 1, z), Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), 2);
                        level.setBlock(pos.set(x, f + 2, z), Blocks.TALL_GRASS.defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), 2);
                    }
                } else {
                    level.setBlock(pos.set(x, f, z), Blocks.SAND.defaultBlockState(), 2);
                }
            }
        }
        // the palms, round the pond
        var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        int palms = 5 + this.roll(0, 0, 5, 3);
        for (int i = 0; i < palms; i++) {
            double a = (i + this.roll(i, 0, 6, 100) / 200.0) * Math.PI * 2 / palms;
            double r = this.edge(a) * 1.35 + this.roll(i, 1, 7, 3);
            int x = cx + (int) Math.round(Math.cos(a) * r), z = cz + (int) Math.round(Math.sin(a) * r);
            if (!box.isInside(new BlockPos(x, f + 1, z))) continue;
            ConfiguredFeature<?, ?> palm = features.get(ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, PALMS[this.roll(i, 2, 8, PALMS.length)])));
            if (palm == null) continue;
            level.setBlock(pos.set(x, f, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = f + 1; y <= f + 3; y++) level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            palm.place(level, generator, RandomSource.create(this.seed + i), new BlockPos(x, f + 1, z));
        }
        // the wandering trader resting by the water, his camels beside him
        BlockPos rest = new BlockPos(cx + (int) Math.round(POND + 2.5), f + 1, cz);
        if (box.isInside(rest)) {
            this.spawn(level, EntityType.WANDERING_TRADER, rest);
            this.spawn(level, EntityType.CAMEL, rest.offset(0, 0, 3));
            this.spawn(level, EntityType.CAMEL, rest.offset(2, 0, -3));
        }
    }

    private void spawn(WorldGenLevel level, EntityType<?> type, BlockPos pos) {
        BlockPos.MutableBlockPos at = pos.mutable();
        while (!level.getBlockState(at).isAir() && at.getY() < pos.getY() + 4) at.move(0, 1, 0);
        if (!(type.create(level.getLevel()) instanceof Mob mob)) return;
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntityWithPassengers(mob);
    }
}
