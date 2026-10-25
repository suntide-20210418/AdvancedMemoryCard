package com.suntide_20210418.advancedmemorycard.network;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.menu.CopyModeMenu;
import com.suntide_20210418.advancedmemorycard.utils.ResourceLocationHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端请求打开复制模式选项界面。
 *
 * <p>校验必须在服务端再做一遍：客户端只负责判断「按键时手里有没有处于复制模式的卡」，
 * 但不能作为打开菜单的依据（伪造包不应该能凭空开出菜单）。
 */
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
        InteractionHand[] hands = InteractionHand.values();
        // 越界数据不应导致数组越界异常
        InteractionHand hand = handOrdinal >= 0 && handOrdinal < hands.length
                ? hands[handOrdinal]
                : InteractionHand.MAIN_HAND;
        return new OpenGuiPacket(hand);
    }

    public static void handleServer(OpenGuiPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }

            ItemStack stack = player.getItemInHand(msg.hand());
            if (!(stack.getItem() instanceof AdvancedMemoryCardItem)) {
                return;
            }
            if (!(CardMode.of(stack) instanceof CopyMode)) {
                return;
            }

            player.openMenu(new SimpleMenuProvider(
                    (wnd, inv, pl) -> new CopyModeMenu(wnd, inv, msg.hand()),
                    stack.getHoverName()
            ));
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
