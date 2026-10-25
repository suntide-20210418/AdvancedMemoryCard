package com.suntide_20210418.advancedmemorycard.item.custom;

import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.Area;
import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.calculateVolume;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.CopyMode.*;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Tooltip.*;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.util.SettingsFrom;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class CopyMode extends CardMode {

    private static final String START_POS = "start_pos";
    private static final String END_POS = "end_pos";
    private static final String IS_COPYING = "is_copying";
    /**
     * 粘贴时应用哪些内容（{@link CopyOption} 的位掩码）。
     *
     * <p>这是「每张卡的偏好」，不是当前模式的状态，因此存在卡片的根标签里而不是
     * {@code mode_data} 里 —— 否则切换模式时 {@code CardMode#cycleMode} 会用新建的
     * 模式实例覆盖 {@code mode_data}，把选项重置成默认值。
     */
    private static final String APPLY_OPTIONS_MASK = "apply_options_mask";

    private BlockPos startPos;
    private BlockPos endPos;
    private boolean isCopying;

    public CopyMode() {}

    @Override
    protected CardMode load(CompoundTag tag) {
        // 处理 startPos 为空的情况
        if (tag.contains(START_POS)) {
            startPos = BlockPos.of(tag.getLong(START_POS));
        } else {
            startPos = null;
        }

        // 处理 endPos 为空的情况
        if (tag.contains(END_POS)) {
            endPos = BlockPos.of(tag.getLong(END_POS));
        } else {
            endPos = null;
        }

        // 处理 isCopying 为空的情况，默认为 false
        isCopying = tag.getBoolean(IS_COPYING);

        return this;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag = super.save(tag); // 先调用父类保存类型信息

        // 只有当值不为 null 时才保存
        if (startPos != null) {
            tag.putLong(START_POS, startPos.asLong());
        }

        if (endPos != null) {
            tag.putLong(END_POS, endPos.asLong());
        }

        tag.putBoolean(IS_COPYING, isCopying);
        return tag;
    }

    // 获取渲染颜色（可以根据不同状态调整）
    public int getSelectionColor() {
        if (isCopying) {
            return ModConfigs.getClientConfig().copySelectionReady.get(); // 绿色 - 准备粘贴状态
        } else if (endPos == null && startPos != null) {
            return ModConfigs.getClientConfig().copySelectionSecond.get(); // 白色 - 选择第二个点状态
        } else {
            return ModConfigs.getClientConfig().copySelectionFirst.get(); // 红色 - 选择第一个点状态
        }
    }

    public BlockPos getStartPos() {
        return startPos;
    }

    public BlockPos getEndPos() {
        return endPos;
    }

    public void setEndPos(ItemStack stack, BlockPos endPos) {
        this.endPos = endPos;
        this.saveToStack(stack);
    }

    public void setStartPos(ItemStack stack, BlockPos startPos) {
        this.startPos = startPos;
        this.saveToStack(stack);
    }

    public void clearPos(ItemStack stack) {
        startPos = null;
        endPos = null;
        isCopying = false;
        this.saveToStack(stack);
    }

    /**
     * 读取这张卡的粘贴选项掩码。
     *
     * <p>旧卡（或从没打开过选项界面）没有该键 → 全选，粘贴行为与改造前一致。
     */
    public static int getApplyMask(ItemStack card) {
        CompoundTag root = getRootData(card);
        return root.contains(APPLY_OPTIONS_MASK, Tag.TAG_INT)
                ? root.getInt(APPLY_OPTIONS_MASK) & CopyOption.ALL
                : CopyOption.ALL;
    }

    /** 写入粘贴选项掩码并落盘（卡级偏好，与当前模式无关，切模式不会丢）。 */
    public static void setApplyMask(ItemStack card, int mask) {
        CompoundTag root = getRootData(card);
        // 允许写入 0（四项全不选）：此时粘贴不会改动任何「可选项」覆盖的内容，
        // 只有不在选项内的部分（重命名、P2P 类型/频段）仍会照常应用。
        root.putInt(APPLY_OPTIONS_MASK, mask & CopyOption.ALL);
        setRootData(card, root);
    }

    /**
     * 生成一张「只保留已勾选导出组件」的卡片副本，供粘贴时作为数据源使用。
     *
     * <p>不能改写原卡：原卡是玩家手上的真实物品，且复制内容（导出结果）与粘贴选项是两件事。
     * 组件表在 {@code ItemStack#copy()} 里是浅拷贝，循环外算一次即可。
     */
    public ItemStack createFilteredCard(ItemStack card) {
        int mask = getApplyMask(card);
        ItemStack filtered = card.copy();
        for (CopyOption option : CopyOption.values()) {
            if ((mask & option.mask()) == 0) {
                filtered.remove(option.component());
            }
        }
        return filtered;
    }

    /**
     * 用目标自身导出的值回填「未勾选但不能靠缺失表达」的选项。
     *
     * <p>AE2 有几处导入实现把「组件缺失」当成「把目标清空」：名称被置 null、样板被清空、
     * P2P 类型分支整段不执行。对这些选项，取消勾选时必须把目标当前值填回去，
     * 语义才等于「这一项不复制、目标保持原样」。只在真的存在这类未勾选项时才调用。
     */
    private static ItemStack withTargetValues(ItemStack template, DataComponentMap targetValues, int applyMask) {
        ItemStack patched = template.copy();
        for (CopyOption option : CopyOption.values()) {
            if (!option.preserveTargetValue() || (applyMask & option.mask()) != 0) {
                continue;
            }
            copyComponent(patched, targetValues, option.component());
        }
        return patched;
    }

    /** 目标没有该项时不填（保持缺失，等价于「目标本来就没有」）。 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copyComponent(ItemStack target, DataComponentMap source, DataComponentType<?> type) {
        Object value = source.get(type);
        if (value != null) {
            target.set((DataComponentType) type, value);
        }
    }

    private static DataComponentMap exportPartSettings(IPart part) {
        DataComponentMap.Builder builder = DataComponentMap.builder();
        part.exportSettings(SettingsFrom.MEMORY_CARD, builder);
        return builder.build();
    }

    @Override
    public InteractionResultHolder<ItemStack> onItemUse(
            Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (this.isCopying) {
            // 只把「勾选了的」导出组件交给导入方：未勾选的组件直接从卡片副本上移除，
            // AE2 的 importGenericSettings 取不到就跳过对应步骤（方块实体与 P2P 部件都走这条数据）。
            int applyMask = getApplyMask(stack);
            ItemStack template = createFilteredCard(stack);
            boolean needsTargetValues = CopyOption.needsTargetValues(applyMask);
            // 1.21.1 方式：获取 DataComponentMap
            DataComponentMap dataComponents = template.getComponents();

            int count = 0;

            if (startPos != null && endPos != null) {
                Iterable<BlockPos> positions = BlockPos.betweenClosed(startPos, endPos);
                for (BlockPos pos : positions) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof AEBaseBlockEntity aeBlockEntity) {
                        DataComponentMap source = needsTargetValues
                                ? withTargetValues(template,
                                        aeBlockEntity.exportSettings(SettingsFrom.MEMORY_CARD, player),
                                        applyMask).getComponents()
                                : dataComponents;
                        aeBlockEntity.importSettings(SettingsFrom.MEMORY_CARD, source, player);
                        count++;
                    }
                    // 处理AE2部件宿主（IPartHost）
                    if (blockEntity instanceof IPartHost partHost) {
                        // 获取所有方向（包括内部）
                        for (Direction direction : Direction.values()) {
                            IPart part = partHost.getPart(direction);
                            if (part != null) {
                                // 部件内部读的是传入栈自己的组件，所以回填也要落在栈副本上。
                                // 必须传真实手持，AE2 在 OFF_HAND 时会直接忽略
                                ItemStack partCard = needsTargetValues
                                        ? withTargetValues(template, exportPartSettings(part), applyMask)
                                        : template;
                                part.onUseItemOn(partCard, player, hand, pos.getCenter());
                                count++;
                            }
                        }
                    }
                }
            }

            player.displayClientMessage(completed(count), true);
            this.isCopying = false;
            this.startPos = null;
            this.endPos = null;
            // 使用新的保存方法
            this.saveToStack(stack);

        } else {
            player.displayClientMessage(failed(), true);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ResourceLocation getType() {
        return ResourceLocation.fromNamespaceAndPath(AdvancedMemoryCardMod.MOD_ID, "copy");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();

        if (startPos == null) {
            startPos = clickedPos;
            // 使用新的保存方法
            this.saveToStack(stack);

            // 向玩家发送信息
            if (player != null) {
                player.displayClientMessage(firstPosMarked(startPos.toShortString()), true);
            }

            return InteractionResult.SUCCESS;

        } else {
            endPos = clickedPos;

            long currentVolume = calculateVolume(startPos, endPos);
            if (currentVolume > getMaxVolume()) {
                if (player != null) {
                    player.displayClientMessage(tooLarge(currentVolume, getMaxVolume()), true);
                }
                // 重置选择
                this.startPos = null;
                this.endPos = null;
                this.saveToStack(stack);
                return InteractionResult.FAIL;
            }

            isCopying = true;
            this.saveToStack(stack);

            if (player != null) {
                player.displayClientMessage(
                        secondPosMarked(
                                endPos.toShortString(),
                                Area(startPos, endPos)[0],
                                Area(startPos, endPos)[1],
                                Area(startPos, endPos)[2]),
                        true);
            }

            return InteractionResult.SUCCESS;
        }
    }

    public BlockPos getTargetedBlockPos(Player player) {
        if (player == null) return null;

        // 使用内置的视线追踪方法 - 1.21.1 中方法可能保持一致
        HitResult hitResult = player.pick(player.blockInteractionRange(), 1.0F, false);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hitResult).getBlockPos();
        }

        return null;
    }

    @Override
    public Component getName() {
        return show();
    }

    @Override
    public Component getDescription() {
        if (startPos == null) {
            // 没有选择任何位置，显示Copy Mode
            return copyInfo();
        } else if (endPos == null) {
            // 已选择第一个位置，显示第一个位置信息
            return copyInfo().copy().append(copyFirstPos(startPos.toShortString()));
        } else {
            // 已选择两个位置，显示完整信息并提示准备粘贴
            return copyInfo()
                    .copy()
                    .append(copyFirstPos(startPos.toShortString()))
                    .append(copySecondPos(endPos.toShortString()))
                    .append(copyReady());
        }
    }

    public static int getMaxVolume() {
        return ModConfigs.getServerConfig().maxCopyVolume.get();
    }

    // 新增：检查模式是否有效的方法
    public boolean isValid() {
        return startPos != null && endPos != null && isCopying;
    }

    // 新增：重置模式状态
    public void reset() {
        startPos = null;
        endPos = null;
        isCopying = false;
    }
}