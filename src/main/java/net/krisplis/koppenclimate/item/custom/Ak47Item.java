package net.krisplis.koppenclimate.item.custom;

import net.krisplis.koppenclimate.sound.ModSounds;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;

/**
 * The AK-47, one of them hidden in the abandoned Soviet estate (ChernobylPiece). No sights to speak of - the right
 * button does nothing - but it's ready again almost at once: every click of the left button fires (semi-automatic,
 * a shot every three ticks while there are bullets: ModItems.BULLET). It carries further than the shotgun but not
 * as far as the Mosin - half the Mosin's reach, at least forty blocks and at most ninety-six - and a hit is an
 * ordinary rifle bullet, which armour softens. Fired fast, its shots stray a little.
 */
public class Ak47Item extends Kar98kItem {
    private static final int CYCLE = 3;
    private static final float SHOT = 7.0F;
    private static final float SPREAD = 1.8F;

    public Ak47Item(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canAim() {
        return false;
    }

    @Override
    protected int cycleTicks() {
        return CYCLE;
    }

    @Override
    protected double reach(ServerLevel level) {
        return Mth.clamp(range(level) * 0.5, 40.0, 96.0);
    }

    @Override
    protected float damage() {
        return SHOT;
    }

    @Override
    protected ResourceKey<DamageType> damageType() {
        return RIFLE_SHOT;
    }

    @Override
    protected float spread(boolean aimed) {
        return SPREAD;
    }

    @Override
    protected void report(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.SHOTGUN.get(), SoundSource.PLAYERS, 3.5F,
                1.75F + level.random.nextFloat() * 0.15F);
    }
}
