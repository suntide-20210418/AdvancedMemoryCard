package com.suntide_20210418.advancedmemorycard.item.custom;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.util.IConfigurableObject;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import appeng.items.tools.MemoryCardItem;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.SettingsFrom;
import com.suntide_20210418.advancedmemorycard.AdvancedMemoryCardMod;
import com.suntide_20210418.advancedmemorycard.config.ModConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.Area;
import static com.suntide_20210418.advancedmemorycard.utils.AreaHelper.calculateVolume;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.CopyMode.*;
import static com.suntide_20210418.advancedmemorycard.utils.TranslateHelper.Tooltip.*;

public class CopyMode extends CardMode {

    private static final String START_POS = "start_pos";
    private static final String END_POS = "end_pos";
    private static final String IS_COPYING = "is_copying";
    /**
     * 粘贴时应用哪些内容（{@link CopyOption} 的位掩码）。
     *
     * <p>这是「每张卡的偏好」，不是当前模式的状态，因此存在卡片的根标签里而不是
     * {@code mode_data} 里 —— 否则切换模式时 {@link CardMode#cycleMode} 会用新建的
     * 模式实例整体重写 {@code mode_data}，把选项重置成默认值。
     */
    private static final String APPLY_OPTIONS_MASK = "apply_options_mask";
    /**
     * 每次「复制完成」（第二个角确定）后是否自动弹出选项界面。
     */
    private static final String AUTO_OPEN_OPTIONS = "auto_open_options";

    private static final String P2P_TYPE_KEY = "p2pType";
    private static final String CONFIG_NAME_KEY = "Config";

    private BlockPos startPos;
    private BlockPos endPos;
    private boolean isCopying;

    public CopyMode() {}

    @Override
    protected CardMode load(CompoundTag tag) {
        if (tag.contains(START_POS)) {
            startPos = BlockPos.of(tag.getLong(START_POS));
        } else {
            startPos = null;
        }

        if (tag.contains(END_POS)) {
            endPos = BlockPos.of(tag.getLong(END_POS));
        } else {
            endPos = null;
        }

        isCopying = tag.getBoolean(IS_COPYING);
        return this;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag data = super.save(tag);

        if (startPos != null) {
            data.putLong(START_POS, startPos.asLong());
        }
        if (endPos != null) {
            data.putLong(END_POS, endPos.asLong());
        }
        data.putBoolean(IS_COPYING, isCopying);
        return data;
    }

    // ==================== 卡片级粘贴选项 ====================

    /**
     * 读取这张卡的粘贴选项掩码。
     *
     * <p>从未配置过的卡默认 0（全不选）：粘贴不改动任何可选项，玩家在自动弹出的界面里
     * 显式勾选后才会写回并沿用。
     */
    public static int getApplyMask(ItemStack card) {
        CompoundTag root = getRootData(card);
        return root.contains(APPLY_OPTIONS_MASK, Tag.TAG_INT)
                ? root.getInt(APPLY_OPTIONS_MASK) & CopyOption.ALL
                : 0;
    }

    /** 写入粘贴选项掩码并落盘（卡级偏好，与当前模式无关，切模式不会丢）。 */
    public static void setApplyMask(ItemStack card, int mask) {
        CompoundTag root = getRootData(card);
        root.putInt(APPLY_OPTIONS_MASK, mask & CopyOption.ALL);
        setRootData(card, root);
    }

    /** 复制完成后是否自动弹出选项界面（未配置过的卡默认弹出）。 */
    public static boolean isAutoOpenOptions(ItemStack card) {
        CompoundTag root = getRootData(card);
        return !root.contains(AUTO_OPEN_OPTIONS, Tag.TAG_BYTE) || root.getBoolean(AUTO_OPEN_OPTIONS);
    }

    public static void setAutoOpenOptions(ItemStack card, boolean enabled) {
        CompoundTag root = getRootData(card);
        root.putBoolean(AUTO_OPEN_OPTIONS, enabled);
        setRootData(card, root);
    }

    // ==================== 粘贴 ====================

