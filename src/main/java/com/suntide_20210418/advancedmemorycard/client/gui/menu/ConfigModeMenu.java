package com.suntide_20210418.advancedmemorycard.client.gui.menu;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.parts.p2p.P2PTunnelPart;
import com.suntide_20210418.advancedmemorycard.client.gui.ModMenu;
import com.suntide_20210418.advancedmemorycard.item.custom.CardMode;
import com.suntide_20210418.advancedmemorycard.item.custom.ConfigMode;
import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 配置模式菜单
 * 负责 P2P 通道的频段配置、绑定管理
 * 服务端界面 menu，继承 AEBaseMenu，负责接收客户端数据并进行操作
 */
public class ConfigModeMenu extends AEBaseMenu {

    //action 标识符
    private static final String REFRESH_P2P = "refresh_p2p";
    private static final String BIND_FREQUENCY = "bind_frequency";
    private static final String SET_CHANNEL_ALIAS = "set_channel_alias";
    private static final String SET_P2P_ALIAS = "set_p2p_alias";
    private static final String SET_PENDING_BIND = "set_pending_bind";
    private static final String AUTO_CONFIG_IO = "auto_config_io";
    private static final String HIGHLIGHT_P2P = "highlight_p2p";
    private static final String HIGHLIGHT_P2P_TUNNEL = "highlight_p2p_tunnel";
    private static final String TOGGLE_CHANNEL_EXPAND = "toggle_channel_expand";
    private static final String P2P_TYPE_FILTER = "p2p_type_filter";
    private static final String TOGGLE_P2P_TYPE_EXPAND = "toggle_p2p_type_expand";
    private static final String SEARCH = "search";


    private ItemStack stack;
    private InteractionHand hand;
    private P2PManager p2pManager;
    private HashMap<String, Short> p2pFrequencyAndAlias = new HashMap<>();
    private HashMap<P2PTunnelPart<?>, ResourceLocation> p2pDevicesMap = new HashMap<>();
    private ArrayList<P2PInfo> p2pInfoList = new ArrayList<>();

    @GuiSync(3)
    private Component mode;
    @GuiSync(4)
    private String p2pInfo;
    @GuiSync(5)
    private String frequencyInfo;
    @GuiSync(6)
    private String searchQuery;

    public ConfigModeMenu(int id, Inventory playerInventory, FriendlyByteBuf host) {
        super(ModMenu.CONFIG_MODE_MENU.get(), id, playerInventory, host);

        //action 注册，注意只能在这个构造函数注册
        registerClientAction(REFRESH_P2P, this::sendRefreshP2P);
        registerClientAction(BIND_FREQUENCY, Short.class, this::sendBindFrequency);
        registerClientAction(SET_CHANNEL_ALIAS, String.class, this::sendSetChannelAlias);
        registerClientAction(SET_P2P_ALIAS, String.class, this::sendSetP2PAlias);
        registerClientAction(SET_PENDING_BIND, this::sendSetPendingBind);
        registerClientAction(AUTO_CONFIG_IO, this::sendAutoConfigIO);
        registerClientAction(HIGHLIGHT_P2P, P2PTunnelPart.class, this::sendHighlightP2P);
        registerClientAction(HIGHLIGHT_P2P_TUNNEL, this::sendHighlightP2PTunnel);
        registerClientAction(TOGGLE_CHANNEL_EXPAND, this::sendToggleChannelExpand);
        registerClientAction(P2P_TYPE_FILTER, String.class, this::sendP2PTypeFilter);
        registerClientAction(TOGGLE_P2P_TYPE_EXPAND, this::sendToggleP2PTypeExpand);
        registerClientAction(SEARCH, String.class, this::sendSearch);
    }


    public ConfigModeMenu(int id, Inventory playerInventory, InteractionHand hand) {
        this(id, playerInventory, (FriendlyByteBuf) null);
        this.stack = this.getPlayer().getItemInHand(hand);
        this.mode = CardMode.of(stack).getName();
        this.hand = hand;
        updateItemInf();
    }

    private record P2PInfo(
            boolean isActive,
            boolean isOutput,
            boolean isConnected,
            boolean isMEP2P,
            boolean isPendingBind,
            int channel,
            short frequency,
            String name,
            String p2pType,
            Level level,
            BlockPos position,
            Direction direction
            ){
    }

