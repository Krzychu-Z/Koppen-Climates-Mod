package net.krisplis.koppenclimate.event;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.entity.ModEntities;
import net.krisplis.koppenclimate.entity.custom.CowboySoulEntity;
import net.krisplis.koppenclimate.village.GhostTownPiece;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The Demon Raid. Every ghost town's church has a pentagram on its floor, a jukebox, and on the altar a chest
 * holding a single record: 13. Put that record in the church's jukebox at night and the dead come for you:
 * <ul>
 *   <li>lightning strikes the bell tower, and a "Demon Raid" bar shows the night running out;</li>
 *   <li>cowboy souls rise all around the church in three waves of twelve (up to 36), and all of them hunt the
 *       player who played the record;</li>
 *   <li>the church catches fire and burns down slowly, from the cross on its tower to its floorboards, until at
 *       dawn nothing is left of it but ash (whatever was in its chest and jukebox lies in the ashes);</li>
 *   <li>at dawn the souls fade away and the raid is over.</li>
 * </ul>
 * By day the record just plays, as it would anywhere. On peaceful nothing happens but a puff of smoke. Each town's
 * church can only burn once. If the player dies or runs far away, the souls stop coming, but the fire burns on.
 * The fire is the church's own: flames that reach beyond it are put out, so the rest of the town never catches.
 * Raids are saved with the world.
 */
