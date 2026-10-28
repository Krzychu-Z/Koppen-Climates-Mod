package net.krisplis.koppenclimate.mixin;

import net.krisplis.koppenclimate.event.CsaVillagePeace;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Bad Omen never sets off a raid in the Mediterranean (Csa) or in one of its towns: there it simply does nothing,
// and the player keeps it, rather than it turning into a Raid Omen as it does on entering a village elsewhere.
@Mixin(targets = "net.minecraft.world.effect.BadOmenMobEffect")
public abstract class BadOmenMixin {
    @Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
    private void koppenclimatemod$noRaidsInCsa(LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof ServerPlayer player && player.level() instanceof ServerLevel level
                && CsaVillagePeace.isCsaTown(level, player.blockPosition())) {
            cir.setReturnValue(true);
        }
    }
}
