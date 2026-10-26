package net.krisplis.koppenclimate.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A gold ring set with jade. It is worn by putting it in the off-hand (left hand) slot: while it is there you
 * can't be poisoned and you carry two golden hearts that come back a minute after they're used up (see
 * event/JadeRing). The ring itself isn't drawn in the hand (see its item model).
 */
public class JadeRingItem extends Item {
    public JadeRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.koppenclimatemod.jade_ring.how").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.koppenclimatemod.jade_ring.effect").withStyle(ChatFormatting.GRAY));
    }
}
