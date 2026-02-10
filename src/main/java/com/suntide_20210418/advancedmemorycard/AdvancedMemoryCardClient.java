package com.suntide_20210418.advancedmemorycard;

import com.suntide_20210418.advancedmemorycard.client.renderer.CopyModeRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = AdvancedMemoryCardMod.MOD_ID, dist = Dist.CLIENT)
public class AdvancedMemoryCardClient {
    public AdvancedMemoryCardClient(ModContainer container) {
        // 注册 CopyModeRenderer 中的事件
        NeoForge.EVENT_BUS.addListener(CopyModeRenderer::onRenderLevelStage);

        // 如果有其他客户端事件，也在这里注册
        // NeoForge.EVENT_BUS.addListener(OtherClientClass::otherEvent);
    }
}