@Mod.EventBusSubscriber(modid = KoppenClimateMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DemonRaids extends SavedData {
    private static final String NAME = KoppenClimateMod.MOD_ID + "_demon_raids";
    private static final ResourceKey<Structure> GHOST_TOWN = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "ghost_town"));
    public static final String SOUL_TAG = KoppenClimateMod.MOD_ID + ".demon_raid";
    private static final int WAVES = 3, PER_WAVE = 12, WAVE_GAP = 600;
    // the shortest burn (the record played just before dawn), and the longest (no daylight cycle)
    private static final long MIN_TICKS = 2400, MAX_TICKS = 12000;
    // the player is out of the raid this far from the church
    private static final double REACH = 128.0;

    private static final class Raid {
        final BlockPos centre;
        final UUID player;
        final long start, end;
        int waves;
        int total;
        boolean playerGone;
        @Nullable
        ServerBossEvent bar;

        Raid(BlockPos centre, UUID player, long start, long end) {
            this.centre = centre;
            this.player = player;
            this.start = start;
            this.end = end;
        }

        boolean inChurch(BlockPos p) {
            int x = p.getX() - this.centre.getX(), y = p.getY() - this.centre.getY(), z = p.getZ() - this.centre.getZ();
            return x >= GhostTownPiece.CHURCH_X0 && x <= GhostTownPiece.CHURCH_X1 && z >= GhostTownPiece.CHURCH_Z0
                    && z <= GhostTownPiece.CHURCH_Z1 && y >= 0 && y <= GhostTownPiece.CHURCH_TOP;
        }

        /** The middle of the church. */
        BlockPos church() {
            return this.centre.offset((GhostTownPiece.CHURCH_X0 + GhostTownPiece.CHURCH_X1) / 2, 4, 0);
        }
    }

    private final Set<Long> used = new HashSet<>();
    private final List<Raid> raids = new ArrayList<>();

    private DemonRaids() {
    }

    private static DemonRaids get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(DemonRaids::new, DemonRaids::load, null), NAME);
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
        GhostTownPiece town = churchAt(level, pos);
        if (town == null) return;
        DemonRaids data = get(level);
        long key = town.centre().asLong();
        // by day, or once the church has burned, the record just plays
        if (data.used.contains(key) || !level.isNight()) return;
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.01);
            return;
        }
        long t = Math.floorMod(level.getDayTime(), 24000L);
        long untilDawn = t >= 12000 ? 23500 - t : 1000 - t;
        long duration = Math.max(MIN_TICKS, Math.min(MAX_TICKS, untilDawn));
        Raid raid = new Raid(town.churchOrigin(), player.getUUID(), level.getGameTime(), level.getGameTime() + duration);
        data.used.add(key);
        data.raids.add(raid);
        data.setDirty();
        // the start: thunder on the bell tower, the bell tolling
        BlockPos tower = town.churchOrigin().offset(GhostTownPiece.CHURCH_X0 + 2, GhostTownPiece.CHURCH_TOP - 5, 0);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(tower.getX() + 0.5, tower.getY(), tower.getZ() + 0.5);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, tower, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 3.0F, 0.5F);
        level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.6F);
    }

    /** The ghost town whose church holds this block, if any. */
    @Nullable
    private static GhostTownPiece churchAt(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, h -> h.is(GHOST_TOWN));
        if (!start.isValid()) return null;
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof GhostTownPiece town && town.inChurch(pos)) return town;
        }
        return null;
    }

    // ---------------------------------------------------------------- running them

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 20 != 0) return;
        DemonRaids data = get(level);
        if (data.raids.isEmpty()) return;
        data.raids.removeIf(raid -> data.tick(level, raid));
        data.setDirty();
    }

    /** One second of a raid; true when it's over. */
    private boolean tick(ServerLevel level, Raid raid) {
        long now = level.getGameTime();
        long elapsed = now - raid.start;
        BlockPos church = raid.church();
        if (!level.isLoaded(church)) return false;   // nobody near: it waits
        boolean over = now >= raid.end || (level.isDay() && elapsed > 400);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(raid.player);
        if (player == null || !player.isAlive() || player.level() != level || player.distanceToSqr(church.getX(), church.getY(), church.getZ()) > REACH * REACH) {
            raid.playerGone = true;
        }
        // the bar: the night running out
        if (raid.bar == null && !raid.playerGone) {
            raid.bar = new ServerBossEvent(Component.literal("Demon Raid"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
            raid.bar.setDarkenScreen(true);
            raid.bar.setCreateWorldFog(true);
        }
        if (raid.bar != null) {
            if (raid.playerGone || over) {
                raid.bar.removeAllPlayers();
                raid.bar = null;
            } else {
                if (!raid.bar.getPlayers().contains(player)) raid.bar.addPlayer(player);
                raid.bar.setProgress(1.0F - Math.min(1.0F, (float) elapsed / (raid.end - raid.start)));
            }
        }
        // the souls rise in waves, and hunt the player
        if (!raid.playerGone && !over) {
            while (raid.waves < WAVES && elapsed >= (long) raid.waves * WAVE_GAP) {
                this.wave(level, raid, player);
                raid.waves++;
            }
            for (CowboySoulEntity soul : level.getEntitiesOfClass(CowboySoulEntity.class, player.getBoundingBox().inflate(64.0),
                    s -> s.getTags().contains(SOUL_TAG))) {
                if (soul.getTarget() != player) soul.setTarget(player);
            }
        }
        // the church burns
        this.burn(level, raid, over ? 1.0 : (double) elapsed / (raid.end - raid.start), now);
        return over;
    }

    private void wave(ServerLevel level, Raid raid, ServerPlayer player) {
        BlockPos church = raid.church();
        var random = level.random;
        for (int i = 0; i < PER_WAVE; i++) {
            for (int attempt = 0; attempt < 8; attempt++) {
                double a = random.nextDouble() * Math.PI * 2, d = 10 + random.nextDouble() * 16;
                int x = church.getX() + (int) Math.round(Math.cos(a) * d), z = church.getZ() + (int) Math.round(Math.sin(a) * d);
                if (!level.isLoaded(new BlockPos(x, 0, z))) continue;
                BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                if (!level.noCollision(ModEntities.COWBOY_SOUL.get().getSpawnAABB(x + 0.5, pos.getY(), z + 0.5))) continue;
                CowboySoulEntity soul = ModEntities.COWBOY_SOUL.get().create(level);
                if (soul == null) return;
                soul.moveTo(x + 0.5, pos.getY() + 0.5, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
                soul.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                soul.addTag(SOUL_TAG);
                soul.setPersistenceRequired();
                soul.setTarget(player);
                level.addFreshEntity(soul);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x + 0.5, pos.getY() + 0.5, z + 0.5, 15, 0.3, 0.8, 0.3, 0.02);
                break;
            }
        }
        level.playSound(null, church, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 3.0F, 0.5F);
    }

    // ---------------------------------------------------------------- the fire

    /** What of the church can burn here: everything standing in it, and its wooden floor. */
    private static boolean burnable(BlockState s, int y) {
        if (s.isAir() || s.getBlock() instanceof BaseFireBlock || !s.getFluidState().isEmpty()) return false;
        if (y > 0) return true;
        return s.is(BlockTags.PLANKS) || s.is(BlockTags.LOGS) || s.is(Blocks.RED_NETHER_BRICKS);
    }

    /** Burn the church down to the given fraction (0-1): top first, then on down to the floor. */
    private void burn(ServerLevel level, Raid raid, double progress, long now) {
        BlockPos c = raid.centre;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        List<BlockPos> standing = new ArrayList<>();
        for (int x = GhostTownPiece.CHURCH_X0; x <= GhostTownPiece.CHURCH_X1; x++) {
            for (int z = GhostTownPiece.CHURCH_Z0; z <= GhostTownPiece.CHURCH_Z1; z++) {
                for (int y = 0; y <= GhostTownPiece.CHURCH_TOP; y++) {
                    m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    if (burnable(level.getBlockState(m), y)) standing.add(m.immutable());
                }
            }
        }
        if (raid.total == 0) raid.total = standing.size();
        // burning order: from the top down, a little ragged
        Map<BlockPos, Double> order = new HashMap<>();
        for (BlockPos p : standing) {
            long h = p.asLong() * 0x9E3779B97F4A7C15L ^ raid.start;
            order.put(p, p.getY() + ((h >>> 40) & 0xFF) / 64.0);
        }
        standing.sort((a, b) -> Double.compare(order.get(b), order.get(a)));
        int target = (int) Math.ceil(raid.total * Math.min(1.0, progress));
        int burned = raid.total - standing.size();
        int toBurn = Math.min(standing.size(), Math.max(0, target - burned));
        var random = level.random;
        for (int i = 0; i < toBurn; i++) {
            BlockPos p = standing.get(i);
            boolean floor = p.getY() == c.getY();
            // (no neighbour updates: nothing else in the church pops off as an item; chests and the jukebox still
            // spill what they hold into the ashes)
            level.setBlock(p, floor ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
            if (random.nextInt(4) == 0) level.playSound(null, p, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8F, 0.6F);
        }
        // the flames: on what burns next
        int front = Math.min(standing.size(), toBurn + 48);
        for (int i = toBurn; i < front; i++) {
            BlockPos p = standing.get(i);
            level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5, 3, 0.4, 0.3, 0.4, 0.01);
            if (random.nextInt(6) == 0) {
                level.sendParticles(ParticleTypes.LAVA, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 1, 0.2, 0.1, 0.2, 0.0);
            }
            BlockPos above = p.above();
            if (random.nextInt(5) == 0 && level.isEmptyBlock(above) && raid.inChurch(above)) {
                level.setBlock(above, BaseFireBlock.getState(level, above), Block.UPDATE_ALL);
            }
        }
        if (front > toBurn) {
            BlockPos p = standing.get(toBurn + random.nextInt(front - toBurn));
            level.playSound(null, p, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 2.0F, 0.8F + random.nextFloat() * 0.4F);
        }
        // the church's fire stays in the church: any flame that's spread beyond it goes out (and once it's all
        // burned, the last flames too)
        boolean done = progress >= 1.0;
        for (int x = GhostTownPiece.CHURCH_X0 - 5; x <= GhostTownPiece.CHURCH_X1 + 5; x++) {
            for (int z = GhostTownPiece.CHURCH_Z0 - 5; z <= GhostTownPiece.CHURCH_Z1 + 5; z++) {
                for (int y = -1; y <= GhostTownPiece.CHURCH_TOP + 3; y++) {
                    m.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    if (level.getBlockState(m).getBlock() instanceof BaseFireBlock && (done || !raid.inChurch(m))) {
                        level.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- saving

    private static DemonRaids load(CompoundTag tag, HolderLookup.Provider registries) {
        DemonRaids data = new DemonRaids();
        for (long l : tag.getLongArray("Used")) data.used.add(l);
        ListTag list = tag.getList("Raids", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag r = list.getCompound(i);
            Raid raid = new Raid(BlockPos.of(r.getLong("Centre")), r.getUUID("Player"), r.getLong("Start"), r.getLong("End"));
            raid.waves = r.getInt("Waves");
            raid.total = r.getInt("Total");
            raid.playerGone = r.getBoolean("PlayerGone");
            data.raids.add(raid);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLongArray("Used", this.used.stream().mapToLong(Long::longValue).toArray());
        ListTag list = new ListTag();
        for (Raid raid : this.raids) {
            CompoundTag r = new CompoundTag();
            r.putLong("Centre", raid.centre.asLong());
            r.putUUID("Player", raid.player);
            r.putLong("Start", raid.start);
            r.putLong("End", raid.end);
            r.putInt("Waves", raid.waves);
            r.putInt("Total", raid.total);
            r.putBoolean("PlayerGone", raid.playerGone);
            list.add(r);
        }
        tag.put("Raids", list);
        return tag;
    }
}
