package net.krisplis.koppenclimate.item.custom;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.item.ModItems;
import net.krisplis.koppenclimate.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * The Kar98k: a long bolt-action rifle with plain iron sights (no telescope), four of them hidden about the French
 * village (FrenchVillagePiece). Built on the shotgun, but:
 * <ul>
 *   <li>a click of the right button raises it to the eye and it stays there - the iron sights right in the middle
 *       of the view, the view narrowed a little (Kar98kClient) - until another right click lowers it (or it's put
 *       away);</li>
 *   <li>the left button fires (ModNetwork): from the hip a shot strays a little, aimed it goes exactly where the
 *       sights are;</li>
 *   <li>its bullet flies as far as the world is simulated round the shooter (the server's simulation distance), and
 *       hits the first creature in its path - shattering glass, pumpkins, pots, lanterns and the like on its way
 *       (see breaks), stopped only by a solid block;</li>
 *   <li>one hit kills a player outright: twelve hearts, through any armour and its enchantments (damage type
 *       koppenclimatemod:rifle) - a little more than a player can take, so tougher creatures take it too;</li>
 *   <li>each shot uses up one bullet (ModItems.BULLET: the same ammunition feeds every gun), and the bolt must be
 *       worked before the next (a second and a quarter).</li>
 * </ul>
 */
public class Kar98kItem extends Item {
    public static final ResourceKey<DamageType> RIFLE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "rifle"));
    /** The soldiers' shots: an ordinary rifle bullet, which armour does stop some of. */
    public static final ResourceKey<DamageType> RIFLE_SHOT = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "rifle_shot"));
    public static final float DAMAGE = 24.0F;
    private static final int BOLT = 25;
    // how far a shot from the hip may stray (degrees)
    private static final float HIP_SPREAD = 2.5F;

    public Kar98kItem(Properties properties) {
        super(properties);
    }

    // the right button: up to the eye, or down again (it stays up between clicks)
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !this.canAim()) return InteractionResultHolder.pass(gun);
        setAimed(gun, !isAimed(gun));
        return InteractionResultHolder.consume(gun);
    }

    // ---- what makes one gun of this family differ from another (the Mosin, the AK-47 override these)

    /** Whether the right button raises it to the eye. */
    public boolean canAim() {
        return true;
    }

    /** Ticks before the next shot (working the bolt, or the action cycling). */
    protected int cycleTicks() {
        return BOLT;
    }

    /** How far its bullet flies. */
    protected double reach(ServerLevel level) {
        return range(level);
    }

    protected float damage() {
        return DAMAGE;
    }

    protected ResourceKey<DamageType> damageType() {
        return RIFLE;
    }

    /** How far a shot strays (degrees). */
    protected float spread(boolean aimed) {
        return aimed ? 0.0F : HIP_SPREAD;
    }

    protected void report(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.SHOTGUN.get(), SoundSource.PLAYERS, 5.0F,
                1.35F + level.random.nextFloat() * 0.1F);
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.PLAYERS,
                0.6F, 0.8F);
    }

    public static boolean isAimed(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && data.contains(AIMED) && data.copyTag().getBoolean(AIMED);
    }

    static void setAimed(ItemStack stack, boolean aimed) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> {
            if (aimed) tag.putBoolean(AIMED, true);
            else tag.remove(AIMED);
        });
    }

    private static final String AIMED = "Aimed";

    // put away (or moved off the hand), it's lowered
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && isAimed(stack) && !(selected && entity instanceof LivingEntity living && living.getMainHandItem() == stack)) {
            setAimed(stack, false);
        }
    }

    // raising and lowering it isn't swapping it for another: no re-equip bob
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    // a rifle is no club: a left click fires it instead (and breaks nothing)
    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state, Level level, net.minecraft.core.BlockPos pos, Player player) {
        return false;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(net.krisplis.koppenclimate.client.Kar98kClient.extensions());
    }

    public static boolean aiming(Player player) {
        ItemStack held = player.getMainHandItem();
        return held.getItem() instanceof Kar98kItem && isAimed(held);
    }

    static ItemStack findAmmo(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.BULLET.get())) return stack;
        }
        return ItemStack.EMPTY;
    }

    /** A left click with the rifle in the main hand (sent by the client): fire, if the bolt is worked and he has a round. */
    public static void pullTrigger(ServerPlayer player) {
        ItemStack gun = player.getMainHandItem();
        if (!(gun.getItem() instanceof Kar98kItem rifle) || player.getCooldowns().isOnCooldown(rifle) || player.isSpectator()) return;
        ServerLevel level = player.serverLevel();
        ItemStack ammo = findAmmo(player);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.8F, 1.6F);
            player.getCooldowns().addCooldown(rifle, 10);
            return;
        }
        double range = rifle.reach(level);
        Vec3 look = player.getViewVector(1.0F);
        float spread = rifle.spread(aiming(player));
        if (spread > 0.0F) look = stray(look, spread, level.random);
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(rifle.damageType()), player);
        shoot(level, player, player.getEyePosition(), look, range, source, rifle.damage());
        rifle.report(level, player);
        Vec3 muzzle = muzzle(player, aiming(player));
        for (ServerPlayer other : level.players()) {
            if (other != player && other.distanceToSqr(muzzle) < 64.0 * 64.0) {
                level.sendParticles(other, ParticleTypes.SMOKE, false, muzzle.x, muzzle.y, muzzle.z, 2, 0.02, 0.02, 0.02, 0.0);
            }
        }
        if (!player.getAbilities().instabuild) {
            ammo.shrink(1);
            gun.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }
        player.getCooldowns().addCooldown(rifle, rifle.cycleTicks());
    }

    /** The direction, strayed at random by up to about the given angle. */
    public static Vec3 stray(Vec3 dir, float degrees, net.minecraft.util.RandomSource random) {
        double s = Math.toRadians(degrees);
        return dir.add(random.triangle(0.0, s), random.triangle(0.0, s), random.triangle(0.0, s)).normalize();
    }

    /** Where the muzzle is: out in front of the eyes, in the middle when aimed, lower and to the right from the hip. */
    public static Vec3 muzzle(LivingEntity shooter, boolean aimed) {
        Vec3 look = shooter.getViewVector(1.0F);
        Vec3 side = new Vec3(-look.z, 0.0, look.x).normalize().scale(aimed ? 0.0 : 0.35);
        return shooter.getEyePosition().add(look.scale(aimed ? 1.6 : 1.5)).add(side).add(0.0, aimed ? -0.08 : -0.3, 0.0);
    }

    /**
     * One bullet: from the eye along the direction, as far as the range, stopped by the first solid block; hitting
     * the first living thing in the way (not the shooter). Returns what it hit, if anything.
     */
    public static Entity shoot(ServerLevel level, LivingEntity shooter, Vec3 start, Vec3 dir, double range, DamageSource source, float damage) {
        Vec3 end = start.add(dir.scale(range));
        boolean griefing = shooter instanceof Player || level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
        // through whatever glass and suchlike is in the way, shattering it, to the first solid block (or a creature)
        for (int pierced = 0; ; pierced++) {
            BlockHitResult block = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
            Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, shooter, start, stop, new AABB(start, stop).inflate(1.0),
                    e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != shooter, 0.3F);
            if (hit != null || block.getType() == HitResult.Type.MISS) return strike(level, hit, stop, dir, source, damage);
            net.minecraft.core.BlockPos pos = block.getBlockPos();
            BlockState state = level.getBlockState(pos);
            if (griefing && pierced < 16 && breaks(state)) {
                level.destroyBlock(pos, true, shooter);
                start = stop.add(dir.scale(0.05));
                continue;
            }
            // a puff of dust where it struck
            level.sendParticles(ParticleTypes.SMOKE, stop.x, stop.y, stop.z, 3, 0.05, 0.05, 0.05, 0.01);
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, state), stop.x, stop.y, stop.z,
                    6, 0.05, 0.05, 0.05, 0.1);
            return null;
        }
    }

    /**
     * What a bullet shatters instead of stopping in: glass of every kind (blocks and panes), pumpkins and melons,
     * flower pots and decorated pots, ice, lanterns, candles, glowstone and sea lanterns.
     */
    public static boolean breaks(BlockState state) {
        return state.is(net.minecraftforge.common.Tags.Blocks.GLASS) || state.is(net.minecraftforge.common.Tags.Blocks.GLASS_PANES)
                || state.is(net.minecraft.world.level.block.Blocks.TINTED_GLASS)
                || state.is(net.minecraft.world.level.block.Blocks.PUMPKIN) || state.is(net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN)
                || state.is(net.minecraft.world.level.block.Blocks.JACK_O_LANTERN) || state.is(net.minecraft.world.level.block.Blocks.MELON)
                || state.is(net.minecraft.tags.BlockTags.FLOWER_POTS) || state.is(net.minecraft.world.level.block.Blocks.DECORATED_POT)
                || state.is(net.minecraft.world.level.block.Blocks.ICE) || state.is(net.minecraft.world.level.block.Blocks.LANTERN)
                || state.is(net.minecraft.world.level.block.Blocks.SOUL_LANTERN) || state.is(net.minecraft.tags.BlockTags.CANDLES)
                || state.is(net.minecraft.tags.BlockTags.CANDLE_CAKES) || state.is(net.minecraft.world.level.block.Blocks.GLOWSTONE)
                || state.is(net.minecraft.world.level.block.Blocks.SEA_LANTERN);
    }

    private static Entity strike(ServerLevel level, @javax.annotation.Nullable EntityHitResult hit, Vec3 end, Vec3 dir, DamageSource source,
                                 float damage) {
        if (hit != null) {
            Entity target = hit.getEntity();
            Vec3 at = hit.getLocation();
            if (target.hurt(source, damage)) {
                Vec3 push = new Vec3(dir.x, 0.0, dir.z).normalize().scale(0.3);
                target.push(push.x, 0.1, push.z);
                target.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 4, 0.15, 0.15, 0.15, 0.1);
            return target;
        }
        return null;
    }

    /** (For Mth's sake: the distance a shot reaches on this server.) */
    public static double range(ServerLevel level) {
        return Mth.clamp(level.getServer().getPlayerList().getSimulationDistance(), 2, 32) * 16.0;
    }
}
