package com.suntide_20210418.advancedmemorycard.mixin;

import appeng.parts.p2p.P2PTunnelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(P2PTunnelPart.class)
public interface P2PTunnelPartMixin {

    /**
     * 公开 setOutput 方法
     */
    @Invoker(value = "setOutput", remap = false)
    void setOutput(boolean output);
}
