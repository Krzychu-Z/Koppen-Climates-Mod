package net.krisplis.koppenclimate.entity.custom;

import net.krisplis.koppenclimate.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** The warm pig of the tropical forests: a vanilla pig in a reddish-brown, mud-spotted coat. Breeds true. */
public class WarmPigEntity extends Pig {
    public WarmPigEntity(EntityType<? extends Pig> type, Level level) {
        super(type, level);
    }

    @Nullable
    @Override
    public Pig getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.WARM_PIG.get().create(level);
    }
}
