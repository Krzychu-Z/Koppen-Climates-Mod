package net.krisplis.koppenclimate.item.custom;

import net.krisplis.koppenclimate.effect.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Cedar perfume: sprayed on (used), it leaves the player smelling of cedar for three minutes (the Cedar Scent
 * effect): friendly animals and villagers come to them, and hostile mobs keep away and give up on them (CedarScent).
 * The bottle is left over.
 */
public class CedarPerfumeItem extends Item {
    public static final int DURATION = 20 * 180;

    public CedarPerfumeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(ModEffects.CEDAR_SCENT.getHolder().get(), DURATION, 0));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 1.0F, 1.3F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) player.drop(bottle, false);
            }
        } else {
            for (int i = 0; i < 12; i++) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, player.getRandomX(0.8), player.getY() + 0.5 + level.random.nextDouble() * 1.4,
                        player.getRandomZ(0.8), 0.0, 0.02, 0.0);
            }
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
