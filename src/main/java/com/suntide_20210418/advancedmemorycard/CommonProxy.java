package com.suntide_20210418.advancedmemorycard;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * 服务端/客户端共用代理。客户端逻辑在 {@link client.ClientProxy} 中扩展。
 */
public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {}

    public void init(FMLInitializationEvent event) {}
}
