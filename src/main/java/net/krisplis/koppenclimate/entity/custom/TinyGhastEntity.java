package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.level.Level;

/**
 * A tiny ghast - a quarter of a ghast's size (the scale attribute shrinks its body, hitbox and model alike) - that
 * haunts the Highland Castle of the Scottish highlands in swarms (see CastlePiece, event/CastleGhasts). It floats and
 * spits fireballs like any ghast, but its fireballs don't blow anything up (they still burn and hurt where they hit,
 * and can be batted back), its cries are higher and quieter, and it takes only a couple of hits.
 */
public class TinyGhastEntity extends Ghast {
    public TinyGhastEntity(EntityType<? extends Ghast> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Ghast.createAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.SCALE, 0.25).add(Attributes.FOLLOW_RANGE, 48.0);
    }

    // its fireballs: no blast, so they don't wreck the castle
    @Override
    public int getExplosionPower() {
        return 0;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public float getVoicePitch() {
        return 1.9F + (this.random.nextFloat() - 0.5F) * 0.3F;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }
}
