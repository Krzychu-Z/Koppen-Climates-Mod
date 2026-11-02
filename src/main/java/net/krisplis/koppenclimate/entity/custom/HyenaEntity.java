package net.krisplis.koppenclimate.entity.custom;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

/**
 * The spotted hyena of the Australian desert: the jackal's ways (a pack that shies away from players by day and hunts
 * them, villagers and animals by night, and won't be tamed), in a bigger, heavier, spotted body with a stronger bite,
 * and a voice like mad giggling.
 */
public class HyenaEntity extends JackalEntity {
    public HyenaEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return JackalEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    // its "laugh": short, high yelps in quick succession
    @Override
    protected SoundEvent getAmbientSound() {
        if (this.getTarget() != null) return SoundEvents.WOLF_GROWL;
        return this.random.nextInt(3) == 0 ? SoundEvents.WOLF_WHINE : SoundEvents.WOLF_AMBIENT;
    }

    @Override
    public float getVoicePitch() {
        return 1.6F + this.random.nextFloat() * 0.35F;
    }
}
