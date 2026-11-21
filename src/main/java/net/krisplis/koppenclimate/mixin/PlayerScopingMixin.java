package net.krisplis.koppenclimate.mixin;

import net.krisplis.koppenclimate.item.custom.MosinItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// With his eye to a Mosin's scope the player counts as scoping, as through a spyglass - so the game itself closes the
// view to the scope's circle, hides his hands, zooms in and slows the mouse (MosinClient sets the zoom). Client only.
@Mixin(Player.class)
public abstract class PlayerScopingMixin {
    @Inject(method = "isScoping", at = @At("HEAD"), cancellable = true)
    private void koppenclimatemod$mosinScope(CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (player.level().isClientSide && MosinItem.scoped(player)) cir.setReturnValue(true);
    }
}
