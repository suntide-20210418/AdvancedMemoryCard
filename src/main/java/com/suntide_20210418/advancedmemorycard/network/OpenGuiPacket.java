package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.client.gui.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.utils.ResourceLocationHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenGuiPacket(InteractionHand hand) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenGuiPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocationHelper.modLoc("open_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenGuiPacket> STREAM_CODEC =
            StreamCodec.of(OpenGuiPacket::encode, OpenGuiPacket::decode);

    private static void encode(FriendlyByteBuf buffer, OpenGuiPacket msg) {
        buffer.writeInt(msg.hand.ordinal());
    }

    private static OpenGuiPacket decode(FriendlyByteBuf buffer) {
        int handOrdinal = buffer.readInt();
        InteractionHand hand = InteractionHand.values()[handOrdinal];
        return new OpenGuiPacket(hand);
    }

    public static void handleServer(OpenGuiPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player != null) {
                player.openMenu(new SimpleMenuProvider(
                        (wnd, inv, pl) -> new CopyModeMenu(wnd, inv, msg.hand),
                        Component.empty()
                ));
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
