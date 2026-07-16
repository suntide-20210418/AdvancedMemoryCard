package com.suntide_20210418.advancedmemorycard.client.gui.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import com.suntide_20210418.advancedmemorycard.client.gui.ModMenu;
import com.suntide_20210418.advancedmemorycard.item.custom.AdvancedMemoryCardItem;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.CopyMode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
/**
 * 服务端界面menu，继承AEBaseMenu，负责接收客户端数据并进行操作
 * blockpos传递不使用blockpos.aslong(), 因为@GuiSync无法传递null数据
 *
 */
public class CopyModeMenu extends AEBaseMenu {

    //action标识符
    private static final String CLEAR_POS = "clear_pos";
    private static final String UPDATE_ITEM_INF = "update_item_inf";
    private static final String REVISE_START_POS = "revise_start_pos";
    private static final String REVISE_END_POS = "revise_end_pos";


    private ItemStack stack;
    private InteractionHand hand;

    @GuiSync(3)
    private Component mode;
    @GuiSync(4)
    private String startPos;
    @GuiSync(5)
    private String endPos;

    public CopyModeMenu(int id, Inventory playerInventory, FriendlyByteBuf host) {
        super(ModMenu.COPY_MODE_MENU.get(), id, playerInventory, host);

        //action注册，注意只能在这个构造函数注册
        registerClientAction(CLEAR_POS, this::sendClearPos);
        registerClientAction(UPDATE_ITEM_INF, this::sendUpdateItemInf);
        registerClientAction(REVISE_START_POS, BlockPos.class, this::sendReviseStartPos);
        registerClientAction(REVISE_END_POS, BlockPos.class, this::sendReviseEndPos);
    }


    public CopyModeMenu(int id, Inventory playerInventory, InteractionHand hand) {
        this(id, playerInventory, (FriendlyByteBuf) null);
        this.stack = this.getPlayer().getItemInHand(hand);
        this.mode = CardMode.of(stack).getName();
        this.hand = hand;
        updateItemInf();
    }


    public void sendReviseEndPos(BlockPos pos) {
        if (this.isClientSide()) {
            sendClientAction(REVISE_END_POS, pos);
        } else {
            if (!pos.equals(new BlockPos(0, 0, 0))){
                ItemStack stack = this.stack;
                CardMode cardMode = CardMode.of(stack);
                if (cardMode instanceof CopyMode copyMode){
                    copyMode.setEndPos(stack, pos);
                }
            }
        }
    }

    public void sendReviseStartPos(BlockPos pos){
        if (this.isClientSide()) {
            sendClientAction(REVISE_START_POS, pos);
        } else {
            ItemStack stack = this.stack;
            CardMode cardMode = CardMode.of(stack);
            if (cardMode instanceof CopyMode copyMode){
                copyMode.setStartPos(stack, pos);
            }
        }
    }

    public void sendClearPos() {
        if (this.isClientSide()) {
            sendClientAction(CLEAR_POS);
        } else {
            if (stack.getItem() instanceof AdvancedMemoryCardItem advancedMemoryCardItem){
                advancedMemoryCardItem.clearCard(this.getPlayer(), this.getPlayer().level());
            }
        }
    }

    public void sendUpdateItemInf() {
        if (this.isClientSide()) {
            sendClientAction(UPDATE_ITEM_INF);
        } else {
            updateItemInf();
        }
    }

    public void updateItemInf(){
        Player player = this.getPlayer();

        this.stack = player.getItemInHand(hand);

        CardMode cardMode = CopyMode.of(stack);
        if (cardMode instanceof CopyMode copyMode) {
            BlockPos startPos = copyMode.getStartPos();
            BlockPos endPos = copyMode.getEndPos();

            // 空值检查并赋默认值
            this.startPos = startPos == null ? "" : startPos.toString();
            this.endPos = endPos == null ? "" :endPos.toString();

        }
    }


    public String getEndPos() {
        return endPos;
    }

    public String getStartPos() {
        return startPos;
    }

    public Component getMode() {
        return mode;
    }
}
