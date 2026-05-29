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
import com.suntide_20210418.advancedmemorycard.client.renderer.P2PRenderer;
import com.suntide_20210418.advancedmemorycard.mixin.P2PTunnelPartMixin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;

public class P2PManager {
    private P2PTunnelPart<?> p2pTunnelPartBinding;
    private IGrid grid;
    private final Player player;
    private final P2PService p2pService;
    private final HashMap<P2PTunnelPart<?>, ResourceLocation> p2pDevicesMap;
    private final HashMap<String, Short> p2pFrequencyAndAlias;

    public P2PManager(P2PTunnelPart<?> p2pTunnelPartBinding, Player player) {
        this.p2pTunnelPartBinding = p2pTunnelPartBinding;
        this.p2pDevicesMap = new HashMap<>();
        this.p2pFrequencyAndAlias = new HashMap<>();
        this.player = player;
        analysisP2P();
        this.p2pService = P2PService.get(grid);
        autoConfigP2PIO();
        renderP2P(p2pTunnelPartBinding);
    }

    public void analysisP2P() {
        // 直接通过 P2PTunnelPart 获取其所在的 GridNode
        IGridNode gridNode = p2pTunnelPartBinding.getGridNode();
        p2pFrequencyAndAlias.clear();
        p2pDevicesMap.clear();
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
                short frequency = p2pPart.getFrequency();
                if (!p2pFrequencyAndAlias.containsValue(frequency)) {
                    p2pFrequencyAndAlias.put(String.valueOf(frequency), frequency);
                }
            }
        }
    }

    public void bind(short frequency){
        if (grid != null) {
            p2pService.updateFreq(p2pTunnelPartBinding, frequency);
        }
    }

    public void autoConfigP2PIO() {
        if (grid == null || grid.isEmpty() || p2pDevicesMap.isEmpty()) {
            return;
        }
        if (p2pService == null) return;

        for (P2PTunnelPart<?> p2pPart : p2pDevicesMap.keySet()) {
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

    public void renderP2P(P2PTunnelPart<?> p2pPart) {
        P2PRenderer p2pRenderer = P2PRenderer.getInstance();
        p2pRenderer.clearAllRenders();
        p2pRenderer.triggerRender(p2pPart, 0xFF0000);
        p2pRenderer.triggerRender(p2pPart.getInput(), 0x00FF00);
        for (P2PTunnelPart<?> output : p2pPart.getOutputs()) {
            if (output == p2pPart) continue;
            p2pRenderer.triggerRender(output, 0x0000FF);
        }
    }

    public void renderP2P(short frequency){
        P2PRenderer p2pRenderer = P2PRenderer.getInstance();
        p2pRenderer.clearAllRenders();
        P2PTunnelPart<?> input = p2pService.getInput(frequency);
        p2pRenderer.triggerRender(input, 0x00FF00);
        input.getOutputs().forEach(output -> p2pRenderer.triggerRender(output, 0x0000FF));
    }

    public P2PTunnelPart<?> getP2PTunnelPart(){
        return p2pTunnelPartBinding;
    }

    public HashMap<P2PTunnelPart<?>, ResourceLocation> getP2PDevicesMap() {
        return p2pDevicesMap;
    }

    public void setP2PTunnelPart(P2PTunnelPart<?> p2pPart){
        this.p2pTunnelPartBinding = p2pPart;
    }

    public HashMap<String, Short> getP2PFrequencyAndAlias() {
        return p2pFrequencyAndAlias;
    }

    public void setFrequencyAlias(String alias, short frequency) {
        for (HashMap.Entry<String, Short> entry : p2pFrequencyAndAlias.entrySet()) {
            if (entry.getValue() == frequency) {
                p2pFrequencyAndAlias.remove(entry.getKey());
                p2pFrequencyAndAlias.put(alias, frequency);
                break;
            }
        }
    }
}
