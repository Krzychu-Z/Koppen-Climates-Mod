package net.krisplis.koppenclimate.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A gold ring set with a ruby - not to be crafted, nor bought: it lies in the crypt sealed right under the altar of
 * the Gothic Cathedral (see CathedralPiece). It is worn by putting it in the off-hand (left hand) slot: while it is
 * there it saves you from dying, as a totem of undying does - and like a totem, it is destroyed doing it (see
 * event/RubyRing). The ring isn't drawn in the hand (see its item model).
 */
public class RubyRingItem extends Item {
    public RubyRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.koppenclimatemod.ruby_ring.how").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.koppenclimatemod.ruby_ring.effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.koppenclimatemod.ruby_ring.off").withStyle(ChatFormatting.DARK_RED));
    }
}
