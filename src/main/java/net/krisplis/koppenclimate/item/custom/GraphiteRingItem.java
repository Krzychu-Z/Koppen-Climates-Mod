package net.krisplis.koppenclimate.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A ring of black graphite set in steel - not to be crafted, nor bought: the one there is lies in a basement of the
 * abandoned Soviet estate (ChernobylPiece), guarded by its mutant dog. It is worn by putting it in the off-hand (left
 * hand) slot: while it is there, whatever hits you is hit back just as hard and knocked back just as far (see
 * event/Rings). The ring isn't drawn in the hand (see its item model).
 */
public class GraphiteRingItem extends Item {
    public GraphiteRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.koppenclimatemod.graphite_ring.how").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.koppenclimatemod.graphite_ring.effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.koppenclimatemod.graphite_ring.off").withStyle(ChatFormatting.DARK_RED));
    }
}
