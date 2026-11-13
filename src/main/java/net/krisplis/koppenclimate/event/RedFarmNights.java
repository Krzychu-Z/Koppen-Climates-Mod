package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * The red farms' nights (RedFarmPiece): after dark, skeleton horsemen ride in - skeletons with bows on skeleton
 * horses, as many as eight at a time around a player at the farm, a new one every few seconds. At sunrise the
 * riders burn as skeletons do, and the horses nobody has tamed fade away.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RedFarmNights {
    private static final String TAG = KoppenClimateMod.MOD_ID + ".red_farm_horseman";
    private static final int MAX_RIDERS = 8;
    private static final double REACH = 48.0;
    // how far round the farm they come in: the farm itself and a little beyond
    private static final int MARGIN = 16;

    private static final ResourceKey<Structure> RED_FARM = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "red_farm"));

    private RedFarmNights() {
    }

    /** The red farm whose grounds (with a margin round them) take in this spot, or an invalid start. */
    private static StructureStart farm(ServerLevel level, BlockPos pos) {
        for (BlockPos p : new BlockPos[]{pos, pos.offset(MARGIN, 0, 0), pos.offset(-MARGIN, 0, 0), pos.offset(0, 0, MARGIN), pos.offset(0, 0, -MARGIN)}) {
            StructureStart start = level.structureManager().getStructureWithPieceAt(p, h -> h.is(RED_FARM));
            if (start.isValid()) return start;
        }
        return StructureStart.INVALID_START;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 60 != 17 || !(player.level() instanceof ServerLevel level)) return;
        AABB around = player.getBoundingBox().inflate(REACH, 24.0, REACH);
        List<SkeletonHorse> horses = level.getEntitiesOfClass(SkeletonHorse.class, around, h -> h.getTags().contains(TAG));
        if (level.isDay()) {
            for (SkeletonHorse horse : horses) {
                if (horse.getOwnerUUID() != null || horse.isLeashed() || horse.getPassengers().stream().anyMatch(p -> p instanceof ServerPlayer)) continue;
                level.sendParticles(ParticleTypes.SOUL, horse.getX(), horse.getY() + 1.0, horse.getZ(), 20, 0.5, 0.6, 0.5, 0.02);
                horse.ejectPassengers();
                horse.discard();
            }
            return;
        }
        if (horses.size() >= MAX_RIDERS) return;
        StructureStart start = farm(level, player.blockPosition());
        if (!start.isValid()) return;
        var bounds = start.getBoundingBox().inflatedBy(MARGIN);
        var random = level.random;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = 16 + random.nextDouble() * 20;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance), z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!bounds.isInside(pos) || !level.getBlockState(pos.below()).getFluidState().isEmpty()) continue;
            if (!level.noCollision(EntityType.SKELETON_HORSE.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) continue;
            SkeletonHorse horse = EntityType.SKELETON_HORSE.create(level);
            Skeleton rider = EntityType.SKELETON.create(level);
            if (horse == null || rider == null) return;
            horse.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            horse.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            horse.setTamed(true);   // so it carries its rider (it's still no one's)
            horse.addTag(TAG);
            rider.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, horse.getYRot(), 0.0F);
            rider.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            rider.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            rider.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            rider.setDropChance(EquipmentSlot.HEAD, 0.0F);
            rider.addTag(TAG);
            level.addFreshEntity(horse);
            level.addFreshEntity(rider);
            rider.startRiding(horse);
            level.playSound(null, pos, SoundEvents.SKELETON_HORSE_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.8F);
            return;
        }
    }
}
