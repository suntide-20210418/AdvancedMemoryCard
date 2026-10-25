package com.suntide_20210418.advancedmemorycard.item.custom;

import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Supplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public abstract class CardMode {
    private static final Map<ResourceLocation, Supplier<CardMode>> REGISTRY = new HashMap<>();
    private static final NavigableSet<ResourceLocation> CYCLE_ORDER = new TreeSet<>();
    private static final String MODE_TYPE = "type";
    private static final String MODE_DATA = "mode_data";

    public static CardMode of(ItemStack stack) {
        CompoundTag data = getData(stack);
        String typeString = data.getString(MODE_TYPE);
        ResourceLocation typeLocation = typeString.isEmpty() ? null : ResourceLocation.tryParse(typeString);
        var supplier = typeLocation != null ? REGISTRY.get(typeLocation) : null;
        if (supplier == null) {
            return new CopyMode();
        } else {
            return supplier.get().load(data);
        }
    }

    /** Extract out the {@link CompoundTag} for this {@link AdvancedMemoryCardItem}'s data */
    public static CompoundTag getData(ItemStack stack) {
        // 1.21.1 方法：从 CustomData 组件获取 CompoundTag
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains(MODE_DATA)) {
                return tag.getCompound(MODE_DATA);
            }
        }
        return new CompoundTag();
    }

    /**
     * 整张卡的根标签（CUSTOM_DATA 全文）。
     *
     * <p>根标签下同时存着两样东西：{@code mode_data}（当前模式的数据）和跨模式共享的
     * 卡片级偏好（例如复制模式的粘贴选项）。后者必须放在 {@code mode_data} 外面，
     * 否则每次切模式写 {@code mode_data} 都会把它覆盖成默认值。
     */
    public static CompoundTag getRootData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    public static void setRootData(ItemStack stack, CompoundTag rootTag) {
        if (rootTag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
        }
    }

    /** 保存当前模式的数据到 ItemStack（保留根标签里的其它键） */
    public static void saveDataToStack(ItemStack stack, CompoundTag modeData) {
        // 1.21.1 方法：读出现有根标签再替换 mode_data，避免把卡片级偏好一起抹掉
        CompoundTag rootTag = getRootData(stack);
        rootTag.put(MODE_DATA, modeData);

        setRootData(stack, rootTag);
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
     * <p>Called before the item's actually used on the part/block so it can be canceled or
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
     * @param tag The mode data tag to save to
     * @return The updated mode data tag
     */
    public CompoundTag save(CompoundTag tag) {
        tag.putString(MODE_TYPE, getType().toString());
        return tag;
    }

    /**
     * 保存模式数据到 ItemStack 的便捷方法
     */
    public void saveToStack(ItemStack stack) {
        CompoundTag modeData = new CompoundTag();
        save(modeData);
        saveDataToStack(stack, modeData);
    }

    /**
     * Load mode data from NBT
     *
     * @param tag The mode data tag to load from
     * @return The loaded mode instance
     */
    protected CardMode load(CompoundTag tag) {
        // 可以在这里加载特定模式的数据
        return this;
    }

    public abstract Component getName();

    public abstract Component getDescription();

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