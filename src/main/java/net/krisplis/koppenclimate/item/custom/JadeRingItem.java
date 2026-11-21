package net.krisplis.koppenclimate.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A gold ring set with jade - not to be crafted: a Thai village's master priest sells one, and now and then one
 * turns up in a Thai house's chest. It is worn by putting it in the off-hand (left hand) slot: while it is there
 * you are as fast as the strongest potion of Swiftness makes you, and no poison of any kind takes hold (see event/JadeRing). The ring itself isn't drawn in the
 * hand (see its item model).
 */
public class JadeRingItem extends Item {
    public JadeRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.koppenclimatemod.jade_ring.how").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("item.koppenclimatemod.jade_ring.effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.koppenclimatemod.jade_ring.off").withStyle(ChatFormatting.DARK_RED));
    }
}
