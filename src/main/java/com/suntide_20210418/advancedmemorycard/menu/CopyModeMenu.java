package com.suntide_20210418.advancedmemorycard.menu;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOption;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyOptions;
import com.suntide_20210418.advancedmemorycard.network.CopyModePacket;
import com.suntide_20210418.advancedmemorycard.network.CopyOptionsSyncPacket;
import com.suntide_20210418.advancedmemorycard.network.NetworkHandler;
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
    private static final String SET_OPTIONS = "set_options:";

    private final InventoryPlayer playerInventory;

    private String startPos = "";
    private String endPos = "";

    /** 服务端持有的权威选项，客户端显示以同步包为准（物品 NBT 不会随空容器自动同步）。 */
    private int applyMask;
    private boolean autoOpen;

    public CopyModeMenu(InventoryPlayer playerInventory) {
        this.playerInventory = playerInventory;
        readOptionsFromCard();
        updateItemInf();
        syncOptionsToClient();
    }

    private void readOptionsFromCard() {
        CopyOptions opts = CopyOptions.of(playerInventory.player.getHeldItem());
        this.applyMask = opts.mask();
        this.autoOpen = opts.isAutoOpen();
    }

    /**
     * 只由服务端的菜单实例发包：客户端的同名对象只是容器镜像，它没有权威状态
     * （{@code getClientGuiElement} 会 new 一个不带 NBT 上下文的菜单）。
     */
    private void syncOptionsToClient() {
        if (playerInventory.player instanceof EntityPlayerMP) {
            NetworkHandler.sendToPlayer(
                new CopyOptionsSyncPacket(applyMask, autoOpen),
                (EntityPlayerMP) playerInventory.player);
        }
    }

    public int getApplyMask() {
        return applyMask;
    }

    public boolean isAutoOpen() {
        return autoOpen;
    }

    @Override
    public void handleCopyAction(String action, BlockPos pos) {
        if (action != null && action.startsWith(SET_OPTIONS)) {
            applyOptions(action.substring(SET_OPTIONS.length()));
            return;
        }
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

    /**
     * 载荷形如 {@code <mask>:<autoOpen>}，来自 {@code CopyModeScreen} 的点击。
     * 选项写在卡片根标签上，所以这里直接改手持的物品，不经过菜单字段。
     */
    private void applyOptions(String payload) {
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            return;
        }
        int mask;
        try {
            mask = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return;
        }
        boolean nextAutoOpen = "1".equals(parts[1]);

        ItemStack stack = playerInventory.player.getHeldItem();
        if (stack != null && stack.getItem() instanceof AdvancedMemoryCardItem) {
            this.applyMask = mask & CopyOption.allBits();
            this.autoOpen = nextAutoOpen;
            CopyOptions.of(stack)
                .withMask(this.applyMask)
                .withAutoOpen(this.autoOpen)
                .save(stack);
            // 空容器不会替我们把 NBT 变化推给客户端，必须显式同步，否则界面要等重开才刷新
            syncOptionsToClient();
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
            ((AdvancedMemoryCardItem) stack.getItem())
                .clearCard(playerInventory.player, playerInventory.player.worldObj);
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
