package com.suntide_20210418.advancedmemorycard.p2p;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPartItem;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.service.P2PService;
import appeng.parts.p2p.MEP2PTunnelPart;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.CustomNameUtil;
import appeng.util.SettingsFrom;
import com.suntide_20210418.advancedmemorycard.mixin.P2PTunnelPartMixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.*;

public class P2PManager {
    private P2PTunnelPart<?> p2pTunnelPartBinding;
    private IGrid grid;
    private final Player player;
    private final Map<P2PTunnelPart<?>, ResourceLocation> p2pDevicesMap;
    private final List<P2PTunnelPart<?>> p2pDevices;
    private final List<ResourceLocation> p2pTypes;

    public P2PManager(P2PTunnelPart<?> p2pTunnelPartBinding, Player player) {
        this.p2pTunnelPartBinding = p2pTunnelPartBinding;
        this.p2pDevicesMap = new HashMap<>();
        this.player = player;
        analysisP2P();
        this.p2pDevices = new ArrayList<>(p2pDevicesMap.keySet());
        this.p2pTypes = new ArrayList<>(p2pDevicesMap.values());
        autoConfigP2PInputAndOutput();
    }

    public void analysisP2P() {
        // 直接通过 P2PTunnelPart 获取其所在的 GridNode
        IGridNode gridNode = p2pTunnelPartBinding.getGridNode();
        if (gridNode == null) {
            return;
        }

        // 通过 GridNode 获取整个网络
        grid = gridNode.getGrid();
        if (grid == null) {
            return;
        }

        // 遍历网络中的所有节点，找出所有 P2P 设备
        for (IGridNode node : grid.getNodes()) {
            Object machine = node.getOwner();
            if (machine instanceof P2PTunnelPart<?> p2pPart) {
                p2pDevicesMap.put(p2pPart, getP2PType(p2pPart));
            }
        }
    }

    public void bind(short frequency){
        if (grid != null) {
            P2PService p2pService = P2PService.get(grid);
            p2pService.updateFreq(p2pTunnelPartBinding, frequency);
        }
    }

    public void autoConfigP2PInputAndOutput() {
        if (grid == null || grid.isEmpty() || p2pDevices == null) {
            return;
        }
        P2PService p2pService = P2PService.get(grid);
        if (p2pService == null) return;

        for (P2PTunnelPart<?> p2pPart : p2pDevices) {
            if (p2pPart == null) continue;
            processSingleP2PPart(p2pPart, p2pService);
        }
    }

    private void processSingleP2PPart(P2PTunnelPart<?> p2pPart, P2PService p2pService) {
        if (!(p2pPart instanceof MEP2PTunnelPart meP2PTunnelPart)) {
            return;
        }

        IGridNode externalNode = meP2PTunnelPart.getExternalFacingNode();
        if (externalNode == null) {
            // 节点已被破坏，默认设为输出
            ((P2PTunnelPartMixin) p2pPart).invokeSetOutput(true);
            return;
        }

        IGrid externalGrid = externalNode.getGrid();
        if (externalGrid == null || externalGrid.isEmpty()) {
            ((P2PTunnelPartMixin) p2pPart).invokeSetOutput(true);
            return;
        }

        boolean hasController = checkForController(externalGrid);
        boolean isConnected = isConnected(p2pPart);

        if (hasController && p2pPart.getFrequency() == 0) {
            if (!p2pPart.isOutput()) {
                short frequency = p2pService.newFrequency();
                p2pService.updateFreq(p2pPart, frequency);
                if (!isConnected) {
                    ((P2PTunnelPartMixin) p2pPart).invokeSetOutput(false);
                }
            }
        } else if (!hasController) {
            ((P2PTunnelPartMixin) p2pPart).invokeSetOutput(true);
        }

    }

    private boolean checkForController(IGrid grid) {
        if (grid == null) return false;
        Iterable<IGridNode> nodes = grid.getNodes();
        if (nodes == null) return false;
        for (IGridNode node : nodes) {
            if (node == null) continue;
            if (node.getOwner() instanceof ControllerBlockEntity) {
                return true;
            }
        }
        return false;
    }

    public static boolean isConnected(P2PTunnelPart<?> p2pPart) {
        if (p2pPart == null) return false;
        if (p2pPart.isOutput()) {
            // 输出设备：检查是否有输入
            return p2pPart.getInput() != null;
        } else {
            // 输入设备：检查是否有输出
            List<?> outputs = p2pPart.getOutputs();
            return outputs != null && !outputs.isEmpty();
        }
    }

    public void renameP2P(P2PTunnelPart<?> p2pPart, String name){
        CompoundTag tag = new CompoundTag();
        CustomNameUtil.setCustomName(tag, name);
        p2pPart.importSettings(SettingsFrom.MEMORY_CARD, tag, player);
    }

    public static ResourceLocation getP2PType(P2PTunnelPart<?> p2pPart){
        IPartItem<?> partItem = p2pPart.getPartItem();
        return IPartItem.getId(partItem);
    }

    public P2PTunnelPart<?> getP2PTunnelPart(){
        return p2pTunnelPartBinding;
    }

    public Map<P2PTunnelPart<?>, ResourceLocation> getP2PDevicesMap() {
        return p2pDevicesMap;
    }

    public List<P2PTunnelPart<?>> getP2pDevices() {
        return p2pDevices;
    }

    public List<ResourceLocation> getP2pTypes() {
        return p2pTypes;
    }

    public void setP2PTunnelPart(P2PTunnelPart<?> p2pPart){
        this.p2pTunnelPartBinding = p2pPart;
    }

    public Player getPlayer() {
        return player;
    }

}
