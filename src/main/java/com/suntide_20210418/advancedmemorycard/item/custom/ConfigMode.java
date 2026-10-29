package com.suntide_20210418.advancedmemorycard.item.custom;

import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.ConfigMode.show;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Tooltip.configInfo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.CommonProxy;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import com.suntide_20210418.advancedmemorycard.utils.TranslateHelper;

import appeng.api.parts.IPartHost;
import appeng.api.parts.SelectedPart;
import appeng.parts.p2p.PartP2PTunnel;

public class ConfigMode extends CardMode {

    private P2PManager currentP2PManager;

    @Override
    public String getType() {
        return AdvancedMemoryCardMod.MOD_ID + ":config";
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (player == null) {
            return false;
        }
        TileEntity blockEntity = world.getTileEntity(x, y, z);
        if (!(blockEntity instanceof IPartHost)) {
            return false;
        }

        PartP2PTunnel hitP2P = findP2P((IPartHost) blockEntity, side, hitX, hitY, hitZ);
        if (hitP2P == null) {
            // 点到线缆本体/其它部件时给出反馈，并且吃掉这次点击：
            // 交回 AE2 会让它把高级内存卡当成普通内存卡去粘贴设置。
            player.addChatMessage(new ChatComponentText(TranslateHelper.Chat.noP2PFound()));
            return true;
        }

        currentP2PManager = new P2PManager(hitP2P, player);
        CommonProxy.putPendingManager(player, currentP2PManager);
        AdvancedMemoryCardItem.logGuiOpen("config", player);
        player.openGui(AdvancedMemoryCardMod.INSTANCE, AdvancedMemoryCardMod.CONFIG_GUI_ID, world, 0, 0, 0);
        return true;
    }

    /**
     * 找玩家点到的 P2P 部件。
     *
     * <p>先按点击位置做精确命中（rv3 的 {@code selectPart} 用局部 0..1 坐标）；
     * 命不中时退化为「点击面 → 反向面 → 中心 → 其余各面」，因为 P2P 的碰撞箱比线缆小，
     * 玩家实际点到的经常是同一根总线上的线缆而不是部件本身。
     */
    private static PartP2PTunnel findP2P(IPartHost partHost, int side, float hitX, float hitY, float hitZ) {
        SelectedPart selected = partHost.selectPart(Vec3.createVectorHelper(hitX, hitY, hitZ));
        if (selected != null && selected.part instanceof PartP2PTunnel) {
            return (PartP2PTunnel) selected.part;
        }

        ForgeDirection clicked = ForgeDirection.getOrientation(side);
        PartP2PTunnel tunnel = asP2P(partHost.getPart(clicked));
        if (tunnel != null) {
            return tunnel;
        }
        tunnel = asP2P(partHost.getPart(clicked.getOpposite()));
        if (tunnel != null) {
            return tunnel;
        }
        tunnel = asP2P(partHost.getPart(ForgeDirection.UNKNOWN));
        if (tunnel != null) {
            return tunnel;
        }
        for (ForgeDirection direction : ForgeDirection.values()) {
            tunnel = asP2P(partHost.getPart(direction));
            if (tunnel != null) {
                return tunnel;
            }
        }
        return null;
    }

    private static PartP2PTunnel asP2P(Object part) {
        return part instanceof PartP2PTunnel ? (PartP2PTunnel) part : null;
    }

    @Override
    public ItemStack onItemUse(World world, EntityPlayer player) {
        return player.getHeldItem();
    }

    @Override
    public String getName() {
        return show();
    }

    @Override
    protected String getDescription() {
        return configInfo();
    }

    public P2PManager getCurrentP2PManager() {
        return currentP2PManager;
    }
}