    @Override
    public InteractionResultHolder<ItemStack> onItemUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.isCopying) {
            player.displayClientMessage(failed(), true);
            return InteractionResultHolder.consume(stack);
        }

        int applyMask = getApplyMask(stack);
        MemoryCardItem card = (MemoryCardItem) stack.getItem();
        CompoundTag source = card.getData(stack);
        // "Config" 记录来源对象，用来防止把样板供应器的设置贴到接口上
        String sourceName = stack.getTag() == null ? "" : stack.getTag().getString(CONFIG_NAME_KEY);
        CompoundTag filtered = CopyOption.createFiltered(source, applyMask);

        boolean filterSettings = !CopyOption.SETTINGS.enabled(applyMask);
        boolean patchTargetValues = CopyOption.needsTargetValues(applyMask);
        boolean fromP2PCard = source.contains(P2P_TYPE_KEY);

        int count = 0;

        if (startPos != null && endPos != null) {
            for (BlockPos pos : BlockPos.betweenClosed(startPos, endPos)) {
                BlockEntity blockEntity = level.getBlockEntity(pos);

                if (blockEntity instanceof AEBaseBlockEntity aeBlockEntity) {
                    CompoundTag tag = prepareForBlockEntity(
                            filtered, aeBlockEntity, applyMask, filterSettings, patchTargetValues, player);
                    aeBlockEntity.importSettings(SettingsFrom.MEMORY_CARD, tag, player);
                    count++;
                }

                if (blockEntity instanceof IPartHost partHost) {
                    for (Direction direction : Direction.values()) {
                        IPart part = partHost.getPart(direction);
                        if (part == null) {
                            continue;
                        }
                        // P2P 卡只能贴 P2P 部件；其余部件要求「设置来源」一致，
                        // 与 AE2 自己的 AEBasePart#useMemoryCard 校验同一规则。
                        boolean isP2P = part instanceof P2PTunnelPart<?>;
                        if (isP2P) {
                            if (!fromP2PCard) {
                                continue;
                            }
                        } else if (!sourceName.equals(settingsNameOf(part))) {
                            continue;
                        }

                        CompoundTag tag = prepareForPart(
                                filtered, part, applyMask, filterSettings, patchTargetValues);
                        applyToPart(partHost, part, direction, tag, player, hand);
                        count++;
                    }
                }
            }
        }

        player.displayClientMessage(completed(count), true);
        this.isCopying = false;
        this.startPos = null;
        this.endPos = null;
        this.save(stack.getOrCreateTag());
        return InteractionResultHolder.consume(stack);
    }

    private static CompoundTag prepareForBlockEntity(
            CompoundTag filtered,
            AEBaseBlockEntity target,
            int applyMask,
            boolean filterSettings,
            boolean patchTargetValues,
            Player player) {
        CompoundTag tag = filtered.copy();
        if (filterSettings && target instanceof IConfigurableObject configurable) {
            CopyOption.removeTargetSettings(tag, configurable.getConfigManager());
        }
        if (patchTargetValues) {
            CompoundTag targetExport = new CompoundTag();
            target.exportSettings(SettingsFrom.MEMORY_CARD, targetExport, player);
            CopyOption.restoreTargetValues(tag, targetExport, applyMask);
        }
        return tag;
    }

    private static CompoundTag prepareForPart(
            CompoundTag filtered, IPart target, int applyMask, boolean filterSettings, boolean patchTargetValues) {
        CompoundTag tag = filtered.copy();
        if (filterSettings && target instanceof IConfigurableObject configurable) {
            CopyOption.removeTargetSettings(tag, configurable.getConfigManager());
        }
        if (patchTargetValues) {
            CompoundTag targetExport = new CompoundTag();
            target.exportSettings(SettingsFrom.MEMORY_CARD, targetExport);
            CopyOption.restoreTargetValues(tag, targetExport, applyMask);
        }
        return tag;
    }

    /**
     * 把过滤后的设置写到部件上。
     *
     * <p>P2P 部件在 AE2 的原生路径里有两步：先按 {@code p2pType} 把部件换成目标类型，
     * 再导入设置。这里直接调 {@code importSettings} 会跳过换型，所以显式补上这一步；
     * 未勾选 P2P 类型时 {@link CopyOption#restoreTargetValues} 回填的是目标自身的类型，
     * 换型自然成为 no-op。
     */
    private static void applyToPart(
            IPartHost host, IPart part, Direction side, CompoundTag tag, Player player, InteractionHand hand) {
        if (part instanceof P2PTunnelPart<?>) {
            String typeId = tag.getString(P2P_TYPE_KEY);
            if (!typeId.isEmpty()) {
                ResourceLocation typeLocation = ResourceLocation.tryParse(typeId);
                IPartItem<?> partItem = typeLocation == null ? null : IPartItem.byId(typeLocation);
                if (partItem != null && partItem != part.getPartItem()
                        && P2PTunnelPart.class.isAssignableFrom(partItem.getPartClass())) {
                    @SuppressWarnings("unchecked")
                    IPart replaced = host.replacePart((IPartItem) partItem, side, player, hand);
                    if (replaced instanceof P2PTunnelPart<?> tunnel) {
                        tunnel.importSettings(SettingsFrom.MEMORY_CARD, tag, player);
                    }
                    return;
                }
            }
        }
        part.importSettings(SettingsFrom.MEMORY_CARD, tag, player);
    }

    /**
     * 目标部件在记忆卡「Config」字段里会写成什么名字。
     *
     * <p>与 {@code AEBasePart#useMemoryCard} 一致：接口与样板供应器的方块/部件共用同一份设置，
     * 所以名字要取方块的。
     */
    private static String settingsNameOf(IPart part) {
        Item item = part.getPartItem().asItem();
        if (AEParts.INTERFACE.asItem() == item) {
            item = AEBlocks.INTERFACE.asItem();
        } else if (AEParts.PATTERN_PROVIDER.asItem() == item) {
            item = AEBlocks.PATTERN_PROVIDER.asItem();
        }
        return item.getDescriptionId();
    }

    @Override
    public ResourceLocation getType() {
        return ResourceLocation.fromNamespaceAndPath(AdvancedMemoryCardMod.MOD_ID, "copy");
    }

    // 获取渲染颜色（可以从客户端配置调整）
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
        this.save(stack.getOrCreateTag());
    }

    public void setStartPos(ItemStack stack, BlockPos startPos) {
        this.startPos = startPos;
        this.save(stack.getOrCreateTag());
    }

    public void clearPos(ItemStack stack) {
        startPos = null;
        endPos = null;
        isCopying = false;
        this.save(stack.getOrCreateTag());
    }

    public BlockPos getTargetedBlockPos(Player player) {
        if (player == null) return null;

        HitResult hitResult = player.pick(player.getBlockReach(), 1.0F, false);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hitResult).getBlockPos();
        }

        return null;
    }

    public static int getMaxVolume() {
        return ModConfigs.getServerConfig().maxCopyVolume.get();
    }

    @Override
    public Component getName() {
        return show();
    }

    @Override
    protected Component getDescription() {
        if (startPos == null) {
            return copyInfo();
        } else if (endPos == null) {
            return copyInfo().append(copyFirstPos(startPos.toShortString()));
        } else {
            return copyInfo()
                    .append(copyFirstPos(startPos.toShortString()))
                    .append(copySecondPos(endPos.toShortString()))
                    .append(copyReady());
        }
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();

        if (startPos == null) {
            startPos = clickedPos;
            this.save(stack.getOrCreateTag());

            if (player != null) {
                player.displayClientMessage(firstPosMarked(startPos.toShortString()), true);
            }
        } else {
            endPos = clickedPos;

            long currentVolume = calculateVolume(startPos, endPos);
            int maxVolume = getMaxVolume();
            if (currentVolume > maxVolume) {
                if (player != null) {
                    player.displayClientMessage(tooLarge(currentVolume, maxVolume), true);
                }
                this.startPos = null;
                this.endPos = null;
                this.save(stack.getOrCreateTag());
                return InteractionResult.FAIL;
            }

            isCopying = true;
            this.save(stack.getOrCreateTag());
            int[] area = Area(startPos, endPos);

            if (player != null) {
                player.displayClientMessage(
                        secondPosMarked(endPos.toShortString(), area[0], area[1], area[2]), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
