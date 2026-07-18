package com.suntide_20210418.advancedmemorycard.client.gui.menu;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.network.CopyModePacket;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import com.suntide_20210418.advancedmemorycard.utils.BlockPos;

/**
 * 复制模式菜单（服务端容器）。
 * 对应 1.20.1 的 CopyModeMenu（继承 AEBaseMenu + registerClientAction），
 * 在 1.12.2 中改为继承 vanilla Container，通过 CopyModePacket（ICopyMenu 回调）接收客户端操作。
 * 1.7.10 适配：移除 EnumHand，统一使用 player.getHeldItem() 获取主手物品。
 */
public class CopyModeMenu extends Container implements CopyModePacket.ICopyMenu {

    private static final String CLEAR_POS = "clear_pos";
    private static final String UPDATE_ITEM_INF = "update_item_inf";
    private static final String REVISE_START_POS = "revise_start_pos";
    private static final String REVISE_END_POS = "revise_end_pos";

    private final InventoryPlayer playerInventory;

    private String startPos = "";
    private String endPos = "";

    public CopyModeMenu(InventoryPlayer playerInventory) {
        this.playerInventory = playerInventory;
        updateItemInf();
    }

    @Override
    public void handleCopyAction(String action, BlockPos pos) {
        switch (action) {
            case CLEAR_POS:
                sendClearPos();
                break;
            case UPDATE_ITEM_INF:
                updateItemInf();
                break;
            case REVISE_START_POS:
                sendReviseStartPos(pos);
                break;
            case REVISE_END_POS:
                sendReviseEndPos(pos);
                break;
        }
    }

    public void sendReviseEndPos(BlockPos pos) {
        if (pos != null && !pos.equals(new BlockPos(0, 0, 0))) {
            ItemStack stack = playerInventory.player.getHeldItem();
            CardMode cardMode = CardMode.of(stack);
            if (cardMode instanceof CopyMode) {
                ((CopyMode) cardMode).setEndPos(stack, pos);
            }
        }
    }

    public void sendReviseStartPos(BlockPos pos) {
        if (pos != null) {
            ItemStack stack = playerInventory.player.getHeldItem();
            CardMode cardMode = CardMode.of(stack);
            if (cardMode instanceof CopyMode) {
                ((CopyMode) cardMode).setStartPos(stack, pos);
            }
        }
    }

    public void sendClearPos() {
        ItemStack stack = playerInventory.player.getHeldItem();
        if (stack.getItem() instanceof AdvancedMemoryCardItem) {
            ((AdvancedMemoryCardItem) stack.getItem()).clearCard(playerInventory.player, playerInventory.player.worldObj);
        }
    }

    public void sendUpdateItemInf() {
        updateItemInf();
    }

    public void updateItemInf() {
        EntityPlayer player = playerInventory.player;
        ItemStack stack = player.getHeldItem();

        CardMode cardMode = CardMode.of(stack);
        if (cardMode instanceof CopyMode) {
            CopyMode copyMode = (CopyMode) cardMode;
            BlockPos sp = copyMode.getStartPos();
            BlockPos ep = copyMode.getEndPos();
            this.startPos = sp == null ? "" : sp.toString();
            this.endPos = ep == null ? "" : ep.toString();
        }
    }

    public String getEndPos() {
        return endPos;
    }

    public String getStartPos() {
        return startPos;
    }

    // 客户端 action 分发
    public void dispatchClientAction(String action) {
        com.suntide_20210418.advancedmemorycard.network.NetworkHandler.sendToServer(new CopyModePacket(action));
    }

    public void dispatchClientAction(String action, BlockPos pos) {
        com.suntide_20210418.advancedmemorycard.network.NetworkHandler.sendToServer(new CopyModePacket(action, pos));
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }
}
