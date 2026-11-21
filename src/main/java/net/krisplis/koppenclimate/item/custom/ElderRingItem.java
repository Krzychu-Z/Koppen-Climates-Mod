package net.krisplis.koppenclimate.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Elder ring: the jade, amber, ruby, sapphire and graphite rings forged into one (a crafting table, all five
 * together). Worn in the off-hand (left hand) slot it does all that the five do - Speed V and no poison, no fire and
 * breath under water, death turned away (as often as it comes), no falls and flight, every blow paid back in kind -
 * and keeps you at the strongest Regeneration besides (see event/Rings). It never wears out, and nothing destroys it:
 * dropped, it burns in no fire or lava, no blast or cactus touches it, it never despawns, and the void gives it back.
 * But taking it off is agony: the Wither and a slowness that roots you to the spot.
 */
public class ElderRingItem extends Item {
    public ElderRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.koppenclimatemod.elder_ring.how").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.koppenclimatemod.elder_ring.effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.koppenclimatemod.elder_ring.off").withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    // indestructible, lying on the ground: unhurt by anything, never despawning, never lost to the void
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.isInvulnerable()) {
            entity.setInvulnerable(true);
            entity.setUnlimitedLifetime();
        }
        Level level = entity.level();
        if (!level.isClientSide && entity.getY() < level.getMinBuildHeight() + 1) {
            // back up onto the ground above, or (over the bottomless End) left hovering just above the void
            entity.setDeltaMovement(0.0, 0.0, 0.0);
            int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, entity.getBlockX(), entity.getBlockZ());
            boolean ground = surface > level.getMinBuildHeight();
            entity.setPos(entity.getX(), ground ? surface + 0.5 : level.getMinBuildHeight() + 2, entity.getZ());
            entity.setNoGravity(!ground);
        }
        return false;
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }
}
