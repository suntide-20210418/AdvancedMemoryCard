package com.suntide_20210418.advancedmemorycard.network;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * 服务端「Netty 线程 → 主线程」的任务队列。
 *
 * <p>
 * 为什么需要它：1.7.10 的 {@code SimpleNetworkWrapper} 处理器跑在 Netty 线程上，
 * 而 {@code MinecraftServer} 在这个版本没有 {@code addScheduledTask}（那个 API 是 1.8 才有的，
 * 已核对 1.7.10 的 srg 映射：{@code func_152344_a} 只存在于客户端 {@code Minecraft}）。
 * 批量绑定一次会改多台设备的网格状态并落盘，在世界线程之外做属于未定义行为。
 *
 * <p>
 * 做法：动作包处理器只把任务入队，这里在服务器 tick 末尾消费。
 * 队列挂在 FML 总线上，任务异常只丢弃该任务，不能让一个坏任务卡住整条队列。
 */
public final class ServerTaskQueue {

    private static final Logger LOGGER = LogManager.getLogger("AdvancedMemoryCard");

    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    private ServerTaskQueue() {}

    /** 由 Netty 线程调用；只入队，不执行。 */
    public static void enqueue(Runnable task) {
        TASKS.add(task);
    }

    /** 在 {@code AdvancedMemoryCardMod#preInit} 注册到 FML 总线。 */
    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(ServerTaskQueue.class);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        Iterator<Runnable> it = TASKS.iterator();
        while (it.hasNext()) {
            Runnable task = it.next();
            it.remove();
            try {
                task.run();
            } catch (RuntimeException e) {
                LOGGER.warn("Server task failed", e);
            }
        }
    }
}
