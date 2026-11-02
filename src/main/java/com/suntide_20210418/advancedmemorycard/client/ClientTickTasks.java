package com.suntide_20210418.advancedmemorycard.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.client.Minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * 客户端「等条件成立再做」的延迟任务队列。
 *
 * <p>
 * 它的存在是因为有些动作只能等服务器回包之后做。传送是最典型的例子：动作包发出去只是
 * 「请求传送」，玩家坐标要等下一两个 tick 的位置同步包才变；跨维度还要等新维度的区块加载完。
 * 在那之前去点亮或转头，算出来的是旧位置的角度，而且新维度里的方块实体还不存在，渲染根本
 * 拿不到部件。
 *
 * <p>
 * 所以这里不做「延迟 N tick」这种猜时间的写法，而是每个 tick 检查一次条件，成立就执行并
 * 移除。超时同样移除 —— 条件永远不成立时（比如服务端拒绝了传送）不能留下一个永久轮询。
 */
public final class ClientTickTasks {

    private static final Logger LOGGER = LogManager.getLogger("AdvancedMemoryCard");

    private static final List<Task> TASKS = new ArrayList<>();

    /**
     * 必须注册实例：1.12.2 的 FML {@code EventBus} 只有 {@code register(Object)}，
     * 传 {@code ClientTickTasks.class} 会去扫描 {@code java.lang.Class} 的方法，
     * {@code onClientTick} 根本不会被注册，所有延迟任务都不会执行。
     */
    private static final ClientTickTasks INSTANCE = new ClientTickTasks();

    private ClientTickTasks() {}

    private static final class Task {

        final Predicate<Minecraft> condition;
        final Runnable action;
        int ticksLeft;

        Task(Predicate<Minecraft> condition, Runnable action, int ticksLeft) {
            this.condition = condition;
            this.action = action;
            this.ticksLeft = ticksLeft;
        }
    }

    /**
     * 注册一个等待条件成立的任务。
     *
     * @param condition 每个 tick 检查一次；成立时执行 {@code action} 并移除任务
     * @param action    条件成立后要执行的动作
     * @param maxTicks  最多等多少个 tick，超时直接丢弃（不执行 {@code action}）
     */
    public static void when(Predicate<Minecraft> condition, Runnable action, int maxTicks) {
        TASKS.add(new Task(condition, action, maxTicks));
    }

    /** 在 {@code ClientProxy#init} 注册到 FML 总线。 */
    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            boolean ready;
            try {
                ready = task.condition.test(mc);
            } catch (RuntimeException e) {
                // 条件本身出错时直接丢弃，不能让一个坏任务卡住整条队列
                LOGGER.warn("Dropping client tick task with failing condition", e);
                it.remove();
                continue;
            }
            if (ready) {
                it.remove();
                try {
                    task.action.run();
                } catch (RuntimeException e) {
                    LOGGER.warn("Client tick task failed", e);
                }
            } else if (task.ticksLeft <= 0) {
                it.remove();
            } else {
                task.ticksLeft--;
            }
        }
    }
}
