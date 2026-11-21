package net.krisplis.koppenclimate.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A gold ring set with a sapphire - not to be crafted, nor bought: the one there is lies sealed in the vault at the
 * end of the Highland Castle's dungeon, behind a wall of cracked stone (see CastlePiece). It is worn by putting it in
 * the off-hand (left hand) slot: while it is there no fall can hurt you, and you fly as in creative mode - double
 * jump to take off, jump and sneak to rise and sink (see event/SapphireRing).
 */
public class SapphireRingItem extends Item {
    public SapphireRingItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.koppenclimatemod.sapphire_ring.how").withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.koppenclimatemod.sapphire_ring.effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.koppenclimatemod.sapphire_ring.off").withStyle(ChatFormatting.DARK_RED));
    }
}
