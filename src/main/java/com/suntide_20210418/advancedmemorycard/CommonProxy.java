package com.suntide_20210418.advancedmemorycard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import com.suntide_20210418.advancedmemorycard.p2p.P2PManager;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * 服务端/客户端共用代理。客户端逻辑在 {@link client.ClientProxy} 中扩展。
 *
 * <p>
 * 此类及其持有的状态（待打开 GUI 的 P2PManager 缓存、客户端 GUI 构造委托）均为
 * 服务端安全，可被专用服务端加载而无需触碰任何客户端类。
 * </p>
 */
public class CommonProxy {

    // 待打开 GUI 时由服务端暂存的 P2PManager（按玩家 UUID 索引），供 GuiHandler 在服务端构造容器时取出。
    private static final Map<UUID, P2PManager> pendingManagers = new HashMap<>();

    public void preInit(FMLPreInitializationEvent event) {}

    public void init(FMLInitializationEvent event) {}

    /**
     * 暂存当前玩家即将打开的配置 GUI 所使用的 P2PManager。
     * 由通用代码（如 ConfigMode）调用，不依赖任何客户端类。
     */
    public static void putPendingManager(EntityPlayer player, P2PManager manager) {
        pendingManagers.put(player.getUniqueID(), manager);
    }

    /**
     * 取出并移除暂存的 P2PManager（一次性消费）。在 GuiHandler 的服务端容器构造阶段调用。
     */
    public static P2PManager takePendingManager(EntityPlayer player) {
        return pendingManagers.remove(player.getUniqueID());
    }

    /**
     * 构造客户端 GUI 屏幕。通用侧（GuiHandler）在 getClientGuiElement 中委托此方法，
     * 由 ClientProxy 覆写以构造真正的屏幕；专用服务端不会调用本方法（getClientGuiElement 仅在客户端执行）。
     *
     * @return 客户端 GUI 对象，或服务端默认返回 null。
     */
    public Object openClientGui(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }
}
