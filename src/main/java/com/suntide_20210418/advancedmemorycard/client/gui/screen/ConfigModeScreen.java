package com.suntide_20210418.advancedmemorycard.client.gui.screen;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import com.suntide_20210418.advancedmemorycard.client.gui.menu.ConfigModeMenu;
import com.suntide_20210418.advancedmemorycard.client.gui.widgets.P2PTreeWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ConfigModeScreen extends AEBaseScreen<ConfigModeMenu> {

    private P2PTreeWidget p2pTree;
    private Scrollbar scrollbar;
//    private FrequencyDetailPanel detailPanel;

    public ConfigModeScreen(ConfigModeMenu menu, Inventory playerInventory,
                            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        initWidgets();
    }

    private void initWidgets() {
        // 如果已经创建，则直接返回（避免重复添加）
        if (this.p2pTree != null) {
            return;
        }

        // 使用 widgets 管理器创建滚动条，而不是手动创建
        scrollbar = this.widgets.addScrollBar("p2p_tree_scrollbar");
        p2pTree = new P2PTreeWidget(0, 0, 0, 0);

        p2pTree.setSelectionListener(p2pInfo -> {
//            if (p2pInfo != null) {
//                if (detailPanel != null) {
//                    detailPanel.setSelectedP2P(p2pInfo);
//                    detailPanel.setVisible(true);
//                }
//            } else if (detailPanel != null) {
//                detailPanel.setVisible(false);
//            }
        });
//        detailPanel = new FrequencyDetailPanel(menu, style);
//        detailPanel.setVisible(false);
        // 注册 widget（键名必须和 JSON 中一致）
        this.widgets.add("p2p_tree", p2pTree);
//        this.widgets.add("detail_panel", detailPanel);
        updateTreeData();
    }


    // 修改 init 方法
    @Override
    protected void init() {
        super.init();
        updateTreeData();
    }

    // 修改 updateTreeData 方法
    private void updateTreeData() {
        if (p2pTree != null) {
            p2pTree.updateData(
                    menu.getClientP2PTypeInfoMap(),
                    menu.getClientChannelInfoMap(),
                    menu.getClientP2PInfoMap()
            );
            updateScrollbar();
        }
    }

    // 更新滚动条
    private void updateScrollbar() {
        if (p2pTree == null || scrollbar == null) return;

        int totalHeight = p2pTree.getTotalContentHeight();
        int visibleHeight = p2pTree.getHeight();

        if (totalHeight <= visibleHeight) {
            scrollbar.setRange(0, 0, P2PTreeWidget.getRowHeight());
            scrollbar.setVisible(true);
            p2pTree.setScrollOffset(0);
        } else {
            int maxScroll = totalHeight - visibleHeight;
            scrollbar.setRange(0, maxScroll, P2PTreeWidget.getRowHeight());
            scrollbar.setVisible(true);
        }
    }

    // 同步滚动偏移
    private void syncScrollOffset() {
        if (p2pTree != null && scrollbar != null) {
            p2pTree.setScrollOffset(scrollbar.getCurrentScroll());
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        menu.sendUpdateItemInfo();
        updateTreeData();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (p2pTree != null && scrollbar != null) {
            // 同步滚动偏移
            syncScrollOffset();
        }

        setTextContent("title", Component.translatable("gui.advancedmemorycard.config_mode.title"));
        setTextContent("info",  Component.translatable("gui.advancedmemorycard.config_mode.info"));
    }

    // 新增：判断鼠标是否在滚动条上
    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        if (scrollbar == null || !scrollbar.isVisible()) {
            return false;
        }

        Rect2i bounds = scrollbar.getBounds();
        // 滚动条的实际位置需要加上屏幕的绝对位置
        // 注意：scrollbar.getBounds() 返回的已经是绝对坐标（因为 setPosition 设置了 displayX/displayY）
        return mouseX >= bounds.getX() &&
                mouseX <= bounds.getX() + bounds.getWidth() &&
                mouseY >= bounds.getY() &&
                mouseY <= bounds.getY() + bounds.getHeight();
    }

    // 修改 mouseDragged 方法
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // 使用辅助方法判断鼠标是否在滚动条上
        if (scrollbar != null && isMouseOverScrollbar(mouseX, mouseY)) {
            Point mousePoint = new Point((int)mouseX, (int)mouseY);
            scrollbar.onMouseDrag(mousePoint, button);
            syncScrollOffset();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // 修改 mouseReleased 方法（可选，优化）
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (scrollbar != null) {
            Point mousePoint = new Point((int)mouseX, (int)mouseY);
            scrollbar.onMouseUp(mousePoint, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // 修改 mouseScrolled 方法（优化）
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 检查鼠标是否在树控件区域内
        if (p2pTree != null && p2pTree.isHoveredOrFocused()) {
            // 让滚动条处理滚轮
            if (scrollbar != null && scrollbar.isVisible()) {
                Point mousePoint = new Point((int)mouseX, (int)mouseY);
                if (scrollbar.onMouseWheel(mousePoint, delta)) {
                    syncScrollOffset();
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 如果点击在滚动条上，让滚动条处理
        if (isMouseOverScrollbar(mouseX, mouseY)) {
            Point mousePoint = new Point((int)mouseX, (int)mouseY);
            scrollbar.onMouseDown(mousePoint, button);
            return true;
        }

        // 否则让父类处理
        return super.mouseClicked(mouseX, mouseY, button);
    }
}