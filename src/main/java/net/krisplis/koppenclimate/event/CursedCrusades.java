package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.TemplarEntity;
import net.krisplis.koppenclimate.village.CathedralPiece;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Cursed Crusade. The haunted Gothic Cathedral of the temperate forest has the ghost towns' pentagram on its
 * nave's floor, a jukebox in its middle, and on the altar a chest holding one record: 13. Put that record in the
 * cathedral's jukebox - by day or by night - or take the Ruby ring out of the crypt under its altar, and its dead
 * knights rise:
 * <ul>
 *   <li>lightning strikes both spires, the bells toll, and a "Cursed Crusade" bar shows its five minutes running
 *       out;</li>
 *   <li>Templars rise in three waves of ten (up to 30) - out of the graves round the cathedral and in its nave - and
 *       all of them rush the player who played the record;</li>
 *   <li>every Templar killed leaves a knight's spoils (twelve iron ingots, four diamonds, a suit of iron armour or a
 *       netherite ingot);</li>
 *   <li>when the five minutes are up - or the last knight of the last wave falls - the Crusade is over, and any
 *       knights still standing crumble to dust.</li>
 * </ul>
 * On peaceful nothing happens but a puff of smoke. Each
 * cathedral's knights rise only once. If the player dies or runs far away, no more rise. Crusades are saved with the
 * world.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CursedCrusades extends SavedData {
    private static final String NAME = KoppenClimateMod.MOD_ID + "_cursed_crusades";
    private static final ResourceKey<Structure> CATHEDRAL = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "gothic_cathedral"));
    public static final String KNIGHT_TAG = KoppenClimateMod.MOD_ID + ".cursed_crusade";
    private static final int WAVES = 3, PER_WAVE = 10, WAVE_GAP = 600;
    // five minutes
    private static final long DURATION = 6000;
    private static final double REACH = 128.0;

    private static final class Crusade {
        final BlockPos origin;
        final int turn;
        final UUID player;
        final long start, end;
        int waves;
        boolean playerGone;
        @Nullable
        ServerBossEvent bar;

        Crusade(BlockPos origin, int turn, UUID player, long start, long end) {
            this.origin = origin;
            this.turn = turn;
            this.player = player;
            this.start = start;
            this.end = end;
        }

        BlockPos at(int lx, int y, int lz) {
            return this.origin.offset(rx(this.turn, lx, lz), y, rz(this.turn, lx, lz));
        }
    }

    // the cathedral's frame, turned (as CathedralPiece's)
    private static int rx(int turn, int lx, int lz) {
        return switch (turn) {
            case 1 -> -lz;
            case 2 -> -lx;
            case 3 -> lz;
            default -> lx;
        };
    }

    private static int rz(int turn, int lx, int lz) {
        return switch (turn) {
            case 1 -> lx;
            case 2 -> -lz;
            case 3 -> -lx;
            default -> lz;
        };
    }

    private final Set<Long> used = new HashSet<>();
    private final List<Crusade> crusades = new ArrayList<>();

    private CursedCrusades() {
    }

    private static CursedCrusades get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(CursedCrusades::new, CursedCrusades::load, null), NAME);
    }

    // ---------------------------------------------------------------- starting one

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ServerPlayer player)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.JUKEBOX) || state.getValue(JukeboxBlock.HAS_RECORD)) return;
        ItemStack held = player.getItemInHand(event.getHand());
        if (!held.is(Items.MUSIC_DISC_13)) return;
        CathedralPiece cathedral = cathedralAt(level, pos);
        if (cathedral == null) return;
        start(level, player, cathedral, pos);
    }

    // ---------------------------------------------------------------- or by taking the Ruby ring from its crypt

    // out of the crypt's chest (when the chest is closed with the ring gone from it, into the player's inventory)
    @SubscribeEvent
    public static void onCloseChest(net.minecraftforge.event.entity.player.PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (!(event.getContainer() instanceof net.minecraft.world.inventory.ChestMenu menu)) return;
        if (!(menu.getContainer() instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) return;
        BlockPos pos = chest.getBlockPos();
        CathedralPiece cathedral = cathedralAt(level, pos);
        if (cathedral == null || !pos.equals(cathedral.world(CathedralPiece.ALTAR_X, CathedralPiece.CRYPT_Y, 0))) return;
        if (holdsRing(chest) || !holdsRing(player.getInventory())) return;
        ring(level, player, cathedral, pos);
    }

    // or picked up off the floor (the chest broken open)
    @SubscribeEvent
    public static void onPickUp(net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        if (!(event.getStack().getItem() instanceof net.krisplis.koppenclimate.item.custom.RubyRingItem)) return;
        CathedralPiece cathedral = cathedralAt(level, player.blockPosition());
        if (cathedral == null) return;
        ring(level, player, cathedral, player.blockPosition());
    }

    private static boolean holdsRing(net.minecraft.world.Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).getItem() instanceof net.krisplis.koppenclimate.item.custom.RubyRingItem) return true;
        }
        return false;
    }

    private static void ring(ServerLevel level, ServerPlayer player, CathedralPiece cathedral, BlockPos pos) {
        if (get(level).used.contains(cathedral.origin().asLong())) return;
        player.displayClientMessage(Component.translatable("event.koppenclimatemod.cursed_crusade.ring")
                .withStyle(net.minecraft.ChatFormatting.DARK_RED), true);
        start(level, player, cathedral, pos);
    }

    /** The Crusade begins (once for each cathedral), against this player. */
    private static void start(ServerLevel level, ServerPlayer player, CathedralPiece cathedral, BlockPos pos) {
        CursedCrusades data = get(level);
        long key = cathedral.origin().asLong();
        if (data.used.contains(key)) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.01);
            return;
        }
        long duration = DURATION;
        int turn = turnOf(cathedral);
        Crusade crusade = new Crusade(cathedral.origin(), turn, player.getUUID(), level.getGameTime(), level.getGameTime() + duration);
        data.used.add(key);
        data.crusades.add(crusade);
        data.setDirty();
        // the start: lightning on both spires, the bells tolling
        for (int side = -1; side <= 1; side += 2) {
            BlockPos spire = crusade.at(4, 66, side * 9);
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(spire.getX() + 0.5, spire.getY(), spire.getZ() + 0.5);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
            level.playSound(null, crusade.at(4, 37, side * 9), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 4.0F, 0.5F);
        }
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    // which way the cathedral was turned: worked out from where its own frame puts a point east of its front
    private static int turnOf(CathedralPiece cathedral) {
        BlockPos o = cathedral.origin(), east = cathedral.world(1, 0, 0);
        int dx = east.getX() - o.getX(), dz = east.getZ() - o.getZ();
        return dx == 1 ? 0 : dz == 1 ? 1 : dx == -1 ? 2 : 3;
    }

    /** The cathedral this block lies in, if any. */
    @Nullable
    private static CathedralPiece cathedralAt(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, h -> h.is(CATHEDRAL));
        if (!start.isValid()) return null;
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof CathedralPiece cathedral && cathedral.inCathedral(pos)) return cathedral;
        }
        return null;
    }

    // ---------------------------------------------------------------- running them

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 20 != 0) return;
        CursedCrusades data = get(level);
        if (data.crusades.isEmpty()) return;
        data.crusades.removeIf(crusade -> data.tick(level, crusade));
        data.setDirty();
    }

    /** One second of a crusade; true when it's over. */
    private boolean tick(ServerLevel level, Crusade crusade) {
        long now = level.getGameTime();
        long elapsed = now - crusade.start;
        BlockPos middle = crusade.at(26, 1, 0);
        if (!level.isLoaded(middle)) return false;   // nobody near: it waits
        boolean over = now >= crusade.end;
        // all three waves out, and every knight of them fallen: won
        if (!over && crusade.waves >= WAVES && elapsed > 100 && level.getEntitiesOfClass(TemplarEntity.class,
                new net.minecraft.world.phys.AABB(middle).inflate(REACH), k -> k.getTags().contains(KNIGHT_TAG)).isEmpty()) {
            over = true;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(crusade.player);
        if (player == null || !player.isAlive() || player.level() != level
                || player.distanceToSqr(middle.getX(), middle.getY(), middle.getZ()) > REACH * REACH) {
            crusade.playerGone = true;
        }
        if (crusade.bar == null && !crusade.playerGone && !over) {
            crusade.bar = new ServerBossEvent(Component.translatable("event.koppenclimatemod.cursed_crusade"),
                    BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
            crusade.bar.setDarkenScreen(true);
            crusade.bar.setCreateWorldFog(true);
        }
        if (crusade.bar != null) {
            if (crusade.playerGone || over) {
                crusade.bar.removeAllPlayers();
                crusade.bar = null;
            } else {
                if (!crusade.bar.getPlayers().contains(player)) crusade.bar.addPlayer(player);
                crusade.bar.setProgress(1.0F - Math.min(1.0F, (float) elapsed / (crusade.end - crusade.start)));
            }
        }
        if (!crusade.playerGone && !over) {
            while (crusade.waves < WAVES && elapsed >= (long) crusade.waves * WAVE_GAP) {
                this.wave(level, crusade, player);
                crusade.waves++;
            }
            for (TemplarEntity knight : level.getEntitiesOfClass(TemplarEntity.class, player.getBoundingBox().inflate(72.0),
                    k -> k.getTags().contains(KNIGHT_TAG))) {
                if (knight.getTarget() != player) knight.setTarget(player);
            }
        }
        if (over) {
            // the end: the knights still standing crumble to dust
            for (TemplarEntity knight : level.getEntitiesOfClass(TemplarEntity.class,
                    new net.minecraft.world.phys.AABB(middle).inflate(REACH), k -> k.getTags().contains(KNIGHT_TAG))) {
                level.sendParticles(ParticleTypes.SOUL, knight.getX(), knight.getY(0.5), knight.getZ(), 12, 0.3, 0.6, 0.3, 0.02);
                level.sendParticles(ParticleTypes.ASH, knight.getX(), knight.getY(0.5), knight.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
                knight.discard();
            }
        }
        return over;
    }

    /** A wave: half out of the graves in the close, half in the nave. */
    private void wave(ServerLevel level, Crusade crusade, ServerPlayer player) {
        var random = level.random;
        for (int i = 0; i < PER_WAVE; i++) {
            for (int attempt = 0; attempt < 10; attempt++) {
                BlockPos pos;
                if (i % 2 == 0) {
                    // in the nave or its aisles
                    pos = crusade.at(4 + random.nextInt(40), 1, random.nextInt(17) - 8);
                } else {
                    // out of the graveyard either side
                    BlockPos p = crusade.at(10 + random.nextInt(48), 0, (random.nextBoolean() ? 1 : -1) * (14 + random.nextInt(10)));
                    pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
                }
                if (!level.isLoaded(pos) || !level.noCollision(ModEntities.TEMPLAR.get().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) continue;
                if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) continue;
                TemplarEntity knight = ModEntities.TEMPLAR.get().create(level);
                if (knight == null) return;
                knight.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
                knight.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                knight.addTag(KNIGHT_TAG);
                knight.setPersistenceRequired();
                knight.setTarget(player);
                level.addFreshEntity(knight);
                level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.3, 0.8, 0.3, 0.02);
                break;
            }
        }
        level.playSound(null, crusade.at(26, 10, 0), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 4.0F, 0.5F);
        level.playSound(null, crusade.at(26, 10, 0), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 6.0F, 0.7F);
    }

    // ---------------------------------------------------------------- saving

    private static CursedCrusades load(CompoundTag tag, HolderLookup.Provider registries) {
        CursedCrusades data = new CursedCrusades();
        for (long l : tag.getLongArray("Used")) data.used.add(l);
        ListTag list = tag.getList("Crusades", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag c = list.getCompound(i);
            Crusade crusade = new Crusade(BlockPos.of(c.getLong("Origin")), c.getInt("Turn"), c.getUUID("Player"), c.getLong("Start"), c.getLong("End"));
            crusade.waves = c.getInt("Waves");
            crusade.playerGone = c.getBoolean("PlayerGone");
            data.crusades.add(crusade);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("Used", this.used.stream().mapToLong(Long::longValue).toArray());
        ListTag list = new ListTag();
        for (Crusade crusade : this.crusades) {
            CompoundTag c = new CompoundTag();
            c.putLong("Origin", crusade.origin.asLong());
            c.putInt("Turn", crusade.turn);
            c.putUUID("Player", crusade.player);
            c.putLong("Start", crusade.start);
            c.putLong("End", crusade.end);
            c.putInt("Waves", crusade.waves);
            c.putBoolean("PlayerGone", crusade.playerGone);
            list.add(c);
        }
        tag.put("Crusades", list);
        return tag;
    }
}
