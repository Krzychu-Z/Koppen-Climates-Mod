package net.krisplis.koppenclimate.item.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Mosin-Nagant: a long bolt-action sniper's rifle with a telescopic sight, hidden in a chest on a rooftop of the
 * abandoned Soviet estate (ChernobylPiece). The Kar98k's sort of rifle - the left button fires, one bullet a shot
 * (ModItems.BULLET) - but harder-hitting still, and further-reaching: as far as the server sends the world to its
 * players (its view distance, or the simulation distance if that's further), so anything the scope can pick out it
 * can hit. The right button puts the eye to the scope (some three and a half times closer), a second click turns
 * the scope up to about eight times, a third lowers it (see client/Kar98kClient, PlayerScopingMixin). Scoped, it
 * shoots exactly where the crosshair is; every shot kicks the muzzle - and the scope - up. The bolt takes a little
 * longer to work than the Kar98k's.
 */
public class MosinItem extends Kar98kItem {
    private static final int BOLT = 30;
    public static final float DAMAGE = 32.0F;
    private static final String ZOOM = "Zoom";

    public MosinItem(Properties properties) {
        super(properties);
    }

    // the right button: the scope, its higher power, and down again
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(gun);
        if (!isAimed(gun)) {
            setAimed(gun, true);
            setZoom(gun, 1);
        } else if (zoom(gun) < 2) {
            setZoom(gun, 2);
        } else {
            setAimed(gun, false);
            setZoom(gun, 0);
        }
        return InteractionResultHolder.consume(gun);
    }

    @Override
    protected int cycleTicks() {
        return BOLT;
    }

    @Override
    protected float damage() {
        return DAMAGE;
    }

    @Override
    protected double reach(ServerLevel level) {
        var players = level.getServer().getPlayerList();
        return Mth.clamp(Math.max(players.getViewDistance(), players.getSimulationDistance()), 2, 32) * 16.0;
    }

    /** The scope's power: 1 (about 3.5 times), 2 (about 8 times). */
    public static int zoom(ItemStack stack) {
        net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        int z = data == null ? 0 : data.copyTag().getInt(ZOOM);
        return Math.max(1, z);
    }

    static void setZoom(ItemStack stack, int zoom) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> {
            if (zoom > 0) tag.putInt(ZOOM, zoom);
            else tag.remove(ZOOM);
        });
    }

    /** Whether this player has his eye to a Mosin's scope. */
    public static boolean scoped(Player player) {
        ItemStack held = player.getMainHandItem();
        return held.getItem() instanceof MosinItem && isAimed(held);
    }
}
