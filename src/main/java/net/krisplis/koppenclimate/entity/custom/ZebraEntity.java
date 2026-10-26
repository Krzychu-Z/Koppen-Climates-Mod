package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** The zebra: a striped donkey of the savanna, quicker on its feet. Tame and saddle it like a donkey. */
public class ZebraEntity extends Donkey {
    public ZebraEntity(EntityType<? extends Donkey> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractChestedHorse.createBaseChestedHorseAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.JUMP_STRENGTH, 0.6);
    }

    @Override
    protected void randomizeAttributes(RandomSource random) {
        super.randomizeAttributes(random);
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25 + random.nextDouble() * 0.06);
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.55 + random.nextDouble() * 0.15);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        if (other instanceof ZebraEntity) {
            ZebraEntity foal = ModEntities.ZEBRA.get().create(level);
            if (foal != null) this.setOffspringAttributes(other, foal);
            return foal;
        }
        return super.getBreedOffspring(level, other);
    }
}
