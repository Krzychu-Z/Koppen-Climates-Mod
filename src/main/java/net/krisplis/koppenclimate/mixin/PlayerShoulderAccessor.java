package net.krisplis.koppenclimate.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// The koala on a player's shoulder (KoalaShoulders): its saved state is updated as it gets hungrier, cleared when it
// starves, and it is set down when the player crouches - all through the player's own (protected) shoulder methods.
@Mixin(Player.class)
public interface PlayerShoulderAccessor {
    @Invoker("setShoulderEntityLeft")
    void koppenclimatemod$setShoulderEntityLeft(CompoundTag tag);

    @Invoker("setShoulderEntityRight")
    void koppenclimatemod$setShoulderEntityRight(CompoundTag tag);

    @Invoker("removeEntitiesOnShoulder")
    void koppenclimatemod$removeEntitiesOnShoulder();
}
