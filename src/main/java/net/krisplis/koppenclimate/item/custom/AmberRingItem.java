package net.krisplis.koppenclimate.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A gold ring set with amber - not to be crafted, nor bought: the one there is lies in the hoard hidden under
 * Pompeii's Colosseum (see PompeiiPiece). It is worn by putting it in the off-hand (left hand) slot: while it is
 * there no fire, lava or burning thing can hurt you, and you breathe under water as on land (see event/AmberRing).
 * It doesn't burn itself, and the ring isn't drawn in the hand (see its item model).
 */
public class AmberRingItem extends Item {
    public AmberRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.koppenclimatemod.amber_ring.how").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.koppenclimatemod.amber_ring.effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.koppenclimatemod.amber_ring.off").withStyle(ChatFormatting.DARK_RED));
    }
}
