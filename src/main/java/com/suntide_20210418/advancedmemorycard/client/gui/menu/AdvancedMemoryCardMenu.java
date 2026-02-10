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
 * blockpos传递不使用blockpos.aslong(), 因为@GuiSycn无法传递null数据
 *
 */
public class AdvancedMemoryCardMenu extends AEBaseMenu {

    //action标识符
    private static final String clearPos = "clear_pos";
    private static final String updateItemInf = "update_item_inf";
    private static final String reviseStartPos = "revise_start_pos";
    private static final String reviseEndPos = "revise_end_pos";


    public ItemStack itemStack;
    private InteractionHand hand;

    @GuiSync(3)
    private Component mode;
    @GuiSync(4)
    private String startPos = "";
    @GuiSync(5)
    private String endPos = "";

    public AdvancedMemoryCardMenu( int id, Inventory playerInventory, FriendlyByteBuf host) {
        super(ModMenu.ADVANCED_MEMORY_CARD_MENU.get(), id, playerInventory, host);

        //action注册，注意只能在这个构造函数注册
        registerClientAction(clearPos , this::sendClearPos);
        registerClientAction(updateItemInf , this::sendUpdateItemInf);
        registerClientAction(reviseStartPos ,BlockPos.class, this::sendReviseStartPos);
        registerClientAction(reviseEndPos ,BlockPos.class, this::sendReviseEndPos);
    }


    public AdvancedMemoryCardMenu( int id, Inventory playerInventory, InteractionHand hand) {
        this(id, playerInventory, (FriendlyByteBuf) null);

        this.itemStack = this.getPlayer().getItemInHand(hand);

        mode = CardMode.of(itemStack).getName();

        this.hand = hand;

        updateItemInf();
    }


    public void sendReviseEndPos(BlockPos pos) {
        if (this.isClientSide()) {
            sendClientAction(reviseEndPos , pos);
        } else {
            if (!pos.equals(new BlockPos(0, 0, 0))){
                ItemStack itemStack1 = this.itemStack;
                CardMode cardMode = CardMode.of(itemStack1);
                if (cardMode instanceof CopyMode copyMode){
                    copyMode.setEndPos(itemStack1,pos);
                }
            }
        }
    }

    public void sendReviseStartPos(BlockPos pos){
        if (this.isClientSide()) {
            sendClientAction(reviseStartPos , pos);
        } else {
            ItemStack itemStack1 = this.itemStack;
            CardMode cardMode = CardMode.of(itemStack1);
            if (cardMode instanceof CopyMode copyMode){
                copyMode.setStartPos(itemStack1,pos);
            }
        }
    }

    public void sendClearPos() {
        if (this.isClientSide()) {
            sendClientAction(clearPos);
        } else {
            if (itemStack.getItem() instanceof AdvancedMemoryCardItem advancedMemoryCardItem){
                advancedMemoryCardItem.clearCard(this.getPlayer() , this.getPlayer().level());
            }
        }
    }

    public void sendUpdateItemInf() {
        if (this.isClientSide()) {
            sendClientAction(updateItemInf);
        }else {
            updateItemInf();
        }
    }

    public void updateItemInf(){
        Player player = this.getPlayer();

        this.itemStack = player.getItemInHand(hand);

        CardMode cardMode = CopyMode.of(itemStack);
        if (cardMode instanceof CopyMode copyMode) {
            BlockPos startPos1 = copyMode.getStartPos();
            BlockPos endPos1 = copyMode.getEndPos();

            // 空值检查并赋默认值
            this.startPos = startPos1 == null ? "" : startPos1.toString();
            this.endPos = endPos1 == null ? "" :endPos1.toString();

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
