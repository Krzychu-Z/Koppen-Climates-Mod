package net.krisplis.koppenclimate.network;

import net.krisplis.koppenclimate.KoppenClimateMod;
import net.krisplis.koppenclimate.item.custom.Kar98kItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/**
 * The mod's network channel. So far it carries one message: a player pulling the trigger of his Kar98k (a left click:
 * the right button is for aiming), which the client can't otherwise tell the server.
 */
public final class ModNetwork {
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(KoppenClimateMod.MOD_ID, "main"))
            .networkProtocolVersion(1)
            .simpleChannel();

    private ModNetwork() {
    }

    /** The trigger pulled (nothing more is needed: the server knows what he holds, where he looks, whether he aims). */
    public record FireRifle() {
        static void encode(FireRifle message, RegistryFriendlyByteBuf buf) {
        }

        static FireRifle decode(RegistryFriendlyByteBuf buf) {
            return new FireRifle();
        }

        static void handle(FireRifle message, CustomPayloadEvent.Context context) {
            ServerPlayer player = context.getSender();
            if (player != null) Kar98kItem.pullTrigger(player);
            context.setPacketHandled(true);
        }
    }

    public static void register() {
        CHANNEL.messageBuilder(FireRifle.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FireRifle::encode)
                .decoder(FireRifle::decode)
                .consumerMainThread(FireRifle::handle)
                .add();
    }

    public static void fireRifle() {
        CHANNEL.send(new FireRifle(), PacketDistributor.SERVER.noArg());
    }
}
