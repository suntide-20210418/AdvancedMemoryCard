package com.suntide_20210418.advancedmemorycard.item.custom;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public abstract class CardMode {
    private static final Map<ResourceLocation, Supplier<CardMode>> REGISTRY = new HashMap<>();
    private static final NavigableSet<ResourceLocation> CYCLE_ORDER = new TreeSet<>();
    private static final String MODE_TYPE = "type";
    private static final String MODE_DATA = "mode_data";

    public static CardMode of(ItemStack stack) {
        CompoundTag data = getData(stack);
        var supplier = REGISTRY.get(ResourceLocation.parse(data.getString(MODE_TYPE)));
        if (supplier == null) {
            return new CopyMode();
        } else {
            return supplier.get().load(data);
        }
    }

    /** Extract out the {@link CompoundTag} for this {@link AdvancedMemoryCardItem}'s data */
    public static CompoundTag getData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.getCompound(MODE_DATA);
    }

    public static void register(ResourceLocation key, Supplier<CardMode> supplier) {
        REGISTRY.put(key, supplier);
        CYCLE_ORDER.add(key);
    }

    public static void register(String namespace, String path, Supplier<CardMode> supplier) {
        register(ResourceLocation.fromNamespaceAndPath(namespace, path), supplier);
    }

    public static void initializeModes() {
        if (REGISTRY.isEmpty()) { // 避免重复注册
            register(AdvancedMemoryCardMod.MOD_ID, "copy", CopyMode::new);
            register(AdvancedMemoryCardMod.MOD_ID, "config", ConfigMode::new);
        }
    }

    public abstract ResourceLocation getType();

    /**
     * Called from the onItemUseFirst method in the {@link AdvancedMemoryCardItem}
     *
     * <p>Called before the item's actually used on the part/block so it can be cancelled or
     * modified to change what the default {@link appeng.items.tools.MemoryCardItem} behavior is.
     */
    public abstract InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context);

    /**
     * Called from the onItemUse method in the {@link AdvancedMemoryCardItem}
     *
     * <p>Mainly for modes to implement "submodes" that players can cycle between with right clicks
     */
    public InteractionResultHolder<ItemStack> onItemUse(
            Level level, Player player, InteractionHand hand) {
        // do nothing by default, some modes won't need this at all
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    /**
     * Save this mode's information to NBT to be loaded later
     *
     * <p>Overriders should call this super method and use the returned tag to put in data.
     * Overriders should then return that same tag so subclasses can use it.
     *
     * @param tag
     * @return
     */
    public CompoundTag save(CompoundTag tag) {
        CompoundTag data = new CompoundTag();
        data.putString(MODE_TYPE, getType().toString());
        tag.put(MODE_DATA, data);
        return data;
    }

    protected CardMode load(CompoundTag tag) {
        return this;
    }

    public abstract Component getName();

    protected abstract Component getDescription();

    public static CardMode cycleMode(CardMode mode, boolean cycleForward) {
        // 检查集合是否为空，避免 NoSuchElementException
        if (CYCLE_ORDER.isEmpty()) {
            return mode; // 如果没有注册的模式，返回当前模式
        }

        ResourceLocation current = mode.getType();
        ResourceLocation next =
                cycleForward ? CYCLE_ORDER.higher(current) : CYCLE_ORDER.lower(current);
        if (next == null) {
            next = cycleForward ? CYCLE_ORDER.first() : CYCLE_ORDER.last();
        }
        return REGISTRY.get(next).get();
    }
}