    public void sendRefreshP2P() {
        if (this.isClientSide()) {
            sendClientAction(REFRESH_P2P);
        } else {
            refreshP2P();
        }
    }

    public void sendBindFrequency(short frequency) {
        if (this.isClientSide()) {
            sendClientAction(BIND_FREQUENCY, frequency);
        } else {
            bindFrequency(frequency);
        }
    }

    public void sendSetChannelAlias(String alias) {
        if (this.isClientSide()) {
            sendClientAction(SET_CHANNEL_ALIAS, alias);
        } else {
            setChannelAlias(alias);
        }
    }

    public void sendSetP2PAlias(String alias) {
        if (this.isClientSide()) {
            sendClientAction(SET_P2P_ALIAS, alias);
        } else {
            setP2PAlias(alias);
        }
    }

    public void sendSetPendingBind() {
        if (this.isClientSide()) {
            sendClientAction(SET_PENDING_BIND);
        } else {
            setPendingBind();
        }
    }

    public void sendAutoConfigIO() {
        if (this.isClientSide()) {
            sendClientAction(AUTO_CONFIG_IO);
        } else {
            autoConfigIO();
        }
    }

    public void sendHighlightP2P(P2PTunnelPart<?> p2pPart) {
        if (this.isClientSide()) {
            sendClientAction(HIGHLIGHT_P2P, p2pPart);
        } else {
            highlightP2P(p2pPart);
        }
    }

    public void sendHighlightP2PTunnel() {
        if (this.isClientSide()) {
            sendClientAction(HIGHLIGHT_P2P_TUNNEL);
        } else {
            highlightP2PTunnel();
        }
    }

    public void sendToggleChannelExpand() {
        if (this.isClientSide()) {
            sendClientAction(TOGGLE_CHANNEL_EXPAND);
        } else {
            toggleChannelExpand();
        }
    }

    public void sendP2PTypeFilter(String filter) {
        if (this.isClientSide()) {
            sendClientAction(P2P_TYPE_FILTER, filter);
        } else {
            p2pTypeFilter(filter);
        }
    }

    public void sendToggleP2PTypeExpand() {
        if (this.isClientSide()) {
            sendClientAction(TOGGLE_P2P_TYPE_EXPAND);
        } else {
            toggleP2PTypeExpand();
        }
    }

    public void sendSearch(String query) {
        if (this.isClientSide()) {
            sendClientAction(SEARCH, query);
        } else {
            search(query);
        }
    }

    public void updateItemInf(){
        Player player = this.getPlayer();

        this.stack = player.getItemInHand(hand);

        CardMode cardMode = ConfigMode.of(stack);
        if (cardMode instanceof ConfigMode configMode) {
            // TODO: 更新 P2P 信息和频段信息
        }
    }

    // ==================== 以下方法需要具体实现 ====================

    private void refreshP2P() {
        // TODO: 刷新 P2P 设备列表
    }

    private void bindFrequency(Short frequency) {
        // TODO: 绑定频段
    }

    private void setChannelAlias(String alias) {
        // TODO: 设置频道别名
    }

    private void setP2PAlias(String alias) {
        // TODO: 设置 P2P 别名
    }

    private void setPendingBind() {
        // TODO: 设置待绑定状态
    }

    private void autoConfigIO() {
        // TODO: 自动配置输入输出
    }

    private void highlightP2P(P2PTunnelPart<?> p2pPart) {
        // TODO: 高亮 P2P 设备
    }

    private void highlightP2PTunnel() {
        // TODO: 高亮 P2P 通道
    }

    private void toggleChannelExpand() {
        // TODO: 切换频道展开状态
    }

    private void p2pTypeFilter(String filter) {
        // TODO: P2P 类型过滤
    }

    private void toggleP2PTypeExpand() {
        // TODO: 切换 P2P 类型展开状态
    }

    private void search(String query) {
        // TODO: 搜索功能
    }


    public String getP2pInfo() {
        return p2pInfo;
    }

    public String getFrequencyInfo() {
        return frequencyInfo;
    }

    public Component getMode() {
        return mode;
    }

    public String getSearchQuery() {
        return searchQuery;
    }
}